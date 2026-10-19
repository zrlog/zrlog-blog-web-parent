package com.zrlog.blog.web;

import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.api.Interceptor;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.config.ZrLogRequestRecordListener;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.blog.web.controller.api.BlogApiCacheController;
import com.zrlog.blog.web.controller.api.BlogApiPublicController;
import com.zrlog.blog.web.controller.page.ArticleController;
import com.zrlog.blog.web.di.BlogComponent;
import com.zrlog.blog.web.di.DaggerBlogComponent;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.template.ZrLogTemplateRender;
import com.zrlog.blog.web.util.BlogNativeImageUtils;
import com.zrlog.business.di.ServiceComponent;
import com.zrlog.business.plugin.ArticleStatisticsPlugin;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.business.plugin.StaticSiteRenderer;
import com.zrlog.common.CacheService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.data.di.DataComponent;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;
import com.zrlog.web.WebSiteInfoProvider;

import java.util.List;

public class BlogWebSetup implements WebSetup {

    private final ZrLogConfig zrLogConfig;
    private final String contextPath;
    private final boolean enablePageStatic;

    public BlogWebSetup(ZrLogConfig zrLogConfig, String contextPath, boolean enablePageStatic) {
        this.zrLogConfig = zrLogConfig;
        this.contextPath = contextPath;
        this.enablePageStatic = enablePageStatic;
        if (zrLogConfig.getServerConfig().isNativeImageAgent()) {
            // Template sampling initializes a request context and freezes SWS bindings.
            // Run it after every module has registered its components.
            zrLogConfig.getServerConfig().addCreateSuccessHandle(() -> {
                BlogNativeImageUtils.reg(zrLogConfig);
                return null;
            });
        }
        zrLogConfig.getServerConfig().addFirstRequestListener(new ZrLogRequestRecordListener(
                zrLogConfig::isInstalled, () -> zrLogConfig.getPlugin(ArticleStatisticsPlugin.class)));
    }

    @Override
    public void setup() {
        zrLogConfig
                .getServerConfig()
                .getBindings()
                .interceptor(
                        BlogApiInterceptor.class,
                        r -> new BlogApiInterceptor(zrLogConfig::isInstalled))
                .interceptor(
                        BlogStaticResourceInterceptor.class,
                        r ->
                                new BlogStaticResourceInterceptor(
                                        () ->
                                                zrLogConfig.getPlugin(
                                                        BlogPageStaticSitePlugin.class)))
                .interceptor(
                        BlogPluginInterceptor.class,
                        r ->
                                new BlogPluginInterceptor(
                                        zrLogConfig::getTokenService,
                                        () -> zrLogConfig.getPlugin(PluginCorePlugin.class)))
                .interceptor(
                        BlogPageInterceptor.class,
                        r ->
                                new BlogPageInterceptor(
                                        request ->
                                                new ZrLogTemplateRender(
                                                        request,
                                                        r.get(CacheService.class),
                                                        zrLogConfig.getTokenService(),
                                                        zrLogConfig.getPlugin(
                                                                PluginCorePlugin.class),
                                                        zrLogConfig.getPlugin(
                                                                TemplateDownloadPlugin.class),
                                                        zrLogConfig.getPlugin(
                                                                BlogPageStaticSitePlugin.class),
                                                        zrLogConfig.getResponseConfig())))
                .singleton(
                        BlogComponent.class,
                        r ->
                                DaggerBlogComponent.factory()
                                        .create(
                                                r.get(DataComponent.class),
                                                r.get(ServiceComponent.class),
                                                zrLogConfig::getTokenService,
                                                () ->
                                                        zrLogConfig.getPlugin(
                                                                PluginCorePlugin.class)))
                .singleton(ArticleService.class, r -> r.get(BlogComponent.class).articles())
                .controller(
                        BlogApiPublicController.class,
                        (r, request, response) -> r.get(BlogComponent.class).publicApi())
                .controller(
                        ArticleController.class,
                        (r, request, response) -> r.get(BlogComponent.class).page())
                .controller(
                        BlogApiCacheController.class,
                        (r, request, response) -> r.get(BlogComponent.class).cacheApi())
                .controller(
                        BlogApiArticleController.class,
                        (r, request, response) -> r.get(BlogComponent.class).articleApi());
        List<Class<? extends Interceptor>> interceptors = zrLogConfig.getServerConfig().getInterceptors();
        interceptors.add(BlogPluginInterceptor.class);
        interceptors.add(BlogStaticResourceInterceptor.class);
        interceptors.add(BlogApiInterceptor.class);
        interceptors.add(BlogPageInterceptor.class);
        //
        BlogRouters.configBlogRouter(zrLogConfig.getServerConfig().getRouter());
        zrLogConfig.getServerConfig().addStaticResourceMapper("/assets/css", "/assets/css", BlogWebSetup.class::getResourceAsStream);
        zrLogConfig.getServerConfig().addStaticResourceMapper("/assets/js", "/assets/js", BlogWebSetup.class::getResourceAsStream);
    }

    @Override
    public Plugins getPlugins() {
        Plugins plugins = new Plugins();
        plugins.add(new TemplateDownloadPlugin(() -> zrLogConfig.getServerConfig().getBindings().build()
                .get(CacheService.class).getPublicWebSiteInfo().getTemplate()));
        if (enablePageStatic) {
            ApplicationContext context = new ApplicationContext(zrLogConfig.getServerConfig());
            context.init();
            plugins.add(
                    new BlogPageStaticSitePlugin(
                            new StaticSiteRenderer(
                                    zrLogConfig.getRequestConfig(),
                                    zrLogConfig.getResponseConfig(),
                                    context),
                            () ->
                                    zrLogConfig
                                            .getWebSetup(WebSiteInfoProvider.class)
                                            .getPublicWebSiteInfo(),
                            contextPath,
                            () ->
                                    zrLogConfig
                                            .getServerConfig()
                                            .getBindings()
                                            .build()
                                            .get(DataComponent.class)
                                            .website()
                                            .faviconBase64DTO()));
        }
        plugins.add(
                zrLogConfig
                        .getServerConfig()
                        .getBindings()
                        .build()
                        .get(BlogComponent.class)
                        .statistics());
        return plugins;
    }
}

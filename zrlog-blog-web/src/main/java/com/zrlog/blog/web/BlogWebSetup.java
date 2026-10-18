package com.zrlog.blog.web;

import com.hibegin.http.server.api.Interceptor;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.config.ZrLogRequestRecordListener;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.blog.web.controller.api.BlogApiCacheController;
import com.zrlog.blog.web.controller.page.ArticleController;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.util.BlogNativeImageUtils;
import com.zrlog.common.CacheService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;
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
            BlogNativeImageUtils.reg(zrLogConfig);
        }
        zrLogConfig.getServerConfig().addFirstRequestListener(new ZrLogRequestRecordListener());
    }

    @Override
    public void setup() {
        zrLogConfig.getServerConfig().getBindings()
                .interceptor(BlogPageInterceptor.class, r -> new BlogPageInterceptor(() -> r.get(CacheService.class)))
                .singleton(ArticleService.class, r -> new ArticleService(r.get(CacheService.class)))
                .controller(ArticleController.class, (r, request, response) -> new ArticleController(r.get(CacheService.class), r.get(ArticleService.class)))
                .controller(BlogApiCacheController.class, (r, request, response) -> new BlogApiCacheController(r.get(CacheService.class)))
                .controller(BlogApiArticleController.class, (r, request, response) -> new BlogApiArticleController(r.get(ArticleService.class)));
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
        plugins.add(new TemplateDownloadPlugin());
        if (enablePageStatic) {
            plugins.add(new BlogPageStaticSitePlugin(zrLogConfig, contextPath));
        }
        plugins.add(new ArticleStatisticsPluginImpl());
        return plugins;
    }
}

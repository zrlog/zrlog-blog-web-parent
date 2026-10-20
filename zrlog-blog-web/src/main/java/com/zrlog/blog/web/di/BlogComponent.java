package com.zrlog.blog.web.di;

import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.blog.web.controller.api.BlogApiCacheController;
import com.zrlog.blog.web.controller.api.BlogApiPublicController;
import com.zrlog.blog.web.controller.page.ArticleController;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.business.di.ServiceBindingsModule;
import com.zrlog.business.di.ServiceComponent;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.common.TokenService;
import com.zrlog.data.di.DataBindingsModule;
import com.zrlog.data.di.DataComponent;
import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;

import java.util.function.Supplier;

@Singleton
@Component(modules = {DataBindingsModule.class, ServiceBindingsModule.class, BlogModule.class})
public interface BlogComponent {
    ArticleService articles();

    ArticleController page();

    BlogApiArticleController articleApi();

    BlogApiCacheController cacheApi();

    BlogApiPublicController publicApi();

    ArticleStatisticsPluginImpl statistics();

    @Component.Factory
    interface Factory {
        BlogComponent create(
                @BindsInstance DataComponent data,
                @BindsInstance ServiceComponent services,
                @BindsInstance Supplier<TokenService> tokens,
                @BindsInstance Supplier<PluginCorePlugin> plugins);
    }
}

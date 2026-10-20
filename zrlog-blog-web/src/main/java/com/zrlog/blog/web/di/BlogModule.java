package com.zrlog.blog.web.di;

import com.zrlog.common.CacheService;
import dagger.Module;
import dagger.Provides;

import java.util.function.Supplier;

@Module
public final class BlogModule {
    private BlogModule() {
    }

    @Provides
    static Supplier<String> webCm(jakarta.inject.Provider<CacheService> cache) {
        return () -> cache.get().getPublicWebSiteInfo().getWebCm();
    }
}

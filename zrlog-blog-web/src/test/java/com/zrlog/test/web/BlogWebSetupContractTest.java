package com.zrlog.test.web;

import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.blog.web.BlogWebSetup;
import com.zrlog.blog.web.BlogWebSetupProvider;
import com.zrlog.blog.web.config.ZrLogRequestRecordListener;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.business.web.ServiceWebSetup;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.data.web.DataWebSetup;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;
import com.zrlog.web.WebSetupContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class BlogWebSetupContractTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static boolean hasPlugin(Plugins plugins, Class<?> pluginClass) {
        return plugins.stream().anyMatch(pluginClass::isInstance);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    public void shouldExposeProviderMetadataAndCreateSetup() throws Exception {
        BlogWebSetupProvider provider = new BlogWebSetupProvider();
        TestZrLogConfig config = config();
        WebSetupContext context = new WebSetupContext(config, new File("db.properties"),
                new File("install.lock"), "/blog", null);

        WebSetup setup = provider.create(context);

        assertEquals("blog", provider.name());
        assertEquals(300, provider.order());
        assertTrue(setup instanceof BlogWebSetup);
    }

    @Test
    public void shouldRegisterBlogRoutesInterceptorsAndStaticResources() throws Exception {
        TestZrLogConfig config = config();
        BlogWebSetup setup = new BlogWebSetup(config, "/blog", false);

        setup.setup();

        List<?> interceptors = config.getServerConfig().getInterceptors();
        assertTrue(interceptors.contains(BlogPluginInterceptor.class));
        assertTrue(interceptors.contains(BlogStaticResourceInterceptor.class));
        assertTrue(interceptors.contains(BlogApiInterceptor.class));
        assertTrue(interceptors.contains(BlogPageInterceptor.class));
        assertTrue(config.getServerConfig().getStaticResourceMapper().containsKey("/assets/css/"));
        assertTrue(config.getServerConfig().getStaticResourceMapper().containsKey("/assets/js/"));
        assertTrue(config.getServerConfig().getRouter().getRouterMap().containsKey("/api/cache"));
        assertTrue(config.getServerConfig().getHttpRequestListenerList().get(0)
                instanceof ZrLogRequestRecordListener);
    }

    @Test
    public void shouldExposeBlogPluginsWithOptionalPageStaticPlugin() throws Exception {
        TestZrLogConfig config = config();

        new DataWebSetup(config).setup();
        new ServiceWebSetup(
                new WebSetupContext(
                        config,
                        new File("db.properties"),
                        new File("install.lock"),
                        "/blog",
                        null))
                .setup();
        BlogWebSetup setup = new BlogWebSetup(config, "/blog", true);
        setup.setup();
        Plugins withStatic = setup.getPlugins();
        Plugins withoutStatic = new BlogWebSetup(config, "/blog", false).getPlugins();

        assertTrue(hasPlugin(withStatic, TemplateDownloadPlugin.class));
        assertTrue(hasPlugin(withStatic, BlogPageStaticSitePlugin.class));
        assertTrue(hasPlugin(withStatic, ArticleStatisticsPluginImpl.class));
        assertTrue(hasPlugin(withoutStatic, TemplateDownloadPlugin.class));
        assertFalse(hasPlugin(withoutStatic, BlogPageStaticSitePlugin.class));
        assertTrue(hasPlugin(withoutStatic, ArticleStatisticsPluginImpl.class));
    }

    private TestZrLogConfig config() throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-setup").getAbsolutePath());
            return new TestZrLogConfig();
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private static class TestZrLogConfig extends ZrLogConfig {

        TestZrLogConfig() {
            super(18080, null, "");
        }

        @Override
        public boolean isInstalled() {
            return false;
        }

        @Override
        public DataSourceWrapper configDatabase() {
            return null;
        }

        @Override
        protected TokenService initTokenService() {
            return null;
        }

        @Override
        public List<IPlugin> getBasePluginList() {
            return new Plugins();
        }
    }
}

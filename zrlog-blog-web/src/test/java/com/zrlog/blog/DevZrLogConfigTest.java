package com.zrlog.blog;

import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.business.plugin.CacheManagerPlugin;
import com.zrlog.business.plugin.PluginCorePluginImpl;
import com.zrlog.business.updater.UpdateVersionInfoPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.inteceptor.DefaultInterceptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevZrLogConfigTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldConfigureBlogDevRuntimeWithoutInstalledDatabase() throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder().getAbsolutePath());

            DevZrLogConfig config = new DevZrLogConfig(7080, null, "/blog");

            assertFalse(config.isInstalled());
            assertTrue(config.getServerConfig().getInterceptors().contains(BlogPluginInterceptor.class));
            assertTrue(config.getServerConfig().getInterceptors().contains(BlogStaticResourceInterceptor.class));
            assertTrue(config.getServerConfig().getInterceptors().contains(BlogApiInterceptor.class));
            assertTrue(config.getServerConfig().getInterceptors().contains(BlogPageInterceptor.class));
            assertTrue(config.getServerConfig().getInterceptors().contains(DefaultInterceptor.class));
            assertTrue(config.getServerConfig().getRouter().getRouterMap().containsKey("/api/cache"));
            assertTrue(config.getServerConfig().getStaticResourceMapper().containsKey("/assets/css/"));
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    @Test
    public void shouldLeaveSharedPluginsToTheServiceModule() throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder().getAbsolutePath());
            DevZrLogConfig config = new DevZrLogConfig(7080, null, "/blog");

            Plugins plugins = new Plugins();
            plugins.addAll(config.getBasePluginList());

            assertFalse(plugins.stream().anyMatch(PluginCorePluginImpl.class::isInstance));
            org.junit.Assert.assertNotNull(config.getWebSetup(com.zrlog.business.web.ServiceWebSetup.class));
            assertFalse(plugins.stream().anyMatch(CacheManagerPlugin.class::isInstance));
            assertTrue(plugins.stream().anyMatch(UpdateVersionInfoPlugin.class::isInstance));
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

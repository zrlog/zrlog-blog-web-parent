package com.zrlog.test.web.plugin;

import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.ArticleStatisticsRunnable;
import com.zrlog.business.plugin.RequestInfo;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.test.support.InMemoryBlogDatabase;
import com.zrlog.web.WebSiteInfoProvider;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ArticleStatisticsPluginImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldRecordOnlyPublicArticleRequests() throws Exception {
        ArticleStatisticsPluginImpl plugin = new ArticleStatisticsPluginImpl();
        PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();

        withConfig(publicWebSiteInfo, () -> {
            plugin.record(requestInfo("/admin/index", 1L));
            plugin.record(requestInfo("/api/article/detail", 2L));
            plugin.record(requestInfo("/install", 3L));
            RequestInfo article = requestInfo("/hello-world", 4L);
            plugin.record(article);

            List<RequestInfo> queue = queue(plugin);
            assertEquals(1, queue.size());
            assertSame(article, queue.get(0));
        });
    }

    @Test
    public void shouldSkipRecordsWhenStatisticsPluginIsDisabledByWebsiteConfig() throws Exception {
        ArticleStatisticsPluginImpl plugin = new ArticleStatisticsPluginImpl();
        PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
        publicWebSiteInfo.setWebCm("<div>no statistics plugin</div>");

        withConfig(publicWebSiteInfo, () -> plugin.record(requestInfo("/hello-world", 4L)));

        assertTrue(queue(plugin).isEmpty());
    }

    @Test
    public void shouldStartAndStopStatisticsScheduler() {
        ArticleStatisticsPluginImpl plugin = new ArticleStatisticsPluginImpl();

        assertFalse(plugin.isStarted());
        assertTrue(plugin.start());
        assertTrue(plugin.start());
        assertTrue(plugin.isStarted());
        assertTrue(plugin.stop());
        assertTrue(plugin.stop());
        assertFalse(plugin.isStarted());
    }

    @Test
    public void shouldSkipStaticPluginRequestsWhenStatisticsRunnableRuns() throws Exception {
        ArticleStatisticsRunnable runnable = new ArticleStatisticsRunnable();
        RequestInfo staticPluginRequest = requestInfo("/hello-world", 4L);
        staticPluginRequest.setUserAgent(BaseStaticSitePlugin.STATIC_USER_AGENT);
        queue(runnable).add(staticPluginRequest);

        runnable.run();

        assertEquals(1, queue(runnable).size());
        assertFalse(staticPluginRequest.isDeal());
    }

    @Test
    public void shouldSkipCrawlerRequestsWhenStatisticsRunnableRuns() throws Exception {
        ArticleStatisticsRunnable runnable = new ArticleStatisticsRunnable();
        RequestInfo crawlerRequest = requestInfo("/hello-world", 4L);
        crawlerRequest.setUserAgent("Googlebot/2.1 (+http://www.google.com/bot.html)");
        queue(runnable).add(crawlerRequest);

        runnable.run();

        assertEquals(1, queue(runnable).size());
        assertFalse(crawlerRequest.isDeal());
    }

    @Test
    public void shouldIncrementArticleClickWithInMemoryDatabaseWhenStatisticsRunnableRuns() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            database.insertArticle(1, "hello-world", "Hello World", "Hello content", "zrlog",
                    "2026-06-01 10:00:00", false, false);
            ArticleStatisticsRunnable runnable = new ArticleStatisticsRunnable();
            RequestInfo requestInfo = requestInfo("/hello-world", 1L);
            queue(runnable).add(requestInfo);

            runnable.run();

            assertTrue(requestInfo.isDeal());
            assertEquals(11L, ((Number) database.scalar("select click from log where logId=?", 1)).longValue());
        }
    }

    @Test
    public void shouldRemoveExpiredAlreadyProcessedRequestsWhenStatisticsRunnableRuns() throws Exception {
        ArticleStatisticsRunnable runnable = new ArticleStatisticsRunnable();
        RequestInfo oldRequest = requestInfo("/hello-world", 4L);
        oldRequest.setDeal(true);
        oldRequest.setRequestTime(System.currentTimeMillis() - 180_000L);
        queue(runnable).add(oldRequest);

        runnable.run();

        assertTrue(queue(runnable).isEmpty());
    }

    private void withConfig(PublicWebSiteInfo publicWebSiteInfo, ThrowingRunnable runnable) throws Exception {
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-statistics").getAbsolutePath());
            Constants.zrLogConfig = new StatisticsZrLogConfig(publicWebSiteInfo);
            runnable.run();
        } finally {
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private static RequestInfo requestInfo(String uri, Long articleId) {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestUri(uri);
        requestInfo.setArticleId(articleId);
        requestInfo.setUserAgent("Mozilla/5.0");
        requestInfo.setRequestTime(System.currentTimeMillis());
        return requestInfo;
    }

    @SuppressWarnings("unchecked")
    private static List<RequestInfo> queue(ArticleStatisticsPluginImpl plugin) throws Exception {
        Field runnableField = ArticleStatisticsPluginImpl.class.getDeclaredField("runnable");
        runnableField.setAccessible(true);
        ArticleStatisticsRunnable runnable = (ArticleStatisticsRunnable) runnableField.get(plugin);
        return queue(runnable);
    }

    @SuppressWarnings("unchecked")
    private static List<RequestInfo> queue(ArticleStatisticsRunnable runnable) throws Exception {
        Field requestInfoListField = ArticleStatisticsRunnable.class.getDeclaredField("requestInfoList");
        requestInfoListField.setAccessible(true);
        return (List<RequestInfo>) requestInfoListField.get(runnable);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class StatisticsZrLogConfig extends ZrLogConfig {

        private final PublicWebSiteInfo publicWebSiteInfo;

        StatisticsZrLogConfig(PublicWebSiteInfo publicWebSiteInfo) {
            super(18080, null, "");
            this.publicWebSiteInfo = publicWebSiteInfo;
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
        public <T> T getWebSetup(Class<T> capability) {
            if (capability == WebSiteInfoProvider.class) {
                CacheService cache = cacheForTest();
                return cache == null ? null : capability.cast(
                        (WebSiteInfoProvider) cache::getPublicWebSiteInfo);
            }
            return super.getWebSetup(capability);
        }

        private CacheService cacheForTest() {
            return new StatisticsCacheService(publicWebSiteInfo);
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

    private static class StatisticsCacheService implements CacheService {

        private final PublicWebSiteInfo publicWebSiteInfo;

        StatisticsCacheService(PublicWebSiteInfo publicWebSiteInfo) {
            this.publicWebSiteInfo = publicWebSiteInfo;
        }

        @Override
        public long getCurrentSqlVersion() {
            return 0;
        }

        @Override
        public long getWebSiteVersion() {
            return 0;
        }

        @Override
        public BaseDataInitVO getInitData() {
            return new BaseDataInitVO();
        }

        @Override
        public BaseDataInitVO refreshInitData() {
            return new BaseDataInitVO();
        }

        @Override
        public PublicWebSiteInfo getPublicWebSiteInfo() {
            return publicWebSiteInfo;
        }

        @Override
        public List<TypeDTO> getArticleTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<TagDTO> getTags() {
            return Collections.emptyList();
        }

        @Override
        public UserBasicDTO getUserInfoById(Long userId) {
            return null;
        }

        @Override
        public Map<String, Object> getTemplateConfigMapWithCache(String template) {
            return Collections.emptyMap();
        }
    }
}

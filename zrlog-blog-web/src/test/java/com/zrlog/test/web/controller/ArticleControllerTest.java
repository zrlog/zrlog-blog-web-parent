package com.zrlog.blog.web.controller.page;

import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.common.CacheService;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.model.Log;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSiteInfoProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ArticleControllerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private ZrLogConfig applicationConfig;

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static void setControllerRequest(Controller controller, HttpRequest request) throws Exception {
        Field field = Controller.class.getDeclaredField("request");
        field.setAccessible(true);
        field.set(controller, request);
    }

    private static void setArticleService(ArticleController controller, ArticleService articleService) throws Exception {
        Field field = ArticleController.class.getDeclaredField("articleService");
        field.setAccessible(true);
        field.set(controller, articleService);
    }

    private static HttpRequest request(String uri, Map<String, Object> attrs) {
        return (HttpRequest) Proxy.newProxyInstance(
                ArticleControllerTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getUri".equals(method.getName())) {
                        return uri;
                    }
                    if ("getAttr".equals(method.getName())) {
                        return attrs;
                    }
                    if ("getContextPath".equals(method.getName())) {
                        return "";
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return "blog.example.com";
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    @Test
    public void shouldParseUriInfoWithPageSuffix() throws Exception {
        ArticleUriInfoVO info = ArticleController.parseUriInfo("/record/2015-06-3.html");
        assertEquals("2015-06", info.getKey());
        assertEquals(3L, info.getPage());
    }

    @Test
    public void shouldParseUriInfoWithoutPageSuffix() throws Exception {
        ArticleUriInfoVO info = ArticleController.parseUriInfo("/tag/java.html");
        assertEquals("java", info.getKey());
        assertEquals(1L, info.getPage());
    }

    @Test
    public void shouldParseSearchUriInfoWithPageSuffix() throws Exception {
        ArticleUriInfoVO info = ArticleController.parseUriInfo("/search/java-2.html");
        assertEquals("java", info.getKey());
        assertEquals(2L, info.getPage());
    }

    @Test
    public void shouldKeepHyphenatedKeyWhenSuffixIsNotNumeric() throws Exception {
        ArticleUriInfoVO info = ArticleController.parseUriInfo("/tag/java-spring.html");

        assertEquals("java-spring", info.getKey());
        assertEquals(1L, info.getPage());
    }

    @Test
    public void shouldParseHyphenatedKeyWithNumericPageSuffix() throws Exception {
        ArticleUriInfoVO info = ArticleController.parseUriInfo("/tag/java-spring-12.html");

        assertEquals("java-spring", info.getKey());
        assertEquals(12L, info.getPage());
    }

    @Test
    public void shouldReturnStaticPageNamesForSimplePages() throws Exception {
        withConfig(
                cache -> {
                    Map<String, Object> attrs = new HashMap<>();
                    ArticleController controller =
                            new ArticleController(
                                    cache,
                                    new ArticleService(cache, () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    }),
                                    () ->
                                            applicationConfig == null
                                                    ? null
                                                    : applicationConfig.getTokenService(),
                                    () -> applicationConfig.getPlugin(PluginCorePlugin.class),
                                    () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    });
                    setControllerRequest(controller, request("/link", attrs));

                    assertEquals("links", controller.link());
                    assertEquals("links", controller.links());
                    assertEquals("archives", controller.archives());
                    assertEquals("categories", controller.categories());
                    assertEquals("tags", controller.tags());
                    assertSame(PageData.class, attrs.get("data").getClass());
                });
    }

    @Test
    public void shouldPrepareTagsPageWithoutArticleRows() throws Exception {
        withConfig(
                cache -> {
                    Map<String, Object> attrs = new HashMap<>();
                    ArticleController controller =
                            new ArticleController(
                                    cache,
                                    new ArticleService(cache, () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    }),
                                    () ->
                                            applicationConfig == null
                                                    ? null
                                                    : applicationConfig.getTokenService(),
                                    () -> applicationConfig.getPlugin(PluginCorePlugin.class),
                                    () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    });
                    setControllerRequest(controller, request("/tags", attrs));

                    assertEquals("tags", controller.tags());
                    assertEquals("tags", attrs.get("yurl"));
                    assertSame(PageData.class, attrs.get("data").getClass());
                });
    }

    @Test
    public void shouldAttachArticleDetailWhenServiceReturnsResult() throws Exception {
        withConfig(
                cache -> {
                    Map<String, Object> attrs = new HashMap<>();
                    ArticleController controller =
                            new ArticleController(
                                    cache,
                                    new ArticleService(cache, () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    }),
                                    () ->
                                            applicationConfig == null
                                                    ? null
                                                    : applicationConfig.getTokenService(),
                                    () -> applicationConfig.getPlugin(PluginCorePlugin.class),
                                    () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    });
                    FakeArticleService articleService = new FakeArticleService(null);
                    ArticleDetailDTO detail = new ArticleDetailDTO();
                    detail.setLogId(99L);
                    articleService.detail = detail;
                    setArticleService(controller, articleService);
                    HttpRequest request = request("/hello-world.html", attrs);
                    setControllerRequest(controller, request);

                    assertEquals("detail", controller.detail());
                    assertSame(detail, attrs.get("log"));
                    assertEquals("hello-world", articleService.lastId);
                    assertSame(request, articleService.lastRequest);
                });
    }

    @Test
    public void shouldLeaveArticleDetailUnsetWhenServiceReturnsNull() throws Exception {
        withConfig(
                cache -> {
                    Map<String, Object> attrs = new HashMap<>();
                    ArticleController controller =
                            new ArticleController(
                                    cache,
                                    new ArticleService(cache, () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    }),
                                    () ->
                                            applicationConfig == null
                                                    ? null
                                                    : applicationConfig.getTokenService(),
                                    () -> applicationConfig.getPlugin(PluginCorePlugin.class),
                                    () -> {
                                        throw new IllegalStateException("Database is unavailable in this test");
                                    });
                    setArticleService(controller, new FakeArticleService(null));
                    setControllerRequest(controller, request("/missing.html", attrs));

                    assertEquals("detail", controller.detail());
                    assertEquals(false, attrs.containsKey("log"));
                });
    }

    private void withConfig(ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder().getAbsolutePath());
            applicationConfig = new TestZrLogConfig();
            runnable.run(((TestZrLogConfig) applicationConfig).cacheForTest());
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private interface ThrowingRunnable {
        void run(CacheService cache) throws Exception;
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
        public <T> T getWebSetup(Class<T> capability) {
            if (capability == WebSiteInfoProvider.class) {
                CacheService cache = cacheForTest();
                return cache == null ? null : capability.cast(
                        (WebSiteInfoProvider) cache::getPublicWebSiteInfo);
            }
            return super.getWebSetup(capability);
        }

        private CacheService cacheForTest() {
            return new TestCacheService();
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

    private static class TestCacheService implements CacheService {
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
            PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
            publicWebSiteInfo.setRows(10L);
            publicWebSiteInfo.setGenerator_html_status(false);
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

    private static class FakeArticleService extends ArticleService {
        private ArticleDetailDTO detail;
        private Object lastId;
        private HttpRequest lastRequest;
        private FakeArticleService(DataSourceWrapper testDataSource) {
            super(new TestCacheService(), () -> new Log(testDataSource));
        }

        @Override
        public ArticleDetailDTO detail(Object idOrAlias, HttpRequest request) throws SQLException {
            lastId = idOrAlias;
            lastRequest = request;
            return detail;
        }
    }
}

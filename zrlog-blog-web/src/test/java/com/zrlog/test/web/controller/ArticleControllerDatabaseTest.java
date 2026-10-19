package com.zrlog.test.web.controller;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.controller.page.ArticleController;
import com.zrlog.common.CacheService;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.test.support.InMemoryBlogDatabase;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArticleControllerDatabaseTest {

    private com.zrlog.common.ZrLogConfig applicationConfig;

    @Test
    public void shouldRenderIndexPageFromInMemoryDatabase() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            applicationConfig = database.config();
            seedArticles(database);
            database.update("update log set sticky=? where logId=?", 2, 1);
            database.update("update log set sticky=? where logId=?", 1, 4);
            Map<String, Object> firstPageAttrs = new HashMap<>();

            String view = controller(database.cacheService(), request("/all-1.html", HttpMethod.GET, "", firstPageAttrs)).index();
            Map<String, Object> secondPageAttrs = new HashMap<>();
            controller(database.cacheService(), request("/all-2.html", HttpMethod.GET, "", secondPageAttrs)).index();

            assertEquals("index", view);
            assertEquals("all-", firstPageAttrs.get("yurl"));
            PageData<ArticleBasicDTO> firstPage = pageData(firstPageAttrs);
            PageData<ArticleBasicDTO> secondPage = pageData(secondPageAttrs);
            assertEquals(3L, firstPage.getTotalElements());
            assertEquals(3L, secondPage.getTotalElements());
            assertEquals(List.of("hello-world", "java-post"), aliases(firstPage));
            assertEquals(List.of("last-public"), aliases(secondPage));
            assertEquals("/blog/hello-world", firstPage.getRows().get(0).getUrl());
            assertTrue(Collections.disjoint(logIds(firstPage), logIds(secondPage)));
            assertNotNull(firstPageAttrs.get("pager"));
        }
    }

    @Test
    public void shouldRenderSearchPageFromRealDao() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            applicationConfig = database.config();
            seedArticles(database);
            Map<String, Object> attrs = new HashMap<>();

            String view = controller(database.cacheService(), request("/search/Java-1.html", HttpMethod.GET, "", attrs))
                    .search();

            assertEquals("page", view);
            assertEquals("Java", attrs.get("key"));
            assertEquals("Java", attrs.get("tipsName"));
            assertEquals("search/Java-", attrs.get("yurl"));
            PageData<ArticleBasicDTO> data = pageData(attrs);
            assertEquals(1L, data.getTotalElements());
            assertEquals("java-post", data.getRows().get(0).getAlias());
        }
    }

    @Test
    public void shouldRenderCategoryTagAndRecordPagesFromRealDao() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            applicationConfig = database.config();
            seedArticles(database);
            database.update("update log set sticky=? where logId=?", 10, 1);

            Map<String, Object> sortAttrs = new HashMap<>();
            assertEquals("page", controller(database.cacheService(), request("/sort/default-1.html", HttpMethod.GET, "",
                    sortAttrs)).sort());
            assertEquals("Default", sortAttrs.get("tipsName"));
            assertEquals(3L, pageData(sortAttrs).getTotalElements());
            assertEquals(List.of("last-public", "java-post"), aliases(pageData(sortAttrs)));

            Map<String, Object> tagAttrs = new HashMap<>();
            assertEquals("page", controller(database.cacheService(), request("/tag/java.html", HttpMethod.GET, "", tagAttrs)).tag());
            assertEquals("java", tagAttrs.get("tipsName"));
            assertEquals(1L, pageData(tagAttrs).getTotalElements());

            Map<String, Object> recordAttrs = new HashMap<>();
            assertEquals("page", controller(database.cacheService(), request("/record/2026_06.html", HttpMethod.GET, "",
                    recordAttrs)).record());
            assertEquals("2026_06", recordAttrs.get("tipsName"));
            assertEquals(3L, pageData(recordAttrs).getTotalElements());
        }
    }

    @Test
    public void shouldRenderArchivesPageWithoutUnboundedArticleQuery() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            applicationConfig = database.config();
            seedArticles(database);
            Map<String, Object> attrs = new HashMap<>();

            assertEquals("archives", controller(database.cacheService(), request("/archives.html", HttpMethod.GET, "", attrs)).archives());

            assertEquals("archives", attrs.get("yurl"));
            PageData<ArticleBasicDTO> data = pageData(attrs);
            assertEquals(0L, data.getTotalElements());
            assertTrue(data.getRows().isEmpty());
        }
    }

    private ArticleController controller(CacheService cache, HttpRequest httpRequest) {
        return new ArticleController(cache, new ArticleService(cache), applicationConfig == null ? null : applicationConfig.getTokenService(), () -> applicationConfig.getPlugin(com.zrlog.business.plugin.PluginCorePlugin.class)) {{
            this.request = httpRequest;
        }};
    }

    @SuppressWarnings("unchecked")
    private static PageData<ArticleBasicDTO> pageData(Map<String, Object> attrs) {
        return (PageData<ArticleBasicDTO>) attrs.get("data");
    }

    private static List<String> aliases(PageData<ArticleBasicDTO> data) {
        return data.getRows().stream().map(ArticleBasicDTO::getAlias).collect(Collectors.toList());
    }

    private static List<Long> logIds(PageData<ArticleBasicDTO> data) {
        return data.getRows().stream().map(ArticleBasicDTO::getLogId).collect(Collectors.toList());
    }

    private static void seedArticles(InMemoryBlogDatabase database) throws Exception {
        database.insertArticle(1, "hello-world", "Hello World", "Hello content", "zrlog",
                "2026-06-01 10:00:00", false, false);
        database.insertArticle(2, "draft-post", "Draft Post", "Draft content", "draft",
                "2026-06-02 10:00:00", true, false);
        database.insertArticle(3, "private-post", "Private Post", "Private content", "private",
                "2026-06-03 10:00:00", false, true);
        database.insertArticle(4, "java-post", "Java Post", "Java content", "java",
                "2026-06-04 10:00:00", false, false);
        database.insertArticle(5, "last-public", "Last Public", "Last content", "misc",
                "2026-06-05 10:00:00", false, false);
    }

    private static HttpRequest request(String uri, HttpMethod method, String key, Map<String, Object> attrs) {
        Map<String, String> params = new HashMap<>();
        if (key != null && !key.isEmpty()) {
            params.put("key", key);
        }
        return (HttpRequest) Proxy.newProxyInstance(
                ArticleControllerDatabaseTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, reflectedMethod, args) -> {
                    if ("getUri".equals(reflectedMethod.getName())) {
                        return uri;
                    }
                    if ("getMethod".equals(reflectedMethod.getName())) {
                        return method;
                    }
                    if ("getAttr".equals(reflectedMethod.getName())) {
                        return attrs;
                    }
                    if ("getContextPath".equals(reflectedMethod.getName())) {
                        return "/blog";
                    }
                    if ("getHeader".equals(reflectedMethod.getName()) && "Host".equals(args[0])) {
                        return "request.example.com";
                    }
                    if ("getHeaderMap".equals(reflectedMethod.getName())) {
                        return Collections.singletonMap("Host", "request.example.com");
                    }
                    if ("getParaToStr".equals(reflectedMethod.getName())) {
                        String value = params.get(args[0].toString());
                        return value == null ? args[1].toString() : value;
                    }
                    if ("toString".equals(reflectedMethod.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }
}

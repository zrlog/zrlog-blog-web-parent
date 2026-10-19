package com.zrlog.test.business.service;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequestImpl;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.business.service.ArticleService.ArticleListOrder;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.test.support.InMemoryBlogDatabase;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArticleServiceDatabaseTest {

    @Test
    public void shouldLoadArticleDetailFromInMemoryDatabase() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            seedThreePublishedArticles(database);
            database.insertComment(1, 2, "first comment");

            ArticleDetailDTO detail = new ArticleService(database.cacheService()).detail("hello-world", request("/blog"));

            assertNotNull(detail);
            assertEquals(Long.valueOf(2), detail.getId());
            assertEquals("hello-world", detail.getAlias());
            assertEquals("/blog/hello-world", detail.getUrl());
            assertEquals("//blog.example.com/blog/hello-world", detail.getNoSchemeUrl());
            assertEquals("//blog.example.com/blog/addComment", detail.getCommentUrl());
            assertEquals("Default", detail.getTypeName());
            assertEquals("/blog/sort/default", detail.getTypeUrl());
            assertEquals(Long.valueOf(1), detail.getCommentSize());
            assertEquals(1, detail.getComments().size());
            assertEquals("first comment", detail.getComments().get(0).getUserComment());
            assertEquals("previous-post", detail.getLastLog().getAlias());
            assertEquals("/blog/previous-post", detail.getLastLog().getUrl());
            assertEquals("next-post", detail.getNextLog().getAlias());
            assertEquals("/blog/next-post", detail.getNextLog().getUrl());
            assertEquals(2, detail.getTags().size());
            assertEquals("java", detail.getTags().get(0).getName());
            assertEquals("/blog/tag/java", detail.getTags().get(0).getUrl());
            assertFalse(detail.getToc().isEmpty());
            assertTrue(detail.getTocHtml().contains("Hello World"));
        }
    }

    @Test
    public void shouldFollowSiteHostStaticModeAndCommentSwitchWithoutGlobalConfig() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            seedThreePublishedArticles(database);
            database.insertComment(1, 2, "first comment");
            ArticleService service = new ArticleService(database.cacheService());
            var site = database.cacheService().getPublicWebSiteInfo();
            site.setHost("configured.example");
            site.setGenerator_html_status(true);
            site.setDisable_comment_status(true);
            ArticleDetailDTO staticArticle = service.detail("hello-world", request("/blog"));
            assertEquals("//configured.example/blog/hello-world.html", staticArticle.getNoSchemeUrl());
            assertEquals("/blog/tag/java.html", staticArticle.getTags().get(0).getUrl());
            assertEquals("/blog/previous-post.html", staticArticle.getLastLog().getUrl());
            assertTrue(staticArticle.getComments().isEmpty());
            assertFalse(staticArticle.getCanComment());
            site.setGenerator_html_status(false);
            site.setHost("renamed.example");
            site.setDisable_comment_status(false);
            ArticleDetailDTO dynamicArticle = service.detail("hello-world", request("/blog"));
            assertEquals("//renamed.example/blog/hello-world", dynamicArticle.getNoSchemeUrl());
            assertEquals("/blog/tag/java", dynamicArticle.getTags().get(0).getUrl());
            assertEquals(1, dynamicArticle.getComments().size());
            assertTrue(dynamicArticle.getCanComment());
        }
    }

    @Test
    public void shouldSearchArticlesThroughRealLogDao() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            database.insertArticle(1, "java-post", "Java Post", "Java content", "java,search",
                    "2026-06-01 10:00:00", false, false);
            database.insertArticle(2, "private-java-post", "Java Private", "Java private", "java",
                    "2026-06-02 10:00:00", false, true);
            database.insertArticle(3, "other-post", "Other Post", "Other content", "other",
                    "2026-06-03 10:00:00", false, false);

            PageData<ArticleBasicDTO> data = new ArticleService(database.cacheService())
                    .pageByKeywords(new PageRequestImpl(1L, 10L), "Java", request("/blog"),
                            ArticleListOrder.STICKY_FIRST);

            assertEquals(1L, data.getTotalElements());
            assertEquals("Java", data.getKey());
            assertEquals(1, data.getRows().size());
            ArticleBasicDTO row = data.getRows().get(0);
            assertTrue(row.getTitle().contains("<font color=\"#CC0000\">Java</font>"));
            assertEquals("java-post", row.getAlias());
            assertEquals("/blog/java-post", row.getUrl());
            assertEquals("/blog/sort/default", row.getTypeUrl());
            assertEquals("java", row.getTags().get(0).getName());
        }
    }

    @Test
    public void shouldUseStickyOrderOnlyForHomeArticlePages() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            database.insertArticle(1, "java-old", "Java Old", "Java old content", "java",
                    "2026-06-01 10:00:00", false, false);
            database.insertArticle(2, "java-new", "Java New", "Java new content", "java",
                    "2026-06-02 10:00:00", false, false);
            database.insertArticle(3, "other-post", "Other Post", "Other content", "other",
                    "2026-06-03 10:00:00", false, false);
            database.update("update log set sticky=? where logId=?", 1, 1);
            ArticleService service = new ArticleService(database.cacheService());

            PageData<ArticleBasicDTO> home = service.pageByKeywords(
                    new PageRequestImpl(1L, 10L), "", request("/blog"), ArticleListOrder.STICKY_FIRST);
            PageData<ArticleBasicDTO> feed = service.pageByKeywords(
                    new PageRequestImpl(1L, 10L), "", request("/blog"), ArticleListOrder.NEWEST_FIRST);
            PageData<ArticleBasicDTO> legacy = service.pageByKeywords(
                    new PageRequestImpl(1L, 10L), "", request("/blog"));
            PageData<ArticleBasicDTO> search = service.pageByKeywords(
                    new PageRequestImpl(1L, 10L), "Java", request("/blog"), ArticleListOrder.STICKY_FIRST);

            assertEquals(3L, home.getTotalElements());
            assertEquals("java-old", home.getRows().get(0).getAlias());
            assertEquals(3L, feed.getTotalElements());
            assertEquals("other-post", feed.getRows().get(0).getAlias());
            assertEquals(3L, legacy.getTotalElements());
            assertEquals("other-post", legacy.getRows().get(0).getAlias());
            assertEquals(2L, search.getTotalElements());
            assertEquals("Java", search.getKey());
            assertEquals("java-new", search.getRows().get(0).getAlias());
        }
    }

    private static void seedThreePublishedArticles(InMemoryBlogDatabase database) throws Exception {
        database.insertArticle(1, "previous-post", "Previous Post", "Previous content", "history",
                "2026-06-01 10:00:00", false, false);
        database.insertArticle(2, "hello-world", "Hello World", "Hello world body", "java,zrlog",
                "2026-06-02 10:00:00", false, false);
        database.insertArticle(3, "next-post", "Next Post", "Next content", "future",
                "2026-06-03 10:00:00", false, false);
    }

    private static HttpRequest request(String contextPath) {
        Map<String, Object> attrs = new HashMap<>();
        return (HttpRequest) Proxy.newProxyInstance(
                ArticleServiceDatabaseTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getContextPath".equals(method.getName())) {
                        return contextPath;
                    }
                    if ("getAttr".equals(method.getName())) {
                        return attrs;
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return "request.example.com";
                    }
                    if ("getHeaderMap".equals(method.getName())) {
                        return Collections.singletonMap("Host", "request.example.com");
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }
}

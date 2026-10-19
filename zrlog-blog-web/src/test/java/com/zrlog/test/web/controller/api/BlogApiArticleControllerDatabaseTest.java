package com.zrlog.test.web.controller.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.data.dto.VisitorCommentDTO;
import com.zrlog.model.Comment;
import com.zrlog.model.Log;
import com.zrlog.test.support.InMemoryBlogDatabase;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlogApiArticleControllerDatabaseTest {

    @Test
    public void shouldServeArticleApisThroughRealDatabase() throws Exception {
        try (InMemoryBlogDatabase database = InMemoryBlogDatabase.open()) {
            seedArticles(database);
            database.insertComment(1, 2, "api comment");
            BlogApiArticleController controller =
                    new BlogApiArticleController(
                            new ArticleService(database.cacheService(), Log::new), Comment::new);

            setControllerRequest(controller, request(Map.of("id", "hello-world")));
            ApiStandardResponse<ArticleDetailDTO> detail = controller.detail();

            assertEquals(Long.valueOf(2), detail.getData().getId());
            assertEquals("hello-world", detail.getData().getAlias());
            assertEquals("api comment", detail.getData().getComments().get(0).getUserComment());

            setControllerRequest(controller, request(Map.of("key", "Java", "page", "1", "size", "10")));
            ApiStandardResponse<PageData<ArticleBasicDTO>> page = controller.index();

            assertEquals(1L, page.getData().getTotalElements());
            assertEquals("java-post", page.getData().getRows().get(0).getAlias());
            assertFalse(page.getData().getRows().get(0).getTags().isEmpty());

            database.update("update log set sticky=? where logId=?", 1, 1);
            setControllerRequest(controller, request(Map.of("page", "1", "size", "10")));
            ApiStandardResponse<PageData<ArticleBasicDTO>> homePage = controller.index();
            setControllerRequest(controller, request(Map.of("feed", "true", "page", "1", "size", "10")));
            ApiStandardResponse<PageData<ArticleBasicDTO>> feedPage = controller.index();

            assertEquals("previous-post", homePage.getData().getRows().get(0).getAlias());
            assertEquals("java-post", feedPage.getData().getRows().get(0).getAlias());

            setControllerRequest(controller, request(Map.of("id", "2")));
            ApiStandardResponse<List<VisitorCommentDTO>> comments = controller.comment();

            assertEquals(1, comments.getData().size());
            assertEquals("api comment", comments.getData().get(0).getUserComment());
            assertEquals("reader", comments.getData().get(0).getUserName());
            assertFalse(comments.getData().get(0).getGravatarId().isEmpty());
        }
    }

    private static void seedArticles(InMemoryBlogDatabase database) throws Exception {
        database.insertArticle(1, "previous-post", "Previous Post", "Previous content", "history",
                "2026-06-01 10:00:00", false, false);
        database.insertArticle(2, "hello-world", "Hello World", "Hello world body", "java,zrlog",
                "2026-06-02 10:00:00", false, false);
        database.insertArticle(3, "java-post", "Java Post", "Java content", "java",
                "2026-06-03 10:00:00", false, false);
    }

    private static void setControllerRequest(Controller controller, HttpRequest request) throws Exception {
        Field field = Controller.class.getDeclaredField("request");
        field.setAccessible(true);
        field.set(controller, request);
    }

    private static HttpRequest request(Map<String, String> params) {
        Map<String, String[]> paramMap = new HashMap<>();
        params.forEach((key, value) -> paramMap.put(key, new String[]{value}));
        return (HttpRequest) Proxy.newProxyInstance(
                BlogApiArticleControllerDatabaseTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getContextPath".equals(method.getName())) {
                        return "/blog";
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return "request.example.com";
                    }
                    if ("getHeaderMap".equals(method.getName())) {
                        return Collections.singletonMap("Host", "request.example.com");
                    }
                    if ("getParamMap".equals(method.getName()) || "decodeParamMap".equals(method.getName())) {
                        return paramMap;
                    }
                    if ("getParaToStr".equals(method.getName())) {
                        String value = params.get(args[0].toString());
                        if (args.length > 1) {
                            return value == null ? args[1].toString() : value;
                        }
                        return value;
                    }
                    if ("getParaToInt".equals(method.getName())) {
                        String value = params.get(args[0].toString());
                        if (value == null) {
                            return args.length > 1 ? args[1] : null;
                        }
                        return Integer.parseInt(value);
                    }
                    if ("getParaToBool".equals(method.getName())) {
                        String value = params.get(args[0].toString());
                        if (value == null) {
                            return args.length > 1 ? args[1] : false;
                        }
                        return Boolean.parseBoolean(value);
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }
}

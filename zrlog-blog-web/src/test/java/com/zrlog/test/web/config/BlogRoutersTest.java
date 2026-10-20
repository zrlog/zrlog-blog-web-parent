package com.zrlog.test.web.config;

import com.hibegin.http.server.web.Router;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.blog.web.controller.api.BlogApiCacheController;
import com.zrlog.blog.web.controller.api.BlogApiPublicController;
import com.zrlog.blog.web.controller.page.ArticleController;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BlogRoutersTest {

    private static void assertRoute(Router router, String path, Class<?> controllerClass, String methodName) {
        Method method = router.getRouterMap().get(path);
        assertNotNull("Missing route " + path, method);
        assertEquals(controllerClass, method.getDeclaringClass());
        assertEquals(methodName, method.getName());
    }

    @Test
    public void shouldRegisterBlogApiAndPageRoutes() {
        Router router = new Router();

        BlogRouters.configBlogRouter(router);

        assertRoute(router, "/api/public/blogResource", BlogApiPublicController.class, "blogResource");
        assertRoute(router, "/api/article/detail", BlogApiArticleController.class, "detail");
        assertRoute(router, "/api/article/comment", BlogApiArticleController.class, "comment");
        assertRoute(router, "/api/cache", BlogApiCacheController.class, "index");
        assertRoute(router, "/", ArticleController.class, "index");
    }
}

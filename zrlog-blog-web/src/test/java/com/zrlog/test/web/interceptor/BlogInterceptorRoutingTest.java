package com.zrlog.test.web.interceptor;

import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.util.http.handle.CloseResponseHandle;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.config.RequestConfig;
import com.hibegin.http.server.config.ServerConfig;
import com.hibegin.http.server.web.Controller;
import com.hibegin.http.server.web.Router;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.business.exception.MissingInstallException;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.util.StaticFileCacheUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlogInterceptorRoutingTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private com.zrlog.common.ZrLogConfig applicationConfig;

    private static HttpRequest request(String uri) {
        return request(uri, null);
    }

    private static HttpRequest request(String uri, String userAgent) {
        return request(uri, userAgent, null);
    }

    private static HttpRequest request(String uri, String userAgent, ServerConfig serverConfig) {
        return request(uri, userAgent, serverConfig, null, HttpMethod.GET);
    }

    private static HttpRequest request(String uri, String userAgent, ServerConfig serverConfig,
                                       RequestConfig requestConfig, HttpMethod httpMethod) {
        return (HttpRequest) Proxy.newProxyInstance(
                BlogInterceptorRoutingTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getUri".equals(method.getName())) {
                        return uri;
                    }
                    if ("getHeader".equals(method.getName()) && "User-Agent".equals(args[0])) {
                        return userAgent;
                    }
                    if ("getContextPath".equals(method.getName())) {
                        return "";
                    }
                    if ("getRequestConfig".equals(method.getName())) {
                        return requestConfig;
                    }
                    if ("getMethod".equals(method.getName())) {
                        return httpMethod;
                    }
                    if ("getServerConfig".equals(method.getName())) {
                        return serverConfig;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    private static ServerConfig pageServerConfig() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        putRoute(serverConfig, "/article", "article");
        putRoute(serverConfig, "/index", "index");
        putRoute(serverConfig, "/sort", "sort");
        putRoute(serverConfig, "/search", "search");
        putRoute(serverConfig, "/tag", "tag");
        putRoute(serverConfig, "/record", "record");
        putRoute(serverConfig, "/detail", "detail");
        return serverConfig;
    }

    private static void putRoute(ServerConfig serverConfig, String path, String methodName) throws Exception {
        Method method = TestPageController.class.getDeclaredMethod(methodName);
        serverConfig.getRouter().getRouterMap().put(path, method);
    }

    private static void assertBlogPageRoute(ServerConfig serverConfig, String uri, String expectedMethod)
            throws Exception {
        TestPageController.lastMethod = null;

        boolean continueChain = new BlogPageInterceptor(renderRequest -> {
            throw new AssertionError("No template should be rendered for this route");
        })
                .doInterceptor(request(uri, null, serverConfig), new CapturedResponse().response());

        assertFalse(continueChain);
        assertEquals(expectedMethod, TestPageController.lastMethod);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    public void shouldMatchBlogApiRequests() {
        BlogApiInterceptor interceptor = new BlogApiInterceptor(() -> true);

        assertTrue(interceptor.isHandleAble(request("/api")));
        assertTrue(interceptor.isHandleAble(request("/api/article/detail")));
        assertFalse(interceptor.isHandleAble(request("/article")));
    }

    @Test
    public void shouldMatchBlogPluginRequests() {
        BlogPluginInterceptor interceptor = new BlogPluginInterceptor(() -> applicationConfig.getTokenService(), () -> applicationConfig.getPlugin(com.zrlog.business.plugin.PluginCorePlugin.class));

        assertTrue(interceptor.isHandleAble(request("/plugin/comment/static/main.js")));
        assertTrue(interceptor.isHandleAble(request("/p/comment")));
        assertTrue(interceptor.isHandleAble(request("/api/plugin/comment")));
        assertTrue(interceptor.isHandleAble(request("/api/p/comment")));
        assertFalse(interceptor.isHandleAble(request("/api/article/detail")));
    }

    @Test
    public void shouldMatchBlogPageRequests() {
        BlogPageInterceptor interceptor = new BlogPageInterceptor(renderRequest -> {
            throw new AssertionError("No template should be rendered for this route");
        });

        assertTrue(interceptor.isHandleAble(request("/")));
        assertTrue(interceptor.isHandleAble(request("/article.html")));
        assertFalse(interceptor.isHandleAble(request("/static/app.js")));
    }

    @Test
    public void shouldSkipAdminAndStaticPluginRequestsForStaticResources() {
        BlogStaticResourceInterceptor interceptor = new BlogStaticResourceInterceptor(() -> applicationConfig.getPlugin(com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin.class));

        assertFalse(interceptor.isHandleAble(request("/admin/login")));
        assertFalse(interceptor.isHandleAble(request("/error/404.html")));
        assertFalse(interceptor.isHandleAble(request("/index.html", BaseStaticSitePlugin.STATIC_USER_AGENT)));
        assertTrue(interceptor.isHandleAble(request("/assets/app.css")));
    }

    @Test
    public void shouldRenderLocalStaticFileAndStopInterceptorChain() throws Exception {
        Path staticPath = temporaryFolder.newFolder("static").toPath();
        Path asset = staticPath.resolve("assets/app.css");
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, "body{color:#333}");
        String previousStaticPath = System.getProperty("sws.static.path");
        try {
            System.setProperty("sws.static.path", staticPath.toString());
            assertTrue(StaticFileCacheUtils.getInstance().getFileFlagFirstByCache("assets/app.css").length() > 0);
            CapturedResponse capturedResponse = new CapturedResponse();

            boolean continueChain = new BlogStaticResourceInterceptor(() -> applicationConfig.getPlugin(com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin.class))
                    .doInterceptor(request("/assets/app.css"), capturedResponse.response());

            assertFalse(continueChain);
            assertTrue(capturedResponse.writtenFile.isFile());
            assertTrue(capturedResponse.writtenFile.getAbsolutePath().endsWith("assets/app.css"));
            assertEquals("max-age=31536000, immutable", capturedResponse.addedHeaders.get("Cache-Control"));
        } finally {
            restoreProperty("sws.static.path", previousStaticPath);
            StaticFileCacheUtils.getInstance().refreshCacheFileMap();
        }
    }

    @Test
    public void shouldRenderClasspathStaticResourceAndStopInterceptorChain() {
        CapturedResponse capturedResponse = new CapturedResponse();

        boolean continueChain = new BlogStaticResourceInterceptor(() -> applicationConfig.getPlugin(com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin.class))
                .doInterceptor(request("/assets/css/markdown.css"), capturedResponse.response());

        assertFalse(continueChain);
        assertEquals(Integer.valueOf(200), capturedResponse.writeCode);
        assertEquals("max-age=31536000, immutable", capturedResponse.addedHeaders.get("Cache-Control"));
        assertEquals("text/css", capturedResponse.addedHeaders.get("Content-Type"));
    }

    @Test
    public void shouldContinueInterceptorChainWhenStaticResourceIsMissing() throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-static").getAbsolutePath());
            applicationConfig = new NotInstalledZrLogConfig();

            boolean continueChain = new BlogStaticResourceInterceptor(() -> applicationConfig.getPlugin(com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin.class))
                    .doInterceptor(request("/missing.css"), new CapturedResponse().response());

            assertTrue(continueChain);
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    @Test
    public void shouldRenderStaticSiteCacheFileAndStopInterceptorChain() throws Exception {
        File cacheFile = temporaryFolder.newFile("hello-world.html");
        Files.writeString(cacheFile.toPath(), "<html>Hello</html>");
        BlogPageStaticSitePlugin staticSitePlugin = mock(BlogPageStaticSitePlugin.class);
        when(staticSitePlugin.loadCacheFile(any())).thenReturn(cacheFile);
        CapturedResponse capturedResponse = new CapturedResponse();

        withStaticSiteConfig(staticSitePlugin, () -> {
            boolean continueChain = new BlogStaticResourceInterceptor(() -> applicationConfig.getPlugin(com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin.class))
                    .doInterceptor(request("/hello-world.html"), capturedResponse.response());

            assertFalse(continueChain);
            assertEquals(cacheFile, capturedResponse.writtenFile);
        });
    }

    @Test
    public void shouldProxyBlogPluginRequestWithoutAdminTokenWhenTokenServiceIsMissing() throws Exception {
        FakePluginCorePlugin plugin = new FakePluginCorePlugin(true);
        CapturedResponse capturedResponse = new CapturedResponse();
        withPluginConfig(plugin, () -> {
            boolean continueChain = new BlogPluginInterceptor(() -> applicationConfig.getTokenService(), () -> applicationConfig.getPlugin(com.zrlog.business.plugin.PluginCorePlugin.class))
                    .doInterceptor(request("/plugin/comment/static/main.js"), capturedResponse.response());

            assertFalse(continueChain);
            assertEquals("/comment/static/main.js", plugin.lastUri);
            assertNull(plugin.lastAdminToken);
            assertNull(capturedResponse.code);
        });
    }

    @Test
    public void shouldRender404WhenPluginCoreRejectsBlogPluginRequest() throws Exception {
        FakePluginCorePlugin plugin = new FakePluginCorePlugin(false);
        CapturedResponse capturedResponse = new CapturedResponse();
        withPluginConfig(plugin, () -> {
            boolean continueChain = new BlogPluginInterceptor(() -> applicationConfig.getTokenService(), () -> applicationConfig.getPlugin(com.zrlog.business.plugin.PluginCorePlugin.class))
                    .doInterceptor(request("/api/p/comment"), capturedResponse.response());

            assertFalse(continueChain);
            assertEquals("/comment", plugin.lastUri);
            assertEquals(Integer.valueOf(404), capturedResponse.code);
        });
    }

    @Test
    public void shouldRender404WhenBlogPageRouteCannotBeResolved() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        CapturedResponse capturedResponse = new CapturedResponse();

        boolean continueChain = new BlogPageInterceptor(renderRequest -> {
            throw new AssertionError("No template should be rendered for this route");
        })
                .doInterceptor(request("/missing", null, serverConfig), capturedResponse.response());

        assertFalse(continueChain);
        assertEquals(Integer.valueOf(404), capturedResponse.code);
    }

    @Test
    public void shouldResolveBlogPageRouteAliases() throws Exception {
        ServerConfig serverConfig = pageServerConfig();

        assertBlogPageRoute(serverConfig, "/article.html", "article");
        assertBlogPageRoute(serverConfig, "/all-2", "index");
        assertBlogPageRoute(serverConfig, "/sort/java", "sort");
        assertBlogPageRoute(serverConfig, "/search/zrlog", "search");
        assertBlogPageRoute(serverConfig, "/tag/test", "tag");
        assertBlogPageRoute(serverConfig, "/record/2026", "record");
        assertBlogPageRoute(serverConfig, "/hello-world", "detail");
    }

    @Test(expected = MissingInstallException.class)
    public void shouldRejectApiRequestsWhenBlogIsNotInstalled() throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-api").getAbsolutePath());
            applicationConfig = new NotInstalledZrLogConfig();

            new BlogApiInterceptor(() -> false).doInterceptor(request("/api/article/detail"), null);
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    @Test
    public void shouldDispatchApiRequestWhenBlogIsInstalled() throws Exception {
        Router router = new Router();
        router.getRouterMap().put("/api/ping", TestApiController.class.getDeclaredMethod("ping"));
        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setRouter(router);
        TestApiController.handled = false;

        withInstalledConfig(() -> {
            boolean continueChain = new BlogApiInterceptor(() -> true).doInterceptor(
                    request("/api/ping", null, new ServerConfig(), requestConfig, HttpMethod.GET),
                    new CapturedResponse().response());

            assertFalse(continueChain);
            assertTrue(TestApiController.handled);
        });
    }

    private void withPluginConfig(FakePluginCorePlugin plugin, ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-plugin").getAbsolutePath());
            applicationConfig = new PluginZrLogConfig(plugin);
            runnable.run();
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private void withStaticSiteConfig(BlogPageStaticSitePlugin plugin, ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-static-cache").getAbsolutePath());
            applicationConfig = new StaticSiteZrLogConfig(plugin);
            runnable.run();
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private void withInstalledConfig(ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            Path rootPath = temporaryFolder.newFolder("zrlog-blog-api-installed").toPath();
            Files.createDirectories(rootPath.resolve("conf"));
            Files.writeString(rootPath.resolve("conf/db.properties"), "");
            System.setProperty("sws.root.path", rootPath.toString());
            applicationConfig = new InstalledZrLogConfig();
            runnable.run();
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static class TestPageController extends Controller {
        private static String lastMethod;

        public Object article() {
            lastMethod = "article";
            return null;
        }

        public Object index() {
            lastMethod = "index";
            return null;
        }

        public Object sort() {
            lastMethod = "sort";
            return null;
        }

        public Object search() {
            lastMethod = "search";
            return null;
        }

        public Object tag() {
            lastMethod = "tag";
            return null;
        }

        public Object record() {
            lastMethod = "record";
            return null;
        }

        public Object detail() {
            lastMethod = "detail";
            return null;
        }
    }

    public static class TestApiController extends Controller {
        private static boolean handled;

        public Object ping() {
            handled = true;
            return null;
        }
    }

    private static class NotInstalledZrLogConfig extends ZrLogConfig {
        NotInstalledZrLogConfig() {
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

    private static class InstalledZrLogConfig extends NotInstalledZrLogConfig {
        @Override
        public boolean isInstalled() {
            return true;
        }
    }

    private static class PluginZrLogConfig extends NotInstalledZrLogConfig {
        private final FakePluginCorePlugin plugin;

        PluginZrLogConfig(FakePluginCorePlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public <T extends IPlugin> T getPlugin(Class<T> pluginClass) {
            if (pluginClass.isInstance(plugin)) {
                return pluginClass.cast(plugin);
            }
            return null;
        }
    }

    private static class StaticSiteZrLogConfig extends NotInstalledZrLogConfig {
        private final BlogPageStaticSitePlugin plugin;

        StaticSiteZrLogConfig(BlogPageStaticSitePlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public <T extends IPlugin> T getPlugin(Class<T> pluginClass) {
            if (pluginClass.isInstance(plugin)) {
                return pluginClass.cast(plugin);
            }
            return null;
        }
    }

    private static class FakePluginCorePlugin implements PluginCorePlugin {
        private final boolean accessResult;
        private String lastUri;
        private AdminTokenVO lastAdminToken;

        FakePluginCorePlugin(boolean accessResult) {
            this.accessResult = accessResult;
        }

        @Override
        public boolean refreshCache(String cacheVersion, HttpRequest request) {
            return false;
        }

        @Override
        public CloseResponseHandle getContext(String uri, HttpMethod method, HttpRequest request,
                                              AdminTokenVO adminTokenVO)
                throws IOException, URISyntaxException, InterruptedException {
            return null;
        }

        @Override
        public <T> T requestService(HttpRequest inputRequest, Map<String, String[]> params,
                                    AdminTokenVO adminTokenVO, Class<T> clazz)
                throws IOException, URISyntaxException, InterruptedException {
            return null;
        }

        @Override
        public boolean accessPlugin(String uri, HttpRequest request, HttpResponse response,
                                    AdminTokenVO adminTokenVO)
                throws IOException, URISyntaxException, InterruptedException {
            lastUri = uri;
            lastAdminToken = adminTokenVO;
            return accessResult;
        }

        @Override
        public String getToken() {
            return "token";
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public boolean stop() {
            return true;
        }
    }

    private static class CapturedResponse {
        private final Map<String, String> addedHeaders = new HashMap<>();
        private File writtenFile;
        private Integer code;
        private Integer writeCode;
        private InputStream writtenStream;

        private HttpResponse response() {
            return (HttpResponse) Proxy.newProxyInstance(
                    BlogInterceptorRoutingTest.class.getClassLoader(),
                    new Class[]{HttpResponse.class},
                    (proxy, method, args) -> {
                        if ("addHeader".equals(method.getName())) {
                            addedHeaders.put(args[0].toString(), args[1].toString());
                            return null;
                        }
                        if ("writeFile".equals(method.getName())) {
                            writtenFile = (File) args[0];
                            return null;
                        }
                        if ("renderCode".equals(method.getName())) {
                            code = (Integer) args[0];
                            return null;
                        }
                        if ("write".equals(method.getName())) {
                            writtenStream = (InputStream) args[0];
                            writeCode = (Integer) args[1];
                            return null;
                        }
                        if ("toString".equals(method.getName())) {
                            return "HttpResponseProxy";
                        }
                        return null;
                    });
        }
    }
}

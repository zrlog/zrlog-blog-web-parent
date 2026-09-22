package com.zrlog.blog.web.template;

import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.HttpVersion;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.config.RequestConfig;
import com.hibegin.http.server.config.ServerConfig;
import com.hibegin.http.server.web.cookie.Cookie;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.business.plugin.type.StaticSiteType;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.dto.PluginDTO;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Proxy;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ZrLogTemplateRenderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldNotCacheStaticPluginRequestsAsGeneratedHtml() throws Exception {
        withConfig(true, () -> assertFalse(canGeneratorHtml(
                request("/article.html", BaseStaticSitePlugin.STATIC_USER_AGENT))));
    }

    @Test
    public void shouldNotCacheHtmlWhenStaticGenerationIsDisabled() throws Exception {
        withConfig(false, () -> assertFalse(canGeneratorHtml(request("/article.html", null))));
    }

    @Test
    public void shouldOnlyCacheHtmlRequestsWhenStaticGenerationIsEnabled() throws Exception {
        withConfig(true, () -> {
            assertTrue(canGeneratorHtml(request("/article.html", null)));
            assertFalse(canGeneratorHtml(request("/assets/app.css", null)));
        });
    }

    @Test
    public void shouldInitializeDefaultTemplateAndCheckTemplateFileExistence() throws Exception {
        withConfig(false, () -> {
            ZrLogTemplateRender render = new ZrLogTemplateRender(templateRequest("/missing.html"));

            assertTrue(render.existsByTemplateName("index"));
            assertFalse(render.existsByTemplateName("missing-template-file"));
        });
    }

    @Test
    public void shouldRejectDirectRenderMethods() throws Exception {
        withConfig(false, () -> {
            ZrLogTemplateRender render = new ZrLogTemplateRender(templateRequest("/missing.html"));

            assertThrows(RuntimeException.class, () -> render.render("index"));
            assertThrows(RuntimeException.class, () -> render.render(new ByteArrayInputStream(
                    "index".getBytes(StandardCharsets.UTF_8))));
        });
    }

    @Test
    public void shouldRenderDefaultTemplateAndTransformHtml() throws Exception {
        withConfig(false, () -> {
            ZrLogTemplateRender render = new ZrLogTemplateRender(templateRequest("/empty.html"));

            String html = render.renderByTemplateName("empty");

            assertNotNull(html);
            assertTrue(html.contains("graalvm native image build"));
            assertTrue(html.contains("<!--"));
        });
    }

    @Test
    public void shouldGenerateTemplateWwwIndexPreviewHtml() throws Exception {
        TestStaticSitePlugin staticSitePlugin = new TestStaticSitePlugin("/generated/template-www");
        withConfig(true, staticSitePlugin, () -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("data", previewPageData());
            HttpRequest request = templateRequest("/index.html",
                    Cookie.saxToCookie("template=" + Constants.TEMPLATE_BASE_PATH + "template-www"), attrs);
            ZrLogTemplateRender render = new ZrLogTemplateRender(request);

            String html = render.renderByTemplateName("index");
            Path output = staticSitePlugin.loadCacheFile(request).toPath();
            String staticHtml = Files.readString(output, StandardCharsets.UTF_8);

            assertTrue(Files.exists(output));
            assertTrue(html.contains("action=\"/search\""));
            assertTrue(staticHtml.contains("Template Preview"));
            assertTrue(html.contains("<nav"));
            assertTrue(html.contains("Template Preview"));
            assertTrue(html.contains("Default"));
            assertTrue(html.contains("12 阅读"));
            assertTrue(html.contains("阅读全文"));
            assertTrue(html.contains("flex items-stretch gap-2"));
            assertTrue(html.contains("min-w-0 h-11 flex-1"));
            assertTrue(html.contains("h-11 shrink-0 inline-flex"));

            HttpRequest englishRequest = templateRequest("/index-en",
                    Cookie.saxToCookie("template=" + Constants.TEMPLATE_BASE_PATH + "template-www"), attrs, "en-US");
            String englishHtml = new ZrLogTemplateRender(englishRequest).renderByTemplateName("index");
            assertTrue(englishHtml.contains("12 views"));
            assertTrue(englishHtml.contains("Read more"));
        });
    }

    @Test
    public void shouldRenderExactOpenGraphUrlForDefaultTemplate() throws Exception {
        withConfig(false, () -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("data", previewPageData());
            HttpRequest request = templateRequest("/all-3", null, attrs);

            String html = new ZrLogTemplateRender(request).renderByTemplateName("index");

            assertTrue(html.contains("<meta property=\"og:url\" content=\"//blog.example.com/all-3\">"));
        });
    }

    @Test
    public void shouldShowPinnedMarkerOnlyOnDefaultTemplateHomePage() throws Exception {
        withConfig(false, () -> {
            PageData<ArticleBasicDTO> data = previewPageData();
            data.getRows().get(0).setSticky(1);
            Map<String, Object> homeAttrs = new HashMap<>();
            homeAttrs.put("data", data);
            String homeHtml = new ZrLogTemplateRender(
                    templateRequest("/all-1", null, homeAttrs)).renderByTemplateName("index");
            Map<String, Object> searchAttrs = new HashMap<>();
            searchAttrs.put("data", data);
            searchAttrs.put("tipsType", "Search");
            searchAttrs.put("tipsName", "Pinned");
            String searchHtml = new ZrLogTemplateRender(
                    templateRequest("/search/Pinned-1", null, searchAttrs)).renderByTemplateName("page");

            assertTrue(homeHtml.contains("class=\"post-card-pinned\""));
            assertFalse(searchHtml.contains("class=\"post-card-pinned\""));
        });
    }

    private void withConfig(boolean generatorHtmlStatus, ThrowingRunnable runnable) throws Exception {
        withConfig(generatorHtmlStatus, null, runnable);
    }

    private void withConfig(boolean generatorHtmlStatus, StaticSitePlugin staticSitePlugin, ThrowingRunnable runnable)
            throws Exception {
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        String previousRootPath = System.getProperty("sws.root.path");
        String previousStaticPath = System.getProperty("sws.static.path");
        String previousCachePath = System.getProperty("sws.cache.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder().getAbsolutePath());
            if (staticSitePlugin != null) {
                String staticPath = staticPreviewRoot().toString();
                System.setProperty("sws.static.path", staticPath);
                System.setProperty("sws.cache.path", staticPath);
            }
            Constants.zrLogConfig = new TestZrLogConfig(generatorHtmlStatus, staticSitePlugin);
            runnable.run();
        } finally {
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
            restoreProperty("sws.static.path", previousStaticPath);
            restoreProperty("sws.cache.path", previousCachePath);
        }
    }

    private static boolean canGeneratorHtml(HttpRequest request) throws Exception {
        return ZrLogTemplateRender.catGeneratorHtml(request);
    }

    private static HttpRequest request(String uri, String userAgent) {
        return (HttpRequest) Proxy.newProxyInstance(
                ZrLogTemplateRenderTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getUri".equals(method.getName())) {
                        return uri;
                    }
                    if ("getHeader".equals(method.getName()) && "User-Agent".equals(args[0])) {
                        return userAgent;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    private static HttpRequest templateRequest(String uri) {
        return templateRequest(uri, null, new HashMap<>());
    }

    private static HttpRequest templateRequest(String uri, Cookie[] cookies) {
        return templateRequest(uri, cookies, new HashMap<>());
    }

    private static HttpRequest templateRequest(String uri, Cookie[] cookies, Map<String, Object> attrs) {
        return templateRequest(uri, cookies, attrs, null);
    }

    private static HttpRequest templateRequest(String uri, Cookie[] cookies, Map<String, Object> attrs,
                                               String acceptLanguage) {
        RequestConfig requestConfig = new RequestConfig();
        ServerConfig serverConfig = new ServerConfig();
        return (HttpRequest) Proxy.newProxyInstance(
                ZrLogTemplateRenderTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getUri".equals(method.getName())) {
                        return uri;
                    }
                    if ("getUrl".equals(method.getName())) {
                        return uri;
                    }
                    if ("getFullUrl".equals(method.getName())) {
                        return "https://blog.example.com" + uri;
                    }
                    if ("getQueryStr".equals(method.getName())) {
                        return "";
                    }
                    if ("getContextPath".equals(method.getName())) {
                        return "";
                    }
                    if ("getCreateTime".equals(method.getName())) {
                        return System.currentTimeMillis();
                    }
                    if ("getMethod".equals(method.getName())) {
                        return HttpMethod.GET;
                    }
                    if ("getCookies".equals(method.getName())) {
                        return cookies;
                    }
                    if ("getAttr".equals(method.getName())) {
                        return attrs;
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return "blog.example.com";
                    }
                    if ("getHeader".equals(method.getName()) && "Accept-Language".equals(args[0])) {
                        return acceptLanguage;
                    }
                    if ("getHeaderMap".equals(method.getName())) {
                        return Collections.singletonMap("Host", "blog.example.com");
                    }
                    if ("getScheme".equals(method.getName())) {
                        return "https";
                    }
                    if ("getRequestConfig".equals(method.getName())) {
                        return requestConfig;
                    }
                    if ("getServerConfig".equals(method.getName())) {
                        return serverConfig;
                    }
                    if ("getHttpVersion".equals(method.getName())) {
                        return HttpVersion.HTTP_1_1;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    private static Path staticPreviewRoot() throws Exception {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = Files.isDirectory(cwd.resolve("zrlog-freemarker-template")) ? cwd : cwd.getParent();
        Path staticPath = root.resolve("static");
        Files.createDirectories(staticPath);
        return staticPath;
    }

    private static PageData<ArticleBasicDTO> previewPageData() {
        return new PageData<>(2L, Arrays.asList(
                previewArticle(1L, "hello-world", "Hello World"),
                previewArticle(2L, "template-preview", "Template Preview")
        ), 1L, 10L);
    }

    private static ArticleBasicDTO previewArticle(Long id, String alias, String title) {
        ArticleBasicDTO article = new ArticleBasicDTO();
        article.setLogId(id);
        article.setAlias(alias);
        article.setTitle(title);
        article.setDigest("This is a template-www preview article for visual validation.");
        article.setCanComment(true);
        article.setClick(12L);
        article.setCommentSize(1L);
        article.setReleaseTime("2026-06-02T10:00:00");
        article.setTypeAlias("default");
        article.setTypeName("Default");
        article.setTypeUrl("/sort/default");
        article.setUrl("/" + alias + ".html");
        article.setThumbnail("");
        return article;
    }

    private static TypeDTO previewType() {
        TypeDTO type = new TypeDTO();
        type.setId(1L);
        type.setAlias("default");
        type.setTypeName("Default");
        type.setTypeamount(2L);
        type.setUrl("/sort/default.html");
        return type;
    }

    private static PluginDTO systemPlugin(String pluginName) {
        PluginDTO plugin = new PluginDTO();
        plugin.setPluginName(pluginName);
        plugin.setSystem(true);
        return plugin;
    }

    private static List<TypeDTO> previewTypes() {
        return Collections.singletonList(previewType());
    }

    private static List<PluginDTO> previewPlugins() {
        return Collections.singletonList(systemPlugin("types"));
    }

    private static class TestStaticSitePlugin implements StaticSitePlugin {

        private final String contextPath;
        private final Map<String, HandleState> handleStatusPageMap = new HashMap<>();
        private final Lock parseLock = new ReentrantLock();
        private final List<File> cacheFiles = new ArrayList<>();

        TestStaticSitePlugin(String contextPath) {
            this.contextPath = contextPath;
        }

        @Override
        public String getVersionFileName() {
            return "version.txt";
        }

        @Override
        public String getDbCacheKey() {
            return "template.www.preview.version";
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getDefaultLang() {
            return Constants.DEFAULT_LANGUAGE;
        }

        @Override
        public Map<String, HandleState> getHandleStatusPageMap() {
            return handleStatusPageMap;
        }

        @Override
        public Lock getParseLock() {
            return parseLock;
        }

        @Override
        public Executor getExecutorService() {
            return Runnable::run;
        }

        @Override
        public List<File> getCacheFiles() {
            return cacheFiles;
        }

        @Override
        public StaticSiteType getType() {
            return StaticSiteType.BLOG;
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

    private static class TestZrLogConfig extends ZrLogConfig {

        private final boolean generatorHtmlStatus;
        private final StaticSitePlugin staticSitePlugin;

        TestZrLogConfig(boolean generatorHtmlStatus) {
            this(generatorHtmlStatus, null);
        }

        TestZrLogConfig(boolean generatorHtmlStatus, StaticSitePlugin staticSitePlugin) {
            super(18080, null, "");
            this.generatorHtmlStatus = generatorHtmlStatus;
            this.staticSitePlugin = staticSitePlugin;
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
        public CacheService getCacheService() {
            return new TestCacheService(generatorHtmlStatus);
        }

        @Override
        protected TokenService initTokenService() {
            return null;
        }

        @Override
        public List<IPlugin> getBasePluginList() {
            return new Plugins();
        }

        @Override
        public Plugins getAllPlugins() {
            Plugins plugins = new Plugins();
            if (staticSitePlugin != null) {
                plugins.add(staticSitePlugin);
            }
            return plugins;
        }
    }

    private static class TestCacheService implements CacheService {

        private final boolean generatorHtmlStatus;

        TestCacheService(boolean generatorHtmlStatus) {
            this.generatorHtmlStatus = generatorHtmlStatus;
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
        public PublicWebSiteInfo getPublicWebSiteInfo() {
            PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
            publicWebSiteInfo.setGenerator_html_status(generatorHtmlStatus);
            publicWebSiteInfo.setTemplate(Constants.getDefaultTemplatePath());
            publicWebSiteInfo.setLanguage(Constants.DEFAULT_LANGUAGE);
            publicWebSiteInfo.setTitle("ZrLog");
            publicWebSiteInfo.setSecond_title("Theme Preview");
            publicWebSiteInfo.setKeywords("zrlog,template,www");
            publicWebSiteInfo.setDescription("blog");
            publicWebSiteInfo.setStaticResourceHost("");
            publicWebSiteInfo.setHost("blog.example.com");
            publicWebSiteInfo.setIcp("");
            publicWebSiteInfo.setWebCm("");
            return publicWebSiteInfo;
        }

        @Override
        public BaseDataInitVO getInitData() {
            BaseDataInitVO initData = new BaseDataInitVO();
            initData.setWebSite(getPublicWebSiteInfo());
            initData.setTypes(previewTypes());
            initData.setPlugins(previewPlugins());
            return initData;
        }

        @Override
        public BaseDataInitVO refreshInitData() {
            return getInitData();
        }

        @Override
        public List<TypeDTO> getArticleTypes() {
            return previewTypes();
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

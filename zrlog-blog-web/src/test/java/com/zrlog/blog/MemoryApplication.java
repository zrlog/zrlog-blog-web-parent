package com.zrlog.blog;

import com.google.gson.Gson;
import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.dao.InMemoryDatabase;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.http.handle.CloseResponseHandle;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.WebServerBuilder;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.business.plugin.CacheManagerPlugin;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.business.version.UpgradeVersionHandler;
import com.zrlog.common.Constants;
import com.zrlog.common.Updater;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.install.business.service.InstallService;
import com.zrlog.install.business.vo.InstallConfigVO;
import com.zrlog.install.web.InstallConstants;
import com.zrlog.install.web.config.DefaultInstallConfig;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Local-only application for reviewing the installed default blog theme with deterministic in-memory data.
 */
public class MemoryApplication {

    private static final Logger LOGGER = LoggerUtil.getLogger(MemoryApplication.class);
    private static final int DEFAULT_PORT = 7080;
    private static final Gson GSON = new Gson();
    private static final String MEMORY_RUNTIME_DIR = ".zrlog-memory";
    private static final String MEMORY_INSTALL_CONFIG_FILE = "conf/memory-install.json";
    private static final String INSTALLED_DEFAULT_CONFIG_FILE = "conf/default-install-preview.json";
    private static final String MEMORY_CONTENT_FILE = "conf/memory-content.json";
    private static final String MEMORY_ASSET_DIR = "conf/memory-assets";

    static {
        Constants.init();
    }

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            throw new IllegalStateException("Start blog memory application failed", e);
        }
    }

    static void start(String[] args) throws Exception {
        System.getProperties().put("sws.run.mode", "dev");
        int port = resolvePort(args);
        boolean installedDefault = isInstalledDefaultMode(args);
        prepareRuntime(port, !installedDefault);
        WebServerBuilder build = new WebServerBuilder.Builder().config(Constants.zrLogConfig).build();
        LOGGER.info("Start ZrLog blog memory application at http://127.0.0.1:" + port
                + Constants.zrLogConfig.getServerConfig().getContextPath() + "/, root=" + PathUtil.getRootPath()
                + ", content=" + (installedDefault ? "installed-default" : "review-fixture"));
        build.start();
    }

    static DevZrLogConfig prepareRuntime(int port) throws Exception {
        return prepareRuntime(port, true);
    }

    static DevZrLogConfig prepareRuntime(int port, boolean seedReviewFixture) throws Exception {
        Path rootPath = memoryRootPath();
        resetMemoryRoot(rootPath);
        PathUtil.setRootPath(rootPath.toString());

        InstallConfigVO installConfig = readInstallConfig(seedReviewFixture
                ? MEMORY_INSTALL_CONFIG_FILE : INSTALLED_DEFAULT_CONFIG_FILE);
        applyRuntimeConfig(installConfig, port);
        InstallConstants.installConfig = new MemoryInstallConfig();
        installFromConfig(installConfig);

        DevZrLogConfig config = new MemoryZrLogConfig(port, null, installConfig.getContextPath());
        Constants.zrLogConfig = config;
        if (seedReviewFixture) {
            seedReviewContent((DataSourceWrapper) config.getDataSource(), readReviewContent());
            copyMemoryAssets(rootPath);
        }
        config.getCacheService().refreshInitData();
        return config;
    }

    static int resolvePort(String[] args) {
        if (args == null) {
            return DEFAULT_PORT;
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith("--port=")) {
                return Integer.parseInt(arg.substring("--port=".length()));
            }
        }
        return DEFAULT_PORT;
    }

    static boolean isInstalledDefaultMode(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if ("--installed-default".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Path memoryRootPath() {
        return projectRootPath().resolve(MEMORY_RUNTIME_DIR).toAbsolutePath().normalize();
    }

    static Path projectRootPath() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(current.resolve(MEMORY_INSTALL_CONFIG_FILE))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.exists(parent.resolve(MEMORY_INSTALL_CONFIG_FILE))) {
            return parent;
        }
        throw new IllegalStateException("Cannot locate project root from " + current);
    }

    private static void resetMemoryRoot(Path rootPath) throws Exception {
        if (!MEMORY_RUNTIME_DIR.equals(rootPath.getFileName().toString())) {
            throw new IllegalStateException("Refuse to reset unexpected memory root: " + rootPath);
        }
        if (!Files.exists(rootPath)) {
            return;
        }
        try (var stream = Files.walk(rootPath)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private static InstallConfigVO readInstallConfig(String configFile) throws Exception {
        Path configPath = projectRootPath().resolve(configFile);
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("Missing " + configPath);
        }
        InstallConfigVO installConfig = GSON.fromJson(Files.readString(configPath), InstallConfigVO.class);
        Properties properties = InMemoryDatabase.h2Properties(installConfig.getDbConfig().getDbName());
        installConfig.getDbConfig().setJdbcUrl(properties.getProperty("jdbcUrl"));
        installConfig.getDbConfig().setDriverClass(properties.getProperty("driverClass"));
        return installConfig;
    }

    private static ReviewContent readReviewContent() throws Exception {
        Path contentPath = projectRootPath().resolve(MEMORY_CONTENT_FILE);
        if (!Files.exists(contentPath)) {
            throw new IllegalStateException("Missing " + contentPath);
        }
        ReviewContent content = GSON.fromJson(Files.readString(contentPath), ReviewContent.class);
        if (content == null || content.articles == null || content.articles.isEmpty()) {
            throw new IllegalStateException("Memory review content must contain at least one article");
        }
        return content;
    }

    private static void applyRuntimeConfig(InstallConfigVO installConfig, int port) {
        Map<String, String> appendWebsite = installConfig.getAppendWebsite();
        if (appendWebsite == null) {
            appendWebsite = new LinkedHashMap<>();
            installConfig.setAppendWebsite(appendWebsite);
        }
        appendWebsite.put("host", "localhost:" + port);
    }

    private static void installFromConfig(InstallConfigVO config) {
        InstallService installService = new InstallService(InstallConstants.installConfig, config);
        if (!installService.install()) {
            throw new IllegalStateException("Install blog memory database failed");
        }
    }

    private static void seedReviewContent(DataSourceWrapper dataSource, ReviewContent content) throws SQLException {
        var runner = dataSource.getQueryRunner();
        runner.update("delete from comment");
        runner.update("delete from log");
        runner.update("delete from type");
        runner.update("delete from tag");
        runner.update("delete from link");
        runner.update("delete from lognav");
        runner.update("update user set header=? where userId=1", "/attached/avatar.svg");

        for (ReviewNavigation navigation : safe(content.navigation)) {
            runner.update("insert into lognav(navId, navName, url, sort, icon) values(?, ?, ?, ?, ?)",
                    navigation.id, navigation.name, navigation.url, navigation.sort, "");
        }
        for (ReviewType type : safe(content.types)) {
            runner.update("insert into type(typeId, typeName, remark, alias, pid, arrange_plugin) "
                            + "values(?, ?, ?, ?, ?, ?)",
                    type.id, type.name, type.remark, type.alias, 0, null);
        }
        for (ReviewTag tag : safe(content.tags)) {
            runner.update("insert into tag(tagId, text, count) values(?, ?, ?)", tag.id, tag.text, tag.count);
        }
        for (ReviewLink link : safe(content.links)) {
            runner.update("insert into link(linkId, linkName, url, alt, sort, status, icon) values(?, ?, ?, ?, ?, ?, ?)",
                    link.id, link.name, link.url, link.alt, link.sort, true, link.icon);
        }
        for (ReviewArticle article : content.articles) {
            runner.update("insert into log(logId, alias, canComment, click, version, content, plain_content, markdown, "
                            + "digest, keywords, thumbnail, recommended, releaseTime, last_update_date, title, typeId, "
                            + "userId, hot, rubbish, privacy, editor_type, arrange_plugin) values(?, ?, ?, ?, ?, ?, ?, "
                            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    article.id, article.alias, article.canComment, article.click, 0, article.content,
                    article.plainContent, article.markdown, article.digest, article.keywords, article.thumbnail, false,
                    article.releaseTime, article.releaseTime, article.title, article.typeId, 1, false, false, false,
                    "markdown", null);
        }
        for (ReviewComment comment : safe(content.comments)) {
            runner.update("insert into comment(commentId, commTime, hide, have_read, td, userComment, userHome, "
                            + "userIp, userMail, userName, logId, postId, header, user_agent, reply_id) values(?, ?, "
                            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    comment.id, comment.time, false, false, comment.time, comment.content, "", "127.0.0.1",
                    "reader@example.com", comment.userName, comment.articleId, "memory-" + comment.id, "",
                    "MemoryApplication", null);
        }
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? new ArrayList<>() : values;
    }

    private static void copyMemoryAssets(Path rootPath) throws Exception {
        Path sourceDir = projectRootPath().resolve(MEMORY_ASSET_DIR);
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalStateException("Missing " + sourceDir);
        }
        Path attachedDir = rootPath.resolve("static/attached");
        Files.createDirectories(attachedDir);
        try (var stream = Files.list(sourceDir)) {
            for (Path source : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
                Files.copy(source, attachedDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static class MemoryZrLogConfig extends DevZrLogConfig {

        private MemoryZrLogConfig(Integer port, Updater updater, String contextPath) {
            super(port, updater, contextPath == null ? "" : contextPath);
        }

        @Override
        public List<IPlugin> getBasePluginList() {
            Plugins basePlugins = new Plugins();
            basePlugins.add(new MemoryPluginCorePlugin());
            basePlugins.add(new CacheManagerPlugin(this));
            return basePlugins;
        }
    }

    private static class MemoryInstallConfig extends DefaultInstallConfig {

        @Override
        public String defaultTemplatePath() {
            return Constants.getDefaultTemplatePath();
        }

        @Override
        public String getZrLogSqlVersion() {
            return String.valueOf(UpgradeVersionHandler.SQL_VERSION);
        }

        @Override
        public File getDbPropertiesFile() {
            return PathUtil.getConfFile("db.properties");
        }
    }

    private static class MemoryPluginCorePlugin implements PluginCorePlugin {

        @Override
        public boolean refreshCache(String cacheVersion, HttpRequest request) {
            return false;
        }

        @Override
        public CloseResponseHandle getContext(String uri, HttpMethod method, HttpRequest request,
                                              AdminTokenVO adminTokenVO) {
            CloseResponseHandle handle = new CloseResponseHandle();
            handle.handle(null, memoryResponse(""));
            return handle;
        }

        @Override
        public <T> T requestService(HttpRequest inputRequest, Map<String, String[]> params,
                                    AdminTokenVO adminTokenVO, Class<T> clazz) {
            return null;
        }

        @Override
        public boolean accessPlugin(String uri, HttpRequest request, HttpResponse response,
                                    AdminTokenVO adminTokenVO) throws URISyntaxException {
            return false;
        }

        @Override
        public String getToken() {
            return "memory";
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean autoStart() {
            return false;
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public boolean stop() {
            return true;
        }

        private static java.net.http.HttpResponse<InputStream> memoryResponse(String body) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return new java.net.http.HttpResponse<>() {
                @Override
                public int statusCode() {
                    return 200;
                }

                @Override
                public java.net.http.HttpRequest request() {
                    return java.net.http.HttpRequest.newBuilder(URI.create("http://127.0.0.1/memory-plugin")).build();
                }

                @Override
                public Optional<java.net.http.HttpResponse<InputStream>> previousResponse() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    return HttpHeaders.of(Map.of("content-type", List.of("text/html;charset=utf-8")),
                            (name, value) -> true);
                }

                @Override
                public InputStream body() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public Optional<javax.net.ssl.SSLSession> sslSession() {
                    return Optional.empty();
                }

                @Override
                public URI uri() {
                    return URI.create("http://127.0.0.1/memory-plugin");
                }

                @Override
                public HttpClient.Version version() {
                    return HttpClient.Version.HTTP_1_1;
                }
            };
        }
    }

    private static class ReviewContent {
        private List<ReviewNavigation> navigation;
        private List<ReviewType> types;
        private List<ReviewTag> tags;
        private List<ReviewLink> links;
        private List<ReviewArticle> articles;
        private List<ReviewComment> comments;
    }

    private static class ReviewNavigation {
        private int id;
        private String name;
        private String url;
        private int sort;
    }

    private static class ReviewType {
        private int id;
        private String name;
        private String alias;
        private String remark;
    }

    private static class ReviewTag {
        private int id;
        private String text;
        private int count;
    }

    private static class ReviewLink {
        private int id;
        private String name;
        private String url;
        private String alt;
        private String icon;
        private int sort;
    }

    private static class ReviewArticle {
        private int id;
        private String alias;
        private String title;
        private String digest;
        private String plainContent;
        private String content;
        private String markdown;
        private String keywords;
        private String thumbnail;
        private int typeId;
        private String releaseTime;
        private boolean canComment;
        private int click;
    }

    private static class ReviewComment {
        private int id;
        private int articleId;
        private String userName;
        private String content;
        private String time;
    }
}

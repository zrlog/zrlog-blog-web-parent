package com.zrlog.blog;

import com.hibegin.common.dao.InMemoryDatabase;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.common.Constants;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.install.web.InstallConstants;
import com.zrlog.install.web.config.DefaultInstallConfig;
import com.zrlog.install.web.config.InstallConfig;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MemoryApplicationTest {

    @Test
    public void shouldPrepareCleanDefaultThemeReviewRuntime() throws Exception {
        String previousRootPath = PathUtil.getRootPath();
        InstallConfig previousInstallConfig = InstallConstants.installConfig;
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        Path projectRoot = MemoryApplication.projectRootPath();
        Path runtimeRoot = projectRoot.resolve(".zrlog-memory");
        DevZrLogConfig config = null;
        try {
            config = MemoryApplication.prepareRuntime(17081);

            assertEquals(runtimeRoot.toString(), PathUtil.getRootPath());
            assertTrue(Files.exists(runtimeRoot.resolve("conf/install.lock")));
            assertTrue(Files.exists(runtimeRoot.resolve("conf/db.properties")));
            assertTrue(Files.exists(runtimeRoot.resolve("static/attached/writing-cover.svg")));
            assertTrue(Files.exists(runtimeRoot.resolve("static/attached/code-cover.svg")));
            assertTrue(Files.exists(runtimeRoot.resolve("static/attached/inline-illustration.svg")));
            assertTrue(Files.exists(runtimeRoot.resolve("static/attached/link-icon.svg")));
            assertNotNull(config.getDataSource());

            Properties properties = loadDbProperties(runtimeRoot);
            assertTrue(properties.getProperty("jdbcUrl").contains("mem:zrlog_blog_memory"));
            try (InMemoryDatabase database = InMemoryDatabase.open(properties, false)) {
                assertEquals("localhost:17081", database.scalar(
                        "select value from website where name=?", "host"));
                assertEquals(Constants.getDefaultTemplatePath(), database.scalar(
                        "select value from website where name=?", "template"));
                assertEquals("zh_CN", database.scalar(
                        "select value from website where name=?", "language"));
                assertEquals("ZrLog", database.scalar(
                        "select value from website where name=?", "title"));
                assertEquals("简单、易用的 Java 博客系统", database.scalar(
                        "select value from website where name=?", "second_title"));
                assertEquals("简单、易用的 Java 博客系统", database.scalar(
                        "select value from website where name=?", "description"));
                assertEquals(5L, ((Number) database.scalar("select count(*) from log")).longValue());
                assertEquals(3L, ((Number) database.scalar("select count(*) from type")).longValue());
                assertEquals(6L, ((Number) database.scalar("select count(*) from tag")).longValue());
                assertEquals(3L, ((Number) database.scalar("select count(*) from link")).longValue());
                assertEquals("/record/2026_07", database.scalar(
                        "select url from lognav where navId=?", 2));
                assertEquals("把第一篇文章留给自己的站点", database.scalar(
                        "select title from log where alias=?", "writing-on-my-own-site"));
                assertEquals("", database.scalar(
                        "select thumbnail from log where alias=?", "notes-from-the-past"));
                assertEquals(1L, ((Number) database.scalar("select count(*) from comment")).longValue());
            }
            assertEquals(Constants.getDefaultTemplatePath(),
                    config.getCacheService().getPublicWebSiteInfo().getTemplate());
            assertEquals(2L, config.getCacheService().getPublicWebSiteInfo().getRows().longValue());
            assertFalse(Files.exists(projectRoot.resolve("conf/memory-install.generated.json")));
        } finally {
            if (config != null) {
                config.stop();
            }
            deleteTree(runtimeRoot);
            PathUtil.setRootPath(previousRootPath);
            InstallConstants.installConfig = previousInstallConfig == null
                    ? new DefaultInstallConfig() : previousInstallConfig;
            Constants.zrLogConfig = previousConfig;
        }
    }

    @Test
    public void shouldResolveDefaultAndConfiguredPort() {
        assertEquals(7080, MemoryApplication.resolvePort(null));
        assertEquals(7080, MemoryApplication.resolvePort(new String[]{"--debug"}));
        assertEquals(17081, MemoryApplication.resolvePort(new String[]{"--port=17081"}));
        assertFalse(MemoryApplication.isInstalledDefaultMode(null));
        assertFalse(MemoryApplication.isInstalledDefaultMode(new String[]{"--port=17081"}));
        assertTrue(MemoryApplication.isInstalledDefaultMode(new String[]{"--installed-default", "--port=17081"}));
    }

    @Test
    public void shouldKeepInstallerContentForInstalledDefaultMode() throws Exception {
        String previousRootPath = PathUtil.getRootPath();
        InstallConfig previousInstallConfig = InstallConstants.installConfig;
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        Path runtimeRoot = MemoryApplication.projectRootPath().resolve(".zrlog-memory");
        DevZrLogConfig config = null;
        try {
            config = MemoryApplication.prepareRuntime(17082, false);

            Properties properties = loadDbProperties(runtimeRoot);
            try (InMemoryDatabase database = InMemoryDatabase.open(properties, false)) {
                assertEquals("ZrLog", database.scalar("select value from website where name=?", "title"));
                assertEquals(1L, ((Number) database.scalar("select count(*) from log")).longValue());
                assertEquals(1L, ((Number) database.scalar("select count(*) from type")).longValue());
                assertEquals(1L, ((Number) database.scalar("select count(*) from tag")).longValue());
                assertEquals(0L, ((Number) database.scalar("select count(*) from link")).longValue());
                assertEquals(2L, ((Number) database.scalar("select count(*) from lognav")).longValue());
                assertEquals("hello-world", database.scalar("select alias from log where logId=1"));
            }
            assertFalse(Files.exists(runtimeRoot.resolve("static/attached/writing-cover.svg")));
            assertEquals(10L, config.getCacheService().getPublicWebSiteInfo().getRows().longValue());
        } finally {
            if (config != null) {
                config.stop();
            }
            deleteTree(runtimeRoot);
            PathUtil.setRootPath(previousRootPath);
            InstallConstants.installConfig = previousInstallConfig == null
                    ? new DefaultInstallConfig() : previousInstallConfig;
            Constants.zrLogConfig = previousConfig;
        }
    }

    private static Properties loadDbProperties(Path runtimeRoot) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(runtimeRoot.resolve("conf/db.properties"))) {
            properties.load(input);
        }
        return properties;
    }

    private static void deleteTree(Path rootPath) throws Exception {
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
}

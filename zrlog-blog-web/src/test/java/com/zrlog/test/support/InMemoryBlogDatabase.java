package com.zrlog.test.support;

import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InMemoryBlogDatabase implements AutoCloseable {

    private final DataSourceWrapper dataSource;
    private final ZrLogTestDatabase database;
    private final ZrLogConfig previousConfig;
    private final TestCacheService cacheService;

    private InMemoryBlogDatabase() throws Exception {
        this.previousConfig = Constants.zrLogConfig;
        this.cacheService = new TestCacheService();
        this.database = ZrLogTestDatabase.open();
        this.dataSource = database.dataSource();
        Constants.zrLogConfig = new TestZrLogConfig(cacheService);
        seedBaseData();
    }

    public static InMemoryBlogDatabase open() throws Exception {
        return new InMemoryBlogDatabase();
    }

    public TestCacheService cacheService() {
        return cacheService;
    }

    public int update(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().update(sql, params);
    }

    public Object scalar(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new ScalarHandler<>(1), params);
    }

    public Map<String, Object> queryOne(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new MapHandler(), params);
    }

    public List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new MapListHandler(), params);
    }

    public void insertArticle(long logId, String alias, String title, String plainContent, String keywords,
                              String releaseTime, boolean rubbish, boolean privacy) throws SQLException {
        update("insert into log (logId, alias, canComment, click, version, content, plain_content, markdown, digest, "
                        + "keywords, thumbnail, recommended, releaseTime, last_update_date, title, typeId, userId, "
                        + "hot, rubbish, privacy, editor_type) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?)",
                logId, alias, true, logId * 10, 0, "<h2>" + title + "</h2><p>" + plainContent + "</p>", plainContent,
                "## " + title, plainContent, keywords, "/attached/" + alias + ".png", false, releaseTime, releaseTime,
                title, 1, 1, false, rubbish, privacy, "markdown");
    }

    public void insertComment(long commentId, long logId, String userComment) throws SQLException {
        update("insert into comment (commentId, commTime, hide, have_read, td, userComment, userHome, userIp, "
                        + "userMail, userName, logId, postId, header, user_agent, reply_id) values (?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?, ?, ?, ?, ?)",
                commentId, "2026-06-03 12:00:00", false, false, "2026-06-03 12:00:00", userComment,
                "https://reader.example.com", "127.0.0.1", "reader@example.com", "reader", logId,
                "post-" + commentId, "", "JUnit", null);
    }

    private void seedBaseData() throws SQLException {
        update("insert into user (userId, email, password, userName, header, secretKey, mfaEnabled, mfaSecret) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                1, "admin@example.com", "pwd", "admin", "/attached/admin.png", "secret", false, null);
        update("insert into type (typeId, alias, remark, typeName, pid, arrange_plugin) values (?, ?, ?, ?, ?, ?)",
                1, "default", "Default category", "Default", 0, null);
    }

    @Override
    public void close() throws Exception {
        try {
            database.close();
        } finally {
            Constants.zrLogConfig = previousConfig;
        }
    }

    private static class TestZrLogConfig extends ZrLogConfig {

        private final TestCacheService cacheService;

        TestZrLogConfig(TestCacheService cacheService) {
            super(18080, null, "");
            this.cacheService = cacheService;
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
            return cacheService;
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

    public static class TestCacheService implements CacheService {

        private final BaseDataInitVO initData = new BaseDataInitVO();
        private final PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
        private final List<TypeDTO> articleTypes = new ArrayList<>();

        TestCacheService() {
            publicWebSiteInfo.setRows(2L);
            publicWebSiteInfo.setHost("blog.example.com");
            publicWebSiteInfo.setGenerator_html_status(false);
            publicWebSiteInfo.setDisable_comment_status(false);
            publicWebSiteInfo.setArticle_thumbnail_status(true);
            publicWebSiteInfo.setArticle_auto_digest_length(120L);
            publicWebSiteInfo.setLanguage(Constants.DEFAULT_LANGUAGE);
            publicWebSiteInfo.setTemplate("default");
            initData.setWebSite(publicWebSiteInfo);
            TypeDTO typeDTO = new TypeDTO();
            typeDTO.setId(1L);
            typeDTO.setAlias("default");
            typeDTO.setTypeName("Default");
            articleTypes.add(typeDTO);
            initData.setTypes(articleTypes);
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
            return initData;
        }

        @Override
        public BaseDataInitVO refreshInitData() {
            return initData;
        }

        @Override
        public PublicWebSiteInfo getPublicWebSiteInfo() {
            return publicWebSiteInfo;
        }

        @Override
        public List<TypeDTO> getArticleTypes() {
            return articleTypes;
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

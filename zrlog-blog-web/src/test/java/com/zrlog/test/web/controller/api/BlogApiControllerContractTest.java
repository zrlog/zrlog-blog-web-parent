package com.zrlog.test.web.controller.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.hibegin.common.dao.DAO;
import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.business.service.ArticleService.ArticleListOrder;
import com.zrlog.blog.web.controller.api.BlogApiArticleController;
import com.zrlog.blog.web.controller.api.BlogApiCacheController;
import com.zrlog.blog.web.controller.api.BlogApiPublicController;
import com.zrlog.business.service.CommonService;
import com.zrlog.common.CacheService;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.I18nVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.data.dto.VisitorCommentDTO;
import com.zrlog.model.Comment;
import com.zrlog.model.Log;
import com.zrlog.util.I18nUtil;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlogApiControllerContractTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldReturnCacheInitDataFromConfiguredCacheService() throws Exception {
        BaseDataInitVO initData = new BaseDataInitVO();
        initData.setVersion(99L);
        FakeCacheService cacheService = new FakeCacheService(initData);
        withConfig(cacheService, () -> {
            ApiStandardResponse<BaseDataInitVO> response = new BlogApiCacheController(cacheService).index();

            assertSame(initData, response.getData());
        });
    }

    @Test
    public void shouldReturnEmptyBlogResourceWhenI18nContextIsMissing() {
        I18nUtil.threadLocal.remove();

        ApiStandardResponse<Map<String, Object>> response = new BlogApiPublicController(new CommonService(() -> new PublicWebSiteInfo())).blogResource();

        assertTrue(response.getData().isEmpty());
    }

    @Test
    public void shouldReturnBlogResourceWithPublicSiteInfoWhenI18nContextExists() throws Exception {
        BaseDataInitVO initData = new BaseDataInitVO();
        PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
        publicWebSiteInfo.setTitle("ZrLog");
        publicWebSiteInfo.setAdmin_darkMode(true);
        publicWebSiteInfo.setAdmin_color_primary("#1677ff");
        publicWebSiteInfo.setAppId("app-id");
        I18nVO i18nVO = new I18nVO();
        Map<String, Object> blog = new HashMap<>();
        blog.put("comment", "Comment");
        i18nVO.setLocale("zh_CN");
        i18nVO.getBlog().put("zh_CN", blog);
        I18nUtil.threadLocal.set(i18nVO);
        try {
            withConfig(new FakeCacheService(initData, publicWebSiteInfo), () -> {
                BlogApiPublicController controller = new BlogApiPublicController(new CommonService(() -> publicWebSiteInfo));
                setControllerRequest(controller, request("/blog", "blog.example.com"));

                ApiStandardResponse<Map<String, Object>> response = controller.blogResource();

                assertEquals(Map.of("comment", "Comment"), blog);
                assertEquals("Comment", response.getData().get("comment"));
                assertEquals("ZrLog", response.getData().get("websiteTitle"));
                assertEquals("//blog.example.com/blog/", response.getData().get("homeUrl"));
                assertEquals("", response.getData().get("articleRoute"));
                assertEquals(true, response.getData().get("admin_darkMode"));
                assertTrue(response.getData().containsKey("buildId"));
            });
        } finally {
            I18nUtil.threadLocal.remove();
        }
    }

    @Test
    public void shouldReturnArticleDetailFromArticleService() throws Exception {
        FakeArticleService articleService = new FakeArticleService();
        BlogApiArticleController controller =
                new BlogApiArticleController(articleService, Comment::new);
        ArticleDetailDTO detail = new ArticleDetailDTO();
        detail.setLogId(7L);
        articleService.detail = detail;
        HttpRequest request = request(Map.of("id", "hello-world"));
        setControllerRequest(controller, request);

        ApiStandardResponse<ArticleDetailDTO> response = controller.detail();

        assertSame(detail, response.getData());
        assertEquals("hello-world", articleService.lastDetailId);
        assertSame(request, articleService.lastDetailRequest);
    }

    @Test
    public void shouldReturnArticlePageDataFromArticleService() throws Exception {
        FakeArticleService articleService = new FakeArticleService();
        BlogApiArticleController controller =
                new BlogApiArticleController(articleService, Comment::new);
        ArticleBasicDTO row = new ArticleBasicDTO();
        row.setLogId(8L);
        articleService.pageData = new PageData<>(1L, List.of(row), 2L, 5L);
        HttpRequest request = request(Map.of("key", "zrlog", "page", "2", "size", "5", "sort", "logId,asc"));
        setControllerRequest(controller, request);

        ApiStandardResponse<PageData<ArticleBasicDTO>> response = controller.index();

        assertSame(articleService.pageData, response.getData());
        assertEquals("zrlog", articleService.lastKeywords);
        assertSame(request, articleService.lastPageRequestHttpRequest);
        assertEquals(Long.valueOf(2L), articleService.lastPageRequest.getPage());
        assertEquals(Long.valueOf(5L), articleService.lastPageRequest.getSize());
        assertEquals(ArticleListOrder.STICKY_FIRST, articleService.lastListOrder);
    }

    @Test
    public void shouldRequestNewestFirstArticlePageForFeed() throws Exception {
        FakeArticleService articleService = new FakeArticleService();
        BlogApiArticleController controller =
                new BlogApiArticleController(articleService, Comment::new);
        setControllerRequest(controller, request(Map.of("feed", "true")));

        controller.index();

        assertEquals(ArticleListOrder.NEWEST_FIRST, articleService.lastListOrder);
    }

    @Test
    public void shouldReturnVisitorCommentsFromDao() throws Exception {
        BlogApiArticleController controller =
                new BlogApiArticleController(new FakeArticleService(), Comment::new);
        FakeCommentQueryRunner queryRunner = new FakeCommentQueryRunner();
        DataSourceWrapper previousDataSource = setDefaultDataSource(dataSource(queryRunner));
        try {
            setControllerRequest(controller, request(Map.of("id", "7")));

            ApiStandardResponse<List<VisitorCommentDTO>> response = controller.comment();

            assertEquals(1, response.getData().size());
            assertEquals(Long.valueOf(5L), response.getData().get(0).getId());
            assertEquals("hello", response.getData().get(0).getUserComment());
            assertEquals("", response.getData().get(0).getGravatarId());
            assertEquals("select * from comment where logId=?", queryRunner.sql);
            assertEquals(7, queryRunner.params[0]);
        } finally {
            restoreDefaultDataSource(previousDataSource);
        }
    }

    private void withConfig(CacheService cacheService, ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder("zrlog-blog-api").getAbsolutePath());
            runnable.run();
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

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

    private static HttpRequest request(String contextPath, String host) {
        return request(contextPath, host, Collections.emptyMap());
    }

    private static HttpRequest request(Map<String, String> params) {
        return request("/blog", "blog.example.com", params);
    }

    private static HttpRequest request(String contextPath, String host, Map<String, String> params) {
        Map<String, String[]> paramMap = new HashMap<>();
        params.forEach((key, value) -> paramMap.put(key, new String[]{value}));
        return (HttpRequest) Proxy.newProxyInstance(
                BlogApiControllerContractTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getContextPath".equals(method.getName())) {
                        return contextPath;
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return host;
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

    private static void setArticleService(BlogApiArticleController controller, ArticleService articleService)
            throws Exception {
        Field field = BlogApiArticleController.class.getDeclaredField("articleService");
        field.setAccessible(true);
        field.set(controller, articleService);
    }

    private static DataSourceWrapper setDefaultDataSource(DataSourceWrapper dataSource) {
        DataSourceWrapper previous = DAO.getDefaultDataSource();
        DAO.setDs(dataSource);
        return previous;
    }

    private static void restoreDefaultDataSource(DataSourceWrapper previousDataSource) {
        DAO.setDs(previousDataSource);
    }

    private static DataSourceWrapper dataSource(QueryRunner queryRunner) {
        return (DataSourceWrapper) Proxy.newProxyInstance(
                BlogApiControllerContractTest.class.getClassLoader(),
                new Class[]{DataSourceWrapper.class},
                (proxy, method, args) -> {
                    if ("getQueryRunner".equals(method.getName())) {
                        return queryRunner;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DataSourceWrapperProxy";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class FakeCacheService implements CacheService {
        private final BaseDataInitVO initData;
        private final PublicWebSiteInfo publicWebSiteInfo;

        FakeCacheService(BaseDataInitVO initData) {
            this(initData, new PublicWebSiteInfo());
        }

        FakeCacheService(BaseDataInitVO initData, PublicWebSiteInfo publicWebSiteInfo) {
            this.initData = initData;
            this.publicWebSiteInfo = publicWebSiteInfo;
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
        private FakeArticleService() {
            super(org.mockito.Mockito.mock(CacheService.class), Log::new);
        }

        private ArticleDetailDTO detail;
        private PageData<ArticleBasicDTO> pageData = new PageData<>(0L, new ArrayList<>());
        private Object lastDetailId;
        private HttpRequest lastDetailRequest;
        private PageRequest lastPageRequest;
        private String lastKeywords;
        private HttpRequest lastPageRequestHttpRequest;
        private ArticleListOrder lastListOrder;

        @Override
        public ArticleDetailDTO detail(Object idOrAlias, HttpRequest request) throws SQLException {
            lastDetailId = idOrAlias;
            lastDetailRequest = request;
            return detail;
        }

        @Override
        public PageData<ArticleBasicDTO> pageByKeywords(PageRequest pageRequest, String keywords,
                                                        HttpRequest request, ArticleListOrder listOrder) {
            lastPageRequest = pageRequest;
            lastKeywords = keywords;
            lastPageRequestHttpRequest = request;
            lastListOrder = listOrder;
            return pageData;
        }
    }

    private static class FakeCommentQueryRunner extends QueryRunner {
        private String sql;
        private Object[] params = new Object[0];

        @Override
        @SuppressWarnings("unchecked")
        public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params) {
            this.sql = sql;
            this.params = params;
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", 5L);
            comment.put("userComment", "hello");
            comment.put("header", "");
            comment.put("commTime", "2026-06-29 12:13:14");
            comment.put("userHome", "https://example.com");
            comment.put("userName", "reader");
            comment.put("userMail", null);
            return (T) List.of(comment);
        }
    }
}

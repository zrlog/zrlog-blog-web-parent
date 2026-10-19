package com.zrlog.test.business.service;

import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.common.CacheService;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSiteInfoProvider;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArticleServiceTest {

    private com.zrlog.common.ZrLogConfig applicationConfig;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldNormalizeArticleForBlogRendering() throws Exception {
        PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
        publicWebSiteInfo.setDisable_comment_status(false);
        publicWebSiteInfo.setGenerator_html_status(false);
        publicWebSiteInfo.setArticle_thumbnail_status(true);
        ArticleBasicDTO article = article();

        withConfig(publicWebSiteInfo, cache -> {
            ArticleBasicDTO result = ArticleService.handlerArticle(article, request("/blog", "blog.example.com"), cache);

            assertEquals(Long.valueOf(10L), result.getId());
            assertEquals("hello-world", result.getAlias());
            assertEquals("/blog/hello-world", result.getUrl());
            assertEquals("/blog/sort/default", result.getTypeUrl());
            assertEquals("//blog.example.com/blog/hello-world", result.getNoSchemeUrl());
            assertEquals("//blog.example.com/blog/addComment", result.getCommentUrl());
            assertEquals(Boolean.TRUE, result.getCanComment());
            assertEquals(Boolean.TRUE, result.getRubbish());
            assertEquals(Boolean.FALSE, result.getPrivacy());
            assertEquals(Boolean.TRUE, result.getHot());
            assertEquals(Boolean.FALSE, result.getRecommended());
            assertEquals("2026-06-29", result.getReleaseTime());
            assertTrue(result.getFullReleaseTime().startsWith("2026-06-29 12:13:14"));
            assertEquals("2026-06-29", result.getLastUpdateDate());
            assertEquals("2026-06-29", result.getLast_update_date());
            assertEquals("", result.getDigest());
            assertEquals("", result.getContent());
            assertEquals("/attached/cover.png", result.getThumbnail());
            assertEquals("Hello", result.getThumbnailAlt());
            assertEquals(2, result.getTags().size());
            assertEquals("java", result.getTags().get(0).getName());
            assertEquals("/blog/tag/java", result.getTags().get(0).getUrl());
            assertEquals("zrlog", result.getTags().get(1).getName());
        });
    }

    @Test
    public void shouldDisableCommentAndClearThumbnailWhenWebsiteConfigDisablesThem() throws Exception {
        PublicWebSiteInfo publicWebSiteInfo = new PublicWebSiteInfo();
        publicWebSiteInfo.setDisable_comment_status(true);
        publicWebSiteInfo.setGenerator_html_status(false);
        publicWebSiteInfo.setArticle_thumbnail_status(false);
        ArticleBasicDTO article = article();

        withConfig(publicWebSiteInfo, cache -> {
            ArticleBasicDTO result = ArticleService.handlerArticle(article, request("/", "blog.example.com"), cache);

            assertEquals(Boolean.FALSE, result.getCanComment());
            assertNull(result.getThumbnail());
            assertNull(result.getThumbnailAlt());
        });
    }

    private static ArticleBasicDTO article() {
        ArticleBasicDTO article = new ArticleBasicDTO();
        article.setLogId(10L);
        article.setAlias("hello-world");
        article.setTypeAlias("default");
        article.setTitle("<b>Hello</b>");
        article.setCanComment(true);
        article.setRubbish(true);
        article.setPrivacy(false);
        article.setHot(true);
        article.setRecommended(false);
        article.setReleaseTime("2026-06-29 12:13:14");
        article.setLastUpdateDate("2026-06-29 13:14:15");
        article.setLast_update_date("2026-06-29 13:14:15");
        article.setDigest(null);
        article.setContent(null);
        article.setThumbnail("/attached/cover.png");
        article.setKeywords("java,zrlog,,");
        return article;
    }

    private void withConfig(PublicWebSiteInfo publicWebSiteInfo, ThrowingRunnable runnable) throws Exception {
        String previousRootPath = System.getProperty("sws.root.path");
        try {
            System.setProperty("sws.root.path", temporaryFolder.newFolder().getAbsolutePath());
            applicationConfig = new ArticleZrLogConfig(publicWebSiteInfo);
            runnable.run(((ArticleZrLogConfig) applicationConfig).cacheForTest());
        } finally {
            restoreProperty("sws.root.path", previousRootPath);
        }
    }

    private static HttpRequest request(String contextPath, String host) {
        return (HttpRequest) Proxy.newProxyInstance(
                ArticleServiceTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getContextPath".equals(method.getName())) {
                        return contextPath;
                    }
                    if ("getHeader".equals(method.getName()) && "Host".equals(args[0])) {
                        return host;
                    }
                    if ("getHeaderMap".equals(method.getName())) {
                        return Collections.singletonMap("Host", host);
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private interface ThrowingRunnable {
        void run(CacheService cache) throws Exception;
    }

    private static class ArticleZrLogConfig extends ZrLogConfig {
        private final PublicWebSiteInfo publicWebSiteInfo;

        ArticleZrLogConfig(PublicWebSiteInfo publicWebSiteInfo) {
            super(18080, null, "");
            this.publicWebSiteInfo = publicWebSiteInfo;
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
        public <T> T getWebSetup(Class<T> capability) {
            if (capability == WebSiteInfoProvider.class) {
                CacheService cache = cacheForTest();
                return cache == null ? null : capability.cast(
                        (WebSiteInfoProvider) cache::getPublicWebSiteInfo);
            }
            return super.getWebSetup(capability);
        }

        private CacheService cacheForTest() {
            return new ArticleCacheService(publicWebSiteInfo);
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

    private static class ArticleCacheService implements CacheService {
        private final PublicWebSiteInfo publicWebSiteInfo;

        ArticleCacheService(PublicWebSiteInfo publicWebSiteInfo) {
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
            return new BaseDataInitVO();
        }

        @Override
        public BaseDataInitVO refreshInitData() {
            return new BaseDataInitVO();
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
}

package com.zrlog.test.web.config;

import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.web.config.ZrLogRequestRecordListener;
import com.zrlog.business.plugin.ArticleStatisticsPlugin;
import com.zrlog.business.plugin.RequestInfo;
import com.zrlog.data.dto.ArticleDetailDTO;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ZrLogRequestRecordListenerTest {

    @Test
    public void shouldRecordHandledArticlePage() throws Exception {
        FakeArticleStatisticsPlugin plugin = new FakeArticleStatisticsPlugin();
        ArticleDetailDTO article = new ArticleDetailDTO();
        article.setLogId(99L);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("log", article);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("X-Real-IP", "203.0.113.9");
        long createTime = System.currentTimeMillis() - 25L;

        withConfig(true, plugin, listener -> listener.onHandled(request(HttpMethod.GET, "/hello-world", attrs, headers, createTime), null));

        assertNotNull(plugin.lastRequestInfo);
        assertEquals(Long.valueOf(99L), plugin.lastRequestInfo.getArticleId());
        assertEquals("/hello-world", plugin.lastRequestInfo.getRequestUri());
        assertEquals("203.0.113.9", plugin.lastRequestInfo.getIp());
        assertEquals("/blog/", plugin.lastRequestInfo.getUrl());
        assertEquals("Mozilla/5.0", plugin.lastRequestInfo.getUserAgent());
        assertTrue(plugin.lastRequestInfo.getUsedTime() >= 0);
    }

    @Test
    public void shouldSkipNonArticleRequests() throws Exception {
        FakeArticleStatisticsPlugin plugin = new FakeArticleStatisticsPlugin();
        ArticleDetailDTO article = new ArticleDetailDTO();
        article.setLogId(99L);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("log", article);

        withConfig(false, plugin, listener -> listener.onHandled(request(HttpMethod.GET, "/hello-world", attrs, Map.of(), System.currentTimeMillis()), null));
        assertNull(plugin.lastRequestInfo);

        withConfig(true, plugin, listener -> listener.onHandled(request(HttpMethod.POST, "/hello-world", attrs, Map.of(), System.currentTimeMillis()), null));
        assertNull(plugin.lastRequestInfo);

        withConfig(true, plugin, listener -> listener.onHandled(request(HttpMethod.GET, "/assets/app.js", attrs, Map.of(), System.currentTimeMillis()), null));
        assertNull(plugin.lastRequestInfo);
    }

    @Test
    public void shouldFollowInstallationAndStatisticsReplacementForTheSameListener() {
        var installed = new java.util.concurrent.atomic.AtomicBoolean();
        var statistics = new java.util.concurrent.atomic.AtomicReference<ArticleStatisticsPlugin>();
        ZrLogRequestRecordListener listener = new ZrLogRequestRecordListener(installed::get, statistics::get);
        ArticleDetailDTO article = new ArticleDetailDTO();
        article.setLogId(42L);
        HttpRequest request = request(HttpMethod.GET, "/article", Map.of("log", article), Map.of(), System.currentTimeMillis());
        FakeArticleStatisticsPlugin first = new FakeArticleStatisticsPlugin();
        statistics.set(first);
        listener.onHandled(request, null);
        assertNull(first.lastRequestInfo);
        installed.set(true);
        listener.onHandled(request, null);
        assertEquals(Long.valueOf(42L), first.lastRequestInfo.getArticleId());
        FakeArticleStatisticsPlugin replacement = new FakeArticleStatisticsPlugin();
        statistics.set(replacement);
        article.setLogId(43L);
        listener.onHandled(request, null);
        assertEquals(Long.valueOf(42L), first.lastRequestInfo.getArticleId());
        assertEquals(Long.valueOf(43L), replacement.lastRequestInfo.getArticleId());
        statistics.set(null);
        listener.onHandled(request, null);
        assertEquals(Long.valueOf(43L), replacement.lastRequestInfo.getArticleId());
    }

    private void withConfig(boolean installed, FakeArticleStatisticsPlugin plugin,
                            java.util.function.Consumer<ZrLogRequestRecordListener> action) {
        action.accept(new ZrLogRequestRecordListener(() -> installed, () -> plugin));
    }

    private static HttpRequest request(HttpMethod method, String uri, Map<String, Object> attrs,
                                       Map<String, String> headers, long createTime) {
        return (HttpRequest) Proxy.newProxyInstance(
                ZrLogRequestRecordListenerTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, invokedMethod, args) -> {
                    if ("getMethod".equals(invokedMethod.getName())) {
                        return method;
                    }
                    if ("getUri".equals(invokedMethod.getName())) {
                        return uri;
                    }
                    if ("getAttr".equals(invokedMethod.getName())) {
                        return attrs;
                    }
                    if ("getCreateTime".equals(invokedMethod.getName())) {
                        return createTime;
                    }
                    if ("getContextPath".equals(invokedMethod.getName())) {
                        return "/blog";
                    }
                    if ("getHeader".equals(invokedMethod.getName())) {
                        return headers.get(args[0].toString());
                    }
                    if ("getHeaderMap".equals(invokedMethod.getName())) {
                        return headers;
                    }
                    if ("getRemoteHost".equals(invokedMethod.getName())) {
                        return "127.0.0.1";
                    }
                    if ("toString".equals(invokedMethod.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }

    private static class FakeArticleStatisticsPlugin implements ArticleStatisticsPlugin {
        private RequestInfo lastRequestInfo;

        @Override
        public void record(RequestInfo requestInfo) {
            lastRequestInfo = requestInfo;
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
}

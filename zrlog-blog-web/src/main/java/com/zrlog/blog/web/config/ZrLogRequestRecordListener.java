package com.zrlog.blog.web.config;

import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpRequestListener;
import com.hibegin.http.server.api.HttpResponse;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.ArticleStatisticsPlugin;
import com.zrlog.business.plugin.RequestInfo;
import com.zrlog.data.dto.ArticleDetailDTO;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ZrLogRequestRecordListener implements HttpRequestListener {

    private final BooleanSupplier installed;
    private final Supplier<ArticleStatisticsPlugin> statistics;

    public ZrLogRequestRecordListener(BooleanSupplier installed, Supplier<ArticleStatisticsPlugin> statistics) {
        this.installed = Objects.requireNonNull(installed);
        this.statistics = Objects.requireNonNull(statistics);
    }

    /**
     * 判断请求是否为文章详情页
     *
     * @param request
     * @return
     */
    private boolean isArticlePage(HttpRequest request) {
        if (!installed.getAsBoolean()) {
            return false;
        }
        if (request.getMethod() != HttpMethod.GET) {
            return false;
        }
        String actionKey = request.getUri();
        //仅保留非静态资源请求或者是以 .html结尾的
        if (actionKey.contains(".") && !actionKey.endsWith(".html")) {
            return false;
        }
        return Objects.nonNull(request.getAttr().get("log"));
    }


    @Override
    public void onHandled(HttpRequest request, HttpResponse httpResponse) {
        long used = System.currentTimeMillis() - request.getCreateTime();
        if (isArticlePage(request)) {
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setIp(WebTools.getRealIp(request));
            requestInfo.setUrl(WebTools.getHomeUrl(request));
            requestInfo.setUserAgent(request.getHeader("User-Agent"));
            requestInfo.setRequestTime(request.getCreateTime());
            requestInfo.setRequestUri(request.getUri());
            ArticleDetailDTO log = (ArticleDetailDTO) request.getAttr().get("log");
            requestInfo.setArticleId(log.getLogId());
            requestInfo.setUsedTime(used);
            ArticleStatisticsPlugin plugin = statistics.get();
            if (Objects.nonNull(plugin)) {
                plugin.record(requestInfo);
            }
        }
    }
}

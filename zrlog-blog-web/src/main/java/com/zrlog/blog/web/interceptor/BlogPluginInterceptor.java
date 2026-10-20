package com.zrlog.blog.web.interceptor;

import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.common.TokenService;
import com.zrlog.common.vo.AdminTokenVO;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 目前插件服务拦截了
 * 1. /plugin/* /p/* （是plugin的短路由，代码逻辑上是不区分的，不检查权限）
 * 如果想了解更多关于插件的实现可以浏览这篇文章 <a href="https://blog.zrlog.com/zrlog-plugin-dev.html">插件实现</a>
 */
public class BlogPluginInterceptor implements HandleAbleInterceptor {
    private static final String userPluginUriPath = "/plugin/";
    private static final String userPluginUriPathShort = "/p/";
    private static final String userPluginUriApiPathShort = "/api/p/";
    private static final String userPluginUriApiPath = "/api/plugin/";
    private final Supplier<TokenService> tokenService;
    private final Supplier<PluginCorePlugin> pluginCore;
    private final List<String> pluginHandlerPaths = Arrays.asList(userPluginUriApiPathShort, userPluginUriApiPath, userPluginUriPath, userPluginUriPathShort);
    public BlogPluginInterceptor(Supplier<TokenService> tokenService, Supplier<PluginCorePlugin> pluginCore) {
        this.tokenService = Objects.requireNonNull(tokenService);
        this.pluginCore = Objects.requireNonNull(pluginCore);
    }

    @Override
    public boolean isHandleAble(HttpRequest request) {
        String actionKey = request.getUri();
        return pluginHandlerPaths.stream().anyMatch(actionKey::startsWith);
    }

    /**
     * 不检查是否登录，错误请求直接直接转化为403
     *
     * @param target
     * @param request
     * @param response
     * @param key
     * @throws IOException
     */
    private void visitorPermission(String target, HttpRequest request, HttpResponse response, AdminTokenVO key) throws IOException, URISyntaxException, InterruptedException {
        String realUri = target;
        for (String prefixUri : pluginHandlerPaths) {
            realUri = realUri.replaceFirst(prefixUri, "/");
        }
        if (pluginCore.get().accessPlugin(realUri, request, response, key)) {
            return;
        }
        response.renderCode(404);
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) throws IOException, URISyntaxException, InterruptedException {
        String target = request.getUri();
        TokenService tokenService = this.tokenService.get();
        if (Objects.isNull(tokenService)) {
            visitorPermission(target, request, response, null);
        } else {
            visitorPermission(target, request, response, tokenService.getAdminTokenVO(request));
        }
        return false;
    }
}

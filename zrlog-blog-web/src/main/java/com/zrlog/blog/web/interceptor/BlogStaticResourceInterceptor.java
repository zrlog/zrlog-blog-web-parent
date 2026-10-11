package com.zrlog.blog.web.interceptor;

import com.hibegin.common.util.FileUtils;
import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.util.MimeTypeUtil;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.business.template.util.BlogResourceUtils;
import com.zrlog.common.Constants;
import com.zrlog.common.exception.UnknownException;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.StaticFileCacheUtils;
import com.zrlog.util.ZrLogUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 静态文件读取优先级
 * 1. /static
 * 2. classpath:/
 * 3. /cache
 */
public class BlogStaticResourceInterceptor implements HandleAbleInterceptor {


    public BlogStaticResourceInterceptor() {
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) {
        File staticFile = PathUtil.getStaticFile(request.getUri());
        //静态文件进行拦截
        if (staticFile.isFile() && staticFile.exists()) {
            //缓存静态资源文件
            if (StaticFileCacheUtils.getInstance().isCacheableByRequest(request.getUri())) {
                ZrLogUtil.putLongTimeCache(response);
            }
            response.writeFile(staticFile);
            return false;
        }
        //静态文件
        if (BlogResourceUtils.getInstance().existsResource(request.getUri().substring(1))) {
            try (InputStream resourceAsStream = BlogStaticResourceInterceptor.class.getResourceAsStream(request.getUri())) {
                if (Objects.nonNull(resourceAsStream)) {
                    ZrLogUtil.putLongTimeCache(response);
                    response.addHeader("Content-Type", MimeTypeUtil.getMimeStrByExt(FileUtils.getFileExt(request.getUri())));
                    response.write(resourceAsStream, 200);
                    return false;
                }
            } catch (IOException ex) {
                throw new UnknownException(ex);
            }
        }
        BlogPageStaticSitePlugin staticSitePlugin = Constants.zrLogConfig.getPlugin(BlogPageStaticSitePlugin.class);
        if (Objects.nonNull(staticSitePlugin)) {
            File cacheFile = staticSitePlugin.loadCacheFile(request);
            if (cacheFile.isFile() && cacheFile.exists()) {
                response.writeFile(cacheFile);
                return false;
            }
        }
        return true;
    }

    /**
     * staticPlugin，自己控制缓存文件的读取和生成方式
     *
     * @param request
     * @return
     */
    @Override
    public boolean isHandleAble(HttpRequest request) {
        // The exported error document is not a successful cached page.
        if (request.getUri().equals("/error/404.html")) {
            return false;
        }
        if (request.getUri().startsWith("/admin")) {
            return false;
        }
        return !BaseStaticSitePlugin.isStaticPluginRequest(request);
    }
}

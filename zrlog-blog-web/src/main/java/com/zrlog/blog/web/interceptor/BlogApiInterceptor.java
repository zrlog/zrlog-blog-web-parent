package com.zrlog.blog.web.interceptor;

import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.web.MethodInterceptor;
import com.zrlog.business.exception.MissingInstallException;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public class BlogApiInterceptor implements HandleAbleInterceptor {
    private final BooleanSupplier installed;

    public BlogApiInterceptor(BooleanSupplier installed) {
        this.installed = Objects.requireNonNull(installed);
    }

    @Override
    public boolean isHandleAble(HttpRequest request) {
        return Objects.equals("/api", request.getUri()) || request.getUri().startsWith("/api/");
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) throws Exception {
        if (!installed.getAsBoolean()) {
            throw new MissingInstallException();
        }
        new MethodInterceptor().doInterceptor(request, response);
        return false;
    }
}

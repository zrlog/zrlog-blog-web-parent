package com.zrlog.blog.hexo.template.impl;

import com.zrlog.blog.web.template.vo.BasePageInfo;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class HexoHelperImpl {
    private final BasePageInfo basePageInfo;

    public HexoHelperImpl(BasePageInfo basePageInfo) {
        this.basePageInfo = basePageInfo;
    }

    public static ProxyExecutable getUrlJoinProvider() {
        return args -> {
            if (args.length == 0) return "";

            String joined = Arrays.stream(args)
                    .map(v -> v.isNull() ? "" : v.asString())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("/"));

            // 正则处理：
            // 1. 将三个及以上连续斜杠转为单斜杠 (/// -> /)
            // 2. 将非协议头位置的双斜杠转为单斜杠 (a//b -> a/b)
            // 3. 保护协议头 (http:// 保持不变)
            return joined.replaceAll("(?<!:)/{2,}", "/");
        };
    }

    // 对应 url_for(path)
    public String url_for(String path) {
        String root = basePageInfo.getBaseUrl();
        if (Objects.isNull(path) || Objects.equals("undefined/", path)) {
            return root;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//")) {
            return path;
        }
        if (basePageInfo.getContextPath().equals("/") || basePageInfo.getContextPath().isEmpty()) {
            if (path.startsWith("/")) {
                return root + path.substring(1);
            }
            if (path.endsWith(".css")) {
                return basePageInfo.getTemplateUrl() + "/source/" + path;
            }
            if (path.endsWith(".js")) {
                return basePageInfo.getTemplateUrl() + "/source/" + path;
            }
            return root + path;
        }
        if (path.startsWith(basePageInfo.getContextPath() + "/")) {
            return root + path.substring(basePageInfo.getContextPath().length() + 1);
        }
        return root + path;
    }
}
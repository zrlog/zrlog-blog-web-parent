package com.zrlog.blog.hexo.template.util;

import com.hibegin.common.util.LoggerUtil;
import com.zrlog.blog.hexo.template.impl.HexoHelperImpl;
import com.zrlog.blog.hexo.template.impl.HexoI18nHelperImpl;
import com.zrlog.blog.hexo.template.impl.HexoPaginator;
import com.zrlog.blog.hexo.template.impl.HexoTagCloud;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.util.YamlLoader;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.ArticleListPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.blog.web.util.WebTools;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoBaseHooks {

    private final BasePageInfo basePageInfo;
    private final JsTemplateRender jsTemplateRender;
    private final String rootPath;
    private final Map<String, Object> theme;

    public HexoBaseHooks(
            String rootPath,
            JsTemplateRender jsTemplateRender,
            BasePageInfo basePageInfo,
            Map<String, Object> theme) {
        this.jsTemplateRender = jsTemplateRender;
        this.rootPath = rootPath;
        this.basePageInfo = basePageInfo;
        this.theme = theme;
    }

    public Map<String, Value> injectMap() {
        // 映射 partial

        Map<String, ProxyExecutable> hexoHelpers = new HashMap<>();

        for (String fun : Arrays.asList("partial", "partial_lang")) {
            hexoHelpers.put(
                    fun,
                    args -> {
                        String path = args[0].asString();
                        Map<String, Object> locals =
                                (args.length > 1 && !args[1].isNull())
                                        ? args[1].as(Map.class)
                                        : null;
                        try {
                            return jsTemplateRender.includeRender(path, locals);
                        } catch (Exception e) {
                            return "<pre>page ->"
                                    + path
                                    + "\n"
                                    + WebTools.htmlEncode(LoggerUtil.recordStackTraceMsg(e))
                                    + "</pre>";
                        }
                    });
        }
        hexoHelpers.put("_p", new HexoI18nHelperImpl(rootPath, basePageInfo.getLocal()));
        hexoHelpers.put("__", new HexoI18nHelperImpl(rootPath, basePageInfo.getLocal()));
        hexoHelpers.put(
                "trim",
                args -> {
                    return args[0].asString().trim();
                });

        if (basePageInfo instanceof ArticleListPageVO) {
            hexoHelpers.put(
                    "paginator", new HexoPaginator(((ArticleListPageVO) basePageInfo).getPager()));
        } else {
            hexoHelpers.put(
                    "paginator",
                    args -> {
                        return "paginator";
                    });
        }
        hexoHelpers.put("tagcloud", new HexoTagCloud(basePageInfo.getInit().getTags()));
        hexoHelpers.put("url_join", HexoHelperImpl.getUrlJoinProvider());

        HexoHelperImpl hexoHelper = new HexoHelperImpl(basePageInfo);
        // 映射 url_for
        hexoHelpers.put(
                "url_for",
                args -> {
                    if (args.length > 0) {
                        return hexoHelper.url_for(args[0].asString());
                    }
                    return hexoHelper.url_for(null);
                });
        hexoHelpers.put(
                "url",
                args -> {
                    if (args.length > 0) {
                        return hexoHelper.url_for(args[0].asString());
                    }
                    return hexoHelper.url_for(null);
                });

        hexoHelpers.put(
                "open_graph",
                args -> {
                    String title = basePageInfo.getTitle();
                    String author = "";
                    String description = basePageInfo.getDescription();
                    StringBuilder sb = new StringBuilder();
                    sb.append(
                            String.format(
                                    "<meta name=\"description\" content=\"%s\"/>\n", description));
                    sb.append("<meta property=\"og:type\" content=\"website\"/>\n");
                    sb.append(
                            String.format("<meta property=\"og:title\" content=\"%s\"/>\n", title));
                    sb.append(
                            String.format(
                                    "<meta property=\"og:author\" content=\"%s\"/>\n", author));
                    sb.append("<meta name=\"twitter:card\" content=\"summary_large_image\"/>\n");

                    return sb.toString();
                });

        hexoHelpers.put(
                "is_post",
                args -> {
                    return basePageInfo instanceof ArticleDetailPageVO;
                });
        hexoHelpers.put(
                "is_tag",
                args -> {
                    return false;
                });
        hexoHelpers.put(
                "toc",
                args -> {
                    return false;
                });

        hexoHelpers.put(
                "is_page",
                args -> {
                    return basePageInfo instanceof ArticleListPageVO;
                });

        hexoHelpers.put(
                "is_archive",
                args -> {
                    return basePageInfo instanceof ArticleListPageVO;
                });
        hexoHelpers.put(
                "is_month",
                args -> {
                    return basePageInfo instanceof ArticleListPageVO;
                });
        hexoHelpers.put(
                "feed_tag",
                args -> {
                    return "feed_tag";
                });
        hexoHelpers.put(
                "favicon_tag",
                args -> {
                    return "favicon_tag";
                });
        hexoHelpers.put(
                "asyncCss",
                args -> {
                    return "asyncCss";
                });
        hexoHelpers.put(
                "vendorCdn",
                args -> {
                    return "vendorCdn";
                });
        hexoHelpers.put(
                "vendorCdnIntegrity",
                args -> {
                    return "vendorCdn";
                });
        hexoHelpers.put(
                "is_home_first_page",
                args -> {
                    return basePageInfo instanceof ArticleListPageVO;
                });
        hexoHelpers.put(
                "markdown",
                args -> {
                    return args[0];
                });
        hexoHelpers.put(
                "truncate",
                args -> {
                    return args[0];
                });
        js(hexoHelpers);

        hexoHelpers.put(
                "is_home",
                args -> {
                    return Objects.equals(YamlLoader.getNestedValue(theme, "page.layout"), "index");
                });
        hexoHelpers.put(
                "is_category",
                args -> {
                    return false;
                });

        hexoHelpers.put(
                "escape_html",
                args -> {
                    return args[0];
                });
        hexoHelpers.put(
                "strip_html",
                args -> {
                    if (args.length == 0 || args[0].isNull()) {
                        return "";
                    }

                    // 获取传入的内容（通常是渲染后的 HTML 字符串）
                    String content = args[0].asString();

                    // 使用正则去除所有 HTML 标签
                    // <[^>]*> 匹配所有的 <...> 标签
                    if (content == null) return "";

                    return content.replaceAll("<[^>]*>", "").trim();
                });

        hexoHelpers.put(
                "full_date",
                args -> {
                    return HexoDateObjUtils.getInstance()
                            .toDateString(
                                    args[0].as(Object.class),
                                    args.length > 1 ? args[1].as(Object.class) : new Date(),
                                    "yyyy-MM-dd HH:mm:ss",
                                    basePageInfo.getLocal());
                });

        hexoHelpers.put(
                "date",
                args -> {
                    return HexoDateObjUtils.getInstance()
                            .toDateString(
                                    args[0].as(Object.class),
                                    args.length > 1 ? args[1].as(Object.class) : null,
                                    "yyyy-MM-dd",
                                    basePageInfo.getLocal());
                });
        hexoHelpers.put(
                "time",
                args -> {
                    return HexoDateObjUtils.getInstance()
                            .toDateString(
                                    args[0].as(Object.class),
                                    args.length > 1 ? args[1].as(Object.class) : null,
                                    "yyyy-MM-dd",
                                    basePageInfo.getLocal());
                });
        hexoHelpers.put(
                "moment",
                args -> {
                    return Value.asValue(
                            Map.of(
                                    "format",
                                    new ProxyExecutable() {
                                        @Override
                                        public Object execute(Value... arguments) {
                                            return "//fixme";
                                        }
                                    }));
                });
        hexoHelpers.put(
                "date_xml",
                args -> {
                    if (args.length == 0 || args[0].isNull()) return "";

                    // 获取日期对象（可能是 Long 时间戳或 Java Date）
                    Object dateObj = args[0].as(Object.class);
                    java.time.ZonedDateTime zonedDateTime;

                    try {
                        if (dateObj instanceof Date) {
                            zonedDateTime =
                                    ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault());
                        } else if (dateObj instanceof Long) {
                            zonedDateTime =
                                    java.time.Instant.ofEpochMilli((Long) dateObj)
                                            .atZone(ZoneId.systemDefault());
                        } else {
                            return dateObj.toString();
                        }

                        // 第二个参数是可选的格式化字符串，例如 date(post.date, 'MMM D, YYYY')
                        String format = (args.length > 1) ? args[1].asString() : "yyyy-MM-dd";

                        // 简单处理：将 Hexo 的 YYYY 转换为 Java 的 yyyy
                        format =
                                format.replace("YYYY", "yyyy")
                                        .replace("YY", "yy")
                                        .replace("DD", "dd");

                        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));
                    } catch (Exception e) {
                        return "Invalid Date";
                    }
                });

        hexoHelpers.put(
                "css",
                args -> {
                    if (args.length == 0 || args[0].isNull()) return "";

                    StringJoiner sb = new StringJoiner("/");

                    // 循环处理所有传入的参数（css 可能被多次调用或传入数组）
                    for (Value arg : args) {
                        sb.add(arg.asString());
                    }
                    return appendCssTag(sb.toString());
                });

        hexoHelpers.put(
                "titlecase",
                args -> {
                    return args[0];
                });

        hexoHelpers.put(
                "list_archives",
                args -> {
                    return "list_archives";
                });

        hexoHelpers.put(
                "list_categories",
                args -> {
                    return "list_categories";
                });

        hexoHelpers.put(
                "list_tags",
                args -> {
                    return "list_tags";
                });

        hexoHelpers.put(
                "get_cdn_url",
                args -> {
                    return basePageInfo.getStaticResourceBaseUrl();
                });

        hexoHelpers.put(
                "is_current",
                args -> {
                    return false;
                });
        Map<String, Value> injects = new HashMap<>();
        for (Map.Entry<String, ProxyExecutable> entry : hexoHelpers.entrySet()) {
            injects.put(entry.getKey(), Value.asValue(entry.getValue()));
        }
        return injects;
    }

    // 辅助拼接方法
    private String appendCssTag(String path) {
        if (path == null || path.isEmpty()) return "";
        // 简单处理：如果没有以 http 或 / 开头，可以自动补充
        String href = path;
        if (!href.contains("://") && !href.startsWith("/")) {
            href = "/" + href;
        }
        // 自动补全后缀
        if (!href.endsWith(".css") && !href.contains("?")) {
            href += ".css";
        }
        if (href.startsWith("/") && !href.startsWith("//")) {
            href = basePageInfo.getTemplateUrl() + "/source" + href;
        }
        return String.format("<link rel=\"stylesheet\" href=\"%s\"/>\n", href);
    }

    private void js(Map<String, ProxyExecutable> hexoHelpers) {
        hexoHelpers.put(
                "js",
                args -> {
                    if (args.length == 0) return "";

                    // 1. 获取脚本路径 (例如: /js/main.js)
                    String src = args[0].asString();

                    // 2. 处理第二个参数 (属性对象，例如 { async: true, defer: true })
                    StringBuilder attributes = new StringBuilder();

                    if (args.length > 1) {
                        // 简单的属性拼接逻辑
                        Value options = args[1];
                        if (options.hasMember("async") && options.getMember("async").asBoolean()) {
                            attributes.append(" async");
                        }
                        if (options.hasMember("defer") && options.getMember("defer").asBoolean()) {
                            attributes.append(" defer");
                        }
                    }
                    // 3. 返回标准的 HTML 标签
                    if (src.startsWith("http")) {
                        return String.format("<script src=\"%s\"%s></script>", src, attributes);
                    }
                    return String.format(
                            "<script src=\"%s\"%s></script>",
                            basePageInfo.getTemplateUrl()
                                    + "/source/"
                                    + src
                                    + (src.endsWith(".js") ? "" : ".js"),
                            attributes);
                });
    }
}

package com.zrlog.blog.polyglot.hooks;

import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.resource.TemplateResolver;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.resource.ZrLogResourceLoader;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

// Java 侧定义 Hook 逻辑
public class IncludeHook implements ProxyExecutable {

    private final JsTemplateRender jsTemplateRender;
    private final TemplateResolver resolver;
    private final BasePageInfo basePageInfo;

    public IncludeHook(
            JsTemplateRender jsTemplateRender,
            TemplateResolver resolver,
            BasePageInfo basePageInfo) {
        this.jsTemplateRender = jsTemplateRender;
        this.resolver = resolver;
        this.basePageInfo = basePageInfo;
    }

    @Override
    public Object execute(Value... arguments) {
        String path = arguments[0].toString();
        Value options = arguments[1];
        String absolutePath = resolver.resolve(path);
        resolver.pushPath(absolutePath);
        Map<String, Object> data =
                (Objects.nonNull(options) && !options.isNull()) ? options.as(Map.class) : null;
        // 注意：Hexo 的 partial 路径通常是相对于当前模板目录的
        try {
            // 拦截
            if (path.equals("_partials/comments/comment")
                    && basePageInfo instanceof ArticleDetailPageVO) {
                return "<plugin name=\""
                        + basePageInfo.getWebs().getComment_plugin_name()
                        + "\" view=\"widget\" param=\"articleId="
                        + ((ArticleDetailPageVO) basePageInfo).getLog().getLogId()
                        + "\"/>\n";
            }
            String testPath =
                    jsTemplateRender.getTemplate()
                            + "/"
                            + path
                            + (path.contains(jsTemplateRender.getTemplateExt())
                                    ? ""
                                    : jsTemplateRender.getTemplateExt());
            // 为绝对路径，不关心路径
            if (ZrLogResourceLoader.exists(testPath)) {
                return jsTemplateRender.render(path, data);
            } else {
                String renderPath =
                        absolutePath.substring((jsTemplateRender.getTemplate() + "/").length());
                return jsTemplateRender.render(renderPath, data);
            }
        } finally {
            // 3. 必须出栈，否则路径上下文会乱
            resolver.popPath();
        }
    }
}

package com.zrlog.blog.polyglot.ejs;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.hooks.IncludeHook;
import com.zrlog.blog.polyglot.resource.ScriptProvider;
import com.zrlog.blog.polyglot.resource.TemplateResolver;
import com.zrlog.blog.polyglot.util.GraalDataUtils;
import com.zrlog.blog.polyglot.util.PolyglotContextUtils;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.resource.ZrLogResourceLoader;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class EjsTemplateRender implements JsTemplateRender {

    private static final Logger LOGGER = LoggerUtil.getLogger(EjsTemplateRender.class);

    private final Context context;
    private final Value jsBindings;
    private final Value ejs;
    private final String templateExt = ".ejs";
    private final String template;
    private final IncludeHook includeHook;
    private final Map<String, Object> locals;
    private final ScriptProvider scriptProvider;

    public EjsTemplateRender(String template, BasePageInfo basePageInfo, Map<String, Object> locals) {
        this.template = template;
        this.scriptProvider = ScriptProvider.getInstance();
        this.includeHook = new IncludeHook(this, new TemplateResolver(template), basePageInfo);
        locals.put("include", includeHook);
        this.locals = locals;
        this.context = PolyglotContextUtils.buildJsContext();
        try {
            this.jsBindings = context.getBindings("js");
            this.jsBindings.putMember("scriptProvider", scriptProvider);
            context.eval("js", "var global = globalThis;");
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/require.js").readAllBytes()));
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/ejs.min.js").readAllBytes()));
            this.ejs = jsBindings.getMember("ejs");
            if (ejs == null || ejs.getMember("render").isNull()) {
                throw new RuntimeException("EJS 引擎未初始化，请先加载 ejs.min.js");
            }
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/hooks.js").readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Map.Entry<String, Object> entry : locals.entrySet()) {
            jsBindings.putMember(entry.getKey(), GraalDataUtils.makeJsFriendly(entry.getValue()));
        }
    }

    @Override
    public Value getJsBindings() {
        return jsBindings;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public String getTemplateExt() {
        return templateExt;
    }

    @Override
    public String render(String page, Map<String, Object> data) {
        long start = System.currentTimeMillis();
        try {
            if (Objects.nonNull(data)) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (locals.containsKey(entry.getKey())) {
                        continue;
                    }
                    jsBindings.putMember(entry.getKey(), GraalDataUtils.makeJsFriendly(entry.getValue()));
                    locals.put(entry.getKey(), entry.getValue());
                }
            }
            String path = (template + "/" + page + (page.endsWith(templateExt) ? "" : templateExt)).replaceAll("//", "/");
            Value options = context.eval("js", "({ async: false, cache: false })");
            String read = ZrLogResourceLoader.read(path);
            Value result = ejs.getMember("render").execute(read, locals, options);
            return result.asString();
        } finally {
            LOGGER.info(page + " used time " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public String includeRender(String page, Map<String, Object> data) {
        return includeHook.execute(Value.asValue(page), Value.asValue(data)).toString();
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public ScriptProvider getScriptProvider() {
        return scriptProvider;
    }

    @Override
    public void close() throws Exception {
        this.context.close();
    }
}

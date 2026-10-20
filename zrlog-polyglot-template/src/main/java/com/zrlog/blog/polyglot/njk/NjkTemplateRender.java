package com.zrlog.blog.polyglot.njk;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.hooks.IncludeHook;
import com.zrlog.blog.polyglot.resource.ScriptProvider;
import com.zrlog.blog.polyglot.resource.TemplateResolver;
import com.zrlog.blog.polyglot.util.GraalDataUtils;
import com.zrlog.blog.polyglot.util.PolyglotContextUtils;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class NjkTemplateRender implements JsTemplateRender {

    private static final Logger LOGGER = LoggerUtil.getLogger(NjkTemplateRender.class);

    private final Context context;
    private final Value jsBindings;
    private final Value env;
    private final String templateExt = ".njk";
    private final String template;
    private final IncludeHook includeHook;
    private final Map<String, Object> locals;
    private final ScriptProvider scriptProvider;

    public NjkTemplateRender(String template, BasePageInfo basePageInfo, Map<String, Object> locals) {
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
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/nunjucks.min.js").readAllBytes()));
            Value njk = jsBindings.getMember("nunjucks");
            if (njk == null || njk.getMember("render").isNull()) {
                throw new RuntimeException("nunjucks 引擎未初始化，请先加载 njk.min.js");
            }
            this.jsBindings.putMember("javaLoader", new NativeJavaLoader(template));
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/hooks.js").readAllBytes()));
            context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/init-njk.js").readAllBytes()));
            this.env = jsBindings.getMember("env");
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
                locals.putAll(data);
            }
            String path = (page + (page.endsWith(templateExt) ? "" : templateExt)).replaceAll("//", "/");
            Value result = env.getMember("render").execute(path, GraalDataUtils.makeJsFriendly(locals));
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
    public void init(Map<String, Object> root, Map<String, Value> helpers) {
        String wrapperScript =
                "var __wrapHelper = function(fn) {\n" +
                        "    return function() {\n" +
                        "        var realCtx = (this && this.ctx) ? this.ctx : globalThis;\n" +
                        "        return fn.apply(realCtx, arguments);\n" +
                        "    };\n" +
                        "};";
        context.eval("js", wrapperScript);
        Value wrapperFactory = context.getBindings("js").getMember("__wrapHelper");
        for (Map.Entry<String, Value> entry : helpers.entrySet()) {
            Value helperValue = entry.getValue();
            if (helperValue.canExecute()) {
                // 如果是函数，就调用工厂给它穿上“隐形外衣”，重定向 this
                helperValue = wrapperFactory.execute(helperValue);
            }

            // 使用你原本的那行极其标准的代码，完成最终注册！
            env.getMember("addGlobal").execute(entry.getKey(), helperValue);
            root.put(entry.getKey(), helperValue);
        }
    }

    @Override
    public void close() throws Exception {
        this.context.close();
    }
}

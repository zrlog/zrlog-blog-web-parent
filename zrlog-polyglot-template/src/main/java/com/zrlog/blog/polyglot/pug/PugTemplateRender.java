package com.zrlog.blog.polyglot.pug;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.ejs.EjsTemplateRender;
import com.zrlog.blog.polyglot.hooks.IncludeHook;
import com.zrlog.blog.polyglot.resource.ScriptProvider;
import com.zrlog.blog.polyglot.resource.TemplateResolver;
import com.zrlog.blog.polyglot.util.GraalDataUtils;
import com.zrlog.blog.polyglot.util.PolyglotContextUtils;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.resource.ZrLogResourceLoader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class PugTemplateRender implements JsTemplateRender {

    private static final Logger LOGGER = LoggerUtil.getLogger(EjsTemplateRender.class);

    private final Context context;
    private final Value jsBindings;
    private final Value pug;
    private final String templateExt = ".pug";
    private final String template;
    private final IncludeHook includeHook;
    private final Map<String, Object> locals;
    private final ScriptProvider scriptProvider;

    public PugTemplateRender(
            String template, BasePageInfo basePageInfo, Map<String, Object> locals) {
        this.template = template;
        this.scriptProvider = ScriptProvider.getInstance();
        this.includeHook = new IncludeHook(this, new TemplateResolver(template), basePageInfo);
        locals.put("include", includeHook);
        this.locals = locals;
        this.context = PolyglotContextUtils.buildJsContext();
        this.jsBindings = context.getBindings("js");
        try {
            this.jsBindings.putMember("scriptProvider", scriptProvider);
            context.eval(
                    "js",
                    "var global = this; var window = this; "
                            + "var process = { env: {} }; "
                            + "var exports = {}; "
                            + "var module = { exports: exports };");

            context.eval("js", "var global = globalThis;");
            String fullPugCode =
                    new String(PathUtil.getConfInputStream("base/scripts/pug.js").readAllBytes());
            Source source = Source.create("js", fullPugCode);
            // 执行文件，此时 require=function... 已经在 JS 运行了
            context.eval(source);
            // 4. 验证
            this.pug =
                    context.eval(
                            "js",
                            "(function() {"
                                    + "  if (module.exports.render) return module.exports;"
                                    + "  if (typeof require === 'function') return require('pug');"
                                    + "  return window.pug;"
                                    + "})()");

            if (pug == null || pug.getMember("render").isNull()) {
                throw new RuntimeException("pug 引擎未初始化，请先加载 pug.js");
            }
            context.eval(
                    "js",
                    new String(
                            PathUtil.getConfInputStream("base/scripts/require.js").readAllBytes()));
            context.eval(
                    "js",
                    new String(
                            PathUtil.getConfInputStream("base/scripts/hooks.js").readAllBytes()));
            scriptProvider.addBaseScriptByPath("fs", "base/scripts/fs.js");
            jsBindings.putMember(
                    "javaReadSync",
                    (ProxyExecutable)
                            args -> {
                                if (EnvKit.isDevMode()) {
                                    LOGGER.info("pug template read file " + args[0]);
                                }
                                return ZrLogResourceLoader.read(args[0].asString());
                            });
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
        if (Objects.nonNull(data)) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (locals.containsKey(entry.getKey())) {
                    continue;
                }
                jsBindings.putMember(
                        entry.getKey(), GraalDataUtils.makeJsFriendly(entry.getValue()));
                locals.put(entry.getKey(), entry.getValue());
            }
        }
        String path =
                (template + "/" + page + (page.endsWith(templateExt) ? "" : templateExt))
                        .replaceAll("//", "/");
        Value options =
                context.eval("js", "({ async: false, cache: false,filename: '" + page + "' })");
        Value result = pug.getMember("renderFile").execute(path, locals, options);
        return result.asString();
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

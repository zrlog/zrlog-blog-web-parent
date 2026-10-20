package com.zrlog.blog.hexo.template;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.hexo.template.util.HexoBaseHooks;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.resource.ScriptProvider;
import com.zrlog.blog.polyglot.sytlus.StylusBundler;
import com.zrlog.blog.polyglot.util.GraalDataUtils;
import com.zrlog.blog.polyglot.util.YamlLoader;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.resource.ResourceScanner;
import com.zrlog.common.resource.ZrLogResourceLoader;
import com.zrlog.common.vo.TemplateVO;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class HexoObjectBox {

    private static final Logger LOGGER = LoggerUtil.getLogger(HexoObjectBox.class);
    protected final Map<String, Object> root;
    protected final BasePageInfo basePageInfo;
    protected final String rootPath;
    protected final TemplateVO templateVO;
    protected final Map<String, Value> helperMap = new LinkedHashMap<>();
    protected final InjectionStorage injectionStorage;
    private boolean stylusInit = false;


    public HexoObjectBox(Map<String, Object> root, String rootPath, BasePageInfo basePageInfo, TemplateVO templateVO, String templateDir) {
        this.root = root;
        this.templateVO = templateVO;
        this.basePageInfo = basePageInfo;
        this.rootPath = rootPath;
        this.injectionStorage = new InjectionStorage(new ConcurrentHashMap<>(), templateDir);
        this.fillConfig();
    }

    public Map<String, Value> getHelperMap() {
        return helperMap;
    }

    protected boolean filterRegister(Context context, String name, Value[] values) {
        if (name.equals("theme_inject")) {
            Value callback = values[1];
            // 关键：当脚本运行回调时，传入我们构造的 injects 代理对象
            callback.execute(createInjectsProxy(context));
            return true;
        }
        return false;
    }

    protected boolean helperRegister(JsTemplateRender jsTemplateRender, String name, Value[] values) {
        return false;
    }

    protected String getStylRoot() {
        return "/source/css";
    }

    public void fixImageUrl(String rootKey, String valueName) {
        Map<String, Object> map = (Map<String, Object>) YamlLoader.getNestedValue(root, rootKey);
        if (Objects.isNull(map)) {
            return;
        }
        Object value = map.get(valueName);
        if (Objects.isNull(value)) {
            return;
        }
        if (value instanceof String) {
            if (((String) value).startsWith("http://") || ((String) value).startsWith("https://")) {
                return;
            }
            if (((String) value).startsWith("/img")) {
                map.put(valueName, basePageInfo.getTemplateUrl() + "/source" + value);
            }
        }
    }

    protected void fillConfig() {

    }

    protected void regisConfig(Value bindings) {

    }

    public List<String> getCompileStyl() {
        return List.of("main.styl"/*, "highlight.styl", "highlight-dark.styl"*/);
    }

    public void regStyleHooks(Context context) throws Exception {

    }

    private void initStylus(Context context) throws IOException {
        if (stylusInit) {
            return;
        }
        stylusInit = true;
        context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/stylus-renderer.min.js").readAllBytes()));
        context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/stylus-polyfill.js").readAllBytes()));
    }

    public void compileStyl(Context context) throws Exception {
        for (String compileStyl : getCompileStyl()) {
            // 3. 准备 Stylus 代码
            String styleRoot = rootPath + getStylRoot();
            String resourceFile = styleRoot + "/" + compileStyl;
            String path = basePageInfo.getTemplate() + getStylRoot() + "/" + compileStyl.replace(".styl", ".css");
            if (ZrLogResourceLoader.exists("classpath:" + path)) {
                continue;
            }
            if (ZrLogResourceLoader.exists(path)) {
                continue;
            }
            File staticFile = PathUtil.getStaticFile(path);
            if (staticFile.exists()) {
                continue;
            }
            if (!ZrLogResourceLoader.exists(resourceFile)) {
                LOGGER.warning(resourceFile + " not found");
                continue;
            }
            try {
                initStylus(context);
                String stylusCode = new StylusBundler(styleRoot).bundle(compileStyl).replaceAll("(?s)/\\*.*?\\*/", "").trim();
                context.getBindings("js").putMember("myStylusCode", stylusCode.trim());
                context.eval("js", "var renderer = new StylusRenderer(myStylusCode); ");
                context.eval("js", new String(PathUtil.getConfInputStream("base/scripts/stylus-hooks.js").readAllBytes()));
                regStyleHooks(context);
                String renderResult = context.eval("js", "renderer.render();").asString();
                if (StringUtils.isEmpty(renderResult)) {
                    throw new RuntimeException("render " + stylusCode.substring(0, Math.min(stylusCode.length(), 100)) + " error");
                }
                IOUtil.writeStrToFile(renderResult, staticFile);
            } catch (Exception e) {
                LOGGER.severe(resourceFile + " compile error " + LoggerUtil.recordStackTraceMsg(e));
            }
        }

    }

    public void initScript(ScriptProvider scriptProvider, JsTemplateRender jsTemplateRender) {

    }

    private void initHexo(JsTemplateRender jsTemplateRender) {
        Value bindings = jsTemplateRender.getJsBindings();
        Context context = jsTemplateRender.getContext();
        Value hexo = context.eval("js", "({})");
        hexo.putMember("theme_dir", rootPath);

        Object themeObj = GraalDataUtils.makeJsFriendly(root);
        hexo.putMember("config", themeObj);
        hexo.putMember("theme", themeObj);

        Value extend = context.eval("js", "({})");
        Value filter = context.eval("js", "({})");
        Value helper = context.eval("js", "({})");
        Value on = context.eval("js", "({})");
        Value tag = context.eval("js", "({})");
        //ignore
        Value generator = context.eval("js", "({})");

        filter.putMember("register", (ProxyExecutable) args -> {
            String type = args[0].asString();
            Value callback = args[1];
            if (!filterRegister(context, type, args)) {
                bindings.putMember(type, callback);
                helperMap.put(type, callback);
            }
            return null;
        });
        helper.putMember("register", (ProxyExecutable) args -> {
            String type = args[0].asString();
            Value callback = args[1];
            if (!helperRegister(jsTemplateRender, type, args)) {
                bindings.putMember(type, callback);
                helperMap.put(type, callback);
                if (EnvKit.isDevMode()) {
                    LOGGER.info("inject helper -> " + type + " success");
                }
            }

            return null;
        });
        generator.putMember("register", (ProxyExecutable) args -> {
            //ignore
            return null;
        });
        generator.putMember("get", (ProxyExecutable) args -> {
            //ignore
            return null;
        });
        tag.putMember("register", (ProxyExecutable) args -> {
            //ignore
            return null;
        });


        extend.putMember("filter", filter);
        extend.putMember("helper", helper);
        extend.putMember("generator", generator);
        extend.putMember("tag", tag);
        hexo.putMember("extend", extend);
        hexo.putMember("on", on);
        bindings.putMember("hexo", hexo);
        bindings.putMember("ctx", hexo);

        initScript(jsTemplateRender.getScriptProvider(), jsTemplateRender);
    }

    public void setup(JsTemplateRender jsTemplateRender) throws Exception {
        Value bindings = jsTemplateRender.getJsBindings();
        Context context = jsTemplateRender.getContext();
        helperMap.putAll(new HexoBaseHooks(rootPath, jsTemplateRender, basePageInfo, root).injectMap());
        for (Map.Entry<String, Value> e : helperMap.entrySet()) {
            bindings.putMember(e.getKey(), e.getValue());
        }
        scanScripts(jsTemplateRender);
        regisConfig(bindings);
        compileStyl(context);
        bindings.putMember("theme", GraalDataUtils.makeJsFriendly(root));
    }

    private void scanScripts(JsTemplateRender jsTemplateRender) throws IOException {
        Context context = jsTemplateRender.getContext();
        initHexo(jsTemplateRender);
        ResourceScanner scanner = new ResourceScanner(rootPath);
        ScriptProvider scriptProvider = jsTemplateRender.getScriptProvider();
        List<String> scripts = scanner.listFiles("scripts/").stream().filter(e -> e.endsWith(".js")).toList();
        scriptProvider.addBaseScriptByPath("path", "base/scripts/path.js");
        scriptProvider.addBaseScriptByPath("hexo-util", "hexo/scripts/hexo-util.js");
        scriptProvider.addBaseScriptByPath("url", "base/scripts/url.js");
        //scriptProvider.addBaseScript("moize", new String(PathUtil.getConfInputStream("hexo/scripts/moize.js").readAllBytes()));
        for (String script : scripts) {
            scriptProvider.addScript(script.substring((rootPath + "/scripts/").length()).replaceAll(".js", ""), script);
            String filename = new File(script).getName();
            scriptProvider.addScript(filename.replaceAll(".js", ""), script);
            scriptProvider.addScript("./" + filename.replaceAll(".js", ""), script);
        }
        for (String scriptPath : scripts) {
            if (scriptPath.contains("/generators/")) {
                continue;
            }
            if (scriptPath.contains("/events/")) {
                continue;
            }
            String code = ZrLogResourceLoader.read(scriptPath);
            if (code.contains("register('") || code.contains("register(\"")) {
                try {
                    context.eval("js", "{" + code + "}");
                    if (EnvKit.isDevMode()) {
                        LOGGER.info("Exec " + scriptPath + " success");
                    }
                } catch (Exception e) {
                    LOGGER.severe("Exec " + scriptPath + " error " + LoggerUtil.recordStackTraceMsg(e));
                }
            }
        }
    }


    /**
     * 创建一个动态代理对象，捕获 injects.xxx.file() 的调用
     */
    private Value createInjectsProxy(Context context) {
        // 这是一个“万能插槽”处理器，处理诸如 injects.header.file(...) 的调用
        return context.eval("js", "(function(storage) {" + "  return new Proxy({}, {" + "    get: function(target, slot) {" + "      return {" + "        file: function(name, path) {" + "          storage.add(slot, path);" + // 调用 Java 的 add 方法
                "        }" + "      };" + "    }" + "  });" + "})").execute(injectionStorage);
    }
}

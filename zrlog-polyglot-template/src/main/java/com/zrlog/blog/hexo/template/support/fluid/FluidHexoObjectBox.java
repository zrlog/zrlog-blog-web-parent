package com.zrlog.blog.hexo.template.support.fluid;

import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.hexo.template.HexoObjectBox;
import com.zrlog.blog.hexo.template.impl.HexoCssExImpl;
import com.zrlog.blog.hexo.template.impl.HexoJsExImpl;
import com.zrlog.blog.hexo.template.support.InjectPoint;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.util.YamlLoader;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.vo.TemplateVO;
import java.util.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class FluidHexoObjectBox extends HexoObjectBox {

    public FluidHexoObjectBox(
            Map<String, Object> root,
            String rootPath,
            BasePageInfo basePageInfo,
            TemplateVO templateVO,
            String templateDir) {
        super(root, rootPath, basePageInfo, templateVO, templateDir);
    }

    @Override
    protected boolean helperRegister(
            JsTemplateRender jsTemplateRender, String name, Value[] values) {
        Value bindings = jsTemplateRender.getJsBindings();
        switch (name) {
            case "inject_point" -> {
                bindings.putMember(name, new InjectPoint(injectionStorage, jsTemplateRender));
                return true;
            }
            case "js_ex" -> {
                bindings.putMember(name, new HexoJsExImpl(basePageInfo));
                return true;
            }
            case "css_ex" -> {
                bindings.putMember(name, new HexoCssExImpl(basePageInfo));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getCompileStyl() {
        return Arrays.asList("main.styl" /*, "highlight.styl", "highlight-dark.styl"*/);
    }

    @Override
    public void regStyleHooks(Context context) throws Exception {
        context.eval(
                "js",
                new String(
                        PathUtil.getConfInputStream("hexo/support/fluid-stylus.js")
                                .readAllBytes()));
        // context.eval("js", "renderer.define('theme-config', function(pathNode) {" + "  return
        // hexo_config_java(pathNode.val);" + "});");
    }

    @Override
    protected void fillConfig() {
        for (String key :
                Arrays.asList(
                        "index",
                        "page",
                        "tag",
                        "about",
                        "page404",
                        "links",
                        "archive",
                        "post",
                        "category")) {
            fixImageUrl(key, "banner_img");
        }

        Map<String, Object> config = (Map<String, Object>) root.get("config");
        Map<String, Object> indexGen =
                (Map<String, Object>)
                        config.computeIfAbsent("index_generator", k -> new HashMap<>());
        indexGen.putIfAbsent("order_by", "name");

        if (basePageInfo instanceof ArticleDetailPageVO) {
            Map<String, Object> comments =
                    (Map<String, Object>) YamlLoader.getNestedValue(config, "post.comments");
            if (Objects.equals(((ArticleDetailPageVO) basePageInfo).getLog().getCanComment(), true)
                    && Objects.nonNull(comments)) {
                comments.put("type", basePageInfo.getWebs().getComment_plugin_name());
                comments.put("enable", true);
            }
        }
        Map<String, Object> footer =
                (Map<String, Object>) YamlLoader.getNestedValue(config, "footer");
        if (Objects.nonNull(footer)) {
            Object content = footer.get("content");
            String webCm =
                    "<div style='display:none'>" + basePageInfo.getWebs().getWebCm() + "</div>";
            if (Objects.nonNull(content) && content.toString().contains("Hexo")) {
                content =
                        content.toString()
                                .replace("Hexo", "ZrLog")
                                .replace("hexo.io", "www.zrlog.com");
            }
            footer.put("content", content + webCm);
        }
    }

    @Override
    protected void regisConfig(Value bindings) {
        bindings.putMember("hexo_config_java", new FluidConfigProxy(this.root));
        bindings.putMember("fluid_version", templateVO.getVersion());
        Map<String, Object> nestedValue =
                (Map<String, Object>) YamlLoader.getNestedValue(root, "index.slogan");
        if (Objects.nonNull(nestedValue)) {
            nestedValue.put("text", basePageInfo.getWebs().getSecond_title());
        }
    }
}

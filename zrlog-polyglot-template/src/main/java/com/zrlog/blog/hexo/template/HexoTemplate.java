package com.zrlog.blog.hexo.template;

import com.zrlog.theme.spi.BundledThemes;

import com.zrlog.blog.hexo.template.support.butterfly.ButterflyHexoObjectBox;
import com.zrlog.blog.hexo.template.support.fluid.FluidHexoObjectBox;
import com.zrlog.blog.hexo.template.support.next.NextHexoObjectBox;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.polyglot.ejs.EjsTemplateRender;
import com.zrlog.blog.polyglot.njk.NjkTemplateRender;
import com.zrlog.blog.polyglot.pug.PugTemplateRender;
import com.zrlog.blog.polyglot.util.YamlLoader;
import com.zrlog.blog.web.template.ZrLogTemplate;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.business.type.TemplateType;
import com.zrlog.common.Constants;
import com.zrlog.common.exception.NotImplementException;
import com.zrlog.common.resource.ZrLogResourceLoader;
import com.zrlog.common.vo.TemplateVO;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class HexoTemplate implements ZrLogTemplate {
    private String template;
    private String rootPath;
    private final TemplateVO templateVO;

    public String getTemplate() {
        return template;
    }

    public HexoTemplate(TemplateVO templateVO) {
        this.templateVO = templateVO;
    }

    @Override
    public void init(File path) throws Exception {
        this.rootPath = path.getAbsolutePath();
        setup();
    }

    @Override
    public String render(String page, BasePageInfo pageInfo) throws Exception {
        Map<String, Object> root = HexoPageConverter.toRootMap(pageInfo, page, rootPath);
        HexoObjectBox hexoObjectBox = buildHexoObjectByTemplate(root, pageInfo);
        try (JsTemplateRender jsTemplateRender = buildJsTemplateRender(root, pageInfo)) {
            hexoObjectBox.setup(jsTemplateRender);
            jsTemplateRender.init(root, hexoObjectBox.getHelperMap());
            String body = jsTemplateRender.render((String) YamlLoader.getNestedValue(root, "page.layout"), root);
            if (jsTemplateRender instanceof EjsTemplateRender) {
                jsTemplateRender.getJsBindings().putMember("body", body);

                return jsTemplateRender.render("layout", root);
            }
            return body;
        }
    }

    private HexoObjectBox buildHexoObjectByTemplate(Map<String, Object> root, BasePageInfo pageInfo) {
        String adapter = BundledThemes.getInstance().find(this.templateVO.getTemplate())
                .map(com.zrlog.theme.spi.BundledThemeProvider::adapter).orElse("");
        if ("fluid".equals(adapter)) {
            return new FluidHexoObjectBox(root, rootPath, pageInfo, templateVO, template);
        }
        if ("butterfly".equals(adapter)) {
            return new ButterflyHexoObjectBox(root, rootPath, pageInfo, templateVO, template);
        }
        if ("next".equals(adapter)) {
            return new NextHexoObjectBox(root, rootPath, pageInfo, templateVO, template);
        }
        return new HexoObjectBox(root, rootPath, pageInfo, templateVO, template);
    }

    private JsTemplateRender buildJsTemplateRender(Map<String, Object> root, BasePageInfo pageInfo) {
        if (templateVO.getViewType().equals(".ejs")) {
            return new EjsTemplateRender(template, pageInfo, root);
        }
        if (templateVO.getViewType().equals(".pug")) {
            return new PugTemplateRender(template, pageInfo, root);
        }
        if (templateVO.getViewType().equals(".njk")) {
            return new NjkTemplateRender(template, pageInfo, root);
        }
        throw new NotImplementException();
    }

    private void setup() {
        this.template = (rootPath + "/layout");
    }

    @Override
    public void initClassTemplate(String templateBase) {
        this.rootPath = "classpath:" + templateBase;
        setup();
    }
}
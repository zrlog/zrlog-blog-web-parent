package com.zrlog.blog.hexo.template.support.next;

import com.zrlog.blog.hexo.template.HexoObjectBox;
import com.zrlog.blog.hexo.template.support.InjectPoint;
import com.zrlog.blog.polyglot.JsTemplateRender;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.vo.TemplateVO;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Value;

public class NextHexoObjectBox extends HexoObjectBox {

    public NextHexoObjectBox(
            Map<String, Object> root,
            String rootPath,
            BasePageInfo basePageInfo,
            TemplateVO templateVO,
            String templateDir) {
        super(root, rootPath, basePageInfo, templateVO, templateDir);

        Map<String, Object> theme = (Map<String, Object>) root.get("theme");
        if (Objects.nonNull(theme)) {
            theme.put("css", basePageInfo.getTemplateUrl() + "/source/" + theme.get("css"));
        }
        theme.put("permalink", "");
        // webCm
        if (basePageInfo instanceof ArticleDetailPageVO) {
            Map<String, Object> page = (Map<String, Object>) root.get("page");
            page.put("content", page.get("content") + basePageInfo.getWebs().getWebCm());
        }
    }

    @Override
    protected boolean helperRegister(
            JsTemplateRender jsTemplateRender, String name, Value[] values) {
        Value bindings = jsTemplateRender.getJsBindings();
        if (name.equals("next_inject")) {
            InjectPoint injectPoint = new InjectPoint(injectionStorage, jsTemplateRender);
            bindings.putMember(name, injectPoint);
            helperMap.put(name, Value.asValue(injectPoint));
            return true;
        }
        return false;
    }
}

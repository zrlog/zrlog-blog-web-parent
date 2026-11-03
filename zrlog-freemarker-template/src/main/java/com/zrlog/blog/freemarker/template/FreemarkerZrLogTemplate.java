package com.zrlog.blog.freemarker.template;

import com.hibegin.http.server.util.FreeMarkerUtil;
import com.zrlog.blog.web.template.ZrLogTemplate;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import java.io.File;

public class FreemarkerZrLogTemplate implements ZrLogTemplate {
    @Override
    public void init(File path) throws Exception {
        FreeMarkerUtil.init(path.getPath());
    }

    @Override
    public String render(String page, BasePageInfo pageInfo) throws Exception {
        return FreeMarkerUtil.renderToFMByModel(page, pageInfo);
    }

    @Override
    public void initClassTemplate(String template) {
        FreeMarkerUtil.initClassTemplate(template);
    }
}

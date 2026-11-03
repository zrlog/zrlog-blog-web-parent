package com.zrlog.blog.hexo.template.impl;

import com.zrlog.blog.web.template.vo.BasePageInfo;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoJsExImpl implements ProxyExecutable {

    private final BasePageInfo basePageInfo;

    public HexoJsExImpl(BasePageInfo basePageInfo) {
        this.basePageInfo = basePageInfo;
    }

    @Override
    public Object execute(Value... args) {
        if (args.length == 0) return "";

        // 1. 获取脚本路径 (例如: /js/main.js)
        String src = args[0].asString();

        // 2. 处理第二个参数 (属性对象，例如 { async: true, defer: true })
        StringBuilder attributes = new StringBuilder();

        if (args.length > 1 && !args[1].isNull()) {
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
            return String.format("<script src=\"%s\"%s></script>", src + args[1], attributes);
        }
        return String.format(
                "<script src=\"%s\"%s></script>",
                basePageInfo.getTemplateUrl() + "/source" + src + "/" + args[1], attributes);
    }
}

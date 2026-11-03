package com.zrlog.blog.hexo.template.support;

import com.zrlog.blog.hexo.template.InjectionStorage;
import com.zrlog.blog.polyglot.JsTemplateRender;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class InjectPoint implements ProxyExecutable {

    private final InjectionStorage injectionStorage;
    private final JsTemplateRender jsTemplateRender;

    public InjectPoint(InjectionStorage injectionStorage, JsTemplateRender jsTemplateRender) {
        this.injectionStorage = injectionStorage;
        this.jsTemplateRender = jsTemplateRender;
    }

    @Override
    public Object execute(Value... arguments) {

        if (arguments.length == 0) return "";
        String pointName = arguments[0].asString();

        // 1. 从之前 setup 阶段填充的 injectionPoints Map 中获取注册的文件路径列表
        // 这里的 injectionPoints 是你存储 List<String> 路径的那个全局 Map
        List<String> filePaths = injectionStorage.get(pointName);

        if (filePaths == null || filePaths.isEmpty()) {
            return "";
        }

        StringBuilder htmlResult = new StringBuilder();
        for (String filePath : filePaths) {
            String renderedContent = jsTemplateRender.render(filePath, null);
            htmlResult.append(renderedContent);
        }

        return htmlResult.toString();
    }
}

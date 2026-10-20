package com.zrlog.blog.polyglot;

import com.zrlog.blog.polyglot.resource.ScriptProvider;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;

public interface JsTemplateRender extends AutoCloseable {


    Value getJsBindings();

    Context getContext();

    String getTemplateExt();

    String render(String page, Map<String, Object> data);

    String includeRender(String page, Map<String, Object> data);

    String getTemplate();

    ScriptProvider getScriptProvider();

    default void init(Map<String, Object> root, Map<String, Value> helpers) {
        //root.putAll(helpers);
    }
}

package com.zrlog.blog.hexo.template.support.fluid;

import com.hibegin.common.util.ObjectHelpers;
import com.zrlog.blog.polyglot.util.YamlLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class FluidConfigProxy implements ProxyExecutable {

    private final Map<String, Object> theme;

    public FluidConfigProxy(Map<String, Object> theme) {
        this.theme = theme;
    }

    @Override
    public Object execute(Value... arguments) {
        String key = arguments[0].asString();
        if (Objects.equals("injects.variable", key)) {
            return new ArrayList<>();
        }
        if (Objects.equals("injects.mixin", key)) {
            return new ArrayList<>();
        }
        if (Objects.equals("injects.style", key)) {
            return new ArrayList<>();
        }
        return ObjectHelpers.requireNonNullElse(YamlLoader.getNestedValue(theme, key), "");
    }
}

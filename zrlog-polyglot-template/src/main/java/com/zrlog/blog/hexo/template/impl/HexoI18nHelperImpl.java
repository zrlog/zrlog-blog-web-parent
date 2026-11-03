package com.zrlog.blog.hexo.template.impl;

import com.zrlog.blog.polyglot.util.YamlLoader;
import com.zrlog.common.resource.ZrLogResourceLoader;
import java.util.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoI18nHelperImpl implements ProxyExecutable {

    private final Map<String, Object> languagesMap = new TreeMap<>();

    public HexoI18nHelperImpl(String rootPath, String lang) {
        for (String langAlias : getLangFiles(lang)) {
            String path = rootPath + "/languages/" + langAlias + ".yml";
            if (ZrLogResourceLoader.exists(path)) {
                languagesMap.putAll(YamlLoader.loadConfig(ZrLogResourceLoader.read(path)));
                break;
            }
        }
    }

    private List<String> getLangFiles(String lang) {
        return Arrays.asList(lang, lang.replace("_", "-"), lang.split("_")[0]);
    }

    @Override
    public Object execute(Value... args) {
        String path = args[0].toString();
        Object nestedValue = YamlLoader.getNestedValue(languagesMap, path);
        if (Objects.isNull(nestedValue)) {
            return path;
        }
        try {
            List<Value> objects = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
            return String.format(
                    nestedValue.toString(),
                    objects.stream().map(e -> e.as(Object.class)).toArray());
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}

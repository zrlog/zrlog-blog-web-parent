package com.zrlog.blog.polyglot.util;

import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class YamlLoader {

    public static Map<String, Object> loadConfig(String ymlConfig) {
        Yaml yaml = new Yaml();
        // 直接解析为嵌套的 Map 结构
        return yaml.load(ymlConfig);
    }

    /** 辅助方法：从嵌套 Map 中根据点号路径获取值 (例如: "menu.home") */
    @SuppressWarnings("unchecked")
    public static Object getNestedValue(Map<String, Object> map, String path) {
        String[] keys = path.split("\\.");
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }
}

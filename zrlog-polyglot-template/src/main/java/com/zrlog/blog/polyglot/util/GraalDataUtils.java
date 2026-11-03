package com.zrlog.blog.polyglot.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

public class GraalDataUtils {
    public static Object makeJsFriendly(Object obj) {
        if (obj == null) {
            return null; // 显式返回 null，JS 能够识别
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<String, Object> converted = new HashMap<>();

            map.forEach(
                    (key, value) -> {
                        // Hexo/EJS 模板通常期望 key 是字符串
                        String stringKey = (key == null) ? "null" : key.toString();
                        // 递归转换 Value
                        converted.put(stringKey, makeJsFriendly(value));
                    });
            return ProxyObject.fromMap(converted);

        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            List<Object> converted =
                    list.stream().map(GraalDataUtils::makeJsFriendly).collect(Collectors.toList());
            // 包装成 JS 原生认可的数组代理
            return ProxyArray.fromList(converted);
        }

        // 基本类型 (String, Number, Boolean) 直接返回
        return obj;
    }
}

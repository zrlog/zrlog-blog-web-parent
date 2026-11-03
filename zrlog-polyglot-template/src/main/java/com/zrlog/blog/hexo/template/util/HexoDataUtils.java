package com.zrlog.blog.hexo.template.util;

import com.hibegin.common.util.LoggerUtil;
import com.zrlog.common.exception.NotImplementException;
import java.util.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoDataUtils {

    private static boolean isTrue(Value value) {
        // 模拟 JavaScript 的 Truthy 逻辑
        boolean matches = false;
        if (value.isBoolean()) {
            matches = value.asBoolean();
        } else if (value.isNumber()) {
            // JS 中，除了 0 和 NaN 以外的数字都是 true
            matches = value.asDouble() != 0;
        } else if (value.isString()) {
            // JS 中，非空字符串是 true
            matches = !value.asString().isEmpty();
        } else if (value.hasMembers() || value.hasArrayElements()) {
            // 对象和数组在 JS 中永远是 true
            matches = true;
        }
        return matches;
    }

    public static Map<String, Object> wrap(List<?> list) {
        return wrap(list, list.size());
    }

    /** 将普通的 List 封装成带 .data 属性的 Map */
    public static Map<String, Object> wrap(List<?> list, int totalLength) {
        Map<String, Object> wrapper = new HashMap<>();
        List<?> data = list != null ? list : List.of();
        // 如果 list 为 null，我们也给一个空数组，防止 Pug 报错 Cannot convert null to object
        wrapper.put("data", data);
        // 模拟 Hexo 集合常用的 length 属性（可选）
        wrapper.put("length", totalLength);
        wrapper.put(
                "count",
                (ProxyExecutable)
                        args -> {
                            return totalLength;
                        });
        wrapper.put(
                "sort",
                (ProxyExecutable)
                        args -> {
                            // 未实现
                            return wrapper;
                        });
        wrapper.put(
                "toArray",
                (ProxyExecutable)
                        args -> {
                            return list;
                        });

        for (String str : Arrays.asList("each", "forEach")) {
            wrapper.put(
                    str,
                    (ProxyExecutable)
                            args -> {
                                Value callback = args[0];
                                // 假设你的数据存在一个 List 里
                                for (int i = 0; i < data.size(); i++) {
                                    // 手动执行回调，传入 (当前项, 当前索引)
                                    try {
                                        callback.execute(data.get(i), i);
                                    } catch (Exception e) {
                                        LoggerUtil.recordStackTraceMsg(e);
                                    }
                                }
                                return wrapper;
                            });
        }

        wrapper.put(
                "limit",
                (ProxyExecutable)
                        args -> {
                            return wrap(
                                    data.stream()
                                            .limit(Math.min(args[0].asInt(), data.size()))
                                            .toList(),
                                    totalLength);
                        });

        wrapper.put(
                "filter",
                (ProxyExecutable)
                        args -> {
                            List<Object> resultList = new ArrayList<>();
                            Value predicate = args[0];
                            // 遍历原始数据
                            for (int i = 0; i < data.size(); i++) {
                                boolean matches = false;
                                if (predicate.canExecute()) {
                                    Value item = Value.asValue(data.get(i));
                                    // 情况 A: 传入的是函数 .filter(item => item.show !== false)
                                    Value execute = predicate.execute(item, i);
                                    matches = isTrue(execute);
                                } else if (predicate.hasMembers()) {
                                    // 情况 B: 传入的是匹配对象 .filter({ category: 'Tech' })
                                    matches = matches(data.get(i), predicate);
                                }

                                if (matches) {
                                    resultList.add(data.get(i));
                                }
                            }

                            // 关键点：返回一个被 wrap 过的对象，而不是 List 本身
                            // 这样返回的对象依然拥有 .filter, .find, .each, .data 等方法
                            return wrap(resultList, totalLength);
                        });

        wrapper.put(
                "find",
                (ProxyExecutable)
                        args -> {
                            if (args.length == 0 || args[0] == null) return null;
                            Value predicate = args[0];

                            // 情况 A: 传入的是函数 (callback)
                            if (predicate.canExecute()) {
                                for (int i = 0; i < data.size(); i++) {
                                    Value item = Value.asValue(data.get(i));
                                    // 执行函数判断，如果返回 true 则返回该项
                                    if (predicate.execute(item, i).asBoolean()) {
                                        return wrap(
                                                Collections.singletonList(data.get(i)),
                                                totalLength);
                                    }
                                }
                            }
                            // 情况 B: 传入的是匹配对象 (如 {id: 1})
                            else if (predicate.hasMembers()) {
                                for (Object datum : data) {
                                    if (matches(datum, predicate)) {
                                        return wrap(Collections.singletonList(datum), totalLength);
                                    }
                                }
                            }
                            return null;
                        });

        return wrapper;
    }

    private static boolean matches(Object item, Value criteria) {
        if (!(item instanceof Map)) {
            throw new NotImplementException();
        }
        Map map = (Map) item;
        for (String key : criteria.getMemberKeys()) {
            Value criterion = criteria.getMember(key);
            // 1. 处理 {$exists: false} 逻辑
            if (criterion.hasMembers() && criterion.hasMember("$exists")) {
                boolean shouldExist = criterion.getMember("$exists").asBoolean();
                if (map.containsKey(key) != shouldExist) {
                    return false;
                }
                continue; // 处理完操作符，跳过普通比较
            }

            // 2. 处理普通值比较 (包括 {parent: null})
            if (!map.containsKey(key)) {
                // 如果查询要求具体的值，但项里根本没这个键，通常返回 false
                // 除非查询的值本身就是 undefined/null
                return criterion.isNull();
            }

            Object itemValue = map.get(key);

            // 使用 Value 提供的 equals 比较 JS 原始值
            if (!Objects.equals(itemValue, criterion.as(Object.class))) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Object> wrapArticle(Map<String, Object> rawData) {
        // 假设 rawData 里有 tags 和 categories
        if (rawData.get("tags") instanceof List) {
            rawData.put("tags", wrap((List<?>) rawData.get("tags")));
        }
        if (rawData.get("categories") instanceof List) {
            rawData.put("categories", wrap((List<?>) rawData.get("categories")));
        }
        return rawData;
    }
}

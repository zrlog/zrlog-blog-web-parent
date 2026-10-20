package com.zrlog.blog.hexo.template;

import org.graalvm.polyglot.HostAccess;

import java.util.Map;

// 直接继承 HashMap，补上 JS 需要的 has 方法
public class JsMapAdapter {
    private final Map<String, Object> internalMap;

    public JsMapAdapter(Map<String, Object> internalMap) {
        this.internalMap = internalMap;
    }

    // Nunjucks 模板里调用的 menu_map.has(path) 就会路由到这里！
    @HostAccess.Export
    public boolean has(String key) {
        if (key == null) return false;
        return internalMap.containsKey(key);
    }

    // 既然模板里用了 has，通常紧接着就会用 get 获取值，顺便也实现一下
    @HostAccess.Export
    public Object get(String key) {
        return internalMap.get(key);
    }

    // 如果模板里还需要遍历，可能还需要 size() 之类的，按需添加即可
    @HostAccess.Export
    public int size() {
        return internalMap.size();
    }
}
package com.zrlog.blog.polyglot.resource;

import com.hibegin.http.server.util.PathUtil;
import com.zrlog.common.resource.ZrLogResourceLoader;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScriptProvider {
    private static final ScriptProvider instance = new ScriptProvider();
    // 提前把所有脚本读入内存，拒绝文件系统
    private final Map<String, String> cache = new HashMap<>();
    private final Map<String, String> baseLibCache = new HashMap<>();

    public static ScriptProvider getInstance() {
        return instance;
    }

    public void addScript(String name, String scriptPath) {
        if (cache.containsKey(name)) {
            return;
        }
        cache.put(name, ZrLogResourceLoader.read(scriptPath));
    }

    public boolean exists(String id) {
        if (cache.containsKey(id)) {
            return true;
        }
        return baseLibCache.containsKey(id);
    }

    public void addBaseScriptByPath(String name, String path) throws IOException {
        if (baseLibCache.containsKey(name)) {
            return;
        }
        baseLibCache.put(name, new String(PathUtil.getConfInputStream(path).readAllBytes()));
    }

    @HostAccess.Export
    public String load(String id) {
        // 这里可以做路径映射，比如把 'hexo-util' 映射到你准备好的 mock 源码
        String realPath = id.replace("../", "");
        if (baseLibCache.containsKey(realPath)) {
            return baseLibCache.get(realPath);
        }
        return cache.getOrDefault(realPath, "");
    }
}
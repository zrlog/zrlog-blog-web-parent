package com.zrlog.blog.polyglot.njk;

import com.hibegin.common.util.LoggerUtil;
import com.zrlog.common.resource.ZrLogResourceLoader;
import org.graalvm.polyglot.HostAccess;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeJavaLoader {
    private static final Logger LOGGER = LoggerUtil.getLogger(NativeJavaLoader.class);
    // 暴露给 JS 的属性，告诉 Nunjucks 这是同步加载器
    @HostAccess.Export
    public final boolean async = false;
    private final String baseDir;

    public NativeJavaLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    @HostAccess.Export
    public boolean isRelative(String filename) {
        return !ZrLogResourceLoader.exists(baseDir + "/" + filename);
    }

    @HostAccess.Export
    public String resolve(String from, String to) {
        // ... (保持上一版的 resolve 不变，专治带 ./ 和 ../ 的正规军) ...
        if (from == null || to.startsWith("/")) return to.startsWith("/") ? to.substring(1) : to;
        try {
            Path parentDir = Paths.get(from).getParent();
            if (parentDir == null) return to;
            return parentDir.resolve(to).normalize().toString().replace("\\", "/");
        } catch (Exception e) {
            return to;
        }
    }

    // 暴露给 JS 的 getSource 方法
    @HostAccess.Export
    public Map<String, Object> getSource(String name) {
        try {
            Path path = Paths.get(baseDir, name);

            // 读取模板内容
            String content = ZrLogResourceLoader.read(path.toString());

            // 构造 Nunjucks 需要的返回结构
            // GraalVM 会自动将这个 HashMap 转换成 JS 的 Object: { src: "...", path: "...", noCache: true }
            Map<String, Object> result = new HashMap<>();
            result.put("src", content);
            result.put("path", name);

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Java Loader read template failed: " + name, e);
            return null;
        }
    }
}

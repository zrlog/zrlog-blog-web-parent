package com.zrlog.blog.polyglot.sytlus;

import com.zrlog.blog.polyglot.resource.TemplateResolver;
import com.zrlog.common.resource.ResourceScanner;
import com.zrlog.common.resource.ZrLogResourceLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StylusBundler {
    // 匹配 @import "path" 或 @import 'path'
    private static final Pattern IMPORT_PATTERN = Pattern.compile("@import\\s+[\"'](.+?)[\"']");
    private final TemplateResolver templateResolver;

    public StylusBundler(String rootDirPath) {
        this.templateResolver = new TemplateResolver(rootDirPath);
    }

    public String bundle(String fileName) throws Exception {
        return resolveRecursive(fileName, new LinkedHashSet<>());
    }

    private String resolveRecursive(String fileName, Set<String> visited) throws Exception {
        // 1. 路径规范化
        if (!fileName.endsWith(".styl")) fileName += ".styl";
        String path = templateResolver.resolve(fileName);

        // 2. 循环引用检查
        if (visited.contains(path)) {
            return "/* 已跳过循环引用: " + fileName + " */\n";
        }
        visited.add(path);
        String content;
        if (ZrLogResourceLoader.exists(path)) {
            content = ZrLogResourceLoader.read(path);
        } else {
            content = "/* ---Not find path " + path + " --- */\n";
        }
        // 4. 递归处理
        StringBuilder sb = new StringBuilder();
        // 头部标记
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        int lastPos = 0;

        while (matcher.find()) {
            // 添加当前文件在 @import 之前的原文
            sb.append(content, lastPos, matcher.start());

            // 获取 @import 的路径
            String importedPath = matcher.group(1);

            // 处理相对路径：相对于当前正在读取的文件目录
            // 这一步对于深层嵌套的目录结构至关重要
            templateResolver.pushPath(path);
            try {
                if (importedPath.contains("/*")) {
                    String basePath = path.substring(0, path.lastIndexOf("/"));
                    List<String> strings =
                            new ResourceScanner(basePath).listFiles(importedPath.replace("*", ""));
                    for (String string : strings) {
                        // 递归插入被导入文件的内容
                        sb.append(
                                resolveRecursive(string.substring(basePath.length() + 1), visited));
                    }
                } else {
                    // 递归插入被导入文件的内容
                    sb.append(resolveRecursive(importedPath, visited));
                }
            } finally {
                templateResolver.popPath();
            }

            lastPos = matcher.end();
        }
        // 添加剩余部分
        sb.append(content.substring(lastPos));

        // 尾部标记
        // sb.append("\n/* --- END IMPORT: ").append(path).append(" --- */\n");
        // log.info("sb.toString() = {}", sb.toString());

        return sb.toString();
    }
}

package com.zrlog.blog.polyglot.resource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

public class TemplateResolver {
    private static final String CLASSPATH_PREFIX = "classpath:";
    private final Stack<String> directoryStack = new Stack<>();
    private final String baseDir;

    public TemplateResolver(String baseDir) {
        // 确保 baseDir 以协议开头或为绝对路径
        this.baseDir = normalizePath(baseDir);
        this.directoryStack.push(this.baseDir);
    }

    public String resolve(String relativePath) {
        String currentDir = directoryStack.peek();
        String result;

        if (currentDir.startsWith(CLASSPATH_PREFIX)) {
            // Classpath 逻辑：手动拼接路径，避免 Path 对象破坏协议头
            String pathPart = currentDir.substring(CLASSPATH_PREFIX.length());
            // 使用 Paths 处理路径合并逻辑（如 ../ 或 ./）
            Path resolved = Paths.get(pathPart).resolve(relativePath).normalize();
            result = CLASSPATH_PREFIX + resolved.toString().replace("\\", "/");
        } else {
            // 文件系统逻辑
            result = Paths.get(currentDir).resolve(relativePath).normalize().toString();
        }
        return result;
    }

    public void pushPath(String filePath) {
        String dirToPush;
        if (filePath.startsWith(CLASSPATH_PREFIX)) {
            // 如果是 classpath，截取最后一个斜杠之前的部分
            int lastSlash = filePath.lastIndexOf('/');
            dirToPush = (lastSlash > CLASSPATH_PREFIX.length())
                    ? filePath.substring(0, lastSlash)
                    : CLASSPATH_PREFIX;
        } else {
            // 如果是普通文件，取其 Parent
            Path p = Paths.get(filePath).getParent();
            dirToPush = (p != null) ? p.toString() : baseDir;
        }
        directoryStack.push(dirToPush); // 确保进栈的是目录！
    }

    public void popPath() {
        if (directoryStack.size() > 1) directoryStack.pop();
    }

    private String normalizePath(String path) {
        if (path.startsWith(CLASSPATH_PREFIX)) return path;
        return Paths.get(path).toAbsolutePath().toString();
    }
}
# AGENTS.md

`zrlog-blog-web` 是博客前台渲染工程，本地目录可命名为 `zrlog-blog-web-parent`。先读本文件，再按任务读取源码和测试。若本地存在 `.codex/AGENTS.md`，同时读取其中的工作区约定；该目录不提交到仓库。

## 工程职责

- `zrlog-blog-web/`：公开博客页面、公开 API、评论、归档、分类、标签、搜索和 RSS。
- `zrlog-freemarker-template/`：Freemarker 模板渲染。
- `zrlog-polyglot-template/`：Polyglot / Hexo 兼容引擎。
- 主题资源由独立主题 JAR 提供，本工程不复制主题源码。

公开接口见 [API 文档](docs/api/README.md)，主题字段见 [Freemarker 数据契约](docs/freemarker-template-data.md)。普通 JDK 发行包不运行 Polyglot/GraalJS 或携带 Hexo 主题；引擎开发测试不改变该边界。

## 构建与验证

```bash
./mvnw test
./mvnw verify
```

内存预览入口为 `bash shell/memory-run.sh`，运行目录为 `.zrlog-memory/`。保留已有工作区改动，不提交运行目录和本地配置。

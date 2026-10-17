# zrlog-blog-web

`zrlog-blog-web` 是 ZrLog 的博客前台渲染工程，负责文章列表、详情、评论、归档、标签、分类、搜索、RSS 和模板渲染。

## 模块

- `zrlog-blog-web/`：博客页面和公开 API。
- `zrlog-freemarker-template/`：Freemarker 模板适配。
- `zrlog-polyglot-template/`：Polyglot / Hexo 模板兼容。

主题资源通过独立主题 JAR 提供；`static/include/templates/` 是外部 ZIP 主题的运行目录。

## 接入文档

- [博客公开 API](docs/api/README.md)
- [Freemarker 模板数据契约](docs/freemarker-template-data.md)
- [仓库职责与最小验证命令](AGENTS.md)

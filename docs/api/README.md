# 博客公开 API 文档

这里是 `zrlog-blog-web` 公开 `/api`
接口的实现与验证入口。契约统一维护于 [zrlog-api/blog-web.yaml](https://github.com/zrlog/zrlog-api/blob/main/blog-web.yaml)。

- [`openapi.yaml`](openapi.yaml)：统一契约的生成快照，用于独立构建和接口一致性检查，不独立修改。
- `zrlog-blog-web/src/main/java/com/zrlog/blog/web/config/BlogRouters.java`：运行时路由来源。
- `zrlog-blog-web/src/main/java/com/zrlog/blog/web/controller/api`：接口实现来源。

人类可浏览的聚合页面由 `zrlog-www`
提供：`https://www.zrlog.com/docs/api?source=blog-web`。官网保存的契约快照只用于展示和静态发布，业务实现及兼容责任仍属于本仓库，契约定义以
zrlog-api 为准。

当前契约覆盖由 `zrlog-blog-web` 直接提供的公开只读接口。`/api/plugin/*` 和 `/api/p/*` 属于插件协议，不在本契约中；每个插件应维护自己的接口定义。

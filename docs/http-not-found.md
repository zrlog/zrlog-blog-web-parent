# 缺失页面的 HTTP 状态

BlogPageInterceptor 使用渲染器已有的页面对象判断响应：NotFindPageVO 返回 404，其他页面保持原有成功响应。继续渲染控制器选中的模板，兼容将 404 作为 detail 内片段的主题；不强制把所有主题切换到独立 404 文档。

ZrLogTemplateRender 不缓存 NotFindPageVO；BlogStaticResourceInterceptor 不将指定错误文档 /error/404.html 作为 200 静态页返回。空列表、空分类和搜索无结果具有 PageData，保持 200。

依赖 base 的静态抓取修复：仅指定错误文档可以导出 404 响应，其余失效链接不会进入普通缓存。旧 200 错误文档的抓取仍兼容，不修改公开 API、模板字段或数据库。原有静态文件和文章模板规则不变。

## 验证（2026-10-11）

- `mvn -o -q verify` 使用本机已有 JDK 21.0.9 通过：130 项测试、正式包构建及既有 JaCoCo 80% 门禁。JDK 25 与本仓库旧 JaCoCo/Byte Buddy 不兼容，未修改构建依赖或降低门禁。
- 新增真实模板/拦截器测试覆盖普通及 .html 缺失地址、404 文档、无缓存，以及空列表/分类/搜索仍为 200。
- 通过真实 InstallService 启动隔离内存站点，运行目录位于本仓库 target/not-found-review，未重置开发者的 .zrlog-memory。
- 实际 HTTP 检查：/sub/not-a-real-article、/sub/not-a-real-article.html、/sub/error/404.html 返回 404；/sub/、已发布文章、空分类和无结果搜索返回 200，均包含完整主题 HTML。报告保留在 target/not-found-review/http-report.json。
- base 的根路径与 /sub 静态抓取测试确认错误文档仍导出且不递归抓取其链接。

尚未部署线上；独立静态托管需由托管服务将导出文件配置为 404 错误文档。

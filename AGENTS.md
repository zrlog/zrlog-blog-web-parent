# AGENTS.md

这份文档是 AI Agent 在 `zrlog-blog-web` 工程内工作的入口规则。进入本仓库后，先读本文件，再按任务打开更细的源码或文档。

## 工程定位

`zrlog-blog-web` 是 ZrLog 的博客前台渲染层，主要负责：

- 博客前台页面：文章列表、文章详情、评论、归档、标签、分类、搜索、RSS。
- Freemarker `.ftl` 主题渲染。
- Polyglot / Hexo 风格模板兼容。
- 通过 Maven 主题 JAR 与 SPI 加载内置资源；主题源码在 zrlog-extensions 的独立仓库维护。
- Java 单元测试与 JaCoCo 覆盖率检查，目标覆盖率约 80%。

## 目录职责

| 路径 | 职责 |
| --- | --- |
| `zrlog-blog-web/` | 博客前台主模块，包含 Controller、Service、Router、Listener、页面 VO 和公共博客行为。 |
| `zrlog-freemarker-template/` | Freemarker 模板适配与渲染集成。修改 `.ftl` 渲染行为或模板数据暴露时，从这里开始。 |
| `zrlog-polyglot-template/` | Polyglot / Hexo 兼容模板支持；主题 JAR 由 zrlog-main 组装。 |
| `static/include/templates/` | 运行时安装的外部主题目录，不作为主题源码仓库。 |
| `docs/` | 面向人和 AI 的开发文档。 |
| `conf/`、`shell/` | 本地运行配置与辅助脚本。修改前确认不是用户本地配置。 |

## 必读文档

- [博客公开 API 文档](docs/api/README.md)
- [Freemarker 模板数据结构](docs/freemarker-template-data.md)
- [内置主题与 SPI](docs/bundled-themes.md)
- 新建独立主题参考 `templates/README.md` 和对应主题仓库的 `AGENTS.md`。
- `zrlog-ops/docs/repository-structure-guide.md`
- `zrlog-ops/acceptance/zrlog-blog-web.yaml`

模板字段不要猜。文档没有写到的字段，需要检查对应 Java 页面对象、DTO 或已有模板用法。

## 构建与验证

常用命令：

```bash
mvn -q -DskipTests compile
mvn test
mvn verify
```

修改 Java 逻辑或测试时，至少运行 `mvn test`。修改可能影响覆盖率、打包或发布时，运行 `mvn verify`。

根 `pom.xml` 配置了 JaCoCo 覆盖率检查。不要为了通过验证降低覆盖率阈值，应补充聚焦测试。

## Freemarker 模板规则

Freemarker 模板的根对象是页面对象本身，不是包了一层的 `model`。模板中应直接使用：

```ftl
${title}
${log.title}
${data.rows}
${init.tags}
${webs.title}
${_res.search!'搜索'}
```

不要写成：

```ftl
${model.title}
${model.log.title}
```

新增或修改主题时遵守：

- 主题源码在独立仓库；内置 JAR 资源放在 `src/main/resources/include/templates/<id>/`，外部 ZIP 安装后放在 `static/include/templates/<id>/`。
- 尽量拆分为 `header.ftl`、`footer.ftl`、`page.ftl`、`detail.ftl`、`article.ftl`、`comment.ftl`、`pager.ftl`、`plugin.ftl`。
- 主题元信息写入 `template.properties`。
- 用户可见文案写入 `language/i18n_*.properties`，不要散落在模板里。
- 优先复用已有 ZrLog 字段，不要随意发明字段约定。
- 可选字段必须加兜底，例如 `${log.thumbnail!''}`。
- 不要硬编码生产域名或外部资源，除非主题文档明确说明该依赖。

## 主题维护边界

默认主题在 `zrlog-extensions/zrlog-template-default` 维护；Signal Notes 等外部主题在各自仓库维护，视觉约束以对应仓库文档为准。渲染工程保留数据契约、引擎与内存预览入口，不复制主题源码。

## 默认主题内存评审环境

`com.zrlog.blog.MemoryApplication` 位于测试源码，是默认主题页面评审入口，不属于发布产物。

关键约束：

- 启动使用 `bash shell/memory-run.sh`，默认端口为 `7080`，可通过 `--port=17081` 或 `ZRLOG_MEMORY_PORT=17081` 覆盖。
- 安装配置放在 `conf/memory-install.json`，可编辑页面数据放在 `conf/memory-content.json`，本地图片放在 `conf/memory-assets/`。
- 每次启动前重置工程目录下的 `.zrlog-memory/`，通过 install-web 的真实安装链路创建 H2 数据库，再注入页面评审数据。
- 不读取或覆盖仓库已有的 `conf/db.properties`、`conf/install.lock`，也不连接远端测试数据库。
- 评审数据必须覆盖首页、文章详情、分类、标签、搜索、归档、无缩略图、长标题和 Markdown 富内容等真实状态。
- `MemoryApplication` 保持在 `src/test/java`，只使用测试作用域的 install-web 和 H2，不得进入正式 JAR 或改变发布依赖。
- 修改内存评审入口后，至少运行 `MemoryApplicationTest`、`mvn test`，并检查正式 JAR 不包含 `MemoryApplication`。
- `bash shell/memory-run.sh --installed-default` 只运行真实安装链路并保留安装器生成的默认内容；发布主题的
  `previewImages` 素材必须从该模式截图，禁止使用 `conf/memory-content.json` 扩展评审数据伪装安装后首页。

## AI 修改流程

1. 先判断任务属于哪一层：Java 博客行为、Freemarker 渲染、Polyglot / Hexo 兼容，还是纯主题资源。
2. 编辑前读取最小必要文档或源码，不要直接猜实现。
3. 保留工作区里已有的用户改动，不要 reset、restore 或覆盖无关文件。
4. 修改 Java 行为时，同步补充或调整测试；修改公开 `/api` 时同步维护 `docs/api/openapi.yaml`。
5. 修改模板时，先核对 `docs/freemarker-template-data.md` 或 Java DTO 中的数据字段。
6. 修改主题视觉时，优先遵守当前主题文档，不要把已经确认的交互和样式退回去。
7. 完成后运行最小但有效的验证命令。
8. 最终回复说明改了什么文件，以及验证是否通过。

## 常见任务入口

| 任务 | 起点 |
| --- | --- |
| 修改公开文章行为 | `zrlog-blog-web/src/main/java` |
| 新增或修复 Freemarker 数据字段 | `zrlog-freemarker-template/` 和 `docs/freemarker-template-data.md` |
| 新建 `.ftl` 主题 | `templates/README.md` 与独立主题仓库 |
| 维护内置主题 | `docs/bundled-themes.md` 与独立主题仓库 |
| 修复 Hexo 主题兼容 | `zrlog-polyglot-template/` |
| 补充测试和覆盖率 | 各模块的 `src/test` 目录 |

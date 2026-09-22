# 内置主题、Maven JAR 与 SPI

博客工程提供渲染引擎、页面数据和预览入口。主题源码由 `zrlog-extensions` 下的独立仓库维护，通过 Maven Central 引入资源 JAR；不再复制到渲染模块，也不在核心维护主题路径白名单。

所有制品的 groupId 为 `com.hibegin`：

| 仓库 / artifactId | 版本 | 主题 id |
| --- | --- | --- |
| zrlog-template-default | 1.0 | default |
| zrlog-template-www | 3.2 | template-www |
| zrlog-template-hexo-fluid | 1.9.8 | hexo-theme-fluid |
| zrlog-template-hexo-butterfly | 5.5.4-b1 | hexo-theme-butterfly |
| zrlog-template-hexo-shiro | 1.2.0 | hexo-theme-shiro |
| zrlog-template-hexo-next | 8.27.0 | hexo-theme-next |

原生主题命名为 `zrlog-template-<name>`，Hexo 移植制品命名为 `zrlog-template-hexo-<name>`，明确其为 ZrLog 适配而非上游官方 Maven 发布。Hexo 版本严格取自资源内的 `package.json`；原生版本取自 `template.properties`。保留上游来源、许可证和版权信息。Maven 已发布版本不可覆盖。

## 注册与默认选择

公共协议是 `com.hibegin:zrlog-template-spi:1.0.0` 中的 `com.zrlog.theme.spi.BundledThemeProvider`。每个 JAR 提供一个 public 无参 provider，并在 `META-INF/services/com.zrlog.theme.spi.BundledThemeProvider` 登记实现类。provider 声明 id、engine、可选 Hexo adapter 和 isDefault。

资源放在 `include/templates/<id>/`。Java 构建工具 `ThemeResourceIndexer` 扫描 Maven 处理后的资源，自动生成 inventory 和 Native Image 元数据。`BundledThemes` 使用 ServiceLoader 发现 provider，拒绝重复 id、冲突默认项和无效资源清单。安装器通过 `Constants.getDefaultTemplatePath()` 取得默认主题；只有声明 `isDefault=true` 的 provider 决定默认值。

新增内置主题只需引入其 Maven 依赖；仅当增加新的渲染引擎或兼容能力时才需要改引擎代码。本工程仅通过 Freemarker 模块引入 default，保证独立启动可以预览默认页面。WWW 和四款 Hexo 主题仅在 zrlog-main 的 zrlog-web 模块组装：WWW 使用 runtime，四款 Hexo 使用 `${zrlog-polyglot-template-scope}` 与引擎保持一致。渲染工程不携带这些可选资源。外部 ZIP 主题继续使用既有安装机制，JAR 不替代市场 ZIP 协议。

## 本地开发与发布

博客模块的 WWW 渲染回归测试使用 test 作用域依赖，不进入发布产物的运行时依赖。`memory-run.sh` 使用测试 classpath，因此这个评审入口也能加载 WWW 测试资源；正常博客应用只携带 default。

构建顺序：SPI → 六个主题（各自 `./mvnw clean install`）→ zrlog-base → 本工程 → zrlog 主工程。已发布版本可直接从 Central 解析。

独立主题仓库在测试通过后，通过与 POM 一致的 `v<version>` tag 触发签名和 Central 发布。先确保 SPI 已发布，再发布主题。核心应用仍遵守 zrlog-ops Release Order 流程；主题 Maven 发布不会自动登记 zrlog-www 市场。

本工程仍可通过 `bash shell/memory-run.sh --installed-default --port=17096` 查看默认页面。入口走 install-web 的真实安装链路，模板与 CSS 从依赖 JAR 读取，页面数据仍由 Java 页面对象和数据库提供；主题 JAR 不包含演示文章数据库。

## Native Image 验证

每个主题 JAR 自动包含资源与 ServiceLoader provider 构造器注册，无需在宿主中添加主题名称。2026-09-22 使用 GraalVM 25.0.4 将 SPI 的 RegistrySmoke 和六个实际主题 JAR 编译为原生程序（`--no-fallback -O1`），发现 6 个 provider 并成功读取 772 个资源、9,369,924 字节；相同 JVM 检查也通过。这是 SPI/资源层的原生验证，不等同于完整应用 Native Image 构建。

# Freemarker 模板数据结构

这份文档面向 `zrlog-freemarker-template` 下的 `.ftl` 模板开发者，重点说明：

- Freemarker 根对象是什么
- 列表页、详情页、404 页分别能拿到什么字段
- `init.*`、`log.*`、`data.*` 这些常用对象里有哪些字段
- 如何给 AI 一个稳定的提示词，让它快速产出可用模板

如果你只是想快速复制一个主题，先看文末的“AI 提示词模板”。

## 1. 渲染入口

Freemarker 模板的渲染入口在：

- `zrlog-freemarker-template/src/main/java/com/zrlog/blog/freemarker/template/FreemarkerZrLogTemplate.java`

实际调用是：

```java
FreeMarkerUtil.renderToFMByModel(page, pageInfo);
```

这意味着：

- Freemarker 根对象不是一个 `Map`
- 根对象就是 `pageInfo`
- 模板里直接写 `${title}`、`${log.title}`、`${init.tags}`，不是 `${model.title}`

## 2. 页面对象类型

不同页面对应不同的 `pageInfo` 子类。

### 2.1 列表页

常见页面：

- `index.ftl`
- `page.ftl`
- `archives.ftl`
- `tags.ftl`

对应对象：

- `ArticleListPageVO extends BasePageInfo`

额外字段：

- `data`
- `pager`
- `tipsType`
- `tipsName`
- `yurl`

### 2.2 详情页

常见页面：

- `detail.ftl`
- `article.ftl`
- `comment.ftl`

对应对象：

- `ArticleDetailPageVO extends BasePageInfo`

额外字段：

- `log`

### 2.3 404 / 未命中页面

常见页面：

- `404.ftl`

对应对象：

- `NotFindPageVO extends BasePageInfo`

通常只有公共字段，没有 `data` 或 `log`。

## 3. BasePageInfo 公共字段

这些字段几乎所有 Freemarker 页面都可以直接使用。

### 3.1 页面与站点基础信息

- `title`: 页面标题
- `keywords`: 页面关键词
- `description`: 页面描述
- `lang`: 页面语言，如 `zh_CN`
- `local`: 本地化标识
- `template`: 当前模板路径
- `arrangePlugin`: 当前装配插件名

### 3.2 URL 相关

- `url`: 当前模板静态资源路径，主题内常用于 `css/js/images`
- `baseUrl`: 站点根路径，通常以 `/` 结尾
- `templateUrl`: 模板路径
- `searchUrl`: 搜索提交地址
- `rurl`: 当前请求相对地址
- `host`: 当前站点 host
- `requrl`: 当前完整请求 URL
- `reqUriPath`: 当前请求 path
- `reqQueryString`: 当前 query string
- `basePath`
- `baseWithHostPath`
- `contextPath`
- `suffix`: 静态化后缀，常见是 `.html`
- `staticBlog`: 是否静态化
- `staticResourceBaseUrl`: 静态资源基础地址

### 3.3 全局对象

- `init`: 站点初始化数据对象，分类、标签、友情链接、导航等都在这里
- `_res`: 模板语言资源和模板配置资源
- `key`: 搜索关键字

### 3.4 站点配置别名

`BasePageInfo` 提供了几个历史兼容别名，它们都指向同一个 `PublicWebSiteInfo`：

- `webs`
- `webSite`
- `website`
- `WEB_SITE`

建议：

- 新模板优先使用 `webs`
- 老模板兼容时可以继续用 `webSite` 或 `website`

## 4. webs / website 对象

`webs` 的类型是 `PublicWebSiteInfo`。常用字段如下：

- `title`: 站点标题
- `second_title`: 副标题
- `keywords`: 站点关键词
- `description`: 站点描述
- `host`: 站点域名
- `icp`: ICP 备案信息
- `webCm`: 页脚统计代码
- `language`: 语言
- `template`: 当前模板
- `rows`: 列表页分页大小
- `staticResourceHost`: 静态资源域名
- `comment_plugin_name`: 评论插件名称
- `author`: 作者
- `disable_comment_status`: 是否全站关闭评论
- `article_thumbnail_status`: 是否启用文章缩略图
- `generator_html_status`: 是否开启静态化
- `admin_color_primary`: 管理后台主色
- `admin_theme`
- `admin_darkMode`
- `admin_compactMode`
- `system_notification`

模板中常见写法：

```ftl
${webs.title}
${webs.icp!''}
<#if webs.webCm?has_content>${webs.webCm}</#if>
```

## 5. _res 对象

`_res` 是一个 `Map<String, Object>`，里面通常有：

- i18n 文案
- 模板配置项
- 可插入的 HTML 片段

常见字段示例：

- `_res.search`
- `_res.searchTip`
- `_res.category`
- `_res.tag`
- `_res.archive`
- `_res.nextArticle`
- `_res.lastArticle`
- `_res.footerLink`
- `_res.globalStyle`
- `_res.navBarBrand`
- `_res.navBg`
- `_res.detailAd`
- `_res.widgetAd`

推荐写法：

```ftl
${_res.search!'搜索'}
${_res.footerLink!''}
<#if _res.widgetAd?has_content>${_res.widgetAd}</#if>
```

## 6. 列表页对象

列表页的核心对象是 `data`，类型为：

- `PageData<ArticleBasicDTO>`

常用字段：

- `data.rows`: 当前页文章列表
- `data.totalElements`: 总记录数
- `data.page`: 当前页码
- `data.size`: 每页数量
- `data.key`: 搜索关键字

模板中最常见的是：

```ftl
<#if data?? && data.rows?has_content>
  <#list data.rows as log>
    <a href="${log.url}">${log.title}</a>
  </#list>
</#if>
```

### 6.1 单篇列表文章 `log`

`data.rows` 中每项是 `ArticleBasicDTO`。常用字段：

- `log.id`
- `log.logId`
- `log.alias`
- `log.title`
- `log.digest`
- `log.content`
- `log.plain_content`
- `log.markdown`
- `log.releaseTime`
- `log.fullReleaseTime`
- `log.lastUpdateDate`
- `log.fullLastUpdateDate`
- `log.click`
- `log.commentSize`
- `log.typeName`
- `log.typeAlias`
- `log.typeUrl`
- `log.url`
- `log.noSchemeUrl`
- `log.commentUrl`
- `log.header`: 作者头像地址，列表页可用于展示文章作者头像；使用时建议做空值兜底
- `log.thumbnail`
- `log.thumbnailAlt`
- `log.canComment`
- `log.recommended`
- `log.hot`
- `log.privacy`
- `log.rubbish`
- `log.userName`
- `log.tags`

### 6.2 标签对象 `log.tags[*]`

类型是 `ArticleDetailDTO.TagsDTO`。

字段：

- `name`
- `url`

示例：

```ftl
<#if log.tags?has_content>
  <#list log.tags as tag>
    <a href="${tag.url}">#${tag.name}</a>
  </#list>
</#if>
```

## 7. 详情页对象

详情页核心对象是 `log`，类型为：

- `ArticleDetailDTO extends ArticleBasicDTO`

它继承了列表页里的大部分字段，另外还增加了：

- `log.lastLog`
- `log.nextLog`
- `log.comments`
- `log.tocHtml`
- `log.toc`

### 7.1 上一篇 / 下一篇

`log.lastLog` 和 `log.nextLog` 常用字段：

- `title`
- `alias`
- `url`

示例：

```ftl
<a href="${log.lastLog.url}">${log.lastLog.title}</a>
<a href="${log.nextLog.url}">${log.nextLog.title}</a>
```

### 7.2 目录

- `log.tocHtml`: 已生成好的 HTML 目录
- `log.toc`: 结构化目录树

如果只是快速做主题，优先用 `tocHtml`。

## 8. pager 对象

分页对象类型：

- `PagerVO`

常用字段：

- `pager.pageList`
- `pager.pageStartUrl`
- `pager.pageEndUrl`
- `pager.startPage`
- `pager.endPage`

`pager.pageList[*]` 常用字段：

- `url`
- `current`
- `desc`
- `number`
- `prev`
- `next`

示例：

```ftl
<#if pager??>
  <#list pager.pageList as page>
    <a href="${page.url}">${page.desc}</a>
  </#list>
</#if>
```

## 9. init 对象

`init` 的类型是 `BaseDataInitVO`。它是模板侧最重要的全局数据源。

常用字段：

- `init.tags`
- `init.types`
- `init.links`
- `init.plugins`
- `init.archiveList`
- `init.archives`
- `init.webSite`
- `init.hotLogs`
- `init.logNavs`
- `init.typeHotLogs`
- `init.statistics`
- `init.users`
- `init.templateConfigCacheMap`

### 9.1 标签 `init.tags[*]`

类型：`TagDTO`

字段：

- `id`
- `text`
- `count`
- `url`
- `keycode`

### 9.2 分类 `init.types[*]`

类型：`TypeDTO`

字段：

- `id`
- `alias`
- `typeName`
- `remark`
- `amount`
- `typeamount`
- `url`
- `arrange_plugin`

### 9.3 友情链接 `init.links[*]`

类型：`LinkDTO`

字段：

- `id`
- `linkName`
- `url`
- `jumpUrl`
- `alt`
- `sort`
- `icon`

### 9.4 顶部导航 `init.logNavs[*]`

类型：`LogNavDTO`

字段：

- `id`
- `navName`
- `url`
- `jumpUrl`
- `sort`
- `current`
- `icon`

### 9.5 插件列表 `init.plugins[*]`

类型：`PluginDTO`

字段：

- `id`
- `pluginName`
- `isSystem`
- `system`
- `level`
- `content`
- `pTitle`

模板里经常通过 `plugin.pluginName` 判断显示哪个系统组件。

### 9.6 存档 `init.archiveList[*]`

类型：`Archive`

字段：

- `url`
- `text`
- `count`

### 9.7 统计 `init.statistics`

字段：

- `totalArticleSize`
- `totalTagSize`
- `totalTypeSize`

## 10. 现有模板里最常见的字段组合

### 10.1 头部

```ftl
<title>${title!''}</title>
<meta name="description" content="${description!''}"/>
<meta name="keywords" content="${keywords!''}"/>
<link rel="stylesheet" href="${url}/css/style.css"/>
```

### 10.2 导航

```ftl
<#list init.logNavs as nav>
  <a href="${nav.url}">${nav.navName}</a>
</#list>
```

### 10.3 列表页

```ftl
<#list data.rows as log>
  <h2><a href="${log.url}">${log.title}</a></h2>
  <p>${log.digest!''}</p>
  <a href="${log.typeUrl}">${log.typeName}</a>
</#list>
```

### 10.4 详情页

```ftl
<h1>${log.title}</h1>
<div>${log.content!''}</div>
<#if log.tags?has_content>
  <#list log.tags as tag>
    <a href="${tag.url}">#${tag.name}</a>
  </#list>
</#if>
```

### 10.5 侧栏

```ftl
<#list init.types as type>
  <a href="${type.url}">${type.typeName}</a>
</#list>

<#list init.tags as tag>
  <a href="${tag.url}">${tag.text}</a>
</#list>
```

## 11. Freemarker 模板编写建议

- 优先使用 `${xxx!''}` 给字符串兜底，避免空值直接报错。
- 对列表先做 `?has_content` 判断。
- 对对象先做 `??` 判断，尤其是 `log`、`pager`、`log.lastLog`、`log.nextLog`。
- 新模板优先使用 `webs`，不要继续扩散 `website` / `WEB_SITE` 这种历史别名。
- 列表页里优先用 `log.url`、`log.typeUrl`，不要自己手拼路径。
- 详情页里优先用 `log.noSchemeUrl` 做转载链接，用 `log.commentUrl` 做评论提交地址。

## 12. 给 AI 的提示词模板

下面这段可以直接给 AI，让它更快生成 ZrLog Freemarker 模板：

```text
请为 ZrLog 生成一个 Freemarker 模板片段。

约束：
1. Freemarker 根对象就是 pageInfo，不要再包一层 model。
2. 公共字段可直接使用：title, keywords, description, url, baseUrl, searchUrl, init, _res, webs, key。
3. 列表页数据在 data.rows，每项是 log，常用字段有：
   log.title, log.url, log.digest, log.releaseTime, log.typeName, log.typeUrl, log.header, log.thumbnail, log.canComment, log.commentSize, log.tags。
4. 详情页数据在 log，额外字段有：
   log.content, log.lastLog, log.nextLog, log.tocHtml, log.noSchemeUrl。
5. 分类、标签、友情链接、导航分别在：
   init.types, init.tags, init.links, init.logNavs。
6. 所有可能为空的字段都要加 Freemarker 兜底，例如 !''、??、?has_content。
7. 不要发明不存在的字段名，优先复用上面这些字段。
```

## 13. 参考模板

可以直接参考这两套现成主题：

- `zrlog-freemarker-template/src/main/resources/include/templates/default`
- `zrlog-freemarker-template/src/main/resources/include/templates/template-www`

建议阅读顺序：

1. `header.ftl`
2. `page.ftl`
3. `detail.ftl`
4. `article.ftl`
5. `plugin.ftl`
6. `footer.ftl`

## 内置主题资源 JAR

内置主题源码已拆到 `zrlog-extensions` 下的 `zrlog-template-default`、`zrlog-template-www` 与四个 `zrlog-template-hexo-*` 工程。FreeMarker/Polyglot 模块通过固定 Maven 依赖带入资源，正常启动博客工程仍可直接看到页面。

注册契约为 `com.hibegin:zrlog-template-spi:1.0.0`，使用 `ServiceLoader<BundledThemeProvider>`。核心层不再枚举主题目录。原有主题 id/路径保持不变，现有数据库与 Cookie 配置仍有效。新增默认主题通过 provider 的 `isDefault()` 声明，应用 classpath 必须且只能选择一个默认 provider。默认主题路径由 `Constants.getDefaultTemplatePath()` 获取，替代原先会被编译器内联的路径常量。

各主题 JAR 自动携带 Native Image 注册和资源索引；`BlogResourceUtils` 合并索引，`BlogNativeImageUtils` 按 provider 引擎进行预热。Hexo 兼容适配器由 provider 显式声明，不再从主题路径猜测。原有 Zip 安装主题继续从站点文件目录读取，不要求实现 SPI。

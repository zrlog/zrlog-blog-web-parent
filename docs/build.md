# 构建与发布

日常测试使用 `./mvnw test`；涉及打包或发布时运行 `./mvnw verify`，执行现有 JaCoCo 80% 覆盖率检查。

## 开发快照

`main` 的发布工作流使用 `./mvnw -B -ntp -U -Psnapshot clean deploy`，保留完整测试、覆盖率检查和上游快照刷新。

`snapshot` profile 使用 Maven Deploy 3.1.4 的 `deployAtEnd`，全部模块成功后才上传。保留父 POM、3 个模块 JAR 和源码包，省去快照 Javadoc 与 GPG 签名。每个坐标只更新一次版本 metadata，各附件使用同一时间戳。

本工程显式维护该 profile，可与尚未更新发布配置的 base 快照配合使用。Polyglot 子模块保留 Javadoc 的 Java 17 配置，执行阶段由父工程继承，避免在快照构建中重新启用 Javadoc。此改动不改变普通 JDK 与 Native 的引擎和主题依赖边界。

Maven 3.10 起会按仓库 origin 校验凭证；`central` 默认关联的下载域名与 Sonatype 发布域名不同。发布工作流读取实际 Maven 版本，3.10 及以上使用 settings 1.3.0，并为 `central` 显式声明 `https://central.sonatype.com`；旧 Maven 保留 settings 1.0.0，避免不支持 `repositoryOrigins` 的警告。升级 Maven 时须验证 HTTP 认证上传，文件仓库部署无法覆盖凭证校验。

## 正式版

`v*` tag 使用不带 `snapshot` profile 的 `clean deploy`，保留 Javadoc、源码、GPG 签名和 Central Publishing bundle 流程。仅 tag 构建导入 GPG 私钥；`snapshot` profile 会跳过发布版号。`main` 分支仍只在测试和部署成功后通知预览构建。

## 本地部署验证

先安装需要验证的 base 快照，再使用临时文件仓库：

```bash
SNAPSHOT_CHECK_DIR=$(mktemp -d)
./mvnw -B -Psnapshot clean deploy \
  "-DaltSnapshotDeploymentRepository=snapshot-check::file://${SNAPSHOT_CHECK_DIR}"
```

检查 4 个坐标及其源码包、校验和与版本 metadata；最后一个模块测试失败时，目标仓库应为空。正式版的有效 POM 中，所有模块仍应绑定 Javadoc、GPG 和 Central 发布步骤。

2026-09-30 验证：128 项测试及原有覆盖率检查通过，本地完整快照构建与部署耗时 12.6 秒。上传项由此前 CI 的 53 个减少到 18 个，其中 metadata 从 27 次减少到 8 次。消费者从清除相关坐标后的独立缓存重新解析，并验证末模块失败不上传和正式版配置。此前 CI Maven 阶段为 138 秒；本地文件仓库不包含 Sonatype 网络耗时，实际提速需由后续 CI 确认。

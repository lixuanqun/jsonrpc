# GitHub Packages 发布指南

本文档说明如何使用 GitHub Actions 自动构建和发布 Maven 包。

## 一、自动构建

每次推送代码到任意分支时，GitHub Actions 会自动：
1. 使用多个 JDK 版本（8, 11, 17, 21）进行编译测试
2. 打包生成 JAR 文件
3. 上传构建产物（可在 Actions 页面下载）

## 二、发布到 GitHub Packages

### 触发条件

以下情况会触发发布：
- 创建 Release
- 推送 Tag（以 `v` 开头，如 `v1.0.0`）
- 手动触发 workflow

### 发布步骤

#### 方式一：创建 Release（推荐）

1. 在 GitHub 仓库页面，点击 **Releases** → **Create a new release**
2. 创建新 Tag，格式：`v1.0.0`
3. 填写 Release 标题和描述
4. 点击 **Publish release**
5. GitHub Actions 会自动构建并发布到 GitHub Packages

#### 方式二：推送 Tag

```bash
# 创建 Tag
git tag v1.0.0

# 推送 Tag
git push origin v1.0.0
```

#### 方式三：手动触发

1. 进入 GitHub 仓库 → **Actions** 标签
2. 选择 **Maven Build & Publish** workflow
3. 点击 **Run workflow** 按钮
4. 选择分支并运行

## 三、使用已发布的包

### 1. 配置 Maven settings.xml

在 `~/.m2/settings.xml` 中添加：

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>你的GitHub用户名</username>
      <password>你的GitHub Personal Access Token</password>
    </server>
  </servers>
</settings>
```

> **注意**：需要创建 [Personal Access Token](https://github.com/settings/tokens)，勾选 `read:packages` 权限

### 2. 在项目 pom.xml 中添加仓库

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/lixuanqun/jsonrpc</url>
    </repository>
</repositories>
```

### 3. 添加依赖

```xml
<dependency>
    <groupId>com.lixq.jsonrpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 四、发布到 Maven Central（可选）

如需发布到 Maven Central，需要额外配置：

### 1. 注册 Sonatype 账号

访问 https://issues.sonatype.org 注册账号并申请 Group ID

### 2. 配置 GPG 签名

```bash
# 生成 GPG 密钥
gpg --full-generate-key

# 查看密钥
gpg --list-secret-keys --keyid-format=long

# 导出私钥（用于 GitHub Secrets）
gpg --armor --export-secret-keys YOUR_KEY_ID
```

### 3. 配置 GitHub Secrets

在 GitHub 仓库 → Settings → Secrets and variables → Actions 中添加：

| Secret 名称 | 说明 |
|------------|------|
| `OSSRH_USERNAME` | Sonatype 用户名 |
| `OSSRH_TOKEN` | Sonatype Token |
| `GPG_PRIVATE_KEY` | GPG 私钥（完整内容） |
| `GPG_PASSPHRASE` | GPG 密钥密码 |

### 4. 修改版本号并发布

```bash
# 修改 pom.xml 中的版本号（去掉 -SNAPSHOT）
# <version>1.0.0</version>

# 提交并创建 Tag
git add pom.xml
git commit -m "Release v1.0.0"
git tag v1.0.0
git push origin main --tags
```

## 五、版本号规范

建议遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

- **主版本号（MAJOR）**：不兼容的 API 变更
- **次版本号（MINOR）**：向下兼容的功能新增
- **修订号（PATCH）**：向下兼容的问题修正

示例：
- `1.0.0` - 首个稳定版本
- `1.0.1` - Bug 修复
- `1.1.0` - 新增功能
- `2.0.0` - 重大变更

## 六、常见问题

### Q: 发布失败，提示认证错误？

确保 GitHub Actions 有 `packages: write` 权限，检查 workflow 文件中的 `permissions` 配置。

### Q: 如何查看已发布的包？

在 GitHub 仓库页面右侧，点击 **Packages** 即可查看所有已发布的版本。

### Q: 如何删除已发布的版本？

在 Packages 页面找到对应版本，点击 **Delete this version**（需要仓库管理员权限）。

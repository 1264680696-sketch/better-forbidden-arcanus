# 从本地代码到 GitHub

## 先理解四个概念

- 工作区：你正在编辑的源码文件。
- 暂存区：准备放入下一次提交的文件。
- 提交（commit）：一次有说明的本地版本快照。
- GitHub：托管远程 Git 仓库的网站；本地提交不会自动上传。

本项目已初始化为独立仓库，主分支是 `main`。作者名和邮箱仅配置在项目 `.git/config`，不会修改全局设置。

## 每次改完一个功能

在项目根目录运行：

```powershell
git status
git diff
.\scripts\dev.ps1 build
.\scripts\dev.ps1 runGameTestServer
git add src scripts build.gradle gradle.properties README.md CHANGELOG.md docs
git diff --cached
git commit -m "feat: 简洁说明本次完成的功能"
```

首次提交应包含 Gradle Wrapper、settings.gradle、.gitignore 等工程文件。后续按实际修改选择暂存范围；`git diff` 查看未暂存变化，`git diff --cached` 查看即将提交的变化。

`.gitignore` 已排除 `libs/*.jar`、构建输出、缓存和测试存档。这些文件不上传。不要把整个 Minecraft 实例当源码仓库上传。

提交前确保 `git diff --check` 没有空白错误。提交失败时先看错误信息；不要为了通过提交而删除未知文件或覆盖别人改动。

## 第一次上传 GitHub

1. 登录 GitHub，创建一个名为 `better-forbidden-arcanus` 的空仓库。
2. 创建时不要另外生成 README、.gitignore 或许可证，本地已经有这些文件。
3. 复制 GitHub 给出的仓库 HTTPS 地址，用真实地址替换下面的占位值。

```powershell
git remote add origin https://github.com/1264680696-sketch/better-forbidden-arcanus.git
git remote -v
git push -u origin main
```

按 Git Credential Manager 弹出的流程登录 GitHub。不要把密码或访问令牌写入源码、README 或命令文件。之后使用 `git push` 上传新提交。

如果已经配置 origin，用 `git remote -v` 先确认，不重复添加；更换远程地址使用 `git remote set-url origin <真实地址>`。

## 发布更新

采用 `主版本.次版本.修订版本`：小修复可发布 0.1.1，新增一组功能可发布 0.2.0；Minecraft 兼容目标单独写在 Release 说明中。

```powershell
# 修改 mod_version 和更新日志，完成构建/测试后：
git add gradle.properties CHANGELOG.md src docs tools scripts README.md .gitignore '打开指南编辑器.bat'
git commit -m "release: 0.10.0"
git tag -a v0.10.0 -m "Release 0.10.0"
git push
git push origin v0.10.0
```

在 GitHub Releases 中选择这个标签，填写更新内容与游戏/前置版本，上传 `build/libs` 中对应版本的附属 JAR。源码提交、版本标签、Release 和安装 JAR 是不同对象。

2026-10-20 已创建并上传私有仓库：
[1264680696-sketch/better-forbidden-arcanus](https://github.com/1264680696-sketch/better-forbidden-arcanus)。

本地 `main` 跟踪 `origin/main`，0.14.0 安装包通过该仓库的 Releases 下载。私有仓库只有你和获邀协作者可以访问；分享链接不会自动授予权限。旧版本标签目前仅保留在本地，首次上传的是 `v0.14.0`。

GitHub 主页显示名称为 `OriginBlue`，登录用户名与仓库地址仍使用 `1264680696-sketch`。移植内容的权利说明见根目录 `THIRD_PARTY_NOTICES.md`；确认发布权限后再决定公开。

0.2.0 的功能代码、配方、贴图、测试代码和文档属于同一次本地版本快照。版本标签指向对应提交；不会自动上传 GitHub。之后先运行 `git status` 查看分支和改动，再用 `git log --oneline -5` 查看提交、`git tag --list` 查看版本标签。

0.4.0 开始，手册内容也是源码的一部分。编辑器“保存到工程”后先运行 `git status`，用 `git diff -- src/main/resources` 查看改了哪些页面，再提交。编辑器备份、日志、构建缓存及第三方 JAR 不提交。重新编辑手册后请根据更新规模修改版本并创建新标签，不移动已发布的旧版本标签。

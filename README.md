# 更好的禁忌与奥秘

Minecraft 1.21.1 / NeoForge 21.1.251 / Forbidden and Arcanus 2.6.1 附属，版本 0.1.0。

## 首版内容

- 暗黑石与神秘磨制暗黑石：新增 36 条切石机配方，与原有 3 条合计 39 条，全部 1:1。
- 基础块单向加工成对应变体；不含镀金块、符文矿和两种基座。失魂沙系列沿用原配方。
- 蝙蝠翼：80% 掉落判定，成功基础掉落 1 片，保留抢夺的数量奖励。
- 蝙蝠汤：下界疣改为发光浆果或发光地衣；蘑菇、碗、蝙蝠翼不变。
- 食用汤后：夜视 I 90 秒、速度 I 30 秒、发光 I 90 秒，均必定获得。
- 保留饱食属性和食用条件。原汤没有返还碗设置，本版沿用该行为。

完整切石清单见 [STONECUTTING.md](docs/STONECUTTING.md)。

## 安装

客户端和服务端都安装此附属，并准备：

- Minecraft 1.21.1。
- NeoForge 21.1.251（首版测试基线）。
- Forbidden and Arcanus 2.6.1。
- 原 Mod 的必需依赖 Valhelsia Core；本项目测试使用 1.1.5。

将 `build/libs/better_forbidden_arcanus-0.1.0.jar` 放入游戏实例的 `mods` 目录。升级时只保留一个版本的附属 JAR；不要移除原 Mod。

## 从源码构建

在项目目录中打开 PowerShell：

```powershell
# 仅首次运行：从当前实例复制两个开发依赖到 libs。
.\scripts\setup-dev.ps1

# 开发脚本使用 JDK 21，并将 Gradle 缓存放在项目内。
.\scripts\dev.ps1 build
.\scripts\dev.ps1 runGameTestServer
.\scripts\dev.ps1 runClientSmoke
.\scripts\dev.ps1 runClient
```

脚本默认 JDK 路径为 `C:\Program Files\Java\jdk-21`。其他电脑可设置 `$env:BFA_JDK_HOME`；也可以使用 JDK 21 直接运行官方 `gradlew.bat`。需要能够访问 Gradle、NeoForge 和 Mojang 下载服务。

开发脚本会在当前进程中采用已启用的 Windows HTTP 代理，并使用实例中的 `.bfa-tmp` 短临时目录以避免 Java Windows 套接字路径限制；不会修改系统代理设置。

`runClient`、`runServer` 和自动测试使用项目 `runs/` 下的独立目录，不读取整合包的存档。第三方依赖只用于开发运行，不打包进附属。

首次下载 Minecraft 编译环境可能耗时较长。用支持 Gradle 的 Java IDE 打开项目根目录，即可查看源码和运行配置。

## 文件怎么分工

- `src/main/java`：Mod 入口与蝙蝠汤 FOOD 组件修改。
- `src/main/resources/data/better_forbidden_arcanus/recipe`：新增切石配方。
- `src/main/resources/data/forbidden_arcanus`：同 ID 覆盖汤配方和蝙蝠翼附加掉落表。
- `src/gametest`：独立测试 Mod；不进入发行 JAR。
- `scripts/generate_resources.py`：生成配方与掉落资源。
- `scripts/verify_resources.py`：与原 JAR 对照检查配方、范围、重复和掉落规则。

## Git 与版本维护

阅读 [Git 入门与发布](docs/GIT_GUIDE.md)。修改功能后先构建和测试，再提交代码。更新 `gradle.properties` 中的 `mod_version`，同时填写 `CHANGELOG.md`，最后给验证通过的提交打版本标签。

本项目的原始代码当前声明 All Rights Reserved。第三方构建工具保留各自许可证；原 Mod 的源码、贴图和 JAR 不属于本项目的发行内容。

# 更好的禁忌与奥秘

Minecraft 1.21.1 / NeoForge 21.1.251 / Forbidden and Arcanus 2.6.1 附属，版本 0.2.0。

## 炽炉与灵魂扩展

炽炉灵魂槽内的灵魂不再被消耗，槽位决定火焰和速度；取出后立即取消加速。普通灵魂为 1.25 倍，腐化灵魂为 1.5 倍，附魔灵魂为 2.5 倍。永恒版本沿用对应倍率。燃料仍正常消耗。

灵魂槽放入腐化、附魔或其永恒版本后，炽炉可将 **1 个灵魂沙加工为 1 个灵魂，并必定生成 1 份失魂沙残留物**。普通灵魂不能启用此配方。残留物沿用原 Mod 的自动合并流程，有输出空间时，每份转换为 1 个失魂沙。基础加工时间为 100 tick，经验产出为 0。输入、燃料、输出或残留物空间不满足条件时停止；取出合格灵魂会清空此配方的进度。

新增两种赫菲斯托斯锻造炉仪式：

| 产物 | 最低等级 | 中央槽 | 四个基座各放一个 | 辉光 | 灵魂精华 | 血 |
|---|---:|---|---|---:|---:|---:|
| 腐化灵魂 ×1 | II | 灵魂 ×1 | 腐化粉末 | 100 | 10 | 250 |
| 附魔灵魂 ×1 | III | 腐化灵魂 ×1 | 附魔之瓶 | 500 | 20 | 500 |

两种仪式均持续 500 tick，经验精华消耗为 0，不要求增强器，更高等级也可使用。

新增永恒灵魂、永恒腐化灵魂、永恒附魔灵魂，暂时仅创造模式和命令获取。独立创造栏“更好的禁忌与奥秘”以永恒灵魂为图标，后续新增物品也归入该栏。

- 永恒灵魂可替代对应仪式材料；永恒基座材料留在原位，永恒主材料留在中央槽，产物弹出到锻造炉上方。其他材料与精华照常消耗。
- 永恒普通、腐化每次提供 1 点灵魂精华；永恒附魔每次提供 10 点，反复供给时物品保留。容器容量不变。原有灵魂在精华供给和仪式中的消耗规则不变。
- 永恒腐化灵魂可以反复将橡树树苗或枯死灌木转化为生长中的埃德木。
- 原有灵魂绑定水晶仪式接受三种永恒替代材料，其他条件不变。
- 金色描边贴图保留五帧动画；制作来源与编辑提示词见 [ASSETS.md](docs/ASSETS.md)。

命令示例：`/give @s better_forbidden_arcanus:eternal_soul`。另两种 ID 为 `eternal_corrupt_soul` 和 `eternal_enchanted_soul`。

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
- NeoForge 21.1.251。
- Forbidden and Arcanus 2.6.1。
- 原 Mod 的必需依赖 Valhelsia Core；本项目测试使用 1.1.5。

将 `build/libs/better_forbidden_arcanus-0.2.0.jar` 放入游戏实例的 `mods` 目录。升级时只保留一个版本的附属 JAR；不要移除原 Mod。

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

- `src/main/java`：Mod 入口、物品与创造栏注册、蝙蝠汤组件，以及限定目标的 Mixin。
- `src/main/resources/data/better_forbidden_arcanus`：新增切石／炽炉配方、灵魂 Tag、仪式和残留物类型。
- `src/main/resources/data/forbidden_arcanus`：覆盖汤配方、蝙蝠翼掉落表、灵魂绑定仪式，以及追加炽炉灵魂 Tag。
- `src/main/resources/assets/better_forbidden_arcanus`：物品模型、动画贴图和中英文名称。
- `src/gametest`：独立测试 Mod；不进入发行 JAR。
- `scripts/generate_resources.py`：生成配方与掉落资源。
- `scripts/verify_resources.py`：与原 JAR 对照检查配方、范围、重复和掉落规则。

## Git 与版本维护

阅读 [Git 入门与发布](docs/GIT_GUIDE.md)。修改功能后先构建和测试，再提交代码。更新 `gradle.properties` 中的 `mod_version`，同时填写 `CHANGELOG.md`，最后给验证通过的提交打版本标签。

本项目的原创代码当前声明 All Rights Reserved。第三方构建工具保留各自许可证。本版包含基于原灵魂贴图编辑的三张临时衍生贴图，其来源见 ASSETS.md；第三方源码和 JAR 不随附属打包。

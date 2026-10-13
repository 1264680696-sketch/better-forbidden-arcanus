# 指南书与可视化编辑器

指南名称为“禁忌与奥秘”，ID 为 `better_forbidden_arcanus:forbidden_arcanus`。使用帕秋莉默认棕色书本，无序合成：**神秘水晶 ×1 + 书 ×1 → 指南 ×1**。不是紫水晶碎片。指南也可从“更好的禁忌与奥秘”创造栏获得。

0.5.0 已写入完整中文主线和实用支线，目录及内容归类见 [GUIDE_STRUCTURE.md](GUIDE_STRUCTURE.md)。打开原有编辑器后刷新页面即可继续编辑。多方块和熔炼页面可通过“原始 JSON”修改，编辑器会保留它们；原生三维结构、世界投影和最终排版以游戏内 Patchouli 为准。

## 打开编辑器

双击工程根目录的 **打开指南编辑器.bat**，浏览器会打开 `http://127.0.0.1:18741`。程序只在本机运行，不需要账号、联网或额外的 Python 库。使用 Python 3.10 或更新版本；此电脑的 Codex 自带 Python 会被优先使用。保持启动窗口运行，关闭该窗口或按 Ctrl+C 停止服务。

如端口占用，在工程目录的 PowerShell 运行：

```powershell
./tools/guide-editor/start.ps1 -Port 18742
```

服务不会自动重启或关闭占用端口的其他程序。

## 写第一篇条目

1. 左侧点击“＋ 分类”，在页面内的表单填写中文名称，例如“灵魂与仪式”。文件 ID 留空即可自动生成，也可自定义为 `souls`；使用小写英文、数字、下划线或连字符。
2. 修改分类名称、简介和图标。点击“选择物品…”，搜“灵魂”等中文名称，点击物品即可自动填入 ID。
3. 点击“＋ 条目”，填写中文名称并选择所属分类。文件 ID 同样可以自动生成，或自定义为 `soul_basics`；创建后可继续修改名称、图标与排序值。
4. 添加文字、合成配方或物品介绍页面。合成页填写已存在的完整配方 ID，例如 `better_forbidden_arcanus:guide_book`；这只展示配方，不会创建新配方。
5. 用“前移／后移”调整页面顺序，右侧预览当前两页。点击“保存到工程”或 Ctrl+S 保存。

封面简介、名称和版本在“指南设置”编辑。默认页面编辑不清除未知字段；其他页面类型和额外参数可通过“原始 JSON”编辑。编辑 JSON 后要先点击“应用 JSON”再保存。图像页等类型会保留，但暂不提供专用表单或真实渲染。

## 搜索并选择物品

分类、条目的“图标物品 ID”和物品介绍页的“介绍的物品 ID”旁都有“选择物品…”按钮。输入中文名、英文名或完整／部分 ID 搜索，例如 `灵魂`、`Soul`、`minecraft:book`；多个关键词以空格分开，例如“永恒 灵魂”。支持“普通灵魂”“腐化灵魂”“附魔灵魂”等常用叫法，缺少中文翻译时这些已知物品会采用对应中文名称。

默认筛选禁忌与奥秘，可切换到原版、本附属、其他已有 Mod 或“全部 Mod”。卡片显示图标、名称、ID 和所属 Mod，点击后自动填入当前字段并更新预览，最后点击“保存到工程”。取消或 Esc 关闭选择器不会修改原来的 ID；方向键可移动卡片焦点，Enter 选择。结果较多时点击“显示更多”。

目录从本地物品模型与中文／英文名称生成，包含拥有本地模型和名称的物品及方块物品，排除内部模板和无名称的模型变体。工程中的中文覆盖优先生效，第三方 JAR 不会打包进工程。没有本地资源的动态物品仍可手动填写 ID；没有平面贴图的三维模型以占位图标显示，但仍可按名称和 ID 选择。增减 Mod 或物品资源后重启编辑器可刷新目录。

| 正文格式 | 用途 |
|---|---|
| `$(br)` / `$(br2)` | 换行 / 分段 |
| `$(l)文字$()` | 加粗 |
| `$(item)神秘水晶$()` | 物品颜色 |
| `$(4)重点$()` | 深红色 |
| `$(l:better_forbidden_arcanus:soul_basics)链接文字$(/l)` | 同一本书的条目链接 |

预览使用本地 Patchouli 93 的原始书本、分隔线和合成贴图，按游戏的 272×180 书本尺寸、116×156 页面区域及 9 像素行高排版，并从已有开发缓存读取 Minecraft 中文位图字形。未准备游戏资源缓存时会回退到普通字体；不会把第三方贴图或字体复制进工程。

“指南设置”预览首页与分类目录；选择分类时预览简介与条目目录，点击书中的目录项可以跳转。条目的第一页文字页标题采用条目名称，与帕秋莉规则一致。合成页读取本地原版有序／无序配方，显示九宫格、输入与产出图标；Tag 材料暂时用标识表示，悬停可查看 Tag ID。

右侧仍是近似预览，并非帕秋莉排版引擎。资源包替换、复杂格式、动态配方、动画图标及三维物品模型以游戏为准。正文超过页面高度时会显示提示，请手动拆为多页。

新建、删除与重新载入采用页面内表单和确认窗口，无需浏览器原生弹窗，可在 Codex 内嵌浏览器使用。ID 格式或重复问题会直接显示在新建窗口里。

## 在游戏中查看

**正式更新：** 保存后，在工程目录运行 `./scripts/dev.ps1 build`，把 `build/libs` 内新 JAR 替换到游戏 `mods` 中，只保留一个版本。重启游戏，合成指南并右键阅读。客户端与服务端都需要 Patchouli 1.21.1-93-NEOFORGE。

**快速预览正文：** 编辑器的“导出内容预览包”下载 ZIP，将它放到当前游戏的 `resourcepacks` 中并启用。此包只包含分类与条目，需要已经安装本附属及指南。按 F3+T 重载资源，再重新打开指南；如帕秋莉仍显示缓存，可在指南首页按 Shift 点击左下角的铅笔按钮重新载入。取消启用预览包后恢复 JAR 内的正文。发布新 JAR 后也应禁用旧预览包，避免它覆盖新内容。

指南设置 `book.json`、合成配方和 Mod 依赖需要重新构建与重启，内容预览包不会覆盖它们。

## 保存与文件位置

- 定义：`src/main/resources/data/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/book.json`
- 配方：`src/main/resources/data/better_forbidden_arcanus/recipe/guide_book.json`
- 分类：`src/main/resources/assets/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/en_us/categories/`
- 条目：同级 `en_us/entries/`。

`en_us` 是帕秋莉必需的基础内容目录；本项目直接在这里写中文，中文游戏通过回退机制读取同一份内容。编辑器只操作这一份基础手册，不创建 `zh_cn` 副本，避免两份不同步。将来需要多语言时可另外添加翻译目录。

每次保存前，旧文件会打包到 `tools/guide-editor/.backups/<时间>.zip`，该目录已被 Git 忽略。要恢复，停止编辑器，将 ZIP 中 `data`、`assets` 目录合并回 `src/main/resources`；若需要完全恢复，另删除恢复点之后新增的分类／条目文件。Git 也可恢复已提交版本。其他窗口修改过源文件时会拒绝旧内容覆盖；使用“重新载入”后继续编辑。

## 官方教程

- [入门与目录结构](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/getting-started/)
- [指南定义字段](https://vazkiimods.github.io/Patchouli/docs/reference/book-json/)
- [条目字段](https://vazkiimods.github.io/Patchouli/docs/reference/entry-json/)
- [内置页面类型](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/page-types/)
- [正文格式](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/text-formatting/)

官网教程随新版更新。工程按本机安装的 1.21.1-93 JAR 校验：配方使用原版 `minecraft:crafting_shapeless`，结果写入 `patchouli:book` 数据组件，不使用旧教程的 NBT 写法或当前版本不存在的书籍配方序列化器。

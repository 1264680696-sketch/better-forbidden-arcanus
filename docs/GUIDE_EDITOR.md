# 指南书与可视化编辑器

指南名称为“禁忌与奥秘”，ID 为 `better_forbidden_arcanus:forbidden_arcanus`。使用帕秋莉默认棕色书本，无序合成：**神秘水晶 ×1 + 书 ×1 → 指南 ×1**。不是紫水晶碎片。指南也可从“更好的禁忌与奥秘”创造栏获得。

## 打开编辑器

双击工程根目录的 **打开指南编辑器.bat**，浏览器会打开 `http://127.0.0.1:18741`。程序只在本机运行，不需要账号、联网或额外的 Python 库。使用 Python 3.10 或更新版本；此电脑的 Codex 自带 Python 会被优先使用。保持启动窗口运行，关闭该窗口或按 Ctrl+C 停止服务。

如端口占用，在工程目录的 PowerShell 运行：

```powershell
./tools/guide-editor/start.ps1 -Port 18742
```

服务不会自动重启或关闭占用端口的其他程序。

## 写第一篇条目

1. 左侧点击“＋ 分类”，填写文件 ID，例如 `souls`。ID 使用小写英文、数字、下划线或连字符；名称可以写中文。
2. 修改分类名称、简介和图标。物品图标例如 `forbidden_arcanus:soul`。
3. 点击“＋ 条目”，填写文件 ID，例如 `soul_basics`；编辑条目名称、所属分类、图标与排序值。
4. 添加文字、合成配方或物品介绍页面。合成页填写已存在的完整配方 ID，例如 `better_forbidden_arcanus:guide_book`；这只展示配方，不会创建新配方。
5. 用“前移／后移”调整页面顺序，右侧预览当前两页。点击“保存到工程”或 Ctrl+S 保存。

封面简介、名称和版本在“指南设置”编辑。默认页面编辑不清除未知字段；其他页面类型和额外参数可通过“原始 JSON”编辑。编辑 JSON 后要先点击“应用 JSON”再保存。图像页等类型会保留，但暂不提供专用表单或真实渲染。

| 正文格式 | 用途 |
|---|---|
| `$(br)` / `$(br2)` | 换行 / 分段 |
| `$(l)文字$()` | 加粗 |
| `$(item)神秘水晶$()` | 物品颜色 |
| `$(4)重点$()` | 深红色 |
| `$(l:better_forbidden_arcanus:soul_basics)链接文字$(/l)` | 同一本书的条目链接 |

右侧是近似预览，并非帕秋莉排版引擎。字体、换页、物品模型、复杂格式及其他配方显示以游戏为准。过长内容应手动分成多页。物品图标从本地 Mod 的贴图读取；缺失时不影响保存。动画图标及三维方块模型以游戏为准。

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

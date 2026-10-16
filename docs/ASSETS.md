# 贴图来源与编辑

以下永恒灵魂段落仅为0.2.0历史制作记录；相关物品与资源已于0.8.0移除。

## 历史：永恒灵魂临时贴图

三张永恒灵魂贴图的编辑目标来自本地 Forbidden and Arcanus 2.6.1 JAR 的 `assets/forbidden_arcanus/textures/item/soul.png`、`corrupt_soul.png`、`enchanted_soul.png`。原灵魂图案来自 Forbidden and Arcanus，原作者为 stal111；本项目不声明拥有原图案。

使用 Codex 内置 image_gen 图像编辑工具添加金色轮廓，随后按五个动画帧裁切并以最近邻缩小为 16×80 像素。沿用原动画元数据：每帧 3 tick，顺序为 0、1、2、3、4、3、2、1。

最终文件位于 `src/main/resources/assets/better_forbidden_arcanus/textures/item/eternal_soul.png`、`eternal_corrupt_soul.png`、`eternal_enchanted_soul.png`；同目录包含对应 `.png.mcmeta`。

三个编辑目标共用以下提示词：

> Edit this Minecraft item texture spritesheet. Preserve its FIVE animation frames, in their exact order vertically, original soul shape, colors and pixel-art details. ONLY add a ONE ORIGINAL PIXEL thick bright golden outline around the silhouette in each frame. Original image is 16 x 80 pixels: five 16x16 square frames stacked vertically. Return all FIVE edited frames on a genuine transparent background as a single vertical strip; keep frame locations consistent, no text, no decorative additions, no blur or antialiasing. If rendered at higher resolution, use exact nearest-neighbor enlargement with uniform square pixel cells so it can be resized back to 16x80. Treat each square independently; outline must not cross into another frame.


## 0.10.0 贴图导出与旧版资源

资源来源、固定提交与适配范围见 [LEGACY_CONTENT.md](LEGACY_CONTENT.md)。帕秋莉原棕色图集保留外观并复制到附属路径；桶物品模型覆盖只改纹理引用；五级强化边框由代码等价生成18×18透明PNG，改为资源绘制。

运行 `python scripts/export_editable_textures.py` 导出到 `D:\更好的禁忌与奥秘-贴图编辑`，已有编辑PNG不会被覆盖。

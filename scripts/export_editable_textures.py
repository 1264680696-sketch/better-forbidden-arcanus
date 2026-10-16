"""Export editable texture copies without modifying dependency jars or existing edits."""
import argparse
import json
from pathlib import Path
import shutil
import struct

PROJECT = Path(__file__).resolve().parents[1]
RESOURCES = PROJECT / 'src/main/resources'


def export(destination):
    destination = Path(destination)
    editable = destination / '可编辑资源包'
    mapping = []
    files = {
        '火神手记': [
            'assets/better_forbidden_arcanus/textures/gui/forbiddenmicon.png',
            'assets/better_forbidden_arcanus/textures/gui/guide_book.png',
            'assets/better_forbidden_arcanus/textures/item/forbiddenmicon.png',
            'assets/better_forbidden_arcanus/models/item/guide_book.json',
            'assets/better_forbidden_arcanus/models/item/forbiddenmicon_closed.json',
        ],
        '埃德木桶': [
            'assets/better_forbidden_arcanus/textures/item/edelwood_bucket.png',
            'assets/forbidden_arcanus/models/item/edelwood_bucket.json',
        ],
        '强化格子边框': [f'assets/better_forbidden_arcanus/textures/gui/upgrade_border_{i}.png' for i in range(1, 6)],
    }
    for category, paths in files.items():
        for relative in paths:
            source = RESOURCES / relative
            for target in [destination/'原始备份'/category/relative, editable/relative]:
                target.parent.mkdir(parents=True, exist_ok=True)
                if not target.exists():
                    shutil.copyfile(source, target)
            size = ''
            if source.suffix == '.png':
                width, height = struct.unpack('>II', source.read_bytes()[16:24])
                size = f'{width}×{height}'
            mapping.append({'分类': category, '资源路径': relative, '尺寸': size})
    pack = editable/'pack.mcmeta'
    if not pack.exists():
        pack.write_text(json.dumps({'pack': {'pack_format': 34, 'description': '更好的禁忌与奥秘：我的贴图'}}, ensure_ascii=False, indent=2), encoding='utf-8')
    (destination/'资源路径.json').write_text(json.dumps(mapping, ensure_ascii=False, indent=2), encoding='utf-8')
    (destination/'使用说明.txt').write_text('''贴图编辑说明（Minecraft 1.21.1）

请编辑“可编辑资源包”里的 PNG；“原始备份”用于恢复，不要直接编辑备份。
修改这里的文件不会自动覆盖 Mod 或游戏资源。本工具再次导出时也不会覆盖已有 PNG 修改。

应用方法：
1. 保留 PNG 原来的画布尺寸、透明区域和文件名，保存为 RGBA PNG。
2. 把整个“可编辑资源包”文件夹复制到游戏实例的 resourcepacks 文件夹。
3. 在游戏“选项→资源包”启用它；资源包应高于其他改变相同贴图的包。
4. 编辑已启用副本后按 F3+T 重载，或者重新进入游戏。
也可将资源包内 assets/ 和 pack.mcmeta 压成 ZIP；它们必须位于 ZIP 根目录。

强化边框：upgrade_border_1～5 对应白、绿、蓝、紫、红。每张18×18，最外一圈为1像素边框，中间16×16保持透明；这是物品格子边框，不是物品贴图描边。
火神手记：gui/forbiddenmicon.png 为512×256旧书图集，左上264×182区域绘制灰色书页；gui/guide_book.png 用于导航及配方控件，维持Patchouli的UV排布。item/forbiddenmicon.png 用于三维封面。guide_book.json 引用 forbiddenmicon_closed.json 模型。独立禁忌之书物品已删除。
埃德木桶：16×16物品贴图；模型覆盖文件将本体物品指向附属贴图，不改变桶的行为。
模型 JSON 中的纹理坐标、parent 和资源路径通常无需修改。

本文件夹只包含编辑素材与资源包，没有游戏存档、账号信息或本体Mod JAR。
''', encoding='utf-8-sig')
    return mapping


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', default=r'D:\更好的禁忌与奥秘-贴图编辑')
    args = parser.parse_args()
    for asset in export(args.output):
        print(asset['分类'], asset['尺寸'], asset['资源路径'])

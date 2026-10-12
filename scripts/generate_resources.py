"""Generate reviewed 1:1 stonecutting additions/overrides and bat resources."""
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data'
NS = 'forbidden_arcanus'
ADDON = 'better_forbidden_arcanus'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def desired_pairs():
    decorations = ['chiseled_polished_darkstone', 'cracked_polished_darkstone_bricks',
                   'tiled_polished_darkstone_bricks']
    raw_shapes = [f'darkstone_{s}' for s in ('slab', 'stairs', 'wall')]
    polished_shapes = [f'polished_darkstone_{s}' for s in ('slab', 'stairs', 'wall')]
    brick_shapes = [f'polished_darkstone_brick_{s}' for s in ('slab', 'stairs', 'wall')]
    redstone = ['polished_darkstone_button', 'polished_darkstone_pressure_plate']
    matrix = {
        'darkstone': ['polished_darkstone', 'polished_darkstone_bricks'] + decorations + raw_shapes + polished_shapes + brick_shapes + redstone,
        'polished_darkstone': ['polished_darkstone_bricks'] + decorations + polished_shapes + brick_shapes + redstone,
        'polished_darkstone_bricks': decorations + brick_shapes,
        'arcane_polished_darkstone': ['chiseled_arcane_polished_darkstone', 'arcane_polished_darkstone_pillar',
                                     'arcane_polished_darkstone_slab', 'arcane_polished_darkstone_stairs', 'arcane_polished_darkstone_wall'],
    }
    return [(source, target) for source, targets in matrix.items() for target in targets]


def main():
    jars = list((ROOT / 'libs').glob('*forbidden_arcanus-2.6.1.jar'))
    if len(jars) != 1:
        raise RuntimeError('Run scripts/setup-dev.ps1 first.')
    with zipfile.ZipFile(jars[0]) as jar:
        existing = {}
        for path in jar.namelist():
            if path.startswith('data/') and '/recipe/' in path and path.endswith('.json'):
                recipe = json.loads(jar.read(path))
                if recipe.get('type') == 'minecraft:stonecutting':
                    pair = (recipe['ingredient'].get('item', '').removeprefix(NS + ':'),
                            recipe['result']['id'].removeprefix(NS + ':'))
                    existing[pair] = (path, recipe)
        retained = overrides = additions = 0
        rows = []
        for source, target in desired_pairs():
            value = {'type': 'minecraft:stonecutting', 'ingredient': {'item': f'{NS}:{source}'},
                     'result': {'count': 1, 'id': f'{NS}:{target}'}}
            original = existing.get((source, target))
            if original and original[1]['result']['count'] == 1:
                retained += 1
                state = '保留原配方'
            elif original:
                overrides += 1
                state = '覆盖原配方为 1:1'
                write(ROOT / 'src/main/resources' / original[0], value)
            else:
                additions += 1
                state = '新增'
                write(DATA / ADDON / 'recipe' / f'{target}_from_{source}_stonecutting.json', value)
            rows.append(f'| `{source}` | `{target}` | 1:1 | {state} |')

        write(DATA / NS / 'recipe/bat_soup.json', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [{'tag': 'c:mushrooms'},
                            [{'item': 'minecraft:glow_berries'}, {'item': 'minecraft:glow_lichen'}],
                            {'item': 'minecraft:bowl'}, {'item': f'{NS}:bat_wing'}],
            'result': {'count': 1, 'id': f'{NS}:bat_soup'}})
        write(DATA / NS / 'loot_table/entities/additions/bat_wing_addition.json', {
            'type': 'minecraft:entity',
            'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0,
                       'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.8}],
                       'entries': [{'type': 'minecraft:item', 'name': f'{NS}:bat_wing'}],
                       'functions': [{'function': 'minecraft:set_count', 'count': 1, 'add': False},
                                     {'function': 'minecraft:enchanted_count_increase',
                                      'enchantment': 'minecraft:looting',
                                      'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0}}]}],
            'random_sequence': f'{NS}:entities/additions/bat_wing_addition'})
    write(ROOT / 'src/main/resources/pack.mcmeta',
          {'pack': {'pack_format': 48, 'description': '更好的禁忌与奥秘 0.1.0'}})
    expectations = [{'input': f'{NS}:{s}', 'output': f'{NS}:{t}'} for s, t in desired_pairs()]
    write(ROOT / 'src/gametest/resources/stonecutting_expectations.json', expectations)
    doc = ['# 切石机配方清单', '', '暗黑石与神秘磨制暗黑石两组均为基础块单向加工，全部 1:1。', '',
           f'共 {len(rows)} 条有效转换：新增 {additions} 条、覆盖 {overrides} 条、保留 {retained} 条。', '',
           '不包含镀金、符文矿、两种基座；失魂沙系列沿用原 Mod 配方，不添加反向转换。', '',
           '| 输入 ID 尾部 | 输出 ID 尾部 | 比例 | 处理方式 |', '| --- | --- | --- | --- |'] + rows
    (ROOT / 'docs/STONECUTTING.md').write_text('\n'.join(doc) + '\n', encoding='utf-8')
    print(json.dumps({'effective': len(rows), 'new': additions, 'overridden': overrides, 'retained': retained}, indent=2))


if __name__ == '__main__':
    main()

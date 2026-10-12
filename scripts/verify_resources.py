"""Check addon data against the original jar, including excluded content."""
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data'
with zipfile.ZipFile(next((ROOT / 'libs').glob('*forbidden_arcanus-2.6.1.jar'))) as jar:
    original = {path: json.loads(jar.read(path)) for path in jar.namelist()
                if path.startswith('data/') and '/recipe/' in path and path.endswith('.json')}
    effective = dict(original)
    addon_files = list(DATA.rglob('*.json'))
    for path in addon_files:
        key = 'data/' + path.relative_to(DATA).as_posix()
        value = json.loads(path.read_text(encoding='utf-8'))
        if '/recipe/' in key:
            effective[key] = value
        assert 'soulless' not in key, 'Cancelled sandstone edits must not ship'
    expected = json.loads((ROOT / 'src/gametest/resources/stonecutting_expectations.json').read_text())
    assert len(expected) == 39
    added = [d for n, d in effective.items() if n.startswith('data/better_forbidden_arcanus/recipe/')]
    assert len(added) == 36
    bases = {'darkstone', 'polished_darkstone', 'polished_darkstone_bricks', 'arcane_polished_darkstone'}
    for recipe in added:
        assert recipe['type'] == 'minecraft:stonecutting'
        assert recipe['result']['count'] == 1
        source = recipe['ingredient']['item'].split(':')[1]
        target = recipe['result']['id'].split(':')[1]
        assert source in bases and source != target
        assert not any(x in target for x in ('pedestal', 'gilded', 'runic', 'soulless'))
        assert source.startswith('arcane_') == ('arcane_' in target)
        assert f'assets/forbidden_arcanus/blockstates/{target}.json' in jar.namelist()
    for pair in expected:
        matches = [r for r in effective.values() if r.get('type') == 'minecraft:stonecutting'
                   and r['ingredient'].get('item') == pair['input'] and r['result']['id'] == pair['output']]
        assert len(matches) == 1 and matches[0]['result']['count'] == 1
    soup = effective['data/forbidden_arcanus/recipe/bat_soup.json']
    assert len(soup['ingredients']) == 4
    assert {x['item'] for x in soup['ingredients'][1]} == {'minecraft:glow_berries', 'minecraft:glow_lichen'}
    assert soup['result'] == {'count': 1, 'id': 'forbidden_arcanus:bat_soup'}
    loot = json.loads((DATA / 'forbidden_arcanus/loot_table/entities/additions/bat_wing_addition.json').read_text())
    assert len(loot['pools']) == 1
    pool = loot['pools'][0]
    assert pool['rolls'] == 1 and pool['conditions'] == [{'condition': 'minecraft:random_chance', 'chance': 0.8}]
    assert pool['functions'][0] == {'function': 'minecraft:set_count', 'count': 1, 'add': False}
    original_loot = json.loads(jar.read('data/forbidden_arcanus/loot_table/entities/additions/bat_wing_addition.json'))
    assert pool['functions'][1] == original_loot['pools'][0]['functions'][1]
    assert not list(DATA.rglob('*global_loot_modifiers*')), 'Do not register duplicate loot hooks'
print('PASS: 36 new + 3 retained stonecutting recipes, soup recipe, 80% loot gate, preserved looting, cancelled sandstone scope')

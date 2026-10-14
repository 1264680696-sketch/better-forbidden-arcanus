"""Check addon data against the original jar, including excluded content."""
import json
from pathlib import Path
import zipfile
import struct

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
        assert '/recipe/' not in key or 'soulless' not in key, 'Do not add sandstone recipes'
    expected = json.loads((ROOT / 'src/gametest/resources/stonecutting_expectations.json').read_text())
    assert len(expected) == 39
    added = [d for n, d in effective.items() if n.startswith('data/better_forbidden_arcanus/recipe/')
             and d.get('type') == 'minecraft:stonecutting']
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
    soul_recipe = effective['data/better_forbidden_arcanus/recipe/clibano_combustion/soul_from_soul_sand.json']
    assert soul_recipe['type'] == 'forbidden_arcanus:clibano_combustion'
    assert soul_recipe['ingredients'] == {'item': 'minecraft:soul_sand'}
    assert soul_recipe['result'] == {'count': 1, 'id': 'forbidden_arcanus:soul'}
    assert soul_recipe['cooking_time'] == 100 and soul_recipe['experience'] == 0
    assert soul_recipe['residue'] == {'type': 'better_forbidden_arcanus:soulless_sand', 'chance': 1.0}
    residue = json.loads((DATA / 'better_forbidden_arcanus/forbidden_arcanus/residue_type/soulless_sand.json').read_text())
    assert residue['combine_info'] == {'required_amount': 1, 'result': {'count': 1, 'id': 'forbidden_arcanus:soulless_sand'}}
    for name, main, side, tier, aureal, souls, blood in [
            ('corrupt_soul', 'soul', 'forbidden_arcanus:corrupti_dust', 2, 100, 10, 250),
            ('enchanted_soul', 'corrupt_soul', 'minecraft:experience_bottle', 3, 500, 20, 500)]:
        ritual = json.loads((DATA / f'better_forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/{name}.json').read_text())
        assert ritual['forge_tier'] == tier and not ritual.get('match_tier_exact', False)
        assert ritual['main_ingredient'] == {'tag': 'better_forbidden_arcanus:' + main}
        assert ritual['inputs'] == [{'amount': 4, 'ingredient': {'item': side}}]
        assert ritual['essences'] == {'aureal': aureal, 'souls': souls, 'blood': blood, 'experience': 0}
        assert ritual['result']['result_item'] == {'count': 1, 'id': 'forbidden_arcanus:' + name}
    assert not (DATA / 'forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/soul_binding_crystal.json').exists()
    for removed in ('eternal_soul', 'eternal_corrupt_soul', 'eternal_enchanted_soul'):
        assert not list((ROOT / 'src/main/resources/assets/better_forbidden_arcanus').rglob(removed + '*'))
    for name in ('soul','corrupt_soul','enchanted_soul'):
        tag = json.loads((DATA / f'better_forbidden_arcanus/tags/item/{name}.json').read_text())
        assert tag['values'] == ['forbidden_arcanus:' + name]
    for tier in range(1,6):
        ritual = json.loads((DATA / f'better_forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/upgrade_equipment_{tier}.json').read_text())
        assert ritual['forge_tier'] == tier and ritual['match_tier_exact'] and ritual['duration'] == 500
        assert sum(i['amount'] for i in ritual['inputs']) == 8
        assert all(v == 0 for v in ritual['essences'].values())
        assert ritual['enhancers'] == ['forbidden_arcanus:artisan_relic']
        assert ritual['main_ingredient'] == {'type':'better_forbidden_arcanus:forgeable_equipment', 'target_tier':tier}
        assert ritual['result'] == {'type':'better_forbidden_arcanus:upgrade_equipment','tier':tier}
    for name in ('staff','sword','pickaxe','axe','shovel','hoe','scepter'):
        recipe=effective[f'data/better_forbidden_arcanus/recipe/draco_arcanus_{name}.json']
        assert recipe['type']=='minecraft:crafting_shaped' and recipe['result']=={'id':'forbidden_arcanus:draco_arcanus_'+name,'count':1}
        assert 'obsidian_ingot' not in json.dumps(recipe)
    slot = json.loads((DATA / 'better_forbidden_arcanus/curios/slots/relic.json').read_text())
    assert slot['size'] == 1 and slot['validators'] == ['curios:tag'] and not slot['add_cosmetic']
    entity_slots = json.loads((DATA / 'better_forbidden_arcanus/curios/entities/player.json').read_text())
    assert entity_slots == {'entities': ['minecraft:player'], 'slots': ['relic']}
    relics = json.loads((DATA / 'curios/tags/item/relic.json').read_text())['values']
    assert len(relics) == len(set(relics)) == 7
    for locale in ('zh_cn', 'en_us'):
        lang = json.loads((ROOT / f'src/main/resources/assets/better_forbidden_arcanus/lang/{locale}.json').read_text(encoding='utf-8'))
        assert 'curios.identifier.relic' in lang
        assert 'curios.modifiers.relic' in lang
        for item_id in relics:
            assert 'tooltip.better_forbidden_arcanus.relic.' + item_id.split(':')[1] in lang
        assert '25%' in lang['tooltip.better_forbidden_arcanus.relic.artisan_relic'] and 'tooltip.better_forbidden_arcanus.forge_upgrade' in lang
        assert not any(key.startswith('tooltip.better_forbidden_arcanus') and 'tyr' in key for key in lang)
guide = json.loads((DATA / 'better_forbidden_arcanus/patchouli_books/forbidden_arcanus/book.json').read_text(encoding='utf-8'))
assert guide['name'] == '禁忌与奥秘' and guide['use_resource_pack'] is True
assert 'model' not in guide and 'book_texture' not in guide, 'Keep Patchouli default brown book'
guide_recipe = json.loads((DATA / 'better_forbidden_arcanus/recipe/guide_book.json').read_text(encoding='utf-8'))
assert guide_recipe['type'] == 'minecraft:crafting_shapeless'
assert guide_recipe['ingredients'] == [{'item': 'forbidden_arcanus:arcane_crystal'}, {'item': 'minecraft:book'}]
assert guide_recipe['result'] == {'id': 'patchouli:guide_book', 'count': 1, 'components': {'patchouli:book': 'better_forbidden_arcanus:forbidden_arcanus'}}
print('PASS: recipe resources, loot, removed souls and upgrades, relic slots, descriptions and guide')

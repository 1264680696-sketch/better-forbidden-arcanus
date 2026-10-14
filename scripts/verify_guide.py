"""Compare the published manual with the pinned mod's recipes and rituals."""
import collections
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'
BOOK = RESOURCES / 'assets/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/en_us'
NS = 'better_forbidden_arcanus:'


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def indexed(directory):
    return {NS + path.relative_to(directory).with_suffix('').as_posix(): load(path)
            for path in directory.rglob('*.json')}


categories = indexed(BOOK / 'categories')
entries = indexed(BOOK / 'entries')
assert len(categories) >= 9 and len(entries) >= 63
recipes, rituals = {}, {}
for jar_path in sorted((ROOT / 'libs').glob('*.jar')):
    with zipfile.ZipFile(jar_path) as jar:
        for path in jar.namelist():
            if path.startswith('data/') and path.endswith('.json'):
                parts = path.split('/')
                if parts[2] == 'recipe':
                    recipes[parts[1] + ':' + '/'.join(parts[3:])[:-5]] = json.loads(jar.read(path))
                if '/hephaestus_forge/ritual/' in path:
                    rituals[parts[1] + ':' + parts[-1][:-5]] = json.loads(jar.read(path))
for path in (RESOURCES / 'data').rglob('*.json'):
    parts = path.relative_to(RESOURCES / 'data').parts
    if parts[1] == 'recipe':
        recipes[parts[0] + ':' + '/'.join(parts[2:])[:-5]] = load(path)
    if 'hephaestus_forge/ritual/' in path.as_posix():
        rituals[parts[0] + ':' + path.stem] = load(path)

multiblocks = []
for entry_id, entry in entries.items():
    assert entry['category'] in categories, entry_id
    assert not entry.get('advancement'), entry_id
    for page in entry['pages']:
        assert not page.get('advancement'), entry_id
        for target in re.findall(r'\$\(l:([^)]*)\)', page.get('text', '')):
            assert target.split('#')[0] in entries, (entry_id, target)
        if page['type'] in ('patchouli:crafting', 'patchouli:smelting'):
            recipe = recipes[page['recipe']]
            expected = ('minecraft:smelting', 'minecraft:blasting') if page['type'] == 'patchouli:smelting' else ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'better_forbidden_arcanus:elytra_duplication')
            assert recipe['type'] in expected, (entry_id, page['recipe'])
        if page['type'] == 'patchouli:multiblock':
            assert page['enable_visualize'] is True
            block = page['multiblock']
            layers = block['pattern']
            assert len({len(layer) for layer in layers}) == 1
            assert len({len(row) for layer in layers for row in layer}) == 1
            counts = collections.Counter(''.join(row for layer in layers for row in layer))
            assert counts['0'] == 1
            assert set(counts) <= set(block['mapping']) | {' ', '_'}
            multiblocks.append((entry_id, block, counts))
for category in categories.values():
    assert not category.get('advancement')
assert len(multiblocks) == 5
for entry_id, block, counts in multiblocks:
    if entry_id.startswith(NS + 'forge_setup/'):
        assert (counts['P'], counts['A'], counts['C'], counts['H']) == (48, 9, 4, 8)
    elif entry_id.endswith('/clibano'):
        assert (counts['B'], counts['P'], counts['0'], counts[' ']) == (17, 8, 1, 1)
        assert block['symmetrical'] is False, 'Clibano core is on one side; preserve rotation'
    elif entry_id.endswith('/obelisk') or entry_id.endswith('/corrupted_obelisk'):
        assert (counts['X'], counts['0']) == (2, 1)
        assert len(block['pattern']) == 3 and len(block['pattern'][0]) == 3 and len(block['pattern'][0][0]) == 3
        assert block['pattern'][2][1][1] == '0'

# The source registry is the independent oracle: every effective ritual belongs
# to its first usable tier, including exact-tier upgrades and grouped armour.
assert len(rituals) == 30
aliases = {
    'better_forbidden_arcanus:soul': '普通灵魂',
    'better_forbidden_arcanus:corrupt_soul': '腐化灵魂',
    'better_forbidden_arcanus:enchanted_soul': '附魔灵魂',
}
for ritual_id, ritual in rituals.items():
    tier = ritual.get('forge_tier', 1)
    stem = ritual_id.split(':')[1]
    group = 'draco_arcanus' if stem.startswith('draco_arcanus_') else 'tyr' if stem.startswith('tyr_') else stem
    entry_id = NS + f'forge_tier_{tier}/{group}'
    entry = entries[entry_id]
    assert entry['category'] == NS + f'forge_tier_{tier}'
    pages = entry['pages']
    if group in ('draco_arcanus', 'tyr'):
        result_id = ritual['result']['result_item']
        if isinstance(result_id, dict):
            result_id = result_id['id']
        offset = next(i for i, p in enumerate(pages) if p.get('item') == result_id)
        pages = pages[offset:offset + 3]
    text = '\n'.join(p.get('text', '') for p in pages)
    for kind, label in [('aureal', '辉光'), ('souls', '灵魂'), ('blood', '血液'), ('experience', '经验')]:
        value = ritual['essences'].get(kind, 0)
        assert re.search(label + '：' + str(value) + r'(?!\d)', text), (ritual_id, kind)
    materials = next(p['text'] for p in pages if p.get('title') == '仪式材料')
    amounts = [int(n) for n in re.findall(r'×(\d+)', materials)]
    assert amounts == [1] + [row.get('amount', 1) for row in ritual['inputs']], ritual_id
    result_id = ritual['result'].get('result_item')
    if isinstance(result_id, dict):
        result_id = result_id['id']
    if result_id:
        assert any(p.get('item') == result_id for p in pages), ritual_id
    for row in ritual['inputs']:
        ingredient = row['ingredient']
        if 'tag' in ingredient:
            title = aliases[ingredient['tag']]
            assert title + '×' + str(row.get('amount', 1)) in text, ritual_id
        else:
            # Counts and IDs are checked independently of translated wording.
            assert '×' + str(row.get('amount', 1)) in text, ritual_id
    if ritual.get('match_tier_exact'):
        assert '精确' in text or '只能' in text or '仅' in text, ritual_id
    if ritual.get('enhancers'):
        assert '必需增强器：' in text and '无必需增强器。' not in text, ritual_id
assert sum(r['result']['type'] == 'better_forbidden_arcanus:upgrade_equipment' for r in rituals.values()) == 5
print(f'PASS: {len(entries)} guide entries, 30 effective rituals, links, recipe types and five native blueprints')

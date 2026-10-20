"""Local Patchouli authoring tool. Python standard library only; no install needed."""
import argparse
import io
import json
import os
from pathlib import Path
import re
import secrets
import tempfile
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
import webbrowser
import zipfile
from functools import lru_cache

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent.parent
RESOURCES = PROJECT / 'src/main/resources'
NAMESPACE = 'better_forbidden_arcanus'
BOOK_ID = NAMESPACE + ':forbidden_arcanus'
BOOK = RESOURCES / 'data' / NAMESPACE / 'patchouli_books/forbidden_arcanus/book.json'
CONTENT = RESOURCES / 'assets' / NAMESPACE / 'patchouli_books/forbidden_arcanus/zh_cn'
SAFE_ID = re.compile(r'[a-z0-9_-]+(?:/[a-z0-9_-]+)*\Z')
RESOURCE_ID = re.compile(r'[a-z0-9_.-]+:[a-z0-9_./-]+\Z')
TOKEN = os.environ.get('BFA_GUIDE_EDITOR_TOKEN') or secrets.token_urlsafe(32)
ASSET_CACHE = PROJECT / '.gradle-user-home/caches/neoformruntime/assets'
BACKUPS = HERE / '.backups'


@lru_cache(maxsize=1)
def asset_index():
    index = ASSET_CACHE / 'indexes/17.json'
    return json.loads(index.read_text())['objects'] if index.is_file() else {}


def cached_asset(name):
    info = asset_index().get(name)
    if info:
        path = ASSET_CACHE / 'objects' / info['hash'][:2] / info['hash']
        if path.is_file():
            return path.read_bytes()
    return None


@lru_cache(maxsize=1)
def resource_jars():
    paths = list((PROJECT / 'libs').glob('*.jar')) + list((PROJECT.parent.parent / 'mods').glob('*.jar'))
    vanilla = PROJECT / 'build/moddev/artifacts/neoforge-21.1.251.jar'
    if vanilla.is_file():
        paths.append(vanilla)
    client = PROJECT / '.gradle-user-home/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar'
    if client.is_file():
        paths.append(client)
    return tuple(dict.fromkeys(paths))


@lru_cache(maxsize=1024)
def jar_resource(path):
    for jar in resource_jars():
        try:
            with zipfile.ZipFile(jar) as archive:
                try:
                    return archive.read(path)
                except KeyError:
                    pass
        except zipfile.BadZipFile:
            continue
    return cached_asset(path.removeprefix('assets/')) if path.startswith('assets/') else None


def resource(path):
    local = RESOURCES / path
    return local.read_bytes() if local.is_file() else jar_resource(path)


def resource_parts(resource_id):
    if not RESOURCE_ID.fullmatch(resource_id) or '..' in resource_id:
        raise ValueError('资源 ID 不正确。')
    return resource_id.split(':', 1)


@lru_cache(maxsize=8)
def names(namespace):
    raw = resource(f'assets/{namespace}/lang/zh_cn.json') or resource(f'assets/{namespace}/lang/en_us.json')
    return json.loads(raw) if raw else {}


def item_name(item_id):
    namespace, path = resource_parts(item_id)
    lang = names(namespace)
    return lang.get(f'item.{namespace}.{path}', lang.get(f'block.{namespace}.{path}', item_id))


@lru_cache(maxsize=1)
def item_catalog():
    """Index item models with actual name keys, excluding templates/overrides."""
    models, english, chinese, mod_names = set(), {}, {}, {
        'minecraft': '我的世界', 'forbidden_arcanus': '禁忌与奥秘',
        'better_forbidden_arcanus': '更好的禁忌与奥秘', 'patchouli': '帕秋莉手册',
    }
    model_pattern = re.compile(r'assets/([a-z0-9_.-]+)/models/item/([a-z0-9_./-]+)\.json\Z')
    language_pattern = re.compile(r'assets/[^/]+/lang/(en_us|zh_cn)\.json\Z')

    def read_entry(path, raw):
        model = model_pattern.fullmatch(path)
        if model:
            item_id = model[1] + ':' + model[2]
            if '..' not in item_id:
                models.add(item_id)
        locale = language_pattern.fullmatch(path)
        if locale:
            try:
                data = json.loads(raw.decode('utf-8-sig'))
                if isinstance(data, dict):
                    (chinese if locale[1] == 'zh_cn' else english).update(
                        {key: value for key, value in data.items() if isinstance(value, str)})
            except (ValueError, UnicodeDecodeError):
                pass

    # Last source wins for translated names: vanilla, installed mods, dev libs,
    # then project resources (including this addon's original-item name fixes).
    for jar in reversed(resource_jars()):
        try:
            with zipfile.ZipFile(jar) as archive:
                for path in archive.namelist():
                    if model_pattern.fullmatch(path):
                        read_entry(path, b'')
                    elif language_pattern.fullmatch(path):
                        read_entry(path, archive.read(path))
                for manifest in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
                    if manifest in archive.namelist():
                        text = archive.read(manifest).decode('utf-8', errors='replace')
                        for block in text.split('[[mods]]')[1:]:
                            mod_id = re.search(r'^\s*modId\s*=\s*"([a-z0-9_.-]+)"', block, re.M)
                            name = re.search(r'^\s*displayName\s*=\s*"([^"\r\n]+)"', block, re.M)
                            if mod_id and name and '${' not in name[1]:
                                mod_names.setdefault(mod_id[1], name[1])
        except (OSError, zipfile.BadZipFile):
            continue
    for path in asset_index():
        if re.fullmatch(r'[^/]+/lang/(en_us|zh_cn)\.json', path):
            raw = cached_asset(path)
            if raw:
                # Supplement missing translations from Mojang's asset cache.
                try:
                    data = json.loads(raw.decode('utf-8-sig'))
                    locale = chinese if path.endswith('/zh_cn.json') else english
                    for key, value in data.items():
                        if isinstance(value, str):
                            locale.setdefault(key, value)
                except (ValueError, UnicodeDecodeError):
                    pass
    for path in sorted((RESOURCES / 'assets').rglob('*.json')):
        relative = path.relative_to(RESOURCES).as_posix()
        if model_pattern.fullmatch(relative) or language_pattern.fullmatch(relative):
            read_entry(relative, path.read_bytes())

    items, counts = [], {}
    common_names = {
        'forbidden_arcanus:soul': ['普通灵魂'],
        'forbidden_arcanus:corrupt_soul': ['腐化灵魂'],
        'forbidden_arcanus:enchanted_soul': ['附魔灵魂'],
        'forbidden_arcanus:soul_binding_crystal': ['灵魂绑定水晶'],
    }
    for tier, label in enumerate(('一级', '二级', '三级', '四级', '五级'), 1):
        # Forge blocks share a translation key in the original mod.
        common_names[f'forbidden_arcanus:hephaestus_forge_tier_{tier}'] = [f'{label}赫菲斯托斯锻炉']
    for item_id in sorted(models):
        namespace, path = item_id.split(':', 1)
        # The editor targets the current source tree, even while an older addon
        # jar is still installed in the game instance.
        if namespace == 'better_forbidden_arcanus' and not (RESOURCES / f'assets/{namespace}/models/item/{path}.json').is_file():
            continue
        # A model alone can be an internal variant; require a canonical item
        # or block name key, rather than interpreting every translation as an ID.
        keys = [f'item.{namespace}.{path}', f'block.{namespace}.{path}']
        key = next((key for key in keys if key in chinese or key in english), None)
        if key is None and item_id not in common_names:
            continue
        display_name = chinese.get(key) or next(iter(common_names.get(item_id, [])), None) or english.get(key, item_id)
        items.append({'id': item_id, 'name': display_name,
                      'english_name': english.get(key, ''), 'aliases': common_names.get(item_id, []), 'mod': namespace,
                      'mod_name': mod_names.get(namespace, namespace)})
        counts[namespace] = counts.get(namespace, 0) + 1
    mods = [{'id': namespace, 'name': mod_names.get(namespace, namespace), 'count': count}
            for namespace, count in sorted(counts.items(), key=lambda row: (
                row[0] != 'forbidden_arcanus', row[0] != 'minecraft',
                row[0] != 'better_forbidden_arcanus', row[0]))]
    return {'items': items, 'mods': mods}


def crafting_recipe(recipe_id):
    namespace, path = resource_parts(recipe_id)
    raw = resource(f'data/{namespace}/recipe/{path}.json')
    if not raw:
        return {'error': '本地资源中未找到该配方。'}
    recipe = json.loads(raw)
    kind = recipe.get('type')
    if kind not in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'better_forbidden_arcanus:elytra_duplication'):
        return {'error': '此配方类型请在游戏中预览。'}

    def ingredient(value):
        if isinstance(value, list):
            return ingredient(value[0]) if value else ''
        return value.get('item', '#' + value['tag'] if value.get('tag') else '') if isinstance(value, dict) else ''

    grid = [''] * 9
    if kind == 'minecraft:crafting_shapeless':
        for index, entry in enumerate(recipe.get('ingredients', [])[:9]):
            grid[index] = ingredient(entry)
    else:
        for row, line in enumerate(recipe.get('pattern', [])[:3]):
            for col, key in enumerate(line[:3]):
                grid[row * 3 + col] = ingredient(recipe.get('key', {}).get(key, {}))
    result = recipe.get('result', {})
    if isinstance(result, str):
        result = {'id': result}
    item = result.get('id', result.get('item', 'minecraft:air'))
    title = item_name(item)
    if result.get('components', {}).get('patchouli:book') == BOOK_ID:
        title = editor_book()['name']
    return {'grid': grid, 'output': item, 'count': result.get('count', 1), 'name': title,
            'shapeless': kind == 'minecraft:crafting_shapeless'}


@lru_cache(maxsize=1)
def recipe_catalog():
    pattern = re.compile(r'data/([a-z0-9_.-]+)/recipe/([a-z0-9_./-]+)\.json\Z')
    candidates = {}
    for jar in resource_jars():
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                match = pattern.fullmatch(name)
                if match:
                    recipe_id = match[1] + ':' + match[2]
                    if recipe_id not in candidates:
                        try: candidates[recipe_id] = json.loads(archive.read(name))
                        except (ValueError, UnicodeDecodeError): pass
    for path in sorted((RESOURCES / 'data').rglob('*.json')):
        match = pattern.fullmatch(path.relative_to(RESOURCES).as_posix())
        if match:
            candidates[match[1] + ':' + match[2]] = json.loads(path.read_text(encoding='utf-8-sig'))
    result = []
    for recipe_id, recipe in candidates.items():
        if recipe.get('type') not in ('minecraft:crafting_shaped','minecraft:crafting_shapeless'): continue
        output = recipe.get('result',{})
        if isinstance(output,dict): output = output.get('id') or output.get('item')
        if not isinstance(output,str): continue
        result.append({'id':recipe_id,'name':item_name(output),'output':output})
    return {'recipes':sorted(result,key=lambda row:row['id'])}


@lru_cache(maxsize=1)
def unicode_glyphs():
    """Minecraft's locally cached Unifont bitmaps; not copied into the project."""
    raw = cached_asset('minecraft/font/unifont.zip')
    glyphs = {}
    if raw:
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            for filename in archive.namelist():
                if filename.endswith('.hex'):
                    for line in archive.read(filename).decode('ascii').splitlines():
                        code, pixels = line.split(':', 1)
                        glyphs[chr(int(code, 16))] = pixels
    return glyphs


def locale_file(locale):
    return RESOURCES / 'assets' / NAMESPACE / 'lang' / (locale + '.json')


def editor_book():
    book = json.loads(BOOK.read_text(encoding='utf-8-sig'))
    path = locale_file(CONTENT.name)
    translations = json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {}
    for field in ('name', 'landing_text'):
        book[field] = translations.get(book.get(field), book.get(field))
    return book


def files():
    result = [BOOK]
    for locale in ('en_us', 'zh_cn'):
        path = locale_file(locale)
        if path.exists():
            result.append(path)
        for kind in ('categories', 'entries'):
            result.extend(sorted((CONTENT.parent / locale / kind).rglob('*.json')))
    return result


def revision():
    import hashlib
    digest = hashlib.sha256()
    for path in files():
        digest.update(str(path.relative_to(RESOURCES)).encode())
        digest.update(path.read_bytes())
    return digest.hexdigest()


def load():
    data = {'book': editor_book()}
    for kind in ('categories', 'entries'):
        data[kind] = {p.relative_to(CONTENT / kind).with_suffix('').as_posix():
                      json.loads(p.read_text(encoding='utf-8-sig'))
                      for p in sorted((CONTENT / kind).rglob('*.json'))}
    return {'document': data, 'revision': revision(), 'token': TOKEN, 'bookId': BOOK_ID, 'projectRoot': str(PROJECT), 'contentRoot': str(RESOURCES)}


def validate(data):
    if not isinstance(data, dict) or set(data) != {'book', 'categories', 'entries'}:
        raise ValueError('文档结构不正确。')
    book = data['book']
    if not isinstance(book, dict) or not isinstance(book.get('name'), str) or not book['name'].strip():
        raise ValueError('请填写指南名称。')
    if not isinstance(book.get('landing_text'), str) or not isinstance(book.get('version'), (str, int)):
        raise ValueError('指南简介和版本格式不正确。')
    if book.get('use_resource_pack') is not True:
        raise ValueError('本版本的指南需要 use_resource_pack=true。')
    if not data['categories'] or not data['entries']:
        raise ValueError('请至少保留一个分类和一个条目。')
    for kind in ('categories', 'entries'):
        if not isinstance(data[kind], dict):
            raise ValueError('分类或条目格式不正确。')
        for key, value in data[kind].items():
            if not SAFE_ID.fullmatch(key) or not isinstance(value, dict):
                raise ValueError('文件 ID 仅支持小写字母、数字、下划线、连字符及子目录。')
            if not isinstance(value.get('name'), str) or not value['name'].strip():
                raise ValueError(f'{key} 缺少名称。')
            if not isinstance(value.get('icon'), str) or not value['icon'].strip():
                raise ValueError(f'{key} 缺少图标。')
            if kind == 'categories':
                if not isinstance(value.get('description'), str):
                    raise ValueError(f'{key} 缺少分类简介。')
            else:
                category = value.get('category', '').removeprefix(NAMESPACE + ':')
                if value.get('category') != NAMESPACE + ':' + category or category not in data['categories']:
                    raise ValueError(f'{key} 的分类不存在。')
                if not isinstance(value.get('pages'), list) or not value['pages']:
                    raise ValueError(f'{key} 至少需要一页。')
                for page in value['pages']:
                    if not isinstance(page, dict) or not isinstance(page.get('type'), str) or not RESOURCE_ID.fullmatch(page['type']):
                        raise ValueError(f'{key} 的页面需要完整类型 ID，例如 patchouli:text。')
                    for field in ('text', 'title', 'recipe', 'recipe2', 'item'):
                        if field in page and not isinstance(page[field], str):
                            raise ValueError(f'{key} 的 {field} 必须是文本。')
                    if page['type'] == 'patchouli:crafting' and not RESOURCE_ID.fullmatch(page.get('recipe', '')):
                        raise ValueError(f'{key} 的合成页需要完整配方 ID。')
                    if page['type'] == 'patchouli:spotlight' and not page.get('item'):
                        raise ValueError(f'{key} 的物品页需要物品 ID。')
    return data


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf-8')


def target_files(data):
    # Preserve the other translation; canonical English IDs also index new Chinese entries.
    targets = {p: p.read_bytes() for p in files() if p != BOOK and not p.is_relative_to(CONTENT)}
    book = dict(data['book'])
    raw = json.loads(BOOK.read_text(encoding='utf-8-sig')) if BOOK.exists() else data['book']
    path = locale_file(CONTENT.name)
    language = json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {}
    for field in ('name', 'landing_text'):
        key = raw.get(field, '')
        if key.startswith('book.' + NAMESPACE + '.'):
            language[key] = book[field]
            book[field] = key
            targets[path] = encoded(language)
    targets[BOOK] = encoded(book)
    other = CONTENT.parent / ('en_us' if CONTENT.name == 'zh_cn' else 'zh_cn')
    for kind in ('categories', 'entries'):
        for key, value in data[kind].items():
            path = CONTENT / kind / (key + '.json')
            if not path.resolve().is_relative_to(CONTENT.resolve()):
                raise ValueError('保存路径超出手册目录。')
            targets[path] = encoded(value)
            counterpart = other / kind / (key + '.json')
            if CONTENT.name == 'zh_cn' and not counterpart.exists():
                targets[counterpart] = encoded(value)
        for old in (CONTENT / kind).rglob('*.json'):
            if old not in targets:
                targets.pop(other / kind / old.relative_to(CONTENT / kind), None)
    return targets


def save(data, expected):
    if expected != revision():
        raise FileExistsError('源文件已被其他窗口或工具修改。请先重新载入，避免覆盖。')
    targets = target_files(validate(data))
    old = {p: p.read_bytes() for p in files()}
    # Leave untouched files byte-for-byte intact, including user-authored drafts.
    for path in targets.keys() & old.keys():
        if json.loads(old[path].decode('utf-8-sig')) == json.loads(targets[path]):
            targets[path] = old[path]
    backup = BACKUPS / (str(time.time_ns()) + '.zip')
    backup.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path, content in old.items():
            archive.writestr(path.relative_to(RESOURCES).as_posix(), content)
    staged = {}
    try:
        for path, content in targets.items():
            if old.get(path) == content:
                continue
            path.parent.mkdir(parents=True, exist_ok=True)
            with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as handle:
                handle.write(content)
                staged[path] = Path(handle.name)
        for path, temporary in staged.items():
            os.replace(temporary, path)
        for path in old.keys() - targets.keys():
            path.unlink()
    except Exception:
        for path, content in old.items():
            path.write_bytes(content)
        for path in targets.keys() - old.keys():
            path.unlink(missing_ok=True)
        raise
    finally:
        for temporary in staged.values():
            temporary.unlink(missing_ok=True)
    return {'revision': revision(), 'message': '已保存到工程；修改前的内容已自动备份。'}


def texture(resource_id):
    """Display only verified resource textures from local mod JARs / project."""
    if not RESOURCE_ID.fullmatch(resource_id) or '..' in resource_id:
        return None
    namespace, name = resource_id.split(':', 1)
    candidates = [f'assets/{namespace}/textures/item/{name}.png',
                  f'assets/{namespace}/textures/block/{name}.png']
    for candidate in candidates:
        raw = resource(candidate)
        if raw:
            return raw
    # Generated/enhancer items often reference a differently named texture.
    # Resolve the first display layer through model parents instead of guessing
    # that every item's texture filename must equal its registry ID.
    model_id, textures, visited = namespace + ':item/' + name, {}, set()
    for _ in range(8):
        if model_id in visited:
            break
        visited.add(model_id)
        try:
            model_namespace, model_path = resource_parts(model_id)
            raw = resource(f'assets/{model_namespace}/models/{model_path}.json')
            if not raw:
                break
            model = json.loads(raw)
            for key, value in model.get('textures', {}).items():
                if isinstance(value, str):
                    textures.setdefault(key, value)
            parent = model.get('parent')
            if not isinstance(parent, str):
                break
            model_id = parent if ':' in parent else 'minecraft:' + parent
        except (ValueError, TypeError, AttributeError):
            break
    for key in dict.fromkeys(['layer0', 'all', 'particle', 'front', 'side'] + list(textures)):
        location = textures.get(key, '')
        for _ in range(8):
            if not location.startswith('#'):
                break
            location = textures.get(location[1:], '')
        if not location or location.startswith('#'):
            continue
        try:
            texture_namespace, texture_path = resource_parts(location if ':' in location else 'minecraft:' + location)
            raw = resource(f'assets/{texture_namespace}/textures/{texture_path}.png')
            if raw:
                return raw
        except ValueError:
            continue
    if resource_id == 'patchouli:guide_book':
        return resource('assets/patchouli/textures/item/book_brown.png')
    return None


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def send(self, content, status=200, mime='application/json; charset=utf-8'):
        if isinstance(content, dict):
            content = encoded(content)
        self.send_response(status)
        self.send_header('Content-Type', mime)
        self.send_header('Content-Length', str(len(content)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Security-Policy', "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'")
        self.end_headers()
        self.wfile.write(content)

    def local(self):
        return self.headers.get('Host') == f'127.0.0.1:{self.server.server_port}'

    def do_GET(self):
        if not self.local():
            return self.send({'error': '请从本机地址访问。'}, 403)
        route = urlparse(self.path)
        try:
            if route.path == '/api/document':
                with self.server.save_lock:
                    return self.send(load())
            if route.path == '/api/export':
                output = io.BytesIO()
                with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
                    archive.writestr('pack.mcmeta', encoded({'pack': {'pack_format': 34, 'description': '禁忌与奥秘 · 手册内容预览'}}))
                    for path in files():
                        if path.is_relative_to(CONTENT.parent) or path.parent == locale_file(CONTENT.name).parent:
                            archive.writestr(path.relative_to(RESOURCES).as_posix(), path.read_bytes())
                return self.send(output.getvalue(), mime='application/zip')
            if route.path == '/api/icon':
                icon = texture(parse_qs(route.query).get('id', [''])[0])
                return self.send(icon or b'', 200 if icon else 404, 'image/png')
            if route.path == '/api/items':
                return self.send(item_catalog())
            if route.path == '/api/recipes':
                return self.send(recipe_catalog())
            if route.path == '/api/texture':
                namespace, path = resource_parts(parse_qs(route.query).get('id', [''])[0])
                if not path.startswith('textures/') or not path.endswith('.png'):
                    return self.send({'error': '仅支持资源中的 PNG 贴图。'}, 400)
                raw = resource(f'assets/{namespace}/{path}')
                return self.send(raw or b'', 200 if raw else 404, 'image/png')
            if route.path == '/api/recipe':
                return self.send(crafting_recipe(parse_qs(route.query).get('id', [''])[0]))
            if route.path == '/api/glyphs':
                chars = parse_qs(route.query).get('chars', [''])[0]
                if len(chars) > 4096:
                    return self.send({'error': '一次最多查询 4096 个字形。'}, 400)
                glyphs = unicode_glyphs()
                return self.send({char: glyphs[char] for char in set(chars) if char in glyphs})
            names = {'/': 'index.html', '/style.css': 'style.css', '/app.js': 'app.js', '/visual.js': 'visual.js'}
            if route.path in names:
                name = names[route.path]
                mime = {'index.html': 'text/html', 'style.css': 'text/css', 'app.js': 'application/javascript', 'visual.js': 'application/javascript'}[name]
                return self.send((HERE / name).read_bytes(), mime=mime + '; charset=utf-8')
            self.send({'error': '页面不存在。'}, 404)
        except Exception as error:
            self.send({'error': str(error)}, 500)

    def do_POST(self):
        if not self.local() or self.headers.get('X-Editor-Token') != TOKEN:
            return self.send({'error': '请在编辑器中保存。'}, 403)
        origin = self.headers.get('Origin')
        if origin and origin != f'http://127.0.0.1:{self.server.server_port}':
            return self.send({'error': '不接受外部页面的保存请求。'}, 403)
        if self.path != '/api/save':
            return self.send({'error': '接口不存在。'}, 404)
        try:
            size = int(self.headers.get('Content-Length', '0'))
            if size <= 0 or size > 4 * 1024 * 1024:
                raise ValueError('文档大小必须在 4 MB 以内。')
            payload = json.loads(self.rfile.read(size))
            with self.server.save_lock:
                self.send(save(payload['document'], payload['revision']))
        except FileExistsError as error:
            self.send({'error': str(error)}, 409)
        except (ValueError, KeyError, TypeError) as error:
            self.send({'error': str(error)}, 400)
        except Exception as error:
            self.send({'error': '保存失败：' + str(error)}, 500)


if __name__ == '__main__':
    import threading
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=18741)
    parser.add_argument('--open', action='store_true')
    parser.add_argument('--content-root', type=Path, help='Use an isolated copy of the resource folder for authoring or verification.')
    parser.add_argument('--lang', choices=('zh_cn', 'en_us'), default='zh_cn', help='Handbook language to edit (default: zh_cn).')
    args = parser.parse_args()
    if args.content_root:
        RESOURCES = args.content_root.resolve()
        BOOK = RESOURCES / 'data' / NAMESPACE / 'patchouli_books/forbidden_arcanus/book.json'
        CONTENT = RESOURCES / 'assets' / NAMESPACE / 'patchouli_books/forbidden_arcanus/zh_cn'
        BACKUPS = RESOURCES.parent / '.guide-backups'
        if not BOOK.is_file():
            raise SystemExit('The resource copy does not contain this guide book.')
    CONTENT = CONTENT.parent / args.lang
    try:
        server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    except OSError:
        raise SystemExit('端口已被使用。可关闭先前编辑器，或使用 --port 18742。')
    server.save_lock = threading.Lock()
    url = f'http://127.0.0.1:{server.server_port}'
    print('禁忌与奥秘 · 手册编辑器 ' + url, flush=True)
    print('仅在本机运行；关闭此进程即可停止。', flush=True)
    if args.open:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

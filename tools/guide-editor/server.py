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
CONTENT = RESOURCES / 'assets' / NAMESPACE / 'patchouli_books/forbidden_arcanus/en_us'
SAFE_ID = re.compile(r'[a-z0-9_-]+(?:/[a-z0-9_-]+)*\Z')
RESOURCE_ID = re.compile(r'[a-z0-9_.-]+:[a-z0-9_./-]+\Z')
TOKEN = os.environ.get('BFA_GUIDE_EDITOR_TOKEN') or secrets.token_urlsafe(32)
ASSET_CACHE = PROJECT / '.gradle-user-home/caches/neoformruntime/assets'


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


def crafting_recipe(recipe_id):
    namespace, path = resource_parts(recipe_id)
    raw = resource(f'data/{namespace}/recipe/{path}.json')
    if not raw:
        return {'error': '本地资源中未找到该配方。'}
    recipe = json.loads(raw)
    kind = recipe.get('type')
    if kind not in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless'):
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
        title = json.loads(BOOK.read_text(encoding='utf-8-sig'))['name']
    return {'grid': grid, 'output': item, 'count': result.get('count', 1), 'name': title,
            'shapeless': kind == 'minecraft:crafting_shapeless'}


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


def files():
    return [BOOK] + [p for kind in ('categories', 'entries')
                     for p in sorted((CONTENT / kind).rglob('*.json'))]


def revision():
    import hashlib
    digest = hashlib.sha256()
    for path in files():
        digest.update(str(path.relative_to(RESOURCES)).encode())
        digest.update(path.read_bytes())
    return digest.hexdigest()


def load():
    data = {'book': json.loads(BOOK.read_text(encoding='utf-8-sig'))}
    for kind in ('categories', 'entries'):
        data[kind] = {p.relative_to(CONTENT / kind).with_suffix('').as_posix():
                      json.loads(p.read_text(encoding='utf-8-sig'))
                      for p in sorted((CONTENT / kind).rglob('*.json'))}
    return {'document': data, 'revision': revision(), 'token': TOKEN, 'bookId': BOOK_ID}


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
    targets = {BOOK: encoded(data['book'])}
    for kind in ('categories', 'entries'):
        for key, value in data[kind].items():
            path = CONTENT / kind / (key + '.json')
            if not path.resolve().is_relative_to(CONTENT.resolve()):
                raise ValueError('保存路径超出手册目录。')
            targets[path] = encoded(value)
    return targets


def save(data, expected):
    if expected != revision():
        raise FileExistsError('源文件已被其他窗口或工具修改。请先重新载入，避免覆盖。')
    targets = target_files(validate(data))
    old = {p: p.read_bytes() for p in files()}
    backup = HERE / '.backups' / (str(time.time_ns()) + '.zip')
    backup.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path, content in old.items():
            archive.writestr(path.relative_to(RESOURCES).as_posix(), content)
    staged = {}
    try:
        for path, content in targets.items():
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
                        if path.is_relative_to(CONTENT):
                            archive.writestr(path.relative_to(RESOURCES).as_posix(), path.read_bytes())
                return self.send(output.getvalue(), mime='application/zip')
            if route.path == '/api/icon':
                icon = texture(parse_qs(route.query).get('id', [''])[0])
                return self.send(icon or b'', 200 if icon else 404, 'image/png')
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
            names = {'/': 'index.html', '/style.css': 'style.css', '/app.js': 'app.js'}
            if route.path in names:
                name = names[route.path]
                mime = {'index.html': 'text/html', 'style.css': 'text/css', 'app.js': 'application/javascript'}[name]
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
    args = parser.parse_args()
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

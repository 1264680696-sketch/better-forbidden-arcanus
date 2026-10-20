"""Validate bilingual resources without changing them or writing reports."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
BASE=ROOT/'assets/better_forbidden_arcanus'
HAN=re.compile('[\u4e00-\u9fff]')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
en=load(BASE/'lang/en_us.json');zh=load(BASE/'lang/zh_cn.json')
assert en.keys()==zh.keys()
for k in en:
 assert not HAN.search(en[k]),k
 assert re.findall(r'%[0-9$]*s',en[k])==re.findall(r'%[0-9$]*s',zh[k]),k
book=BASE/'patchouli_books/forbidden_arcanus'
a={p.relative_to(book/'en_us').as_posix():load(p) for p in (book/'en_us').rglob('*.json')}
b={p.relative_to(book/'zh_cn').as_posix():load(p) for p in (book/'zh_cn').rglob('*.json')}
assert a.keys()==b.keys()
text={'name','description','text','title','result_summary','duration_text','aureal_text','souls_text','blood_text','experience_text','studio_forge_warnings'}
def structural(x):
 if isinstance(x,dict):return {k:structural(v) for k,v in x.items() if k not in text}
 if isinstance(x,list):return [structural(v) for v in x if not (isinstance(v,dict) and v.get('type')=='patchouli:text' and v.get('title')=='Continued')]
 return x
for n in a:
 assert not HAN.search(json.dumps(a[n],ensure_ascii=False)),n
 assert structural(a[n])==structural(b[n]),n
links=lambda d: sorted(re.findall(r'\$\(l:([^)]*)\)',json.dumps(d,ensure_ascii=False)))
assert links(a)==links(b)
common=load(ROOT/'data/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/book.json')
assert common['name'] in en and common['landing_text'] in en
print(f'Bilingual resources valid: {len(en)} language keys, {len(a)} handbook files per language.')

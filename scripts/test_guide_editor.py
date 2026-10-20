"""Exercise transactional guide saves on temporary copies, never on the actual guide."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('guide_editor', Path(__file__).resolve().parents[1] / 'tools/guide-editor/server.py')
editor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(editor)

class GuideSaveTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.settings = {key:getattr(editor,key) for key in ('RESOURCES','BOOK','CONTENT','BACKUPS')}
        editor.RESOURCES = root / 'resources'
        editor.BOOK = editor.RESOURCES / 'data/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/book.json'
        editor.CONTENT = editor.RESOURCES / 'assets/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/en_us'
        editor.BACKUPS = root / 'backups'
        self.data = {'book':{'name':'禁忌与奥秘','landing_text':'开始阅读','version':'0.9.4','use_resource_pack':True},'categories':{'first':{'name':'入门','description':'','icon':'minecraft:book'}},'entries':{'draft':{'name':'用户草稿','category':'better_forbidden_arcanus:first','icon':'minecraft:book','pages':[{'type':'patchouli:multiblock','text':'$(l:first)跳转$(/l)','multiblock':{'pattern':[['0']],'mapping':{'0':'minecraft:stone'}},'custom_field':{'keep':True}}]}}}
        for path,raw in editor.target_files(self.data).items():
            path.parent.mkdir(parents=True,exist_ok=True)
            # Preserve nonstandard whitespace and a BOM: untouched drafts must not be reformatted.
            path.write_bytes(b'\xef\xbb\xbf'+raw.replace(b'\n',b'\r\n'))
        self.original={p:p.read_bytes() for p in editor.files()}
    def tearDown(self):
        for key,value in self.settings.items():setattr(editor,key,value)
        self.temp.cleanup()
    def test_changed_file_only_and_unknown_page_fields(self):
        data=copy.deepcopy(editor.load()['document']);data['book']['name']='更名'
        editor.save(data,editor.revision())
        for path,raw in self.original.items():
            if path!=editor.BOOK:self.assertEqual(path.read_bytes(),raw)
        self.assertEqual(editor.load()['document']['entries']['draft'],self.data['entries']['draft'])
        self.assertTrue(list(editor.BACKUPS.glob('*.zip')))
    def test_create_edit_delete_and_reload(self):
        data=copy.deepcopy(self.data)
        data['categories']['new']={'name':'新章节','description':'中文简介','icon':'minecraft:book'}
        data['entries']['new']={'name':'新条目','category':'better_forbidden_arcanus:new','icon':'minecraft:book','pages':[{'type':'patchouli:text','text':'$(l)第一行$()$(br)第二行'}]}
        editor.save(data,editor.revision());self.assertEqual(editor.load()['document'],data)
        editor.save(self.data,editor.revision());self.assertFalse((editor.CONTENT/'entries/new.json').exists())
        self.assertFalse((editor.CONTENT/'categories/new.json').exists())
        self.assertEqual(editor.load()['document'],self.data)
    def test_revision_conflict_does_not_write(self):
        before=editor.revision()
        editor.BOOK.write_bytes(editor.encoded({**self.data['book'],'name':'外部修改'}))
        with self.assertRaises(FileExistsError):editor.save(self.data,before)
        self.assertEqual(editor.load()['document']['book']['name'],'外部修改')
    def test_invalid_paths_do_not_write(self):
        data=copy.deepcopy(self.data);data['entries']['../escape']=data['entries']['draft']
        with self.assertRaises(ValueError):editor.save(data,editor.revision())
        self.assertEqual({p:p.read_bytes() for p in editor.files()},self.original)
    def test_failed_replace_rolls_back(self):
        data=copy.deepcopy(self.data);data['book']['name']='更名'
        data['entries']['draft']['name']='另一标题'
        real_replace=editor.os.replace
        calls=0
        def interrupted(a,b):
            nonlocal calls
            calls+=1
            if calls==2:raise OSError('simulated write failure')
            return real_replace(a,b)
        with patch.object(editor.os,'replace',side_effect=interrupted):
            with self.assertRaises(OSError):editor.save(data,editor.revision())
        self.assertEqual({p:p.read_bytes() for p in editor.files()},self.original)
        self.assertFalse(any(p.name.startswith('tmp') for p in editor.RESOURCES.rglob('*')))

    def test_bilingual_edit_preserves_other_translation_and_metadata_keys(self):
        import shutil
        english = editor.CONTENT
        chinese = english.with_name('zh_cn')
        shutil.copytree(english, chinese)
        editor.CONTENT = chinese
        keys = {'name':'book.better_forbidden_arcanus.name','landing_text':'book.better_forbidden_arcanus.landing_text'}
        editor.BOOK.write_bytes(editor.encoded({**self.data['book'], **keys}))
        for locale, title in [('zh_cn','中文书名'),('en_us','English Book')]:
            p = editor.locale_file(locale);p.parent.mkdir(parents=True,exist_ok=True)
            p.write_bytes(editor.encoded({keys['name']:title,keys['landing_text']:'Introduction'}))
        english_before = {p:p.read_bytes() for p in english.rglob('*.json')}
        data = editor.load()['document'];self.assertEqual(data['book']['name'],'中文书名')
        data['book']['name'] = '新中文书名';data['entries']['draft']['name'] = '中文条目'
        data['entries']['new'] = copy.deepcopy(data['entries']['draft'])
        editor.save(data, editor.revision())
        self.assertEqual(json.loads(editor.BOOK.read_text())['name'], keys['name'])
        self.assertEqual(json.loads(editor.locale_file('zh_cn').read_text(encoding='utf8'))[keys['name']],'新中文书名')
        self.assertEqual(json.loads(editor.locale_file('en_us').read_text())[keys['name']],'English Book')
        self.assertEqual({p:p.read_bytes() for p in english_before},english_before)
        self.assertTrue((english/'entries/new.json').exists())
        del data['entries']['new'];editor.save(data,editor.revision())
        self.assertFalse((english/'entries/new.json').exists())
        self.assertFalse((chinese/'entries/new.json').exists())

if __name__=='__main__':unittest.main()

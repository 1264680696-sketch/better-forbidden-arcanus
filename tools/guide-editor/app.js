'use strict';
const $ = id => document.getElementById(id);
const NS = 'better_forbidden_arcanus:';
let doc, revision, token, selection = {kind: 'entries', id: 'guide_book'}, page = 0, dirty = false, saving = false;
let rawDraft = null;
function el(tag, text, cls) { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (cls) node.className = cls; return node; }
function toast(message, error = false) { $('toast').textContent = message; $('toast').className = error ? 'error' : ''; $('toast').style.display = 'block'; clearTimeout(toast.timer); toast.timer = setTimeout(() => $('toast').style.display = 'none', 6000); }
function changed() { dirty = true; $('status').textContent = '● 有未保存的修改'; preview(); }
function current() { return selection.kind === 'book' ? doc.book : doc[selection.kind][selection.id]; }
function icon(id) { const img = el('img', undefined, 'tiny-icon'); img.alt = ''; img.src = '/api/icon?id=' + encodeURIComponent((id || '').split('[')[0]); img.addEventListener('error', () => { const fallback = el('span', id === 'minecraft:book' ? '▤' : '◇', 'tiny-icon icon-fallback'); fallback.title = id; img.replaceWith(fallback); }); return img; }
function button(text, action, cls) { const b = el('button', text, cls); b.type = 'button'; b.addEventListener('click', action); return b; }
function field(parent, title, object, key, multiline = false, hint = '') {
  const label = el('label', title), input = el(multiline ? 'textarea' : 'input');
  input.value = object[key] ?? ''; label.append(input); if (hint) label.append(el('small', hint)); parent.append(label);
  input.addEventListener('input', () => { object[key] = key === 'sortnum' ? (Number(input.value) || 0) : input.value; changed(); if (key === 'name') tree(); });
  return input;
}
function sorted(map) { return Object.entries(map).sort((a,b) => (a[1].sortnum || 0) - (b[1].sortnum || 0)); }
function select(kind, id) {
  if (rawDraft !== null && !confirm('原始 JSON 有尚未应用的编辑。放弃这些编辑并切换？')) return;
  rawDraft = null; selection = {kind, id}; page = 0; render();
}
function tree() {
  $('tree').replaceChildren();
  for (const [id, category] of sorted(doc.categories)) {
    const group = el('div', undefined, 'category');
    const active = selection.kind === 'categories' && selection.id === id;
    const title = button('', () => select('categories', id), 'category-name' + (active ? ' active' : ''));
    title.append(icon(category.icon), el('span', category.name || id)); group.append(title);
    for (const [eid, entry] of sorted(doc.entries).filter(([,e]) => e.category === NS + id)) {
      group.append(button(entry.name || eid, () => select('entries', eid), 'entry' + (selection.kind === 'entries' && selection.id === eid ? ' active' : '')));
    }
    $('tree').append(group);
  }
}
function jsonPanel(parent, object, replace) {
  const details = el('details', undefined, 'raw-panel'); details.append(el('summary', '原始 JSON · 更多字段 / 页面类型'));
  const input = el('textarea', undefined, 'json'); input.value = JSON.stringify(object, null, 2); input.setAttribute('aria-label', '原始 JSON');
  input.addEventListener('input', () => { rawDraft = input.value; $('status').textContent = '● JSON 尚未应用'; });
  details.append(input, button('应用 JSON', () => { try {
    const value = JSON.parse(input.value); if (!value || typeof value !== 'object' || Array.isArray(value)) throw Error('需要一个 JSON 对象。');
    if ('pages' in object && (!Array.isArray(value.pages) || !value.pages.length || value.pages.some(p => !p || typeof p !== 'object' || typeof p.type !== 'string'))) throw Error('条目至少需要一个带 type 的页面对象。');
    if ('category' in object && (typeof value.category !== 'string' || !doc.categories[value.category.replace(NS,'')])) throw Error('条目分类必须存在。');
    if ('name' in object && (typeof value.name !== 'string' || !value.name.trim())) throw Error('名称不能为空。');
    if ('type' in object && typeof value.type !== 'string') throw Error('页面需要 type 字段。');
    if ('icon' in value && typeof value.icon !== 'string') throw Error('本编辑器的图标字段请填写物品 ID 文本。');
    replace(value); rawDraft = null; changed(); render();
  } catch(e) { toast('JSON 错误：' + e.message, true); } })); parent.append(details);
}
function render() {
  tree(); const object = current(); $('fields').replaceChildren(); $('page-fields').replaceChildren(); $('page-tabs').replaceChildren();
  $('page-section').hidden = selection.kind !== 'entries';
  $('editing-label').textContent = selection.kind === 'book' ? '指南设置' : selection.kind === 'categories' ? '分类设置' : '条目设置';
  if (selection.kind === 'book') {
    field($('fields'), '指南名称', object, 'name');
    field($('fields'), '封面简介', object, 'landing_text', true, '支持帕秋莉格式。指南设置修改后需要重新构建 JAR。');
    field($('fields'), '指南版本', object, 'version');
    jsonPanel($('fields'), object, value => doc.book = value);
  } else {
    $('fields').append(el('p', '文件 ID：' + selection.id, 'preview-note'));
    field($('fields'), selection.kind === 'categories' ? '分类名称' : '条目名称', object, 'name');
    field($('fields'), '图标物品 ID', object, 'icon', false, '例如 forbidden_arcanus:arcane_crystal');
    field($('fields'), '排序值', object, 'sortnum', false, '数值越小，越靠前。');
    if (selection.kind === 'categories') {
      field($('fields'), '分类简介', object, 'description', true);
      $('fields').append(button('删除分类', () => {
        if (Object.values(doc.entries).some(e => e.category === NS + selection.id)) return toast('请先移动或删除该分类的条目。', true);
        if (Object.keys(doc.categories).length < 2) return toast('至少保留一个分类。', true);
        if (!confirm('删除此分类？保存后会删除对应文件，原文件会备份。')) return;
        delete doc.categories[selection.id]; selection = {kind: 'book'}; changed(); render();
      }, 'danger'));
    } else {
      const label = el('label', '所属分类'), dropdown = el('select');
      for (const [id, cat] of sorted(doc.categories)) { const opt = el('option', cat.name); opt.value = NS + id; dropdown.append(opt); }
      dropdown.value = object.category; dropdown.addEventListener('change', () => { object.category = dropdown.value; changed(); tree(); }); label.append(dropdown); $('fields').append(label);
      $('fields').append(button('删除条目', () => {
        if (Object.keys(doc.entries).length < 2) return toast('至少保留一个条目。', true);
        if (!confirm('删除此条目及其全部页面？保存前仍可重新载入。')) return;
        delete doc.entries[selection.id]; selection = {kind: 'entries', id: Object.keys(doc.entries)[0]}; page = 0; changed(); render();
      }, 'danger'));
      if (!Array.isArray(object.pages) || !object.pages.length) { object.pages = [{type: 'patchouli:text', text: ''}]; changed(); }
      page = Math.min(page, object.pages.length - 1);
      object.pages.forEach((p,i) => $('page-tabs').append(button(String(i+1), () => { if (rawDraft !== null) return toast('请先应用原始 JSON，或重新载入。', true); page = i; render(); }, i === page ? 'selected' : '')));
      pageFields(object.pages[page]);
    }
    jsonPanel($('fields'), object, value => doc[selection.kind][selection.id] = value);
  }
  preview();
}
function pageFields(p) {
  const types = ['patchouli:text', 'patchouli:crafting', 'patchouli:spotlight'];
  const names = ['文字页', '合成配方页', '物品介绍页'];
  const label = el('label', '页面类型'), dropdown = el('select');
  types.forEach((t,i) => { const option = el('option', names[i]); option.value = t; dropdown.append(option); });
  if (!types.includes(p.type)) { const option = el('option', p.type || '未知类型'); option.value = p.type || ''; dropdown.append(option); }
  dropdown.value = p.type;
  dropdown.addEventListener('change', () => { p.type = dropdown.value; if (p.type === 'patchouli:crafting' && !p.recipe) p.recipe = NS + 'guide_book'; if (p.type === 'patchouli:spotlight' && !p.item) p.item = 'forbidden_arcanus:arcane_crystal'; changed(); render(); });
  label.append(dropdown); $('page-fields').append(label);
  field($('page-fields'), '页面标题（可留空）', p, 'title');
  if (p.type === 'patchouli:crafting') { field($('page-fields'), '配方 ID', p, 'recipe'); field($('page-fields'), '第二个配方 ID（可留空）', p, 'recipe2').addEventListener('input', () => { if (!p.recipe2) delete p.recipe2; }); }
  if (p.type === 'patchouli:spotlight') field($('page-fields'), '介绍的物品 ID', p, 'item');
  const text = field($('page-fields'), '正文', p, 'text', true, '格式示例：$(item)神秘水晶$()；$(br2) 分段。'); text.rows = 7;
  const tools = el('div', undefined, 'format-tools');
  for (const [name, start, end] of [['换行','$(br)',''],['分段','$(br2)',''],['加粗','$(l)','$()'],['物品色','$(item)','$()'],['重点色','$(4)','$()']]) {
    tools.append(button(name, () => { const a = text.selectionStart, b = text.selectionEnd, middle = text.value.slice(a,b); text.setRangeText(start + middle + end, a,b,'end'); p.text = text.value; text.focus(); changed(); }));
  }
  $('page-fields').append(tools);
  const row = el('div', undefined, 'row');
  const shift = direction => { const pages = current().pages, next = page + direction; if (next < 0 || next >= pages.length) return; [pages[page],pages[next]] = [pages[next],pages[page]]; page = next; changed(); render(); };
  row.append(button('↑ 前移', () => shift(-1)), button('↓ 后移', () => shift(1)), button('删除此页', () => { if (current().pages.length === 1) return toast('条目至少保留一页。', true); if (!confirm('删除当前页面？')) return; current().pages.splice(page,1); page = Math.max(0,page-1); changed(); render(); }, 'danger'));
  $('page-fields').append(row); jsonPanel($('page-fields'), p, value => current().pages[page] = value);
}
function markup(parent, source) {
  // Render via text nodes: manuscript contents cannot inject HTML or scripts.
  const colors = {'0':'#171713','4':'#9d3333','6':'#9c6616','2':'#426d36','item':'#bb00bb','thing':'#449900'};
  let style = {}, container = parent;
  for (const part of String(source || '').split(/(\$\([^)]*\))/g)) {
    if (part.startsWith('$(')) {
      const code = part.slice(2,-1);
      if (code === 'br' || code === 'br2') { parent.append(el('br')); if (code === 'br2') parent.append(el('br')); }
      else if (!code || code === 'reset') style = {};
      else if (code === 'l') style.fontWeight = 'bold';
      else if (code === 'o') style.fontStyle = 'italic';
      else if (colors[code]) style.color = colors[code];
      else if (/^#[a-f0-9]{3}(?:[a-f0-9]{3})?$/i.test(code)) style.color = code;
      else if (code === 'li') parent.append(document.createTextNode('• '));
      else if (code.startsWith('l:')) style.textDecoration = 'underline';
      else if (code === '/l') delete style.textDecoration;
      continue;
    }
    container = el('span', part); Object.assign(container.style, style); parent.append(container);
  }
}
function drawPage(target, p, index) {
  target.replaceChildren();
  if (!p) { target.append(el('div', '留白，待下一段发现。', 'empty')); return; }
  if (p.title) target.append(el('h3', p.title));
  if (p.type === 'patchouli:crafting') {
    const art = el('div', undefined, 'recipe-art');
    if (p.recipe === NS + 'guide_book') {
      const grid = el('div', undefined, 'recipe-grid');
      for (const [id, op] of [['forbidden_arcanus:arcane_crystal','+'],['minecraft:book','→']]) { const cell = el('div', undefined, 'ingredient'); cell.append(icon(id)); grid.append(cell, el('span', op)); }
      grid.append(el('span', '📖')); art.append(grid, el('div', '神秘水晶 + 书 / 无序合成', 'recipe-id'));
    } else art.append(el('div', '合成配方', 'recipe-id'));
    art.append(el('div', p.recipe || '请填写配方 ID', 'recipe-id')); if (p.recipe2) art.append(el('div', p.recipe2, 'recipe-id')); target.append(art);
  } else if (p.type === 'patchouli:spotlight') { const item = el('div', undefined, 'spotlight'); item.append(icon(p.item)); target.append(item, el('div', p.item, 'recipe-id')); }
  else if (p.type !== 'patchouli:text') target.append(el('p', '此页面类型请在游戏中查看：' + p.type, 'recipe-id'));
  const body = el('p'); markup(body, p.text); target.append(body, el('div', String(index+1), 'folio'));
}
function preview() {
  if (!doc) return;
  const object = current(); let pages, title, category;
  if (selection.kind === 'entries') { pages = object.pages || []; title = object.name; category = doc.categories[(object.category || '').replace(NS,'')]?.name || ''; }
  else if (selection.kind === 'categories') { pages = [{type:'patchouli:text',title:object.name,text:object.description}]; title = object.name; category = '分类简介'; }
  else { pages = [{type:'patchouli:text',title:doc.book.name,text:doc.book.landing_text}]; title = doc.book.name; category = '封面简介'; }
  $('breadcrumb').textContent = doc.book.name + ' / ' + category; $('preview-title').textContent = title;
  const offset = selection.kind === 'entries' ? Math.floor(page/2)*2 : 0;
  drawPage($('left-page'), pages[offset], offset); drawPage($('right-page'), pages[offset+1], offset+1);
  $('page-count').textContent = pages.length ? `${offset+1}–${Math.min(offset+2,pages.length)} / ${pages.length} 页` : '暂无页面';
  $('previous').disabled = offset === 0 || selection.kind !== 'entries'; $('next').disabled = offset+2 >= pages.length || selection.kind !== 'entries';
}
async function loadDocument() { try { const response = await fetch('/api/document'); const data = await response.json(); if (!response.ok) throw Error(data.error); doc = data.document; revision = data.revision; token = data.token; if (!doc[selection.kind]?.[selection.id]) selection = {kind:'entries',id:Object.keys(doc.entries)[0]}; dirty = false; rawDraft = null; page = 0; $('status').textContent = '所有修改已保存'; render(); } catch(e) { toast('载入失败：' + e.message,true); $('status').textContent = '载入失败'; } }
async function saveDocument() {
  if (saving || !doc) return false;
  if (rawDraft !== null) { toast('请先点击“应用 JSON”，再保存。',true); return false; }
  saving = true; $('save').disabled = true; const snapshot = JSON.stringify(doc);
  try { const response = await fetch('/api/save', {method:'POST',headers:{'Content-Type':'application/json','X-Editor-Token':token},body:JSON.stringify({document:JSON.parse(snapshot),revision})}); const data = await response.json(); if (!response.ok) throw Error(data.error); revision = data.revision; dirty = snapshot !== JSON.stringify(doc); $('status').textContent = dirty ? '● 有未保存的修改' : '所有修改已保存'; toast(data.message); return !dirty; }
  catch(e) { toast(e.message,true); return false; } finally { saving = false; $('save').disabled = false; }
}
function newId(kind) { const id = prompt('填写文件 ID（小写英文、数字或下划线），例如 souls：'); if (id === null) return null; if (!/^[a-z0-9_-]+(?:\/[a-z0-9_-]+)*$/.test(id)) { toast('ID 格式不正确。',true); return null; } if (doc[kind][id]) { toast('这个 ID 已存在。',true); return null; } return id; }
$('save').addEventListener('click',saveDocument);
$('reload').addEventListener('click', () => { if ((dirty || rawDraft !== null) && !confirm('放弃未保存的修改，重新读取工程？')) return; loadDocument(); });
$('book-settings').addEventListener('click', () => select('book'));
$('add-category').addEventListener('click', () => { if (rawDraft !== null) return toast('请先应用 JSON。',true); const id = newId('categories'); if (!id) return; doc.categories[id] = {name:'新分类',description:'在这里填写分类简介。',icon:'forbidden_arcanus:arcane_crystal',sortnum:Object.keys(doc.categories).length}; selection = {kind:'categories',id}; changed(); render(); });
$('add-entry').addEventListener('click', () => { if (rawDraft !== null) return toast('请先应用 JSON。',true); const id = newId('entries'); if (!id) return; const category = selection.kind === 'categories' ? selection.id : selection.kind === 'entries' ? current().category.replace(NS,'') : Object.keys(doc.categories)[0]; doc.entries[id] = {name:'新条目',category:NS+category,icon:'forbidden_arcanus:arcane_crystal',pages:[{type:'patchouli:text',title:'新的发现',text:'在这里写下你的指南内容。'}]}; selection = {kind:'entries',id}; page = 0; changed(); render(); });
$('add-page').addEventListener('click', () => { if (rawDraft !== null) return toast('请先应用 JSON。',true); current().pages.push({type:'patchouli:text',title:'新页面',text:''}); page = current().pages.length-1; changed(); render(); });
$('previous').addEventListener('click', () => { if (rawDraft !== null) return toast('请先应用 JSON。',true); page = Math.max(0,Math.floor(page/2)*2-2); render(); });
$('next').addEventListener('click', () => { if (rawDraft !== null) return toast('请先应用 JSON。',true); page = Math.min(current().pages.length-1,Math.floor(page/2)*2+2); render(); });
$('export').addEventListener('click', async () => { if ((dirty || rawDraft !== null) && !(await saveDocument())) return; const a = el('a'); a.href='/api/export'; a.download='forbidden-arcanus-guide-preview.zip'; a.click(); toast('预览包只包含正文与目录；指南设置需要重新构建。'); });
document.addEventListener('keydown', e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') { e.preventDefault(); saveDocument(); } });
window.addEventListener('beforeunload', e => { if (dirty || rawDraft !== null) { e.preventDefault(); e.returnValue = ''; } });
loadDocument();

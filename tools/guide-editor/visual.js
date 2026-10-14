/* Direct manuscript editing. Existing JSON fields and specialised page types are retained. */
let readingMode = false, inlineField = null, inlineRange = null, holdInline = false;
let draftKey = '', draftTimer, pendingDraft = null;
const expandedChapters = new Set(), collapsedChapters = new Set();
const manuscriptColors = {0:'#171713',2:'#426d36',4:'#9d3333',6:'#9c6616',item:'#bb00bb',thing:'#449900'};
const oldSaveDocument = saveDocument;
const oldDrawPage = drawPage;
function replaceClick(id, action) {
  const old = $(id), fresh = old.cloneNode(true); old.replaceWith(fresh); fresh.addEventListener('click', action); return fresh;
}
function editableFocused() { return inlineField && (holdInline || inlineField.contains(document.activeElement)); }
function rememberRange() {
  const selected = window.getSelection();
  if (inlineField && selected.rangeCount && inlineField.contains(selected.anchorNode)) inlineRange = selected.getRangeAt(0).cloneRange();
}
function persistDraft() {
  if (!doc || !draftKey || !dirty) return;
  try { localStorage.setItem(draftKey, JSON.stringify({document:doc, revision, selection, page, time:Date.now()})); }
  catch { $('status').textContent = '● 尚未保存 · 请保存到工程'; }
}
changed = function() {
  dirty = true; $('status').textContent = '● 有未保存的修改';
  if (!editableFocused()) preview(); else requestAnimationFrame(updateOverflow);
  clearTimeout(draftTimer); draftTimer = setTimeout(persistDraft, 350);
  clearTimeout(glyphTimer); glyphTimer = setTimeout(loadGlyphs, 250);
};
function richMarkup(parent, source) {
  let style = {};
  for (const part of String(source || '').split(/(\$\([^)]*\))/g)) {
    if (part.startsWith('$(')) {
      const code = part.slice(2,-1);
      if (code === 'br' || code === 'br2') { parent.append(el('br')); if (code === 'br2') parent.append(el('br')); }
      else if (!code || code === 'reset') style = {};
      else if (code === 'l') style.bold = true;
      else if (code === 'o') style.italic = true;
      else if (manuscriptColors[code] || /^#[\da-f]{6}$/i.test(code)) style.color = manuscriptColors[code] || code;
      else if (code.startsWith('l:')) style.link = code.slice(2);
      else if (code === '/l') delete style.link;
      else {
        const token = el('span', code === 'li' ? '• ' : '◇', 'manuscript-token');
        token.dataset.patchouliToken = part; token.contentEditable = 'false'; token.title = '保留原格式：' + part; parent.append(token);
      }
      continue;
    }
    const run = el('span', part);
    if (style.bold) run.style.fontWeight = 'bold';
    if (style.italic) run.style.fontStyle = 'italic';
    if (style.color) run.style.color = style.color;
    if (style.link) { run.dataset.entryLink = style.link; run.style.textDecoration = 'underline'; run.title = style.link; }
    parent.append(run);
  }
}
function hexColor(value) {
  if (value.startsWith('#')) return value;
  const rgb = value.match(/\d+/g); return rgb?.length >= 3 ? '#' + rgb.slice(0,3).map(n => Number(n).toString(16).padStart(2,'0')).join('') : '';
}
function manuscriptText(root) {
  function walk(node, inherited = {}) {
    if (node.nodeType === Node.TEXT_NODE) {
      let text = node.nodeValue.replace(/\u00a0/g, ' ').replace(/\r?\n/g,'$(br)');
      if (!text) return '';
      const prefix = (inherited.bold?'$(l)':'') + (inherited.italic?'$(o)':'') + (inherited.color?'$('+inherited.color+')':'');
      if (inherited.link) text = '$(l:'+inherited.link+')' + text + '$(/l)';
      return prefix + text + (prefix?'$()':'');
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return '';
    if (node.dataset.patchouliToken) return node.dataset.patchouliToken;
    if (node.tagName === 'BR') return '$(br)';
    const style = {...inherited};
    if (['B','STRONG'].includes(node.tagName) || ['bold','700'].includes(node.style.fontWeight)) style.bold = true;
    if (['I','EM'].includes(node.tagName) || node.style.fontStyle === 'italic') style.italic = true;
    const color = node.getAttribute('color') || node.style.color;
    if (color) style.color = hexColor(color);
    if (node.dataset.entryLink) style.link = node.dataset.entryLink;
    let result = [...node.childNodes].map(child => walk(child,style)).join('');
    if (['DIV','P'].includes(node.tagName) && node !== root && node.previousSibling) result = '$(br)' + result;
    return result;
  }
  return walk(root);
}
function makeInline(target, object, key, label, rich = false) {
  target.contentEditable = 'true'; target.spellcheck = false;
  target.classList.add('manuscript-edit'); target.dataset.field = key;
  target.dataset.placeholder = rich ? '点击这里，写下你的内容…' : '点击填写标题';
  target.setAttribute('role','textbox'); target.setAttribute('aria-label',label);
  target.setAttribute('aria-multiline',String(rich));
  if (rich) richMarkup(target, object[key]); else target.textContent = object[key] || '';
  let wasEdited=false;
  target.addEventListener('focus', () => { inlineField = target; inlineRange = null; });
  const commit = () => {
    if(!wasEdited)return;
    const next = rich ? manuscriptText(target) : target.textContent.replace(/[\r\n]/g,'');
    if (object[key] !== next) { object[key] = next; if(key==='name')for(const peer of document.querySelectorAll('.manuscript-edit[data-field="name"]'))if(peer!==target)peer.textContent=next; changed(); }
  };
  target.addEventListener('input', event => { wasEdited=true; if (!event.isComposing) commit(); });
  target.addEventListener('compositionend',()=>{wasEdited=true;commit();});
  target.addEventListener('keyup', rememberRange); target.addEventListener('mouseup', rememberRange);
  target.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !rich) { event.preventDefault(); target.blur(); }
    if (event.key === 'Escape') { event.preventDefault(); target.blur(); }
  });
  target.addEventListener('paste', event => {
    event.preventDefault(); document.execCommand('insertText',false,event.clipboardData.getData('text/plain'));
  });
  target.addEventListener('blur', () => {
    commit(); if (holdInline) return;
    if (inlineField === target) inlineField = null;
    // Do not replace a newly focused neighbouring field during blur.
    setTimeout(()=>{tree();if(current())$('preview-title').textContent=current().name;persistDraft();requestAnimationFrame(updateOverflow);},0);
  });
  return target;
}
function visualTitle(target, object, key, label, itemObject) {
  const row = el('div',undefined,'visual-title-row');
  if (itemObject && !readingMode) {
    const item = button('',()=>openItemPicker(itemObject,'icon'),'paper-icon'); item.append(icon(itemObject.icon));
    item.title = '点击更换图标'; item.setAttribute('aria-label','更换图标'); row.append(item);
  }
  const heading = el('h3');
  if (readingMode) pixelText(heading,object[key]); else makeInline(heading,object,key,label);
  row.append(heading); target.append(row,el('div',undefined,'game-separator'));
}
function focusWritingTitle() {
  requestAnimationFrame(() => { const title = $('left-page').querySelector('[data-field="name"]'); if (!title) return;
    title.focus(); const range = document.createRange(); range.selectNodeContents(title); const selected = window.getSelection(); selected.removeAllRanges(); selected.addRange(range);
  });
}
function createInline(kind) {
  if (!doc || rawDraft !== null) return toast('请先载入手册或应用 JSON。',true);
  let number = 1, prefix = kind === 'categories' ? 'category' : 'entry';
  while (Object.hasOwn(doc[kind],prefix+'_'+number)) number++;
  const id = prefix+'_'+number;
  if (kind === 'categories') doc.categories[id] = {name:'新章节',description:'',icon:'forbidden_arcanus:arcane_crystal',sortnum:Math.max(0,...Object.values(doc.categories).map(c=>Number(c.sortnum)||0))+1};
  else {
    const category = selection.kind === 'categories' ? selection.id : selection.kind === 'entries' ? current().category.replace(NS,'') : sorted(doc.categories)[0][0];
    doc.entries[id] = {name:'新条目',category:NS+category,icon:'forbidden_arcanus:arcane_crystal',sortnum:Math.max(0,...Object.values(doc.entries).map(e=>Number(e.sortnum)||0))+1,pages:[{type:'patchouli:text',text:''}]};
  }
  selection = {kind,id}; page = 0; readingMode = false; changed(); render(); focusWritingTitle();
}
function addVisualPage(type) {
  if (selection.kind !== 'entries') return;
  if (rawDraft !== null) return toast('请先应用 JSON。',true);
  const p = {type,text:''};
  if (type === 'patchouli:crafting') p.recipe = NS+'guide_book';
  if (type === 'patchouli:spotlight') p.item = 'forbidden_arcanus:arcane_crystal';
  current().pages.push(p); page = current().pages.length-1; changed(); render();
}
function toolsForPage() {
  const tools = $('writer-tools'); tools.replaceChildren();
  $('chapter-back').hidden=selection.kind==='book';$('chapter-back').textContent=selection.kind==='entries'?'← 章节目录':'← 手册目录';$('delete-selected').hidden=readingMode||selection.kind==='book';$('delete-selected').textContent=selection.kind==='entries'?'删除条目':'删除章节';
  $('reading-mode').textContent = readingMode ? '返回编辑' : '阅读预览'; $('reading-mode').setAttribute('aria-pressed',String(readingMode));
  if (readingMode) { tools.append(el('span','阅读模式 · 与本地游戏字形一致，点击“返回编辑”继续书写。')); return; }
  for (const [name,command,value] of [['加粗','bold',null],['斜体','italic',null],['物品色','foreColor','#bb00bb'],['重点色','foreColor','#9d3333']]) {
    const control = button(name,()=>{
      if (!inlineField || !inlineRange) return toast('先在书页正文中选中文字。');
      inlineField.focus(); const selected = window.getSelection(); selected.removeAllRanges(); selected.addRange(inlineRange);
      document.execCommand(command,false,value); inlineField.dispatchEvent(new InputEvent('input',{bubbles:true})); rememberRange();
    });
    control.addEventListener('mousedown',e=>{rememberRange();e.preventDefault();}); tools.append(control);
  }
  if (selection.kind !== 'entries') return;
  tools.append(el('span',undefined,'toolbar-divider'));
  const pages = current().pages, pick = el('select'); pick.setAttribute('aria-label','当前编辑页');
  pages.forEach((p,i)=>{const option=el('option','第 '+(i+1)+' 页');option.value=i;pick.append(option);});pick.value=page;
  pick.addEventListener('change',()=>{page=Number(pick.value);render();}); tools.append(pick);
  tools.append(button('＋ 文字页',()=>addVisualPage('patchouli:text')),button('＋ 物品页',()=>addVisualPage('patchouli:spotlight')),button('＋ 配方页',()=>addVisualPage('patchouli:crafting')));
  const move = direction => {const next=page+direction;if(next<0||next>=pages.length)return;[pages[page],pages[next]]=[pages[next],pages[page]];page=next;changed();render();};
  tools.append(button('前移',()=>move(-1)),button('后移',()=>move(1)),button('删除页',async()=>{
    if(pages.length===1)return toast('条目至少保留一页。',true);
    if(!(await askConfirmation('删除当前第 '+(page+1)+' 页？保存前可以重新载入，保存后有备份。')))return;
    pages.splice(page,1);page=Math.min(page,pages.length-1);changed();render();
  },'danger'));
}
tree = function() {
  if (!doc) return; $('tree').replaceChildren();
  const search = $('tree-search').value.trim().toLowerCase();
  const categoryId = selection.kind==='categories'?selection.id:selection.kind==='entries'?current()?.category.replace(NS,''):null;
  for (const [id,category] of sorted(doc.categories)) {
    const entries=sorted(doc.entries).filter(([,entry])=>entry.category===NS+id);
    const matchCategory=(category.name+' '+id).toLowerCase().includes(search);
    const matches=entries.filter(([key,e])=>(e.name+' '+key).toLowerCase().includes(search));
    if(search&&!matchCategory&&!matches.length)continue;
    const group=el('div',undefined,'category'), head=el('div',undefined,'chapter-heading');
    const open=Boolean(search)||expandedChapters.has(id)||categoryId===id&&!collapsedChapters.has(id);
    const fold=button(open?'⌄':'›',()=>{if(open){expandedChapters.delete(id);collapsedChapters.add(id);}else{expandedChapters.add(id);collapsedChapters.delete(id);}tree();},'chapter-fold');fold.setAttribute('aria-label','展开章节 '+category.name);fold.setAttribute('aria-expanded',String(open));
    const title=button('',()=>select('categories',id),'category-name'+(categoryId===id?' active':''));title.append(icon(category.icon),el('span',category.name),el('small',String(entries.length)));
    head.append(fold,title);group.append(head);
    if(open)for(const [eid,entry] of search&&!matchCategory?matches:entries)group.append(button(entry.name||eid,()=>select('entries',eid),'entry'+(selection.kind==='entries'&&selection.id===eid?' active':'')));
    $('tree').append(group);
  }
};
function indexPage(target,title,rows,kind) {
  target.replaceChildren(); gameTitle(target,title); const list=el('div',undefined,'game-index');
  for(const [id,value] of rows.slice(page*5,page*5+5)) {
    const row=el('div',undefined,'game-index-row');
    const picture=button('',()=>openItemPicker(value,'icon'),'index-picture');picture.append(icon(value.icon));picture.title='更换“'+value.name+'”的图标';picture.setAttribute('aria-label',picture.title);
    if(readingMode)picture.disabled=true;
    const name=button('',()=>select(kind,id),'index-link');pixelText(name,value.name);row.append(picture,name);list.append(row);
  }
  target.append(list);
  if(!readingMode)target.append(button(kind==='categories'?'＋ 新建章节':'＋ 新建条目',()=>createInline(kind),'paper-add'));
}
drawPage = function(target,p,index) {
  if(readingMode){oldDrawPage(target,p,index);return;}
  target.replaceChildren();
  if(!p) {
    if(selection.kind==='entries') {const note=el('p','继续写下一页','empty-leaf');target.append(note,button('＋ 文字页',()=>addVisualPage('patchouli:text'),'paper-add'),button('＋ 物品页',()=>addVisualPage('patchouli:spotlight'),'paper-add'),button('＋ 合成配方页',()=>addVisualPage('patchouli:crafting'),'paper-add'));}
    return;
  }
  const choosePage=()=>{if(selection.kind==='entries'&&page!==index){page=index;toolsForPage();}};
  target.onfocusin=choosePage;
  if(p.type==='patchouli:text')visualTitle(target,index===0?current():p,index===0?'name':'title',index===0?'条目名称':'页面标题',index===0?current():null);
  else if(p.type==='patchouli:crafting') {
    drawRecipe(target,p.recipe,p.title);if(p.recipe2)drawRecipe(target,p.recipe2,p.title);
    const edit=button('更换配方',()=>{page=index;openRecipePicker(p,'recipe');},'paper-edit-control');target.append(edit);if(p.recipe2)target.append(button('更换第二个配方',()=>openRecipePicker(p,'recipe2'),'paper-edit-control'));
  } else if(p.type==='patchouli:spotlight') {
    visualTitle(target,p,'title','页面标题'); const picture=button('',()=>openItemPicker(p,'item'),'game-spotlight');picture.append(icon(p.item));picture.title='选择介绍的物品';picture.setAttribute('aria-label','选择介绍的物品');target.append(picture);
  } else {
    gameTitle(target,p.name||p.title||'结构与特殊页面');const note=el('p',p.type==='patchouli:multiblock'?'结构已保留 · 游戏中可预览和投影':'特殊页面已保留','recipe-placeholder');target.append(note);
    target.append(button('结构与页面设置',()=>{page=index;openPageSettings();},'paper-edit-control'));
  }
  const used=[...target.children].reduce((sum,node)=>{const style=getComputedStyle(node);return sum+node.offsetHeight+(parseFloat(style.marginTop)||0)+(parseFloat(style.marginBottom)||0);},0);
  const body=el('div',undefined,'game-body');makeInline(body,p,'text','第 '+(index+1)+' 页正文',true);body.style.maxHeight=Math.max(24,154-used)+'px';target.append(body);
};
function openPageSettings(label) {
  // Reuse the full settings editor in a sheet beside the manuscript, never replacing a special page.
  $('advanced-panel').hidden=false;$('advanced-toggle').setAttribute('aria-expanded','true');render();
  if(label)for(const node of $('page-fields').querySelectorAll('label'))if(node.firstChild?.textContent===label)node.querySelector('input')?.focus();
}
preview = function() {
  if(!doc||editableFocused())return;
  const object=current();if(!object)return;
  $('book-preview').style.backgroundImage=`url("/api/texture?id=${encodeURIComponent(doc.book.book_texture||'patchouli:textures/gui/book_brown.png')}")`;
  $('book-preview').style.color='#'+(doc.book.text_color||'333333');
  document.body.classList.toggle('reading',readingMode);toolsForPage();
  const left=$('left-page'),right=$('right-page');left.replaceChildren();right.replaceChildren();
  let offset=0,total=0;
  if(selection.kind==='entries') {
    total=object.pages.length;page=Math.min(page,total-1);offset=Math.floor(page/2)*2;
    drawPage(left,object.pages[offset],offset);drawPage(right,object.pages[offset+1],offset+1);
    $('breadcrumb').textContent=doc.book.name+' / '+(doc.categories[object.category.replace(NS,'')]?.name||'');
    $('page-count').textContent=`第 ${offset+1}–${Math.min(offset+2,total)} 页 / 共 ${total} 页`;
    $('previous').disabled=offset===0;$('next').disabled=offset+2>=total;
  } else {
    const isBook=selection.kind==='book', rows=isBook?sorted(doc.categories):sorted(doc.entries).filter(([,e])=>e.category===NS+selection.id);
    total=Math.max(1,Math.ceil(rows.length/5));page=Math.min(page,total-1);
    visualTitle(left,object,'name',isBook?'指南名称':'章节名称',isBook?null:object);
    const body=el('div',undefined,'game-body');
    if(readingMode)markup(body,object[isBook?'landing_text':'description']);else makeInline(body,object,isBook?'landing_text':'description',isBook?'封面简介':'章节简介',true);
    left.append(body);indexPage(right,isBook?'章节目录':'条目目录',rows,isBook?'categories':'entries');
    $('breadcrumb').textContent=isBook?'禁忌与奥秘 / 章节目录':doc.book.name+' / '+object.name;
    $('page-count').textContent=`目录 ${page+1} / ${total} · ${rows.length} ${isBook?'个章节':'个条目'}`;
    $('previous').disabled=page===0;$('next').disabled=page+1>=total;
  }
  const caption=el('h2');caption.id='preview-title';$('preview-title').replaceWith(caption);if(readingMode)caption.textContent=object.name;else makeInline(caption,object,'name',selection.kind==='entries'?'当前条目名称':selection.kind==='categories'?'当前章节名称':'当前指南名称');fitBook();requestAnimationFrame(updateOverflow);
};
saveDocument = async function() {
  if(document.activeElement?.isContentEditable)document.activeElement.blur();
  const result=await oldSaveDocument();
  if(result&&draftKey){localStorage.removeItem(draftKey);$('draft-banner').hidden=true;}
  return result;
};
function offerDraft(draft) {
  pendingDraft=draft;const banner=$('draft-banner');banner.replaceChildren();banner.hidden=false;
  const same=draft.revision===revision;
  banner.append(el('span',same?'发现此工程未保存的本地草稿。':'发现旧草稿，但工程文件已更新。可下载草稿对照，避免覆盖新内容。'));
  if(same)banner.append(button('恢复草稿',()=>{doc=pendingDraft.document;selection=pendingDraft.selection||{kind:'book'};page=pendingDraft.page||0;banner.hidden=true;changed();render();}));
  banner.append(button('下载草稿',()=>{const a=el('a');a.href=URL.createObjectURL(new Blob([JSON.stringify(draft.document,null,2)],{type:'application/json'}));a.download='guide-unsaved-draft.json';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);}),button('丢弃草稿',async()=>{if(!(await askConfirmation('丢弃这个浏览器中的未保存草稿？工程文件不会改变。')))return;localStorage.removeItem(draftKey);banner.hidden=true;}));
}
loadDocument = async function() {
  try {
    $('status').textContent='正在读取工程…';const response=await fetch('/api/document',{signal:AbortSignal.timeout(15000)}),data=await response.json();if(!response.ok)throw Error(data.error);
    doc=data.document;revision=data.revision;token=data.token;dirty=false;rawDraft=null;page=0;recipes.clear();selection={kind:'book'};inlineField=null;
    draftKey='bfa-guide-draft:'+(data.contentRoot||data.projectRoot||data.bookId);$('status').textContent='所有修改已保存';render();loadGlyphs();
    try{const draft=JSON.parse(localStorage.getItem(draftKey)||'null');if(draft?.document)offerDraft(draft);}catch{}
  }catch(error){$('status').textContent='连接中断 · 点击重新载入';toast('编辑器连接失败，请双击“打开指南编辑器.bat”，再点重新载入。',true);}
};
replaceClick('save',saveDocument);replaceClick('add-category',()=>createInline('categories'));replaceClick('add-entry',()=>createInline('entries'));
replaceClick('add-page',()=>addVisualPage('patchouli:text'));
for(const [id,direction] of [['previous',-1],['next',1]])replaceClick(id,()=>{
  if(rawDraft!==null)return toast('请先应用 JSON。',true);
  page=selection.kind==='entries'?Math.max(0,Math.min(current().pages.length-1,Math.floor(page/2)*2+direction*2)):Math.max(0,page+direction);render();
});
$('home').addEventListener('click',()=>select('book'));
$('visual-new-category').addEventListener('click',()=>createInline('categories'));$('visual-new-entry').addEventListener('click',()=>createInline('entries'));
$('tree-search').addEventListener('input',tree);
$('reading-mode').addEventListener('click',()=>{readingMode=!readingMode;render();});
$('advanced-toggle').addEventListener('click',()=>{const panel=$('advanced-panel');panel.hidden=!panel.hidden;$('advanced-toggle').setAttribute('aria-expanded',String(!panel.hidden));fitBook();});
document.addEventListener('selectionchange',rememberRange);
window.addEventListener('pagehide',persistDraft);
$('advanced-panel').querySelector('.panel-heading').append(button('关闭',()=>{$('advanced-panel').hidden=true;$('advanced-toggle').setAttribute('aria-expanded','false');},'advanced-close'));
updateOverflow=function(){const pages=['left-page','right-page'].filter(id=>{const paper=$(id),body=paper.querySelector('.game-body.manuscript-edit');return paper.scrollHeight>paper.clientHeight+1||body&&body.scrollHeight>body.clientHeight+1;});$('overflow-warning').hidden=!pages.length;$('overflow-warning').textContent=pages.length?'正文超过这一页的容量。可以继续编辑，再拆成多页；保存不会截断文字。':'';for(const id of ['left-page','right-page'])$(id).classList.toggle('overflowed',pages.includes(id));};
fitBook=function(){const narrow=window.matchMedia('(max-width:720px)').matches;const scale=$('book-frame').clientWidth/(narrow?136:272);$('book-preview').style.transform=`scale(${scale})`;$('preview-scale').textContent=narrow?'书页编辑':'帕秋莉 · '+scale.toFixed(1)+' 倍';};
const back=button('← 章节目录',()=>{if(selection.kind==='entries')select('categories',current().category.replace(NS,''));else select('book');});back.id='chapter-back';$('home').after(back);
const remove=button('删除条目',async()=>{if(selection.kind==='book')return;const kind=selection.kind,id=selection.id,object=current();if(kind==='categories'&&Object.values(doc.entries).some(e=>e.category===NS+id))return toast('章节中还有条目，请先移动或删除这些条目。',true);if(Object.keys(doc[kind]).length<2)return toast('至少保留一个章节和一个条目。',true);if(!(await askConfirmation('删除“'+object.name+'”？保存后原文件会自动备份。')))return;const parent=kind==='entries'?object.category.replace(NS,''):null;delete doc[kind][id];selection=parent?{kind:'categories',id:parent}:{kind:'book'};page=0;changed();render();},'danger');remove.id='delete-selected';$('advanced-toggle').after(remove);
let recipeCatalogPromise=null, recipePickTarget=null;
const recipeDialog=el('dialog');recipeDialog.id='recipe-dialog';recipeDialog.setAttribute('aria-label','选择合成配方');
const recipeHeading=el('div',undefined,'dialog-heading');recipeHeading.append(el('h2','选择合成配方'),button('×',()=>recipeDialog.close()));
const recipeSearch=el('input');recipeSearch.type='search';recipeSearch.placeholder='搜索产物中文名或配方 ID';recipeSearch.setAttribute('aria-label','搜索合成配方');
const recipeList=el('div',undefined,'recipe-choice-list');recipeDialog.append(recipeHeading,recipeSearch,recipeList);document.body.append(recipeDialog);
async function openRecipePicker(object,key) {
    recipePickTarget={object,key};recipeSearch.value='';recipeList.textContent='正在读取本地配方…';recipeDialog.showModal();recipeSearch.focus();
    try {
        if(!recipeCatalogPromise)recipeCatalogPromise=fetch('/api/recipes').then(async response=>{const data=await response.json();if(!response.ok)throw Error(data.error||'配方读取失败');return data.recipes;}).catch(error=>{recipeCatalogPromise=null;throw error;});
        await recipeCatalogPromise;filterRecipes();
    }catch(error){recipeList.textContent=error.message;}
}
async function filterRecipes() {
    if(!recipeDialog.open||!recipeCatalogPromise)return;
    const catalog=await recipeCatalogPromise,words=recipeSearch.value.trim().toLowerCase().split(/\s+/).filter(Boolean);
    recipeList.replaceChildren();const matches=catalog.filter(row=>words.every(word=>(row.name+' '+row.id).toLowerCase().includes(word)));
    for(const recipe of matches.slice(0,100)) {
        const card=button('',()=>{const target=recipePickTarget;target.object[target.key]=recipe.id;recipeDialog.close();changed();render();},'recipe-choice');
        card.append(icon(recipe.output),el('span',recipe.name),el('small',recipe.id));recipeList.append(card);
    }
    if(!matches.length)recipeList.textContent='没有找到工作台配方。祭坛和炽炉配方使用对应的原有页面。';
    else if(matches.length>100)recipeList.append(el('p','显示前100项，请输入关键词缩小范围。'));
}
recipeSearch.addEventListener('input',filterRecipes);

const menu=button('☰ 目录',()=>{document.body.classList.toggle('directory-open');menu.setAttribute('aria-expanded',String(document.body.classList.contains('directory-open')));});menu.id='directory-toggle';menu.setAttribute('aria-expanded','false');$('home').before(menu);
const standardSelect=select;select=async function(kind,id){await standardSelect(kind,id);document.body.classList.remove('directory-open');menu.setAttribute('aria-expanded','false');};
loadDocument();

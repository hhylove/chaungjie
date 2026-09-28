/* Shared UI decoration: no business records, storage or workflow mutations. */
(() => {
  const embedded = /\/hr\//.test(location.pathname);
  if (embedded) document.body.classList.add('ux-embedded');
  let queued=false;
  function decorate(){
    queued=false;
    document.querySelectorAll('.field').forEach(label=>{
      const input=label.querySelector('input[required],select[required],textarea[required]');
      if(input&&!label.querySelector('.ux-required')){
        const mark=document.createElement('span');mark.className='ux-required';mark.textContent=' *';mark.title='必填';mark.setAttribute('aria-hidden','true');
        // Place beside the label text, never between an input and its help.
        const text=Array.from(label.childNodes).find(n=>n.nodeType===3&&n.textContent.trim());
        if(text){const line=document.createElement('span');line.className='ux-field-label';text.before(line);line.append(text,mark);}
      }
    });
    document.querySelectorAll('.modal-content>form,.drawer-body>form').forEach(form=>{
      if(form.dataset.uxForm)return;
      form.dataset.uxForm='true';
      if(form.querySelector('[required]')){
        const note=document.createElement('p');note.className='ux-form-note';note.textContent='带 * 的项目为必填；请核对资料后提交。';form.prepend(note);
      }
    });
    document.querySelectorAll('.table-wrap,.table-scroll').forEach(wrap=>{
      if(wrap.dataset.uxTable)return;
      wrap.dataset.uxTable='true';wrap.tabIndex=0;wrap.setAttribute('aria-label','数据表格，可横向滚动查看完整字段');
    });
    document.querySelectorAll('.empty').forEach(el=>{el.setAttribute('role','status');});
    document.querySelectorAll('.modal-content>.timeline,.drawer-body>.timeline').forEach(t=>{
      if(t.children.length<7||t.parentElement.tagName==='DETAILS')return;
      const details=document.createElement('details');details.className='ux-stages-overview';const summary=document.createElement('summary');summary.textContent=`查看完整操作记录（${t.children.length} 条）`;t.before(details);details.append(summary,t);
    });
  }
  const observer=new MutationObserver(()=>{if(!queued){queued=true;requestAnimationFrame(decorate);}});
  observer.observe(document.getElementById('app'),{childList:true,subtree:true});
  observer.observe(document.getElementById('overlay'),{childList:true,subtree:true});
  decorate();
})();

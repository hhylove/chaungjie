/* Goal workflow. Shared state is supplied by app.js; no independent demo copy. */
let goalFilter='all',goalSearch='';
const goalStates=['待制定方案','待老板审核','执行中','待老板验收','验收驳回 · 调整中','已完成'];
const goalNow=()=>new Date().toISOString();
const newGoalId=()=>crypto.randomUUID();
const goalOwner=g=>role===g.center&&level==='负责人';
const goalById=id=>state.items.find(i=>i.id===id&&i.type==='goal'&&visible(i));
const goalComplete=m=>m.status==='complete'&&Array.isArray(m.evidence)&&m.evidence.length>0;
const goalReady=g=>g.milestones.length>0&&g.milestones.every(goalComplete);
const goalProgress=g=>g.milestones.length?Math.round(g.milestones.filter(goalComplete).length/g.milestones.length*100):0;
function migrateGoals(){
  const legacy=state.items.filter(i=>i.type==='goal'&&!i.goalSchema);
  if(!legacy.length)return;
  if(!storageBlocked){try{const raw=localStorage.getItem(key);if(raw&&!localStorage.getItem(key+'-before-goals-v2'))localStorage.setItem(key+'-before-goals-v2',raw);}catch{storageBlocked=true;}}
  for(const g of legacy){
    g.legacyGoal=structuredClone(g);g.goalSchema=2;g.why='';g.expected=g.description||'';g.planDue=g.due;g.commitment='';g.milestones=[];g.planVersions=[];g.audit=[];g.reviews=[];g.stage=0;g.progress=0;g.planRevision=0;g.rejection='';
    g.history.push('目标流程升级：旧记录保留，负责人需补齐结构化里程碑和承诺后重新提交审核。未用旧进度代替成果证据。');
    if(g.center===1&&!shops.includes(g.shop))g.shop=shops[0];
    // Existing content is retained; missing reasons are shown as missing, never invented.
  }
  if(!storageBlocked){try{state.revision=(state.revision||0)+1;localStorage.setItem(key,JSON.stringify(state));}catch{storageBlocked=true;}}
}
function goalAudit(d,g,text){
  const entry={at:goalNow(),by:role===0?'老板':g.owner,text};g.audit.push(entry);g.history.push(`${entry.by}：${text}`);
  d.log.push({id:g.id,...entry});let doc=d.docs.find(x=>x.id==='FLOW-'+g.id);
  if(!doc){doc={id:'FLOW-'+g.id,title:g.title+' · 过程记录',category:'流程记录',scope:g.center===4?'财务':g.shop==='公司'?'内部':g.shop,center:g.center,body:'',source:g.id+' · 自动记录',goalId:g.id,version:0};d.docs.push(doc);}
  doc.title=g.title+' · 过程记录';doc.goalId=g.id;doc.body=goalSummary(g);doc.version++;
}
function goalSummary(g){return `目标：${g.title}\n为什么要做：${g.why||'原记录未填写'}\n要的结果：${g.expected}\n负责人：${g.owner}\n期限：${g.due}\n验收标准：${g.standard||'待制定'}\n承诺：${g.commitment||'待填写'}\n当前状态：${goalStates[g.stage]}\n\n里程碑：\n${g.milestones.map((m,n)=>`${n+1}. ${m.title}（${m.start} 至 ${m.end}）· ${goalComplete(m)?'已完成':'未完成'}\n成果：${(m.evidence||[]).map(e=>e.name||e.text||e.url).join('；')}`).join('\n')}\n\n过程记录：\n${g.history.join('\n')}`;}
function goalCommit(id,mutate,message){
  if(storageBlocked){toast('本地存储不可用，未提交变更。请先导出当前资料后检查浏览器存储。');return false;}
  try{
    const persisted=localStorage.getItem(key);if(persisted&&(JSON.parse(persisted).revision||0)!==(state.revision||0)){toast('其他页面已更新数据，请刷新后再操作，避免覆盖新结果。');return false;}
    const draft=structuredClone(state);const g=draft.items.find(i=>i.id===id);mutate(g,draft);draft.revision=(state.revision||0)+1;
    localStorage.setItem(key,JSON.stringify(draft));state=draft;render();if(id)goalDetail(id);else $('#overlay').innerHTML='';if(message)toast(message);return true;
  }catch(e){toast(e.message||'保存失败，变更未提交');return false;}
}
function requireGoal(condition,message){if(!condition)throw Error(message);}
function goalPage(){
  const all=items().filter(i=>i.type==='goal');const counts={all:all.length,mine:all.filter(canAct).length,plan:all.filter(g=>g.stage===0).length,review:all.filter(g=>g.stage===1).length,active:all.filter(g=>[2,4].includes(g.stage)).length,accept:all.filter(g=>g.stage===3).length,done:all.filter(g=>g.stage===5).length};
  const matches=g=>goalFilter==='all'||goalFilter==='mine'&&canAct(g)||goalFilter==='plan'&&g.stage===0||goalFilter==='review'&&g.stage===1||goalFilter==='active'&&[2,4].includes(g.stage)||goalFilter==='accept'&&g.stage===3||goalFilter==='done'&&g.stage===5;
  const arr=all.filter(matches).filter(g=>(g.title+g.id+g.owner+g.expected).toLowerCase().includes(goalSearch.toLowerCase()));
  return heading('目标管理','老板明确方向，负责人承诺交付；里程碑成果齐备后，由老板统一验收。',role===0?'<button class="primary" data-new="goal">＋ 下达目标</button>':'')+`<div class="goal-stats">${[['all','全部目标'],['mine','待我处理'],['active','执行与调整中'],['accept','待老板验收']].map(([v,t])=>`<button class="metric ${goalFilter===v?'selected':''}" data-goal-filter="${v}"><span class="metric-title">${t}</span><strong>${counts[v]}</strong><span class="sub">查看目标 →</span></button>`).join('')}</div><div class="tabs goal-tools"><input id="goal-search" aria-label="搜索目标" placeholder="搜索目标、编号、负责人" value="${esc(goalSearch)}"><button id="goal-search-submit">搜索</button><button id="goal-clear-search">清空</button></div><div class="tabs">${[['all','全部'],['plan','待制定方案'],['review','待老板审核'],['active','执行 / 调整中'],['accept','待老板验收'],['done','已完成']].map(([v,t])=>`<button data-goal-filter="${v}" class="${goalFilter===v?'active':''}">${t} ${counts[v]}</button>`).join('')}</div><section class="panel"><div class="table-wrap"><table><thead><tr><th>目标 / 负责人</th><th>阶段</th><th>里程碑成果</th><th>截止日期</th><th>时限状态</th><th>操作</th></tr></thead><tbody>${arr.map(g=>`<tr data-goal-row="${g.id}"><td><button class="flat goal-title-link" data-open="${g.id}">${esc(g.title)}</button><br><span class="sub">${g.id} · ${esc(g.owner)}${g.shop!=='公司'?' · '+esc(g.shop):''}</span></td><td>${goalStates[g.stage]}</td><td>${g.milestones.filter(goalComplete).length} / ${g.milestones.length}<div class="progress"><span style="width:${goalProgress(g)}%"></span></div></td><td>${g.due}</td><td>${badge(g)}</td><td><button data-open="${g.id}">${canAct(g)?g.stage===0?'制定方案':g.stage===1?'审核方案':g.stage===3?'验收目标':'更新成果':'查看详情'}</button></td></tr>`).join('')}</tbody></table></div>${arr.length?'':'<div class="empty">当前没有匹配的目标。新下达的目标会自动出现在对应负责人的工作台。</div>'}</section>`;
}
function bindGoalPage(){
  document.querySelectorAll('[data-goal-filter]').forEach(b=>b.onclick=()=>{goalFilter=b.dataset.goalFilter;render();});
  if($('#goal-search-submit'))$('#goal-search-submit').onclick=()=>{goalSearch=$('#goal-search').value.trim();render();};
  if($('#goal-search'))$('#goal-search').onkeydown=e=>{if(e.key==='Enter'){goalSearch=e.target.value.trim();render();}};
  if($('#goal-clear-search'))$('#goal-clear-search').onclick=()=>{goalSearch='';goalFilter='all';render();};
}
function goalCreate(editId){
  if(role!==0){toast('仅老板可下达或修改目标');return;}
  const existing=editId?goalById(editId):null;if(editId&&(!existing||![0,1].includes(existing.stage)))return;
  const g=existing||{title:'',why:'',expected:'',due:crossAdd(crossDay(),21),planDue:crossAdd(crossDay(),2),center:2,shop:'公司'};
  modal(existing?'编辑目标要求':'下达目标',`<form id="goal-create-form"><div class="form-grid"><label class="field">接收负责人<select id="goal-center">${centers.slice(1).map((n,i)=>`<option value="${i+1}" ${g.center===i+1?'selected':''}>${i===0?'林悦 · 事业主理人':n+'负责人'}</option>`).join('')}</select></label><label class="field" id="goal-shop-field" ${g.center===1?'':'hidden'}>所属店铺<select id="goal-shop">${shops.map(s=>`<option ${s===g.shop?'selected':''}>${s}</option>`).join('')}</select></label><label class="field wide">目标<input id="goal-title" required maxlength="120" value="${esc(g.title)}" placeholder="需要实现什么目标"></label><label class="field wide">为什么要做<textarea id="goal-why" required>${esc(g.why)}</textarea></label><label class="field wide">要的结果<textarea id="goal-expected" required>${esc(g.expected)}</textarea></label><label class="field">负责人方案提交期限<input type="date" id="goal-plan-due" required value="${g.planDue}"></label><label class="field">目标完成期限<input type="date" id="goal-due" required value="${g.due}"></label></div><p class="sub">这里只填写目标要求。验收标准、里程碑和承诺由接收负责人制定。${existing?'修改后须由负责人重新提交方案。':''}</p><div class="modal-footer"><button type="button" id="goal-cancel">取消</button><button class="primary" type="submit">${existing?'保存并退回负责人确认':'下达给负责人'}</button></div></form>`);
  $('#goal-center').onchange=()=>$('#goal-shop-field').hidden=$('#goal-center').value!=='1';
  if(existing)$('#goal-center').disabled=true;
  $('#goal-cancel').onclick=()=>existing?goalDetail(existing.id):$('#overlay').innerHTML='';
  $('#goal-create-form').onsubmit=e=>{
    e.preventDefault();const values={title:$('#goal-title').value.trim(),why:$('#goal-why').value.trim(),expected:$('#goal-expected').value.trim(),planDue:$('#goal-plan-due').value,due:$('#goal-due').value,center:Number($('#goal-center').value)};values.shop=values.center===1?$('#goal-shop').value:'公司';
    if(!values.title||!values.why||!values.expected){toast('请填写目标、为什么要做和要的结果');return;}if(values.planDue>values.due){toast('方案提交期限不能晚于目标完成期限');return;}
    const owner=values.center===1?'林悦 · 事业主理人':centers[values.center]+'负责人';let createdId;
    const ok=goalCommit(existing?.id,(item,d)=>{
      requireGoal(role===0,'仅老板可操作');
      if(item){requireGoal([0,1].includes(item.stage),'该目标已经启动，不能修改下达要求');Object.assign(item,values,{owner,stage:0,commitment:'',rejection:'老板修改目标要求，请调整方案后重新承诺。'});goalAudit(d,item,'修改目标要求，退回负责人重新确认。');}
      else{createdId='CJ-'+d.seq++;const goal={...values,id:createdId,type:'goal',goalSchema:2,owner,description:values.expected,stage:0,light:'green',progress:0,standard:'',commitment:'',milestones:[],planVersions:[],planRevision:0,reviews:[],audit:[],history:[],createdAt:goalNow()};d.items.push(goal);goalAudit(d,goal,'下达目标：'+values.title+'；原因：'+values.why+'；要的结果：'+values.expected+'；方案期限：'+values.planDue+'；目标期限：'+values.due);}
    },existing?'目标已更新，负责人需重新确认':'目标已下达，负责人工作台和待办已同步');
    if(ok){page='goals';goalFilter='all';goalSearch='';render();goalDetail(existing?.id||createdId);}
  };
}
function goalDetail(id){activeRecordId=id;
  const g=goalById(id);if(!g){toast('当前工作台无权访问此目标');return;}
  const owner=goalOwner(g),count=g.milestones.filter(goalComplete).length;
  const stageView=g.stage===4?2:g.stage;const steps=['负责人制定方案','老板审核','里程碑执行','老板验收','完成'];
  const stageIndex=g.stage===5?4:stageView;
  let controls='';
  if(role===0&&[0,1].includes(g.stage))controls+='<button id="goal-edit">编辑目标要求</button>';
  if(owner&&g.stage===0)controls+='<button class="primary" id="goal-plan">制定 / 调整方案</button>';
  if(role===0&&g.stage===1)controls+='<button id="goal-plan-reject">驳回方案</button><button class="primary" id="goal-plan-approve">审核通过，立即启动</button>';
  if(owner&&[2,4].includes(g.stage)){if(g.stage===4)controls+='<button id="goal-replan">调整方案并重新送审</button>';controls+=`<button class="primary" id="goal-request-acceptance" ${goalReady(g)?'':'disabled'}>全部完成，提交老板验收</button>`;}
  if(role===0&&g.stage===3)controls+='<button id="goal-final-reject">验收不通过，驳回调整</button><button class="primary" id="goal-final-approve">验收通过，完成目标</button>';
  if(role===0&&g.stage!==5)controls+='<button id="extend">审批延期</button>';
  modal(g.title,`<div class="row-title"><span class="sub">${g.id} · ${esc(g.owner)} · ${esc(g.shop)}</span><span class="badge">${goalStates[g.stage]}</span>${badge(g)}</div><div class="flow">${steps.map((s,n)=>`<span class="${n<stageIndex?'done':n===stageIndex?'current':''}">${n+1}. ${s}</span>`).join('')}</div>${g.rejection?`<div class="notice rejection"><b>老板的驳回 / 修改意见</b><br>${esc(g.rejection)}</div>`:''}<div class="goal-requirements"><div><span>为什么要做</span><p>${esc(g.why||'旧版本未单独填写；老板可在启动前补充。')}</p></div><div><span>要的结果</span><p>${esc(g.expected)}</p></div><div><span>方案提交期限</span><p>${g.planDue}</p></div><div><span>目标完成期限</span><p>${g.due}</p></div></div><div class="detail-box"><b>负责人验收标准</b><p>${esc(g.standard||'待负责人制定')}</p><b>负责人承诺</b><p>${esc(g.commitment||'待负责人作出承诺')}</p>${g.startedAt?`<span class="sub">审核启动：${new Date(g.startedAt).toLocaleString('zh-CN')} · 方案 v${g.planRevision}</span>`:''}</div><div class="row-title"><h3>里程碑与工作成果</h3><span class="badge">${count} / ${g.milestones.length} 已完成 · ${goalProgress(g)}%</span></div><div class="progress"><span style="width:${goalProgress(g)}%"></span></div><div class="milestones">${g.milestones.map((m,n)=>`<article class="milestone" data-milestone="${m.id}"><div class="row-title"><h3>${n+1}. ${esc(m.title)}</h3><span class="badge ${goalComplete(m)?'green':g.stage===4?'yellow':''}">${goalComplete(m)?'成果已提交':g.stage===4?'待整改':'待完成'}</span></div><p class="sub">${m.start} 开始 → ${m.end} 结束</p>${(m.evidence||[]).map(e=>e.kind==='file'?`<button class="evidence-link" data-evidence-download="${e.id}">↓ ${esc(e.name)} <span class="sub">${Math.ceil(e.size/1024)} KB</span></button>`:e.kind==='link'?`<a class="evidence-link" href="${esc(e.url)}" target="_blank" rel="noopener noreferrer">↗ ${esc(e.url)}</a>`:`<div class="evidence-text"><b>工作成果</b><p>${esc(e.text)}</p></div>`).join('')||'<p class="sub">尚未提交工作成果</p>'}${m.previousEvidence?.length?`<button class="flat" data-milestone-history="${m.id}">查看历史交付（${m.previousEvidence.length} 次）</button>`:''}${owner&&[2,4].includes(g.stage)?`<div class="milestone-actions"><button class="${goalComplete(m)?'':'primary'}" data-milestone-deliver="${m.id}">${goalComplete(m)?'补充 / 更新成果':'提交成果并完成里程碑'}</button>${goalComplete(m)?`<button data-milestone-reopen="${m.id}">重新调整</button>`:''}</div>`:''}</article>`).join('')||'<div class="empty">等待负责人制定里程碑事项、开始时间和结束时间。</div>'}</div>${owner&&[2,4].includes(g.stage)&&!goalReady(g)?'<p class="sub">全部里程碑必须分别提交资料或工作成果，完成后才可申请老板验收。</p>':''}<div class="modal-footer goal-actions">${controls||`<span class="sub">${g.stage===5?'目标已由老板验收完成，当前记录只读。':'当前由 '+(actor(g)===0?'老板':g.owner)+' 处理。'}</span>`}</div><div class="goal-links"><button id="goal-back-list">返回目标列表</button><button id="goal-records">查看关联知识记录</button><button id="goal-export">导出目标记录</button>${g.planVersions.length?'<button id="goal-versions">查看方案版本</button>':''}</div><h3>完整过程记录</h3><div class="timeline">${g.history.map(h=>`<div>${esc(h)}</div>`).join('')}</div>`);
  $('.modal').classList.add('goal-modal');
  const on=(selector,fn)=>{if($(selector))$(selector).onclick=fn;};
  on('#goal-edit',()=>goalCreate(id));on('#goal-plan',()=>goalPlan(id));on('#goal-replan',()=>goalPlan(id,true));
  on('#goal-plan-approve',()=>goalDecision(id,'approve-plan'));on('#goal-plan-reject',()=>goalDecision(id,'reject-plan'));
  on('#goal-final-approve',()=>goalDecision(id,'approve-final'));on('#goal-final-reject',()=>goalDecision(id,'reject-final'));
  on('#goal-request-acceptance',()=>goalRequestAcceptance(id));on('#extend',()=>goalExtension(id));
  on('#goal-back-list',()=>{$('#overlay').innerHTML='';page='goals';goalFilter='all';goalSearch='';render();});
  on('#goal-records',()=>{$('#overlay').innerHTML='';page='knowledge';search=g.id;render();});
  on('#goal-export',()=>goalDownload(new Blob([JSON.stringify(g,null,2)],{type:'application/json'}),g.id+'-目标记录.json'));
  on('#goal-versions',()=>goalVersions(id));
  document.querySelectorAll('[data-milestone-deliver]').forEach(b=>b.onclick=()=>goalDeliver(id,b.dataset.milestoneDeliver));
  document.querySelectorAll('[data-milestone-reopen]').forEach(b=>b.onclick=()=>goalReopen(id,b.dataset.milestoneReopen));
  document.querySelectorAll('[data-milestone-history]').forEach(b=>b.onclick=()=>goalEvidenceHistory(id,b.dataset.milestoneHistory));
  bindEvidenceDownloads(id);
}
function goalPlan(id,replan=false){
  const g=goalById(id);if(!g||!goalOwner(g)||!(g.stage===0||replan&&g.stage===4))return;
  let draft=structuredClone(g.milestones.length?g.milestones:[{id:newGoalId(),title:'',start:g.due<'2026-09-24'?g.due:'2026-09-24',end:g.due,status:'pending',evidence:[],previousEvidence:[]}]);
  modal('负责人制定目标方案',`<form id="goal-plan-form"><div class="detail-box"><b>${esc(g.title)}</b><br>要的结果：${esc(g.expected)}<br>目标截止：${g.due}</div><label class="field">验收标准<textarea id="goal-standard" required placeholder="描述可验证的标准、数量、质量或业务结果">${esc(g.standard||'')}</textarea></label><div class="row-title" style="margin:22px 0 12px"><h3>里程碑事项与时间</h3><button type="button" id="goal-ai-plan">✧ AI 拆解建议（演示）</button></div><div id="milestone-editor"></div><button type="button" id="goal-add-milestone">＋ 添加里程碑</button><label class="field" style="margin-top:22px">我的交付承诺<textarea id="goal-commitment" required placeholder="说明对交付结果和期限的承诺">${esc(g.commitment||'')}</textarea></label><label class="goal-checkbox"><input type="checkbox" id="goal-pledge">我确认验收标准和每项里程碑时间，并承诺按期提交成果。</label><div class="notice">提交后由老板审核；通过即启动。负责人对下属的任务安排在独立的“任务下发”栏目进行。</div><div class="modal-footer"><button type="button" id="goal-plan-cancel">返回</button><button type="button" id="goal-save-draft">保存草稿</button><button class="primary" type="submit">承诺并提交老板审核</button></div></form>`);
  $('.modal').classList.add('goal-modal');
  const collect=()=>{draft=Array.from(document.querySelectorAll('.milestone-edit')).map((el,n)=>({...draft[n],title:el.querySelector('[name="milestone-title"]').value.trim(),start:el.querySelector('[name="milestone-start"]').value,end:el.querySelector('[name="milestone-end"]').value}));};
  const draw=()=>{$('#milestone-editor').innerHTML=draft.map((m,n)=>`<div class="milestone-edit" data-index="${n}"><label class="field">里程碑 ${n+1}<input name="milestone-title" aria-label="里程碑事项 ${n+1}" value="${esc(m.title)}" required></label><label class="field">开始时间<input type="date" name="milestone-start" aria-label="开始时间 ${n+1}" value="${esc(m.start)}" max="${g.due}" required></label><label class="field">结束时间<input type="date" name="milestone-end" aria-label="结束时间 ${n+1}" value="${esc(m.end)}" max="${g.due}" required></label><button type="button" data-remove-milestone="${n}" aria-label="删除里程碑 ${n+1}" ${draft.length===1?'disabled':''}>删除</button></div>`).join('');document.querySelectorAll('[data-remove-milestone]').forEach(b=>b.onclick=()=>{collect();draft.splice(Number(b.dataset.removeMilestone),1);draw();});};draw();
  $('#goal-add-milestone').onclick=()=>{collect();draft.push({id:newGoalId(),title:'',start:g.due<'2026-09-24'?g.due:'2026-09-24',end:g.due,status:'pending',evidence:[],previousEvidence:[]});draw();};
  $('#goal-ai-plan').onclick=()=>{collect();if(draft.some(m=>m.title)){toast('已有里程碑已保留。先清空事项后再使用演示建议，避免覆盖你的方案。');return;}const start=g.planDue<=g.due?g.planDue:g.due;draft=['确认实施方案与验收口径','完成实施并形成可查验成果','核对最终结果并整理验收资料'].map(title=>({id:newGoalId(),title,start,end:g.due,status:'pending',evidence:[],previousEvidence:[]}));draw();toast('已生成规则示例，请按实际工作调整日期和事项；未连接真实模型。');};
  $('#goal-plan-cancel').onclick=()=>goalDetail(id);
  const submit=send=>{
    collect();const standard=$('#goal-standard').value.trim(),commitment=$('#goal-commitment').value.trim(),pledged=$('#goal-pledge').checked;
    if(send&&(!standard||!commitment||!pledged)){toast('请填写验收标准、交付承诺，并勾选承诺确认');return;}
    if(send&&(!draft.length||draft.some(m=>!m.title||!m.start||!m.end))){toast('每个里程碑都必须填写事项、开始和结束时间');return;}
    if(draft.some(m=>m.start&&m.end&&(m.start>m.end||m.end>g.due))){toast('里程碑开始时间不得晚于结束时间，结束时间不得超过目标期限');return;}
    goalCommit(id,(item,d)=>{requireGoal(goalOwner(item)&&(item.stage===0||replan&&item.stage===4),'当前状态不能修改方案');item.standard=standard;item.commitment=commitment;for(const m of draft){const old=item.milestones.find(x=>x.id===m.id);if(old&&(old.title!==m.title||old.start!==m.start||old.end!==m.end))m.status='pending';}item.milestones=draft;item.progress=goalProgress(item);if(send){item.planRevision++;item.planVersions.push({version:item.planRevision,at:goalNow(),standard,commitment,milestones:structuredClone(draft)});item.stage=1;item.pledgedAt=goalNow();item.rejection='';goalAudit(d,item,'负责人作出承诺，提交方案 v'+item.planRevision+' 给老板审核。');}else{item.stage=0;goalAudit(d,item,'保存方案草稿，尚未提交老板审核。');}},send?'方案已提交，老板待办已同步':'方案草稿已保存');
  };
  $('#goal-save-draft').onclick=()=>submit(false);$('#goal-plan-form').onsubmit=e=>{e.preventDefault();submit(true);};
}
function goalDecision(id,kind){
  const g=goalById(id);if(!g||role!==0)return;
  const expected=kind.includes('plan')?1:3;if(g.stage!==expected)return;
  const reject=kind.startsWith('reject'),final=kind.endsWith('final');const title=final?(reject?'验收不通过，驳回调整':'验收通过，完成目标'):(reject?'驳回负责人方案':'审核方案并立即启动');
  modal(title,`<div class="detail-box">${esc(g.title)}<br>验收标准：${esc(g.standard)}<br>负责人承诺：${esc(g.commitment)}<br>里程碑成果：${g.milestones.filter(goalComplete).length} / ${g.milestones.length}</div><form id="goal-decision-form"><label class="field">${reject?'驳回原因与修改要求（必填）':'审核 / 验收意见（选填）'}<textarea id="goal-decision-note" ${reject?'required':''}></textarea></label>${reject&&final?`<div class="notice">选择需要整改的里程碑。保留原有交付记录；所选事项需重新提交成果后才能再次申请验收。</div>${g.milestones.map(m=>`<label class="goal-checkbox"><input type="checkbox" name="rework-milestone" value="${m.id}" checked>${esc(m.title)}</label>`).join('')}`:''}<div class="modal-footer"><button type="button" id="goal-decision-cancel">返回</button><button class="primary">确认${reject?'驳回':final?'验收完成':'启动'}</button></div></form>`);
  $('#goal-decision-cancel').onclick=()=>goalDetail(id);
  $('#goal-decision-form').onsubmit=e=>{e.preventDefault();const note=$('#goal-decision-note').value.trim();const selected=Array.from(document.querySelectorAll('[name="rework-milestone"]:checked')).map(el=>el.value);if(reject&&!note){toast('请填写驳回原因');return;}if(reject&&final&&!selected.length){toast('请选择至少一个需要整改的里程碑');return;}
    goalCommit(id,(item,d)=>{
      requireGoal(role===0&&item.stage===expected,'当前状态已改变，不能重复审核');
      item.reviews.push({kind,note,at:goalNow(),planRevision:item.planRevision,milestones:structuredClone(item.milestones)});
      if(kind==='approve-plan'){requireGoal(item.standard&&item.commitment&&item.pledgedAt&&item.milestones.length&&item.milestones.every(m=>m.title&&m.start&&m.end&&m.start<=m.end&&m.end<=item.due),'方案未完整填写，不能启动');item.stage=2;item.startedAt=goalNow();item.rejection='';}
      if(kind==='reject-plan'){item.stage=0;item.rejection=note;}
      if(kind==='reject-final'){requireGoal(goalReady(item),'里程碑成果不完整');item.stage=4;item.rejection=note;item.milestones.forEach(m=>{if(selected.includes(m.id))m.status='rework';});item.progress=goalProgress(item);}
      if(kind==='approve-final'){requireGoal(goalReady(item),'全部里程碑成果齐备后才能完成');item.stage=5;item.completedAt=goalNow();item.rejection='';item.progress=100;}
      goalAudit(d,item,title+'。'+(note||'确认符合已提交标准。'));
      if(kind==='approve-final'){const docId='GOAL-RESULT-'+id;let doc=d.docs.find(x=>x.id===docId);if(!doc){doc={id:docId,title:item.title+' · 完成交付',category:'交付成果',scope:item.center===4?'财务':item.shop==='公司'?'内部':item.shop,center:item.center,goalId:id,version:0};d.docs.push(doc);}Object.assign(doc,{body:goalSummary(item),source:id+' · 老板最终验收',version:doc.version+1});}
    },title+'，相关工作台已同步');
  };
}
function goalRequestAcceptance(id){
  const g=goalById(id);if(!g||!goalOwner(g)||![2,4].includes(g.stage)||!goalReady(g)){toast('全部里程碑提交成果后才能申请验收');return;}
  goalCommit(id,(item,d)=>{requireGoal(goalOwner(item)&&[2,4].includes(item.stage)&&goalReady(item),'尚未具备提交验收条件');item.stage=3;item.acceptanceRequestedAt=goalNow();goalAudit(d,item,'全部 '+item.milestones.length+' 个里程碑已提交成果，申请老板统一验收。');},'已提交老板验收，等待老板确认');
}
let goalDBPromise;
function goalDB(){if(!goalDBPromise)goalDBPromise=new Promise((resolve,reject)=>{const req=indexedDB.open('chuangjie-goal-files',1);req.onupgradeneeded=()=>req.result.createObjectStore('files',{keyPath:'id'});req.onsuccess=()=>resolve(req.result);req.onerror=()=>{goalDBPromise=null;reject(Error('本地附件存储不可用，未提交成果'));};});return goalDBPromise;}
async function goalStoreFiles(files){const db=await goalDB();return new Promise((resolve,reject)=>{const tx=db.transaction('files','readwrite');files.forEach(file=>tx.objectStore('files').put(file));tx.oncomplete=()=>resolve();tx.onerror=()=>reject(Error('附件保存失败，未完成里程碑'));tx.onabort=()=>reject(Error('附件写入中断，未完成里程碑'));});}
async function goalReadFile(id){const db=await goalDB();return new Promise((resolve,reject)=>{const req=db.transaction('files').objectStore('files').get(id);req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(Error('附件读取失败'));});}
function goalDownload(blob,name){const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),2000);}
function bindEvidenceDownloads(goalId){document.querySelectorAll('[data-evidence-download]').forEach(b=>b.onclick=async()=>{const g=goalById(goalId);if(!g)return;const id=b.dataset.evidenceDownload;const all=g.milestones.flatMap(m=>[...(m.evidence||[]),...(m.previousEvidence||[]).flatMap(v=>v.evidence)]);const e=all.find(e=>e.id===id);if(!e)return;try{const record=await goalReadFile(id);if(!record)throw Error('该附件不在当前浏览器中，可能已清理本地存储');goalDownload(record.blob,e.name);}catch(error){toast(error.message);}});}
function goalDeliver(id,milestoneId){
  const g=goalById(id),m=g?.milestones.find(m=>m.id===milestoneId);if(!g||!m||!goalOwner(g)||![2,4].includes(g.stage))return;
  modal('提交里程碑成果',`<div class="detail-box"><b>${esc(m.title)}</b><br>${m.start} → ${m.end}<br>目标：${esc(g.title)}</div><form id="goal-delivery-form"><label class="field">上传资料或工作成果文件<input type="file" id="goal-evidence-files" multiple></label><p class="sub">文件真实保存在当前浏览器，刷新后可下载。单个文件最多 10 MB，每次合计最多 30 MB。</p><label class="field">工作成果内容<textarea id="goal-evidence-text" placeholder="也可直接提交报告、结论或其他可验收的工作成果内容"></textarea></label><label class="field" style="margin-top:15px">成果链接（选填）<input type="url" id="goal-evidence-url" placeholder="https://…"></label><div class="notice">至少提供文件、工作成果内容或有效链接之一。${m.evidence.length?'更新时将保留原交付版本；本次成果独立作为最新交付。':''}</div><div class="modal-footer"><button type="button" id="goal-delivery-cancel">返回</button><button class="primary" id="goal-delivery-submit">保存成果并完成该里程碑</button></div></form>`);
  $('#goal-delivery-cancel').onclick=()=>goalDetail(id);
  $('#goal-delivery-form').onsubmit=async e=>{
    e.preventDefault();const files=Array.from($('#goal-evidence-files').files),text=$('#goal-evidence-text').value.trim(),url=$('#goal-evidence-url').value.trim();
    if(!files.length&&!text&&!url){toast('请上传资料、填写工作成果或提供成果链接');return;}
    if(url){try{if(!['https:','http:'].includes(new URL(url).protocol))throw Error();}catch{toast('成果链接必须是有效的 HTTP 或 HTTPS 地址');return;}}
    if(files.some(f=>f.size>10*1024*1024)||files.reduce((n,f)=>n+f.size,0)>30*1024*1024){toast('附件超出大小限制：每个 10 MB，每次合计 30 MB');return;}
    const button=$('#goal-delivery-submit');button.disabled=true;button.textContent='正在保存成果…';
    try{
      const stored=files.map(f=>({id:newGoalId(),goalId:id,milestoneId,blob:f,name:f.name,size:f.size,type:f.type}));if(stored.length)await goalStoreFiles(stored);
      const evidence=stored.map(f=>({id:f.id,kind:'file',name:f.name,size:f.size,type:f.type,at:goalNow()}));if(text)evidence.push({id:newGoalId(),kind:'text',text,at:goalNow()});if(url)evidence.push({id:newGoalId(),kind:'link',url,at:goalNow()});
      goalCommit(id,(item,d)=>{requireGoal(goalOwner(item)&&[2,4].includes(item.stage),'目标已离开执行阶段，不能修改成果');const milestone=item.milestones.find(m=>m.id===milestoneId);requireGoal(milestone,'里程碑不存在');if(milestone.evidence.length)milestone.previousEvidence.push({at:milestone.completedAt||goalNow(),evidence:structuredClone(milestone.evidence)});milestone.evidence=evidence;milestone.status='complete';milestone.completedAt=goalNow();item.progress=goalProgress(item);goalAudit(d,item,'里程碑「'+milestone.title+'」完成，提交 '+evidence.length+' 份成果。');},'成果已保存，目标进度和相关工作台已更新');
    }catch(error){toast(error.message);}finally{if(button.isConnected){button.disabled=false;button.textContent='保存成果并完成该里程碑';}}
  };
}
function goalReopen(id,milestoneId){const g=goalById(id);if(!g||!goalOwner(g)||![2,4].includes(g.stage))return;modal('重新调整里程碑',`<form id="goal-reopen-form"><label class="field">调整原因<textarea id="goal-reopen-reason" required></textarea></label><p class="sub">保留已有成果。该里程碑恢复待调整状态，需重新提交成果才能申请总体验收。</p><div class="modal-footer"><button type="button" id="goal-reopen-cancel">返回</button><button class="primary">确认调整</button></div></form>`);$('#goal-reopen-cancel').onclick=()=>goalDetail(id);$('#goal-reopen-form').onsubmit=e=>{e.preventDefault();const reason=$('#goal-reopen-reason').value.trim();if(!reason)return;goalCommit(id,(item,d)=>{requireGoal(goalOwner(item)&&[2,4].includes(item.stage),'当前状态不能调整里程碑');const m=item.milestones.find(m=>m.id===milestoneId);requireGoal(m&&goalComplete(m),'该里程碑尚未完成');m.status='rework';item.progress=goalProgress(item);goalAudit(d,item,'重新调整「'+m.title+'」：'+reason);},'该里程碑已恢复待调整状态');};}
function goalEvidenceHistory(id,milestoneId){const g=goalById(id),m=g?.milestones.find(m=>m.id===milestoneId);if(!m)return;modal(m.title+' · 历史交付',`${m.previousEvidence.map((v,n)=>`<div class="detail-box"><b>历史交付 ${n+1}</b><p class="sub">${new Date(v.at).toLocaleString('zh-CN')}</p>${v.evidence.map(e=>e.kind==='file'?`<button data-evidence-download="${e.id}">↓ ${esc(e.name)}</button>`:e.kind==='link'?`<a href="${esc(e.url)}" target="_blank" rel="noopener noreferrer">${esc(e.url)}</a>`:`<p>${esc(e.text)}</p>`).join('')}</div>`).join('')}<button id="goal-history-back">返回目标</button>`);$('#goal-history-back').onclick=()=>goalDetail(id);bindEvidenceDownloads(id);}
function goalVersions(id){const g=goalById(id);if(!g)return;modal('方案版本与审核记录',`${g.planVersions.map(v=>`<div class="detail-box"><b>方案 v${v.version}</b> · ${new Date(v.at).toLocaleString('zh-CN')}<p>验收标准：${esc(v.standard)}</p><p>承诺：${esc(v.commitment)}</p>${v.milestones.map(m=>`<p>${esc(m.title)} · ${m.start} → ${m.end}</p>`).join('')}</div>`).join('')}<h3>老板审核与验收</h3>${g.reviews.map(r=>`<div class="detail-box">${({'approve-plan':'方案通过','reject-plan':'方案驳回','approve-final':'最终验收通过','reject-final':'最终验收驳回'})[r.kind]} · v${r.planRevision}<p>${esc(r.note||'通过')}</p><span class="sub">${new Date(r.at).toLocaleString('zh-CN')}</span></div>`).join('')||'<p class="sub">尚无审核记录</p>'}<button id="goal-version-back">返回目标</button>`);$('#goal-version-back').onclick=()=>goalDetail(id);}
function goalExtension(id){const g=goalById(id);if(!g||role!==0||g.stage===5)return;modal('目标延期审批',`<form id="goal-extension-form"><div class="notice">批准延期后保留原有灯色；已经红灯的目标仍保持红灯。里程碑原计划日期不会自动变更。</div><label class="field">新目标截止日期<input type="date" id="new-due" min="${g.due}" value="${g.due}" required></label><label class="field">审批理由<textarea id="reason" required></textarea></label><div class="modal-footer"><button type="button" id="goal-extension-back">返回</button><button type="button" id="deny-extension">拒绝延期</button><button class="primary" id="approve-extension">批准延期</button></div></form>`);$('#goal-extension-back').onclick=()=>goalDetail(id);const apply=ok=>{const reason=$('#reason').value.trim(),due=$('#new-due').value;if(!reason){toast('请填写审批理由');return;}if(ok&&(!due||due<=g.due)){toast('新期限必须晚于当前期限');return;}goalCommit(id,(item,d)=>{requireGoal(role===0&&item.stage!==5,'当前目标不可延期');if(ok){item.originalDue??=item.due;item.due=due;item.extended=true;item.extensionReason=reason;}else item.extensionDenied=true;goalAudit(d,item,(ok?'批准延期至 '+due:'拒绝延期，按原期限计时')+'：'+reason);},'延期审批已记录');};$('#goal-extension-form').onsubmit=e=>{e.preventDefault();apply(true);};$('#deny-extension').onclick=()=>apply(false);}
// Same-origin tabs update without copying a second set of business data.
window.addEventListener('storage',event=>{if(event.key!==key||!event.newValue)return;try{const incoming=JSON.parse(event.newValue);if(incoming.version!==1||!Array.isArray(incoming.items)||!Array.isArray(incoming.docs)||!Array.isArray(incoming.log))return;if($('#overlay')?.children.length){toast('另一页面更新了数据。请刷新后继续，当前未提交输入仍保留。');return;}state=incoming;migrateGoals();migrateTasks();render();}catch{toast('其他页面的数据更新未能读取，请刷新检查。');}});

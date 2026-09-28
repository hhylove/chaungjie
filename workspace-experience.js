/* Navigation and process presentation. Keeps existing handlers and business logic. */
const uxPaths={
 overview:'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
 goals:'M20 12a8 8 0 1 1-8-8 M12 8a4 4 0 1 0 4 4 M12 12l9-9 M16 3h5v5',
 dispatch:'M8 4h12v17H4V4h4 M8 2h8v4H8z M8 11h8 M8 16h5',
 tasks:'M9 5h12 M9 12h12 M9 19h12 M2 5l2 2 3-4 M2 12l2 2 3-4 M2 19l2 2 3-4',
 knowledge:'M12 5C8 2 4 3 2 4v15c4-2 7-1 10 1 3-2 6-3 10-1V4c-3-1-6-2-10 1z M12 5v15',
 settings:'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M5 19l2-2 M17 7l2-2',
 ai:'M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3z',
 gallery:'M3 3h18v18H3z M3 16l5-5 5 5 3-3 5 5 M16 7h.01',
 selection:'M3 3h18v18H3z M3 16l5-5 5 5 3-3 5 5 M16 7h.01',
 inventory:'M3 7l9-5 9 5v11l-9 4-9-4z M3 7l9 5 9-5 M12 12v10',
 stock:'M3 7l9-5 9 5v11l-9 4-9-4z M3 7l9 5 9-5 M12 12v10',
 potential:'M9 18h6 M9 22h6 M8 14a7 7 0 1 1 8 0l-1 3H9z',
 development:'M5 20l4-1L21 7l-4-4L5 15z M14 6l4 4 M3 22h18',
 lifecycle:'M20 9a8 8 0 0 0-14-4L3 8 M3 3v5h5 M4 15a8 8 0 0 0 14 4l3-3 M16 16h5v5',
 clearance:'M3 6h18v15H3z M1 3h22v4H1 M8 12h8 M12 10v7 M9 14l3 3 3-3',
 hr:'M8 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M1 22v-4a7 7 0 0 1 14 0v4 M17 4a4 4 0 0 1 0 8 M18 15c4 0 5 3 5 6',
 entrepreneurs:'M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M4 22v-3a8 8 0 0 1 16 0v3',
 photography:'M3 6h4l2-3h6l2 3h4v15H3z M12 9a4 4 0 1 0 0 8 4 4 0 0 0 0-8',
 video:'M3 4h18v16H3z M9 8l6 4-6 4z',
 service:'M3 4h18v13H9l-5 4v-4H3z M7 9h10 M7 13h6'
};
const uxIcon=id=>`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${uxPaths[id]||uxPaths.service}"/></svg>`;
const uxFlowDefinitions={
 task:{steps:[['任务下发','负责人','确认任务、交付标准、执行人和起止日期。'],['接收与执行','指定员工','先确认接收；执行中反馈进度、阻碍并准备成果。'],['负责人验收','下发负责人','对照验收标准检查成果；不符合要求时说明整改意见。'],['完成归档','系统','验收通过，任务完成并保留全过程记录。']],map:[1,1,2,1,3]},
 purchase:{steps:[['申请采购','事业主理人','填写采购理由、预估销量、采购数量和时长期望。'],['审核与指派','供应链负责人','审核必要性，指定采购文员和跟单期限。'],['采购跟单','采购文员','记录供应商沟通、采购进展及到货数量。'],['到货质检','采购文员','核对到货数量，提交质量检查结果和相关材料。'],['最终审核','供应链负责人','确认到货和质量符合要求后完成采购流程。'],['完成','系统','保存采购记录与质检资料。']],map:[1,2,3,4,5]},
 develop:{steps:[['开发立项','供应链负责人','核验开发理由、定价、受众定位、优势和风险，指定本中心设计师。'],['效果图制作','供应链设计师','完成设计说明并上传真实效果图；没有效果图不可提交审核。'],['效果图审核','供应链负责人','审核效果图，确定跟样人及期限。'],['谈价打样与跟踪','指定跟样人','持续记录报价、打样进展和样品到货情况。'],['样品品质验收','供应链负责人','检验样品品质；不合格退回跟样人继续处理。'],['开发完成','系统','归档开发依据、效果图和样品验收资料。']],map:[0,1,2,3,4,0,5]},
 gallerycycle:{steps:[['整理商品资料','供应链员工','上传供应商文件夹，分类并补齐名称、克重、工费等资料。'],['审核与发布','供应链负责人','检查本期供应商覆盖和商品资料，审核后发布。'],['同步运营选品','系统','发布商品展示到运营选品库，本期任务完成。']],map:[0,1,2]},
 potentialcycle:{steps:[['研究与记录','供应链员工','关联潜力商品，持续整理图片、想法和五个维度的证据。'],['主管评审','供应链负责人','逐项核验评分依据；退回的商品需继续补充。'],['本期完成','系统','关联商品全部评审通过后，本期任务完成。']],map:[0,1,2]},
 potential:{steps:[['记录想法与证据','供应链员工','填写搜索或曝光、类目销量、竞争机会、毛利空间、主观感受五维评分及证据。'],['主管评审','供应链负责人','根据五维评分和证据评审，必要时退回补充。'],['评审通过','系统','纳入潜力品成果统计，保留持续跟踪记录。']],map:[0,1,2]},
 clearance:{steps:[['制定清仓方案','供应链负责人','选择多个商品，填写原价、清仓价、目标销售额及执行项目。'],['销售与复盘','事业主理人','执行清仓，更新销售数据、预估金额和复盘意见。'],['评论与评分','指定评价人','检查执行结果并评论，给予1至5星评价。'],['完成归档','系统','保存方案、各店销售结果和评价。']],map:[0,1,2,3]},
 producttrial:{steps:[['接收销售任务','事业主理人','核对商品、所属店铺、统计期间和交付要求。'],['执行与填报','事业主理人','填写销售金额、曝光量、转化率及商品分析意见。'],['审核销售反馈','供应链负责人','确认数据和分析，退回时说明需要补充的内容。'],['更新商品档案','系统','通过后更新商品生命周期中的各店反馈。']],map:[0,1,2,3]},
 visual:{steps:[['审核与排期','视觉负责人','确认优先级、制作范围、执行人和计划日期。'],['接收任务','设计师 / 摄影师','确认制作要求和优先级，按排期开始工作。'],['制作并上传成果','指定执行人','上传图片或视频文件并填写交付说明，保留文件版本。'],['主管审核','视觉负责人','核对成果是否满足要求；驳回时写明修改意见。'],['完成与归档','系统','归档命名后的文件及审批记录。']],map:[0,1,2,3,4]},
 stock:{steps:[['逐码盘点','指定仓管员','按本次库存快照填写所有商品实盘数量，不能遗漏。'],['差异追因','指定仓管员','逐款标记盘亏或盘盈，记录原因及问题点。'],['负责人复核','仓储负责人','盘亏记录损失金额，盘盈分析原因，正常库存填写优化方向。'],['财务处理','财务人员','复核盘点结果并记录处理说明，必要时退回复核。'],['盘点结束','系统','归档盘点清单、差异说明和处理记录。']],map:[0,1,2,3,4]},
 service:{steps:[['受理与分配','创业者发展中心','确认服务范围，受理后指定承接人；拒绝须说明原因。'],['承接与交付','指定承接人','按要求完成服务并提交成果资料。'],['主理人验收','申请主理人','检查交付结果，验收不通过时写明需要调整的内容。'],['服务完成','系统','保存申请、交付与验收记录。']],map:[0,1,2,0,1,3]}
};
function uxWorkflow(type,record,preview=false){
 const spec=uxFlowDefinitions[type];if(!spec)return '';
 const index=preview?0:(spec.map[record.stage]??0),finished=!preview&&record.stage===stages[type].length-1;
 const rejected=!preview&&(record.stage===3&&type==='task'||record.stage===5&&type==='develop'||record.stage===3&&type==='service'||record.stage===4&&type==='service'||!!record.blocked);
 const current=spec.steps[index],stopped=record.governance?.ended?'已终止':record.governance?.withdrawn?'已撤回':'';if(stopped)return `<section class="ux-workflow" aria-label="完整流程"><div class="ux-workflow-head"><strong>完整流程</strong><span>${stopped} · 不计完成</span></div><ol class="ux-steps">${spec.steps.map(([name,owner],n)=>`<li class="${n<index?'done':'future'}"><span class="ux-step-index">${n+1}</span><div><strong>${name}</strong><small>${owner}<br>${n<index?'已流转':n===index?'在此停止':'未执行'}</small></div></li>`).join('')}</ol></section>`;
 return `<section class="ux-workflow" aria-label="${preview?'办理路径':'完整流程'}"><div class="ux-workflow-head"><strong>${preview?'办理路径':'完整流程'}</strong><span>${preview?'提交后按顺序流转':finished?'全部步骤已完成':`第 ${index+1} / ${spec.steps.length} 步${rejected?' · 需补充或调整':''}`}</span></div><ol class="ux-steps">${spec.steps.map(([name,owner],n)=>`<li class="${preview?'future':finished||n<index?'done':n===index?'current':'future'}" ${!preview&&!finished&&n===index?'aria-current="step"':''}><span class="ux-step-index">${!preview&&(finished||n<index)?'✓':n+1}</span><div><strong>${name}</strong><small>${owner}<br>${preview?'':finished||n<index?'已完成':n===index?'当前步骤':'待进行'}</small></div></li>`).join('')}</ol>${preview?'':`<div class="ux-current ${rejected?'rework':''}"><small>${finished?'流程结果':rejected?'当前需补充 / 调整':'当前办理事项'}</small><strong>${current[0]}${record.due&&!finished?' · 截止 '+esc(record.rectifyDue||record.nodeDue||record.due):''}</strong><p>${rejected&&record.blocked?esc(record.blocked):current[2]}</p></div>`}</section>`;
}
const uxRouteGuides={
 dispatch:['任务办理路径',['明确目标与要求','指定执行人和期限','员工接收与交付','负责人验收归档'],'普通、重要、紧急分别标识；驳回后保留原成果，再提交修改版本。'],
 selection:['从选品到采购完成',['浏览商品 / 查看大图','填写采购理由与数量','供应链审核并指派','跟单、质检与终审'],'采购不经过财务审核；完成采购流程与ERP实际入库是不同事件。'],
 inventory:['库存查看说明',['确认所属店铺','核对商品数量','查看重量与工费'],'当前展示所属项目的演示数据，尚未连接ERP。'],
 productwork:['商品销售任务',['接收指定商品任务','执行并填报销售数据','供应链负责人审核','更新商品档案'],'曝光量填写次数，转化率填写百分比；人工反馈与ERP经营数据分开保存。'],
 gallery:['每周图片整理流程',['系统按周生成任务','上传供应商产品文件夹','分类并补齐商品资料','负责人审核 / 同步运营'],'可以先保存整理进展；未发布商品不会展示到运营选品界面。'],
 potential:['每月两次潜力品研究',['记录图片与想法','持续补充五维评分和证据','提交主管评审','通过后纳入成果统计'],'五个维度：搜索或曝光、类目销量、竞争机会、毛利空间、主观感受。'],
 clearance:['清仓协作流程',['供应链制定方案','指定项目接收和执行','主理人填写数据与复盘','负责人评论与五星评价'],'先用月份、状态、项目和商品编码定位任务；历史未完事项持续保留。'],
 development:['产品开发完整路径',['明确开发依据','本中心设计师出效果图','负责人审核并安排跟样','谈价打样与样品品质验收'],'开发理由、定价、受众定位、优势和风险必须完整；效果图与样品验收不能跳过。'],
 lifecycle:['商品档案与反馈',['按ERP编码建立档案','查看各店销售与退款','下发测款或清仓','审核结果回写商品档案'],'ERP数据、人工填报、审批结论分别展示，所有反馈保留来源。'],
 stock:['盘点完整流程',['生成季度大盘 / 月度抽盘','逐码盘点与差异追因','仓库负责人复核','财务处理后结案'],'月度抽盘50–150款；每次任务保存库存快照，翻页和刷新不会重新抽样。'],
 photography:['图片制作流程',['主管确认优先级与排期','设计师接收任务','上传图片与成果说明','主管审核并归档'],'按商品编码自动命名，保留每次交付版本。'],
 video:['视频制作流程',['主管确认优先级与排期','摄影师接收任务','上传视频成果','主管审核并归档'],'制作顺序优先关注紧急和重要任务，具体完成时间以排期为准。'],
 received:['部门协作制作流程',['接收视觉服务需求','视觉负责人排期与分配','设计师 / 摄影师交付','按原服务要求审核完成'],'直接视觉服务由视觉主管验收；经发展中心协调的其他服务保留主理人验收。'],
 service:['平台服务分两条路径',['视觉服务 → 视觉负责人','其他服务 → 发展中心','指定承接人交付成果','按对应流程完成验收'],'视觉服务由视觉主管审核完成；其他服务由申请主理人验收完成。'],
 platformservices:['其他平台服务办理',['发展中心受理或说明原因拒绝','指定承接人和期限','交付成果','主理人验收'],'主理人直接提出的视觉服务在视觉中心处理，无需发展中心再次受理。'],
 requests:['开发协作路径',['确认业务需求和交付范围','安排负责人和时间','提交实施成果','发起方验收'],'当前协作事项沿用平台服务流程；页面中的建设规划为示例。'],
 knowledge:['公司知识的使用方式',['搜索获授权资料','核对来源和版本','查看关联目标 / 任务','记录并复用经验'],'通过任务或目标详情可追溯原始成果；模型和服务器知识检索尚未接入。']
};
function uxGuideMarkup(def){return `<details class="ux-route-guide"><summary><strong>${def[0]}</strong><span>展开查看整个过程</span></summary><ol>${def[1].map((s,n)=>`<li><b>${String(n+1).padStart(2,'0')}</b><span>${s}</span></li>`).join('')}</ol><p>${def[2]}</p></details>`;}
const uxOriginalModal=modal;
modal=function(title,body){
 const record=typeof activeRecordId!=='undefined'?state.items.find(i=>i.id===activeRecordId&&visible(i)):null;
 let flowRecord=record,flowType=record?.type;
 if(record?.type==='service'&&record.serviceKind==='visual'){flowRecord=state.items.find(i=>i.id===record.visualTaskId);flowType='visual';}
 if(record?.lifecycleCode&&['producttrial','clearance'].includes(record.type))flowType='producttrial';
 if(body.includes('class="flow"')&&flowRecord&&uxFlowDefinitions[flowType])body=body.replace(/<div class="flow">[\s\S]*?<\/div>/,uxWorkflow(flowType,flowRecord));
 if(flowRecord&&!body.includes('ux-workflow')&&((uxFlowDefinitions[flowType]&&title===record.title)||/id="task-(?:update|deliver|review|extension|extension-review)-form"/.test(body)))body=uxWorkflow(flowType,flowRecord)+body;
 const forms=[['id="task-create-form"','task'],['id="commerce-create"',title.includes('采购')?'purchase':'service'],['id="development-create"','develop'],['id="life-task-create"','producttrial'],['id="potential-create"','potential']];
 if(!body.includes('ux-workflow')&&!body.includes('goal-process')){const match=forms.find(([id])=>body.includes(id));if(match)body=uxWorkflow(match[1],{},true)+body;}
 uxOriginalModal(title,body);
};
const uxOriginalRender=render;
let uxLastView='';
render=function(){const view=role+':'+page;uxOriginalRender();uxDecorateShell();if(view!==uxLastView){window.scrollTo({top:0,left:0,behavior:'instant'});uxLastView=view;}};
listPanel=function(records){
 const done=i=>i.stage===stages[i.type].length-1,pending=records.filter(canAct).length,late=records.filter(i=>!done(i)&&i.light!=='green').length;
 return `<section class="panel ux-record-list"><div class="panel-head"><div><h2>工作事项 <span class="badge">${records.length}</span></h2><p>待我处理 ${pending} · 延期 ${late} · 已完成 ${records.filter(done).length}</p></div></div><div class="ux-list-tools"><label>查找事项<input class="ux-local-search" type="search" placeholder="事项名称、编号、负责人或店铺" aria-label="查找当前列表事项"></label><label>办理状态<select class="ux-local-state"><option value="all">全部事项</option><option value="mine">待我处理</option><option value="late">延期事项</option><option value="done">已完成</option></select></label><span class="ux-list-count">${records.length} 条</span></div><div class="table-wrap"><table><thead><tr><th>事项 / 编号</th><th>负责人 / 项目</th><th>当前环节</th><th>截止时间</th><th>状态</th><th>操作</th></tr></thead><tbody>${records.map(i=>`<tr data-ux-record data-query="${esc([i.title,i.id,i.owner,i.shop].join(' ').toLowerCase())}" data-mine="${canAct(i)}" data-done="${done(i)}" data-late="${!done(i)&&i.light!=='green'}"><td><b>${esc(i.title)}</b> ${taskPriorityBadge(i)}<br><span class="sub">${esc(i.id)} · ${names[i.type]}</span></td><td>${esc(i.owner)}<br><span class="sub">${esc(i.shop)}</span></td><td>${serviceStageLabel(i)}</td><td>${esc(i.due)}${i.extended?'<br><span class="sub">已延期 · 保留履约记录</span>':''}</td><td>${badge(i)}</td><td><button class="${canAct(i)?'primary':''}" data-open="${i.id}">${canAct(i)?'去办理':'查看详情'} →</button></td></tr>`).join('')}</tbody></table></div><div class="empty ux-list-empty" ${records.length?'hidden':''}>当前没有匹配事项，可以调整搜索词或办理状态。</div></section>`;
};
function uxDecorateShell(){
 const sidebar=document.querySelector('#app>aside');if(!sidebar)return;
 sidebar.id='workspace-navigation';
 sidebar.querySelector('.brand').innerHTML='<span class="brand-mark">创</span><span class="brand-copy">创界<small>WORKSPACE</small></span>';
 sidebar.querySelector(':scope>.nav-label')?.classList.add('workspace-caption');
 const nav=sidebar.querySelector('.nav'),center=nav.querySelector('.center-nav');
 const groups={overview:'工作总览',goals:'目标与协作',dispatch:'目标与协作',ai:'智能助手',hr:'人员与组织',entrepreneurs:'人员与组织'};
 let previous='';Array.from(center.querySelectorAll('button')).forEach(b=>{const group=groups[b.dataset.page]||'业务工作';if(group!==previous){const label=document.createElement('div');label.className='nav-group-title';label.textContent=group;b.before(label);previous=group;}});
 nav.querySelectorAll('button[data-page]').forEach(b=>{
   const id=b.dataset.page;const label=b.textContent.replace(b.querySelector('.icon')?.textContent||'','').trim();
   b.setAttribute('aria-label',label);b.title=label;b.innerHTML=`<span class="icon">${uxIcon(id)}</span><span class="nav-text">${esc(label)}</span>`;
   if(b.classList.contains('active'))b.setAttribute('aria-current','page');
   if(id==='ai')b.classList.add('nav-ai');
   if(id==='tasks'){const count=items().filter(canAct).length+crossApprovals().filter(x=>crossCanAct(x)&&!approvalOperationalKinds.has(x.kind)).length;if(count){const c=document.createElement('span');c.className='nav-count';c.textContent=count;b.append(c);}}
 });
 const top=document.querySelector('.main>header'),crumb=top.querySelector('.crumb'),locationBox=document.createElement('div');locationBox.className='header-location';crumb.before(locationBox);locationBox.append(crumb);
 const toggle=document.createElement('button');toggle.type='button';toggle.className='nav-toggle';toggle.setAttribute('aria-label','展开或收起导航');toggle.setAttribute('aria-controls','workspace-navigation');toggle.textContent='☰';locationBox.prepend(toggle);
 const shade=document.createElement('button');shade.className='nav-shade';shade.type='button';shade.setAttribute('aria-label','关闭导航');document.getElementById('app').append(shade);
 const update=()=>toggle.setAttribute('aria-expanded',String(innerWidth<=800?document.body.classList.contains('workspace-nav-open'):!document.body.classList.contains('workspace-collapsed')));
 toggle.onclick=()=>{document.body.classList.toggle(innerWidth<=800?'workspace-nav-open':'workspace-collapsed');update();};shade.onclick=()=>{document.body.classList.remove('workspace-nav-open');update();};update();
 nav.addEventListener('click',e=>{if(e.target.closest('[data-page]'))document.body.classList.remove('workspace-nav-open');},true);
 const def=uxRouteGuides[page];if(def)document.querySelector('.main>main>.page-head')?.insertAdjacentHTML('afterend',uxGuideMarkup(def));
 document.querySelectorAll('.ux-record-list').forEach(panel=>{const apply=()=>{const query=panel.querySelector('.ux-local-search').value.trim().toLowerCase(),status=panel.querySelector('.ux-local-state').value;let n=0;panel.querySelectorAll('[data-ux-record]').forEach(row=>{row.hidden=!row.dataset.query.includes(query)||(status!=='all'&&row.dataset[status]!=='true');if(!row.hidden)n++;});panel.querySelector('.ux-list-count').textContent=n+' 条';panel.querySelector('.ux-list-empty').hidden=n>0;};panel.querySelector('.ux-local-search').oninput=apply;panel.querySelector('.ux-local-state').onchange=apply;});
 document.querySelectorAll('.main iframe').forEach(f=>{f.style.borderColor='#dce7ed';f.style.borderRadius='14px';f.style.height='calc(100dvh - 230px)';f.style.minHeight='540px';});
}
document.addEventListener('keydown',e=>{if(e.key==='Escape')document.body.classList.remove('workspace-nav-open');});
uxDecorateShell();

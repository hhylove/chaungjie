export const today=()=>{const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
const text=(v,label)=>{if(typeof v!=='string'||!v.trim())throw Error('请填写'+label);return v.trim();};
const money=(v,label,allowZero=false)=>{if(v===''||v==null||!Number.isFinite(Number(v))||(allowZero?Number(v)<0:Number(v)<=0))throw Error(label+'须为'+(allowZero?'非负':'正')+'数');return Math.round(Number(v)*100)/100;};
const dateValid=v=>/^\d{4}-\d{2}-\d{2}$/.test(v)&&!Number.isNaN(Date.parse(v+'T12:00:00Z'))&&new Date(v+'T12:00:00Z').toISOString().slice(0,10)===v;
const find=(s,id)=>{const p=s.projects.find(p=>p.id===id);if(!p)throw Error('项目不存在');return p;};
const roleCheck=(role,expected)=>{if(role!==expected)throw Error('此步骤须由'+expected+'处理');};
const log=(p,role,action,note='')=>p.history.push({at:new Date().toISOString(),role,action,note});
export function emptyState(){return {version:1,revision:0,projects:[]};}
export function importLegacy(source){
 const state=emptyState();for(const p of source?.ventures||[]){if(!Array.isArray(p.phases)||p.phases.length!==3)continue;const rates=[0,0,0],reports=[];const monthSet=[...new Set((p.months||[]).map(m=>m.month))].sort();for(const month of monthSet){const rows=p.months.filter(m=>m.month===month);for(const m of rows)if(m.phase>=0&&m.phase<3)rates[m.phase]=Number(m.rate)||0;reports.push({id:crypto.randomUUID(),month,used:Math.round(rows.reduce((n,m)=>n+(Number(m.used)||0),0)*100)/100,rates:[...rates],assessment:rows.map(m=>`第${Number(m.phase)+1}阶段：${m.note||''}`).join('\n'),updatedAt:rows.at(-1)?.date||month+'-01',by:'创业者发展中心总监',legacyFiles:rows.flatMap(m=>m.files||[])});}
 state.projects.push({id:p.id,name:p.title,owner:source.founders?.find(f=>f.id===p.founderId)?.name||p.owner||'待补充负责人',shop:p.shop||'',license:p.license||'',phases:structuredClone(p.phases),status:p.status==='管理中'?'管理中':p.status==='已驳回'?'已驳回':'待总经理审核',createdAt:p.history?.[0]?.time?.slice(0,10)||today(),approvedAt:p.status==='管理中'?(p.history?.find(h=>h.action==='ventureReview')?.time?.slice(0,10)||null):null,reports,reportHistory:[],history:[{at:new Date().toISOString(),role:'系统',action:'迁入原项目记录',note:'项目改为独立管理，原记录保留备查'}],legacy:structuredClone(p)});
 }return state;
}
export function submitProject(state,input,role,now=today()){
 roleCheck(role,'创业者发展中心总监');const name=text(input.name,'项目名称'),owner=text(input.owner,'项目负责人'),shop=String(input.shop||'').trim(),license=String(input.license||'').trim();if(!shop&&!license)throw Error('店铺与营业执照至少填写一项；两项都有请同时填写');
 if(!Array.isArray(input.phases)||input.phases.length!==3)throw Error('必须填写三个阶段的预算、目标和截止日期');let prior='';const phases=input.phases.map((p,i)=>{const goal=text(p.goal,`第${i+1}阶段目标`),budget=money(p.budget,`第${i+1}阶段预算`);if(!dateValid(p.due)||p.due<now||(prior&&p.due<=prior))throw Error('三个阶段截止日期须不早于今天，且依次递增');prior=p.due;return {goal,budget,due:p.due};});
 let p;if(input.id){p=find(state,input.id);if(p.status!=='已驳回')throw Error('仅被驳回项目可修改重提');p.applicationHistory??=[];p.applicationHistory.push({name:p.name,owner:p.owner,shop:p.shop,license:p.license,phases:structuredClone(p.phases)});Object.assign(p,{name,owner,shop,license,phases,status:'待总经理审核'});}else{p={id:crypto.randomUUID(),name,owner,shop,license,phases,status:'待总经理审核',createdAt:now,approvedAt:null,reports:[],reportHistory:[],history:[]};state.projects.unshift(p);}log(p,role,input.id?'修改后重新提交':'建立项目并提交总经理审核');return p;
}
export function reviewProject(state,id,result,note,role,now=today()){
 roleCheck(role,'总经理');const p=find(state,id);if(p.status!=='待总经理审核')throw Error('项目已处理，请刷新后查看');if(!['通过','驳回'].includes(result))throw Error('请选择审批结果');const comment=text(note,'审核意见');p.status=result==='通过'?'管理中':'已驳回';if(result==='通过')p.approvedAt=now;log(p,role,'项目审核'+result,comment);return p;
}
export function saveReport(state,id,input,role,now=today()){
 roleCheck(role,'创业者发展中心总监');const p=find(state,id);if(p.status!=='管理中')throw Error('总经理审核通过后才能更新月度记录');if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(input.month)||input.month>now.slice(0,7)||(p.approvedAt&&input.month<p.approvedAt.slice(0,7)))throw Error('月份须在项目批准当月至当前月份之间');
 const used=money(input.used,'本月预算使用金额',true),assessment=text(input.assessment,'项目评估');if(!Array.isArray(input.rates)||input.rates.length!==3||input.rates.some(v=>v===''||v==null||!Number.isFinite(Number(v))||Number(v)<0||Number(v)>100))throw Error('三个阶段达成率均须填写0–100之间的数字');
 const previous=p.reports.find(r=>r.month===input.month),report={id:previous?.id||crypto.randomUUID(),month:input.month,used,rates:input.rates.map(Number),assessment,updatedAt:new Date().toISOString(),by:role};if(previous){p.reportHistory.push(structuredClone(previous));Object.assign(previous,report);}else p.reports.push(report);p.reports.sort((a,b)=>a.month.localeCompare(b.month));log(p,role,previous?'修订月度陪跑记录':'更新月度陪跑记录',input.month+' · '+assessment);return p;
}
export function summary(p,now=today(),asOf=''){
 const reports=p.reports.filter(r=>!asOf||r.month<=asOf).slice().sort((a,b)=>a.month.localeCompare(b.month)),latest=reports.at(-1),rates=latest?.rates||[0,0,0],budget=p.phases.reduce((n,x)=>n+Number(x.budget),0),used=reports.reduce((n,x)=>n+x.used,0);const evalDay=asOf&&asOf<now.slice(0,7)?new Date(Date.UTC(Number(asOf.slice(0,4)),Number(asOf.slice(5,7)),0,12)).toISOString().slice(0,10):now;
 const delayed=p.status==='管理中'?p.phases.map((phase,i)=>phase.due<evalDay&&rates[i]<100?i:-1).filter(i=>i>=0):[];
 return {budget,used,remaining:budget-used,latest,rates,delayed,overBudget:used>budget,missingMonth:p.status==='管理中'&&!reports.some(r=>r.month===(asOf||now.slice(0,7))),reached:rates.filter(n=>n===100).length};
}

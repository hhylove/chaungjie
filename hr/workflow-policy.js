// One policy for embedded business forms and the shared inbox.
export const businessDay=(value=new Date())=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value));
export const addDays=(value,n)=>businessDay(new Date(Date.parse(value+'T12:00:00+08:00')+n*86400000));
export function validDate(value){return /^\d{4}-\d{2}-\d{2}$/.test(value||'')&&!Number.isNaN(Date.parse(value))&&new Date(value+'T12:00:00Z').toISOString().slice(0,10)===value;}
export function deadlineLight(due,{now=new Date(),previous='green',extended=false,done=false}={}){
 if(done)return 'done';if(previous==='red')return 'red';
 const elapsed=new Date(now)-new Date(due+'T23:59:59.999+08:00');
 return elapsed>=3*86400000?'red':elapsed>0||extended||previous==='yellow'?'yellow':'green';
}
export function docReviewer(r){return r.subject?'创业发展总监':r.category==='制度规范'||r.kind==='template'?(r.baseVersion>0&&r.changeLevel==='一般维护'?'创业发展总监':'总经理'):'创业发展总监';}
export function ensureWorkflow(x,stage,{now=businessDay(),totalDue,days=2}={}){
 if(!x.workflow)x.workflow={created:now,totalDue:totalDue||addDays(now,14),nodes:{},events:[],legacy:!x.created&&!x.date&&!x.createdAt};
 const w=x.workflow;if(!validDate(w.totalDue))throw Error('请填写有效的流程总期限');w.nodes??={};w.events??=[];
 if(!w.nodes[stage])w.nodes[stage]={entered:now,due:[addDays(now,days),w.totalDue].sort()[0]};
 w.stage=stage;return w;
}
export function recordAction(x,action,{person,personId,center,role,note,result,at=new Date().toISOString()}={}){
 const w=ensureWorkflow(x,x.workflow?.stage||x.status||String(x.stage??'办理'));
 w.events.push({id:crypto.randomUUID(),action,person:person||role,personId,center,role,note,result,at});
}
export function workflowLifecycle(s,v,role){
 const groups=['docRequests','purchases','sales','founderCandidates','requests','processes','founderAssets'];
 if(!groups.includes(v.collection))throw Error('此事项不支持撤回或终止');
 const x=s[v.collection]?.find(x=>x.id===v.id);if(!x)throw Error('事项不存在');
 if(x.governance?.ended)throw Error('事项已终止，不可再次办理');
 if(x.closed||['已通过','已入库','已完成','已立项','使用中','已耗用','财务结案'].includes(x.status))throw Error('已完成或已发生交付的事项不能取消，请办理后续更正流程');
 if(!v.note?.trim())throw Error('请填写原因');
 const identity=v.actorIdentity||{},creator=x.workflow?.events?.[0],isCreator=creator?.personId?creator.personId===identity.personId:role==='人事专员';
 const approver=v.collection==='docRequests'?docReviewer(x):v.collection==='sales'||v.collection==='purchases'?'财务负责人':v.collection==='founderCandidates'?'总经理':'创业发展总监';
 const started=x.tasks?.some(t=>t.done)||(x.workflow?.events||[]).some(e=>/Review|Advance|审核|审批|验收/.test(e.action)&&e.result!=='驳回')||['采购入库','上级确认','待销售','待派发'].includes(x.status)||Number(x.stage)>0;
 x.governance??={};
 if(v.decision==='审批'){
  if(role!==approver||x.governance.termination?.status!=='待审核')throw Error('当前身份无权审批终止');
  if(!['通过','驳回'].includes(v.result))throw Error('请选择审批结果');
  x.governance.termination.status=v.result;x.governance.termination.review=v.note;
  if(v.result==='通过'){x.governance.ended=true;x.governance.endedAt=new Date().toISOString();}
 }else{
  if(!isCreator)throw Error('仅原发起人可以撤回或申请终止');
  if(v.decision==='恢复'){if(!x.governance.withdrawn)throw Error('事项未撤回');x.governance.withdrawn=false;}
  else if(!started)x.governance.withdrawn=true;
  else x.governance.termination={status:'待审核',reason:v.note,approver,applicant:identity,at:new Date().toISOString()};
 }
 if((x.governance.ended||x.governance.withdrawn)&&v.collection==='sales'){const m=s.materials.find(m=>m.id===x.materialId);if(m?.status==='销售处理中')m.status=x.previousStatus;}
 if(v.decision==='恢复'&&v.collection==='sales'){const m=s.materials.find(m=>m.id===x.materialId);if(!m||m.status!==x.previousStatus)throw Error('库存已变化，不能恢复原销售申请');m.status='销售处理中';}
 recordAction(x,'撤回终止管理',{...identity,role,note:v.note,result:v.result});return x;
}
export function requireRework(v,totalDue){
 if(v.result!=='驳回')return;
 if(!v.note?.trim())throw Error('请说明驳回原因');
 if(!validDate(v.rectifyDue)||v.rectifyDue<businessDay())throw Error('请指定不早于今天的整改提交期限');
 if(totalDue&&v.rectifyDue>totalDue)throw Error('整改期限超过流程总期限，请先办理延期');
}
export function applyRework(x,v,owner='人事专员'){
 if(v.result==='驳回')x.rework={note:v.note,due:v.rectifyDue,owner,status:'待整改',at:new Date().toISOString()};
 else if(x.rework)x.rework.status='已重新提交';
}
export function extendWorkflow(x,due,note,approved){
 const w=x.workflow;if(!w)throw Error('流程期限不存在');
 if(!note?.trim())throw Error('请填写审批意见');
 if(approved){if(!validDate(due)||due<=w.totalDue)throw Error('新期限必须晚于现期限');w.originalDue??=w.totalDue;w.totalDue=due;w.extended=true;w.light=deadlineLight(w.nodes[w.stage]?.due||w.originalDue,{previous:w.light});if(w.light!=='red')w.light='yellow';if(w.nodes[w.stage])w.nodes[w.stage].due=due;}
 recordAction(x,approved?'批准延期':'拒绝延期',{role:'流程负责人',note});
}

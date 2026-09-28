import {workflowLifecycle,docReviewer,ensureWorkflow,recordAction,requireRework,applyRework} from './workflow-policy.js';
import {uid,day,audit,makeProcess} from './engine.js';
export const recruiterStages=['候选人','面谈','人员评估','项目计划书','项目评估','立项'];
export function migrate(s){
  for(const k of ['docRequests','materials','purchases','sales','movements','founderNeeds','founderCandidates','founders','ventures'])s[k]??=[];
  s.founderAssets??=[];
  for(const f of s.founders){const p=s.ventures.find(p=>p.founderId===f.id&&p.status==='管理中');if(p?.phases?.[0]){f.testGoal??=p.phases[0].goal;f.testDue??=p.phases[0].due;f.testBudget??=p.phases[0].budget;}f.job??='事业主理人';f.project??=s.ventures.filter(p=>p.founderId===f.id).map(p=>p.title).join('、')||s.founderNeeds.find(n=>n.id===s.founderCandidates.find(c=>c.id===f.candidateId)?.needId)?.title||'';if(!['测试','正式'].includes(f.status))f.status=s.ventures.some(p=>p.founderId===f.id&&p.releasedPhase>=1)?'正式':'测试';}
  for(const c of s.candidates)if(c.stage==='Offer'){c.stage='待入职';c.history.push({stage:'流程调整',role:'系统',note:'取消入职前 Offer 签收，保留录用审批结果',time:new Date().toISOString()});}
  return s;
}
const requireRole=(r,...allowed)=>{if(!allowed.includes(r))throw Error('本步骤须由'+allowed.join(' / ')+'处理');};
const required=(v,label)=>{if(!String(v??'').trim())throw Error('请填写'+label);return String(v).trim();};
const amount=(v,label,zero=false)=>{const n=Number(v);if(v===''||v==null||!Number.isFinite(n)||(zero?n<0:n<=0))throw Error(label+'必须为'+(zero?'非负':'正')+'数');return n;};
const integer=(v,label)=>{const n=amount(v,label);if(!Number.isInteger(n))throw Error(label+'必须为整数');return n;};
const find=(xs,id)=>{const x=xs.find(x=>x.id===id);if(!x)throw Error('记录不存在');return x;};
const status=(x,value)=>{if(x.status!==value)throw Error('该记录状态已变化，请刷新后处理');};
const history=(x,role,action,note='')=>{(x.history??=[]).push({time:new Date().toISOString(),role,action,note});};
const evidence=(v)=>required(v.note,'处理意见 / 依据');
export function remaining(m,now=day()){
  if(m.kind==='易耗品')return m.value;
  const years=(new Date(now+'T12:00:00')-new Date(m.date+'T12:00:00'))/(365.25*864e5);
  return Math.round(Math.max(0,m.value*(1-Math.max(0,years)/m.years))*100)/100;
}
export function ready(s,f){return ['经营合作协议','创界平台服务协议'].every(category=>s.documents.some(d=>d.subject===f.id&&d.category===category&&d.versions.length))&&f.setupStatus==='已确认';}
export function recipients(s){return [...s.employees.filter(e=>e.status!=='已离职').map(e=>({id:e.id,name:e.name,kind:'员工'})),...s.founders.filter(f=>!f.employeeId).map(f=>({id:f.id,name:f.name,kind:f.relation==='雇佣'?'雇佣主理人':'合作主理人'}))];}
function transitionDoc(s,v,role){
  requireRole(role,'人事专员','创业发展总监');
  const old=v.resubmit?find(s.docRequests,v.resubmit):null;
  if(old){status(old,'已驳回');if(old.applicant!==role)throw Error('请由原申请角色修改重提');}
  const d=v.docId?find(s.documents,v.docId):null;
  if(d?.subject&&v.category!==d.category)throw Error('已签署协议不能改为另一种类型，请从人员档案分别上传');
  if(d&&s.docRequests.some(x=>x.docId===d.id&&x.status==='待审批'))throw Error('该文件已有待审批更新');
  const title=required(v.title,'文件名称'),category=required(v.category,'分类'),reason=required(v.reason,'新增 / 更新原因');
  if(!v.content?.trim()&&!v.files?.length)throw Error('请填写完整内容或上传文件');
  const ids=[...new Set(v.recipients||[])];if(ids.some(id=>!s.employees.some(e=>e.id===id&&e.status!=='已离职')))throw Error('签收对象已离职或不存在');
  if(v.subject){find(s.founders,v.subject);if(!v.files?.length)throw Error('合作协议须上传双方已签署的协议文件');}
  if(v.kind==='template'&&!v.files?.length)throw Error('合同模板须上传文件');
  if(v.subject&&s.docRequests.some(x=>x.subject===v.subject&&x.category===category&&x.status==='待审批'))throw Error('此类协议已有待审批申请');
  const r={kind:d?.kind||v.kind||(v.subject?'signed':''),changeLevel:v.changeLevel||'关键内容',id:uid(),docId:d?.id||null,baseVersion:d?.versions.length||0,title,category,subject:d?.subject||v.subject||'',content:v.content||'',files:v.files||[],reason,recipients:ids,applicant:role,status:'待审批',created:day(),history:[]};
  if(old){if(old.workflow)r.workflow=structuredClone(old.workflow);old.status='已重提';r.previousId=old.id;history(old,role,'修改重提',r.id);}
  history(r,role,'提交审批',reason);s.docRequests.unshift(r);return r;
}
// Commands run against a draft; the UI persists only after the entire command succeeds.
function rawCommand(s,action,v,role){
  migrate(s);let x;
  if(['docReview','purchaseReview','purchaseConfirm','saleReview','founderSetupReview','ventureReview','fundingReview','founderAdvance'].includes(action)&&!['通过','驳回'].includes(v.result))throw Error('请选择通过或驳回');
  switch(action){
  case 'requestSubmit':requireRole(role,'人事专员','直属上级');required(v.job,'岗位');required(v.reason,'用人理由');x={...v,id:uid(),count:integer(v.count,'招聘人数'),status:'待审批',created:day(),history:[]};s.requests.unshift(x);break;
  case 'requestReview':requireRole(role,'创业发展总监');x=find(s.requests,v.id);status(x,'待审批');evidence(v);if(!['通过','驳回'].includes(v.result))throw Error('请选择审批结果');x.status=v.result==='通过'?'招聘中':'已驳回';break;
  case 'requestResubmit':requireRole(role,'人事专员');x=find(s.requests,v.id);status(x,'已驳回');evidence(v);x.reason=v.note;x.status='待审批';break;
  case 'docSubmit':return transitionDoc(s,v,role);
  case 'docReview':{
    x=find(s.docRequests,v.id);requireRole(role,docReviewer(x));status(x,'待审批');evidence(v);
    if(v.result==='驳回'){x.status='已驳回';history(x,role,'驳回',v.note);return x;}
    if(v.result!=='通过')throw Error('请选择审批结果');
    let d=x.docId?find(s.documents,x.docId):null;
    if(d&&d.versions.length!==x.baseVersion)throw Error('有效版本已更新，请驳回后重新提交');
    if(x.recipients.some(id=>!s.employees.some(e=>e.id===id&&e.status!=='已离职')))throw Error('签收对象已离职，请驳回调整');
    if(!d){d={kind:x.kind,id:uid(),title:x.title,category:x.category,subject:x.subject,versions:[]};s.documents.unshift(d);x.docId=d.id;}
    const version=d.versions.length+1;d.title=x.title;d.category=x.category;
    d.versions.push({version,date:day(),content:x.content,files:x.files,requestId:x.id,approvedBy:role,approvedAt:new Date().toISOString(),approvalNote:v.note});
    for(const id of x.recipients){const p=makeProcess(s,find(s.employees,id),'制度签收',day(),{docId:d.id,title:d.title,version,source:'system'});s.receipts.push({id:uid(),employeeId:id,docId:d.id,version,processId:p.id,signedAt:null});}
    x.status='已通过';x.version=version;history(x,role,'批准并发布 V'+version,v.note);audit(s,null,'文件审批通过：'+x.title,role,v.note);return x;
  }
  case 'purchaseSubmit':{
    requireRole(role,'人事专员');required(v.name,'物资名称');required(v.reason,'采购理由');
    if(!['有价资产','易耗品'].includes(v.kind))throw Error('请选择物资类型');
    const quantity=integer(v.quantity,'数量'),value=amount(v.value,'单价'),years=amount(v.years,'折价年限',v.kind==='易耗品');
    if(v.id){x=find(s.purchases,v.id);status(x,'已驳回');Object.assign(x,{name:v.name,kind:v.kind,reason:v.reason,quantity,value,years,status:'财务审核'});}else{x={...v,id:uid(),quantity,value,years,status:'财务审核',date:day(),history:[]};s.purchases.unshift(x);}history(x,role,'采购申请',v.reason);return x;
  }
  case 'purchaseReview':requireRole(role,'财务负责人');x=find(s.purchases,v.id);status(x,'财务审核');evidence(v);x.status=v.result==='通过'?'采购入库':'已驳回';break;
  case 'purchaseReceive':requireRole(role,'人事专员');x=find(s.purchases,v.id);status(x,'采购入库');evidence(v);if(!v.files?.length)throw Error('请上传采购 / 到货凭证');x.files=v.files;x.receiveNote=v.note;x.status='上级确认';break;
  case 'purchaseConfirm':{
    requireRole(role,'创业发展总监');x=find(s.purchases,v.id);status(x,'上级确认');evidence(v);
    if(v.result==='驳回'){x.status='采购入库';break;}
    x.status='已入库';const count=x.kind==='有价资产'?x.quantity:1;
    for(let i=0;i<count;i++)s.materials.unshift({id:uid(),code:'XZ-'+uid().slice(0,8).toUpperCase(),name:x.name,kind:x.kind,value:x.value,years:x.years,date:day(),quantity:x.kind==='有价资产'?1:x.quantity,status:'在库',holder:'',purchaseId:x.id,history:[]});break;
  }
  case 'issue':{
    requireRole(role,'人事专员');x=find(s.materials,v.id);status(x,'在库');const person=find(recipients(s),v.holder);evidence(v);const quantity=integer(v.quantity,'派发数量');if(quantity>x.quantity)throw Error('库存不足');
    if(x.kind==='有价资产'){if(quantity!==1)throw Error('有价资产按独立编码逐件派发');x.status='领用中';x.holder=person.id;}else{x.quantity-=quantity;if(!x.quantity)x.status='已耗用';}
    s.movements.unshift({id:uid(),materialId:x.id,name:x.name,holder:person.id,person:person.name,type:'派发',quantity,date:day(),note:v.note});break;
  }
  case 'return':requireRole(role,'人事专员');x=find(s.materials,v.id);if(x.holder?.startsWith('founder:'))throw Error('请到创业者人员档案办理归还，保持领用记录同步');status(x,'领用中');evidence(v);s.movements.unshift({id:uid(),materialId:x.id,name:x.name,holder:x.holder,type:'归还',quantity:1,date:day(),note:v.note});x.holder='';x.status=v.damaged==='是'?'损坏待处理':'在库';break;
  case 'replace':{
    requireRole(role,'人事专员');x=find(s.materials,v.id);status(x,'领用中');if(x.holder?.startsWith('founder:'))throw Error('请到创业者人员档案先归还再申请新物资');const replacement=find(s.materials,v.replacement);status(replacement,'在库');if(replacement.kind!=='有价资产'||replacement.id===x.id)throw Error('请选择另一件在库有价资产');evidence(v);
    const holder=x.holder;replacement.holder=holder;replacement.status='领用中';x.holder='';x.status=v.damaged==='是'?'损坏待处理':'在库';history(replacement,role,'换发',v.note);s.movements.unshift({id:uid(),materialId:x.id,name:x.name,holder,type:'换发',replacement:replacement.id,quantity:1,date:day(),note:v.note});break;
  }
  case 'damage':requireRole(role,'人事专员');x=find(s.materials,v.id);status(x,'在库');evidence(v);x.status='损坏待处理';break;
  case 'saleSubmit':{
    requireRole(role,'人事专员');const m=find(s.materials,v.materialId);if(!['在库','损坏待处理'].includes(m.status))throw Error('仅可销售在库或已归还的损坏物资');required(v.reason,'销售原因');const quantity=integer(v.quantity,'销售数量');if(quantity>m.quantity)throw Error('数量超过库存');
    x={...v,id:uid(),name:m.name,quantity,remaining:amount(v.remaining,'剩余价值',true),status:'财务审核',date:day(),previousStatus:m.status,history:[]};m.status='销售处理中';history(x,role,'申请销售',v.reason);s.sales.unshift(x);return x;
  }
  case 'saleResubmit':{requireRole(role,'人事专员');x=find(s.sales,v.id);status(x,'已驳回');evidence(v);const m=find(s.materials,x.materialId);if(!['在库','损坏待处理'].includes(m.status)||m.quantity<x.quantity)throw Error('库存已变化，请核对后新建申请');x.reason=v.note;x.remaining=amount(v.remaining,'剩余价值',true);x.previousStatus=m.status;m.status='销售处理中';x.status='财务审核';break;}
  case 'saleReview':{
    requireRole(role,'财务负责人');x=find(s.sales,v.id);status(x,'财务审核');evidence(v);
    if(v.result==='驳回'){x.status='已驳回';find(s.materials,x.materialId).status=x.previousStatus;}else{if(!['需入账','无需入账'].includes(v.accounting))throw Error('请选择是否入账');x.accounting=v.accounting;x.status='待销售';}break;
  }
  case 'saleExecute':requireRole(role,'人事专员');x=find(s.sales,v.id);status(x,'待销售');evidence(v);if(!v.files?.length)throw Error('请上传销售凭证');x.price=amount(v.price,'实际销售金额',true);x.files=v.files;x.status='财务结案';break;
  case 'saleClose':{
    requireRole(role,'财务负责人');x=find(s.sales,v.id);status(x,'财务结案');evidence(v);if(x.accounting==='需入账')required(v.voucher,'入账凭证号');x.voucher=v.voucher||'';x.status='已完成';const m=find(s.materials,x.materialId);m.quantity-=x.quantity;m.status=m.quantity?x.previousStatus:'已售出';break;
  }
  case 'founderNeed':requireRole(role,'创业发展总监');required(v.title,'用人需求');required(v.reason,'需求理由');x={...v,id:uid(),status:'招聘中',history:[]};s.founderNeeds.unshift(x);break;
  case 'founderCandidate':requireRole(role,'人事专员');find(s.founderNeeds,v.needId);required(v.name,'候选人姓名');required(v.channel,'招聘渠道');if(!['投资合作','雇佣'].includes(v.relation))throw Error('请选择关系');x={...v,id:uid(),stage:0,status:'跟进中',history:[]};s.founderCandidates.unshift(x);break;
  case 'founderAdvance':{
    x=find(s.founderCandidates,v.id);status(x,'跟进中');requireRole(role,...(x.stage===5?['总经理']:x.stage<2||x.stage===3?['人事专员']:['创业发展总监']));evidence(v);
    if(v.result==='驳回'){if(x.stage===0)throw Error('候选人登记阶段请直接推进');x.stage--;history(x,role,'退回：'+recruiterStages[x.stage],v.note);return x;}
    if(x.stage===3){if(!v.files?.length)throw Error('请上传候选人提交的项目计划书');x.planFiles=v.files;}
    if(x.stage===5){x.status='已立项';const f={id:uid(),name:x.name,job:x.job||'事业主理人',project:x.project||find(s.founderNeeds,x.needId).title,relation:x.relation,candidateId:x.id,setupStatus:'待办理',status:'测试',history:[]};s.founders.unshift(f);x.founderId=f.id;}else x.stage++;break;
  }
  case 'employeeFounder':requireRole(role,'人事专员');{const e=find(s.employees,v.employeeId);if(e.status==='已离职')throw Error('已离职人员不可登记');if(s.founders.some(f=>f.employeeId===e.id))throw Error('已登记为事业主理人');x={id:uid(),name:e.name,relation:'雇佣',employeeId:e.id,setupStatus:'待办理',status:'筹备中',history:[]};s.founders.unshift(x);break;}
  case 'founderSetup':requireRole(role,'人事专员');x=find(s.founders,v.id);if(!['待办理','已驳回'].includes(x.setupStatus))throw Error('已提交或已确认');evidence(v);if(!v.files?.length)throw Error('请上传手续核验凭证');x.setupFiles=v.files;x.setupNote=v.note;x.setupStatus='待审核';break;
  case 'founderSetupReview':requireRole(role,'创业发展总监');x=find(s.founders,v.id);if(x.setupStatus!=='待审核')throw Error('当前不在审核中');evidence(v);x.setupStatus=v.result==='通过'?'已确认':'已驳回';break;
  case 'ventureSubmit':{
    requireRole(role,'创业发展总监');const f=find(s.founders,v.founderId);if(!ready(s,f))throw Error('请先完成两份协议审批和人事手续审核');
    required(v.title,'项目名称');if(!v.shop?.trim()&&!v.license?.trim())throw Error('店铺和营业执照至少填写一项；两项都有请同时填写');
    if(!Array.isArray(v.phases)||v.phases.length!==3)throw Error('请填写三个阶段');let last=day();
    for(const p of v.phases){required(p.goal,'阶段目标');p.budget=amount(p.budget,'阶段预算');if(!/^\d{4}-\d{2}-\d{2}$/.test(p.due)||p.due<last)throw Error('阶段截止日期须从今天起依次递增');last=p.due;}
    if(v.id){x=find(s.ventures,v.id);status(x,'已驳回');Object.assign(x,{title:v.title,founderId:v.founderId,shop:v.shop,license:v.license,phases:v.phases,status:'总经理审核'});}else{x={...v,id:uid(),status:'总经理审核',releasedPhase:-1,months:[],funding:[],history:[]};s.ventures.unshift(x);}break;
  }
  case 'ventureResubmit':requireRole(role,'创业发展总监');x=find(s.ventures,v.id);status(x,'已驳回');evidence(v);x.status='总经理审核';break;
  case 'ventureReview':requireRole(role,'总经理');x=find(s.ventures,v.id);status(x,'总经理审核');evidence(v);x.status=v.result==='通过'?'管理中':'已驳回';if(x.status==='管理中'){x.releasedPhase=0;}break;
  case 'founderProfile':requireRole(role,'人事专员');x=find(s.founders,v.id);x.job=required(v.job,'职务');x.project=required(v.project,'项目');break;
  case 'founderTestPlan':requireRole(role,'创业发展总监');x=find(s.founders,v.id);status(x,'测试');if(x.formalReview?.status==='待审核')throw Error('正式评估待审核，不能修改测试条件');x.testGoal=required(v.goal,'阶段性目标');if(!/^\d{4}-\d{2}-\d{2}$/.test(v.due)||v.due<day())throw Error('请选择有效截止日期');x.testDue=v.due;x.testBudget=amount(v.budget,'测试资金');break;
  case 'founderFormalSubmit':requireRole(role,'创业发展总监');x=find(s.founders,v.id);status(x,'测试');if(!x.testGoal||!x.testDue)throw Error('请先设置测试目标及期限');if(day()>x.testDue)throw Error('测试期限已过，不能按期达标转正式');if(x.formalReview?.status==='待审核')throw Error('已提交审核');if(Number(v.rate)!==100||!v.files?.length)throw Error('须达成100%并上传证据');evidence(v);(x.formalHistory??=[]).push(...(x.formalReview?[structuredClone(x.formalReview)]:[]));x.formalReview={status:'待审核',note:v.note,files:v.files,budget:amount(v.budget,'正式阶段资金'),submitted:day()};break;
  case 'founderFormalReview':requireRole(role,'总经理');x=find(s.founders,v.id);status(x,'测试');if(x.formalReview?.status!=='待审核')throw Error('没有待审核评估');if(!['通过','驳回'].includes(v.result))throw Error('请选择审批结果');evidence(v);x.formalReview.status=v.result==='通过'?'已通过':'已驳回';x.formalReview.review=v.note;if(v.result==='通过')x.status='正式';break;
  case 'founderAssetApply':requireRole(role,'人事专员');find(s.founders,v.founderId);x={id:uid(),founderId:v.founderId,name:required(v.name,'办公资产名称'),code:required(v.code,'资产编码'),value:amount(v.value,'资产价值',true),note:evidence(v),status:'待审核',history:[]};if(s.founderAssets.some(a=>a.code===x.code&&!['已驳回','已归还'].includes(a.status)))throw Error('此编码已有未结束的派发记录');s.founderAssets.unshift(x);break;
  case 'founderAssetReview':requireRole(role,'创业发展总监');x=find(s.founderAssets,v.id);status(x,'待审核');if(!['通过','驳回'].includes(v.result))throw Error('请选择审批结果');evidence(v);x.status=v.result==='通过'?'待派发':'已驳回';break;
  case 'founderAssetDeliver':requireRole(role,'人事专员');x=find(s.founderAssets,v.id);status(x,'待派发');evidence(v);if(!v.files?.length)throw Error('请上传领取凭证');x.files=v.files;x.status='使用中';break;
  case 'founderAssetReturn':requireRole(role,'人事专员');x=find(s.founderAssets,v.id);status(x,'使用中');evidence(v);if(!v.files?.length)throw Error('请上传归还凭证');x.returnFiles=v.files;x.status='已归还';break;
  case 'ventureMonth':{
    requireRole(role,'创业发展总监');x=find(s.ventures,v.id);status(x,'管理中');if(!/^\d{4}-\d{2}$/.test(v.month)||v.month>day().slice(0,7))throw Error('请选择不晚于当前月份的月份');const phase=Number(v.phase);if(!Number.isInteger(phase)||phase<0||phase>x.releasedPhase)throw Error('该阶段尚未批准');const rate=amount(v.rate,'达成率',true);if(rate>100)throw Error('达成率不可超过100%');required(v.note,'项目评估');if(!v.files?.length)throw Error('请上传目标达成证据');
    const prior=x.months.find(m=>m.month===v.month&&m.phase===phase);const entry={...v,phase,rate,used:amount(v.used,'本月预算使用金额',true),date:day()};if(prior){(x.monthHistory??=[]).push(structuredClone(prior));Object.assign(prior,entry);}else x.months.push(entry);break;
  }
  case 'fundingSubmit':{
    requireRole(role,'创业发展总监');x=find(s.ventures,v.id);status(x,'管理中');evidence(v);if(x.releasedPhase>=2)throw Error('全部阶段均已批准');if(x.funding.some(f=>f.status==='待审核'))throw Error('已有待审核申请');
    const p=x.releasedPhase;if(!x.months.some(m=>m.phase===p&&m.rate===100&&m.date<=x.phases[p].due))throw Error('须在当前阶段期限内达成100%，并上传月度评估证据后才能申请下一阶段预算');x.funding.push({id:uid(),phase:p+1,status:'待审核',note:v.note,date:day()});break;
  }
  case 'fundingReview':requireRole(role,'总经理');x=find(s.ventures,v.id);{const f=find(x.funding,v.fundingId);status(f,'待审核');evidence(v);if(v.result==='通过'&&(f.phase!==x.releasedPhase+1||!x.months.some(m=>m.phase===x.releasedPhase&&m.rate===100&&m.date<=x.phases[x.releasedPhase].due)))throw Error('阶段达标依据已变化，请驳回并重新核验');f.status=v.result==='通过'?'已通过':'已驳回';f.review=v.note;if(f.status==='已通过')x.releasedPhase=f.phase;break;}
  default:throw Error('未知操作');
  }
  history(x,role,action,v.note||v.reason||'');audit(s,null,action,role,v.note||v.reason||'');return x;
}

export function command(s,action,v,role){
 if(v.result!==undefined&&!['通过','驳回'].includes(v.result))throw Error('请选择有效审批结果');
 if(action==='workflowLifecycle')return workflowLifecycle(s,v,role);
 const groups={doc:'docRequests',purchase:'purchases',sale:'sales',request:'requests',founderAdvance:'founderCandidates'};
 const group=groups[action]||groups[Object.keys(groups).find(k=>action.startsWith(k))];
 const old=group?s[group]?.find(x=>x.id===v.id):null;
 if(old?.governance?.ended||old?.governance?.withdrawn)throw Error('事项已终止或撤回，不能继续流转');
 if(old){ensureWorkflow(old,old.status||String(old.stage));requireRework(v,old.workflow.totalDue);}
 const x=rawCommand(s,action,v,role);
 if(x){const w=ensureWorkflow(x,x.status||String(x.stage),{totalDue:v.totalDue});applyRework(x,v);recordAction(x,action,{...v.actorIdentity,role,note:v.note||v.reason,result:v.result});
 if(['已通过','已入库','已完成','已立项'].includes(x.status)){w.completedAt=new Date().toISOString();}
 }
 return x;
}

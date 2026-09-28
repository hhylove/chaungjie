import {businessDay,ensureWorkflow,recordAction} from './workflow-policy.js';
export const roles=['人事专员','直属上级','创业发展总监','财务风控','员工本人','管理层','财务负责人','总经理'];
export const centers=['创业者发展中心','产品供应链中心','AI科技中心','仓储履约中心','财务风控中心','项目运营中心','视觉内容中心'];
export const uid=()=>globalThis.crypto.randomUUID();
export const day=businessDay;
export const add=(d,n)=>{const x=new Date(d+'T12:00:00');x.setDate(x.getDate()+n);return day(x)};
export const diff=(a,b)=>Math.round((new Date(a+'T12:00:00')-new Date(b+'T12:00:00'))/86400000);
export function audit(s,e,text,role='系统',evidence=''){s.events.unshift({id:uid(),employeeId:e,time:new Date().toISOString(),text,role,evidence})}
const onboarding=[
['入职前','身份与银行卡资料核验','人事专员',-3,'info'],['入职前','岗位与薪酬审批','创业发展总监',-3,'pay'],['入职前','劳动合同准备','人事专员',-2,'draft'],['入职前','工位与电脑准备','人事专员',-1,'desk'],['入职前','OA / ERP / 业务权限申请','直属上级',-1,'accessApply'],
['入职当天','本人身份核验与到岗确认','人事专员',0,'arrival'],['入职当天','劳动合同签署','员工本人',0,'contract'],['入职当天','保密及知识产权文件签署','员工本人',0,'nda'],['入职当天','员工手册签收','员工本人',0,'handbook'],['入职当天','公司制度签收','员工本人',0,'policy'],['入职当天','岗位职责签收','员工本人',0,'job'],['入职当天','信息安全制度签收','员工本人',0,'security'],['入职当天','办公资产领取','员工本人',0,'asset'],['入职当天','系统权限开通','人事专员',0,'access'],
['入职后','社保依法办理确认','人事专员',7,'social'],['入职后','新员工培训完成','员工本人',3,'training'],['入职后','岗位培训完成','员工本人',5,'jobTraining'],['入职后','直属上级确认','直属上级',7,'manager']];
export function makeProcess(s,e,type,now=day(),payload={}){
 if(s.processes.some(p=>p.employeeId===e.id&&p.type===type&&!p.closed&&type!=='制度签收'))throw Error('该员工已有同类运行中流程');
 if(!['入职','转正','离职','调岗','调薪','晋升','制度签收','合同续签','证照更新'].includes(type))throw Error('不支持的流程类型');
 if(e.status==='已离职')throw Error('已离职员工不能再发起流程');
 if(type==='转正'&&(e.status!=='试用期'||s.processes.some(p=>p.employeeId===e.id&&p.type==='入职'&&!p.closed)))throw Error('仅手续已完成的试用期员工可进入转正');
 if(type==='调薪'&&!(Number(payload.salary)>0))throw Error('目标薪酬须大于零');
 if(['调岗','晋升'].includes(type)&&!payload.job?.trim())throw Error('请填写目标岗位');
 let specs=onboarding,base=e.start;
 if(type==='转正'){base=e.probation;specs=[['试用评价','试用期评价','直属上级',-30,'review'],['转正审批','转正与薪酬审批','创业发展总监',-7,'approval'],['转正确认','定岗定薪及员工确认','员工本人',0,'confirm'],['转正执行','人事生效确认','人事专员',0,'execute']];}
 else if(type==='离职'){base=now;specs=[['发起','离职申请与日期确认','员工本人',0,'request'],['审批','离职审批','创业发展总监',1,'approval'],['交接','工作交接确认','直属上级',3,'handover'],['结清','资产归还核验','人事专员',5,'assetReturn'],['结清','OA / ERP / 业务权限回收','人事专员',5,'revoke'],['结清','工资与应付款结算','财务风控',5,'settle'],['结算确认','员工结算确认','员工本人',6,'confirm'],['归档','离职档案归档','人事专员',7,'archive']];}
 else if(['调岗','调薪','晋升'].includes(type)){base=now;specs=[['申请',`${type}申请确认`,'人事专员',0,'request'],['意见','直属上级意见','直属上级',1,'opinion'],['审批','总监审批','创业发展总监',2,'approval'],['确认','员工确认','员工本人',3,'confirm'],['执行','人事执行变更','人事专员',3,'execute']];}
 else if(type==='制度签收'){base=now;specs=[['签收',`${payload.title} · V${payload.version} 签收`,'员工本人',7,'receipt']];}
 else if(type==='合同续签'||type==='证照更新'){base=now;specs=[['复核','更新资料复核','人事专员',0,'check'],['确认','本人签署 / 确认','员工本人',2,'confirm'],['归档','有效期更新归档','人事专员',3,'execute']];}
 const p={id:uid(),source:payload.source||'manual',employeeId:e.id,type,created:now,payload,tasks:specs.map(([stage,title,owner,offset,key])=>({id:uid(),stage,title,owner,due:add(base,offset),key,required:true,done:false})),closed:null};
 s.processes.unshift(p);audit(s,e.id,`自动建立${type}流程`);return p;
}
export function canTask(p,t){const index=p.tasks.indexOf(t);return !t.done&&p.tasks.slice(0,index).filter(x=>x.stage!==t.stage).every(x=>x.done)}
export function finishTask(s,pid,tid,role,evidence,now=day(),extra={}){
 const p=s.processes.find(x=>x.id===pid),t=p?.tasks.find(x=>x.id===tid);if(p?.governance?.ended||p?.governance?.withdrawn)throw Error('事项已终止或撤回');if(!t||p.closed)throw Error('流程不存在或已经完成');if(t.rework?.status==='待整改')throw Error('请先提交整改资料再办理');
 if(!canTask(p,t))throw Error('前置必要节点尚未完成，不能跳过');if(t.owner!==role)throw Error(`本节点须由${t.owner}处理`);if(!evidence?.trim())throw Error('请填写办理依据或凭证说明');
 const e=s.employees.find(x=>x.id===p.employeeId);if(e.status==='已离职')throw Error('该员工已离职，不能继续办理');
 if(p.type==='入职'&&t.key==='arrival'&&now<e.start)throw Error('尚未到约定入职日期，不能确认到岗');
 if(p.type==='入职'&&t.key==='access'&&(e.status!=='试用期'&&e.status!=='在职'||!p.tasks.some(x=>x.key==='arrival'&&x.done)))throw Error('员工尚未确认到岗，不能开通系统权限');
 if(p.type==='转正'&&t.key==='execute'&&now<e.probation)throw Error('尚未到转正生效日');
 if(p.type==='离职'&&t.key==='assetReturn'&&(s.materials||[]).some(m=>m.holder===e.id&&m.status==='领用中'))throw Error('仍有领用资产未归还，请在行政物资仓库办理归还');
 if(t.key==='social'&&extra.social!=='已参保')throw Error('必须确认已依法参保并提供办理证明，不能以放弃协议关闭');
 ensureWorkflow(p,t.id,{now,totalDue:p.tasks.at(-1)?.due});recordAction(p,t.title,{...extra.actorIdentity,role,note:evidence,result:'通过'});t.done=true;t.completedAt=new Date().toISOString();t.actor=role;t.evidence=evidence.trim();audit(s,e.id,`${p.type} · ${t.title}`,role,t.evidence);
 if(p.type==='入职'){
 if(t.key==='arrival')e.status='试用期';if(t.key==='contract')e.contract='已签署';if(t.key==='social'){e.social='已参保';e.socialReason='';}if(t.key==='asset')e.asset='已领取';if(t.key==='access')e.access='已开通';
 }
 if(p.type==='离职'&&t.key==='assetReturn')e.asset='已归还';if(p.type==='离职'&&t.key==='revoke')e.access='已回收';
 if(p.tasks.every(x=>x.done)){
 p.closed=new Date().toISOString();p.workflow.completedAt=p.closed;audit(s,e.id,`${p.type}流程完成`);
 if(p.type==='入职'){makeProcess(s,e,'转正',now,{source:'system'});}
 if(p.type==='转正')e.status='在职';if(p.type==='离职'){e.status='已离职';for(const other of s.processes.filter(x=>x.employeeId===e.id&&!x.closed)){other.closed=p.closed;other.cancelled=true;audit(s,e.id,`${other.type}流程终止：员工已离职`);}}
 if(p.type==='调薪')e.salary=Number(p.payload.salary);if(['调岗','晋升'].includes(p.type)){e.job=p.payload.job;e.center=p.payload.center||e.center;}
 if(p.type==='合同续签'){e.contractEnd=p.payload.date;e.contract='已签署';}if(p.type==='证照更新')e.certificateEnd=p.payload.date;
 if(p.type==='制度签收'){const r=s.receipts.find(x=>x.processId===p.id);if(r){r.signedAt=t.completedAt;r.actor=role;r.evidence=t.evidence;}}
 }
 return p;
}
export function alerts(s,now=day()){
 const out=[];
 for(const p of s.processes.filter(x=>!x.closed))for(const t of p.tasks.filter(x=>!x.done)){
 const n=diff(t.due,now);if(n<=7)out.push({key:t.id,employeeId:p.employeeId,pid:p.id,title:t.title,due:t.due,owner:t.owner,level:n<0?'red':n===0?'today':'soon',major:false,waiting:!canTask(p,t)});
 }
 for(const e of s.employees.filter(x=>x.status!=='已离职')){
 if(e.social==='未参保')out.push({key:`social-${e.id}`,employeeId:e.id,title:'社保异常待整改',due:now,owner:'人事专员',level:'red',major:true});
 for(const [field,title] of [['contractEnd','劳动合同到期'],['certificateEnd','证照到期']])if(e[field]){const n=diff(e[field],now);if(n<=30)out.push({key:`${field}-${e.id}-${e[field]}`,employeeId:e.id,title,due:e[field],owner:'人事专员',level:n<0?'red':'soon',major:field==='contractEnd'&&n<0});}
 }
 return out.sort((a,b)=>({red:0,today:1,soon:2}[a.level]-{red:0,today:1,soon:2}[b.level])||a.due.localeCompare(b.due));
}
export function tick(s,now=day()){
 let changed=false;const current=alerts(s,now).filter(a=>a.level==='red');
 for(const a of current)if(!s.escalations.some(x=>x.key===a.key&&!x.resolved)){
 const target=a.major?'管理层 / 财务风控':'创业发展总监';s.escalations.push({...a,id:uid(),target,time:new Date().toISOString(),resolved:null});audit(s,a.employeeId,`异常升级至${target}：${a.title}`);changed=true;
 }
 for(const x of s.escalations.filter(x=>!x.resolved))if(!current.some(a=>a.key===x.key)){x.resolved=new Date().toISOString();audit(s,x.employeeId,`异常已解决：${x.title}`);changed=true;}
 return changed;
}
export function newEmployee(s,input,role,now=day()){
 if(role!=='人事专员')throw Error('请由人事专员录入人员');
 if(!input.name?.trim()||!input.job?.trim()||!input.center?.trim()||!input.start||!input.probation||input.probation<input.start||!input.contractEnd||input.contractEnd<=input.start||!(Number(input.salary)>0))throw Error('请完整填写姓名、岗位、中心、有效日期和薪酬；试用结束日不得早于入职日');
 const e={...input,id:uid(),salary:Number(input.salary),status:'待入职',contract:'待签署',social:'待办理',asset:'待领取',access:'待开通',manager:input.manager||'直属上级',socialReason:''};s.employees.push(e);audit(s,e.id,'建立员工档案',role);makeProcess(s,e,'入职',now);return e;
}
export function publish(){throw Error('制度发布须提交管理层审批，不能直接发布');}
export const stages=['候选人','面试','评价','录用审批','待入职','已转员工'];
export function advanceCandidate(s,id,role,note){const c=s.candidates.find(x=>x.id===id);if(!c||c.employeeId)throw Error('候选人已转换或不存在');const owners=['人事专员','直属上级','直属上级','创业发展总监'];const i=stages.indexOf(c.stage);if(i<0||i>=4)throw Error('请通过转员工建立档案');if(owners[i]!==role)throw Error(`当前步骤须由${owners[i]}处理`);if(!note.trim())throw Error('请填写评价 / 决策依据');c.history.push({stage:c.stage,role,note,time:new Date().toISOString()});c.stage=stages[i+1];audit(s,null,`候选人${c.name}：${c.stage}`,role,note);}
export function convert(s,id,input,role,now=day()){const c=s.candidates.find(x=>x.id===id);if(!c||c.employeeId||c.stage!=='待入职')throw Error('必须完成录用审批，且不可重复转换');const r=s.requests.find(x=>x.id===c.requestId);if(r.status!=='招聘中')throw Error('用人需求尚未通过审批');if(s.candidates.filter(x=>x.requestId===r.id&&x.employeeId).length>=r.count)throw Error('该需求录用名额已满');const e=newEmployee(s,{...input,name:c.name,center:r.center,job:r.job},role,now);c.employeeId=e.id;c.stage='已转员工';audit(s,e.id,'从招聘候选人转入员工档案',role);return e;}
export function seed(now=day()){
 const s={schemaVersion:1,employees:[],processes:[],events:[],escalations:[],requests:[],candidates:[],documents:[],receipts:[]};
 const names=['陈思远','林知夏','周予安','许清禾','沈亦舟','苏晚晴','陆景行','宋予宁','顾言','叶嘉禾','徐子墨','唐可欣','王书宁','李明远','赵嘉琪','吴承泽','郑一诺','孙若琳','何以恒','高雨桐','罗嘉诚','梁芷晴','谢文博','冯思涵','邓子昂','韩佳怡','曹景然','彭语桐','曾浩宇','肖安琪','袁致远','蔡心悦','潘书航','余欣然','杜沐晨'];
 names.forEach((name,i)=>{const status=i<3?'试用期':i>=32?'待入职':'在职';const start=status==='待入职'?add(now,4+i-32):status==='试用期'?add(now,-83+i):add(now,-180-i*3);const e={id:`emp-${i+1}`,name,center:centers[i%5],project:i%4===0?'银器事业项目':'平台公共服务',job:['人事专员','产品运营','AI应用工程师','履约专员','财务专员'][i%5],manager:['王书宁','顾言','徐子墨','陆景行','李明远'][i%5],start,probation:add(start,90),salary:8000+i*200,status,contract:status==='待入职'?'待签署':'已签署',contractEnd:i===3?add(now,12):add(start,730),certificateEnd:i===7?add(now,5):'',social:i===0?'未参保':status==='待入职'?'待办理':'已参保',socialReason:i===0?'演示：员工参保资料待补，需人事跟进整改':'',asset:status==='待入职'?'待领取':'已领取',access:status==='待入职'?'待开通':'已开通'};s.employees.push(e);audit(s,e.id,'演示档案初始化（虚构数据）');
 if(status==='待入职')makeProcess(s,e,'入职',now);if(status==='试用期')makeProcess(s,e,'转正',now,{source:'system'});
 });
 for(const [i,title] of ['劳动合同模板','保密与知识产权协议','员工手册','岗位说明书','新员工培训材料','信息安全制度','公司管理制度'].entries())s.documents.push({id:`doc-${i}`,title,category:i<2?'合同协议':i===4?'培训材料':'制度规范',versions:[{version:1,date:add(now,-30),content:`${title} · 演示占位内容\n此版本用于演示档案和签收流程，不是可直接签署的正式文件。\n正式发布前需补充适用范围、职责、执行细则、生效日期及审核记录。`}]});
 s.requests.push({id:'req-1',job:'电商运营',center:centers[1],count:1,reason:'银器事业项目补充运营岗位',status:'招聘中'},{id:'req-2',job:'AI应用工程师',center:centers[2],count:1,reason:'支持平台流程自动化',status:'待审批'});
 s.candidates.push({id:'can-1',name:'江予白',requestId:'req-1',stage:'面试',history:[]},{id:'can-2',name:'程星野',requestId:'req-1',stage:'候选人',history:[]});tick(s,now);return s;
}

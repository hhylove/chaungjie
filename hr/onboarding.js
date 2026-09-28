import {ensureWorkflow,recordAction,requireRework,applyRework} from './workflow-policy.js';
// Entrepreneur onboarding keeps people separate and draws materials from the administrative warehouse.
export const agreementTypes=['经营合作协议','创界平台服务协议'];
const req=(v,label)=>{if(!String(v??'').trim())throw Error('请填写'+label);return String(v).trim();};
const allowed=(role,want)=>{if(role!==want)throw Error('此步骤须由'+want+'处理');};
const log=(x,role,action,note)=>{(x.history??=[]).push({time:new Date().toISOString(),role,action,note});};
export function onboardingStatus(s,f){
 const assets=(s.founderAssets||[]).filter(a=>a.founderId===f.id&&a.materialId);
 const contracts=agreementTypes.every(c=>(s.documents||[]).some(d=>d.subject===f.id&&d.category===c&&d.versions?.length));
 return [{key:'account',label:'OA账号与必要权限',done:f.account?.status==='已开通',status:f.account?.status||'待申请'}, {key:'profile',label:'创业者档案',done:f.profile?.status==='已确认',status:f.profile?.status||'待维护'}, {key:'contracts',label:'两份已签署协议',done:contracts,status:contracts?'已生效':'待签署 / 审批'}, {key:'assets',label:'行政物资派发',done:assets.some(a=>a.deliveredAt)&&!assets.some(a=>['待审核','待派发'].includes(a.status)),status:assets.some(a=>['待审核','待派发'].includes(a.status))?'办理中':assets.some(a=>a.deliveredAt)?'已派发':'待申请'}];
}
function rawOnboardingCommand(s,action,v,role,admin){
 let f=s.founders.find(f=>f.id===v.id),x;
 if(action.startsWith('onboardStock')&&action!=='onboardStockApply'){x=s.founderAssets.find(a=>a.id===v.id);if(!x)throw Error('物资申请不存在');f=s.founders.find(f=>f.id===x.founderId);}
 if(!f)throw Error('创业者不存在');const note=req(v.note,'处理意见 / 依据');
 const review=()=>{allowed(role,'创业发展总监');if(!['通过','驳回'].includes(v.result))throw Error('请选择审批结果');};
 switch(action){
 case 'onboardAccountSubmit':allowed(role,'人事专员');if(['待审核','待开通'].includes(f.account?.status))throw Error('当前账号申请正在办理');{const account=req(v.account,'OA账号'),permissions=req(v.permissions,'必要权限'),scope=req(v.scope,'数据范围');if(s.founders.some(p=>p.id!==f.id&&p.account?.account===account))throw Error('此账号已分配给其他创业者');(f.accountHistory??=[]).push(...(f.account?[structuredClone(f.account)]:[]));f.account={account,permissions,scope,note,status:'待审核'};}break;
 case 'onboardAccountReview':review();if(f.account?.status!=='待审核')throw Error('没有待审核账号');f.account.status=v.result==='通过'?'待开通':'已驳回';f.account.review=note;break;
 case 'onboardAccountActivate':allowed(role,'人事专员');if(f.account?.status!=='待开通')throw Error('请先审核账号和权限');f.account.status='已开通';f.account.activatedAt=new Date().toISOString();f.account.activationNote=note;break;
 case 'onboardProfileSubmit':allowed(role,'人事专员');if(f.profile?.status==='待审核')throw Error('档案正在审核');if(!/^\d{4}-\d{2}-\d{2}$/.test(v.start)||Number.isNaN(Date.parse(v.start+'T12:00:00Z'))||new Date(v.start+'T12:00:00Z').toISOString().slice(0,10)!==v.start)throw Error('请选择有效合作开始日期');(f.profileHistory??=[]).push(...(f.profile?[structuredClone(f.profile)]:[]));f.profile={job:req(v.job,'职务'),project:req(v.project,'项目'),contact:req(v.contact,'联系方式'),start:req(v.start,'合作开始日期'),background:req(v.background,'履历与档案说明'),files:v.files||[],note,status:'待审核'};break;
 case 'onboardProfileReview':review();if(f.profile?.status!=='待审核')throw Error('没有待审核档案');f.profile.status=v.result==='通过'?'已确认':'已驳回';f.profile.review=note;if(v.result==='通过'){f.job=f.profile.job;f.project=f.profile.project;}break;
 case 'onboardStockApply':allowed(role,'人事专员');{const m=admin?.materials?.find(m=>m.id===v.materialId),quantity=Number(v.quantity);if(!m||m.status!=='在库')throw Error('请选择行政仓库中在库物资');if(!Number.isInteger(quantity)||quantity<1||quantity>m.quantity||(m.kind==='有价资产'&&quantity!==1))throw Error('数量不正确或库存不足');x={id:crypto.randomUUID(),founderId:f.id,materialId:m.id,name:m.name,code:m.code,value:m.value,kind:m.kind,quantity,note,status:'待审核',history:[]};s.founderAssets.unshift(x);}break;
 case 'onboardStockReview':review();if(x.status!=='待审核')throw Error('申请已处理');if(v.result==='驳回'){x.status='已驳回';break;}{const m=admin?.materials?.find(m=>m.id===x.materialId);if(!m||m.status!=='在库'||m.quantity<x.quantity)throw Error('库存已变化，无法批准，请驳回后重新申请');m.status='创业者派发预留';m.reservation=x.id;log(m,role,'预留创业者物资',f.name+' · '+note);x.status='待派发';}break;
 case 'onboardStockDeliver':allowed(role,'人事专员');if(x.status!=='待派发')throw Error('请先审核派发申请');if(!v.files?.length)throw Error('请上传领取凭证');{const m=admin?.materials?.find(m=>m.id===x.materialId);if(!m||m.reservation!==x.id||m.status!=='创业者派发预留')throw Error('库存预留信息不一致');delete m.reservation;if(m.kind==='有价资产'){m.status='领用中';m.holder='founder:'+f.id;m.holderName=f.name;}else{m.quantity-=x.quantity;m.status=m.quantity?'在库':'已耗用';}log(m,role,'创业者物资出库',f.name+' · '+note);(admin.movements??=[]).unshift({id:crypto.randomUUID(),materialId:m.id,name:m.name,holder:'founder:'+f.id,person:f.name,type:'创业者派发',quantity:x.quantity,date:new Date().toISOString().slice(0,10),note});x.files=v.files;x.deliveredAt=new Date().toISOString();x.status=m.kind==='有价资产'?'使用中':'已耗用';}break;
 case 'onboardStockReturn':allowed(role,'人事专员');if(x.status!=='使用中')throw Error('仅使用中的有价资产可以归还');if(!v.files?.length)throw Error('请上传归还凭证');{const m=admin?.materials?.find(m=>m.id===x.materialId);if(!m||m.holder!=='founder:'+f.id||m.status!=='领用中')throw Error('资产领用记录已变化');m.status='在库';m.holder='';m.holderName='';log(m,role,'创业者归还资产',note);admin.movements.unshift({id:crypto.randomUUID(),materialId:m.id,name:m.name,holder:'founder:'+f.id,person:f.name,type:'创业者归还',quantity:1,date:new Date().toISOString().slice(0,10),note});x.returnFiles=v.files;x.status='已归还';}break;
 default:throw Error('未知办理步骤');
 }
 log(x||f,role,action,note);return x||f;
}

export function onboardingCommand(s,action,v,role,admin){
 const person=s.founders.find(p=>p.id===v.id);
 const old=action.startsWith('onboardStock')?s.founderAssets.find(a=>a.id===v.id):person?.[action.startsWith('onboardAccount')?'account':'profile'];
 if(old?.governance?.ended||old?.governance?.withdrawn)throw Error('事项已终止或撤回');
 if(old){ensureWorkflow(old,old.status);requireRework(v,old.workflow.totalDue);}
 const result=rawOnboardingCommand(s,action,v,role,admin);
 const x=action.startsWith('onboardStock')?result:result[action.startsWith('onboardAccount')?'account':'profile'];
 if(x){if(old?.workflow&&!x.workflow)x.workflow=structuredClone(old.workflow);ensureWorkflow(x,x.status);applyRework(x,v);recordAction(x,action,{...v.actorIdentity,role,note:v.note,result:v.result});if(['已开通','已确认','使用中','已耗用','已归还'].includes(x.status))x.workflow.completedAt=new Date().toISOString();}
 return result;
}

import {initOperations,operationsPage} from './operations-ui.js';
import {onboardingCommand} from './onboarding.js';
import {command,migrate} from './operations.js';
const KEY='chuangjie-founders-v1',oldKey='chuangjie-hr-v1';
const menus=['创业者人员','招募','协议与合同'];
const platformParams=new URLSearchParams(location.search),platformActor=platformParams.get('actor'),platformPerson=platformParams.get('person');
let page=menus[0],role=['人事专员','创业发展总监'].includes(platformActor)?platformActor:'人事专员',state,storageError='';
function initialize(){
 const empty={schemaVersion:1,employees:[],processes:[],events:[],escalations:[],requests:[],candidates:[],documents:[],receipts:[]};
 const old=JSON.parse(localStorage.getItem(oldKey)||'null');
 if(old){for(const k of ['founderNeeds','founderCandidates','founders','ventures'])empty[k]=structuredClone(old[k]||[]);empty.documents=structuredClone((old.documents||[]).filter(d=>d.subject));empty.docRequests=structuredClone((old.docRequests||[]).filter(d=>d.subject));empty.founderAssets=(old.materials||[]).filter(m=>m.holder&&(old.founders||[]).some(f=>m.holder===(f.employeeId||f.id))).map(m=>({id:crypto.randomUUID(),founderId:old.founders.find(f=>m.holder===(f.employeeId||f.id)).id,name:m.name,code:m.code,value:m.value,status:'使用中',note:'历史创业者领用记录迁入，保留原资产编码',history:structuredClone(m.history||[])}));}
 return migrate(empty);
}
try{const raw=localStorage.getItem(KEY);state=raw?migrate(JSON.parse(raw)):initialize();if(!Array.isArray(state.founders)||!Array.isArray(state.documents))throw Error();if(!raw)localStorage.setItem(KEY,JSON.stringify(state));}catch{state=migrate({employees:[],processes:[],events:[],escalations:[],requests:[],candidates:[],documents:[],receipts:[]});storageError='存储异常，原数据未覆盖，暂不可提交。';}
let savedRaw=localStorage.getItem(KEY);
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const $=q=>document.querySelector(q);
function toast(text){$('#toast').textContent=text;$('#toast').classList.add('show');setTimeout(()=>$('#toast').classList.remove('show'),3500);}
function close(){$('#overlay').innerHTML='';document.body.classList.remove('modal-open');}
function panel(title,body){$('#overlay').innerHTML=`<div class="backdrop"><section class="drawer wide" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="drawer-top"><h2>${esc(title)}</h2><button data-founder-close aria-label="关闭">×</button></div><div class="drawer-body">${body}</div></section></div>`;document.body.classList.add('modal-open');}
function render(){$('#app').innerHTML=`<aside class="sidebar"><nav aria-label="创业者管理功能">${menus.map(m=>`<button data-founder-page="${m}" class="${page===m?'active':''}">${m}</button>`).join('')}</nav></aside><div class="shell"><header><div class="breadcrumb">创业者发展中心 / 创业者管理 / ${page}</div><div class="inherited-identity"><small>当前身份</small><strong>${esc(platformPerson||role)} · ${esc(role)}</strong></div></header><main><div class="demo-banner">操作权限继承平台顶部身份；管理层、总经理等审批请到公共协作处理。${esc(storageError)}</div>${operationsPage(page)}</main></div>`;}
document.addEventListener('click',e=>{const b=e.target.closest('[data-founder-page]');if(b){page=b.dataset.founderPage;close();render();window.scrollTo(0,0);}if(e.target.closest('[data-founder-close]'))close();});
document.addEventListener('change',e=>{if(e.target.id==='founder-role'){role=e.target.value;close();render();}});
document.addEventListener('keydown',e=>{if(e.key==='Escape')close();});
initOperations({inventory:()=>JSON.parse(localStorage.getItem(oldKey)||'null'),runCommand:(action,v,actor)=>{if(storageError)throw Error(storageError);if(localStorage.getItem(KEY)!==savedRaw)throw Error('数据已变更，请刷新后重试');const beforeAdmin=localStorage.getItem(oldKey),admin=beforeAdmin?JSON.parse(beforeAdmin):null,draft=structuredClone(state);if(action.startsWith('onboard'))onboardingCommand(draft,action,v,actor,admin);else command(draft,action,v,actor);const next=JSON.stringify(draft);let wroteAdmin=false;try{if(action.startsWith('onboardStock')&&admin){localStorage.setItem(oldKey,JSON.stringify(admin));wroteAdmin=true;}localStorage.setItem(KEY,next);}catch(e){if(wroteAdmin)localStorage.setItem(oldKey,beforeAdmin);throw e;}state=draft;savedRaw=next;},state:()=>state,role:()=>role,panel,close,toast,render,navigate:p=>{const target=p==='协议档案'?'协议与合同':p;if(!menus.includes(target))return;page=target;close();render();},commit:draft=>{if(storageError)throw Error(storageError);localStorage.setItem(KEY,JSON.stringify(draft));state=draft;}});
window.addEventListener('storage',e=>{if(e.key===KEY&&e.newValue){try{state=migrate(JSON.parse(e.newValue));savedRaw=e.newValue;close();render();toast('创业者数据已同步更新');}catch{toast('数据更新失败，请刷新');}}});
render();

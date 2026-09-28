import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {webcrypto} from 'node:crypto';
import * as policy from '../hr/workflow-policy.js';
const source=fs.readFileSync(new URL('../workflow-governance.js',import.meta.url),'utf8');
function fixture(){
 const db=new Map(),ctx={Date,Intl,JSON,crypto:webcrypto,structuredClone,console,role:2,level:'负责人',employeeId:'emp-2-1',state:{revision:0,items:[],docs:[]},key:'main',crossHrKey:'hr',crossFounderKey:'founder',stages:{task:['待接收','执行中','待负责人验收','已驳回 · 待调整','已完成'],goal:['制定方案','老板审核','执行中','老板验收','调整中','已完成']},search:'',activeRecordId:'',shops:[],crossDay:policy.businessDay,crossAdd:policy.addDays,crossStatus:(due,done,extended,previous)=>policy.deadlineLight(due,{done,extended,previous}),goalNow:()=>new Date().toISOString(),goalComplete:m=>m.done,visible:()=>true,nodeIsAutomatic:i=>i.source==='system',ceoIsDone:i=>i.stage===4,esc:x=>String(x??''),heading:()=>'',toast:x=>ctx.error=x,$:()=>null,document:{querySelectorAll:()=>[]},localStorage:{getItem:k=>db.get(k)||null,setItem:(k,v)=>db.set(k,v)},crossLoad:k=>JSON.parse(db.get(k)||'null')};
 ctx.crossPerson=()=>({person:'测试人员',personId:ctx.level==='负责人'?'manager-'+ctx.role:ctx.employeeId,center:ctx.role});
 for(const name of ['taskEmployees','ceoNodeRows','ceoManagementItems'])ctx[name]=()=>[];
 for(const name of ['navMarkup','embeddedDevelopmentIdentity','centerPage','canAct','taskCommit','goalCommit','commerceCommit','supplyCommit','stockCommit','visualCommit','modal','detail','completedItemNodes','knowledge','aiAnswer','render','bind'])ctx[name]=()=>{};
 ctx.docVisible=d=>d.scope==='全员'||d.center===ctx.role;ctx.crossCenterIndex=()=>2;
 const context=vm.createContext(ctx);vm.runInContext(source,context);return {ctx,db,run:code=>vm.runInContext(code,context)};
}
test('规则层完整加载，执行节点沿用交付期限，审批默认两天且不超过总期限',()=>{
 const f=fixture(),due=policy.addDays(policy.businessDay(),10);f.ctx.item={type:'task',stage:1,due};
 assert.equal(f.run('governanceNodeDue(item)'),due);f.ctx.item.stage=2;
 assert.equal(f.run('governanceNodeDue(item)'),policy.addDays(policy.businessDay(),2));
 f.ctx.item.due=policy.addDays(policy.businessDay(),1);assert.equal(f.run('governanceNodeDue(item)'),f.ctx.item.due);
});
test('账号本人、中心负责人和其他中心的个人资料访问隔离',()=>{
 const f=fixture();f.ctx.doc={center:2,scope:'内部',sensitivity:'个人资料',ownerId:'emp-2-1'};
 f.ctx.role=5;assert.equal(f.run('docVisible(doc)'),false);
 f.ctx.role=2;f.ctx.level='员工';f.ctx.employeeId='emp-2-2';assert.equal(f.run('docVisible(doc)'),false);
 f.ctx.employeeId='emp-2-1';assert.equal(f.run('docVisible(doc)'),true);
 f.ctx.level='负责人';assert.equal(f.run('docVisible(doc)'),true);
});
test('知识检索只包含最新审批生效版本，签署合同不进入公司知识',()=>{
 const f=fixture();f.db.set('hr',JSON.stringify({documents:[{id:'a',title:'公司规范',category:'制度规范',versions:[{version:1,approvedAt:'old',content:'旧版本'},{version:2,approvedAt:'new',content:'新版本'}]},{id:'b',title:'个人合作协议',subject:'person',versions:[{approvedAt:'new',content:'合同细节'}]}]}));
 const docs=f.run('governanceKnowledge()');assert.equal(docs.length,1);assert.equal(docs[0].body,'新版本');assert.equal(docs[0].version,2);
 assert.equal(f.run("governanceSensitive({title:'工资表',center:2})"),true);
});
test('撤回恢复保留记录，启动后终止须审批且不计完成',()=>{
 const f=fixture();const i={id:'test',type:'task',center:2,stage:0,due:policy.addDays(policy.businessDay(),5),history:[]};f.ctx.state.items=[i];f.db.set('main',JSON.stringify(f.ctx.state));
 f.run("governanceChange(state.items[0],'withdraw','修订申请')");assert.equal(f.ctx.state.items[0].governance.withdrawn,true);
 f.run("governanceChange(state.items[0],'resubmit')");assert.equal(f.ctx.state.items[0].governance.withdrawn,false);
 f.ctx.state.items[0].stage=1;f.db.set('main',JSON.stringify(f.ctx.state));
 f.run("governanceChange(state.items[0],'request','计划取消')");assert.equal(f.ctx.state.items[0].governance.termination.status,'待审核');
 f.run("governanceChange(state.items[0],'review','确认取消','通过')");assert.equal(f.ctx.state.items[0].governance.ended,true);assert.equal(f.ctx.state.items[0].stage,1);assert.equal(f.run('completedItemNodes(state.items[0]).length'),0);
});
test('不根据旧阶段猜测员工节点；自动任务与未完结流程不计入',()=>{
 const f=fixture();f.ctx.item={id:'i',type:'task',stage:4,title:'示例'};assert.equal(f.run('completedItemNodes(item).length'),0);
 f.ctx.item.eventsV2=[{id:'e1',node:'发起',stage:'发起',person:'测试人员',personId:'p',center:2,at:'2026-09-27'},{id:'e2',node:'处理',stage:'0',person:'员工',personId:'e',center:2,at:'2026-09-27'}];assert.equal(f.run('completedItemNodes(item).length'),2);
 f.ctx.item.source='system';assert.equal(f.run('completedItemNodes(item).length'),0);
});
test('行政终止不允许其他人撤回，也不允许结束后再次办理',()=>{
 const x={id:'r',status:'财务审核',workflow:{totalDue:policy.addDays(policy.businessDay(),5),stage:'财务审核',nodes:{},events:[{personId:'a',action:'purchaseSubmit'},{personId:'b',action:'purchaseReview',result:'通过'}]}},s={purchases:[x]};
 assert.throws(()=>policy.workflowLifecycle(s,{collection:'purchases',id:'r',note:'取消',actorIdentity:{personId:'c'}},'人事专员'),/发起人/);
 policy.workflowLifecycle(s,{collection:'purchases',id:'r',note:'取消',actorIdentity:{personId:'a'}},'人事专员');
 assert.equal(x.governance.termination.status,'待审核');
 policy.workflowLifecycle(s,{collection:'purchases',id:'r',note:'同意',decision:'审批',result:'通过',actorIdentity:{personId:'b'}},'财务负责人');
 assert.equal(x.governance.ended,true);assert.throws(()=>policy.workflowLifecycle(s,{collection:'purchases',id:'r',note:'恢复'},'人事专员'),/已终止/);
});

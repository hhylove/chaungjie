const test=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
function environment(){
 const c={state:{items:[]},activeRecordId:'',visible:()=>true,modal:(title,body)=>{c.last={title,body};},render:()=>{},listPanel:()=>'',document:{querySelector:()=>null,addEventListener:()=>{}},window:{scrollTo:()=>{}},esc:s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])),stages:{task:Array(5),purchase:Array(5),develop:Array(7),gallerycycle:Array(3),potentialcycle:Array(3),potential:Array(3),clearance:Array(4),producttrial:Array(4),visual:Array(5),stock:Array(5),service:Array(6)}};
 vm.createContext(c);vm.runInContext(fs.readFileSync('workspace-experience.js','utf8'),c);return c;
}
test('每种流程的所有状态都有唯一当前步骤，完结后全部完成',()=>{
 const c=environment();for(const [type,states] of Object.entries(c.stages)){for(let stage=0;stage<states.length;stage++){c.record={stage,due:'2026-09-30'};c.type=type;const html=vm.runInContext('uxWorkflow(type,record)',c);assert.ok(html.includes('完整流程'));assert.equal((html.match(/aria-current="step"/g)||[]).length,stage===states.length-1?0:1,type+':'+stage);if(stage===states.length-1)assert.ok(html.includes('全部步骤已完成'));}}
});
test('驳回回到实际办理步骤，不作为正常流程的下一站',()=>{
 const c=environment();const html=vm.runInContext("uxWorkflow('task',{stage:3,blocked:'补充证据'})",c);assert.match(html,/需补充或调整/);assert.match(html,/补充证据/);assert.doesNotMatch(html,/<strong>已驳回/);
});
test('视觉服务采用关联视觉工单进度，不显示发展中心受理或主理人验收',()=>{
 const c=environment();c.activeRecordId='service-1';c.state.items=[{id:'service-1',title:'海报',type:'service',serviceKind:'visual',visualTaskId:'visual-1',stage:1},{id:'visual-1',type:'visual',stage:2,due:'2026-09-30'}];c.modal('海报','<div class="flow"><span>旧流程</span></div>');assert.match(c.last.body,/制作并上传成果/);assert.match(c.last.body,/第 3 \/ 5 步/);assert.doesNotMatch(c.last.body,/发展中心|主理人验收/);
});
test('商品销售清仓任务显示接收、执行、审核和档案更新',()=>{
 const c=environment();c.activeRecordId='clear-1';c.state.items=[{id:'clear-1',title:'清仓任务',type:'clearance',lifecycleCode:'CY-1',stage:1}];c.modal('清仓任务','<form id="life-report-form"></form>');assert.match(c.last.body,/接收销售任务/);assert.match(c.last.body,/更新商品档案/);assert.doesNotMatch(c.last.body,/制定清仓方案/);
});
test('新建表单只展示流程预览，不虚构已完成步骤',()=>{
 const c=environment();c.modal('下发部门任务','<form id="task-create-form"></form>');assert.match(c.last.body,/办理路径/);assert.doesNotMatch(c.last.body,/aria-current|已完成/);
});
test('流程说明转义业务输入',()=>{
 const c=environment();const html=vm.runInContext("uxWorkflow('task',{stage:3,blocked:'<img src=x onerror=alert(1)>',due:'2026-09-30'})",c);assert.match(html,/&lt;img/);assert.doesNotMatch(html,/<img/);
});

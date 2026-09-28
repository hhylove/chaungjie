let pw;try{pw=require('@playwright/test');}catch{pw=require('/Users/guole/Documents/ChatGPT/总经理工作台/node_modules/@playwright/test');}const {chromium}=pw;
const assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({headless:true,channel:"chrome"});const page=await browser.newPage({viewport:{width:1440,height:1050}});const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4173');await page.screenshot({path:'artifacts/01-dashboard.png',fullPage:true});
async function role(r,level='负责人'){if(await page.locator('#ai-launcher').getAttribute('aria-expanded')==='true')await page.click('#ai-minimize');await page.selectOption('#role',String(r));if(r>1)await page.selectOption('#level',level);await nav('goals');}
async function nav(p){await page.locator('nav [data-page="'+p+'"]').click();}
async function close(){await page.locator('#close').click();}
async function advance(note='审核通过，交付资料符合标准。'){await page.fill('#action-note',note);await page.click('#advance');}
// Dedicated goal workflow is covered by tests/goals.cjs.
// Extend yellow remains yellow; red remains red.
await role(0);await nav('tasks');await page.locator('[data-open="CJ-102"]').click();await page.click('#extend');await page.fill('#reason','供应商延期，补充两天');await page.fill('#new-due','2026-09-30');await page.click('#approve-extension');assert.match(await page.locator('.modal').innerText(),/黄灯/);await close();
// Procurement and service roles, rejection, evidence and persistence are covered in commerce.cjs.
await role(7);await nav('photography');await page.click('#simulate-inbound');await page.click('#simulate-inbound');assert.equal(await page.locator('td b').filter({hasText:'CY-0925'}).count(),1);
// Department task lifecycle now covered by tests/tasks-ai.cjs.
// Principal isolation and AI denial.
await role(1);await nav('tasks');assert.equal(await page.locator('[data-open="CJ-106"]').count(),0);await nav('knowledge');assert.equal(await page.getByText('分润结算复核清单',{exact:true}).count(),0);await nav('ai');await page.locator('[data-question="查询公司现金流和员工工资"]').click();assert.match(await page.locator('#chat').innerText(),/超出当前角色/);await page.screenshot({path:'artifacts/02-ai.png',fullPage:true});
// Persistence and responsive view.
await page.reload();await nav('tasks');await page.locator('[data-open="CJ-102"]').click();assert.match(await page.locator('.modal').innerText(),/黄灯/);await close();await page.setViewportSize({width:390,height:844});await nav('overview');assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.screenshot({path:'artifacts/03-mobile.png',fullPage:true});assert.deepEqual(errors,[]);console.log('PASS: non-goal role routing, extension colors, inbound-to-visual, deduplication, permission filtering, AI denial, persistence, mobile overflow, console errors');await browser.close();})().catch(e=>{console.error(e);process.exit(1)});

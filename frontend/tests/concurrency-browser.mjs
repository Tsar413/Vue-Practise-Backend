import {chromium,expect} from '@playwright/test'
import {writeFileSync} from 'node:fs'
const browser=await chromium.launch({executablePath:'/opt/google/chrome/chrome',headless:true,args:['--no-sandbox']});const context=await browser.newContext();const page=await context.newPage();const checks=[],errors=[];page.on('pageerror',e=>errors.push(e.message));function done(name){checks.push({name,passed:true});console.log('通过',name)}
try{
 await page.goto('http://localhost:5173/login');await page.getByLabel('系统用户编号',{exact:true}).fill('DEMO2026001');await page.getByLabel('登录密码').fill('123456');await page.getByRole('button',{name:'登录平台'}).click();await expect(page).toHaveURL('http://localhost:5173/');const saved=await page.evaluate(()=>sessionStorage.getItem('vue-practice-session'))
 await page.getByRole('navigation').getByRole('link',{name:'我的项目数据',exact:true}).click();await page.getByRole('button',{name:'票券记录',exact:true}).click();await expect(page.locator('.table-wrap tbody')).toContainText('有效');await expect(page.getByRole('alert')).toHaveCount(0);done('票券页面自动使用普通模拟用户并展示真实状态')
 await page.getByRole('button',{name:'校园报修',exact:true}).click();await expect(page.locator('.identity-bar select option')).not.toHaveCount(1);const option=await page.locator('.identity-bar option').evaluateAll(els=>els.find(x=>x.textContent.includes('模拟报修人')).value);await page.locator('.identity-bar select').selectOption(option);await page.getByRole('button',{name:'工单列表',exact:true}).click();await expect(page.locator('.identity-bar')).toContainText('报修人仅可查询本人');const list=await page.locator('.table-wrap tbody tr').evaluateAll(rows=>rows.map(r=>r.children[3]?.textContent.trim()));expect(list.every(id=>id===option)).toBeTruthy();done('切换模拟身份后仅展示所选报修人可见范围')
 await page.getByRole('navigation').getByRole('link',{name:'接口文档',exact:true}).click();await page.getByLabel('搜索接口').fill('查询抢票模拟用户列表');await page.locator('.endpoint-summary').click()
 const other=await context.newPage();await other.goto('http://localhost:5173/login');await other.evaluate(v=>sessionStorage.setItem('vue-practice-session',v),saved);await other.goto('http://localhost:5173/docs');await other.getByLabel('搜索接口').fill('查询抢票模拟用户列表');await other.locator('.endpoint-summary').click()
 // 读取真实后端响应后延迟交付，模拟慢网络；没有构造业务响应。
 let sent=0
 await page.route('**/api/practice/*/ticket/users',async route=>{sent++;const response=await route.fetch();await new Promise(r=>setTimeout(r,14000));await route.fulfill({response})})
 await page.getByRole('button',{name:'发送请求',exact:true}).click();await expect(other.locator('.endpoint-body .toolbar button').first()).toBeDisabled();done('已打开的另一标签页实时同步十秒冷却')
 await page.getByLabel('搜索接口').fill('查询抢票活动列表');await page.getByLabel('搜索接口').fill('查询抢票模拟用户列表');await page.locator('.endpoint-summary').click();await page.waitForTimeout(10500);await expect(page.getByRole('button',{name:'请求进行中…',exact:true})).toBeDisabled();expect(sent).toBe(1);done('超过十秒且切换接口后，未结束的同一请求仍禁止重复发送')
 await expect(page.getByRole('button',{name:'发送请求',exact:true})).toBeEnabled({timeout:10000});done('真实慢响应完成后恢复按钮')
 expect(errors).toEqual([])
}catch(e){console.error(e);process.exitCode=1}finally{writeFileSync('../docs/concurrency-browser-results.json',JSON.stringify({checks,errors,date:new Date().toISOString()},null,2));await browser.close()}

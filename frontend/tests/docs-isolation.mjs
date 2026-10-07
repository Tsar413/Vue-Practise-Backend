import {chromium, expect} from '@playwright/test'
import {execFileSync} from 'node:child_process'
import {writeFileSync} from 'node:fs'
import {fileURLToPath} from 'node:url'
import ticketOps from '../src/data/ticket/index.js'
import repairOps from '../src/data/repair/index.js'
import {validate} from '../src/api/schema.js'

if (process.env.BACKEND_ISOLATED !== '1') throw new Error('仅允许本项目隔离服务')
const root=fileURLToPath(new URL('../../',import.meta.url))
// Read only the existing isolated fixture's access code; do not revoke its login.
const accessCode=execFileSync(root+'.runtime/local/usr/bin/mariadb',['--no-defaults',`--socket=${root}.runtime/db.sock`,'-uroot','-N','-B','-e',"SELECT api_access_code FROM vue_practice_isolated.sys_login_token WHERE user_id='DEMO2026001'"],{encoding:'utf8'}).trim()
expect(accessCode).toMatch(/^[a-f0-9]{64}$/)
const checks=[],errors=[]
const done=name=>{checks.push({name,passed:true});console.log('通过',name)}
async function request(op, params={}, body){
 let path=op.path.replace('{accessCode}',accessCode)
 const query=new URLSearchParams()
 for(const p of op.parameters||[]){if(p.name==='accessCode'||params[p.name]==null)continue;if(p.in==='path')path=path.replace(`{${p.name}}`,encodeURIComponent(params[p.name]));else query.set(p.name,params[p.name])}
 const response=await fetch('http://localhost:8100'+path+(query.size?'?'+query:''),{method:op.method,headers:body?{'Content-Type':'application/json'}:{},body:body?JSON.stringify(body):undefined})
 expect(response.status).toBe(200)
 const result=await response.json();expect(result.code).toBe(200)
 expect(validate(result,op.responses['200'].content['application/json'].schema)).toEqual([])
 return result.data
}
const op=id=>[...ticketOps,...repairOps].find(o=>o.operationId===id)
let browser,draft,admin
try{
 const ticketUsers=await request(op('TicketController_getAllUsers'))
 const repairUsers=await request(op('RepairController_getAllUsers'))
 const ticketUser=ticketUsers.find(u=>u.role==='USER'&&u.status===1)
 const repairUser=repairUsers.find(u=>u.role==='REPORTER'&&u.status===1)
 admin=ticketUsers.find(u=>u.role==='ADMIN'&&u.status===1)
 expect((await request(op('TicketController_getOneUser'),{userId:ticketUser.userNo})).id).toBe(ticketUser.id)
 expect((await request(op('RepairController_getOneUser'),{userId:repairUser.id})).id).toBe(repairUser.id)
 done('真实查询：抢票 userNo 字符串与报修数值 id 分别正确')
 const orders=await request(op('RepairController_getOrders'),{operatorId:repairUser.id})
 expect(orders.length).toBeGreaterThan(0)
 const detail=await request(op('RepairController_getOrderDetail'),{orderId:orders[0].id,operatorId:repairUser.id})
 expect(detail.order.id).toBe(orders[0].id)
 done('真实工单详情响应符合重新生成的嵌套对象定义')

 browser=await chromium.launch({executablePath:process.env.CHROME_PATH||'/opt/google/chrome/chrome',headless:true,args:['--no-sandbox']})
 const context=await browser.newContext({viewport:{width:1440,height:1050}})
 // UI session fixture only; all practice requests below use the real access code.
 await context.addInitScript(code=>sessionStorage.setItem('vue-practice-session',JSON.stringify({userId:'DEMO2026001',realName:'文档验收',role:'STUDENT',apiAccessCode:code})),accessCode)
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept())
 await page.goto('http://localhost:5173/docs')
 await expect(page.locator('.badge.enabled')).toHaveText('15 个接口')
 await expect(page.locator('.doc-folder')).toHaveCount(4)
 await page.locator('.docs-intro summary').click()
 await expect(page.locator('.docs-intro')).not.toContainText(/REPORTER|工单|报修/)
 await page.getByRole('button',{name:'全部展开',exact:true}).click()
 for(const locator of await page.locator('.endpoint-summary').all())await locator.click()
 await expect(page.locator('.doc-folders')).not.toContainText(/Repair|repair|REPORTER|MAINTAINER|工单|设备|制冷/)
 await page.evaluate(()=>scrollTo(0,0))
 await page.screenshot({path:root+'docs/screenshots/docs-ticket-regenerated.png',fullPage:false,mask:[page.locator('.url-copy')]})
 done('抢票 15 个接口及四个目录的参数、角色、示例没有报修内容')
 await page.getByLabel('搜索接口').fill('创建抢票活动')
 // The exact summary is sourced from the generated operation.
 await page.getByLabel('搜索接口').fill(op('TicketController_saveNewActivity').summary)
 const endpoint=page.locator('.endpoint')
 await expect(endpoint).toHaveCount(1)
 const create=op('TicketController_saveNewActivity')
 const body=structuredClone(create.requestBody.content['application/json'].example)
 body.operatorId=admin.id;body.activityName='接口文档重新生成验收'
 for(const [field,hours] of Object.entries({bookingStartTime:1,bookingEndTime:24,activityStartTime:25,activityEndTime:27}))body[field]=new Date(Date.now()+hours*3600000).toISOString().slice(0,19)
 await endpoint.getByLabel('JSON请求内容').fill(JSON.stringify(body,null,2))
 await endpoint.getByRole('button',{name:'发送请求',exact:true}).click()
 await expect(endpoint.locator('.response-meta')).toContainText('HTTP 200')
 const result=JSON.parse(await endpoint.locator('.response pre').innerText());draft=result.data
 expect(validate(result,create.responses['200'].content['application/json'].schema)).toEqual([])
 expect(draft.status).toBe(0);expect(draft.description).toContain('观影')
 done('浏览器按再生成的活动示例和 ISO 日期真实创建草稿成功')
 await request(op('TicketController_deleteActivity'),{activityId:draft.id,operatorId:admin.id});draft=null
 done('验收草稿已通过对应抢票删除接口清理')

 await page.getByRole('button',{name:'校园报修',exact:true}).click()
 await expect(page.getByLabel('搜索接口')).toHaveValue('')
 await expect(page.locator('.badge.enabled')).toHaveText('29 个接口')
 await expect(page.locator('.doc-folder')).toHaveCount(6)
 await page.locator('.docs-intro summary').click()
 await expect(page.locator('.docs-intro')).not.toContainText(/票券|报名|USER：/)
 await page.getByRole('button',{name:'全部展开',exact:true}).click()
 for(const locator of await page.locator('.endpoint-summary').all())await locator.click()
 await expect(page.locator('.doc-folders')).not.toContainText(/Ticket|ticket|抢票|报名|票券|活动/)
 await page.evaluate(()=>scrollTo(0,0))
 await page.screenshot({path:root+'docs/screenshots/docs-repair-regenerated.png',fullPage:false,mask:[page.locator('.url-copy')]})
 done('报修 29 个接口及六个目录没有抢票字段，项目切换清空旧搜索')
 await page.getByLabel('搜索接口').fill(op('RepairController_getOrderDetail').summary)
 await expect(page.locator('.schema-table').first()).toContainText('reporterId')
 await expect(page.locator('.schema-table').first()).toContainText('maintainerId')
 await page.clock.install();await page.clock.fastForward(11000)
 await page.getByLabel('orderId',{exact:true}).fill(String(orders[0].id))
 await page.getByLabel('operatorId',{exact:true}).fill(String(repairUser.id))
 await page.getByRole('button',{name:'发送请求',exact:true}).click()
 await expect(page.locator('.response-meta')).toContainText('HTTP 200')
 expect(JSON.parse(await page.locator('.response pre').innerText()).data.order.id).toBe(orders[0].id)
 done('浏览器报修工单详情真实请求使用 repair 路径，并展示完整嵌套响应字段')
 await page.getByLabel('搜索接口').fill('')
 await page.setViewportSize({width:390,height:844})
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true)
 await page.evaluate(()=>scrollTo(0,0))
 await page.screenshot({path:root+'docs/screenshots/docs-regenerated-mobile.png',fullPage:false,mask:[page.locator('.url-copy')]})
 done('390px 手机布局没有页面横向溢出')
 const teacher=await browser.newContext()
 await teacher.addInitScript(()=>sessionStorage.setItem('vue-practice-session',JSON.stringify({userId:'UI_ONLY',role:'TEACHER',realName:'教师界面验收'})))
 const teacherPage=await teacher.newPage();await teacherPage.goto('http://localhost:5173/docs')
 await expect(teacherPage.locator('.badge.enabled')).toHaveText('23 个接口')
 await teacherPage.getByRole('button',{name:'校园抢票',exact:true}).click()
 await expect(teacherPage.locator('.badge.enabled')).toHaveText('15 个接口')
 await teacherPage.getByRole('button',{name:'全部展开',exact:true}).click()
 await teacherPage.locator('.endpoint-summary').first().click()
 await expect(teacherPage.getByRole('button',{name:'发送请求',exact:true})).toHaveCount(0)
 done('教师系统文档保持独立，业务文档仍只读（界面会话验证）')
 expect(errors).toEqual([])
}catch(e){console.error(String(e).replaceAll(accessCode,'{访问码}'));process.exitCode=1}
finally{
 if(draft&&admin)await request(op('TicketController_deleteActivity'),{activityId:draft.id,operatorId:admin.id})
 await browser?.close()
 writeFileSync(root+'docs/docs-regeneration-results.json',JSON.stringify({date:new Date().toISOString(),checks,errors},null,2))
}

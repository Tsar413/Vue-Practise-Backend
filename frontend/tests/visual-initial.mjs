import {chromium} from '@playwright/test'
const browser=await chromium.launch({executablePath:'/opt/google/chrome/chrome',headless:true,args:['--no-sandbox']})
const page=await browser.newPage({viewport:{width:1440,height:1000}})
const errors=[];page.on('pageerror',e=>errors.push(e.message))
await page.goto('http://localhost:5173');await page.getByRole('heading',{name:'账号登录'}).waitFor();await page.screenshot({path:'../docs/screenshots/login-desktop.png',fullPage:true})
await page.setViewportSize({width:390,height:844});await page.screenshot({path:'../docs/screenshots/login-mobile.png',fullPage:true});console.log({errors,overflow:await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth)})
await browser.close()

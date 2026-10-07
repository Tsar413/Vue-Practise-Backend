import{N as l,c as r,b as e,F as c,m,d as u,O as p,t as s,a as f,o as k,f as d}from"./index-DY9rjfq6.js";const g={class:"section-heading"},v=["onClick"],x={class:"address"},I={class:"panel"},N={class:"section-heading"},C={__name:"Addresses",setup(b){const i={ticket:l("ticket"),repair:l("repair")},a=`import axios from 'axios'

const request = axios.create({
  baseURL: '${i.ticket}',
  timeout: 15000
})

// 基础地址已包含 /ticket，只添加接口后缀
const response = await request.get('/activities')
if (response.data.code === 200) {
  console.log(response.data.data)
}`;return(B,t)=>(k(),r(c,null,[t[4]||(t[4]=e("h1",null,"我的接口地址",-1)),t[5]||(t[5]=e("p",null,"请复制对应项目的接口基础地址，作为你在 Vue 项目中配置的请求地址。",-1)),(k(),r(c,null,m(i,(o,n)=>e("section",{key:n,class:"panel"},[e("div",g,[e("h2",null,s(n==="ticket"?"校园抢票":"校园报修")+"基础地址",1),e("button",{onClick:V=>u(p)(o)},"复制地址",8,v)]),e("code",x,s(o),1),e("p",null,[d("查询"+s(n==="ticket"?"活动":"设备")+"时，在此基础地址后添加 ",1),e("code",null,s(n==="ticket"?"/activities":"/devices"),1),t[1]||(t[1]=d("。请勿重复添加 ",-1)),e("code",null,"/"+s(n),1),t[2]||(t[2]=d("。",-1))])])),64)),e("section",I,[e("div",N,[t[3]||(t[3]=e("h2",null,"在你的 Vue 项目中使用",-1)),e("button",{class:"secondary",onClick:t[0]||(t[0]=o=>u(p)(a))},"复制示例")]),e("pre",null,s(a))]),t[6]||(t[6]=f('<section class="panel"><h2>开始前，请分清这些编号</h2><dl class="guide-list"><dt>网页登录凭证</dt><dd>token 用于平台登录后的系统操作，通过 Authorization: Bearer 原始token 发送；请勿再次哈希或使用 tokenHash。</dd><dt>实训访问码</dt><dd>apiAccessCode 已包含在你的接口地址中，对应本人工作空间。实训请求无需携带网页登录 token，请使用本人地址。</dd><dt>学号与模拟身份</dt><dd>学号是系统登录账号；userNo 是项目内模拟编号；模拟用户数据库 ID 是列表中的 id。业务 operatorId 和票券 userId 使用数据库 ID。抢票用户详情路径中的 userId 是 userNo。</dd><dt>重置后的数据</dt><dd>重置后，请重新查询数据，不要继续使用原来的活动或工单ID。访问码保持不变。</dd></dl></section>',1))],64))}};export{C as default};

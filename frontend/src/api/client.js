import axios from 'axios'
const configured = import.meta.env.VITE_API_BASE_URL
if (!configured || !/^https?:\/\//.test(configured)) throw new Error('请配置完整的后端地址 VITE_API_BASE_URL')
export const API_BASE = configured.replace(/\/+$/, '')
export const SESSION_KEY = 'vue-practice-session'
export function session() { try { return JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null') } catch { return null } }
export const system = axios.create({ baseURL: API_BASE, timeout: 30000 })
export const practice = axios.create({ baseURL: API_BASE, timeout: 30000 })
system.interceptors.request.use(c => { const s=session(); if(s?.token && c.url !== '/login') c.headers.Authorization=`Bearer ${s.token}`; return c })
system.interceptors.response.use(r=>{if(r.data?.code===401&&r.config.url!=='/login'){sessionStorage.removeItem(SESSION_KEY);window.dispatchEvent(new Event('session-expired'))}return r}, e=>{if(e.response?.status===401 && e.config?.url!=='/login') {sessionStorage.removeItem(SESSION_KEY); window.dispatchEvent(new Event('session-expired'))} return Promise.reject(e)})
export function unwrap(response) {
 const body=response.data
 if(body && typeof body==='object' && 'code' in body) {if(body.code!==200) throw new Error(body.message || '请求未成功'); return body.data}
 return body
}
export function errorText(e) {return e.response?.data?.message || (e.code==='ECONNABORTED'?'请求超时，请检查服务状态；请先查询结果再决定是否重新提交。':e.code==='ERR_NETWORK'?'无法连接服务，请检查网络、服务地址及跨域配置。':e.message) || '请求失败，请稍后重试。'}
export const sys = async (method,url,data,params)=>unwrap(await system.request({method,url,data,params}))
export function practicePath(project, suffix='') {
 const code=session()?.apiAccessCode
 if(!code) throw new Error('当前账号没有实训访问码，请重新登录或联系教师。')
 if(!['ticket','repair'].includes(project)) throw new Error('项目不存在')
 return `/api/practice/${encodeURIComponent(code)}/${project}${suffix}`
}
export const practiceBase=project=>API_BASE+practicePath(project)
export const query = async (project,path,params)=>unwrap(await practice.get(practicePath(project,path),{params}))
export async function binary(path) {
 const response=await practice.get(path,{responseType:'blob',validateStatus:()=>true})
 if(response.status>=400 || response.data.type.includes('json')) {
  let message='图片读取失败'
  try {message=JSON.parse(await response.data.text()).message || message} catch {}
  throw new Error(message)
 }
 return response
}
export function filename(headers,fallback='实训图片') {
 const disposition=headers['content-disposition'] || ''
 const utf=disposition.match(/filename\*=UTF-8''([^;]+)/i)
 if(utf) {try{return decodeURIComponent(utf[1])}catch{}}
 return disposition.match(/filename="?([^";]+)"?/i)?.[1] || fallback
}
export function saveBlob(blob,name) {const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}

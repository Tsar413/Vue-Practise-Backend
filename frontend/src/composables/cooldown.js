import { ref,computed,onUnmounted } from 'vue'
import { useAuth } from '../stores/auth'
import {deadlineKey,remaining,formatRemaining} from './cooldown-core'
export function useCooldown(kind) {
 const auth=useAuth(), key=deadlineKey(auth.user.userId,kind)
 const read=()=>Number(localStorage.getItem(key)||0)
 const deadline=ref(read()),now=ref(Date.now())
 const tick=()=>{deadline.value=read();now.value=Date.now()}
 const timer=setInterval(tick,250)
 window.addEventListener('storage',tick)
 onUnmounted(()=>{clearInterval(timer);window.removeEventListener('storage',tick)})
 const seconds=computed(()=>remaining(deadline.value,now.value))
 return {seconds,label:computed(()=>formatRemaining(seconds.value)),start(ms){deadline.value=Date.now()+ms;localStorage.setItem(key,String(deadline.value));tick()},refresh:tick,key}
}
// 同一浏览器标签页间串行检查冷却，重置请求进行中也不会重复发出。
export async function exclusive(name,fn) {
 if(navigator.locks) return navigator.locks.request(name,{ifAvailable:true},lock=>{if(!lock) throw new Error('另一个页面正在提交，请等待完成。');return fn()})
 const key=`${name}:lock`,now=Date.now(),old=Number(localStorage.getItem(key)||0)
 if(old>now)throw new Error('另一个页面正在提交，请等待完成。')
 localStorage.setItem(key,String(now+90000));try{return await fn()}finally{localStorage.removeItem(key)}
}

import { defineStore } from 'pinia'
import { session,SESSION_KEY,sys } from '../api/client'
export const useAuth=defineStore('auth',{
 state:()=>({user:session(),notice:''}),getters:{teacher:s=>s.user?.role==='TEACHER'},
 actions:{
 async login(id,password){const d=await sys('post','/login',{id:id.trim(),password});if(!d?.token || !['TEACHER','STUDENT'].includes(d.role))throw new Error('登录响应不完整，请联系教师。');this.user=d;sessionStorage.setItem(SESSION_KEY,JSON.stringify(d));this.notice=''},
 clear(){this.user=null;sessionStorage.removeItem(SESSION_KEY)},
 async logout(){try{await sys('post','/logout')}finally{this.clear()}}
 }})

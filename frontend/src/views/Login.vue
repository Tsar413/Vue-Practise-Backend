<script setup>
import {ref} from 'vue'
import {useAuth} from '../stores/auth'
import {router} from '../router'
import {errorText} from '../api/client'
const auth=useAuth(),id=ref(''),password=ref(''),busy=ref(false),error=ref('')
async function submit(){busy.value=true;error.value='';try{await auth.login(id.value,password.value);router.push('/')}catch(e){error.value=errorText(e)}finally{busy.value=false}}
</script>
<template><div class="login-layout"><section class="login-intro"><span class="eyebrow">从这里，开始今天的实践</span><h1>让课堂知识<br>在实践中生长<span>。</span></h1><p>在属于你的工作空间中，认识数据、理解接口，<br>一步一步完成自己的 Vue 项目。</p><div class="lesson-strips"><div><b>01</b><span>校园抢票<small>活动查询 · 报名与票券</small></span></div><div><b>02</b><span>校园报修<small>设备查询 · 工单流程</small></span></div></div><p class="quiet">请先查看参数要求，再填写请求内容。</p></section><form class="login-form" @submit.prevent="submit"><span class="eyebrow">欢迎回到实训课堂</span><h2>账号登录</h2><p>请使用教师分配的系统用户编号和密码。</p><div v-if="error||auth.notice" role="alert" class="alert error">{{error||auth.notice}}</div><label>系统用户编号<input v-model="id" autocomplete="username" placeholder="学生请填写学号" maxlength="50" required></label><label>登录密码<input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" maxlength="50" required></label><button class="wide" :disabled="busy">{{busy?'正在登录…':'登录平台 →'}}</button><div class="login-foot">账号由教师统一创建或导入。<br>如无法登录，请联系任课教师。</div></form></div></template>

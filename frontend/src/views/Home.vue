<script setup>
import {onMounted,ref} from 'vue'
import {useAuth} from '../stores/auth'
import {sys,errorText} from '../api/client'
import Detail from '../components/Detail.vue'
const auth=useAuth(),workspace=ref(null),error=ref(''),busy=ref(false)
async function load(){busy.value=true;error.value='';try{workspace.value=await sys('get',`/api/sys-workspace/one/${encodeURIComponent(auth.user?.userId)}`)}catch(e){error.value=errorText(e)}finally{busy.value=false}}
onMounted(()=>{if(!auth.teacher)load()})
</script>
<template><section class="welcome"><div><span class="eyebrow">{{auth.teacher?'有序管理，从容教学':'我的实训课堂'}}</span><h1>{{auth.user?.realName}}，{{auth.teacher?'欢迎回到教学平台':'开始今天的实践吧'}}<span>。</span></h1><p>{{auth.teacher?'管理班级与账号，为学生准备独立的实训环境。':'请先查看项目数据，了解模拟用户与业务状态，再开始接口练习。'}}</p><RouterLink class="button" :to="auth.teacher?'/teacher/classes':'/data'">{{auth.teacher?'管理我的班级':'查看我的项目数据'}} →</RouterLink></div><div class="welcome-side"><span class="outline-icon">◇</span><strong>{{auth.teacher?'组织 · 指导 · 成长':'观察 · 理解 · 实践'}}</strong><span>每一次动手，都是一次进步</span></div></section>
<section class="steps"><div v-for="(step,i) in (auth.teacher?['创建班级','分配账号','初始化空间','开展实训']:['查看项目数据','复制接口地址','阅读参数要求','尝试接口请求'])" :key="step"><b>0{{i+1}}</b><span>{{step}}</span></div></section>
<div class="section-heading"><h2>{{auth.teacher?'教学工作台':'我的实训项目'}}</h2><span>{{auth.teacher?'统一管理，有序开展':'两个项目，一个专属空间'}}</span></div>
<div v-if="auth.teacher" class="project-grid"><RouterLink class="project-tile" to="/teacher/users"><span class="project-number">01</span><h2>系统用户</h2><p>创建学生和教师账号，按班级查询学生，维护账号状态。</p><span class="link-text">进入用户管理 →</span></RouterLink><RouterLink class="project-tile green" to="/teacher/workspaces"><span class="project-number">02</span><h2>工作空间</h2><p>按班级初始化项目，按学号管理空间与重置数据。</p><span class="link-text">进入空间管理 →</span></RouterLink></div>
<div v-else class="project-grid"><RouterLink class="project-tile" to="/data?project=ticket"><span class="project-number">01</span><h2>校园抢票</h2><p>认识模拟用户、活动与票券，理解报名、取消和核销的完整流程。</p><span class="link-text">查看抢票数据 →</span></RouterLink><RouterLink class="project-tile green" to="/data?project=repair"><span class="project-number">02</span><h2>校园报修</h2><p>从设备与工单出发，观察派单、维修、确认与评价的业务过程。</p><span class="link-text">查看报修数据 →</span></RouterLink></div>
<section v-if="!auth.teacher" class="panel"><div class="section-heading"><h2>我的工作空间</h2><button class="secondary" :disabled="busy" @click="load">刷新信息</button></div><div v-if="error" class="alert error">{{error}}</div><p v-if="busy">正在读取空间信息…</p><Detail v-if="workspace" :value="workspace"/><p class="hint">重置后，请重新查询数据，不要继续使用原来的活动或工单ID。</p></section>
</template>

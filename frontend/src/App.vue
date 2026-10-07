<script setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {router} from './router'
import {useAuth} from './stores/auth'
import {ui,notify} from './composables/ui'
import {errorText} from './api/client'
const auth=useAuth(),route=useRoute()
const links=computed(()=>auth.teacher?[['/','教学首页'],['/teacher/classes','班级管理'],['/teacher/users','系统用户'],['/teacher/workspaces','工作空间'],['/teacher/import','学生导入'],['/docs','接口文档']]:[['/','实训首页'],['/data','我的项目数据'],['/addresses','接口地址'],['/docs','接口文档'],['/reset','数据重置']])
async function logout(){try{await auth.logout()}catch(e){notify(`浏览器登录状态已清理。服务端退出失败：${errorText(e)}`,true)}finally{router.push('/login')}}
window.addEventListener('session-expired',()=>{auth.clear();auth.notice='登录已失效，请重新登录。';router.push('/login')})
</script>
<template>
 <div class="site"><div class="topline"><div class="container topinner"><span><i class="dot"></i> 欢迎来到 Vue 实训课堂</span><span v-if="auth.user">{{auth.user.realName}} <span class="toprole">{{auth.teacher?'教师':'学生'}}</span><button class="text-button" @click="logout">退出登录</button></span><span v-else>在实践中学习 · 在探索中成长</span></div></div>
 <header class="container brandbar"><RouterLink to="/" class="brand"><span class="brandmark">◇</span><div>Vue实训管理平台<small>连接课堂与实践</small></div></RouterLink><div class="brandnote"><span>学习有方向，实践有空间</span><small>独立空间 · 真实数据 · 动手实践</small></div></header>
 <nav class="mainnav" aria-label="主导航"><div class="container navinner"><template v-if="auth.user"><RouterLink v-for="[url,title] in links" :key="url" :to="url" :class="{active:route.path===url}">{{title}}</RouterLink></template><span v-else class="navlabel">账号登录</span><span class="navtail">{{auth.teacher?'教学管理':'实训课堂'}}</span></div></nav>
 <main class="container main"><div v-if="auth.user" class="breadcrumb">首页 <span>/</span> {{route.meta.title}}</div><RouterView v-if="route.meta.public || auth.user" :key="route.path" /></main>
 <footer><div class="container footergrid"><div class="footerbrand">Vue实训管理平台<small>从理解接口开始，把想法变成作品。</small></div><div><strong>课堂指引</strong><p>请使用教师分配的账号登录<br>遇到账号或空间问题，请联系任课教师</p></div><div><strong>实践约定</strong><p>使用本人工作空间<br>重置前保存需要保留的内容</p></div></div><div class="copyright">Vue 实训课堂 · 学习、实践、进步</div></footer>
 <div v-if="ui.message" role="status" class="toast" :class="{error:ui.error}">{{ui.message}}<button aria-label="关闭提示" @click="ui.message=''">×</button></div>
 </div>
</template>

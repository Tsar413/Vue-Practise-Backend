<script setup>
import {ref,computed,watch,onMounted,onUnmounted} from 'vue'
import {useAuth} from '../stores/auth'
import {useCooldown} from '../composables/cooldown'
import ticketOps from '../data/ticket'
import repairOps from '../data/repair'
import Endpoint from '../components/Endpoint.vue'
import {groupOperations} from '../data/doc-groups'
const auth=useAuth(),cool=useCooldown('docs'),systemOps=ref([]),search=ref(''),project=ref(auth.teacher?'system':'ticket'),opened=ref({}),revision=ref(0)
const projectNames={ticket:'校园抢票',repair:'校园报修',system:'系统管理'}
const operations=computed(()=>project.value==='ticket'?ticketOps:project.value==='repair'?repairOps:systemOps.value)
const groups=computed(()=>groupOperations(operations.value,project.value))
const dictionaries={
 ticket:[['模拟角色','USER：普通用户；ADMIN：抢票管理员'],['模拟用户','0：停用；1：启用'],['活动','0：草稿；1：已发布；2：已关闭'],['票券','0：已取消；1：有效；2：已核销']],
 repair:[['模拟角色','REPORTER：报修人；MAINTAINER：维修师傅；ADMIN：报修管理员'],['模拟用户和设备','0：停用；1：启用 / 在用'],['工单','0：已撤销；1：待分派；2：待接单；3：维修中；4：待确认；5：已完成'],['图片类型','1：故障图片；2：维修图片'],['图片状态','0：临时未关联；1：已关联']],
 system:[['系统角色','TEACHER：教师；STUDENT：学生'],['账号、班级和空间','0：停用或暂停；1：启用']]
}
const visibleGroups=computed(()=>{
 const term=search.value.trim().toLowerCase()
 return groups.value.map(group=>({
  ...group,
  operations:group.operations.filter(op=>!term||`${group.name} ${op.summary} ${op.path} ${op.method} ${op.description} ${op.parameters?.map(p=>p.name).join(' ')}`.toLowerCase().includes(term))
 })).filter(group=>group.operations.length)
})
const visibleCount=computed(()=>visibleGroups.value.reduce((total,group)=>total+group.operations.length,0))
const allExpanded=computed(()=>visibleGroups.value.length>0&&visibleGroups.value.every(group=>opened.value[group.key]))
function toggleAll(){const expand=!allExpanded.value;for(const group of visibleGroups.value)opened.value[group.key]=expand}
// 搜索时自动展开命中的目录，接口仍由用户按需展开。
watch(project,()=>{search.value=''})
watch([search,project],()=>{if(search.value.trim())for(const group of visibleGroups.value)opened.value[group.key]=true})

function reset(e){if(e.type==='storage'&&e.key!==`practice:${auth.user?.userId}:reset-event`)return;revision.value++}
onMounted(async()=>{if(auth.teacher){const s=await import('../data/system-spec.json');systemOps.value=s.default}window.addEventListener('storage',reset);window.addEventListener('practice-reset',reset)})
onUnmounted(()=>{window.removeEventListener('storage',reset);window.removeEventListener('practice-reset',reset)})
</script>
<template><div class="section-heading"><div><h1>接口文档</h1><p>从参数到响应，逐步理解每一次请求。</p></div><span class="badge enabled">{{visibleCount}} 个接口</span></div><section class="docs-intro"><h3>{{projectNames[project]}}接口</h3><p v-if="project!=='system'"><code>/api/practice/{accessCode}/{{project}}</code></p><p>示例仅用于说明字段。业务 ID、编号和日期请替换为当前项目的有效值；模拟用户 ID 应从本项目用户列表查询，重置后重新查询。</p><p v-if="!auth.teacher">本站在线尝试每次发送后需等待十秒；失败请求也需等待。你的独立 Vue 项目直接请求后端不受此限制。只读数据查询、普通图片预览和下载无需等待。</p><details :key="project"><summary>查看{{projectNames[project]}}状态与角色说明</summary><table><tbody><tr v-for="row in dictionaries[project]" :key="row[0]"><th>{{row[0]}}</th><td>{{row[1]}}</td></tr></tbody></table></details></section><div class="docs-toolbar"><div class="tabs"><button v-if="auth.teacher" :class="{active:project==='system'}" @click="project='system'">系统管理</button><button :class="{active:project==='ticket'}" @click="project='ticket'">校园抢票</button><button :class="{active:project==='repair'}" @click="project='repair'">校园报修</button></div><input v-model="search" type="search" placeholder="在当前项目中搜索接口…" aria-label="搜索接口"><button class="secondary" :disabled="!visibleGroups.length" @click="toggleAll">{{allExpanded?'全部折叠':'全部展开'}}</button></div><div v-if="!auth.teacher&&cool.seconds.value" class="cooldown-banner" role="status">请在 {{cool.seconds.value}} 秒后再次发送请求。你可以继续阅读文档和编辑参数。</div><div class="doc-folders">
 <section v-for="group in visibleGroups" :key="group.key" class="doc-folder">
  <h2 class="doc-folder-heading">
   <button class="doc-folder-toggle" :aria-expanded="!!opened[group.key]" :aria-controls="`folder-${group.key}`" @click="opened[group.key]=!opened[group.key]">
    <svg class="folder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M3 7V5a1 1 0 0 1 1-1h6l2 3h8a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1Z"/><path d="M3 9h18"/></svg>
    <span class="doc-folder-name">{{group.name}}<small>{{group.description}}</small></span>
    <span class="doc-folder-count">{{group.operations.length}} 个接口</span>
    <span class="folder-chevron" aria-hidden="true">{{opened[group.key]?'−':'＋'}}</span>
   </button>
  </h2>
  <div v-show="opened[group.key]" :id="`folder-${group.key}`" class="doc-folder-content">
   <Endpoint v-for="op in group.operations" :key="op.operationId+revision" :op="op" :cool="cool"/>
  </div>
 </section>
 <p v-if="!visibleCount" class="empty">没有找到匹配的接口，请调整搜索条件。</p>
</div></template>

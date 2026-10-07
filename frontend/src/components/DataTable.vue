<script setup>
import {computed,ref,watch} from 'vue'
import {labels,display} from '../composables/ui'
const props=defineProps({rows:{type:Array,default:()=>[]},columns:Array,kind:String,actions:Boolean})
const search=ref(''),page=ref(1)
const filtered=computed(()=>props.rows.filter(r=>Object.values(r).some(v=>String(v??'').toLowerCase().includes(search.value.toLowerCase()))))
const pages=computed(()=>Math.max(1,Math.ceil(filtered.value.length/10)))
const visible=computed(()=>filtered.value.slice((page.value-1)*10,page.value*10))
watch([search,()=>props.rows],()=>page.value=1)
</script>
<template><div class="table-tools"><span>共 {{filtered.length}} 条 <small>· 当前列表内筛选、分页</small></span><input v-model="search" type="search" placeholder="筛选已加载数据…" aria-label="筛选已加载数据"></div><div class="table-wrap"><table><thead><tr><th v-for="c in columns" :key="c">{{labels[c]||c}}</th><th v-if="actions">操作</th></tr></thead><tbody><tr v-for="(row,i) in visible" :key="row.id??i"><td v-for="c in columns" :key="c"><span :class="{badge:c==='status',enabled:c==='status'&&row[c]===1}">{{display(row[c],c,kind)}}</span></td><td v-if="actions"><div class="row-actions"><slot :row="row" /></div></td></tr><tr v-if="!visible.length"><td :colspan="columns.length+(actions?1:0)" class="empty">暂无数据。请调整查询条件，或刷新后再查看。</td></tr></tbody></table></div><div class="pager"><button class="secondary" :disabled="page<=1" @click="page--">上一页</button><span>第 {{page}} / {{pages}} 页</span><button class="secondary" :disabled="page>=pages" @click="page++">下一页</button></div></template>

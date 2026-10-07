<script setup>
import {labels,display} from '../composables/ui'
defineProps({value:Object,kind:String})
</script>
<template><dl class="detail-list"><template v-for="(v,k) in value" :key="k"><dt>{{labels[k]||k}}</dt><dd><Detail v-if="v&&typeof v==='object'&&!Array.isArray(v)" :value="v" :kind="kind"/><span v-else>{{display(v,k,kind)}}</span></dd></template></dl></template>

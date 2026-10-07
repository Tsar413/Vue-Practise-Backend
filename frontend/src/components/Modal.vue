<script setup>
import {onMounted,onUnmounted,ref} from 'vue'
defineProps({title:String});const emit=defineEmits(['close']);const box=ref(null);let previous
function key(e){if(e.key==='Escape')emit('close');if(e.key==='Tab'){const els=[...box.value.querySelectorAll('button,input,select,textarea,a[href]')].filter(x=>!x.disabled);const first=els[0],last=els.at(-1);if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus()}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus()}}}
onMounted(()=>{previous=document.activeElement;box.value.focus();document.addEventListener('keydown',key);document.body.style.overflow='hidden'})
onUnmounted(()=>{document.removeEventListener('keydown',key);document.body.style.overflow='';previous?.focus()})
</script>
<template><Teleport to="body"><div class="modal-backdrop" @click.self="emit('close')"><section ref="box" tabindex="-1" role="dialog" aria-modal="true" :aria-label="title" class="modal"><header><h2>{{title}}</h2><button class="text-button" aria-label="关闭" @click="emit('close')">×</button></header><slot/></section></div></Teleport></template>

import {createRouter,createWebHistory} from 'vue-router'
import {useAuth} from './stores/auth'
const routes=[
 {path:'/login',component:()=>import('./views/Login.vue'),meta:{public:true,title:'账号登录'}},
 {path:'/',component:()=>import('./views/Home.vue'),meta:{title:'实训首页'}},
 {path:'/teacher/classes',component:()=>import('./views/Management.vue'),props:{kind:'classes'},meta:{teacher:true,title:'班级管理'}},
 {path:'/teacher/users',component:()=>import('./views/Management.vue'),props:{kind:'users'},meta:{teacher:true,title:'系统用户'}},
 {path:'/teacher/workspaces',component:()=>import('./views/Management.vue'),props:{kind:'workspaces'},meta:{teacher:true,title:'工作空间'}},
 {path:'/teacher/import',component:()=>import('./views/Import.vue'),meta:{teacher:true,title:'学生导入'}},
 {path:'/data',component:()=>import('./views/ProjectData.vue'),meta:{student:true,title:'我的项目数据'}},
 {path:'/addresses',component:()=>import('./views/Addresses.vue'),meta:{student:true,title:'接口地址'}},
 {path:'/docs',component:()=>import('./views/Docs.vue'),meta:{title:'接口文档'}},
 {path:'/reset',component:()=>import('./views/Reset.vue'),meta:{student:true,title:'数据重置'}},
 {path:'/:pathMatch(.*)*',redirect:'/'}
]
export const router=createRouter({history:createWebHistory(),routes,scrollBehavior:()=>({top:0})})
router.beforeEach(to=>{const a=useAuth();if(!to.meta.public&&!a.user)return '/login';if(to.path==='/login'&&a.user)return '/';if(to.meta.teacher&&!a.teacher || to.meta.student&&a.teacher)return '/';document.title=`${to.meta.title||'实训首页'} · Vue实训管理平台`})

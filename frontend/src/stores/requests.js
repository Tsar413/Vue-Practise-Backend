import {reactive} from 'vue'
// 页面、接口切换不会丢失尚未完成的请求状态。
export const pendingRequests=reactive(new Set())

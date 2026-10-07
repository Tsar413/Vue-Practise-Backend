import { reactive } from 'vue'
export const ui=reactive({message:'',error:false})
let timer
export function notify(message,error=false){clearTimeout(timer);ui.message=message;ui.error=error;timer=setTimeout(()=>ui.message='',6000)}
export async function copy(text){try{await navigator.clipboard.writeText(text);notify('已复制到剪贴板')}catch{const area=document.createElement('textarea');area.value=text;document.body.append(area);area.select();const ok=document.execCommand('copy');area.remove();notify(ok?'已复制到剪贴板':'复制失败，请长按或选中地址手动复制。',!ok)}}
export const roles={TEACHER:'教师',STUDENT:'学生',ADMIN:'模拟管理员',USER:'模拟用户',REPORTER:'模拟报修人',MAINTAINER:'模拟维修师傅'}
export const labels={id:'编号',imageUrl:'图片地址',attachmentId:'附件ID',fromStatus:'处理前状态',toStatus:'处理后状态',targetUserId:'目标用户ID',coverUrl:'封面地址',remark:'备注',orderImageId:'工单图片关联ID',processRecordId:'处理记录ID',uploaderId:'上传人ID',originalName:'文件名',contentType:'文件类型',studentId:'学号',classId:'班级编号',className:'班级名称',username:'用户名',realName:'姓名',role:'角色',status:'状态',userNo:'模拟用户编号',name:'名称',phone:'联系电话',department:'部门',campus:'校区',activityName:'活动名称',location:'地点',quota:'名额',bookedCount:'已报名人数',deviceName:'设备名称',deviceType:'设备类型',deviceNo:'设备编号',orderNo:'工单编号',title:'标题',ticketNo:'票券编号',userId:'模拟用户数据库ID',activityId:'活动ID',workspaceId:'工作空间ID',reporterId:'报修人ID',maintainerId:'维修师傅ID',description:'说明',repairResult:'维修结果',contactPhone:'联系电话',createTime:'创建时间',updateTime:'更新时间',bookingTime:'报名时间',bookingStartTime:'报名开始',bookingEndTime:'报名结束',activityStartTime:'活动开始',activityEndTime:'活动结束',cancelTime:'撤回时间',verifyTime:'核销时间',verifyUserId:'核销人ID',completedTime:'完成时间',lastLoginTime:'最近登录',reporterName:'报修人',maintainerName:'维修师傅',order:'工单',orderId:'工单ID',operatorId:'模拟操作人ID',operatorName:'操作人',action:'处理动作',content:'内容',score:'评分',comment:'评价内容',imageId:'图片ID',imageType:'图片类型',originalFilename:'文件名',fileSize:'文件大小',mimeType:'文件类型',previewPath:'预览路径',downloadPath:'下载路径',sortOrder:'顺序',deviceId:'设备ID',apiAccessCode:'访问码',ticketInitialized:'抢票初始化',repairInitialized:'报修初始化'}
export function display(value,key,kind=''){
 if(value==null || value==='')return '—'
 if(key==='role')return roles[value]||value
 if(key==='status')return ({activities:['草稿','已发布','已关闭'],records:['已取消','有效','已核销'],orders:['已撤回','待分派','待接单','维修中','待确认','已完成']}[kind]||['停用 / 暂停','启用'])[value]||value
 if(key==='action')return ({CREATE:'创建',ASSIGN:'派单',ACCEPT:'接单',RECORD:'追加维修说明',SUBMIT:'提交结果',RETURN:'退回',CONFIRM:'确认',CANCEL:'撤回'})[value]||value
 if(typeof value==='object')return JSON.stringify(value)
 return String(value).replace(/<[^>]*>/g,'')
}

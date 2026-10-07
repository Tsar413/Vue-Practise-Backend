import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {remaining,deadlineKey,formatRemaining} from '../src/composables/cooldown-core.js'
import {validate,safeExample} from '../src/api/schema.js'
import ticketOps from '../src/data/ticket/index.js'
import repairOps from '../src/data/repair/index.js'
import {projectSuffix} from '../src/api/project-contract.js'
import {groupOperations} from '../src/data/doc-groups.js'
test('截止时间按真实时间计算，跨学生和冷却类型隔离',()=>{assert.equal(remaining(310000,10000),300);assert.equal(remaining(310000,10321),300);assert.equal(remaining(310000,310001),0);assert.equal(formatRemaining(272),'04:32');assert.notEqual(deadlineKey('0001','reset'),deadlineKey('0002','reset'));assert.notEqual(deadlineKey('0001','reset'),deadlineKey('0001','docs'))})
test('请求校验挡住缺失字段、非法枚举与错误数字，且不改写示例为假有效ID',()=>{const schema={type:'object',required:['operatorId'],properties:{operatorId:{type:'integer'},status:{type:'integer',enum:[0,1]}}};assert.ok(validate({},schema).length);assert.ok(validate({operatorId:NaN},schema).length);assert.ok(validate({operatorId:1,status:3},schema).length);assert.deepEqual(validate({operatorId:1,status:0},schema),[]);assert.equal(safeExample({operatorId:101}).operatorId,null)})
test('学生文档只包含业务接口；图片读取不包含operatorId',()=>{const ops=[...ticketOps,...repairOps];assert.equal(ops.length,44);assert.ok(ops.every(o=>o.path.startsWith('/api/practice/')));for(const op of ops.filter(o=>/\/images\/\{imageId\}\/(preview|download)$/.test(o.path))){assert.ok(!op.parameters.some(p=>p.name==='operatorId'))}assert.equal(JSON.parse(readFileSync(new URL('../src/data/system-spec.json',import.meta.url))).length,23)})
test('两项目各自包含完整操作且归入唯一功能目录，跨项目字段和示例不串入',()=>{
 for(const [project,ops,count,forbidden] of [['ticket',ticketOps,15,/Repair|repair|REPORTER|MAINTAINER|工单|报修|设备|制冷/],['repair',repairOps,29,/Ticket|ticket|抢票|报名|票券|活动/]]){
  assert.equal(ops.length,count)
  assert.equal(new Set(ops.map(o=>`${o.method} ${o.path}`)).size,count)
  for(const op of ops){assert.equal(op.project,project);assert.ok(op.path.startsWith(`/api/practice/{accessCode}/${project}/`));assert.doesNotMatch(JSON.stringify(op),forbidden)}
  assert.equal(groupOperations(ops,project).flatMap(g=>g.operations).length,count)
  assert.equal(groupOperations(project==='ticket'?repairOps:ticketOps,project).length,0)
 }
 assert.ok(!ticketOps.some(o=>o.method==='POST'&&o.path.endsWith('/users')))
 assert.ok(repairOps.some(o=>o.method==='POST'&&o.path.endsWith('/users')))
})
test('全部 JSON 请求及成功响应示例通过对应字段约束',()=>{
 for(const op of [...ticketOps,...repairOps]){
  const request=op.requestBody?.content?.['application/json']
  if(request)assert.deepEqual(validate(request.example,request.schema),[],op.operationId+' request')
  const response=op.responses['200'].content?.['application/json']
  if(response)assert.deepEqual(validate(response.example,response.schema),[],op.operationId+' response')
 }
})
test('不同项目的 userId 含义、活动日期和工单嵌套结构保持源码契约',()=>{
 const find=id=>[...ticketOps,...repairOps].find(o=>o.operationId===id)
 assert.equal(find('TicketController_getOneUser').parameters.find(p=>p.name==='userId').schema.type,'string')
 assert.equal(find('RepairController_getOneUser').parameters.find(p=>p.name==='userId').schema.type,'integer')
 assert.equal(find('TicketController_getUserRecords').parameters.find(p=>p.name==='userId').schema.type,'integer')
 const body=find('TicketController_saveNewActivity').requestBody.content['application/json']
 assert.match(body.example.bookingStartTime,/^\d{4}-\d{2}-\d{2}T/)
 assert.equal(find('RepairController_getOrderDetail').responses['200'].content['application/json'].schema.properties.data.properties.order.type,'object')
 assert.ok(find('RepairController_getOrderDetail').responses['200'].content['application/json'].schema.properties.data.properties.order.properties.reporterId)
})
test('发送前拒绝路径和项目不匹配，正常路径保留完整后缀',()=>{
 assert.equal(projectSuffix('ticket','/api/practice/{accessCode}/ticket/users/{userId}/records'),'/users/{userId}/records')
 assert.throws(()=>projectSuffix('ticket','/api/practice/{accessCode}/repair/users'),/不一致/)
 assert.throws(()=>projectSuffix('repair','/api/practice/{accessCode}/repair-other/users'),/不一致/)
})

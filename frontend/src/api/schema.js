export function validate(value,schema={},path='请求内容') {
 const errors=[]
 if(value==null){if(!schema.nullable)errors.push(`${path}不能为空`);return errors}
 if(schema.type==='object'){
  if(typeof value!=='object'||Array.isArray(value))return [`${path}必须是对象`]
  for(const k of schema.required||[])if(value[k]===undefined||value[k]===null||value[k]==='')errors.push(`${path}.${k}为必填项`)
  for(const [k,v] of Object.entries(value)){if(schema.properties?.[k]&&v!==undefined)errors.push(...validate(v,schema.properties[k],`${path}.${k}`))}
 }else if(schema.type==='array'){
  if(!Array.isArray(value))return [`${path}必须是数组`]
  if(schema.minItems!=null&&value.length<schema.minItems)errors.push(`${path}至少${schema.minItems}项`)
  if(schema.maxItems!=null&&value.length>schema.maxItems)errors.push(`${path}最多${schema.maxItems}项`)
  value.forEach((v,i)=>errors.push(...validate(v,schema.items,`${path}[${i}]`)))
 }else if(schema.type==='integer'||schema.type==='number'){
  if(typeof value!=='number'||!Number.isFinite(value)||(schema.type==='integer'&&!Number.isSafeInteger(value)))errors.push(`${path}必须是${schema.type==='integer'?'安全整数':'数字'}`)
  if(schema.minimum!=null&&value<schema.minimum)errors.push(`${path}不能小于${schema.minimum}`)
  if(schema.maximum!=null&&value>schema.maximum)errors.push(`${path}不能大于${schema.maximum}`)
 }else if(schema.type==='string'){
  if(typeof value!=='string')return [`${path}必须是字符串`]
  if(schema.maxLength&&value.length>schema.maxLength)errors.push(`${path}不能超过${schema.maxLength}个字符`)
  if(schema.minLength&&value.length<schema.minLength)errors.push(`${path}至少${schema.minLength}个字符`)
  if(schema.pattern&&!new RegExp(schema.pattern).test(value))errors.push(`${path}格式不正确`)
 }
 if(schema.enum&&!schema.enum.includes(value))errors.push(`${path}不在允许的取值中`)
 return errors
}
export function safeExample(value){if(Array.isArray(value))return value.map(safeExample);if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([k,v])=>[k,/^(operatorId|userId|deviceId|orderId|activityId|recordId|imageId|maintainerId)$/.test(k)?null:k==='imageIds'?[]:safeExample(v)]));return value}

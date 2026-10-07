import ticketGroups from './ticket/groups.json' with { type: 'json' }
import repairGroups from './repair/groups.json' with { type: 'json' }

// 业务分组由控制器方法清单生成，不根据相似的路径名推测归属。
const definitions = {
  ticket: ticketGroups,
  repair: repairGroups,
  system: [
    { id: 'session', name: '登录与服务', description: '登录、退出与服务连通检查', matches: path => !path.startsWith('/api/') },
    { id: 'classes', name: '班级管理', description: '班级查询、维护与启停', matches: path => path.startsWith('/api/sys-class/') },
    { id: 'users', name: '系统用户', description: '账号维护与学生导入', matches: path => path.startsWith('/api/sys-user/') },
    { id: 'workspaces', name: '工作空间', description: '空间查询、启停、初始化与重置', matches: path => path.startsWith('/api/sys-workspace/') },
  ],
}

export function groupOperations(operations, project) {
  const groups = (definitions[project] || []).map(group => ({ ...group, key: `${project}-${group.id}`, operations: [] }))
  for (const operation of operations) {
    if (operation.project !== project) continue
    const group = groups.find(item => project === 'system' ? item.matches(operation.path) : item.id === operation.group)
    if (!group) throw new Error(`接口未配置功能目录：${operation.operationId}`)
    group.operations.push(operation)
  }
  return groups.filter(group => group.operations.length)
}

import request from '@/config/axios'

export interface WecomSyncRun {
  id: number
  departmentCount: number
  userCount: number
  createTime: string
}

export interface WecomDepartment {
  wecomDeptId: number
  systemDeptId?: number
  parentWecomDeptId: number
  name: string
  leaderUserIds?: string
  sort: number
  lastSeenAt: string
}

export interface WecomUser {
  wecomUserId: string
  name: string
  departmentIds: string
  hasMobile: boolean
  hasEmail: boolean
  hasSex: boolean
  systemUserId?: number
  lastSeenAt: string
}

export const getLatest = () => request.get<WecomSyncRun>({ url: '/system/wecom-sync/latest' })
export const getDepartments = () =>
  request.get<WecomDepartment[]>({ url: '/system/wecom-sync/departments' })
export const getUsers = () => request.get<WecomUser[]>({ url: '/system/wecom-sync/users' })
export const executeSync = () => request.post<WecomSyncRun>({ url: '/system/wecom-sync/execute' })
export const applySync = () => request.post<{
  createdDepartments: number
  updatedDepartments: number
  createdUsers: number
  updatedUsers: number
}>({ url: '/system/wecom-sync/apply' })

export const linkUser = (data: { wecomUserId: string; systemUserId: number }) =>
  request.put({ url: '/system/wecom-sync/link', data })

export const linkDepartment = (data: { wecomDeptId: number; systemDeptId: number }) =>
  request.put({ url: '/system/wecom-sync/link-department', data })


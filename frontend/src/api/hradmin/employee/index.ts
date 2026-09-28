import request from '@/config/axios'

/** 人事任职状态独立于系统账号状态。 */
export interface EmployeeArchive {
  id?: number
  employeeNo: string
  userId?: number
  name: string
  deptId?: number
  positionName?: string
  managerUserId?: number
  hireDate?: string
  probationEndDate?: string
  contractEndDate?: string
  projectName?: string
  contractStatus?: number
  socialStatus?: number
  socialReason?: string
  employmentStatus: number
  remark?: string
  createTime?: string
  accountName?: string
  accountDeptId?: number
  accountStatus?: number
}

export const getEmployeePage = (params: PageParam) =>
  request.get<{ list: EmployeeArchive[]; total: number }>({ url: '/hradmin/employee/page', params })
export interface EmployeeOverview {
  total: number
  active: number
  pending: number
  probation: number
  attention: number
  dueSoon: number
}
export const getEmployeeOverview = () =>
  request.get<EmployeeOverview>({ url: '/hradmin/employee/overview' })
export const getEmployee = (id: number) =>
  request.get<EmployeeArchive>({ url: '/hradmin/employee/get', params: { id } })
export const createEmployee = (data: EmployeeArchive) =>
  request.post<number>({ url: '/hradmin/employee/create', data })
export const updateEmployee = (data: EmployeeArchive) =>
  request.put<boolean>({ url: '/hradmin/employee/update', data })

export const confirmArrival = (id: number, userId: number) =>
  request.post<boolean>({ url: '/hradmin/employee/confirm-arrival', params: { id, userId } })

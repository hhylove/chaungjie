import request from '@/config/axios'

/** 人事任职状态独立于系统账号状态。 */
export interface EmployeeArchive {
  id?: number
  employeeNo?: string
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
  agreedMonthlySalary?: number
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
export const getEmployeeSalary = (employeeId: number) =>
  request.get<{ agreedMonthlySalary: number; status: string }>({ url: '/hradmin/employee/salary', params: { employeeId } })
export const createEmployee = (data: EmployeeArchive) =>
  request.post<number>({ url: '/hradmin/employee/create', data })
export const createExistingEmployee = (data: EmployeeArchive) =>
  request.post<number>({ url: '/hradmin/employee/create-existing', data })
export const updateEmployee = (data: EmployeeArchive) =>
  request.put<boolean>({ url: '/hradmin/employee/update', data })

export interface OnboardingTask {
  id: number
  employeeId: number
  stageNo: number
  sequenceNo: number
  stage: string
  taskKey: string
  title: string
  owner: string
  dueDate: string
  doneAt?: string
  evidence?: string
  actorUserId?: number
}
export const getOnboarding = (employeeId: number) =>
  request.get<OnboardingTask[]>({ url: '/hradmin/employee/onboarding', params: { employeeId } })
export const getMyArchive = () => request.get<EmployeeArchive>({ url: '/hradmin/employee/mine' })
export const getMyOnboarding = () => request.get<OnboardingTask[]>({ url: '/hradmin/employee/onboarding/mine' })
export const completeOnboarding = (data: { employeeId: number; taskKey: string; evidence: string; userId?: number; socialStatus?: '已参保' }) =>
  request.post<boolean>({ url: '/hradmin/employee/onboarding/complete', data })
export const activateExistingAccount = (data: { employeeId: number; userId: number; evidence: string }) =>
  request.post<boolean>({ url: '/hradmin/employee/activate-existing', data })

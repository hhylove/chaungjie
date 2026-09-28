import request from '@/config/axios'
import type { EmployeeArchive } from '@/api/hradmin/employee'

export interface HiringRequest {
  id: number
  job: string
  deptId: number
  count: number
  reason: string
  status: string
  reviewNote?: string
}

export interface Candidate {
  id: number
  requestId: number
  name: string
  stage: string
  latestNote?: string
  employeeId?: number
}

export interface CandidateHistory {
  id: number
  stage: string
  note: string
  actorUserId: number
  createTime: string
}

export const getRequests = () => request.get<HiringRequest[]>({ url: '/hradmin/recruitment/requests' })
export const getCandidates = () => request.get<Candidate[]>({ url: '/hradmin/recruitment/candidates' })
export const getHistory = (candidateId: number) => request.get<CandidateHistory[]>({ url: '/hradmin/recruitment/history', params: { candidateId } })
export const submitRequest = (data: Pick<HiringRequest, 'job' | 'deptId' | 'count' | 'reason'>) => request.post<number>({ url: '/hradmin/recruitment/submit', data })
export const reviewRequest = (data: { id: number; approved: boolean; note: string }) => request.post<boolean>({ url: '/hradmin/recruitment/review', data })
export const addCandidate = (data: { requestId: number; name: string }) => request.post<number>({ url: '/hradmin/recruitment/candidate', data })
export const advanceCandidate = (data: { id: number; expectedStage: string; note: string }) => request.post<boolean>({ url: '/hradmin/recruitment/advance', data })
export const convertCandidate = (data: { id: number; archive: EmployeeArchive }) => request.post<number>({ url: '/hradmin/recruitment/convert', data })

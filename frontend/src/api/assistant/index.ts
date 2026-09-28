import request from '@/config/axios'

export interface AssistantConfig {
  tenantId: number
  enabled: boolean
  modelBaseUrl: string | null
  modelName: string | null
  retentionDays: number
  requestsPerMinute: number
}

export interface AssistantUser {
  id: number
  username: string
  nickname: string
  deptId: number | null
  status: number
  enabled: boolean
}

export interface AssistantAnswer {
  conversationId: number
  answer: string
}

export interface AssistantConversation {
  id: number
  title: string
  create_time: string
  update_time: string
}

export interface AssistantMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  create_time: string
}

export const getOverview = (): Promise<Record<string, number | boolean | string>> =>
  request.get({ url: '/assistant/admin/overview' })

export const getConfig = (): Promise<AssistantConfig> =>
  request.get({ url: '/assistant/admin/config' })

export const saveConfig = (data: Omit<AssistantConfig, 'tenantId'>): Promise<boolean> =>
  request.put({ url: '/assistant/admin/config', data })

export const testModel = (): Promise<string> =>
  request.post({ url: '/assistant/admin/model/test' })

export const getUsers = (params: { pageNo: number; pageSize: number; username?: string; deptId?: number }): Promise<{ list: AssistantUser[]; total: number }> =>
  request.get({ url: '/assistant/admin/users', params })

export const setUserGrants = (userIds: number[], enabled: boolean): Promise<boolean> =>
  request.put({ url: '/assistant/admin/users/grant', data: { userIds, enabled } })

export const getAudit = (limit = 50): Promise<Record<string, unknown>[]> =>
  request.get({ url: '/assistant/admin/audit', params: { limit } })

export const getAvailability = (): Promise<boolean> =>
  request.get({ url: '/assistant/chat/availability' })

export const ask = (question: string, conversationId?: number): Promise<AssistantAnswer> =>
  request.post({ url: '/assistant/chat/ask', data: { question, conversationId } })

export const getConversations = (): Promise<AssistantConversation[]> =>
  request.get({ url: '/assistant/chat/conversations' })

export const getMessages = (conversationId: number): Promise<AssistantMessage[]> =>
  request.get({ url: '/assistant/chat/messages', params: { conversationId } })

export interface KnowledgeBase {
  id: number
  name: string
  description: string | null
  visibility: 'all' | 'restricted'
  enabled: boolean
  document_count: number
}

export interface KnowledgeDocument {
  id: number
  knowledge_base_id: number
  file_name: string
  version: number
  enabled: boolean
  index_status: string
  update_time: number
}

const knowledgeUrl = '/assistant/admin/knowledge'
export const getEmbeddingConfig = (): Promise<{ baseUrl: string | null; modelName: string | null }> =>
  request.get({ url: `${knowledgeUrl}/embedding-config` })
export const saveEmbeddingConfig = (data: { baseUrl: string; modelName: string }): Promise<boolean> =>
  request.put({ url: `${knowledgeUrl}/embedding-config`, data })
export const testEmbedding = (): Promise<string> => request.post({ url: `${knowledgeUrl}/embedding-test` })
export const getKnowledgeBases = (): Promise<KnowledgeBase[]> => request.get({ url: `${knowledgeUrl}/bases` })
export const createKnowledgeBase = (data: Omit<KnowledgeBase, 'id' | 'document_count'>): Promise<number> =>
  request.post({ url: `${knowledgeUrl}/bases`, data })
export const updateKnowledgeBase = (data: Omit<KnowledgeBase, 'document_count'>): Promise<boolean> =>
  request.put({ url: `${knowledgeUrl}/bases/${data.id}`, data })
export const getKnowledgeGrants = (id: number): Promise<Array<{ principal_type: string; principal_id: number }>> =>
  request.get({ url: `${knowledgeUrl}/bases/${id}/grants` })
export const saveKnowledgeGrants = (id: number, userIds: number[], deptIds: number[], roleIds: number[]): Promise<boolean> =>
  request.put({ url: `${knowledgeUrl}/bases/${id}/grants`, data: { userIds, deptIds, roleIds } })
export const getKnowledgeDocuments = (id: number): Promise<KnowledgeDocument[]> =>
  request.get({ url: `${knowledgeUrl}/bases/${id}/documents` })
export const uploadKnowledgeDocument = (id: number, file: File, documentId?: number): Promise<number> => {
  const data = new FormData()
  data.append('file', file)
  if (documentId) data.append('documentId', String(documentId))
  return request.post({ url: `${knowledgeUrl}/bases/${id}/documents`, data, headersType: 'multipart/form-data' })
}
export const setKnowledgeDocumentEnabled = (id: number, documentId: number, enabled: boolean): Promise<boolean> =>
  request.put({ url: `${knowledgeUrl}/bases/${id}/documents/${documentId}/enabled`, params: { enabled } })
export const deleteKnowledgeDocument = (id: number, documentId: number): Promise<boolean> =>
  request.delete({ url: `${knowledgeUrl}/bases/${id}/documents/${documentId}` })

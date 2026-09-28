<template>
  <ContentWrap title="独立知识库">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="知识库与文档" name="knowledge">
        <el-alert type="info" :closable="false" class="mb-20px"
          title="文档仅供已授权员工检索。员工、部门和角色的权限变更会在下次查询时生效。" />
        <div class="flex items-center justify-between mb-16px">
          <span class="text-13px text-[var(--el-text-color-secondary)]">先创建知识库，再配置可见范围并上传文档</span>
          <el-button type="primary" @click="editBase()"><Icon icon="ep:plus" class="mr-5px" />新建知识库</el-button>
        </div>
        <el-table :data="bases" v-loading="loading" row-key="id" highlight-current-row
          :current-row-key="selectedBase?.id" @row-click="selectBase">
          <el-table-column prop="name" label="知识库" min-width="160" show-overflow-tooltip />
          <el-table-column prop="description" label="用途" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="可见范围" width="145">
            <template #default="{ row }"><el-tag :type="row.visibility === 'all' ? 'warning' : 'info'">
              {{ row.visibility === 'all' ? '本租户全员' : '指定人员' }}
            </el-tag></template>
          </el-table-column>
          <el-table-column prop="document_count" label="文档数" width="95" align="center" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="155" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="selectBase(row)">管理文档</el-button>
              <el-button link @click.stop="editBase(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedBase" class="detail-section">
          <div class="flex items-center justify-between mb-18px">
            <div>
              <div class="text-16px font-600">{{ selectedBase.name }}</div>
              <div class="text-12px text-[var(--el-text-color-secondary)] mt-4px">文档与可见范围</div>
            </div>
            <el-button @click="selectedBase = undefined">收起</el-button>
          </div>
          <el-alert v-if="selectedBase.visibility === 'all'" type="warning" :closable="false" class="mb-18px"
            title="当前知识库对本租户所有已开通员工可见；如需限制范围，请在知识库编辑中改为「指定人员」。" />
          <div v-else class="mb-24px">
            <div class="sub-title">可见范围</div>
            <el-row :gutter="16">
              <el-col :xs="24" :md="8" class="mb-12px">
                <div class="field-label">指定员工</div>
                <el-select v-model="userIds" multiple filterable collapse-tags clearable placeholder="选择员工" class="w-full">
                  <el-option v-for="user in users" :key="user.id" :label="`${user.nickname}（${user.username}）`" :value="user.id" />
                </el-select>
              </el-col>
              <el-col :xs="24" :md="8" class="mb-12px">
                <div class="field-label">部门</div>
                <el-select v-model="deptIds" multiple filterable collapse-tags clearable placeholder="选择部门" class="w-full">
                  <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
                </el-select>
              </el-col>
              <el-col :xs="24" :md="8" class="mb-12px">
                <div class="field-label">角色群组</div>
                <el-select v-model="roleIds" multiple filterable collapse-tags clearable placeholder="选择角色" class="w-full">
                  <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
                </el-select>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="grantSaving" @click="saveGrants">保存可见范围</el-button>
            <span class="text-12px text-[var(--el-text-color-placeholder)] ml-12px">未选任何对象时，员工无法检索此库</span>
          </div>

          <el-divider />
          <div class="flex items-center justify-between mb-16px">
            <div class="sub-title mb-0">文档</div>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".txt,.md,.pdf,.docx"
              :disabled="uploading" :on-change="(file) => upload(file.raw!)">
              <el-button type="primary" :loading="uploading"><Icon icon="ep:upload" class="mr-5px" />上传文档</el-button>
            </el-upload>
          </div>
          <div class="text-12px text-[var(--el-text-color-placeholder)] mb-12px">支持 TXT、Markdown、PDF、DOCX，单文件不超过 2 MB；扫描版 PDF 暂不支持识别。</div>
          <el-table :data="documents" v-loading="documentLoading" empty-text="暂无文档，请先上传">
            <el-table-column prop="file_name" label="文档" min-width="200" show-overflow-tooltip />
            <el-table-column label="版本" width="90"><template #default="{ row }">v{{ row.version }}</template></el-table-column>
            <el-table-column label="索引" width="100">
              <template #default="{ row }"><el-tag :type="row.index_status === 'ready' ? 'success' : 'warning'">
                {{ row.index_status === 'ready' ? '已完成' : row.index_status }}
              </el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="更新时间" width="180"><template #default="{ row }">{{ formatDate(row.update_time) }}</template></el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-upload class="inline-block" :auto-upload="false" :show-file-list="false" accept=".txt,.md,.pdf,.docx"
                  :disabled="uploading" :on-change="(file) => upload(file.raw!, row.id)">
                  <el-button link type="primary">更新</el-button>
                </el-upload>
                <el-button link @click="toggleDocument(row)">{{ row.enabled ? '停用' : '启用' }}</el-button>
                <el-button link type="danger" @click="removeDocument(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="向量模型" name="embedding">
        <el-alert type="info" :closable="false" class="mb-20px"
          title="向量模型用于检索文档，与全局配置中的回答模型分开。两者都必须部署在内网。" />
        <el-form label-width="145px" class="max-w-700px">
          <el-form-item label="内网服务地址">
            <el-input v-model="embedding.baseUrl" placeholder="http://127.0.0.1:8000/v1" />
            <div class="text-12px text-[var(--el-text-color-placeholder)]">填写兼容 /embeddings 接口的 /v1 根地址</div>
          </el-form-item>
          <el-form-item label="向量模型标识"><el-input v-model="embedding.modelName" placeholder="服务端暴露的向量模型名称" /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="embeddingSaving" @click="saveEmbedding">保存配置</el-button>
            <el-button :loading="embeddingTesting" :disabled="!savedEmbeddingReady" @click="checkEmbedding">测试已保存配置</el-button>
          </el-form-item>
        </el-form>
        <el-alert type="warning" :closable="false" class="max-w-700px"
          title="已有文档索引时，更换向量模型需先处理旧文档并重新建立索引。" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-dialog v-model="dialogOpen" :title="form.id ? '编辑知识库' : '新建知识库'" width="520px">
    <el-form label-width="90px">
      <el-form-item label="名称" required><el-input v-model="form.name" maxlength="120" placeholder="例如：员工手册" /></el-form-item>
      <el-form-item label="用途"><el-input v-model="form.description" type="textarea" maxlength="500" :rows="3" placeholder="这份知识库包含哪些内容" /></el-form-item>
      <el-form-item label="可见范围"><el-select v-model="form.visibility" class="w-full">
        <el-option label="指定员工、部门或角色" value="restricted" /><el-option label="本租户全员" value="all" />
      </el-select></el-form-item>
      <el-form-item v-if="form.id" label="启用"><el-switch v-model="form.enabled" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="baseSaving" @click="saveBase">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import * as RoleApi from '@/api/system/role'
import { formatDate } from '@/utils/formatTime'
import {
  getEmbeddingConfig, saveEmbeddingConfig, testEmbedding, getKnowledgeBases, createKnowledgeBase,
  updateKnowledgeBase, getKnowledgeGrants, saveKnowledgeGrants, getKnowledgeDocuments,
  uploadKnowledgeDocument, setKnowledgeDocumentEnabled, deleteKnowledgeDocument,
  type KnowledgeBase, type KnowledgeDocument
} from '@/api/assistant'

defineOptions({ name: 'AssistantKnowledge' })
const message = useMessage()
const activeTab = ref('knowledge')
const embedding = reactive({ baseUrl: '', modelName: '' })
const savedEmbedding = ref({ baseUrl: '', modelName: '' })
const savedEmbeddingReady = computed(() => !!savedEmbedding.value.baseUrl && !!savedEmbedding.value.modelName &&
  embedding.baseUrl === savedEmbedding.value.baseUrl && embedding.modelName === savedEmbedding.value.modelName)
const bases = ref<KnowledgeBase[]>([])
const selectedBase = ref<KnowledgeBase>()
const documents = ref<KnowledgeDocument[]>([])
const users = ref<UserApi.UserVO[]>([])
const departments = ref<DeptApi.DeptVO[]>([])
const roles = ref<RoleApi.RoleVO[]>([])
const userIds = ref<number[]>([])
const deptIds = ref<number[]>([])
const roleIds = ref<number[]>([])
const loading = ref(false)
const documentLoading = ref(false)
const uploading = ref(false)
const grantSaving = ref(false)
const embeddingSaving = ref(false)
const embeddingTesting = ref(false)
const baseSaving = ref(false)
const dialogOpen = ref(false)
const form = reactive({ id: 0, name: '', description: '', visibility: 'restricted' as 'all' | 'restricted', enabled: true })

const loadBases = async () => {
  loading.value = true
  try {
    bases.value = await getKnowledgeBases()
    if (selectedBase.value) selectedBase.value = bases.value.find((item) => item.id === selectedBase.value?.id)
  } finally { loading.value = false }
}
const selectBase = async (base: KnowledgeBase) => {
  selectedBase.value = base
  documentLoading.value = true
  try {
    const [docs, grants] = await Promise.all([getKnowledgeDocuments(base.id), getKnowledgeGrants(base.id)])
    if (selectedBase.value?.id !== base.id) return
    documents.value = docs
    userIds.value = grants.filter((g) => g.principal_type === 'user').map((g) => g.principal_id)
    deptIds.value = grants.filter((g) => g.principal_type === 'dept').map((g) => g.principal_id)
    roleIds.value = grants.filter((g) => g.principal_type === 'role').map((g) => g.principal_id)
  } finally { documentLoading.value = false }
}
const editBase = (base?: KnowledgeBase) => {
  Object.assign(form, base ? { id: base.id, name: base.name, description: base.description || '',
    visibility: base.visibility, enabled: base.enabled } : { id: 0, name: '', description: '', visibility: 'restricted', enabled: true })
  dialogOpen.value = true
}
const saveBase = async () => {
  if (!form.name.trim()) return message.error('请输入知识库名称')
  baseSaving.value = true
  try {
    if (form.id) await updateKnowledgeBase({ ...form })
    else await createKnowledgeBase({ ...form })
    dialogOpen.value = false
    await loadBases()
    message.success('知识库已保存')
  } finally { baseSaving.value = false }
}
const saveEmbedding = async () => {
  embeddingSaving.value = true
  try {
    await saveEmbeddingConfig({ ...embedding })
    savedEmbedding.value = { ...embedding }
    message.success('向量模型配置已保存')
  } finally { embeddingSaving.value = false }
}
const checkEmbedding = async () => {
  embeddingTesting.value = true
  try { await testEmbedding(); message.success('向量模型连接成功') } finally { embeddingTesting.value = false }
}
const saveGrants = async () => {
  if (!selectedBase.value) return
  grantSaving.value = true
  try {
    await saveKnowledgeGrants(selectedBase.value.id, userIds.value, deptIds.value, roleIds.value)
    message.success('可见范围已保存')
  } finally { grantSaving.value = false }
}
const upload = async (file: File, documentId?: number) => {
  if (!selectedBase.value || !file) return
  if (file.size > 2_000_000) return message.error('文件不能超过 2 MB')
  uploading.value = true
  try {
    await uploadKnowledgeDocument(selectedBase.value.id, file, documentId)
    documents.value = await getKnowledgeDocuments(selectedBase.value.id)
    await loadBases()
    message.success('文档已解析并建立索引')
  } finally { uploading.value = false }
}
const toggleDocument = async (doc: KnowledgeDocument) => {
  if (!selectedBase.value) return
  await setKnowledgeDocumentEnabled(selectedBase.value.id, doc.id, !doc.enabled)
  documents.value = await getKnowledgeDocuments(selectedBase.value.id)
}
const removeDocument = async (doc: KnowledgeDocument) => {
  if (!selectedBase.value) return
  try { await message.confirm(`确定删除文档「${doc.file_name}」及其索引吗？`) } catch { return }
  await deleteKnowledgeDocument(selectedBase.value.id, doc.id)
  documents.value = await getKnowledgeDocuments(selectedBase.value.id)
  await loadBases()
}
onMounted(async () => {
  const [config, userList, deptList, roleList] = await Promise.all([
    getEmbeddingConfig(), UserApi.getSimpleUserList(), DeptApi.getSimpleDeptList(), RoleApi.getSimpleRoleList()
  ])
  embedding.baseUrl = config.baseUrl || ''
  embedding.modelName = config.modelName || ''
  savedEmbedding.value = { ...embedding }
  users.value = userList
  departments.value = deptList
  roles.value = roleList
  await loadBases()
})
</script>

<style scoped>
.detail-section { margin-top: 24px; padding-top: 24px; border-top: 1px solid var(--el-border-color-light); }
.sub-title { margin-bottom: 16px; font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); }
.field-label { margin-bottom: 8px; font-size: 13px; color: var(--el-text-color-regular); }
</style>

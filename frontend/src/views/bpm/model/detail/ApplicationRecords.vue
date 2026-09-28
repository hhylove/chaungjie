<template>
  <div class="application-records" v-loading="loading">
    <template v-if="deployed">
      <!-- 筛选 -->
      <el-form :model="queryParams" class="filter-bar" inline size="small" @submit.prevent>
        <el-form-item label="提交">
          <el-date-picker
            v-model="queryParams.createTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            start-placeholder="开始"
            end-placeholder="结束"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            class="filter-date"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item label="完成">
          <el-date-picker
            v-model="queryParams.endTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            start-placeholder="开始"
            end-placeholder="结束"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            class="filter-date"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryParams.status"
            placeholder="审批状态"
            clearable
            class="filter-control"
            @change="handleQuery"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-tree-select
            v-model="deptId"
            :data="deptList"
            :props="deptTreeProps"
            node-key="id"
            check-strictly
            clearable
            filterable
            placeholder="部门"
            class="filter-control"
            @change="handleDeptChange"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryParams.startUserId"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userLoading"
            placeholder="申请人"
            class="filter-control"
            @change="handleQuery"
          >
            <el-option
              v-for="user in filteredUserList"
              :key="user.id"
              :label="user.nickname"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="queryParams.processInstanceId"
            placeholder="审批编号"
            clearable
            class="filter-control"
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button :loading="exportLoading" :disabled="total === 0" @click="handleExport">
            导出
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <div ref="tableWrapRef" class="records-scroll">
        <el-table
          :data="list"
          :height="tableHeight"
          size="small"
          class="records-table"
          @row-click="handleDetail"
        >
        <el-table-column label="审批编号" prop="id" min-width="200" show-overflow-tooltip />
        <el-table-column label="提交时间" prop="startTime" width="170" :formatter="dateFormatter" />
        <el-table-column label="完成时间" prop="endTime" width="170">
          <template #default="{ row }">
            {{ row.endTime ? formatDate(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="申请人和部门" min-width="200">
          <template #default="{ row }">
            <span class="applicant">
              {{ row.startUser?.nickname || '-' }}
              <template v-if="row.startUser?.deptName"> / {{ row.startUser.deptName }}</template>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" prop="status" width="120">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="附件/签名" min-width="220" align="center">
          <template #default="{ row }">
            <TaskEvidenceCell :attachments="row.attachments" :sign-pic-urls="row.signPicUrls" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrap">
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </template>

    <el-empty v-else description="流程尚未发布，暂无申请记录" :image-size="80" />
  </div>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import download from '@/utils/download'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import * as UserApi from '@/api/system/user'
import * as DeptApi from '@/api/system/dept'
import { handleTree } from '@/utils/tree'
import TaskEvidenceCell from '@/views/bpm/task/components/TaskEvidenceCell.vue'

defineOptions({ name: 'BpmModelApplicationRecords' })

const props = defineProps<{
  processDefinitionKey?: string
  deployed?: boolean
  modelName?: string
}>()

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const exportLoading = ref(false)
const userLoading = ref(false)
const total = ref(0)
const list = ref<any[]>([])
const userList = ref<UserApi.UserVO[]>([])
const deptList = ref<any[]>([])
const deptId = ref<number>()
const tableWrapRef = ref<HTMLElement>()
const tableHeight = ref(280)
let requestSeq = 0
let userRequestSeq = 0
let filtersLoaded = false
let tableResizeObserver: ResizeObserver | undefined

const deptTreeProps = {
  label: 'name',
  value: 'id',
  children: 'children'
}

const defaultCreateTime = (): string[] => [
  dayjs().subtract(30, 'day').startOf('day').format('YYYY-MM-DD HH:mm:ss'),
  dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')
]

const queryParams = reactive({
  pageNo: 1,
  pageSize: 50,
  startUserId: undefined as number | undefined,
  status: undefined as number | undefined,
  processInstanceId: '',
  processDefinitionKey: undefined as string | undefined,
  createTime: defaultCreateTime() as string[],
  endTime: [] as string[]
})

const filteredUserList = computed(() => {
  if (!deptId.value) return userList.value
  return userList.value.filter((u) => u.deptId === deptId.value)
})

const buildQueryParams = () => ({
  ...queryParams,
  pageNo: queryParams.pageNo,
  processDefinitionKey: props.processDefinitionKey,
  processInstanceId: queryParams.processInstanceId.trim() || undefined,
  endTime: queryParams.endTime?.length ? queryParams.endTime : undefined
})

const getList = async () => {
  if (!props.deployed || !props.processDefinitionKey) {
    list.value = []
    total.value = 0
    return
  }
  const seq = ++requestSeq
  loading.value = true
  try {
    const data = await ProcessInstanceApi.getProcessInstanceManagerPage(buildQueryParams())
    if (seq !== requestSeq) return
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleDeptChange = () => {
  const hadSelectedUser = Boolean(queryParams.startUserId)
  queryParams.startUserId = undefined
  userList.value = []
  userRequestSeq++
  if (hadSelectedUser) handleQuery()
}

const searchUsers = async (keyword: string) => {
  const normalizedKeyword = keyword.trim()
  if (normalizedKeyword.length < 2) {
    userList.value = []
    userRequestSeq++
    return
  }
  const seq = ++userRequestSeq
  userLoading.value = true
  try {
    const users = await UserApi.getSimpleUserListByNickname(normalizedKeyword)
    if (seq !== userRequestSeq) return
    userList.value = (users || []).filter((user) => user.status === undefined || user.status === 0)
  } finally {
    if (seq === userRequestSeq) userLoading.value = false
  }
}

const handleDetail = (row: any) => {
  router.push({
    name: 'BpmProcessInstanceDetail',
    query: { id: row.id }
  })
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
  } catch {
    return
  }
  exportLoading.value = true
  try {
    const data = await ProcessInstanceApi.exportProcessInstanceManagerExcel({
      ...buildQueryParams(),
      pageNo: 1
    })
    const safeModelName = (props.modelName || '申请记录').replace(/[\\/:*?"<>|]/g, '_')
    download.excel(data, `${safeModelName}_${dayjs().format('YYYYMMDD')}.xls`)
  } finally {
    exportLoading.value = false
  }
}

const bindTableHeight = async () => {
  await nextTick()
  const wrap = tableWrapRef.value
  if (!wrap) return
  tableResizeObserver?.disconnect()
  const update = () => {
    tableHeight.value = wrap.clientHeight || 280
  }
  tableResizeObserver = new ResizeObserver(update)
  tableResizeObserver.observe(wrap)
  update()
}

const ensureFilterOptions = async () => {
  if (filtersLoaded) return
  const depts = await DeptApi.getSimpleDeptList()
  deptList.value = handleTree(depts)
  filtersLoaded = true
}

watch(
  () => [props.processDefinitionKey, props.deployed] as const,
  async () => {
    if (!props.deployed) {
      list.value = []
      total.value = 0
      return
    }
    await ensureFilterOptions()
    await bindTableHeight()
    handleQuery()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  requestSeq++
  userRequestSeq++
  tableResizeObserver?.disconnect()
})
</script>

<style lang="scss" scoped>
.application-records {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 4px 0 0;
}

.filter-bar {
  flex-shrink: 0;
  margin-bottom: 6px;

  :deep(.el-form-item) {
    margin-right: 8px;
    margin-bottom: 6px;
  }

  :deep(.el-form-item__label) {
    height: 24px;
    padding-right: 6px;
    color: #646a73;
    font-size: 12px;
    font-weight: 400;
    line-height: 24px;
  }
}

.filter-date {
  width: 210px;
}

.filter-control {
  width: 140px;
}

.records-scroll {
  flex: 1;
  min-height: 180px;
  overflow: hidden;
}

.records-table {
  width: 100%;

  :deep(.el-table__row) {
    cursor: pointer;
  }
}

.applicant {
  color: #1f2329;
}

.pagination-wrap {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: flex-end;
  height: 40px;
  margin-top: auto;
  border-top: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(255, 255, 255, 0.72);

  :deep(.el-pagination) {
    float: none !important;
    margin: 0 !important;
  }
}

@media (max-width: 960px) {
  .filter-date,
  .filter-control {
    width: 160px;
  }
}
</style>

<template>
  <div class="hr-workspace">
    <div v-if="!canManage" class="workspace-inner">
      <h1>我的入职事项</h1>
      <OnboardingPanel v-if="myArchive" :employee="myArchive" own />
      <el-empty v-else description="暂无已关联的员工档案" />
    </div>
    <div v-if="canManage" class="workspace-intro">
      <span class="eyebrow">CHUANGJIE / HR</span>
      <h1>人事行政管理</h1>
      <p>人员、招聘、流程、制度与档案、行政物资仓库</p>
    </div>
    <div v-if="canManage" class="workspace-body">
      <nav class="workspace-nav" aria-label="人事行政业务">
        <button :class="{ active: page === '人员' }" type="button" @click="page = '人员'">人员</button>
        <button :class="{ active: page === '招聘' }" type="button" v-if="checkPermi(['hradmin:recruitment:query'])" @click="page = '招聘'">招聘</button>
        <button type="button" disabled title="人事流程尚未接入后端">流程</button>
        <button type="button" disabled title="制度档案尚未接入后端">制度与档案</button>
        <button type="button" disabled title="行政物资尚未接入后端">行政物资仓库</button>
      </nav>
      <RecruitmentPanel v-if="page === '招聘'" :departments="departments" @converted="onConverted" />
      <div v-else class="workspace-inner">
        <div class="heading-row">
          <div>
            <span class="eyebrow">PEOPLE & OPERATIONS</span>
            <h2>人员总览</h2>
            <p>先处理需要关注的事，再看每个人的状态。</p>
          </div>
          <el-button v-if="checkPermi(['hradmin:employee:create']) && checkPermi(['hradmin:salary:write'])" type="primary" @click="openCreate">＋ 新增人员</el-button>
        </div>
        <div class="stat-grid">
          <button v-for="card in statCards" :key="card.label" type="button" class="stat-card" @click="applyStat(card.filter)">
            <span>{{ card.label }}</span><strong>{{ card.value }}<small>人</small></strong><span class="stat-arrow">↗</span>
          </button>
        </div>
        <div class="section-head"><h3>今天，先关注这些</h3><span>根据已登记的档案计算</span></div>
        <div class="attention-grid">
          <button type="button" class="attention-card risk" @click="applyStat('attention')">
            <span>!　异常需处理</span><strong>{{ overview.attention }}<small>人</small></strong><p>合同到期或已登记未参保</p><em>查看人员 →</em>
          </button>
          <div class="attention-card"><span>✓　今日待办</span><strong>—</strong><p>流程数据接入后显示</p></div>
          <div class="attention-card"><span>◷　未来 7 天到期</span><strong>{{ overview.dueSoon }}<small>人</small></strong><p>劳动合同到期提醒</p></div>
        </div>
        <div class="roster-panel">
          <div class="roster-heading"><div><h3>全员名册 <small>{{ total }}</small></h3><p>点击姓名查看员工档案</p></div><el-button v-if="activeQuickFilter" text @click="applyStat('')">清除快捷筛选 ×</el-button></div>
          <el-form :model="query" inline class="roster-filters">
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable placeholder="搜索姓名" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="所属中心">
        <el-select v-model="query.deptId" clearable filterable class="!w-170px" @change="search">
          <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="任职状态">
        <el-select v-model="query.employmentStatus" clearable class="!w-150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" v-bind="item" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="search">查询</el-button>
      </el-form-item>
    </el-form>

          <div class="roster-table">
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="姓名 / 所属中心" min-width="170">
        <template #default="{ row }"><button class="person-link" type="button" @click="openDetail(row)"><strong>{{ row.name }}</strong><small>{{ deptName(row.accountDeptId || row.deptId) }}</small></button></template>
      </el-table-column>
      <el-table-column label="职务 / 项目" min-width="160"><template #default="{ row }"><strong>{{ row.positionName || '未登记' }}</strong><br /><small>{{ row.projectName || '—' }}</small></template></el-table-column>
      <el-table-column label="任职状态" min-width="110">
        <template #default="{ row }">{{ displayStatus(row) }}</template>
      </el-table-column>
      <el-table-column prop="hireDate" label="入职日期" min-width="120" />
      <el-table-column label="劳动合同" min-width="110"><template #default="{ row }">{{ row.contractStatus == null ? '未登记' : row.contractStatus === 1 ? '已签署' : '待签署' }}</template></el-table-column>
      <el-table-column label="社保" min-width="110"><template #default="{ row }">{{ row.socialStatus == null ? '未登记' : row.socialStatus === 1 ? '已参保' : '未参保' }}</template></el-table-column>
      <el-table-column label="制度签收" min-width="100">—</el-table-column>
      <el-table-column label="培训" min-width="80">—</el-table-column>
      <el-table-column label="转正" min-width="110"><template #default="{ row }">{{ probationLabel(row) }}</template></el-table-column>
      <el-table-column label="当前异常" min-width="150"><template #default="{ row }"><span :class="{ 'risk-text': employeeRisk(row) }">{{ employeeRisk(row) || '暂无已登记异常' }}</span></template></el-table-column>
      <el-table-column label="操作" width="255" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['hradmin:employee:update']"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="
              row.userId &&
              row.accountStatus != null &&
              checkPermi(['system:permission:assign-user-role'])
            "
            link
            type="primary"
            @click="openRoleForm(row)"
          >
            配置角色
          </el-button>
        </template>
      </el-table-column>
    </el-table>
          </div>
          <Pagination :total="total" v-model:page="query.pageNo" v-model:limit="query.pageSize" @pagination="load" />
        </div>
      </div>
    </div>
  </div>

  <Dialog v-model="formVisible" :title="form.id ? '编辑人员档案' : '新增人员'">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="105px" v-loading="saving">
      <el-form-item v-if="!form.id" label="录入类型">
        <el-radio-group v-model="createMode">
          <el-radio value="pending">待入职人员</el-radio>
          <el-radio value="existing">原有在职人员</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-alert v-if="!form.id" :title="createMode === 'existing' ? '记录原有员工的实际入职日期和当前情况，不生成招聘或入职流程。' : '建立待入职档案；本人实际到岗后再单独开通权限。'" type="info" :closable="false" class="mb-4" />
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="部门">
        <el-input v-if="form.userId" :model-value="deptName(selectedAccountDeptId)" disabled />
        <el-select v-else v-model="form.deptId" clearable filterable class="w-full">
          <el-option
            v-for="dept in departments"
            :key="dept.id"
            :label="dept.name"
            :value="dept.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位">
        <el-input v-model="form.positionName" maxlength="100" />
      </el-form-item>
      <el-form-item label="所属项目"><el-input v-model="form.projectName" maxlength="100" /></el-form-item>
      <el-form-item label="直属负责人">
        <el-select v-model="form.managerUserId" clearable filterable class="w-full">
          <el-option
            v-for="user in accounts"
            :key="user.id"
            :label="`${user.nickname}（ID ${user.id}）`"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="createMode === 'existing' && !form.id ? '实际入职日' : '约定到岗日'" prop="hireDate">
        <el-date-picker
          v-model="form.hireDate"
          type="date"
          value-format="YYYY-MM-DD"
          class="w-full"
          :disabled="!!form.id && editingOnboarding"
        />
      </el-form-item>
      <el-form-item label="试用期结束"><el-date-picker v-model="form.probationEndDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item>
      <el-form-item label="合同到期"><el-date-picker v-model="form.contractEndDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item>
      <el-form-item v-if="!form.id" label="约定月薪（元）"><el-input-number v-model="form.agreedMonthlySalary" :min="0.01" :precision="2" class="w-full" /></el-form-item>
      <el-form-item v-if="form.id && !editingOnboarding" label="劳动合同"><el-select v-model="form.contractStatus" clearable class="w-full"><el-option label="待签署" :value="0" /><el-option label="已签署" :value="1" /></el-select></el-form-item>
      <el-form-item v-if="form.id && !editingOnboarding" label="社保状态"><el-select v-model="form.socialStatus" clearable class="w-full"><el-option label="未参保" :value="0" /><el-option label="已参保" :value="1" /></el-select></el-form-item>
      <el-form-item v-if="form.id && !editingOnboarding && form.socialStatus === 0" label="未参保说明"><el-input v-model="form.socialReason" type="textarea" maxlength="500" /></el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" maxlength="1000" show-word-limit />
      </el-form-item>
      <el-alert v-if="form.id && form.userId" title="角色授权由有权限的管理员另行办理。" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </Dialog>
  <el-drawer v-model="detailVisible" :title="`${detail?.accountName || detail?.name || ''} · 员工档案`" size="min(720px, 100%)">
    <template v-if="detail">
      <div class="employee-hero"><div class="employee-avatar">{{ detail.name.slice(-2) }}</div><div><h2>{{ detail.name }}</h2><p>{{ deptName(detail.accountDeptId || detail.deptId) }} · {{ detail.positionName || '岗位未登记' }}</p></div></div>
      <el-alert v-if="employeeRisk(detail)" :title="employeeRisk(detail)" type="warning" :closable="false" class="mb-4" />
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="basic"><el-descriptions :column="2" border><el-descriptions-item label="档案编号">{{ detail.id }}</el-descriptions-item><el-descriptions-item label="人员状态">{{ statusLabel(detail.employmentStatus) }}</el-descriptions-item><el-descriptions-item label="所属中心">{{ deptName(detail.accountDeptId || detail.deptId) }}</el-descriptions-item><el-descriptions-item label="所属项目">{{ detail.projectName || '未登记' }}</el-descriptions-item><el-descriptions-item label="岗位">{{ detail.positionName || '未登记' }}</el-descriptions-item><el-descriptions-item label="入职日期">{{ detail.hireDate || '未登记' }}</el-descriptions><OnboardingPanel v-if="onboardingEligible" :employee="detail" @changed="refreshDetail" /></el-tab-pane>
        <el-tab-pane label="任职薪酬" name="employment"><el-descriptions :column="1" border><el-descriptions-item label="岗位">{{ detail.positionName || '未登记' }}</el-descriptions-item><el-descriptions-item label="试用期结束">{{ detail.probationEndDate || '未登记' }}</el-descriptions-item><el-descriptions-item v-if="checkPermi(['hradmin:salary:read'])" label="约定月薪">{{ salary ? `¥ ${salary.agreedMonthlySalary} · ${salary.status}` : '未登记' }}</el-descriptions-item></el-descriptions></el-tab-pane>
        <el-tab-pane label="合同社保" name="contract"><el-descriptions :column="1" border><el-descriptions-item label="劳动合同">{{ detail.contractStatus == null ? '未登记' : detail.contractStatus === 1 ? '已签署' : '待签署' }}</el-descriptions-item><el-descriptions-item label="合同到期">{{ detail.contractEndDate || '未登记' }}</el-descriptions-item><el-descriptions-item label="社保">{{ detail.socialStatus == null ? '未登记' : detail.socialStatus === 1 ? '已参保' : '未参保' }}</el-descriptions-item><el-descriptions-item label="办理说明">{{ detail.socialReason || '—' }}</el-descriptions-item></el-descriptions></el-tab-pane>
        <el-tab-pane label="培训制度" name="training"><el-empty description="尚无培训与制度签收记录" /></el-tab-pane>
        <el-tab-pane label="资产权限" name="asset"><p>系统账号：{{ detail.userId ? '已关联' : '待开通' }}</p><el-button v-if="!onboardingEligible && detail.employmentStatus === 1 && !detail.userId && checkPermi(['hradmin:employee:update']) && checkPermi(['system:user:update'])" @click="openExistingActivation">核验并开通原有人员账号</el-button><el-empty description="尚无资产领用记录" /></el-tab-pane>
        <el-tab-pane label="人事记录" name="history"><el-empty description="人事办理记录尚未接入" /></el-tab-pane>
      </el-tabs>
    </template>
  </el-drawer>
  <Dialog v-model="existingActivationVisible" title="原有在职人员账号开通">
    <p>核验档案、本人身份及账号后开通；仅已在职且无入职流程的人员适用。</p>
    <el-select v-model="existingActivationUserId" filterable placeholder="选择已核验的禁用账号" class="w-full"><el-option v-for="user in disabledAccounts" :key="user.id" :label="user.nickname" :value="user.id" /></el-select>
    <el-input v-model="existingActivationEvidence" type="textarea" placeholder="填写身份核验和授权依据" class="mt-3" />
    <template #footer><el-button @click="existingActivationVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="activateExisting">确认开通</el-button></template>
  </Dialog>
  <UserAssignRoleForm ref="roleFormRef" @success="load" />
</template>

<script lang="ts" setup>
import * as EmployeeApi from '@/api/hradmin/employee'
import * as UserApi from '@/api/system/user'
import * as DeptApi from '@/api/system/dept'
import { checkPermi } from '@/utils/permission'
import UserAssignRoleForm from '@/views/system/user/UserAssignRoleForm.vue'
import RecruitmentPanel from './RecruitmentPanel.vue'
import OnboardingPanel from './OnboardingPanel.vue'

defineOptions({ name: 'HradminEmployee' })
const message = useMessage()
const canManage = checkPermi(['hradmin:employee:query'])
const myArchive = ref<EmployeeApi.EmployeeArchive>()
const page = ref<'人员' | '招聘'>('人员')
const salary = ref<{ agreedMonthlySalary: number; status: string }>()
const onboardingEligible = ref(false)
const overview = ref<EmployeeApi.EmployeeOverview>({ total: 0, active: 0, pending: 0, probation: 0, attention: 0, dueSoon: 0 })
const statCards = computed(() => [
  { label: '全部人员', value: overview.value.total, filter: '' },
  { label: '在职', value: overview.value.active, filter: 'active' },
  { label: '试用期', value: overview.value.probation, filter: 'probation' },
  { label: '待入职', value: overview.value.pending, filter: 'pending' },
  { label: '存在异常', value: overview.value.attention, filter: 'attention' }
])
const detailVisible = ref(false)
const detail = ref<EmployeeApi.EmployeeArchive>()
const detailTab = ref('basic')
const activeQuickFilter = ref('')
const openDetail = async (row: EmployeeApi.EmployeeArchive) => {
  if (!row.id) return
  detail.value = await EmployeeApi.getEmployee(row.id)
  salary.value = checkPermi(['hradmin:salary:read']) ? await EmployeeApi.getEmployeeSalary(row.id) : undefined
  onboardingEligible.value = (await EmployeeApi.getOnboarding(row.id)).length > 0
  detailTab.value = 'basic'
  detailVisible.value = true
}
const today = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
const employeeRisk = (row: EmployeeApi.EmployeeArchive) => {
  if (row.employmentStatus === 3) return ''
  if (row.contractEndDate && row.contractEndDate < today()) return '劳动合同已到期'
  if (row.socialStatus === 0) return '未参保，需跟进'
  return ''
}
const probationLabel = (row: EmployeeApi.EmployeeArchive) =>
  row.probationEndDate && row.employmentStatus === 4
    ? `${row.probationEndDate} 到期` : '—'
const displayStatus = (row: EmployeeApi.EmployeeArchive) => statusLabel(row.employmentStatus)
const applyStat = (filter: string) => {
  activeQuickFilter.value = filter
  query.employmentStatus = filter === 'active' ? 1 : filter === 'probation' ? 4 : filter === 'pending' ? 0 : undefined
  query.attentionOnly = filter === 'attention' ? true : undefined
  search()
}
const statusOptions = [
  { value: 0, label: '待入职' },
  { value: 1, label: '在职' },
  { value: 4, label: '试用期' },
  { value: 2, label: '离职交接' },
  { value: 3, label: '已离职' }
]
const statusLabel = (value: number) =>
  statusOptions.find((item) => item.value === value)?.label || '未知'
const deptName = (id?: number) => departments.value.find((dept) => dept.id === id)?.name || '—'
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  deptId: undefined as number | undefined,
  employmentStatus: undefined as number | undefined,
  attentionOnly: undefined as boolean | undefined
})
const rows = ref<EmployeeApi.EmployeeArchive[]>([])
const total = ref(0)
const loading = ref(false)
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const roleFormRef = ref()
const accounts = ref<UserApi.UserVO[]>([])
const departments = ref<DeptApi.DeptVO[]>([])
const selectedAccountDeptId = computed(
  () =>
    accounts.value.find((user) => user.id === form.value.userId)?.deptId || form.value.accountDeptId
)
const createMode = ref<'pending' | 'existing'>('existing')
const editingOnboarding = ref(false)
const existingActivationVisible = ref(false)
const existingActivationUserId = ref<number>()
const existingActivationEvidence = ref('')
const disabledAccounts = computed(() => accounts.value.filter(user => user.status === 1))
const emptyForm = (): EmployeeApi.EmployeeArchive => ({
  name: '',
  employmentStatus: 0
})
const form = ref<EmployeeApi.EmployeeArchive>(emptyForm())
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  hireDate: [{ required: true, message: '请选择约定到岗日', trigger: 'change' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await EmployeeApi.getEmployeePage(query)
    rows.value = data.list
    total.value = data.total
    overview.value = await EmployeeApi.getEmployeeOverview()
  } finally {
    loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  load()
}
const loadChoices = async () => {
  departments.value = await DeptApi.getSimpleDeptList()
  if (checkPermi(['system:user:query'])) {
    // ponytail: 当前员工规模小于 200；超过后改为服务端搜索账号，避免候选列表截断。
    const data = await UserApi.getUserPage({ pageNo: 1, pageSize: 200 })
    accounts.value = data.list
  }
}
const openCreate = async () => {
  form.value = emptyForm()
  createMode.value = 'existing'
  editingOnboarding.value = false
  formVisible.value = true
  await loadChoices()
}
const onConverted = async (id: number) => {
  page.value = '人员'
  await load()
  const row = await EmployeeApi.getEmployee(id)
  await openDetail(row)
}
const refreshDetail = async () => {
  if (detail.value?.id) detail.value = await EmployeeApi.getEmployee(detail.value.id)
  await load()
}
const openExistingActivation = async () => {
  existingActivationUserId.value = undefined
  existingActivationEvidence.value = ''
  await loadChoices()
  existingActivationVisible.value = true
}
const activateExisting = async () => {
  if (!detail.value?.id || !existingActivationUserId.value || !existingActivationEvidence.value.trim()) return message.warning('请选择账号并填写核验依据')
  saving.value = true
  try {
    await EmployeeApi.activateExistingAccount({ employeeId: detail.value.id, userId: existingActivationUserId.value, evidence: existingActivationEvidence.value })
    existingActivationVisible.value = false
    await refreshDetail()
    message.success('账号已开通')
  } finally { saving.value = false }
}
const openEdit = async (row: EmployeeApi.EmployeeArchive) => {
  form.value = { ...row }
  editingOnboarding.value = (await EmployeeApi.getOnboarding(row.id!)).length > 0
  formVisible.value = true
  await loadChoices()
}
const save = async () => {
  await formRef.value?.validate()
  if (!form.value.id && (!form.value.agreedMonthlySalary || form.value.agreedMonthlySalary <= 0)) return message.warning('请填写约定月薪')
  if (!form.value.id && createMode.value === 'pending' &&
      (!form.value.probationEndDate || !form.value.contractEndDate || !form.value.hireDate ||
       form.value.probationEndDate < form.value.hireDate || form.value.contractEndDate <= form.value.hireDate))
    return message.warning('请填写有效的试用期结束日和合同到期日')
  saving.value = true
  try {
    if (form.value.id) {
      await EmployeeApi.updateEmployee(form.value)
    } else if (createMode.value === 'existing') {
      await EmployeeApi.createExistingEmployee({ ...form.value, employmentStatus: 1 })
    } else {
      await EmployeeApi.createEmployee(form.value)
    }
    message.success('保存成功')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
const openRoleForm = async (row: EmployeeApi.EmployeeArchive) => {
  if (!row.userId) return
  const user = await UserApi.getUser(row.userId)
  roleFormRef.value?.open(user)
}
onMounted(async () => {
  if (canManage) {
    departments.value = await DeptApi.getSimpleDeptList()
    await load()
  } else {
    try { myArchive.value = await EmployeeApi.getMyArchive() } catch { myArchive.value = undefined }
  }
})
</script>

<style scoped>
.hr-workspace{width:100%;max-width:none;min-width:0;box-sizing:border-box;color:#28463b}.workspace-intro{padding:18px 6px 20px}.workspace-intro h1{font-size:27px;font-weight:700;margin:4px 0}.workspace-intro p,.heading-row p,.roster-heading p{color:#84958d;font-size:13px;margin:0}.eyebrow{color:#7e9c8e;font-size:10px;letter-spacing:2px;font-weight:700}.workspace-body{background:#f6f8f6;border:1px solid #e5ece8;border-radius:18px;overflow:hidden;min-height:650px}.workspace-nav{display:flex;align-items:center;gap:6px;padding:14px 18px;background:#fff;border-bottom:1px solid #edf0ed;overflow:auto}.workspace-nav button{border:0;background:transparent;white-space:nowrap;padding:10px 16px;color:#667c70;border-radius:7px}.workspace-nav button.active{background:#e6f0e9;color:#19664b;font-weight:700}.workspace-nav button:disabled{opacity:.55;cursor:not-allowed}.workspace-inner{padding:32px clamp(16px,2vw,36px) 44px;min-width:0}.heading-row,.roster-heading{display:flex;justify-content:space-between;align-items:center}.heading-row h2{font-size:24px;margin:4px 0 5px}.stat-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin:26px 0 33px}.stat-card{position:relative;text-align:left;border:1px solid #e9eeea;background:#fff;border-radius:11px;padding:20px 18px;color:#87988d;min-height:101px}.stat-card:hover{border-color:#94baa6}.stat-card strong{display:block;color:#25483a;font-size:30px;line-height:1.35;margin-top:12px}.stat-card small,.attention-card small{font-size:12px;font-weight:400;margin-left:5px}.stat-arrow{position:absolute;right:18px;bottom:18px}.section-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:13px}.section-head h3,.roster-heading h3{font-size:16px;font-weight:700;margin:0}.section-head span{font-size:12px;color:#91a096}.attention-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:24px}.attention-card{border:1px solid #e9eeea;border-radius:11px;background:#fff;padding:19px;text-align:left;min-height:122px;color:#28463b}.attention-card.risk{background:#fffaf7;border-color:#f5e6df;cursor:pointer}.attention-card span{font-weight:700;font-size:13px}.attention-card strong{float:right;font-size:23px}.attention-card p{clear:both;color:#88978c;font-size:12px;padding-top:12px}.attention-card em{font-style:normal;color:#809a8a;font-size:11px}.roster-panel{background:#fff;border:1px solid #e9eeea;border-radius:11px;padding:23px 18px;min-width:0}.roster-heading{margin:0 0 18px}.roster-heading h3 small{background:#edf2ee;color:#6d8776;padding:3px 6px;font-size:11px}.roster-filters{display:flex;flex-wrap:wrap;align-items:center;gap:8px 18px;margin-bottom:12px}.roster-filters :deep(.el-form-item){margin:0}.roster-filters :deep(.el-input){width:190px}.roster-table{width:100%;overflow-x:auto}.person-link{display:flex;flex-direction:column;text-align:left;color:#234e3c;background:none;border:0;cursor:pointer;padding:4px 0}.person-link:hover{text-decoration:underline}.person-link small,.roster-table small{color:#8b9b91}.risk-text{color:#c76752}.employee-hero{display:flex;gap:14px;align-items:center;padding:12px 0 24px}.employee-hero h2{font-size:19px;font-weight:700}.employee-hero p{color:#87988d}.employee-avatar{display:grid;place-items:center;width:48px;height:48px;border-radius:12px;background:#e3eee7;color:#276247;font-weight:700}
@media(max-width:1200px){.stat-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.attention-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:900px){.workspace-inner{padding:20px 14px}.stat-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.attention-grid{grid-template-columns:1fr}.heading-row{align-items:flex-start;gap:12px}.workspace-nav{padding:10px}}
</style>

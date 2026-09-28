<template>
  <section class="recruitment">
    <header class="heading-row">
      <div><span class="eyebrow">PEOPLE & OPERATIONS</span><h2>招聘与入职</h2><p>按阶段跟进候选人，录用通过后衔接入职手续。</p></div>
      <el-button v-if="checkPermi(['hradmin:recruitment:submit'])" type="primary" @click="requestVisible = true">＋ 用人申请</el-button>
    </header>
    <div class="metrics">
      <div><small>招聘需求</small><strong>{{ requests.length }}</strong></div>
      <div><small>跟进中</small><strong>{{ candidates.filter(x => !x.employeeId).length }}</strong></div>
      <div><small>待录用审批</small><strong>{{ candidates.filter(x => x.stage === '录用审批').length }}</strong></div>
      <div><small>已转员工</small><strong>{{ candidates.filter(x => x.employeeId).length }}</strong></div>
    </div>
    <div class="panel">
      <div class="tabs"><button :class="{ selected: view === 'candidates' }" @click="view = 'candidates'">候选人跟进　{{ candidates.length }}</button><button :class="{ selected: view === 'requests' }" @click="view = 'requests'">用人需求　{{ requests.length }}</button></div>
      <template v-if="view === 'requests'">
        <div v-for="item in requests" :key="item.id" class="request-card">
          <div><el-tag :type="item.status === '招聘中' ? 'success' : item.status === '已驳回' ? 'danger' : 'warning'">{{ item.status }}</el-tag><h3>{{ item.job }} · {{ item.count }} 人</h3><p>{{ deptName(item.deptId) }} · {{ item.reason }}</p></div>
          <div>
            <el-button v-if="item.status === '待审批' && checkPermi(['hradmin:recruitment:review'])" @click="openReview(item)">审核需求</el-button>
            <el-button v-if="item.status === '招聘中' && checkPermi(['hradmin:recruitment:candidate'])" @click="openCandidateNew(item)">登记候选人</el-button>
          </div>
        </div>
        <el-empty v-if="!requests.length" description="暂无用人需求" />
      </template>
      <template v-else>
        <div class="filters"><el-input v-model="candidateQuery" placeholder="搜索候选人姓名或申请岗位" clearable /><el-select v-model="candidateStage"><el-option label="全部阶段" value="全部" /><el-option v-for="item in stages" :key="item" :label="item" :value="item" /></el-select></div>
        <el-table :data="shownCandidates">
          <el-table-column prop="name" label="候选人" />
          <el-table-column label="申请岗位"><template #default="{ row }">{{ requestOf(row)?.job || '—' }}</template></el-table-column>
          <el-table-column prop="stage" label="当前阶段" />
          <el-table-column prop="latestNote" label="最近跟进" />
          <el-table-column label="操作"><template #default="{ row }"><el-button link type="primary" @click="openCandidate(row)">查看招聘流程</el-button></template></el-table-column>
        </el-table>
      </template>
    </div>
    <Dialog v-model="requestVisible" title="发起用人申请">
      <el-form label-width="90px"><el-form-item label="招聘岗位"><el-input v-model="requestForm.job" /></el-form-item><el-form-item label="所属中心"><el-select v-model="requestForm.deptId" class="w-full"><el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item><el-form-item label="招聘人数"><el-input-number v-model="requestForm.count" :min="1" :max="35" /></el-form-item><el-form-item label="用人理由"><el-input v-model="requestForm.reason" type="textarea" /></el-form-item></el-form>
      <template #footer><el-button @click="requestVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitRequest">提交</el-button></template>
    </Dialog>
    <Dialog v-model="reviewVisible" title="用人需求审批">
      <p>{{ selectedRequest?.job }} · {{ deptName(selectedRequest?.deptId) }} · {{ selectedRequest?.reason }}</p>
      <el-radio-group v-model="reviewApproved"><el-radio :value="true">同意</el-radio><el-radio :value="false">驳回</el-radio></el-radio-group>
      <el-input v-model="note" type="textarea" placeholder="审批依据" />
      <template #footer><el-button @click="reviewVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="reviewRequest">确认审批</el-button></template>
    </Dialog>
    <Dialog v-model="candidateNewVisible" title="添加候选人">
      <el-input v-model="candidateName" placeholder="候选人姓名" />
      <template #footer><el-button @click="candidateNewVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="createCandidate">保存</el-button></template>
    </Dialog>
    <el-drawer v-model="candidateVisible" :title="`${selectedCandidate?.name || ''} · 招聘流程`" size="min(640px, 100%)">
      <p>{{ requestOf(selectedCandidate)?.job }} · {{ deptName(requestOf(selectedCandidate)?.deptId) }}</p>
      <el-steps direction="vertical" :active="Math.max(0, stages.indexOf(selectedCandidate?.stage || '候选人'))" finish-status="success">
        <el-step v-for="stage in stages" :key="stage" :title="stage" />
      </el-steps>
      <h3>当前：{{ selectedCandidate?.stage }}</h3>
      <el-button v-if="selectedCandidate?.stage === '待入职' && canConvert" type="primary" @click="openConvert">建立入职档案</el-button>
      <template v-else-if="selectedCandidate && stages.indexOf(selectedCandidate.stage) < 4">
        <el-input v-model="note" type="textarea" placeholder="处理记录 / 决策依据" />
        <el-button v-if="canAdvance" type="primary" :loading="saving" @click="advance">办理当前步骤</el-button>
      </template>
      <h3>跟进记录</h3><p v-for="item in history" :key="item.id">{{ item.stage }} · {{ item.note }}</p>
    </el-drawer>
    <Dialog v-model="convertVisible" title="录用转员工">
      <el-form label-width="120px">
        <el-form-item label="姓名"><el-input :model-value="selectedCandidate?.name" disabled /></el-form-item>
        <el-form-item label="职务"><el-input :model-value="requestOf(selectedCandidate)?.job" disabled /></el-form-item>
        <el-form-item label="所属中心"><el-input :model-value="deptName(requestOf(selectedCandidate)?.deptId)" disabled /></el-form-item>
        <el-form-item label="所属项目"><el-input v-model="archive.projectName" /></el-form-item>
        <el-form-item label="直属上级"><el-select v-model="archive.managerUserId" clearable filterable class="w-full"><el-option v-for="user in managers" :key="user.id" :label="user.nickname" :value="user.id" /></el-select></el-form-item>
        <el-form-item label="入职日期"><el-date-picker v-model="archive.hireDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item>
        <el-form-item label="试用期结束日期"><el-date-picker v-model="archive.probationEndDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item>
        <el-form-item label="劳动合同到期日"><el-date-picker v-model="archive.contractEndDate" type="date" value-format="YYYY-MM-DD" class="w-full" /></el-form-item>
        <el-form-item label="约定月薪（元）"><el-input-number v-model="archive.agreedMonthlySalary" :min="0.01" :precision="2" class="w-full" /></el-form-item>
      </el-form>
      <p>保存后建立待入职档案。实际到岗及系统权限开通分别办理。</p>
      <template #footer><el-button @click="convertVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="convert">建立入职档案</el-button></template>
    </Dialog>
  </section>
</template>

<script setup lang="ts">
import * as RecruitmentApi from '@/api/hradmin/recruitment'
import type { EmployeeArchive } from '@/api/hradmin/employee'
import * as UserApi from '@/api/system/user'
import { checkPermi } from '@/utils/permission'

const props = defineProps<{ departments: Array<{ id: number; name: string }> }>()
const emit = defineEmits<{ converted: [id: number] }>()
const message = useMessage()
const stages = ['候选人', '面试', '评价', '录用审批', '待入职', '已转员工']
const requests = ref<RecruitmentApi.HiringRequest[]>([])
const candidates = ref<RecruitmentApi.Candidate[]>([])
const history = ref<RecruitmentApi.CandidateHistory[]>([])
const view = ref<'candidates' | 'requests'>('candidates')
const candidateQuery = ref('')
const candidateStage = ref('全部')
const requestVisible = ref(false)
const reviewVisible = ref(false)
const candidateNewVisible = ref(false)
const candidateVisible = ref(false)
const convertVisible = ref(false)
const saving = ref(false)
const selectedRequest = ref<RecruitmentApi.HiringRequest>()
const selectedCandidate = ref<RecruitmentApi.Candidate>()
const requestForm = reactive({ job: '', deptId: undefined as number | undefined, count: 1, reason: '' })
const reviewApproved = ref(true)
const candidateName = ref('')
const note = ref('')
const managers = ref<UserApi.UserVO[]>([])
const archive = ref<EmployeeArchive>({ name: '', employmentStatus: 0, projectName: '平台公共服务' })
const canConvert = computed(() => checkPermi(['hradmin:recruitment:convert']) && checkPermi(['hradmin:employee:create']) && checkPermi(['hradmin:salary:write']))
const canAdvance = computed(() => {
  const stage = selectedCandidate.value?.stage
  return checkPermi([stage === '候选人' ? 'hradmin:recruitment:candidate' : stage === '录用审批' ? 'hradmin:recruitment:approve' : 'hradmin:recruitment:manager'])
})
const deptName = (id?: number) => props.departments.find(d => d.id === id)?.name || '—'
const requestOf = (candidate?: RecruitmentApi.Candidate) => requests.value.find(r => r.id === candidate?.requestId)
const shownCandidates = computed(() => candidates.value.filter(c =>
  (candidateStage.value === '全部' || c.stage === candidateStage.value) &&
  (!candidateQuery.value || `${c.name}${requestOf(c)?.job || ''}`.includes(candidateQuery.value.trim()))
))
const load = async () => {
  ;[requests.value, candidates.value] = await Promise.all([RecruitmentApi.getRequests(), RecruitmentApi.getCandidates()])
}
const run = async (action: () => Promise<unknown>, close: () => void) => {
  saving.value = true
  try { await action(); close(); await load(); message.success('已保存') }
  finally { saving.value = false }
}
const submitRequest = () => {
  if (!requestForm.job.trim() || !requestForm.reason.trim() || !requestForm.deptId) return message.warning('请填写完整的用人申请')
  return run(() => RecruitmentApi.submitRequest({ ...requestForm, deptId: requestForm.deptId! }), () => { requestVisible.value = false; requestForm.job = ''; requestForm.reason = '' })
}
const openReview = (item: RecruitmentApi.HiringRequest) => { selectedRequest.value = item; note.value = ''; reviewApproved.value = true; reviewVisible.value = true }
const reviewRequest = () => {
  if (!selectedRequest.value || !note.value.trim()) return message.warning('请填写审批依据')
  return run(() => RecruitmentApi.reviewRequest({ id: selectedRequest.value!.id, approved: reviewApproved.value, note: note.value }), () => reviewVisible.value = false)
}
const openCandidateNew = (item: RecruitmentApi.HiringRequest) => { selectedRequest.value = item; candidateName.value = ''; candidateNewVisible.value = true }
const createCandidate = () => {
  if (!selectedRequest.value || !candidateName.value.trim()) return message.warning('请填写候选人姓名')
  return run(() => RecruitmentApi.addCandidate({ requestId: selectedRequest.value!.id, name: candidateName.value }), () => candidateNewVisible.value = false)
}
const openCandidate = async (item: RecruitmentApi.Candidate) => { selectedCandidate.value = item; note.value = ''; history.value = await RecruitmentApi.getHistory(item.id); candidateVisible.value = true }
const advance = async () => {
  if (!selectedCandidate.value || !note.value.trim()) return message.warning('请填写处理依据')
  await run(() => RecruitmentApi.advanceCandidate({ id: selectedCandidate.value!.id, expectedStage: selectedCandidate.value!.stage, note: note.value }), () => {})
  const refreshed = candidates.value.find(c => c.id === selectedCandidate.value?.id)
  if (refreshed) await openCandidate(refreshed)
}
const openConvert = async () => {
  archive.value = { name: selectedCandidate.value?.name || '', employmentStatus: 0, projectName: '平台公共服务' }
  if (checkPermi(['system:user:query'])) managers.value = (await UserApi.getUserPage({ pageNo: 1, pageSize: 200 })).list
  convertVisible.value = true
}
const convert = async () => {
  if (!selectedCandidate.value || !archive.value.hireDate || !archive.value.probationEndDate || !archive.value.contractEndDate || !archive.value.agreedMonthlySalary) return message.warning('请填写入职日期、试用期、合同日期和约定月薪')
  if (archive.value.probationEndDate < archive.value.hireDate || archive.value.contractEndDate <= archive.value.hireDate) return message.warning('试用期结束日不得早于入职日，合同到期日须晚于入职日')
  let employeeId = 0
  await run(async () => { employeeId = await RecruitmentApi.convertCandidate({ id: selectedCandidate.value!.id, archive: archive.value }) }, () => { convertVisible.value = false; candidateVisible.value = false })
  emit('converted', employeeId)
}
onMounted(load)
</script>

<style scoped>
.recruitment{padding:32px clamp(16px,2vw,36px);color:#28463b}.heading-row{display:flex;align-items:center;justify-content:space-between}.heading-row h2{font-size:24px;margin:4px 0}.heading-row p{color:#84958d;font-size:13px}.eyebrow{font-size:10px;letter-spacing:2px;color:#7e9c8e}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:26px 0}.metrics>div{background:#fff;border:1px solid #e9eeea;border-radius:11px;padding:18px}.metrics small{display:block;color:#84958d}.metrics strong{font-size:28px}.panel{background:#fff;border:1px solid #e9eeea;border-radius:11px;padding:20px}.tabs{display:flex;gap:18px;border-bottom:1px solid #e9eeea;margin-bottom:16px}.tabs button{border:0;background:none;padding:10px;color:#728579}.tabs button.selected{color:#176448;border-bottom:2px solid #176448}.request-card{display:flex;justify-content:space-between;gap:16px;align-items:center;border-bottom:1px solid #edf0ed;padding:15px}.request-card h3{margin:8px 0}.request-card p{margin:0;color:#84958d}.filters{display:flex;gap:12px;max-width:520px;margin:16px 0}.filters .el-input{width:300px}.filters .el-select{width:180px}
@media(max-width:800px){.metrics{grid-template-columns:repeat(2,1fr)}.request-card{align-items:flex-start;flex-direction:column}}
</style>

<template>
  <section class="onboarding">
    <h3>入职流程 <small>{{ tasks.filter(t => t.doneAt).length }} / {{ tasks.length }} 项完成</small></h3>
    <p v-if="tasks.length && tasks.every(t => t.doneAt)" class="complete">入职手续已完成</p>
    <div v-for="stage in ['入职前', '入职当天', '入职后']" :key="stage" class="stage">
      <h4>{{ stage }}</h4>
      <div v-for="task in tasks.filter(t => t.stage === stage)" :key="task.id" class="task">
        <div><strong>{{ task.title }}</strong><small>{{ task.owner }} · 截止 {{ task.dueDate }}</small><p v-if="task.doneAt">✓ 已办理 · {{ task.evidence }}</p></div>
        <el-button v-if="!task.doneAt && canHandle(task)" :disabled="!canStart(task)" @click="open(task)">办理</el-button>
        <span v-else-if="!task.doneAt">待办理</span>
      </div>
    </div>
    <Dialog v-model="visible" :title="selected?.title || '办理入职事项'">
      <p>{{ employee.name }} · {{ selected?.owner }} · 截止 {{ selected?.dueDate }}</p>
      <el-alert v-if="selected?.taskKey === 'social'" title="仅在已依法参保并取得办理证明后确认，不能以放弃协议关闭。" type="warning" :closable="false" />
      <el-checkbox v-if="selected?.taskKey === 'social'" v-model="insured" class="mt-3">确认已依法参保</el-checkbox>
      <el-form-item v-if="selected?.taskKey === 'access'" label="禁用账号"><el-select v-model="userId" filterable class="w-full"><el-option v-for="user in disabledAccounts" :key="user.id" :label="user.nickname" :value="user.id" /></el-select></el-form-item>
      <el-input v-model="evidence" type="textarea" :rows="3" placeholder="填写文件版本、凭证编号或核验情况等可追溯依据" />
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="complete">确认办理</el-button></template>
    </Dialog>
  </section>
</template>

<script setup lang="ts">
import * as EmployeeApi from '@/api/hradmin/employee'
import * as UserApi from '@/api/system/user'
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/store/modules/user'

const props = defineProps<{ employee: EmployeeApi.EmployeeArchive; own?: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const message = useMessage()
const userStore = useUserStore()
const tasks = ref<EmployeeApi.OnboardingTask[]>([])
const disabledAccounts = ref<UserApi.UserVO[]>([])
const selected = ref<EmployeeApi.OnboardingTask>()
const visible = ref(false)
const saving = ref(false)
const evidence = ref('')
const userId = ref<number>()
const insured = ref(false)
const load = async () => { tasks.value = props.own ? await EmployeeApi.getMyOnboarding() : await EmployeeApi.getOnboarding(props.employee.id!) }
const canHandle = (task: EmployeeApi.OnboardingTask) => {
  const permission: Record<string, string> = { 人事专员: 'hradmin:onboarding:hr', 直属上级: 'hradmin:onboarding:manager', 创业发展总监: 'hradmin:onboarding:director' }
  return task.owner === '员工本人' ? props.employee.userId === userStore.getUser.id : checkPermi([permission[task.owner]])
}
const canStart = (task: EmployeeApi.OnboardingTask) =>
  tasks.value.every(item => item.stageNo >= task.stageNo || !!item.doneAt) &&
  (task.taskKey !== 'access' || tasks.value.some(item => item.taskKey === 'arrival' && item.doneAt))
const open = async (task: EmployeeApi.OnboardingTask) => {
  selected.value = task; evidence.value = ''; userId.value = undefined; insured.value = false
  if (task.taskKey === 'access' && checkPermi(['system:user:query'])) disabledAccounts.value = (await UserApi.getUserPage({ pageNo: 1, pageSize: 200 })).list.filter(user => user.status === 1)
  visible.value = true
}
const complete = async () => {
  if (!selected.value || !evidence.value.trim()) return message.warning('请填写办理依据')
  if (selected.value.taskKey === 'social' && !insured.value) return message.warning('请先确认已依法参保')
  if (selected.value.taskKey === 'access' && !userId.value) return message.warning('请选择已核对的禁用账号')
  saving.value = true
  try {
    await EmployeeApi.completeOnboarding({ employeeId: props.employee.id!, taskKey: selected.value.taskKey, evidence: evidence.value, userId: userId.value, socialStatus: insured.value ? '已参保' : undefined })
    visible.value = false; await load(); emit('changed'); message.success('事项已办理')
  } finally { saving.value = false }
}
watch(() => props.employee.id, load, { immediate: true })
</script>

<style scoped>
.onboarding h3{display:flex;align-items:center;gap:12px;margin:22px 0}.onboarding h3 small{font-size:12px;color:#84958d}.complete{color:#23805b}.stage{margin:18px 0}.stage h4{margin:0 0 10px;color:#28463b}.task{display:flex;justify-content:space-between;align-items:center;gap:16px;border-bottom:1px solid #e8eee9;padding:12px 0}.task strong,.task small{display:block}.task small{color:#82948a;margin-top:5px}.task p{color:#23805b;font-size:12px;margin:7px 0 0}.task span{color:#9baaa0;font-size:12px}
</style>

<template>
  <ContentWrap>
    <div class="flex items-center justify-between gap-16px">
      <div>
        <div class="text-18px font-600">企业微信通讯录同步</div>
        <div class="mt-6px text-13px text-[var(--el-text-color-secondary)]">
          先同步并核对企微可见范围，再应用到系统部门和账号。已关联记录会更新，新账号默认禁用且不授予角色。
        </div>
      </div>
      <div class="flex gap-8px">
        <el-button v-hasPermi="['system:wecom-sync:execute']" :loading="syncing" @click="sync">
          <Icon icon="ep:refresh" class="mr-5px" />立即同步
        </el-button>
        <el-button v-hasPermi="['system:wecom-sync:apply']" type="primary" :loading="applying" :disabled="!latest" @click="apply">
          应用到系统
        </el-button>
      </div>
    </div>
    <div class="mt-22px flex flex-wrap gap-28px text-14px">
      <span>最近同步：{{ latest?.createTime ? formatDate(latest.createTime) : '尚未同步' }}</span>
      <span>部门：{{ latest?.departmentCount ?? '—' }}</span>
      <span>员工：{{ latest?.userCount ?? '—' }}</span>
      <span>待关联账号：{{ pendingCount }}</span>
    </div>
    <el-alert v-if="missingParentCount" class="mt-16px" type="warning" :closable="false"
      :title="`${missingParentCount} 个部门的上级不在企微应用可见范围内；应用后会暂挂系统根节点，扩大范围并重新同步后可修正。`" />
    <el-alert v-if="users.length && !profileFieldCount" class="mt-16px" type="warning" :closable="false"
      title="当前企微应用未返回任何员工的手机、邮箱或性别，重复同步也无法补齐。需要可读取敏感字段的通讯录凭证，或另接员工逐人授权流程；现有资料不会被空值覆盖。" />
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="tab">
      <el-tab-pane label="员工" name="users">
        <div class="mb-16px flex gap-12px">
          <el-input v-model="keyword" clearable placeholder="搜索姓名或企微员工 ID" class="!w-280px" />
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
        <el-table v-loading="loading" :data="visibleUsers" stripe :show-overflow-tooltip="true">
          <el-table-column label="姓名" prop="name" min-width="130" />
          <el-table-column label="企微员工 ID" prop="wecomUserId" min-width="170" />
          <el-table-column label="所属部门" min-width="200">
            <template #default="scope">{{ departmentNames(scope.row.departmentIds) }}</template>
          </el-table-column>
          <el-table-column label="手机 / 邮箱 / 性别" min-width="190">
            <template #default="scope">
              {{ [scope.row.hasMobile && '手机', scope.row.hasEmail && '邮箱', scope.row.hasSex && '性别'].filter(Boolean).join('、') || '未返回' }}
            </template>
          </el-table-column>
          <el-table-column label="系统账号" width="150">
            <template #default="scope">
              <el-tag v-if="scope.row.systemUserId" type="success">ID {{ scope.row.systemUserId }}</el-tag>
              <el-tag v-else type="warning">待关联</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="scope">
              <el-button v-hasPermi="['system:wecom-sync:link']" link type="primary" @click="openLink(scope.row)">
                核对关联
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="最近同步" min-width="170">
            <template #default="scope">{{ formatDate(scope.row.lastSeenAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="部门" name="departments">
        <el-table v-loading="loading" :data="departments" stripe :show-overflow-tooltip="true">
          <el-table-column label="企微部门 ID" prop="wecomDeptId" width="150" />
          <el-table-column label="部门名称" prop="name" min-width="220" />
          <el-table-column label="负责人" min-width="160">
            <template #default="scope">{{ departmentLeaderNames(scope.row.leaderUserIds) }}</template>
          </el-table-column>
          <el-table-column label="上级部门" min-width="200">
            <template #default="scope">{{ departmentMap.get(scope.row.parentWecomDeptId) || '—' }}</template>
          </el-table-column>
          <el-table-column label="系统部门" width="140">
            <template #default="scope">{{ scope.row.systemDeptId ? `ID ${scope.row.systemDeptId}` : '待应用' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="scope">
              <el-button v-if="!scope.row.systemDeptId" v-hasPermi="['system:wecom-sync:link']"
                link type="primary" @click="openDepartmentLink(scope.row)">关联现有部门</el-button>
            </template>
          </el-table-column>
          <el-table-column label="最近同步" min-width="170">
            <template #default="scope">{{ formatDate(scope.row.lastSeenAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <el-dialog v-model="linkVisible" title="关联现有系统账号" width="480px">
    <p class="mb-16px">企微员工：{{ linkingUser?.name }}（{{ linkingUser?.wecomUserId }}）</p>
    <el-select v-model="selectedUserId" filterable clearable class="w-full" placeholder="选择已开通的系统账号">
      <el-option
        v-for="account in systemUsers"
        :key="account.id"
        :value="account.id"
        :label="`${account.nickname}（ID ${account.id}，${account.deptName || '无部门'}）`"
      />
    </el-select>
    <template #footer>
      <el-button @click="linkVisible = false">取消</el-button>
      <el-button type="primary" :loading="linking" :disabled="!selectedUserId" @click="confirmLink">确认关联</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="departmentLinkVisible" title="关联现有系统部门" width="480px">
    <p class="mb-16px">企微部门：{{ linkingDepartment?.name }}（ID {{ linkingDepartment?.wecomDeptId }}）</p>
    <el-select v-model="selectedDeptId" filterable clearable class="w-full" placeholder="选择现有系统部门">
      <el-option v-for="dept in systemDepartments" :key="dept.id" :value="dept.id"
        :label="`${dept.name}（ID ${dept.id}）`" />
    </el-select>
    <template #footer>
      <el-button @click="departmentLinkVisible = false">取消</el-button>
      <el-button type="primary" :loading="departmentLinking" :disabled="!selectedDeptId"
        @click="confirmDepartmentLink">确认关联</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as WecomApi from '@/api/system/wecomSync'
import * as UserApi from '@/api/system/user'
import * as DeptApi from '@/api/system/dept'
import type { WecomDepartment, WecomSyncRun, WecomUser } from '@/api/system/wecomSync'

defineOptions({ name: 'SystemWecomSync' })
const message = useMessage()
const loading = ref(false)
const syncing = ref(false)
const applying = ref(false)
const tab = ref('users')
const keyword = ref('')
const linkVisible = ref(false)
const linking = ref(false)
const departmentLinkVisible = ref(false)
const departmentLinking = ref(false)
const linkingDepartment = ref<WecomDepartment>()
const selectedDeptId = ref<number>()
const systemDepartments = ref<Array<{ id: number; name: string }>>([])
const linkingUser = ref<WecomUser>()
const selectedUserId = ref<number>()
const systemUsers = ref<Array<{ id: number; nickname: string; deptName?: string }>>([])
const latest = ref<WecomSyncRun>()
const departments = ref<WecomDepartment[]>([])
const users = ref<WecomUser[]>([])
const departmentMap = computed(() => new Map(departments.value.map((item) => [item.wecomDeptId, item.name])))
const pendingCount = computed(() => users.value.filter((item) => !item.systemUserId).length)
const profileFieldCount = computed(() => users.value.filter((item) => item.hasMobile || item.hasEmail || item.hasSex).length)
const missingParentCount = computed(() => departments.value.filter((item) => item.parentWecomDeptId !== 0 && !departmentMap.value.has(item.parentWecomDeptId)).length)
const visibleUsers = computed(() =>
  users.value.filter((item) => !keyword.value || item.name.includes(keyword.value) || item.wecomUserId.includes(keyword.value))
)

function departmentNames(raw: string): string {
  try {
    const ids = JSON.parse(raw) as number[]
    return ids.map((id) => departmentMap.value.get(id) || String(id)).join('、') || '—'
  } catch {
    return '—'
  }
}

function departmentLeaderNames(raw?: string): string {
  if (!raw) return '—'
  try {
    const ids = JSON.parse(raw) as string[]
    const names = new Map(users.value.map((item) => [item.wecomUserId, item.name]))
    return ids.map((id) => names.get(id) || id).join('、') || '—'
  } catch {
    return '—'
  }
}

async function load() {
  loading.value = true
  try {
    const [run, deptRows, userRows] = await Promise.all([
      WecomApi.getLatest(), WecomApi.getDepartments(), WecomApi.getUsers()
    ])
    latest.value = run || undefined
    departments.value = deptRows || []
    users.value = userRows || []
  } finally {
    loading.value = false
  }
}

async function sync() {
  await message.confirm('将读取当前租户企微应用可见的部门和员工，确认开始同步？')
  syncing.value = true
  try {
    await WecomApi.executeSync()
    message.success('同步完成')
    await load()
  } finally {
    syncing.value = false
  }
}

async function apply() {
  await message.confirm('将创建或更新已映射的系统部门，并按企微 userid 更新已关联员工的姓名、部门及可获取资料；未关联员工创建禁用账号。角色和账号状态保持不变。确认应用？')
  applying.value = true
  try {
    const result = await WecomApi.applySync()
    message.success(`部门新增 ${result.createdDepartments}、更新 ${result.updatedDepartments}；员工新增 ${result.createdUsers}、更新 ${result.updatedUsers}`)
    await load()
  } finally {
    applying.value = false
  }
}

async function openLink(user: WecomUser) {
  linkingUser.value = user
  selectedUserId.value = user.systemUserId
  systemUsers.value = await UserApi.getSimpleUserList()
  linkVisible.value = true
}

async function confirmLink() {
  if (!linkingUser.value || !selectedUserId.value) return
  linking.value = true
  try {
    await WecomApi.linkUser({ wecomUserId: linkingUser.value.wecomUserId, systemUserId: selectedUserId.value })
    message.success('关联完成')
    linkVisible.value = false
    await load()
  } finally {
    linking.value = false
  }
}

async function openDepartmentLink(department: WecomDepartment) {
  linkingDepartment.value = department
  selectedDeptId.value = undefined
  systemDepartments.value = await DeptApi.getSimpleDeptList()
  departmentLinkVisible.value = true
}

async function confirmDepartmentLink() {
  if (!linkingDepartment.value || !selectedDeptId.value) return
  departmentLinking.value = true
  try {
    await WecomApi.linkDepartment({
      wecomDeptId: linkingDepartment.value.wecomDeptId,
      systemDeptId: selectedDeptId.value
    })
    message.success('部门关联完成，再点击“应用到系统”即可更新')
    departmentLinkVisible.value = false
    await load()
  } finally {
    departmentLinking.value = false
  }
}
onMounted(load)
</script>


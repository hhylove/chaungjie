<template>
  <ContentWrap>
    <el-form :inline="true" class="-mb-15px" label-width="70px">
      <el-form-item label="账号">
        <el-input v-model="username" placeholder="输入员工账号" clearable class="!w-240px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="部门">
        <el-tree-select v-model="deptId" :data="departments" :props="defaultProps" check-strictly
          clearable node-key="id" placeholder="全部部门" class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="search"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="reset"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap title="使用人员">
    <div class="flex items-center justify-between mb-16px">
      <span class="text-13px text-[var(--el-text-color-secondary)]">默认未开通；开通后还需租户总开关开启、账号状态正常</span>
      <div>
        <el-button type="primary" plain :disabled="selected.length === 0" @click="changeSelected(true)">批量开通</el-button>
        <el-button type="danger" plain :disabled="selected.length === 0" @click="changeSelected(false)">批量停用</el-button>
      </div>
    </div>
    <el-table :data="rows" v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="username" label="账号" min-width="130" show-overflow-tooltip />
      <el-table-column prop="nickname" label="姓名" min-width="130" show-overflow-tooltip />
      <el-table-column label="部门" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.deptId == null ? '-' : (departmentNames[row.deptId] || row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="账号状态" width="110">
        <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">
          {{ row.status === 0 ? '正常' : '停用' }}
        </el-tag></template>
      </el-table-column>
      <el-table-column label="助理状态" width="130">
        <template #default="{ row }"><el-tag :type="row.enabled && row.status === 0 ? 'success' : 'info'">
          {{ row.enabled ? (row.status === 0 ? '已开通' : '账号停用') : '未开通' }}
        </el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }"><el-switch :model-value="row.enabled" :loading="changingId === row.id"
          @change="changeOne(row)" /></template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="pageNo" v-model:limit="pageSize" :total="total" @pagination="load" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getUsers, setUserGrants, type AssistantUser } from '@/api/assistant'
import * as DeptApi from '@/api/system/dept'
import { defaultProps, handleTree } from '@/utils/tree'

defineOptions({ name: 'AssistantUsers' })
const message = useMessage()
const username = ref('')
const deptId = ref<number | undefined>()
const departments = ref<Tree[]>([])
const departmentNames = ref<Record<number, string>>({})
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const changingId = ref<number | null>(null)
const rows = ref<AssistantUser[]>([])
const selected = ref<AssistantUser[]>([])
const onSelect = (value: AssistantUser[]) => { selected.value = value }
const search = () => { pageNo.value = 1; load() }
const reset = () => { username.value = ''; deptId.value = undefined; search() }

const load = async () => {
  loading.value = true
  try {
    const result = await getUsers({ pageNo: pageNo.value, pageSize: pageSize.value,
      username: username.value || undefined, deptId: deptId.value })
    rows.value = result.list
    total.value = result.total
  } finally { loading.value = false }
}

const changeOne = async (user: AssistantUser) => {
  changingId.value = user.id
  try {
    await setUserGrants([user.id], !user.enabled)
    message.success('开通状态已更新')
    await load()
  } finally { changingId.value = null }
}

const changeSelected = async (enabled: boolean) => {
  await setUserGrants(selected.value.map((user) => user.id), enabled)
  message.success('批量配置完成')
  await load()
}

onMounted(async () => {
  const list = await DeptApi.getSimpleDeptList()
  departments.value = handleTree(list)
  departmentNames.value = Object.fromEntries(list.map((item) => [item.id, item.name]))
  await load()
})
</script>

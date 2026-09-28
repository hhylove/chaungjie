<template>
  <ContentWrap title="助理使用审计">
    <el-alert title="展示最近 50 条使用与管理记录。只记录操作、结果和耗时，不展示员工提问或回答正文。"
      type="info" :closable="false" class="mb-16px" />
    <div class="flex items-center justify-between mb-16px">
      <span class="text-13px text-[var(--el-text-color-secondary)]">记录仅限当前租户，员工无法查看此页面</span>
      <el-button :loading="loading" @click="load"><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDate(row.create_time) }}</template>
      </el-table-column>
      <el-table-column label="操作人" min-width="180">
        <template #default="{ row }">
          <div>{{ row.nickname || row.username || '未知用户' }}</div>
          <div class="text-12px text-[var(--el-text-color-placeholder)]">{{ row.username ? `@${row.username}` : `ID ${row.user_id}` }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="130">
        <template #default="{ row }">{{ actionLabels[row.action] || row.action }}</template>
      </el-table-column>
      <el-table-column label="结果" min-width="150">
        <template #default="{ row }"><el-tag :type="outcomeType(row.outcome)">
          {{ outcomeLabels[row.outcome] || row.outcome }}
        </el-tag></template>
      </el-table-column>
      <el-table-column label="模型" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.model_name || '-' }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="120">
        <template #default="{ row }">{{ row.latency_ms == null ? '-' : `${row.latency_ms} ms` }}</template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAudit } from '@/api/assistant'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'AssistantAudit' })
const actionLabels: Record<string, string> = {
  ask: '提问', model_test: '模型测试', config_update: '配置更新',
  user_enable: '开通员工', user_disable: '停用员工'
}
const outcomeLabels: Record<string, string> = {
  success: '成功', failed: '失败', not_enabled: '未开通', rate_limited: '请求过于频繁',
  forbidden_conversation: '无权访问会话', revoked_during_request: '请求中被停用',
  model_or_storage_error: '模型或存储异常'
}
const outcomeType = (outcome: string) => outcome === 'success' ? 'success' :
  outcome === 'rate_limited' || outcome === 'not_enabled' ? 'warning' : 'danger'
const rows = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const load = async () => {
  loading.value = true
  try { rows.value = await getAudit() } finally { loading.value = false }
}
onMounted(load)
</script>

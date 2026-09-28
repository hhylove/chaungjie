<template>
  <ContentWrap title="智能助理总览">
    <div class="flex items-center justify-between mb-18px">
      <div class="text-14px text-[var(--el-text-color-secondary)]">当前租户的服务状态与最近 7 天使用情况</div>
      <el-button :loading="loading" @click="load"><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" class="mb-16px">
        <el-card shadow="never" class="h-100%">
          <div class="metric-label">服务状态</div>
          <div class="metric-value"><el-tag :type="overview.enabled ? 'success' : 'info'" size="large">
            {{ overview.enabled ? '运行中' : '未启用' }}
          </el-tag></div>
          <div class="metric-note">{{ overview.enabled ? '已开通员工可使用悬浮窗' : '请先到全局配置开启助理' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-16px">
        <el-card shadow="never" class="h-100%">
          <div class="metric-label">回答模型</div>
          <div class="metric-value"><el-tag :type="modelTagType" size="large">{{ modelState }}</el-tag></div>
          <div class="metric-note">连接测试仅表示服务可访问</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-16px">
        <el-card shadow="never" class="h-100%">
          <div class="metric-label">已开通员工</div>
          <div class="metric-number">{{ overview.enabledUsers ?? 0 }}</div>
          <div class="metric-note">账号停用后无法使用助理</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-16px">
        <el-card shadow="never" class="h-100%">
          <div class="metric-label">近 7 天提问</div>
          <div class="metric-number">{{ overview.requests7d ?? 0 }}</div>
          <div class="metric-note">其中失败 {{ overview.failures7d ?? 0 }} 次</div>
        </el-card>
      </el-col>
    </el-row>
    <el-alert type="info" :closable="false" class="mt-8px"
      title="知识库可提供已授权文档的问答；营业额、入库、面试等实时业务数据尚未接入。" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOverview } from '@/api/assistant'

defineOptions({ name: 'AssistantOverview' })
const overview = ref<Record<string, number | boolean | string>>({})
const loading = ref(false)
const modelState = computed(() => {
  if (!overview.value.modelConfigured) return '未配置'
  if (overview.value.lastModelTestOutcome === 'success') return '最近测试成功'
  if (overview.value.lastModelTestOutcome === 'failed') return '最近测试失败'
  return '待连接测试'
})
const modelTagType = computed(() => overview.value.lastModelTestOutcome === 'success' ? 'success' :
  overview.value.lastModelTestOutcome === 'failed' ? 'danger' : 'warning')
const load = async () => {
  loading.value = true
  try { overview.value = await getOverview() } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.metric-label { color: var(--el-text-color-secondary); font-size: 13px; }
.metric-value { margin: 17px 0 12px; min-height: 28px; }
.metric-number { margin: 11px 0 5px; font-size: 30px; font-weight: 600; line-height: 1.4; }
.metric-note { color: var(--el-text-color-placeholder); font-size: 12px; }
</style>

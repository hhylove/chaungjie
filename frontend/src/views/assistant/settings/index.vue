<template>
  <ContentWrap title="全局配置">
    <el-alert type="info" :closable="false" class="mb-20px"
      title="这里控制当前租户的助理服务。员工还需在「使用人员」单独开通，才能看到悬浮窗。" />
    <div class="section-title">服务设置</div>
    <el-form label-width="150px" class="max-w-680px">
      <el-form-item label="启用本租户助理">
        <el-switch v-model="form.enabled" />
        <span class="field-hint">关闭后，已开通员工也无法使用助理</span>
      </el-form-item>
      <el-form-item label="会话保存天数">
        <el-input-number v-model="form.retentionDays" :min="1" :max="365" controls-position="right" />
        <span class="field-hint">到期后自动清理会话与审计记录</span>
      </el-form-item>
      <el-form-item label="每人每分钟上限">
        <el-input-number v-model="form.requestsPerMinute" :min="1" :max="120" controls-position="right" />
        <span class="field-hint">限制单个员工的提问频率</span>
      </el-form-item>
    </el-form>
    <el-divider />
    <div class="section-title">回答模型</div>
    <el-form label-width="150px" class="max-w-680px">
      <el-form-item label="模型服务地址">
        <el-input v-model="form.modelBaseUrl" placeholder="http://127.0.0.1:8000/v1" />
        <div class="field-hint">仅支持内网 HTTP 地址，填写 OpenAI 兼容接口的 /v1 根地址</div>
      </el-form-item>
      <el-form-item label="模型标识">
        <el-input v-model="form.modelName" placeholder="服务端暴露的模型名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save"><Icon icon="ep:check" class="mr-5px" />保存配置</el-button>
        <el-button :loading="testing" :disabled="!savedModelReady" @click="checkModel">测试已保存配置</el-button>
        <span class="field-hint">修改地址或名称后，先保存再测试</span>
      </el-form-item>
    </el-form>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getConfig, saveConfig, testModel } from '@/api/assistant'

defineOptions({ name: 'AssistantSettings' })
const message = useMessage()
const saving = ref(false)
const testing = ref(false)
const form = reactive({ enabled: false, modelBaseUrl: '', modelName: '', retentionDays: 30, requestsPerMinute: 10 })
const savedModel = ref({ modelBaseUrl: '', modelName: '' })
const savedModelReady = computed(() => !!savedModel.value.modelBaseUrl && !!savedModel.value.modelName &&
  savedModel.value.modelBaseUrl === form.modelBaseUrl && savedModel.value.modelName === form.modelName)

onMounted(async () => {
  const value = await getConfig()
  form.enabled = value.enabled
  form.modelBaseUrl = value.modelBaseUrl ?? ''
  form.modelName = value.modelName ?? ''
  form.retentionDays = value.retentionDays
  form.requestsPerMinute = value.requestsPerMinute
  savedModel.value = { modelBaseUrl: form.modelBaseUrl, modelName: form.modelName }
})

const save = async () => {
  saving.value = true
  try {
    await saveConfig({ ...form })
    savedModel.value = { modelBaseUrl: form.modelBaseUrl, modelName: form.modelName }
    message.success('配置已保存')
  } finally { saving.value = false }
}

const checkModel = async () => {
  testing.value = true
  try {
    await testModel()
    message.success('内网模型连接成功')
  } finally { testing.value = false }
}
</script>

<style scoped>
.section-title { margin: 0 0 20px; font-size: 16px; font-weight: 600; color: var(--el-text-color-primary); }
.field-hint { margin-left: 12px; color: var(--el-text-color-placeholder); font-size: 12px; }
</style>

<template>
  <div class="rule-settings" v-loading="loading">
    <p class="hint">提交人权限、审批去重、标题摘要、打印模板等规则</p>
    <ExtraSettings v-if="ready" ref="extraSettingsRef" v-model="formData" />
    <div class="rule-settings__footer" v-if="editable">
      <el-button type="primary" :loading="saveLoading" @click="handleSave">保存规则</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import * as ModelApi from '@/api/bpm/model'
import { BpmAutoApproveType, BpmModelFormType, BpmModelType } from '@/utils/constants'
import ExtraSettings from '../form/ExtraSettings.vue'

defineOptions({ name: 'BpmModelRuleSettings' })

const props = withDefaults(
  defineProps<{
    modelId: number | string
    editable?: boolean
  }>(),
  {
    editable: false
  }
)

const message = useMessage()
const loading = ref(true)
const saveLoading = ref(false)
const ready = ref(false)
const extraSettingsRef = ref()
const processData = ref<any>()

const formData: any = ref({
  id: undefined,
  name: '',
  key: '',
  type: BpmModelType.BPMN,
  formType: BpmModelFormType.NORMAL,
  formId: '',
  allowCancelRunningProcess: true,
  processIdRule: {
    enable: false,
    prefix: '',
    infix: '',
    postfix: '',
    length: 5
  },
  autoApprovalType: BpmAutoApproveType.NONE,
  titleSetting: { enable: false, title: '' },
  summarySetting: { enable: false, summary: [] },
  allowWithdrawTask: false,
  printTemplateSetting: { enable: false }
})

provide('processData', processData)
provide('modelData', formData)

const normalizeModelSettings = (data: any) => {
  if (!data) return data
  if (!data.processIdRule) {
    data.processIdRule = {
      enable: false,
      prefix: '',
      infix: '',
      postfix: '',
      length: 5
    }
  }
  if (data.autoApprovalType == null) {
    data.autoApprovalType = BpmAutoApproveType.NONE
  }
  if (!data.titleSetting) {
    data.titleSetting = { enable: false, title: '' }
  }
  if (!data.summarySetting) {
    data.summarySetting = { enable: false, summary: [] }
  }
  if (!data.printTemplateSetting) {
    data.printTemplateSetting = { enable: false }
  }
  if (data.allowWithdrawTask == null) {
    data.allowWithdrawTask = false
  }
  return data
}

const loadModel = async () => {
  loading.value = true
  ready.value = false
  try {
    const data = await ModelApi.getModel(String(props.modelId))
    formData.value = normalizeModelSettings(data)
    if (formData.value.type === BpmModelType.BPMN) {
      processData.value = formData.value.bpmnXml
    } else if (formData.value.type === BpmModelType.SIMPLE) {
      processData.value = formData.value.simpleModel
    }
    ready.value = true
    await nextTick()
    extraSettingsRef.value?.initData?.()
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!props.editable || saveLoading.value) return
  saveLoading.value = true
  try {
    await ModelApi.updateModel(formData.value)
    message.success('规则已保存，发布后生效')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

watch(
  () => props.modelId,
  () => {
    if (props.modelId) loadModel()
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.rule-settings {
  height: 100%;
  padding: 8px 0 32px;
  max-width: 760px;
  overflow: auto;

  &__footer {
    display: flex;
    justify-content: flex-start;
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid rgba(31, 35, 41, 0.08);
  }
}

.hint {
  margin: 0 0 8px;
  color: #8a909c;
  font-size: 13px;
}
</style>

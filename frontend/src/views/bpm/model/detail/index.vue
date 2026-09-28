<template>
  <ContentWrap>
    <div class="model-detail" v-loading="pageLoading">
      <template v-if="model">
        <!-- 头部 -->
        <div class="model-detail__header">
          <div class="model-detail__title-wrap">
            <el-button link class="back-btn" @click="handleBack">
              <Icon icon="ep:arrow-left" :size="18" />
            </el-button>
            <el-image v-if="model.icon" :src="model.icon" class="model-detail__icon" />
            <div v-else class="model-detail__icon model-detail__icon--fallback">
              {{ model.name ? model.name.slice(0, 1) : '流' }}
            </div>
            <div class="min-w-0">
              <div class="model-detail__name truncate">{{ model.name || '流程详情' }}</div>
              <div class="model-detail__meta">
                <span v-if="deployStatus === 'undeployed'" class="tag tag--warn">未部署</span>
                <span v-else-if="deployStatus === 'suspended'" class="tag">已停用</span>
                <span v-else class="tag tag--ok">
                  已发布
                  <template v-if="model.processDefinition?.version">
                    v{{ model.processDefinition.version }}
                  </template>
                </span>
                <span class="meta-text" v-if="model.key">流程标识 {{ model.key }}</span>
              </div>
            </div>
          </div>
          <div class="model-detail__actions">
            <el-button type="primary" :disabled="!canDeployModel" @click="handleDeploy">
              {{ hasDeployment ? '发布新版本' : '发布' }}
            </el-button>
            <el-button :disabled="!canEditModel" @click="openFullEdit">编辑流程</el-button>
          </div>
        </div>

        <!-- 页签：用 lazy，避免 v-if 反复销毁导致 parentNode 报错 -->
        <el-tabs v-model="activeTab" class="model-detail__tabs" lazy>
          <el-tab-pane v-if="canViewRecords" label="申请记录" name="records" lazy>
            <ApplicationRecords
              :process-definition-key="model.key"
              :deployed="hasDeployment"
              :model-name="model.name"
            />
          </el-tab-pane>
          <el-tab-pane label="模板设置" name="template" lazy>
            <TemplateSettings :model-id="model.id" :model="model" :editable="canEditModel" />
          </el-tab-pane>
          <el-tab-pane v-if="canEditModel" label="规则设置" name="rules" lazy>
            <RuleSettings :model-id="model.id" :editable="canEditModel" />
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-result v-else-if="loadError" icon="error" title="流程详情加载失败">
        <template #extra>
          <el-button type="primary" @click="loadModel">重新加载</el-button>
        </template>
      </el-result>
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { getCurrentUserId } from '@/utils/auth'
import { checkPermi } from '@/utils/permission'
import * as ModelApi from '@/api/bpm/model'
import ApplicationRecords from './ApplicationRecords.vue'
import TemplateSettings from './TemplateSettings.vue'
import RuleSettings from './RuleSettings.vue'

defineOptions({ name: 'BpmModelDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const { t } = useI18n()

const pageLoading = ref(true)
const loadError = ref(false)
const model = ref<any>()
const activeTab = ref((route.query.tab as string) || 'records')

const deployStatus = computed(() => {
  const pd = model.value?.processDefinition
  if (!pd) return 'undeployed'
  if (pd.suspensionState === 2) return 'suspended'
  return 'published'
})

const isModelManager = computed(() =>
  Boolean(model.value?.managerUserIds?.includes(getCurrentUserId()))
)
const canEditModel = computed(() => isModelManager.value && checkPermi(['bpm:model:update']))
const canDeployModel = computed(() => isModelManager.value && checkPermi(['bpm:model:deploy']))
const canViewRecords = computed(() => checkPermi(['bpm:process-instance:manager-query']))
const hasDeployment = computed(
  () => deployStatus.value === 'published' || deployStatus.value === 'suspended'
)

const normalizeActiveTab = () => {
  const availableTabs = [
    ...(canViewRecords.value ? ['records'] : []),
    'template',
    ...(canEditModel.value ? ['rules'] : [])
  ]
  if (!availableTabs.includes(activeTab.value)) {
    activeTab.value = availableTabs[0]
  }
}

const loadModel = async () => {
  const id = route.params.id as string
  if (!id) {
    pageLoading.value = false
    message.error('缺少流程模型编号')
    return
  }
  pageLoading.value = true
  loadError.value = false
  model.value = undefined
  try {
    model.value = await ModelApi.getModel(id)
    normalizeActiveTab()
  } catch {
    loadError.value = true
  } finally {
    pageLoading.value = false
  }
}

const handleBack = () => {
  router.push({ name: 'BpmModel' })
}

const openFullEdit = () => {
  if (!canEditModel.value || !model.value?.id) return
  router.push({
    name: 'BpmModelUpdate',
    params: { id: model.value.id, type: 'update' }
  })
}

const handleDeploy = async () => {
  if (!canDeployModel.value || !model.value?.id) return
  try {
    await message.confirm(
      hasDeployment.value ? '是否确认发布该流程的新版本？' : '是否确认发布该流程？'
    )
  } catch {
    return
  }
  try {
    await ModelApi.deployModel(model.value.id)
    message.success(t('发布成功') || '发布成功')
    await loadModel()
  } catch (error: any) {
    message.error(error?.message || '发布失败')
  }
}

watch(activeTab, (tab) => {
  if (route.query.tab === tab) return
  nextTick(() => {
    router.replace({
      query: { ...route.query, tab }
    })
  })
})

watch([canViewRecords, canEditModel], normalizeActiveTab)

watch(
  () => route.params.id,
  () => loadModel(),
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.model-detail {
  display: flex;
  flex-direction: column;
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) -
      var(--app-content-padding) * 2 - 35px
  );
  min-height: 480px;
}

.model-detail__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.model-detail__title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.back-btn {
  padding: 4px !important;
  color: #646a73 !important;
}

.model-detail__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  flex-shrink: 0;
  object-fit: cover;

  &--fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-weight: 600;
    background: var(--brand-gradient, linear-gradient(135deg, #6d5efc, #4f8cff));
  }
}

.model-detail__name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.3;
}

.model-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(100, 106, 115, 0.12);
  color: #646a73;

  &--ok {
    background: rgba(52, 199, 89, 0.12);
    color: #1f8f3a;
  }

  &--warn {
    background: rgba(255, 149, 0, 0.14);
    color: #c56a00;
  }
}

.meta-text {
  font-size: 12px;
  color: #8a909c;
}

.model-detail__tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;

  :deep(.el-tabs__header) {
    flex-shrink: 0;
    margin-bottom: 8px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow: hidden;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
}
</style>

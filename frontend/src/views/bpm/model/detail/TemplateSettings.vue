<template>
  <div class="template-settings">
    <p class="hint">
      {{
        editable
          ? '配置流程的基本信息、表单和审批节点。点进对应项即可编辑。'
          : '当前为只读模式，你没有该流程的编辑权限。'
      }}
    </p>
    <div class="setting-list">
      <div
        class="setting-item"
        :class="{ 'is-disabled': !editable }"
        role="button"
        :tabindex="editable ? 0 : -1"
        :aria-disabled="!editable"
        @click="openDesign(0)"
        @keydown.enter="openDesign(0)"
        @keydown.space.prevent="openDesign(0)"
      >
        <div class="setting-item__icon">
          <Icon icon="ep:document" :size="22" />
        </div>
        <div class="setting-item__body">
          <div class="setting-item__title">基本信息</div>
          <div class="setting-item__desc"> 名称、图标、分类、发起人范围、管理员 </div>
        </div>
        <Icon icon="ep:arrow-right" class="setting-item__arrow" />
      </div>
      <div
        class="setting-item"
        :class="{ 'is-disabled': !editable }"
        role="button"
        :tabindex="editable ? 0 : -1"
        :aria-disabled="!editable"
        @click="openDesign(1)"
        @keydown.enter="openDesign(1)"
        @keydown.space.prevent="openDesign(1)"
      >
        <div class="setting-item__icon">
          <Icon icon="ep:edit" :size="22" />
        </div>
        <div class="setting-item__body">
          <div class="setting-item__title">表单设计</div>
          <div class="setting-item__desc">配置申请人需要填写的表单字段</div>
        </div>
        <Icon icon="ep:arrow-right" class="setting-item__arrow" />
      </div>
      <div
        class="setting-item"
        :class="{ 'is-disabled': !editable }"
        role="button"
        :tabindex="editable ? 0 : -1"
        :aria-disabled="!editable"
        @click="openDesign(2)"
        @keydown.enter="openDesign(2)"
        @keydown.space.prevent="openDesign(2)"
      >
        <div class="setting-item__icon">
          <Icon icon="ep:share" :size="22" />
        </div>
        <div class="setting-item__body">
          <div class="setting-item__title">流程设计</div>
          <div class="setting-item__desc">配置审批节点、条件分支与抄送人</div>
        </div>
        <Icon icon="ep:arrow-right" class="setting-item__arrow" />
      </div>
    </div>

    <div class="summary" v-if="model">
      <div class="summary__title">当前模板摘要</div>
      <div class="summary__row">
        <span class="label">流程名称</span>
        <span>{{ model.name || '-' }}</span>
      </div>
      <div class="summary__row">
        <span class="label">流程标识</span>
        <span>{{ model.key || '-' }}</span>
      </div>
      <div class="summary__row">
        <span class="label">表单类型</span>
        <span>{{ formTypeText }}</span>
      </div>
      <div class="summary__row">
        <span class="label">发布状态</span>
        <span>{{ deployText }}</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { BpmModelFormType } from '@/utils/constants'

defineOptions({ name: 'BpmModelTemplateSettings' })

const props = withDefaults(
  defineProps<{
    modelId: number | string
    model?: any
    editable?: boolean
  }>(),
  {
    editable: false
  }
)

const router = useRouter()

const formTypeText = computed(() => {
  if (!props.model) return '-'
  return props.model.formType === BpmModelFormType.NORMAL ? '流程表单' : '业务表单'
})

const deployText = computed(() => {
  if (!props.model?.processDefinition) return '未部署'
  if (props.model.processDefinition.suspensionState === 2) return '已停用'
  return `已发布 (v${props.model.processDefinition.version ?? '-'})`
})

const openDesign = (step: number) => {
  if (!props.editable) return
  router.push({
    name: 'BpmModelUpdate',
    params: { id: props.modelId, type: 'update' },
    query: { step: String(step) }
  })
}
</script>

<style lang="scss" scoped>
.template-settings {
  height: 100%;
  max-width: 720px;
  padding: 8px 0 24px;
  overflow: auto;
}

.hint {
  margin: 0 0 16px;
  color: #8a909c;
  font-size: 13px;
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--glass-border, rgba(109, 94, 252, 0.12));
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease;

  &:hover {
    background: rgba(109, 94, 252, 0.06);
    border-color: rgba(109, 94, 252, 0.28);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary-light-5);
    outline-offset: 2px;
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.62;

    &:hover {
      background: rgba(255, 255, 255, 0.55);
      border-color: var(--glass-border, rgba(109, 94, 252, 0.12));
    }
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-color-primary);
    background: rgba(109, 94, 252, 0.1);
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
  }

  &__desc {
    margin-top: 4px;
    font-size: 13px;
    color: #8a909c;
  }

  &__arrow {
    color: #c0c4cc;
    flex-shrink: 0;
  }
}

.summary {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(31, 35, 41, 0.08);

  &__title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1f2329;
  }

  &__row {
    display: flex;
    gap: 16px;
    padding: 8px 0;
    font-size: 14px;
    color: #1f2329;

    .label {
      width: 88px;
      flex-shrink: 0;
      color: #8a909c;
    }
  }
}
</style>

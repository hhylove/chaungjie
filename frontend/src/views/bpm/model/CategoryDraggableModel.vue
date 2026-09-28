<template>
  <div class="category-block">
    <!-- 分类头部 -->
    <div class="category-header" v-memo="[categoryInfo.name, isCategorySorting, modelList.length]">
      <div class="flex items-center min-w-0">
        <el-tooltip content="拖动排序" v-if="isCategorySorting">
          <Icon
            :size="20"
            icon="ic:round-drag-indicator"
            class="ml-4px category-drag-icon cursor-move text-#8a909c"
          />
        </el-tooltip>
        <h3 class="ml-12px mr-8px text-16px font-600 truncate">{{ categoryInfo.name }}</h3>
        <span class="category-count">{{ categoryInfo.modelList?.length || 0 }}</span>
      </div>

      <div class="flex-1 flex items-center" v-show="!isCategorySorting">
        <div
          v-if="categoryInfo.modelList.length > 0"
          class="ml-12px flex items-center cursor-pointer transition-transform duration-300"
          :class="isExpand ? 'rotate-180' : 'rotate-0'"
          @click="isExpand = !isExpand"
        >
          <Icon icon="ep:arrow-down-bold" color="#999" />
        </div>

        <div class="ml-auto flex items-center gap-8px pr-8px">
          <template v-if="!isModelSorting">
            <el-button
              v-if="categoryInfo.modelList.length > 0"
              link
              type="info"
              @click.stop="handleModelSort"
            >
              <Icon icon="fa:sort-amount-desc" class="mr-4px" />
              排序
            </el-button>
            <el-button v-else link type="primary" @click.stop="openModelForm('create')">
              <Icon icon="ep:plus" class="mr-4px" />
              新建流程
            </el-button>
            <el-dropdown
              @command="(command) => handleCategoryCommand(command, categoryInfo)"
              placement="bottom"
            >
              <el-button link type="info">
                <Icon icon="ep:setting" class="mr-4px" />
                分类
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="handleRename">重命名</el-dropdown-item>
                  <el-dropdown-item command="handleDeleteCategory">删除该类</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button @click.stop="handleModelSortCancel">取 消</el-button>
            <el-button type="primary" @click.stop="handleModelSortSubmit">保存排序</el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 模型列表 -->
    <el-collapse-transition>
      <div v-show="isExpand" class="category-body">
        <el-empty
          v-if="!modelList?.length"
          description="该分类下暂无流程"
          :image-size="64"
        />
        <div v-else :class="['model-row-list', `model-row-list-${categoryInfo.id}`]">
          <div
            v-for="row in modelList"
            :key="row.id"
            class="model-row"
            :class="{ 'is-sorting': isModelSorting }"
          >
            <div class="model-row__main">
              <el-tooltip content="拖动排序" v-if="isModelSorting">
                <Icon
                  icon="ic:round-drag-indicator"
                  class="drag-icon cursor-move text-#8a909c mr-8px shrink-0"
                />
              </el-tooltip>
              <el-image
                v-if="row.icon"
                :src="row.icon"
                class="model-row__icon shrink-0 cursor-pointer"
                @click="openModelDetail(row)"
              />
              <div
                v-else
                class="model-row__icon model-row__icon--fallback shrink-0 cursor-pointer"
                @click="openModelDetail(row)"
              >
                {{ subString(row.name, 0, 1) }}
              </div>
              <div class="min-w-0 model-row__clickable" @click="openModelDetail(row)">
                <div class="flex items-center gap-8px min-w-0">
                  <span class="model-row__title truncate" :title="row.name">{{ row.name }}</span>
                  <span
                    v-if="row.processDefinition?.suspensionState === 2"
                    class="model-row__tag"
                  >
                    已停用
                  </span>
                  <span v-else-if="!row.processDefinition" class="model-row__tag model-row__tag--warn">
                    未部署
                  </span>
                </div>
                <div v-if="row.description" class="model-row__desc truncate" :title="row.description">
                  {{ row.description }}
                </div>
              </div>
            </div>

            <div class="model-row__scope">
              <span class="truncate" :title="getVisibilityText(row)">{{ getVisibilityText(row) }}</span>
              <el-button
                v-show="!isModelSorting"
                link
                type="primary"
                class="model-row__scope-edit"
                :disabled="!isManagerUser(row) && !hasPermiUpdate"
                @click="openModelForm('update', row.id)"
              >
                修改
              </el-button>
            </div>

            <div class="model-row__ops" v-show="!isModelSorting">
              <el-button
                link
                type="primary"
                :disabled="!isManagerUser(row) && !hasPermiUpdate"
                @click="openModelForm('update', row.id)"
              >
                编辑
              </el-button>
              <el-dropdown
                v-if="hasPermiMore || hasPermiDeploy"
                trigger="click"
                @command="(command) => handleModelCommand(command, row)"
              >
                <el-button link type="primary">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      command="handleDeploy"
                      :disabled="!isManagerUser(row) && !hasPermiDeploy"
                    >
                      发布
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleCopy"
                      :disabled="!isManagerUser(row) && !hasPermiUpdate"
                    >
                      复制
                    </el-dropdown-item>
                    <el-dropdown-item command="handleFormDetail">表单信息</el-dropdown-item>
                    <el-dropdown-item command="handleDefinitionList" v-if="hasPermiPdQuery">
                      历史版本
                    </el-dropdown-item>
                    <el-dropdown-item command="handleExport" v-if="hasPermiExport">
                      导出
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleReport"
                      v-if="
                        checkPermi(['bpm:process-instance:manager-query']) && row.processDefinition
                      "
                      :disabled="!isManagerUser(row)"
                    >
                      报表
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleChangeState"
                      v-if="hasPermiUpdate && row.processDefinition"
                      :disabled="!isManagerUser(row)"
                      divided
                    >
                      {{ row.processDefinition.suspensionState === 1 ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleClean"
                      v-if="checkPermi(['bpm:model:clean'])"
                      :disabled="!isManagerUser(row)"
                    >
                      清理
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleDelete"
                      v-if="hasPermiDelete"
                      :disabled="!isManagerUser(row)"
                    >
                      <span class="text-[var(--el-color-danger)]">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </el-collapse-transition>
  </div>

  <!-- 弹窗：重命名分类 -->
  <Dialog :fullscreen="false" class="rename-dialog" v-model="renameCategoryVisible" width="400">
    <template #title>
      <div class="pl-10px font-bold text-18px">重命名分类</div>
    </template>
    <div class="px-30px">
      <el-input v-model="renameCategoryForm.name" />
    </div>
    <template #footer>
      <div class="pr-25px pb-25px">
        <el-button @click="renameCategoryVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleRenameConfirm">确 定</el-button>
      </div>
    </template>
  </Dialog>

  <!-- 弹窗：表单详情 -->
  <Dialog title="表单详情" :fullscreen="true" v-model="formDetailVisible">
    <form-create :rule="formDetailPreview.rule" :option="formDetailPreview.option" />
  </Dialog>
</template>

<script lang="ts" setup>
import { CategoryApi, CategoryVO } from '@/api/bpm/category'
import Sortable from 'sortablejs'
import * as ModelApi from '@/api/bpm/model'
import * as FormApi from '@/api/bpm/form'
import { setConfAndFields2 } from '@/utils/formCreate'
import { BpmModelFormType } from '@/utils/constants'
import { checkPermi } from '@/utils/permission'
import { getCurrentUserId } from '@/utils/auth'
import { cloneDeep, isEqual } from 'lodash-es'
import { useDebounceFn } from '@vueuse/core'
import { subString } from '@/utils/index'
import download from '@/utils/download'

defineOptions({ name: 'CategoryDraggableModel' })

interface UserInfo {
  nickname: string
  [key: string]: any
}

interface ProcessDefinition {
  deploymentTime: string
  version: number
  suspensionState: number
}

interface ModelInfo {
  id: number
  name: string
  icon?: string
  startUsers?: UserInfo[]
  startDepts?: Array<{ name: string }>
  processDefinition?: ProcessDefinition
  formType?: number
  formId?: number
  formName?: string
  formCustomCreatePath?: string
  managerUserIds?: number[]
  type?: number
  [key: string]: any
}

interface CategoryInfoProps {
  id: number
  name: string
  modelList: ModelInfo[]
}

const props = defineProps<{
  categoryInfo: CategoryInfoProps
  isCategorySorting: boolean
}>()

const emit = defineEmits(['success'])
const message = useMessage()
const { t } = useI18n()
const { push } = useRouter()
const router = useRouter()

const isModelSorting = ref(false)
const originalData = ref<ModelInfo[]>([])
const modelList = ref<ModelInfo[]>([])
const isExpand = ref(false)
let sortableInstance: Sortable | null = null

const hasPermiUpdate = computed(() => checkPermi(['bpm:model:update']))
const hasPermiDelete = computed(() => checkPermi(['bpm:model:delete']))
const hasPermiDeploy = computed(() => checkPermi(['bpm:model:deploy']))
const hasPermiExport = computed(() => checkPermi(['bpm:model:export']))
const hasPermiMore = computed(() =>
  checkPermi([
    'bpm:process-definition:query',
    'bpm:model:update',
    'bpm:model:delete',
    'bpm:model:export'
  ])
)
const hasPermiPdQuery = computed(() => checkPermi(['bpm:process-definition:query']))

const getVisibilityText = (row: ModelInfo) => {
  if (!row.startUsers?.length && !row.startDepts?.length) {
    return '全公司可见'
  }
  if (row.startUsers?.length === 1) {
    return row.startUsers[0].nickname
  }
  if (row.startDepts?.length === 1) {
    return row.startDepts[0].name
  }
  if (row.startDepts && row.startDepts.length > 1) {
    return `${row.startDepts[0].name}等 ${row.startDepts.length} 个部门`
  }
  if (row.startUsers && row.startUsers.length > 1) {
    return `${row.startUsers[0].nickname}等 ${row.startUsers.length} 人`
  }
  return '全部可见'
}

const handleModelCommand = (command: string, row: any) => {
  switch (command) {
    case 'handleDeploy':
      handleDeploy(row)
      break
    case 'handleCopy':
      openModelForm('copy', row.id)
      break
    case 'handleFormDetail':
      handleFormDetail(row)
      break
    case 'handleDefinitionList':
      handleDefinitionList(row)
      break
    case 'handleDelete':
      handleDelete(row)
      break
    case 'handleExport':
      handleExport(row)
      break
    case 'handleChangeState':
      handleChangeState(row)
      break
    case 'handleClean':
      handleClean(row)
      break
    case 'handleReport':
      router.push({
        name: 'BpmProcessInstanceReport',
        query: {
          processDefinitionId: row.processDefinition.id,
          processDefinitionKey: row.key
        }
      })
      break
    default:
      break
  }
}

const handleExport = async (row: any) => {
  const data = await ModelApi.exportModel(row.id)
  download.json(new Blob([JSON.stringify(data, null, 2)]), `${row.key || row.name || 'model'}.json`)
  message.success('导出成功')
}

const handleCategoryCommand = async (command: string, row: any) => {
  switch (command) {
    case 'handleRename':
      renameCategoryForm.value = await CategoryApi.getCategory(row.id)
      renameCategoryVisible.value = true
      break
    case 'handleDeleteCategory':
      await handleDeleteCategory()
      break
    default:
      break
  }
}

const handleDelete = async (row: any) => {
  try {
    await message.delConfirm()
    await ModelApi.deleteModel(row.id)
    message.success(t('common.delSuccess'))
    emit('success')
  } catch {}
}

const handleClean = async (row: any) => {
  try {
    await message.confirm('是否确认清理流程名字为"' + row.name + '"的数据项?')
    await ModelApi.cleanModel(row.id)
    message.success('清理成功')
    emit('success')
  } catch {}
}

const handleChangeState = async (row: any) => {
  const state = row.processDefinition.suspensionState
  const newState = state === 1 ? 2 : 1
  try {
    const statusState = state === 1 ? '停用' : '启用'
    await message.confirm('是否确认' + statusState + '流程名字为"' + row.name + '"的数据项?')
    await ModelApi.updateModelState(row.id, newState)
    message.success(statusState + '成功')
    emit('success')
  } catch {}
}

const handleDeploy = async (row: any) => {
  try {
    await message.confirm('是否确认发布该流程？')
    await ModelApi.deployModel(row.id)
    message.success(t('发布成功'))
    emit('success')
  } catch {}
}

const handleDefinitionList = (row: any) => {
  push({
    name: 'BpmProcessDefinition',
    query: { key: row.key }
  })
}

const formDetailVisible = ref(false)
const formDetailPreview = ref({
  rule: [],
  option: {}
})
const handleFormDetail = async (row: any) => {
  if (row.formType == BpmModelFormType.NORMAL) {
    const data = await FormApi.getForm(row.formId)
    setConfAndFields2(formDetailPreview, data.conf, data.fields)
    formDetailVisible.value = true
  } else {
    await push({ path: row.formCustomCreatePath })
  }
}

const isManagerUser = (row: any) => {
  const userId = getCurrentUserId()
  return row.managerUserIds && row.managerUserIds.includes(userId)
}

const destroySortable = () => {
  sortableInstance?.destroy()
  sortableInstance = null
}

const handleModelSort = () => {
  if (isModelSorting.value) {
    handleModelSortCancel()
  } else {
    originalData.value = cloneDeep(props.categoryInfo.modelList)
    isModelSorting.value = true
    nextTick(() => initSort())
  }
}

const handleModelSortSubmit = async () => {
  const ids = modelList.value.map((item: any) => item.id)
  await ModelApi.updateModelSortBatch(ids)
  destroySortable()
  isModelSorting.value = false
  message.success('排序模型成功')
  emit('success')
}

const handleModelSortCancel = () => {
  modelList.value = cloneDeep(originalData.value)
  destroySortable()
  isModelSorting.value = false
}

const initSort = useDebounceFn(() => {
  destroySortable()
  const grid = document.querySelector(`.model-row-list-${props.categoryInfo.id}`)
  if (!grid) return

  sortableInstance = Sortable.create(grid as HTMLElement, {
    animation: 150,
    draggable: '.model-row',
    handle: '.drag-icon',
    onEnd: ({ newDraggableIndex, oldDraggableIndex }) => {
      if (oldDraggableIndex !== newDraggableIndex) {
        modelList.value.splice(
          newDraggableIndex ?? 0,
          0,
          modelList.value.splice(oldDraggableIndex ?? 0, 1)[0]
        )
      }
    }
  })
}, 200)

const updateModeList = useDebounceFn(() => {
  const newModelList = props.categoryInfo.modelList
  if (!isEqual(modelList.value, newModelList)) {
    modelList.value = cloneDeep(newModelList)
    if (newModelList?.length > 0) {
      isExpand.value = true
    }
  }
}, 100)

const renameCategoryVisible = ref(false)
const renameCategoryForm = ref({
  name: ''
})
const handleRenameConfirm = async () => {
  if (renameCategoryForm.value?.name.length === 0) {
    return message.warning('请输入名称')
  }
  await CategoryApi.updateCategory(renameCategoryForm.value as CategoryVO)
  message.success('重命名成功')
  renameCategoryVisible.value = false
  emit('success')
}

const handleDeleteCategory = async () => {
  try {
    if (props.categoryInfo.modelList.length > 0) {
      return message.warning('该分类下仍有流程定义,不允许删除')
    }
    await message.confirm('确认删除分类吗?')
    await CategoryApi.deleteCategory(props.categoryInfo.id)
    message.success(t('common.delSuccess'))
    emit('success')
  } catch {}
}

const openModelForm = async (type: string, id?: number) => {
  if (type === 'create') {
    await push({ name: 'BpmModelCreate' })
  } else {
    await push({
      name: 'BpmModelUpdate',
      params: { id, type }
    })
  }
}

const openModelDetail = async (row: any) => {
  if (isModelSorting.value) return
  await push({
    name: 'BpmModelDetail',
    params: { id: row.id }
  })
}

watchEffect(() => {
  if (props.categoryInfo?.modelList) {
    updateModeList()
  }
  if (props.isCategorySorting) {
    isExpand.value = false
  }
})

onBeforeUnmount(() => {
  destroySortable()
})
</script>

<style lang="scss">
.rename-dialog.el-dialog {
  padding: 0 !important;

  .el-dialog__header {
    border-bottom: none;
  }

  .el-dialog__footer {
    border-top: none !important;
  }
}
</style>

<style lang="scss" scoped>
.category-block {
  overflow: hidden;
}

.category-header {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 16px;
  background: #f7f8fa;
}

.category-count {
  font-size: 13px;
  color: #8f959e;
}

.category-body {
  padding: 0;
}

.model-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 1fr) 120px;
  gap: 16px;
  align-items: center;
  min-height: 68px;
  padding: 10px 20px;
  background: #fff;
  border-top: 1px solid #f0f1f2;

  &:hover {
    background: #f7f8fa;

    .model-row__scope-edit {
      opacity: 1;
    }
  }

  &.is-sorting {
    cursor: grab;
  }

  &__main {
    display: flex;
    align-items: center;
    min-width: 0;
  }

  &__icon {
    width: 36px;
    height: 36px;
    margin-right: 12px;
    overflow: hidden;
    border-radius: 8px;

    &--fallback {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      color: #fff;
      background: #3370ff;
    }
  }

  &__title {
    font-size: 14px;
    color: #1f2329;
  }

  &__clickable {
    cursor: pointer;

    &:hover .model-row__title {
      color: var(--el-color-primary);
    }
  }

  &__desc {
    margin-top: 2px;
    font-size: 12px;
    color: #8f959e;
  }

  &__tag {
    flex-shrink: 0;
    padding: 0 6px;
    font-size: 12px;
    line-height: 20px;
    color: #646a73;
    background: #f2f3f5;
    border-radius: 4px;

    &--warn {
      color: #b26206;
      background: #fff7e8;
    }
  }

  &__scope {
    display: flex;
    gap: 8px;
    align-items: center;
    min-width: 0;
    font-size: 13px;
    color: #646a73;
  }

  &__scope-edit {
    flex-shrink: 0;
    opacity: 0;
  }

  &__ops {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}

.dark {
  .category-header {
    background: rgba(255, 255, 255, 0.04);
  }

  .model-row {
    background: transparent;
    border-top-color: rgba(255, 255, 255, 0.06);

    &:hover {
      background: rgba(255, 255, 255, 0.04);
    }
  }

  .model-row__title {
    color: var(--el-text-color-primary);
  }
}

@media (max-width: 900px) {
  .model-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .model-row__ops {
    justify-content: flex-start;
  }

  .model-row__scope-edit {
    opacity: 1;
  }
}
</style>

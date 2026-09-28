<template>
  <div
    v-if="attachments.length || signList.length"
    class="evidence-cell"
    @click.stop
  >
    <!-- 图片直接展示 -->
    <div v-if="imageAttachments.length" class="evidence-images">
      <el-image
        v-for="(image, index) in imageAttachments"
        :key="image"
        class="evidence-images__item"
        :src="image"
        :preview-src-list="imageAttachments"
        :initial-index="index"
        fit="cover"
        preview-teleported
      />
    </div>

    <!-- 非图片附件仍用列表 -->
    <el-popover v-if="fileAttachments.length" placement="top" :width="320" trigger="click">
      <template #reference>
        <el-button link type="primary">
          <Icon icon="ep:paperclip" class="mr-4px" />
          {{ fileAttachments.length }} 个附件
        </el-button>
      </template>
      <div class="attachment-list">
        <div v-for="attachment in fileAttachments" :key="attachment" class="attachment-item">
          <div class="attachment-item__icon">
            <Icon icon="ep:document" />
          </div>
          <a
            class="attachment-item__name"
            :href="attachment"
            :title="getFileNameFromUrl(attachment)"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ getFileNameFromUrl(attachment) }}
          </a>
        </div>
      </div>
    </el-popover>

    <div v-if="signList.length" class="evidence-signs">
      <span class="evidence-signs__label">签名</span>
      <el-image
        v-for="sign in signList"
        :key="sign"
        class="evidence-signs__item"
        :src="sign"
        :preview-src-list="signList"
        fit="contain"
        preview-teleported
      />
    </div>
  </div>
  <span v-else class="text-[var(--el-text-color-placeholder)]">-</span>
</template>

<script lang="ts" setup>
import { getFileNameFromUrl, isImage } from '@/utils/file'

defineOptions({ name: 'BpmTaskEvidenceCell' })

const props = withDefaults(
  defineProps<{
    attachments?: string[]
    signPicUrl?: string
    signPicUrls?: string[]
  }>(),
  {
    attachments: () => [],
    signPicUrls: () => []
  }
)

const imageAttachments = computed(() => props.attachments.filter((item) => isImage(item)))
const fileAttachments = computed(() => props.attachments.filter((item) => !isImage(item)))

const signList = computed(() => {
  const signs = [...props.signPicUrls]
  if (props.signPicUrl && !signs.includes(props.signPicUrl)) {
    signs.unshift(props.signPicUrl)
  }
  return signs.filter(Boolean)
})
</script>

<style lang="scss" scoped>
.evidence-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.evidence-images__item {
  width: 56px;
  height: 56px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: zoom-in;
  background: #fff;
}

.evidence-signs {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.evidence-signs__label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.evidence-signs__item {
  width: 72px;
  height: 32px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: #fff;
}

.attachment-list {
  max-height: 260px;
  overflow-x: hidden;
  overflow-y: auto;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 6px 0;

  & + & {
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.attachment-item__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--el-color-primary);
  font-size: 18px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.attachment-item__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--el-color-primary);
  font-size: 13px;
  line-height: 1.4;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover {
    color: var(--el-color-primary-light-3);
  }
}
</style>

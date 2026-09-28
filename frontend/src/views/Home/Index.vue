<template>
  <div class="workplace">
    <!-- 问候条 -->
    <el-card shadow="never" class="workplace-hero mb-16px">
      <el-skeleton :loading="loading" animated>
        <div class="flex flex-wrap items-center justify-between gap-16px">
          <div class="flex items-center min-w-0">
            <el-avatar :src="avatar" :size="64" class="mr-16px shrink-0">
              <img src="@/assets/imgs/avatar.png" alt="" />
            </el-avatar>
            <div class="min-w-0">
              <div class="text-20px font-600 text-[var(--el-text-color-primary)] truncate">
                {{ greeting }}，{{ username }}
              </div>
              <div class="mt-8px text-13px text-[var(--el-text-color-secondary)]">
                {{ todayText }} · 今天也要高效推进工作
              </div>
            </div>
          </div>
          <div class="flex items-center gap-8px flex-wrap">
            <div
              class="workplace-stat"
              role="button"
              tabindex="0"
              @click="goTodo"
              @keyup.enter="goTodo"
            >
              <div class="workplace-stat__label">待办</div>
              <div class="workplace-stat__value">{{ summary.todo }}</div>
            </div>
            <div
              class="workplace-stat"
              role="button"
              tabindex="0"
              @click="goNotify"
              @keyup.enter="goNotify"
            >
              <div class="workplace-stat__label">未读消息</div>
              <div class="workplace-stat__value">{{ summary.unread }}</div>
            </div>
            <div class="workplace-stat workplace-stat--muted">
              <div class="workplace-stat__label">公告</div>
              <div class="workplace-stat__value">{{ summary.notice }}</div>
            </div>
          </div>
        </div>
      </el-skeleton>
    </el-card>

    <el-row :gutter="16">
      <!-- 待我处理 -->
      <el-col :xl="14" :lg="14" :md="24" :sm="24" :xs="24" class="mb-16px">
        <el-card shadow="never" class="workplace-panel h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-600">待我处理</span>
              <el-link type="primary" :underline="false" @click="goTodo">查看全部</el-link>
            </div>
          </template>
          <el-skeleton :loading="loading" animated>
            <el-empty v-if="!todoList.length" description="暂无待办，去忙别的吧" :image-size="72" />
            <div v-else class="workplace-list">
              <div
                v-for="item in todoList"
                :key="item.id"
                class="workplace-list__item"
                @click="openTodo(item)"
              >
                <div class="min-w-0 flex-1">
                  <div class="text-14px font-500 truncate text-[var(--el-text-color-primary)]">
                    {{ item.processInstance?.name || item.name || '审批任务' }}
                  </div>
                  <div class="mt-6px text-12px text-[var(--el-text-color-secondary)] truncate">
                    {{ item.name }}
                    <template v-if="item.processInstance?.startUser?.nickname">
                      · 发起人 {{ item.processInstance.startUser.nickname }}
                    </template>
                  </div>
                </div>
                <div class="shrink-0 text-12px text-[var(--el-text-color-placeholder)] ml-12px">
                  {{ formatTime(item.createTime, 'MM-dd HH:mm') }}
                </div>
              </div>
            </div>
          </el-skeleton>
        </el-card>
      </el-col>

      <!-- 消息与公告 -->
      <el-col :xl="10" :lg="10" :md="24" :sm="24" :xs="24" class="mb-16px">
        <el-card shadow="never" class="workplace-panel h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-600">消息与公告</span>
              <el-link type="primary" :underline="false" @click="goNotify">我的站内信</el-link>
            </div>
          </template>
          <el-skeleton :loading="loading" animated>
            <el-tabs v-model="infoTab" class="workplace-tabs">
              <el-tab-pane :label="`未读消息 (${summary.unread})`" name="message">
                <el-empty
                  v-if="!messageList.length"
                  description="没有未读消息"
                  :image-size="64"
                />
                <div v-else class="workplace-list">
                  <div
                    v-for="item in messageList"
                    :key="item.id"
                    class="workplace-list__item"
                    @click="goNotify"
                  >
                    <div class="min-w-0 flex-1">
                      <div class="text-14px truncate">
                        {{ item.templateNickname || '系统通知' }}
                      </div>
                      <div
                        class="mt-6px text-12px text-[var(--el-text-color-secondary)] line-clamp-2"
                      >
                        {{ item.templateContent }}
                      </div>
                    </div>
                    <div class="shrink-0 text-12px text-[var(--el-text-color-placeholder)] ml-12px">
                      {{ formatTime(item.createTime, 'MM-dd') }}
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane :label="`公告 (${summary.notice})`" name="notice">
                <el-empty v-if="!noticeList.length" description="暂无公告" :image-size="64" />
                <div v-else class="workplace-list">
                  <div v-for="item in noticeList" :key="item.id" class="workplace-list__item">
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-8px">
                        <span class="dict-tag-capsule dict-tag-capsule--primary !h-22px !text-12px">
                          <span class="dict-tag-capsule__dot"></span>
                          公告
                        </span>
                        <span class="text-14px truncate">{{ item.title }}</span>
                      </div>
                      <div class="mt-8px text-12px text-[var(--el-text-color-placeholder)]">
                        {{ formatTime(item.createTime, 'yyyy-MM-dd HH:mm') }}
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-skeleton>
        </el-card>
      </el-col>
    </el-row>

    <!-- 常用入口 -->
    <el-card shadow="never" class="workplace-panel">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-600">常用功能</span>
          <span class="text-12px text-[var(--el-text-color-placeholder)]">按你的菜单权限展示</span>
        </div>
      </template>
      <el-skeleton :loading="loading" animated>
        <el-empty v-if="!shortcuts.length" description="暂无可用功能入口" :image-size="64" />
        <div v-else class="workplace-shortcuts">
          <div
            v-for="item in shortcuts"
            :key="item.path"
            class="workplace-shortcut"
            @click="goPath(item.path)"
          >
            <div class="workplace-shortcut__icon" :style="{ background: item.bg }">
              <Icon :icon="item.icon" :size="20" color="#fff" />
            </div>
            <div class="workplace-shortcut__name truncate" :title="item.name">{{ item.name }}</div>
          </div>
        </div>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { formatTime } from '@/utils'
import { pathResolve } from '@/utils/routerHelper'
import { isUrl } from '@/utils/is'
import { CommonStatusEnum } from '@/utils/constants'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import * as TaskApi from '@/api/bpm/task'
import * as NotifyMessageApi from '@/api/system/notify/message'
import * as NoticeApi from '@/api/system/notice'
import type { NoticeVO } from '@/api/system/notice'
import type { NotifyMessageVO } from '@/api/system/notify/message'

defineOptions({ name: 'Index' })

const { t } = useI18n()
const { push } = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const loading = ref(true)
const infoTab = ref('message')
const avatar = computed(() => userStore.getUser.avatar)
const username = computed(() => userStore.getUser.nickname || '用户')

const summary = reactive({
  todo: 0,
  unread: 0,
  notice: 0
})

const todoList = ref<any[]>([])
const messageList = ref<NotifyMessageVO[]>([])
const noticeList = ref<NoticeVO[]>([])

type ShortcutItem = {
  name: string
  path: string
  icon: string
  bg: string
}

const shortcuts = ref<ShortcutItem[]>([])

const SHORTCUT_COLORS = [
  'linear-gradient(135deg, #7c5cff, #4f7cff)',
  'linear-gradient(135deg, #4f7cff, #38bdf8)',
  'linear-gradient(135deg, #8b5cf6, #6d5efc)',
  'linear-gradient(135deg, #06b6d4, #3b82f6)',
  'linear-gradient(135deg, #6366f1, #a78bfa)',
  'linear-gradient(135deg, #2563eb, #7c5cff)'
]

/** 优先展示的常用入口（按路径匹配用户菜单） */
const SHORTCUT_PRESETS: Array<{ match: RegExp; name?: string; icon?: string }> = [
  { match: /\/bpm\/task\/todo/, name: '待办审批', icon: 'ep:checked' },
  { match: /\/crm\/(customer|backlog)/, icon: 'ep:user' },
  { match: /\/erp\/home/, icon: 'ep:office-building' },
  { match: /\/wms\/home/, icon: 'ep:box' },
  { match: /\/mall\/home/, icon: 'ep:shopping-cart' },
  { match: /\/iot\/home/, icon: 'ep:cpu' },
  { match: /\/fms\/home/, icon: 'ep:coin' },
  { match: /\/mes\/home/, icon: 'ep:set-up' },
  { match: /\/ai\/chat/, icon: 'ep:chat-dot-round' },
  { match: /\/system\/user/, icon: 'ep:avatar' },
  { match: /\/system\/notice/, icon: 'ep:bell' },
  { match: /\/infra\/job/, icon: 'ep:timer' }
]

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayText = computed(() => {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  return `${formatTime(now, 'yyyy年MM月dd日')} 星期${week}`
})

const collectMenuLeaves = (
  routes: AppRouteRecordRaw[],
  parentPath = '/'
): Array<{ title: string; path: string; icon: string }> => {
  const leaves: Array<{ title: string; path: string; icon: string }> = []
  for (const route of routes) {
    if (route.meta?.hidden) continue
    const fullPath = isUrl(route.path) ? route.path : pathResolve(parentPath, route.path)
    const children = (route.children || []).filter((child) => !child.meta?.hidden)
    if (children.length) {
      leaves.push(...collectMenuLeaves(children, fullPath))
      continue
    }
    if (!route.meta?.title || isUrl(fullPath)) continue
    if (fullPath === '/' || fullPath === '/index' || fullPath.startsWith('/user/')) continue
    leaves.push({
      title: t(route.meta.title as string),
      path: fullPath,
      icon: (route.meta.icon as string) || 'ep:menu'
    })
  }
  return leaves
}

const buildShortcuts = () => {
  const leaves = collectMenuLeaves(permissionStore.getRouters)
  const picked: ShortcutItem[] = []
  const used = new Set<string>()

  for (const preset of SHORTCUT_PRESETS) {
    const hit = leaves.find((leaf) => preset.match.test(leaf.path) && !used.has(leaf.path))
    if (!hit) continue
    used.add(hit.path)
    picked.push({
      name: preset.name || hit.title,
      path: hit.path,
      icon: preset.icon || hit.icon,
      bg: SHORTCUT_COLORS[picked.length % SHORTCUT_COLORS.length]
    })
    if (picked.length >= 8) break
  }

  if (picked.length < 8) {
    for (const leaf of leaves) {
      if (used.has(leaf.path)) continue
      used.add(leaf.path)
      picked.push({
        name: leaf.title,
        path: leaf.path,
        icon: leaf.icon,
        bg: SHORTCUT_COLORS[picked.length % SHORTCUT_COLORS.length]
      })
      if (picked.length >= 8) break
    }
  }

  shortcuts.value = picked
}

const loadTodos = async () => {
  try {
    const data = await TaskApi.getTaskTodoPage({ pageNo: 1, pageSize: 8 })
    todoList.value = data?.list || []
    summary.todo = data?.total ?? todoList.value.length
  } catch {
    todoList.value = []
    summary.todo = 0
  }
}

const loadMessages = async () => {
  try {
    const [count, list] = await Promise.all([
      NotifyMessageApi.getUnreadNotifyMessageCount(),
      NotifyMessageApi.getUnreadNotifyMessageList()
    ])
    summary.unread = Number(count) || 0
    messageList.value = (list || []).slice(0, 6)
  } catch {
    summary.unread = 0
    messageList.value = []
  }
}

const loadNotices = async () => {
  try {
    const data = await NoticeApi.getNoticePage({
      pageNo: 1,
      pageSize: 6,
      status: CommonStatusEnum.ENABLE
    })
    noticeList.value = data?.list || []
    summary.notice = data?.total ?? noticeList.value.length
  } catch {
    noticeList.value = []
    summary.notice = 0
  }
}

const goTodo = () => {
  push({ name: 'BpmTodoTask' }).catch(() => {
    push({ path: '/bpm/task/todo' }).catch(() => undefined)
  })
}

const goNotify = () => {
  push({ name: 'MyNotifyMessage' })
}

const goPath = (path: string) => {
  push({ path })
}

const openTodo = (row: any) => {
  push({
    name: 'BpmProcessInstanceDetail',
    query: {
      id: row.processInstance?.id,
      taskId: row.id
    }
  })
}

const getAllApi = async () => {
  loading.value = true
  try {
    buildShortcuts()
    await Promise.all([loadTodos(), loadMessages(), loadNotices()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getAllApi()
})
</script>

<style lang="scss" scoped>
.workplace-hero {
  :deep(.el-card__body) {
    padding: 20px 24px;
  }
}

.workplace-stat {
  min-width: 88px;
  padding: 10px 16px;
  cursor: pointer;
  background: rgba(109, 94, 252, 0.08);
  border: 1px solid rgba(109, 94, 252, 0.16);
  border-radius: 14px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 18px rgba(109, 94, 252, 0.12);
  }

  &--muted {
    cursor: default;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  &__label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    margin-top: 4px;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
    color: var(--el-color-primary);
  }
}

.workplace-panel {
  :deep(.el-card__header) {
    padding: 14px 18px;
  }

  :deep(.el-card__body) {
    padding: 8px 12px 16px;
  }
}

.workplace-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 8px;
  }
}

.workplace-list {
  max-height: 360px;
  overflow: auto;
}

.workplace-list__item {
  display: flex;
  align-items: flex-start;
  padding: 12px 10px;
  cursor: pointer;
  border-radius: 12px;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(109, 94, 252, 0.06);
  }

  & + & {
    border-top: 1px solid rgba(124, 92, 255, 0.08);
  }
}

.workplace-shortcuts {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 12px;
  padding: 8px 6px 4px;
}

.workplace-shortcut {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  padding: 14px 8px;
  cursor: pointer;
  border-radius: 16px;
  transition:
    background 0.2s ease,
    transform 0.2s ease;

  &:hover {
    background: rgba(109, 94, 252, 0.06);
    transform: translateY(-2px);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 14px;
    box-shadow: 0 8px 16px rgba(109, 94, 252, 0.2);
  }

  &__name {
    max-width: 100%;
    font-size: 13px;
    color: var(--el-text-color-primary);
    text-align: center;
  }
}

@media (max-width: 1200px) {
  .workplace-shortcuts {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .workplace-shortcuts {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>

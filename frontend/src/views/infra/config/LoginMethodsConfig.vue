<template>
  <ContentWrap title="登录页入口">
    <template #header>
      <div class="w-full flex justify-end">
        <el-button
          type="primary"
          :loading="saving"
          v-hasPermi="['infra:config:create', 'infra:config:update']"
          @click="save"
        >
          保存设置
        </el-button>
      </div>
    </template>

    <div v-loading="loading" class="login-methods">
      <p class="login-methods__intro">选择在登录页显示的入口，保存后刷新登录页生效。</p>

      <div class="login-methods__groups">
        <section v-for="group in groups" :key="group.title" class="login-methods__group">
          <div class="login-methods__heading">
            <h3>{{ group.title }}</h3>
            <span>{{ group.description }}</span>
          </div>
          <div class="login-methods__options">
            <div v-for="item in group.options" :key="item.key" class="login-methods__option">
              <span>{{ item.label }}</span>
              <el-switch v-model="methods[item.key]" :aria-label="item.label" />
            </div>
          </div>
        </section>
      </div>

      <p class="login-methods__note">GitHub 和支付宝尚未接入，开启后点击仍会提示未配置。</p>
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as ConfigApi from '@/api/infra/config'

const message = useMessage()
const groups: {
  title: string
  description: string
  options: { key: keyof ConfigApi.LoginMethods; label: string }[]
}[] = [
  {
    title: '常用入口',
    description: '账号密码登录始终保留',
    options: [
      { key: 'mobile', label: '手机登录' },
      { key: 'qrCode', label: '二维码登录' },
      { key: 'register', label: '注册' }
    ]
  },
  {
    title: '第三方登录',
    description: '按需显示授权入口',
    options: [
      { key: 'wechat', label: '微信' },
      { key: 'dingtalk', label: '钉钉' },
      { key: 'github', label: 'GitHub' },
      { key: 'alipay', label: '支付宝' }
    ]
  }
]
const methods = reactive<ConfigApi.LoginMethods>({ ...ConfigApi.defaultLoginMethods })
const config = ref<ConfigApi.ConfigVO>()
const loading = ref(false)
const saving = ref(false)

const load = async () => {
  loading.value = true
  try {
    const page = await ConfigApi.getConfigPage({
      pageNo: 1,
      pageSize: 10,
      key: ConfigApi.LOGIN_METHODS_KEY
    })
    config.value = page.list.find(
      (item: ConfigApi.ConfigVO) => item.key === ConfigApi.LOGIN_METHODS_KEY
    )
    if (config.value) {
      const saved = JSON.parse(config.value.value)
      for (const group of groups) {
        for (const item of group.options) methods[item.key] = saved[item.key] === true
      }
    }
  } finally {
    loading.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    const data = {
      ...config.value,
      category: 'login',
      name: '登录页入口',
      key: ConfigApi.LOGIN_METHODS_KEY,
      value: JSON.stringify(methods),
      visible: true,
      remark: '控制登录页入口显示；不控制服务端登录能力'
    } as ConfigApi.ConfigVO
    if (config.value?.id) await ConfigApi.updateConfig(data)
    else await ConfigApi.createConfig(data)
    message.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.login-methods {
  max-width: 1080px;
  padding: 8px 8px 4px;
}

.login-methods__intro {
  margin: 0 0 20px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.login-methods__groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.login-methods__group {
  padding: 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-extra-light);
}

.login-methods__heading {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}

.login-methods__heading h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.login-methods__heading span,
.login-methods__note {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.login-methods__options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.login-methods__option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.login-methods__note {
  margin: 16px 0 0;
}

@media (max-width: 900px) {
  .login-methods__groups {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 500px) {
  .login-methods__options {
    grid-template-columns: 1fr;
  }
}
</style>

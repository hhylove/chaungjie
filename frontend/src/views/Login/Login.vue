<template>
  <div
    :class="prefixCls"
    class="relative h-[100%] lt-md:px-10px lt-sm:px-10px lt-xl:px-10px lt-xl:px-10px"
  >
    <div class="relative mx-auto h-full flex">
      <div
        :class="`${prefixCls}__left flex-1 relative p-30px lt-xl:hidden overflow-hidden`"
      >
        <!-- 左上角的 logo + 系统标题 -->
        <div class="relative z-1 flex items-center text-white">
          <img alt="" class="mr-10px h-48px w-48px rounded-12px shadow-lg" src="@/assets/imgs/logo.png" />
          <span class="text-20px font-bold tracking-wide">{{ underlineToHump(appStore.getTitle) }}</span>
        </div>
        <!-- 左边的欢迎语 -->
        <div class="relative z-1 h-[calc(100%-60px)] flex items-center justify-center">
          <TransitionGroup
            appear
            enter-active-class="animate__animated animate__bounceInLeft"
            tag="div"
            class="max-w-420px"
          >
            <div key="1" class="mb-28px">
              <div class="login-orb"></div>
            </div>
            <div key="2" class="text-36px font-700 text-white leading-tight">
              {{ t('login.welcome') }}
            </div>
            <div key="3" class="mt-16px text-15px font-normal text-white/75 leading-relaxed">
              {{ t('login.message') }}
            </div>
          </TransitionGroup>
        </div>
      </div>
      <div
        :class="`${prefixCls}__right relative flex-1 p-30px lt-sm:p-10px overflow-x-hidden overflow-y-auto`"
      >
        <!-- 右上角：移动端显示标题，桌面端仅保留主题切换 -->
        <div
          class="flex items-center justify-between at-2xl:justify-end at-xl:justify-end"
          style="color: var(--el-text-color-primary)"
        >
          <div class="flex items-center at-2xl:hidden at-xl:hidden">
            <img alt="" class="mr-10px h-48px w-48px rounded-12px" src="@/assets/imgs/logo.png" />
            <span class="text-20px font-bold">{{ underlineToHump(appStore.getTitle) }}</span>
          </div>
          <div class="flex items-center justify-end space-x-10px h-48px">
            <ThemeSwitch />
          </div>
        </div>
        <!-- 右边的登录界面 -->
        <Transition appear enter-active-class="animate__animated animate__bounceInRight">
          <div
            class="m-auto h-[calc(100%-60px)] w-[100%] flex items-center at-2xl:max-w-460px at-lg:max-w-460px at-md:max-w-460px at-xl:max-w-460px"
          >
            <div class="login-panel m-auto w-full">
              <!-- 账号登录 -->
              <LoginForm class="m-auto h-auto p-8px" />
              <!-- 手机登录 -->
              <MobileForm class="m-auto h-auto p-8px" />
              <!-- 二维码登录 -->
              <QrCodeForm class="m-auto h-auto p-8px" />
              <!-- 注册 -->
              <RegisterForm class="m-auto h-auto p-8px" />
              <!-- 三方登录 -->
              <SSOLoginVue class="m-auto h-auto p-8px" />
              <!-- 忘记密码 -->
              <ForgetPasswordForm class="m-auto h-auto p-8px" />
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { underlineToHump } from '@/utils'

import { useDesign } from '@/hooks/web/useDesign'
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'

import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'

defineOptions({ name: 'Login' })

const { t } = useI18n()
const appStore = useAppStore()
const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: hidden;
  background: linear-gradient(145deg, #eef2ff 0%, #f5f3ff 38%, #e0f2fe 72%, #f8fafc 100%);

  &::before {
    position: absolute;
    inset: 0;
    pointer-events: none;
    content: '';
    background:
      radial-gradient(ellipse 55% 45% at 15% 20%, rgba(124, 92, 255, 0.2), transparent 60%),
      radial-gradient(ellipse 45% 40% at 85% 80%, rgba(56, 189, 248, 0.16), transparent 55%);
  }

  &__left {
    color: #fff;
    background: linear-gradient(155deg, #5b4dff 0%, #6d5efc 42%, #3b82f6 78%, #38bdf8 100%);

    &::before {
      position: absolute;
      inset: 0;
      z-index: 0;
      pointer-events: none;
      content: '';
      background:
        radial-gradient(circle at 20% 25%, rgba(255, 255, 255, 0.22), transparent 42%),
        radial-gradient(circle at 80% 70%, rgba(56, 189, 248, 0.35), transparent 45%);
    }

    &::after {
      position: absolute;
      right: -20%;
      bottom: -10%;
      z-index: 0;
      width: 70%;
      height: 60%;
      pointer-events: none;
      content: '';
      background: rgba(255, 255, 255, 0.08);
      border-radius: 40% 60% 50% 50%;
      filter: blur(2px);
    }
  }

  &__right {
    background: transparent;
  }
}

.login-orb {
  width: 180px;
  height: 180px;
  background:
    radial-gradient(circle at 35% 30%, rgba(255, 255, 255, 0.85), transparent 40%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.35), rgba(56, 189, 248, 0.25));
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 40% 60% 55% 45%;
  box-shadow:
    0 20px 50px rgba(49, 46, 129, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  animation: login-orb-float 5s ease-in-out infinite;
}

@keyframes login-orb-float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
    border-radius: 40% 60% 55% 45%;
  }

  50% {
    transform: translateY(-12px) rotate(8deg);
    border-radius: 55% 45% 40% 60%;
  }
}

.login-panel {
  padding: 28px 28px 18px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 24px;
  box-shadow:
    0 18px 48px rgba(76, 29, 149, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.dark {
  .#{$prefix-cls} {
    background: linear-gradient(145deg, #0b1026 0%, #151336 42%, #0f172a 100%);
  }

  .login-panel {
    background: rgba(15, 23, 42, 0.72);
    border-color: rgba(148, 163, 184, 0.18);
    box-shadow: 0 18px 48px rgba(0, 0, 0, 0.35);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-orb {
    animation: none;
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: transparent;
  }
}
</style>

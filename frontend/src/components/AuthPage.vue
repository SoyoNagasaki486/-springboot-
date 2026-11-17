<script setup>
import { ref } from 'vue'
import {
  ArrowRight, AtSign, BadgeCheck, CheckCircle2, FlaskConical, KeyRound,
  LogIn, ShieldCheck, UserPlus, UserRound,
} from 'lucide-vue-next'
import { api, jsonOptions } from '@/services/api'

const emit = defineEmits(['authenticated'])
const mode = ref('login')
const loading = ref(false)
const error = ref('')
const message = ref('')
const login = ref({ username: 'admin', password: '1234' })
const register = ref({ realName: '', username: '', role: 'STUDENT', password: '' })

async function submitLogin() {
  loading.value = true
  error.value = ''
  try {
    emit('authenticated', await api('/login', jsonOptions('POST', login.value)))
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  loading.value = true
  error.value = ''
  try {
    const result = await api('/register', jsonOptions('POST', register.value))
    login.value.username = register.value.username
    login.value.password = register.value.password
    message.value = result.message
    mode.value = 'login'
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

function switchMode(next) {
  mode.value = next
  error.value = ''
  message.value = ''
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-story">
      <div class="auth-brand"><div class="brand-mark"><ShieldCheck :size="23" /></div><strong>LabGuard</strong></div>
      <div class="auth-copy">
        <div class="auth-illustration"><FlaskConical :size="52" :stroke-width="1.5" /></div>
        <h1>让每一次实验，<br><span>都从安全开始。</span></h1>
        <p>统一完成安全培训、准入考试、资格审核和实验室预约，让实验资源开放有序、过程可追溯。</p>
        <div class="auth-points">
          <span><CheckCircle2 :size="18" /> 培训与考试结果闭环</span>
          <span><CheckCircle2 :size="18" /> 准入资格自动校验</span>
          <span><CheckCircle2 :size="18" /> 设备和隐患全程留痕</span>
        </div>
      </div>
      <small>高校实验室安全准入与预约管理系统</small>
    </section>

    <section class="auth-panel">
      <div class="auth-form-wrap">
        <div class="auth-heading">
          <h2>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ mode === 'login' ? '登录后进入实验室安全管理工作台' : '注册学生或教师账号，开始安全学习' }}</p>
        </div>
        <div class="auth-tabs">
          <button :class="{ active: mode === 'login' }" @click="switchMode('login')"><LogIn :size="16" />账号登录</button>
          <button :class="{ active: mode === 'register' }" @click="switchMode('register')"><UserPlus :size="16" />新用户注册</button>
        </div>
        <div v-if="error" class="form-alert error">{{ error }}</div>
        <div v-if="message" class="form-alert success">{{ message }}</div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
          <label>账号<div class="auth-input"><AtSign :size="18" /><input v-model.trim="login.username" autocomplete="username" required placeholder="请输入账号" /></div></label>
          <label>密码<div class="auth-input"><KeyRound :size="18" /><input v-model="login.password" type="password" autocomplete="current-password" required placeholder="请输入密码" /></div></label>
          <button class="primary-button auth-submit" :disabled="loading">
            {{ loading ? '正在登录…' : '安全登录' }}<ArrowRight v-if="!loading" :size="18" />
          </button>
          <p class="demo-tip">演示账号：admin / student / teacher，密码均为 1234</p>
        </form>

        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <label>姓名<div class="auth-input"><UserRound :size="18" /><input v-model.trim="register.realName" maxlength="50" required placeholder="请输入真实姓名" /></div></label>
          <label>账号<div class="auth-input"><AtSign :size="18" /><input v-model.trim="register.username" minlength="3" maxlength="50" required placeholder="至少 3 个字符" /></div></label>
          <label>身份<div class="auth-input"><BadgeCheck :size="18" /><select v-model="register.role"><option value="STUDENT">学生</option><option value="TEACHER">教师</option></select></div></label>
          <label>密码<div class="auth-input"><KeyRound :size="18" /><input v-model="register.password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required placeholder="至少 8 个字符" /></div></label>
          <button class="primary-button auth-submit" :disabled="loading">
            {{ loading ? '正在创建…' : '创建账号' }}<ArrowRight v-if="!loading" :size="18" />
          </button>
        </form>
      </div>
    </section>
  </main>
</template>

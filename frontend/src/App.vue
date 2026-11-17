<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, provide, reactive, ref } from 'vue'
import {
  AlertTriangle, BookOpenCheck, Boxes, ClipboardCheck, FlaskConical, Gauge,
  ChevronDown, GraduationCap, Menu, Megaphone, ScrollText, ShieldCheck, Users,
  Wrench, X,
} from 'lucide-vue-next'
import AuthPage from '@/components/AuthPage.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import DashboardPage from '@/components/DashboardPage.vue'
import ExamPage from '@/components/ExamPage.vue'
import QualificationPage from '@/components/QualificationPage.vue'
import ReservationPage from '@/components/ReservationPage.vue'
import ResourcePage from '@/components/ResourcePage.vue'
import TrainingPage from '@/components/TrainingPage.vue'
import UserProfileDialog from '@/components/UserProfileDialog.vue'

import { api, clearSecurityContext } from '@/services/api'

const user = ref(null)
const restoringSession = ref(true)
const currentPage = ref('dashboard')
const sidebarOpen = ref(false)
const toast = ref(null)
const profileOpen = ref(false)
let toastTimer
let confirmResolver
const confirmDialog = reactive({
  open: false,
  title: '确认操作',
  message: '',
  confirmText: '确认',
  tone: 'danger',
})

function requestConfirm(options = {}) {
  if (confirmResolver) confirmResolver(false)
  Object.assign(confirmDialog, {
    open: true,
    title: options.title || '确认操作',
    message: options.message || '',
    confirmText: options.confirmText || '确认',
    tone: options.tone || 'danger',
  })
  return new Promise((resolve) => { confirmResolver = resolve })
}

function closeConfirm(confirmed) {
  confirmDialog.open = false
  document.body.classList.remove('dialog-open')
  const resolve = confirmResolver
  confirmResolver = null
  resolve?.(confirmed)
}

provide('confirm', requestConfirm)

const allGroups = [
  { label: '总览', items: [{ page: 'dashboard', label: '安全总览', icon: Gauge, roles: ['ADMIN', 'TEACHER', 'STUDENT'] }] },
  { label: '用户管理', items: [{ page: 'users', label: '用户与权限', icon: Users, roles: ['ADMIN'] }] },
  { label: '安全培训', items: [
    { page: 'courses', label: '培训课程', icon: GraduationCap, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
    { page: 'study-records', label: '学习记录', icon: BookOpenCheck, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  ] },
  { label: '准入考试', items: [
    { page: 'exam', label: '在线考试', icon: ClipboardCheck, roles: ['TEACHER', 'STUDENT'] },
    { page: 'questions', label: '考试题库', icon: ScrollText, roles: ['ADMIN'] },
    { page: 'qualifications', label: '准入资格', icon: ShieldCheck, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  ] },
  { label: '实验室管理', items: [
    { page: 'labs', label: '实验室信息', icon: FlaskConical, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
    { page: 'reservations', label: '预约管理', icon: Boxes, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  ] },
  { label: '设备管理', items: [
    { page: 'equipment', label: '设备台账', icon: Wrench, roles: ['ADMIN', 'TEACHER'] },
    { page: 'equipment-uses', label: '使用登记', icon: Boxes, roles: ['ADMIN', 'TEACHER'] },
  ] },
  { label: '安全管理', items: [
    { page: 'hazards', label: '危险源', icon: AlertTriangle, roles: ['ADMIN', 'TEACHER'] },
    { page: 'violations', label: '违规记录', icon: ShieldCheck, roles: ['ADMIN', 'TEACHER'] },
    { page: 'checks', label: '安全检查', icon: ClipboardCheck, roles: ['ADMIN', 'TEACHER'] },
  ] },
  { label: '系统管理', items: [
    { page: 'notices', label: '通知公告', icon: Megaphone, roles: ['ADMIN'] },
    { page: 'logs', label: '操作日志', icon: ScrollText, roles: ['ADMIN'] },
  ] },
]

const groups = computed(() => !user.value ? [] : allGroups
  .map((group) => ({
    ...group,
    items: group.items.filter((item) => item.roles.includes(user.value.role))
      .map((item) => ({ ...item, icon: markRaw(item.icon) })),
  }))
  .filter((group) => group.items.length))

const roleLabel = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[user.value?.role] || '用户'))
const pageTitle = computed(() => groups.value.flatMap((group) => group.items)
  .find((item) => item.page === currentPage.value)?.label || '业务管理')

function authenticated(value) {
  user.value = value
  profileOpen.value = false
  currentPage.value = 'dashboard'
}

function go(page) {
  currentPage.value = page
  sidebarOpen.value = false
}

function notify(payload, type = 'success') {
  const value = typeof payload === 'string' ? { message: payload, type } : payload
  toast.value = value
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = null }, 2800)
}

async function logout() {
  const confirmed = await requestConfirm({
    title: '退出当前账号？',
    message: '退出后需要重新输入账号和密码才能进入管理平台。',
    confirmText: '退出登录',
    tone: 'neutral',
  })
  if (!confirmed) return
  try { await api('/logout', { method: 'POST' }) } catch { /* 会话失效时仍清理页面状态 */ }
  user.value = null
  profileOpen.value = false
  clearSecurityContext()
  currentPage.value = 'dashboard'
}

async function logoutFromProfile() {
  profileOpen.value = false
  await nextTick()
  await logout()
}

function sessionExpired() {
  if (confirmDialog.open) closeConfirm(false)
  profileOpen.value = false
  clearSecurityContext()
  user.value = null
  currentPage.value = 'dashboard'
  notify({ message: '登录状态已失效，请重新登录', type: 'error' })
}

onMounted(async () => {
  window.addEventListener('labguard:unauthorized', sessionExpired)
  try { user.value = await api('/session') } catch { user.value = null }
  finally { restoringSession.value = false }
})

onBeforeUnmount(() => window.removeEventListener('labguard:unauthorized', sessionExpired))
</script>

<template>
  <div v-if="restoringSession" class="session-loading"><ShieldCheck :size="34" /><span>正在恢复安全会话…</span></div>
  <AuthPage v-else-if="!user" @authenticated="authenticated" />
  <div v-else class="app-shell">
    <div v-if="sidebarOpen" class="mobile-mask" @click="sidebarOpen = false" />
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="brand">
        <div class="brand-mark"><ShieldCheck :size="23" /></div>
        <div><strong>LabGuard</strong><span>高校实验室安全管理平台</span></div>
        <button class="icon-btn sidebar-close" aria-label="关闭导航" @click="sidebarOpen = false"><X :size="20" /></button>
      </div>
      <nav class="navigation" aria-label="系统导航">
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <div class="nav-label">{{ group.label }}</div>
          <button v-for="item in group.items" :key="item.page" class="nav-item"
                  :class="{ active: currentPage === item.page }" @click="go(item.page)">
            <component :is="item.icon" :size="18" :stroke-width="1.9" />
            <span>{{ item.label }}</span>
          </button>
        </div>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn menu-button" aria-label="打开导航" @click="sidebarOpen = true"><Menu :size="22" /></button>
          <div><span class="topbar-caption">当前模块</span><strong>{{ pageTitle }}</strong></div>
        </div>
        <button class="account" aria-label="查看用户信息" @click="profileOpen = true">
          <span class="avatar">{{ user.real_name.slice(-1) }}</span>
          <span><b>{{ user.real_name }}</b><small>{{ roleLabel }} · 查看个人信息</small></span>
          <ChevronDown :size="17" />
        </button>
      </header>

      <main class="content">
        <DashboardPage v-if="currentPage === 'dashboard'" :user="user" @navigate="go" @notify="notify" />
        <TrainingPage v-else-if="currentPage === 'courses' && user.role !== 'ADMIN'" :user="user" @notify="notify" />
        <ExamPage v-else-if="currentPage === 'exam'" :user="user" @notify="notify" />
        <QualificationPage v-else-if="currentPage === 'qualifications'" :user="user" @notify="notify" />
        <ReservationPage v-else-if="currentPage === 'reservations'" :user="user" @notify="notify" />
        <ResourcePage v-else :key="currentPage" :type="currentPage" :user="user" @notify="notify" />
      </main>
    </section>

    <Transition name="toast">
      <div v-if="toast" class="toast-message" :class="toast.type">
        <ShieldCheck v-if="toast.type !== 'error'" :size="18" />
        <AlertTriangle v-else :size="18" />
        {{ toast.message }}
      </div>
    </Transition>
    <ConfirmDialog v-bind="confirmDialog" @confirm="closeConfirm(true)" @cancel="closeConfirm(false)" />
    <UserProfileDialog :open="profileOpen" :user="user" :role-label="roleLabel"
                       @close="profileOpen = false" @logout="logoutFromProfile" />
  </div>
</template>

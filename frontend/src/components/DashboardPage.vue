<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, CalendarClock, FlaskConical, ShieldCheck, TriangleAlert, Wrench } from 'lucide-vue-next'
import { api } from '@/services/api'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['navigate', 'notify'])
const dashboard = ref(null)
const labs = ref([])
const notices = ref([])
const loading = ref(true)

const metrics = computed(() => !dashboard.value ? [] : [
  { label: '已获准入资格', value: dashboard.value.qualified, hint: '当前有效用户', tone: 'green', icon: ShieldCheck },
  { label: '待审核预约', value: dashboard.value.pending, hint: '需要及时处理', tone: 'amber', icon: CalendarClock },
  { label: '仪器设备', value: dashboard.value.equipment, hint: '已纳入台账', tone: 'blue', icon: Wrench },
  { label: '登记危险源', value: dashboard.value.hazards, hint: '重点安全对象', tone: 'red', icon: TriangleAlert },
])

const maxUsage = computed(() => Math.max(1, ...(dashboard.value?.labUsage || []).map((item) => Number(item.value))))

onMounted(async () => {
  try {
    const [data, labRows, noticeRows] = await Promise.all([api('/dashboard'), api('/labs'), api('/notices')])
    dashboard.value = data
    labs.value = labRows
    notices.value = noticeRows
  } catch (exception) {
    emit('notify', { message: exception.message, type: 'error' })
  } finally {
    loading.value = false
  }
})

function statusTone(value) {
  if (/停用/.test(value)) return 'danger'
  if (/维护|整改/.test(value)) return 'warning'
  return 'success'
}
</script>

<template>
  <section>
    <div class="page-heading dashboard-heading">
      <div><h1>安全准入总览</h1><p>{{ user.real_name }}，这里汇总实验室资源、准入资格和安全风险的当前态势。</p></div>
      <button class="secondary-button" @click="emit('navigate', 'reservations')">进入预约管理<ArrowRight :size="17" /></button>
    </div>
    <div v-if="loading" class="loading-state">正在加载工作台数据…</div>
    <template v-else>
      <div class="metrics-grid">
        <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
          <div class="metric-icon"><component :is="metric.icon" :size="21" /></div>
          <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.hint }}</small>
        </article>
      </div>

      <div class="dashboard-grid">
        <div class="dashboard-main">
          <section class="panel">
            <div class="panel-heading"><div><h2>实验室运行状态</h2><p>开放时间、风险等级和容量信息</p></div><button class="text-button" @click="emit('navigate', 'labs')">查看全部<ArrowRight :size="16" /></button></div>
            <div class="lab-list">
              <article v-for="lab in labs.slice(0, 4)" :key="lab.id" class="lab-row">
                <div class="lab-symbol"><FlaskConical :size="20" /></div>
                <div class="lab-main"><strong>{{ lab.name }}</strong><span>{{ lab.location }} · {{ lab.level }}</span></div>
                <div class="lab-time"><strong>{{ lab.capacity }} 人</strong><span>{{ lab.open_time }}</span></div>
                <span class="status" :class="statusTone(lab.status)">{{ lab.status }}</span>
              </article>
            </div>
          </section>

          <section class="panel usage-panel">
            <div class="panel-heading"><div><h2>实验室预约热度</h2><p>按实验室累计预约数量</p></div></div>
            <div class="usage-chart">
              <div v-for="item in dashboard.labUsage" :key="item.label" class="usage-item">
                <div class="usage-label"><span>{{ item.label }}</span><b>{{ item.value }} 次</b></div>
                <div class="usage-track"><i :style="{ width: `${Math.max(8, Number(item.value) / maxUsage * 100)}%` }" /></div>
              </div>
            </div>
          </section>
        </div>

        <aside class="panel notice-panel">
          <div class="panel-heading"><div><h2>安全公告</h2><p>近期培训与运行提醒</p></div></div>
          <article v-for="notice in notices.slice(0, 5)" :key="notice.id" class="notice-item">
            <span>{{ notice.publish_time }}</span><h3>{{ notice.title }}</h3><p>{{ notice.content }}</p>
          </article>
          <div v-if="!notices.length" class="empty-state">暂无公告</div>
        </aside>
      </div>
    </template>
  </section>
</template>

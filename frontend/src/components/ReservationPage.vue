<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { CalendarPlus, Check, Search, Trash2, X, XCircle } from 'lucide-vue-next'
import { api, jsonOptions } from '@/services/api'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['notify'])
const confirmAction = inject('confirm')
const rows = ref([])
const labs = ref([])
const search = ref('')
const loading = ref(true)
const modalOpen = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = 10
const today = new Date().toISOString().slice(0, 10)
const maxDate = new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
const form = reactive({ labId: '', date: today, slot: '09:00-12:00', purpose: '' })

const filteredRows = computed(() => {
  let result = props.user.role === 'STUDENT'
    ? rows.value.filter((row) => Number(row.user_id) === Number(props.user.id)) : rows.value
  const keyword = search.value.trim().toLowerCase()
  return keyword ? result.filter((row) => Object.values(row).join(' ').toLowerCase().includes(keyword)) : result
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)))
const visibleRows = computed(() => filteredRows.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const openLabs = computed(() => labs.value.filter((lab) => lab.status === '开放'))

watch(search, () => { page.value = 1 })
watch(pageCount, (count) => { if (page.value > count) page.value = count })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [reservationRows, labRows] = await Promise.all([api('/reservations'), api('/labs')])
    rows.value = reservationRows
    labs.value = labRows
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}

function openForm() {
  form.labId = openLabs.value[0]?.id || ''
  form.date = today
  form.slot = '09:00-12:00'
  form.purpose = ''
  modalOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    const result = await api('/reservations', jsonOptions('POST', {
      labId: Number(form.labId), date: form.date, slot: form.slot, purpose: form.purpose,
    }))
    modalOpen.value = false
    emit('notify', result.message)
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { saving.value = false }
}

async function audit(row, status) {
  try {
    await api(`/reservations/${row.id}`, jsonOptions('PUT', { status }))
    emit('notify', `预约已${status === '已通过' ? '通过' : '驳回'}`)
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
}

async function cancel(row) {
  const confirmed = await confirmAction({
    title: '取消这条预约？',
    message: `${row.lab_name} · ${row.reserve_date} · ${row.time_slot}，取消后如需使用须重新提交预约。`,
    confirmText: '确认取消',
    tone: 'danger',
  })
  if (!confirmed) return
  try {
    await api(`/reservations/${row.id}`, { method: 'DELETE' })
    emit('notify', '预约已取消')
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
}

function statusTone(value) {
  if (/驳回/.test(value)) return 'danger'
  if (/待审核|已取消/.test(value)) return 'warning'
  return 'success'
}
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>实验室预约管理</h1><p>系统依次校验准入资格、实验室开放状态与预约时段冲突。</p></div><button v-if="user.role === 'STUDENT'" class="primary-button" @click="openForm"><CalendarPlus :size="17" />提交预约</button></div>
    <section class="panel table-panel">
      <div class="table-toolbar"><label class="search-box"><Search :size="17" /><input v-model="search" placeholder="搜索申请人、实验室或日期" /></label><span>共 {{ filteredRows.length }} 条预约</span></div>
      <div v-if="loading" class="loading-state">正在加载预约数据…</div>
      <div v-else class="table-scroll"><table class="responsive-table"><thead><tr><th>申请人</th><th>实验室</th><th>预约日期</th><th>时间段</th><th>使用目的</th><th>状态</th><th>审核人</th><th>操作</th></tr></thead>
        <tbody><tr v-for="row in visibleRows" :key="row.id"><td data-label="申请人"><b>{{ row.real_name }}</b></td><td data-label="实验室">{{ row.lab_name }}</td><td data-label="预约日期">{{ row.reserve_date }}</td><td data-label="时间段">{{ row.time_slot }}</td><td data-label="使用目的">{{ row.purpose }}</td><td data-label="状态"><span class="status" :class="statusTone(row.status)">{{ row.status }}</span></td><td data-label="审核人">{{ row.reviewer || '—' }}</td>
          <td data-label="操作" class="row-actions"><template v-if="row.status === '待审核' && user.role !== 'STUDENT'"><button class="table-action approve" @click="audit(row, '已通过')"><Check :size="16" />通过</button><button class="table-action danger" @click="audit(row, '驳回')"><XCircle :size="16" />驳回</button></template><button v-else-if="user.role === 'STUDENT' && ['待审核', '已通过'].includes(row.status) && row.reserve_date >= today" class="table-action danger" @click="cancel(row)"><Trash2 :size="16" />取消</button><span v-else>—</span></td>
        </tr><tr v-if="!visibleRows.length"><td colspan="8" class="empty-state">暂无预约记录</td></tr></tbody></table></div>
      <div v-if="!loading && filteredRows.length > pageSize" class="pagination" aria-label="分页导航"><button class="secondary-button" :disabled="page === 1" @click="page--">上一页</button><span>第 {{ page }} / {{ pageCount }} 页</span><button class="secondary-button" :disabled="page === pageCount" @click="page++">下一页</button></div>
    </section>

    <Teleport to="body"><div v-if="modalOpen" class="modal-backdrop" @mousedown.self="modalOpen = false"><form class="modal-card" @submit.prevent="submit">
      <div class="modal-heading"><div><h2>提交实验室预约</h2><p>仅显示当前处于开放状态的实验室</p></div><button type="button" class="icon-btn" @click="modalOpen = false"><X :size="20" /></button></div>
      <div v-if="!openLabs.length" class="form-alert error">当前没有开放预约的实验室</div>
      <div class="form-grid"><label class="full">实验室<select v-model="form.labId" required><option v-for="lab in openLabs" :key="lab.id" :value="lab.id">{{ lab.name }} · {{ lab.level }} · {{ lab.capacity }} 人</option></select></label><label>预约日期<input v-model="form.date" type="date" :min="today" :max="maxDate" required /></label><label>预约时段<select v-model="form.slot"><option>09:00-12:00</option><option>14:00-17:00</option><option>18:00-21:00</option></select></label><label class="full">使用目的<textarea v-model.trim="form.purpose" minlength="4" maxlength="255" rows="4" required placeholder="请说明实验课程、课题或项目用途" /></label></div>
      <div class="modal-actions"><button type="button" class="secondary-button" @click="modalOpen = false">取消</button><button class="primary-button" :disabled="saving || !openLabs.length">{{ saving ? '提交中…' : '提交预约' }}</button></div>
    </form></div></Teleport>
  </section>
</template>

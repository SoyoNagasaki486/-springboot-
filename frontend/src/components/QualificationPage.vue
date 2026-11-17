<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckCircle2, Search, ShieldCheck, X, XCircle } from 'lucide-vue-next'
import { api, jsonOptions } from '@/services/api'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['notify'])
const qualifications = ref([])
const candidates = ref([])
const labs = ref([])
const search = ref('')
const loading = ref(true)
const saving = ref(false)
const modalOpen = ref(false)
const selected = ref(null)
const nextYear = new Date()
nextYear.setFullYear(nextYear.getFullYear() + 1)
const form = reactive({ labName: '', status: '已通过', validUntil: nextYear.toISOString().slice(0, 10) })
const canAudit = computed(() => ['ADMIN', 'TEACHER'].includes(props.user.role))

const rows = computed(() => {
  if (!canAudit.value) return qualifications.value
  return candidates.value.map((candidate) => {
    const records = qualifications.value.filter((item) => item.user_name === candidate.user_name)
    const approved = records.find((item) => item.status === '已通过') || records[0]
    return { ...candidate, qualification: approved }
  })
})

const visibleRows = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return keyword ? rows.value.filter((row) => Object.values(row).join(' ').toLowerCase().includes(keyword)) : rows.value
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const requests = [api('/manage/qualifications')]
    if (canAudit.value) requests.push(api('/qualifications/candidates'), api('/labs'))
    const [qualificationRows, candidateRows = [], labRows = []] = await Promise.all(requests)
    qualifications.value = qualificationRows
    candidates.value = candidateRows
    labs.value = labRows
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}

function openAudit(row, status = '已通过') {
  selected.value = row
  const existing = row.qualification
  form.labName = existing?.lab_name || labs.value.find((lab) => lab.status === '开放')?.name || labs.value[0]?.name || ''
  form.status = status
  form.validUntil = existing?.valid_until || nextYear.toISOString().slice(0, 10)
  modalOpen.value = true
}

async function submit() {
  saving.value = true
  try {
    const result = await api('/qualifications/audit', jsonOptions('POST', {
      userName: selected.value.user_name,
      labName: form.labName,
      status: form.status,
      validUntil: form.validUntil || null,
    }))
    modalOpen.value = false
    emit('notify', result.message)
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { saving.value = false }
}

function tone(value) {
  if (/未通过|未取得/.test(String(value))) return 'danger'
  if (/待/.test(String(value))) return 'warning'
  return 'success'
}
</script>

<template>
  <section>
    <div class="page-heading">
      <div><h1>准入资格审核</h1><p>{{ canAudit ? '系统汇总培训完成情况与最近考试成绩，审核通过后自动同步预约准入状态。' : '查看本人已审核的适用实验室、成绩和资格有效期。' }}</p></div>
    </div>
    <section class="panel table-panel">
      <div class="table-toolbar"><label class="search-box"><Search :size="17" /><input v-model="search" placeholder="搜索人员或实验室" /></label><span>共 {{ visibleRows.length }} 条记录</span></div>
      <div v-if="loading" class="loading-state">正在汇总培训、考试与资格数据…</div>
      <div v-else class="table-scroll">
        <table v-if="canAudit" class="responsive-table">
          <thead><tr><th>用户</th><th>已完成课程</th><th>最近成绩</th><th>考试结果</th><th>当前资格</th><th>适用实验室</th><th>有效期</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.user_id">
              <td data-label="用户"><b>{{ row.user_name }}</b></td>
              <td data-label="已完成课程">{{ row.completed_courses }} 门</td>
              <td data-label="最近成绩">{{ row.latest_score }} 分</td>
              <td data-label="考试结果"><span class="status" :class="tone(row.exam_passed ? '已通过' : '未通过')">{{ row.exam_passed ? '已通过' : '未通过' }}</span></td>
              <td data-label="当前资格"><span class="status" :class="tone(row.qualification?.status || '未取得')">{{ row.qualification?.status || '未取得' }}</span></td>
              <td data-label="适用实验室">{{ row.qualification?.lab_name || '—' }}</td>
              <td data-label="有效期">{{ row.qualification?.valid_until || '—' }}</td>
              <td data-label="操作" class="row-actions"><button class="table-action approve" :disabled="row.completed_courses < 1 || !row.exam_passed" :title="row.completed_courses < 1 || !row.exam_passed ? '需先完成培训并通过考试' : '授予准入资格'" @click="openAudit(row, '已通过')"><CheckCircle2 :size="16" />通过</button><button class="table-action danger" @click="openAudit(row, '未通过')"><XCircle :size="16" />不通过</button></td>
            </tr>
            <tr v-if="!visibleRows.length"><td colspan="8" class="empty-state">暂无待审核用户</td></tr>
          </tbody>
        </table>
        <table v-else class="responsive-table">
          <thead><tr><th>适用实验室</th><th>考试成绩</th><th>资格状态</th><th>有效期至</th></tr></thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.id"><td data-label="适用实验室"><b>{{ row.lab_name }}</b></td><td data-label="考试成绩">{{ row.score }} 分</td><td data-label="资格状态"><span class="status" :class="tone(row.status)">{{ row.status }}</span></td><td data-label="有效期至">{{ row.valid_until || '—' }}</td></tr>
            <tr v-if="!visibleRows.length"><td colspan="4" class="empty-state">尚无准入资格，请先完成培训和考试</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <Teleport to="body"><div v-if="modalOpen" class="modal-backdrop" @mousedown.self="modalOpen = false"><form class="modal-card qualification-modal" @submit.prevent="submit">
      <div class="modal-heading"><div><h2>审核 {{ selected.user_name }} 的准入资格</h2><p>已完成 {{ selected.completed_courses }} 门课程，最近考试 {{ selected.latest_score }} 分</p></div><button type="button" class="icon-btn" @click="modalOpen = false"><X :size="20" /></button></div>
      <div class="qualification-evidence"><ShieldCheck :size="20" /><span>通过审核需同时满足：至少完成一门安全课程、最近考试达到 60 分，并设置有效期限。</span></div>
      <div class="form-grid"><label class="full">适用实验室<select v-model="form.labName" required><option v-for="lab in labs" :key="lab.id" :value="lab.name">{{ lab.name }} · {{ lab.status }}</option></select></label><label>审核结论<select v-model="form.status"><option>已通过</option><option>未通过</option></select></label><label>有效期至<input v-model="form.validUntil" type="date" :required="form.status === '已通过'" /></label></div>
      <div class="modal-actions"><button type="button" class="secondary-button" @click="modalOpen = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '提交中…' : '确认审核' }}</button></div>
    </form></div></Teleport>
  </section>
</template>

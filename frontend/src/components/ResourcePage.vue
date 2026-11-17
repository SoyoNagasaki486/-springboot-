<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { Edit3, Plus, Search, Trash2, X } from 'lucide-vue-next'
import { resourceConfigs, normalizeForm } from '@/config/resources'
import { api, jsonOptions } from '@/services/api'

const props = defineProps({ type: { type: String, required: true }, user: { type: Object, required: true } })
const emit = defineEmits(['notify'])
const confirmAction = inject('confirm')
const config = resourceConfigs[props.type]
const rows = ref([])
const loading = ref(true)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref(null)
const saving = ref(false)
const page = ref(1)
const pageSize = 10
const form = reactive({})

const tableFields = computed(() => config.fields.filter((field) => field.table !== false))
const canEdit = computed(() => !config.readonly && (config.editRoles || []).includes(props.user.role))
const filteredRows = computed(() => {
  let result = rows.value
  if (props.user.role === 'STUDENT' && ['study-records', 'qualifications'].includes(props.type)) {
    result = result.filter((row) => row.user_name === props.user.real_name)
  }
  const keyword = search.value.trim().toLowerCase()
  return keyword ? result.filter((row) => Object.values(row).join(' ').toLowerCase().includes(keyword)) : result
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)))
const visibleRows = computed(() => filteredRows.value.slice((page.value - 1) * pageSize, page.value * pageSize))

watch(search, () => { page.value = 1 })
watch(pageCount, (count) => { if (page.value > count) page.value = count })

onMounted(load)

async function load() {
  loading.value = true
  try { rows.value = await api(`/manage/${props.type}`) }
  catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}

function defaultValue(field) {
  if (field.boolean) return field.options?.[0]?.[0] ?? false
  if (field.options?.length) return Array.isArray(field.options[0]) ? field.options[0][0] : field.options[0]
  if (field.type === 'date') return new Date().toISOString().slice(0, 10)
  return ''
}

function openForm(row = null) {
  editingId.value = row?.id || null
  config.fields.forEach((field) => {
    let value = row?.[field.key]
    if (field.optionalOnEdit && row) value = ''
    if (field.type === 'datetime-local' && value) value = String(value).replace(' ', 'T')
    form[field.key] = value ?? defaultValue(field)
  })
  modalOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const body = normalizeForm(config, form)
    if (editingId.value) {
      config.fields.filter((field) => field.optionalOnEdit && !body[field.key]).forEach((field) => delete body[field.key])
    }
    body.operator = props.user.real_name
    await api(`/manage/${props.type}${editingId.value ? `/${editingId.value}` : ''}`,
      jsonOptions(editingId.value ? 'PUT' : 'POST', body))
    modalOpen.value = false
    emit('notify', editingId.value ? '记录已更新' : '记录已保存')
    await load()
  } catch (exception) {
    emit('notify', { message: exception.message, type: 'error' })
  } finally { saving.value = false }
}

async function remove(row) {
  const label = row[tableFields.value[0].key] ?? `记录 ${row.id}`
  const confirmed = await confirmAction({
    title: '删除这条记录？',
    message: `“${label}”删除后无法恢复，请确认该记录已不再被业务流程使用。`,
    confirmText: '确认删除',
    tone: 'danger',
  })
  if (!confirmed) return
  try {
    await api(`/manage/${props.type}/${row.id}`, { method: 'DELETE' })
    emit('notify', '记录已删除')
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
}

function optionValue(option) { return Array.isArray(option) ? option[0] : option }
function optionLabel(option) { return Array.isArray(option) ? option[1] : option }
function displayValue(value, field) {
  if (value === null || value === undefined || value === '') return '—'
  if (field.boolean) return value === true || value === 1 ? (field.key === 'completed' ? '已完成' : '已取得') : (field.key === 'completed' ? '学习中' : '未取得')
  const text = String(value)
  return text.length > 54 ? `${text.slice(0, 54)}…` : text
}
function isStatus(field) { return ['status', 'qualified', 'completed', 'result', 'return_status', 'level'].includes(field.key) }
function statusTone(value) {
  const text = String(value)
  if (/驳回|未通过|停用|重大|异常|未取得/.test(text)) return 'danger'
  if (/待|整改|维修|中风险|学习中/.test(text)) return 'warning'
  return 'success'
}
</script>

<template>
  <section>
    <div class="page-heading">
      <div><h1>{{ config.title }}</h1><p>{{ config.description }}</p></div>
      <button v-if="canEdit" class="primary-button" @click="openForm()"><Plus :size="17" />新增记录</button>
    </div>
    <section class="panel table-panel">
      <div class="table-toolbar">
        <label class="search-box"><Search :size="17" /><input v-model="search" placeholder="输入关键词筛选" /></label>
        <span>共 {{ filteredRows.length }} 条记录</span>
      </div>
      <div v-if="loading" class="loading-state">正在加载数据…</div>
      <div v-else class="table-scroll">
        <table class="responsive-table">
          <thead><tr><th v-for="field in tableFields" :key="field.key">{{ field.label }}</th><th v-if="canEdit" class="actions-column">操作</th></tr></thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.id">
              <td v-for="field in tableFields" :key="field.key" :data-label="field.label">
                <div v-if="field.progress" class="progress-cell"><div class="progress-track"><i :style="{ width: `${row[field.key]}%` }" /></div><b>{{ row[field.key] }}%</b></div>
                <span v-else-if="isStatus(field)" class="status" :class="statusTone(displayValue(row[field.key], field))">{{ displayValue(row[field.key], field) }}</span>
                <span v-else>{{ displayValue(row[field.key], field) }}</span>
              </td>
              <td v-if="canEdit" data-label="操作" class="row-actions">
                <button class="table-action" aria-label="编辑" @click="openForm(row)"><Edit3 :size="16" />编辑</button>
                <button class="table-action danger" aria-label="删除" @click="remove(row)"><Trash2 :size="16" />删除</button>
              </td>
            </tr>
            <tr v-if="!visibleRows.length"><td :colspan="tableFields.length + (canEdit ? 1 : 0)" class="empty-state">暂无符合条件的数据</td></tr>
          </tbody>
        </table>
      </div>
      <div v-if="!loading && filteredRows.length > pageSize" class="pagination" aria-label="分页导航">
        <button class="secondary-button" :disabled="page === 1" @click="page--">上一页</button>
        <span>第 {{ page }} / {{ pageCount }} 页</span>
        <button class="secondary-button" :disabled="page === pageCount" @click="page++">下一页</button>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="modalOpen" class="modal-backdrop" @mousedown.self="modalOpen = false">
        <form class="modal-card" @submit.prevent="save">
          <div class="modal-heading"><div><h2>{{ editingId ? '编辑' : '新增' }}{{ config.title }}</h2><p>请按业务要求完整填写字段</p></div><button type="button" class="icon-btn" @click="modalOpen = false"><X :size="20" /></button></div>
          <div class="form-grid">
            <label v-for="field in config.fields.filter((item) => item.form !== false)" :key="field.key" :class="{ full: field.full }">
              {{ field.label }}<small v-if="field.optionalOnEdit && editingId">（留空则不修改）</small>
              <small v-if="field.help" class="field-help">{{ field.help }}</small>
              <select v-if="field.type === 'select'" v-model="form[field.key]" required>
                <option v-for="option in field.options" :key="String(optionValue(option))" :value="optionValue(option)">{{ optionLabel(option) }}</option>
              </select>
              <textarea v-else-if="field.type === 'textarea'" v-model.trim="form[field.key]" :placeholder="field.placeholder" rows="8" required />
              <input v-else v-model="form[field.key]" :type="field.type || 'text'" :minlength="field.minlength" :maxlength="field.maxlength" :required="!(field.optionalOnEdit && editingId)" />
            </label>
          </div>
          <div class="modal-actions"><button type="button" class="secondary-button" @click="modalOpen = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '保存中…' : '保存记录' }}</button></div>
        </form>
      </div>
    </Teleport>
  </section>
</template>

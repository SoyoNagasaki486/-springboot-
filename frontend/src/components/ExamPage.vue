<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckCircle2, ClipboardCheck, RotateCcw, Send, XCircle } from 'lucide-vue-next'
import { api, jsonOptions } from '@/services/api'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['notify'])
const questions = ref([])
const records = ref([])
const answers = reactive({})
const loading = ref(true)
const submitting = ref(false)
const result = ref(null)

const answeredCount = computed(() => Object.values(answers).filter(Boolean).length)
const ownRecords = computed(() => records.value.filter((row) => row.user_name === props.user.real_name).slice(0, 6))

onMounted(load)

async function load() {
  try {
    const [questionRows, recordRows] = await Promise.all([api('/exam/questions'), api('/exam/records')])
    questions.value = questionRows
    records.value = recordRows
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}

async function submit() {
  if (answeredCount.value !== questions.value.length) {
    emit('notify', { message: '请完成全部题目后再提交', type: 'error' }); return
  }
  submitting.value = true
  try {
    result.value = await api('/exam/submit', jsonOptions('POST', { answers }))
    const fresh = await api('/exam/records')
    records.value = fresh
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { submitting.value = false }
}

async function restart() {
  Object.keys(answers).forEach((key) => delete answers[key])
  result.value = null
  loading.value = true
  try {
    questions.value = await api('/exam/questions')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>在线安全准入考试</h1><p>系统每次随机组卷，按本次试卷总分折算百分制，达到 60 分即通过考试。</p></div><div class="exam-progress"><b>{{ answeredCount }}/{{ questions.length }}</b><span>已完成题目</span></div></div>
    <div v-if="loading" class="loading-state">正在加载考试题目…</div>
    <div v-else class="exam-layout">
      <form class="exam-paper panel" @submit.prevent="submit">
        <div class="exam-paper-head"><ClipboardCheck :size="22" /><div><h2>实验室安全知识测试</h2><p>本次随机抽取 {{ questions.length }} 道题，请独立完成后提交</p></div></div>
        <article v-for="(question, index) in questions" :key="question.id" class="question-block">
          <div class="question-number">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="question-body"><h3>{{ question.content }}<span>{{ question.score }} 分</span></h3>
            <label v-for="key in ['A', 'B', 'C', 'D']" :key="key" class="answer-option" :class="{ selected: answers[question.id] === key }">
              <input v-model="answers[question.id]" type="radio" :name="String(question.id)" :value="key" />
              <b>{{ key }}</b><span>{{ question[`option_${key.toLowerCase()}`] }}</span>
            </label>
          </div>
        </article>
        <div class="exam-submit"><span>提交后将立即判分并保存成绩</span><button class="primary-button" :disabled="submitting || !questions.length"><Send :size="17" />{{ submitting ? '正在判分…' : '提交试卷' }}</button></div>
      </form>

      <aside class="panel exam-history">
        <div class="panel-heading"><div><h2>我的考试记录</h2><p>最近 {{ ownRecords.length }} 次成绩</p></div></div>
        <article v-for="record in ownRecords" :key="record.id"><div><strong>{{ record.score }}</strong><span>分</span></div><p><b>{{ record.passed ? '已通过' : '未通过' }}</b><span>{{ record.exam_time }}</span></p></article>
        <div v-if="!ownRecords.length" class="empty-state">尚无考试记录</div>
      </aside>
    </div>

    <Teleport to="body"><div v-if="result" class="modal-backdrop"><article class="result-card">
      <component :is="result.passed ? CheckCircle2 : XCircle" :size="54" :class="result.passed ? 'pass-icon' : 'fail-icon'" />
      <h2>{{ result.passed ? '考试通过' : '本次未通过' }}</h2><div class="result-score">{{ result.score }}<span>分</span></div>
      <p>{{ result.passed ? '成绩已保存，请等待教师或管理员完成准入资格审核。' : '请继续完成安全课程学习，复习后可再次参加考试。' }}</p>
      <button class="primary-button" @click="restart"><RotateCcw :size="17" />重新随机组卷</button>
    </article></div></Teleport>
  </section>
</template>

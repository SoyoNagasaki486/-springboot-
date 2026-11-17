<script setup>
import { computed, onMounted, ref } from 'vue'
import { BookOpen, CheckCircle2, ChevronLeft, ChevronRight, Clock3, ListChecks, Play, ShieldCheck, X } from 'lucide-vue-next'
import { api, jsonOptions } from '@/services/api'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['notify'])
const courses = ref([])
const records = ref([])
const selected = ref(null)
const saving = ref(false)
const loading = ref(true)
const currentSection = ref(0)
const acknowledged = ref(false)

const publishedCourses = computed(() => courses.value.filter((course) => course.status === '已发布'))
const readerSections = computed(() => parseCourseContent(selected.value?.content))
const activeSection = computed(() => readerSections.value[currentSection.value])
const selectedRecord = computed(() => selected.value ? recordFor(selected.value) : null)
const completed = computed(() => Boolean(selectedRecord.value?.completed))
const readerProgress = computed(() => {
  if (completed.value) return 100
  if (readerSections.value.length <= 1) return 90
  return Math.round((currentSection.value / (readerSections.value.length - 1)) * 90)
})

onMounted(load)

async function load() {
  try {
    const [courseRows, recordRows] = await Promise.all([api('/manage/courses'), api('/manage/study-records')])
    courses.value = courseRows
    records.value = recordRows.filter((row) => row.user_name === props.user.real_name)
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { loading.value = false }
}

function recordFor(course) {
  return records.value.find((row) => row.course_title === course.title)
}

function courseSummary(course) {
  const lines = String(course.content || '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const summary = lines.find((line) => !line.startsWith('##') && !line.startsWith('-') && !line.startsWith('>'))
    || lines.find((line) => line.startsWith('-'))?.slice(1).trim()
  return summary || '进入课程查看完整培训内容和学习要求。'
}

function parseCourseContent(content = '') {
  const sections = []
  let section = { title: '课程导学', paragraphs: [], points: [], notes: [] }
  const pushSection = () => {
    if (section.paragraphs.length || section.points.length || section.notes.length) sections.push(section)
  }
  String(content).split(/\r?\n/).map((line) => line.trim()).filter(Boolean).forEach((line) => {
    if (line.startsWith('## ')) {
      pushSection()
      section = { title: line.slice(3).trim(), paragraphs: [], points: [], notes: [] }
    } else if (line.startsWith('- ')) {
      section.points.push(line.slice(2).trim())
    } else if (line.startsWith('> ')) {
      section.notes.push(line.slice(2).trim())
    } else {
      section.paragraphs.push(line)
    }
  })
  pushSection()
  return sections.length ? sections : [{
    title: '课程导学',
    paragraphs: ['课程内容正在完善，请联系实验室管理员。'],
    points: [],
    notes: [],
  }]
}

function openCourse(course) {
  selected.value = course
  acknowledged.value = false
  const sections = parseCourseContent(course.content)
  const progress = Number(recordFor(course)?.progress || 0)
  currentSection.value = progress >= 100 || sections.length <= 1
    ? 0 : Math.min(sections.length - 1, Math.floor((progress / 90) * (sections.length - 1)))
}

function syncLocalProgress(progress) {
  const current = recordFor(selected.value)
  if (current) {
    current.progress = Math.max(Number(current.progress || 0), progress)
    current.completed = progress === 100 || current.completed
  } else {
    records.value.push({
      user_name: props.user.real_name,
      course_title: selected.value.title,
      progress,
      completed: progress === 100,
    })
  }
}

async function nextSection() {
  if (currentSection.value >= readerSections.value.length - 1) return
  saving.value = true
  try {
    const next = currentSection.value + 1
    if (!completed.value) {
      const progress = Math.max(
        Number(selectedRecord.value?.progress || 0),
        Math.round((next / (readerSections.value.length - 1)) * 90),
      )
      await api('/study/progress', jsonOptions('POST', { courseId: selected.value.id, progress }))
      syncLocalProgress(progress)
    }
    currentSection.value = next
  } catch (exception) {
    emit('notify', { message: exception.message, type: 'error' })
  } finally { saving.value = false }
}

async function completeCourse() {
  if (!acknowledged.value || currentSection.value !== readerSections.value.length - 1) {
    emit('notify', { message: '请完成全部章节并确认已阅读安全要求', type: 'error' })
    return
  }
  saving.value = true
  try {
    const result = await api('/study/progress', jsonOptions('POST', {
      courseId: selected.value.id, progress: 100,
    }))
    emit('notify', result.message)
    selected.value = null
    await load()
  } catch (exception) { emit('notify', { message: exception.message, type: 'error' }) }
  finally { saving.value = false }
}
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>安全培训中心</h1><p>完成规定课程学习，是参加准入考试和资格审核的重要依据。</p></div></div>
    <div v-if="loading" class="loading-state">正在加载培训课程…</div>
    <div v-else class="course-grid">
      <article v-for="(course, index) in publishedCourses" :key="course.id" class="course-card">
        <div class="course-cover" :class="`cover-${index % 4}`"><BookOpen :size="32" /><span>{{ course.category }}</span></div>
        <div class="course-content">
          <div class="course-meta"><span><Clock3 :size="15" />{{ course.duration }} 分钟</span><span v-if="recordFor(course)?.completed" class="completed"><CheckCircle2 :size="15" />已完成</span></div>
          <h2>{{ course.title }}</h2><p>{{ courseSummary(course) }}</p>
          <div class="progress-track"><i :style="{ width: `${recordFor(course)?.progress || 0}%` }" /></div>
          <div class="course-actions"><span>学习进度 {{ recordFor(course)?.progress || 0 }}%</span><button class="primary-button small" @click="openCourse(course)"><Play :size="15" />{{ recordFor(course)?.completed ? '复习课程' : recordFor(course)?.progress ? '继续学习' : '开始学习' }}</button></div>
        </div>
      </article>
      <div v-if="!publishedCourses.length" class="empty-state panel">当前没有已发布课程</div>
    </div>

    <Teleport to="body">
      <div v-if="selected" class="modal-backdrop" @mousedown.self="selected = null">
        <article class="modal-card course-reader">
          <div class="modal-heading"><div><span class="reader-category">{{ selected.category }}</span><h2>{{ selected.title }}</h2><p>建议学习时长 {{ selected.duration }} 分钟 · 共 {{ readerSections.length }} 个章节</p></div><button class="icon-btn" aria-label="关闭课程" @click="selected = null"><X :size="20" /></button></div>
          <div class="reader-progress"><div><span>章节 {{ currentSection + 1 }} / {{ readerSections.length }}</span><b>{{ completed ? '已完成' : `学习进度 ${readerProgress}%` }}</b></div><div class="progress-track"><i :style="{ width: `${readerProgress}%` }" /></div></div>
          <div v-if="activeSection" class="reader-content">
            <div class="reader-section-heading"><span><BookOpen :size="17" />第 {{ currentSection + 1 }} 章</span><h3>{{ activeSection.title }}</h3></div>
            <p v-for="paragraph in activeSection.paragraphs" :key="paragraph">{{ paragraph }}</p>
            <ul v-if="activeSection.points.length" class="reader-points"><li v-for="point in activeSection.points" :key="point"><ListChecks :size="17" /><span>{{ point }}</span></li></ul>
            <div v-for="note in activeSection.notes" :key="note" class="reader-note"><ShieldCheck :size="18" /><span>{{ note }}</span></div>
            <label v-if="currentSection === readerSections.length - 1 && !completed" class="reader-acknowledgement"><input v-model="acknowledged" type="checkbox" /><span>我已阅读并理解本课程全部安全要求，知晓违规操作可能造成的风险。</span></label>
            <div v-else-if="currentSection === readerSections.length - 1" class="reader-completed"><CheckCircle2 :size="19" />该课程已完成，可随时复习。</div>
          </div>
          <div class="modal-actions reader-actions">
            <button class="secondary-button" @click="selected = null">稍后继续</button>
            <button v-if="currentSection > 0" class="secondary-button" :disabled="saving" @click="currentSection--"><ChevronLeft :size="17" />上一章</button>
            <button v-if="currentSection < readerSections.length - 1" class="primary-button" :disabled="saving" @click="nextSection">下一章<ChevronRight :size="17" /></button>
            <button v-else class="primary-button" :disabled="saving || completed || !acknowledged" @click="completeCourse"><CheckCircle2 :size="17" />{{ completed ? '课程已完成' : saving ? '记录中…' : '确认完成学习' }}</button>
          </div>
        </article>
      </div>
    </Teleport>
  </section>
</template>

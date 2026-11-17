<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { AlertTriangle, LogOut, X } from 'lucide-vue-next'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  tone: { type: String, default: 'danger' },
})
const emit = defineEmits(['confirm', 'cancel'])
const dialog = ref(null)

watch(() => props.open, async (open) => {
  document.body.classList.toggle('dialog-open', open)
  if (open) {
    await nextTick()
    dialog.value?.focus()
  }
})

onBeforeUnmount(() => document.body.classList.remove('dialog-open'))

function cancel() { emit('cancel') }
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="open" class="confirm-backdrop" @mousedown.self="cancel">
        <section ref="dialog" class="confirm-dialog" role="alertdialog" aria-modal="true"
                 aria-labelledby="confirm-title" aria-describedby="confirm-message" tabindex="-1"
                 @keydown.esc.prevent="cancel">
          <button class="icon-btn confirm-close" type="button" aria-label="关闭确认弹窗" @click="cancel"><X :size="19" /></button>
          <div class="confirm-icon" :class="tone">
            <LogOut v-if="tone === 'neutral'" :size="23" />
            <AlertTriangle v-else :size="23" />
          </div>
          <div class="confirm-copy">
            <h2 id="confirm-title">{{ title }}</h2>
            <p id="confirm-message">{{ message }}</p>
          </div>
          <div class="confirm-actions">
            <button type="button" class="secondary-button" @click="cancel">取消</button>
            <button type="button" class="confirm-button" :class="tone" @click="emit('confirm')">{{ confirmText }}</button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

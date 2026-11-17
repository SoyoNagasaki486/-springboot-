<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { AtSign, BadgeCheck, Hash, LogOut, ShieldCheck, UserRound, X } from 'lucide-vue-next'

const props = defineProps({
  open: { type: Boolean, default: false },
  user: { type: Object, required: true },
  roleLabel: { type: String, default: '用户' },
})
const emit = defineEmits(['close', 'logout'])
const dialog = ref(null)

watch(() => props.open, async (open) => {
  document.body.classList.toggle('dialog-open', open)
  if (open) {
    await nextTick()
    dialog.value?.focus()
  }
})

onBeforeUnmount(() => document.body.classList.remove('dialog-open'))
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="open" class="profile-backdrop" @mousedown.self="emit('close')">
        <section ref="dialog" class="profile-dialog" role="dialog" aria-modal="true"
                 aria-labelledby="profile-title" tabindex="-1" @keydown.esc.prevent="emit('close')">
          <button class="icon-btn profile-close" type="button" aria-label="关闭用户信息" @click="emit('close')"><X :size="19" /></button>
          <header class="profile-hero">
            <div class="profile-avatar">{{ user.real_name.slice(-1) }}</div>
            <div>
              <span class="profile-eyebrow"><UserRound :size="14" />当前登录用户</span>
              <h2 id="profile-title">{{ user.real_name }}</h2>
              <p>{{ roleLabel }} · 账号状态正常</p>
            </div>
          </header>

          <div class="profile-details">
            <article><AtSign :size="18" /><div><span>登录账号</span><strong>{{ user.username }}</strong></div></article>
            <article><UserRound :size="18" /><div><span>真实姓名</span><strong>{{ user.real_name }}</strong></div></article>
            <article><ShieldCheck :size="18" /><div><span>系统角色</span><strong>{{ roleLabel }}</strong></div></article>
            <article><Hash :size="18" /><div><span>用户编号</span><strong>#{{ user.id }}</strong></div></article>
          </div>

          <div class="profile-access" :class="{ qualified: user.qualified }">
            <BadgeCheck :size="20" />
            <div><span>实验室准入状态</span><strong>{{ user.qualified ? '已取得有效资格' : '暂未取得有效资格' }}</strong></div>
          </div>

          <footer class="profile-actions">
            <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
            <button class="profile-logout" type="button" @click="emit('logout')"><LogOut :size="17" />安全退出</button>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

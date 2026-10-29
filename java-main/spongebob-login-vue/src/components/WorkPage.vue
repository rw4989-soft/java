<template>
  <div class="work">
    <OceanBackdrop />
    <PageNav active="work" :nickname="displayName" @logout="$emit('logout')" />

    <main class="main">
      <!-- ============ 页头 ============ -->
      <section class="hero">
        <p class="kicker">🕐 {{ todayLabel }} · 比奇堡时间</p>
        <h1 class="hero-title">打工日常</h1>
        <p class="hero-sub">在蟹堡王，每个人都有自己的班要上。今天轮到谁？</p>

        <div class="hero-stats">
          <div class="hs">
            <b>{{ totalShifts }}</b>
            <span>总班次</span>
          </div>
          <div class="hs">
            <b>{{ totalCoins }}</b>
            <span>蟹堡币</span>
          </div>
          <div class="hs">
            <b>{{ bestStreak }}</b>
            <span>最长连打</span>
          </div>
          <div class="hs">
            <b>{{ totalS }}</b>
            <span>S 级班次</span>
          </div>
        </div>
      </section>

      <!-- ============ 骨架 ============ -->
      <div v-if="loading" class="roles">
        <div v-for="n in 2" :key="n" class="role-card skeleton">
          <span class="sk sk-avatar"></span>
          <span class="sk sk-line w60"></span>
          <span class="sk sk-line"></span>
          <span class="sk sk-line w80"></span>
        </div>
      </div>

      <!-- ============ 出错 ============ -->
      <div v-else-if="error" class="empty">
        <div class="empty-emoji">🐙</div>
        <p class="empty-title">{{ error }}</p>
        <p class="empty-sub">可能是后端没启动，或者这个接口没连上</p>
        <button type="button" class="ghost-btn" @click="load">重试一次</button>
      </div>

      <!-- ============ 角色卡 ============ -->
      <section v-else class="roles">
        <article
          v-for="(r, ri) in roles"
          :key="r.code"
          class="role-card"
          :class="{ done: r.todayDone }"
          :style="{ '--accent': r.accent, animationDelay: ri * 0.09 + 's' }"
        >
          <span class="card-shine"></span>

          <div class="role-top">
            <div class="avatar-wrap">
              <span class="avatar-big">{{ r.emoji }}</span>
            </div>
            <div class="role-id">
              <h2>{{ r.name }}</h2>
              <p class="role-job">{{ r.job }}</p>
              <p class="role-place">📍 {{ r.workplace }}</p>
            </div>
            <span class="level-badge">{{ r.level }}</span>
          </div>

          <p class="tagline">「{{ r.tagline }}」</p>

          <div class="gauges">
            <div class="gauge">
              <div class="gauge-head">
                <span>心情</span>
                <b>{{ r.mood }} <em>{{ r.moodLabel }}</em></b>
              </div>
              <div class="bar">
                <i :style="{ width: r.mood + '%', background: moodColor(r.mood) }"></i>
              </div>
            </div>

            <div class="gauge">
              <div class="gauge-head">
                <span>绩效</span>
                <b>{{ r.performance }}</b>
              </div>
              <div class="bar">
                <i class="perf" :style="{ width: r.levelProgress + '%' }"></i>
              </div>
              <p class="gauge-hint">{{ nextHint(r) }}</p>
            </div>
          </div>

          <div class="wallet">
            <span class="coin">💰 {{ r.coins }}</span>
            <span v-if="r.streak" class="streak" :class="{ hot: r.streak >= 3 }">🔥 连续 {{ r.streak }} 天</span>
            <span class="count">已上 {{ r.shiftCount }} 班</span>
          </div>

          <div class="grades">
            <span class="grades-label">评级</span>
            <span v-for="g in ['S', 'A', 'B', 'C']" :key="g" class="grade-chip" :class="'g-' + g">
              {{ g }} <b>{{ r.gradeCounts[g] || 0 }}</b>
            </span>
          </div>

          <button
            type="button"
            class="start"
            :disabled="r.todayDone"
            @click="startShift(r)"
          >
            <template v-if="r.todayDone">✅ 今天已打卡 · 明天见</template>
            <template v-else>🕐 打卡上班 <small>5 幕剧情</small></template>
          </button>
        </article>
      </section>

      <!-- ============ 打卡时间轴 ============ -->
      <section v-if="!loading && !error" class="timeline-wrap">
        <div class="sec-head">
          <h2>打卡记录 <small>最近 {{ shifts.length }} 次</small></h2>
          <span class="sec-note">每打完一班就写一条，像一条海底的痕迹</span>
        </div>

        <div v-if="shifts.length" class="timeline">
          <div v-for="s in shifts" :key="s.id" class="tl-item">
            <span class="tl-dot" :class="'dot-' + s.grade">{{ s.emoji }}</span>
            <div class="tl-card">
              <div class="tl-head">
                <strong>{{ s.roleName }}</strong>
                <span class="tl-grade" :class="'g-' + s.grade">{{ s.grade }}</span>
                <span class="tl-date">{{ s.dateLabel }}</span>
              </div>
              <p class="tl-summary">{{ s.summary }}</p>
              <div class="tl-nums">
                <span :class="s.coinsGain >= 0 ? 'up' : 'down'">💰 {{ signed(s.coinsGain) }}</span>
                <span :class="s.moodDelta >= 0 ? 'up' : 'down'">😊 {{ signed(s.moodDelta) }}</span>
                <span :class="s.performDelta >= 0 ? 'up' : 'down'">📈 {{ signed(s.performDelta) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="tl-empty">
          <span class="empty-emoji small">🐚</span>
          <p>还没有打卡记录。今天，就是第一天。</p>
        </div>
      </section>

      <footer class="footer">比奇堡 · 打工日常 · 每一班都算数 🧽</footer>
    </main>

    <!-- ============ 班次层 ============ -->
    <transition name="layer">
      <div v-if="session" class="layer" @click.self="askExit">
        <div class="shift-card">
          <!-- 顶栏 -->
          <div class="shift-head">
            <span class="s-emoji">{{ session.role.emoji }}</span>
            <div class="s-meta">
              <strong>{{ session.role.name }}</strong>
              <small v-if="phase !== 'summary'">第 {{ session.index + 1 }} / {{ session.scenes.length }} 幕</small>
              <small v-else>班次已结算</small>
            </div>

            <div v-if="phase !== 'summary'" class="s-gain">
              <span :class="live.coins >= 0 ? 'up' : 'down'">💰 {{ signed(live.coins) }}</span>
              <span :class="live.mood >= 0 ? 'up' : 'down'">😊 {{ signed(live.mood) }}</span>
              <span :class="live.perf >= 0 ? 'up' : 'down'">📈 {{ signed(live.perf) }}</span>
            </div>

            <button type="button" class="s-close" aria-label="结束本次班次" @click="askExit">×</button>
          </div>

          <div v-if="phase !== 'summary'" class="progress">
            <i :style="{ width: progressPct + '%' }"></i>
          </div>

          <!-- 退出确认 -->
          <transition name="fade">
            <div v-if="exitAsk" class="exit-ask">
              <span>现在走的话，这班算没上成，进度不保留。</span>
              <button type="button" class="mini danger" @click="abandonShift">确定走</button>
              <button type="button" class="mini" @click="exitAsk = false">再站会儿</button>
            </div>
          </transition>

          <!-- 正文 -->
          <transition name="swap" mode="out-in">
            <!-- 场景 -->
            <div v-if="phase === 'scene' && scene" :key="'scene-' + session.index" class="scene-body">
              <div class="scene-tags">
                <span>🕐 {{ scene.time }}</span>
                <span>📍 {{ scene.place }}</span>
              </div>
              <div class="scene-icon">{{ scene.icon }}</div>
              <h3 class="scene-title">{{ scene.title }}</h3>
              <p class="scene-desc">{{ scene.desc }}</p>

              <div class="choices">
                <button
                  v-for="(c, i) in scene.choices"
                  :key="i"
                  type="button"
                  class="choice"
                  :class="'tone-' + c.tone"
                  @click="choose(i)"
                >
                  <span class="choice-idx">{{ ['A', 'B', 'C'][i] }}</span>
                  <span class="choice-text">{{ c.text }}</span>
                  <span class="choice-hint">{{ toneLabel(c.tone) }}</span>
                </button>
              </div>
            </div>

            <!-- 结果 -->
            <div v-else-if="phase === 'result' && currentChoice" :key="'result-' + session.index" class="result-body">
              <div class="result-icon">{{ scene.icon }}</div>
              <p class="result-choice">「{{ currentChoice.text }}」</p>
              <p class="result-text">{{ currentChoice.result }}</p>

              <div class="result-deltas">
                <span v-if="currentChoice.coins" :class="currentChoice.coins > 0 ? 'up' : 'down'">
                  💰 {{ signed(currentChoice.coins) }}
                </span>
                <span v-if="currentChoice.mood" :class="currentChoice.mood > 0 ? 'up' : 'down'">
                  😊 {{ signed(currentChoice.mood) }}
                </span>
                <span v-if="currentChoice.perf" :class="currentChoice.perf > 0 ? 'up' : 'down'">
                  📈 {{ signed(currentChoice.perf) }}
                </span>
                <span v-if="!currentChoice.coins && !currentChoice.mood && !currentChoice.perf" class="flat">
                  — 数值没有变化
                </span>
              </div>

              <button type="button" class="continue" @click="next">
                {{ isLast ? '下班 · 结算这一班' : '继续下一幕' }} →
              </button>
            </div>

            <!-- 结算 -->
            <div v-else-if="phase === 'summary' && report" key="summary" class="summary-body">
              <div class="stamp" :class="'stamp-' + report.grade">{{ report.grade }}</div>
              <h3 class="st-title">{{ report.title }}</h3>
              <p class="st-comment">{{ report.comment }}</p>

              <div class="st-grid">
                <div class="st-cell">
                  <span>蟹堡币</span>
                  <b :class="report.coinsGain >= 0 ? 'up' : 'down'">{{ signed(report.coinsGain) }}</b>
                  <small>共 {{ report.coins }}</small>
                </div>
                <div class="st-cell">
                  <span>心情</span>
                  <b :class="report.moodDelta >= 0 ? 'up' : 'down'">{{ signed(report.moodDelta) }}</b>
                  <small>{{ report.mood }} · {{ report.moodLabel }}</small>
                </div>
                <div class="st-cell">
                  <span>绩效</span>
                  <b :class="report.performDelta >= 0 ? 'up' : 'down'">{{ signed(report.performDelta) }}</b>
                  <small>共 {{ report.performance }}</small>
                </div>
                <div class="st-cell">
                  <span>连续打卡</span>
                  <b>{{ report.streak }} 天</b>
                  <small>第 {{ report.shiftCount }} 班</small>
                </div>
              </div>

              <div v-if="report.levelUp" class="levelup">
                🎊 职级提升 · {{ report.level }}
              </div>

              <button type="button" class="continue" @click="backToLobby">回到更衣室</button>
            </div>

            <div v-else class="scene-body loading-body" key="loading">
              <div class="spinner"></div>
              <p>正在收拾今天的工位…</p>
            </div>
          </transition>

          <!-- 浮动数值 -->
          <div class="floats" aria-hidden="true">
            <span
              v-for="f in floats"
              :key="f.id"
              class="float"
              :class="f.cls"
              :style="{ animationDelay: (f.delay || 0) + 'ms' }"
            >{{ f.text }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 轻提示 -->
    <transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import OceanBackdrop from './OceanBackdrop.vue'
import PageNav from './PageNav.vue'
import { drawShift, getWorkOverview, settleShift } from '../api/work.js'
import { getNickname } from '../utils/auth.js'
import { sfx } from '../utils/sound.js'

const props = defineProps({
  nickname: { type: String, default: '' }
})

defineEmits(['logout'])

const router = useRouter()
const displayName = computed(() => props.nickname || getNickname() || '海绵宝宝')

// ==================== 大厅状态 ====================
const loading = ref(true)
const error = ref('')
const roles = ref([])
const levels = ref([])
const shifts = ref([])
const totalShifts = ref(0)
const totalCoins = ref(0)
const bestStreak = ref(0)
const totalS = ref(0)
const todayLabel = ref('')

// ==================== 班次状态 ====================
const session = ref(null)       // { role, scenes, index, picks }
const phase = ref('scene')      // scene | result | summary
const scene = ref(null)
const currentChoice = ref(null)
const live = ref({ coins: 0, mood: 0, perf: 0 })
const report = ref(null)
const exitAsk = ref(false)
const floats = ref([])
let floatSeq = 0

const toast = ref('')
let toastTimer = null

// ==================== 计算 ====================
const progressPct = computed(() => {
  if (!session.value) return 0
  const total = session.value.scenes.length
  const done = session.value.index + (phase.value === 'result' ? 1 : 0)
  // 第一幕给一点点底，否则进度条全空，看着像「还没开始」
  return Math.max(6, Math.round((done / total) * 100))
})

const isLast = computed(() => session.value && session.value.index === session.value.scenes.length - 1)

// ==================== 工具 ====================
function signed(n) {
  return n > 0 ? `+${n}` : String(n)
}

function moodColor(v) {
  if (v >= 70) return 'linear-gradient(90deg, #4dd0e1, #80cbc4)'
  if (v >= 40) return 'linear-gradient(90deg, #ffd54f, #ffb74d)'
  return 'linear-gradient(90deg, #ff8a65, #e57373)'
}

const TONE = {
  money: '偏赚钱',
  mood: '偏心情',
  perf: '偏绩效',
  risky: '看运气'
}
function toneLabel(tone) {
  return TONE[tone] || '选择'
}

function nextHint(r) {
  const idx = levels.value.findIndex((l) => l.name === r.level)
  const next = levels.value[idx + 1]
  if (!next) return '已经是最高职级 · 蟹堡王的传说'
  const need = Math.max(0, next.min - r.performance)
  return `距「${next.name}」还差 ${need} 绩效`
}

function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2400)
}

function goLogin() {
  router.replace({ name: 'login' })
}

function spawnFloat(c) {
  const items = []
  if (c.coins) items.push({ text: `💰 ${signed(c.coins)}`, cls: c.coins > 0 ? 'up' : 'down' })
  if (c.mood) items.push({ text: `😊 ${signed(c.mood)}`, cls: c.mood > 0 ? 'up' : 'down' })
  if (c.perf) items.push({ text: `📈 ${signed(c.perf)}`, cls: c.perf > 0 ? 'up' : 'down' })

  items.forEach((it, i) => {
    const id = ++floatSeq
    floats.value.push({ id, ...it, delay: i * 110 })
    setTimeout(() => {
      floats.value = floats.value.filter((f) => f.id !== id)
    }, 1600)
  })
}

// ==================== 数据 ====================
async function load() {
  error.value = ''
  const res = await getWorkOverview()
  if (!res.ok) {
    loading.value = false
    if (res.code === 401) return goLogin()
    error.value = res.message
    return
  }

  const d = res.data || {}
  roles.value = d.roles || []
  levels.value = d.levels || []
  shifts.value = d.recent || []
  totalShifts.value = d.totalShifts || 0
  todayLabel.value = d.todayLabel || ''

  totalCoins.value = roles.value.reduce((s, r) => s + (r.coins || 0), 0)
  bestStreak.value = roles.value.reduce((m, r) => Math.max(m, r.streak || 0), 0)
  totalS.value = roles.value.reduce((s, r) => s + ((r.gradeCounts && r.gradeCounts.S) || 0), 0)

  loading.value = false
}

// ==================== 班次流程 ====================
async function startShift(r) {
  if (r.todayDone) {
    showToast('今天已经打过卡啦，明天再来 🕐')
    return
  }

  sfx.click()
  const res = await drawShift(r.code)
  if (!res.ok) {
    if (res.code === 401) return goLogin()
    showToast(res.message)
    return
  }

  const d = res.data
  session.value = {
    role: { ...r, emoji: d.emoji || r.emoji },
    scenes: d.scenes || [],
    index: 0,
    picks: []
  }
  scene.value = session.value.scenes[0]
  phase.value = 'scene'
  currentChoice.value = null
  live.value = { coins: 0, mood: 0, perf: 0 }
  report.value = null
  exitAsk.value = false
  sfx.select()
}

function choose(i) {
  if (phase.value !== 'scene' || !scene.value) return

  const c = scene.value.choices[i]
  session.value.picks.push({ sceneId: scene.value.id, choiceIndex: i })
  currentChoice.value = c

  live.value = {
    coins: live.value.coins + c.coins,
    mood: live.value.mood + c.mood,
    perf: live.value.perf + c.perf
  }

  phase.value = 'result'
  spawnFloat(c)

  if (c.coins > 0 || c.mood > 0 || c.perf > 0) sfx.coin()
  else sfx.select()
}

async function next() {
  if (!session.value) return

  if (!isLast.value) {
    sfx.click()
    session.value.index += 1
    scene.value = session.value.scenes[session.value.index]
    currentChoice.value = null
    phase.value = 'scene'
    return
  }

  await doSettle()
}

async function doSettle() {
  phase.value = 'summary'
  report.value = null

  const res = await settleShift(session.value.role.code, session.value.picks)
  if (!res.ok) {
    if (res.code === 401) return goLogin()
    showToast(res.message)
    backToLobby()
    await load()
    return
  }

  report.value = res.data
  sfx.win()
  await load()
}

function askExit() {
  if (phase.value === 'summary') {
    backToLobby()
    return
  }
  exitAsk.value = true
  setTimeout(() => { exitAsk.value = false }, 6000)
}

function abandonShift() {
  sfx.bad()
  session.value = null
  exitAsk.value = false
  showToast('这班算没上成，明天补上 🙂')
}

function backToLobby() {
  sfx.click()
  session.value = null
  scene.value = null
  currentChoice.value = null
  report.value = null
  phase.value = 'scene'
  exitAsk.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape' && session.value) askExit()
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.work {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #eaf7ff;
}

.main {
  position: relative;
  z-index: 2;
  flex: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 38px 20px 60px;
}

/* ================= 页头 ================= */
.hero {
  text-align: center;
  margin-bottom: 34px;
  animation: fadeUp 0.55s ease both;
}

.kicker {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 999px;
  background: rgba(126, 226, 255, 0.14);
  border: 1px solid rgba(126, 226, 255, 0.35);
  color: #b9e6ff;
  font-size: 12.5px;
  letter-spacing: 1px;
  margin-bottom: 14px;
}

.hero-title {
  font-size: 40px;
  letter-spacing: 4px;
  color: #fff8e1;
  text-shadow: 0 3px 0 rgba(0, 0, 0, 0.28), 0 0 26px rgba(255, 213, 79, 0.4);
}

.hero-sub {
  margin-top: 8px;
  color: #b9e6ff;
  font-size: 14.5px;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.hs {
  min-width: 104px;
  padding: 10px 18px;
  border-radius: 16px;
  background: rgba(6, 66, 104, 0.4);
  border: 1px solid rgba(160, 232, 255, 0.3);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.hs b {
  font-size: 22px;
  color: #ffd54f;
  text-shadow: 0 0 14px rgba(255, 193, 7, 0.4);
}
.hs span {
  font-size: 11.5px;
  color: #8fdcff;
  letter-spacing: 1px;
}

/* ================= 角色卡 ================= */
.roles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.role-card {
  position: relative;
  overflow: hidden;
  padding: 24px 24px 22px;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.13) 0%, rgba(255, 255, 255, 0.02) 46%, rgba(2, 44, 72, 0.18) 100%),
    rgba(6, 66, 104, 0.36);
  border: 2px solid rgba(160, 232, 255, 0.4);
  box-shadow: 0 16px 40px rgba(1, 26, 44, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.16);
  transition: transform 0.25s, border-color 0.25s, box-shadow 0.25s;
  animation: fadeUp 0.6s ease both;
}

.role-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--accent) 70%, transparent);
  box-shadow: 0 22px 50px rgba(1, 26, 44, 0.42), 0 0 30px color-mix(in srgb, var(--accent) 26%, transparent);
}

.role-card.done {
  opacity: 0.78;
}

.card-shine {
  position: absolute;
  inset: -40% -60% auto -60%;
  height: 70%;
  background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.22), transparent 65%);
  pointer-events: none;
}

.role-top {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
}

.avatar-wrap {
  position: relative;
  width: 68px;
  height: 68px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: radial-gradient(circle at 50% 35%, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.06));
  border: 2px solid color-mix(in srgb, var(--accent) 55%, transparent);
  box-shadow: 0 0 24px color-mix(in srgb, var(--accent) 30%, transparent);
}

.avatar-big {
  font-size: 38px;
  line-height: 1;
  animation: bob 3.2s ease-in-out infinite;
}

.role-id {
  flex: 1;
  min-width: 0;
}
.role-id h2 {
  font-size: 21px;
  color: #fff8e1;
  letter-spacing: 1px;
}
.role-job {
  margin-top: 3px;
  font-size: 12.5px;
  color: color-mix(in srgb, var(--accent) 80%, #ffffff);
}
.role-place {
  margin-top: 2px;
  font-size: 11.5px;
  color: #8fdcff;
  opacity: 0.85;
}

.level-badge {
  flex-shrink: 0;
  align-self: flex-start;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: bold;
  color: #5d3700;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  box-shadow: 0 2px 0 #c17900;
}

.tagline {
  margin: 16px 0 18px;
  padding-left: 12px;
  border-left: 3px solid color-mix(in srgb, var(--accent) 60%, transparent);
  font-size: 12.5px;
  line-height: 1.65;
  color: #b9e6ff;
  font-style: italic;
}

/* 数值条 */
.gauges {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.gauge-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
  color: #9fd8f5;
  margin-bottom: 6px;
}
.gauge-head b {
  color: #eaf7ff;
  font-size: 12.5px;
}
.gauge-head em {
  font-style: normal;
  font-size: 11px;
  color: #8fdcff;
  margin-left: 4px;
}

.bar {
  height: 9px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.5);
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
}
.bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.65s cubic-bezier(0.22, 1, 0.36, 1);
}
.bar i.perf {
  background: linear-gradient(90deg, #ffd54f, #ffb300);
  box-shadow: 0 0 12px rgba(255, 193, 7, 0.55);
}

.gauge-hint {
  margin-top: 5px;
  font-size: 11px;
  color: #7fc4e6;
}

/* 钱包 */
.wallet {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed rgba(126, 226, 255, 0.25);
  font-size: 12.5px;
}
.coin {
  font-weight: bold;
  color: #ffd54f;
  font-size: 14px;
}
.streak {
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(255, 152, 0, 0.16);
  color: #ffcc80;
  font-size: 11.5px;
}
.streak.hot {
  background: rgba(255, 87, 34, 0.22);
  color: #ffab91;
  animation: pulse 1.8s ease-in-out infinite;
}
.count {
  color: #8fdcff;
  font-size: 11.5px;
}

/* 评级 */
.grades {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.grades-label {
  font-size: 11px;
  color: #7fc4e6;
  margin-right: 2px;
}
.grade-chip {
  min-width: 40px;
  text-align: center;
  padding: 3px 8px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: bold;
  background: rgba(1, 30, 50, 0.42);
  border: 1px solid transparent;
}
.grade-chip b {
  font-size: 11.5px;
  margin-left: 1px;
}
.g-S { color: #ffd54f; border-color: rgba(255, 213, 79, 0.5); }
.g-A { color: #7ee2ff; border-color: rgba(126, 226, 255, 0.45); }
.g-B { color: #a5d6a7; border-color: rgba(165, 214, 167, 0.4); }
.g-C { color: #b0bec5; border-color: rgba(176, 190, 197, 0.32); }

/* 打卡按钮 */
.start {
  width: 100%;
  margin-top: 16px;
  padding: 13px 0;
  border: none;
  border-radius: 16px;
  font-family: inherit;
  font-size: 15px;
  font-weight: bold;
  letter-spacing: 1px;
  color: #4a2c00;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  box-shadow: 0 4px 0 #c17900, 0 8px 22px rgba(255, 152, 0, 0.32);
  cursor: pointer;
  transition: transform 0.14s, box-shadow 0.14s, filter 0.2s;
}
.start small {
  font-size: 11px;
  opacity: 0.7;
  font-weight: normal;
  margin-left: 4px;
}
.start:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #c17900, 0 12px 26px rgba(255, 152, 0, 0.42);
}
.start:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #c17900;
}
.start:disabled {
  cursor: default;
  background: rgba(126, 226, 255, 0.16);
  color: #9fd8f5;
  box-shadow: none;
  border: 2px dashed rgba(126, 226, 255, 0.32);
}

/* ================= 时间轴 ================= */
.timeline-wrap {
  margin-top: 44px;
}

.sec-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.sec-head h2 {
  font-size: 19px;
  color: #fff8e1;
  letter-spacing: 1px;
}
.sec-head small {
  font-size: 12px;
  color: #8fdcff;
  font-weight: normal;
  margin-left: 6px;
}
.sec-note {
  font-size: 11.5px;
  color: #7fc4e6;
}

.timeline {
  position: relative;
  padding-left: 30px;
}
.timeline::before {
  content: '';
  position: absolute;
  left: 13px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: linear-gradient(180deg, rgba(126, 226, 255, 0.55), rgba(126, 226, 255, 0.06));
}

.tl-item {
  position: relative;
  margin-bottom: 12px;
  animation: fadeUp 0.45s ease both;
}

.tl-dot {
  position: absolute;
  left: -30px;
  top: 12px;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  font-size: 14px;
  border-radius: 50%;
  background: rgba(3, 45, 74, 0.95);
  border: 2px solid rgba(126, 226, 255, 0.5);
  box-shadow: 0 0 12px rgba(126, 226, 255, 0.28);
}
.tl-dot.dot-S { border-color: #ffd54f; box-shadow: 0 0 14px rgba(255, 213, 79, 0.5); }
.tl-dot.dot-A { border-color: #7ee2ff; }
.tl-dot.dot-B { border-color: #a5d6a7; }
.tl-dot.dot-C { border-color: #90a4ae; }

.tl-card {
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(6, 66, 104, 0.3);
  border: 1px solid rgba(160, 232, 255, 0.24);
  transition: background 0.2s, border-color 0.2s;
}
.tl-card:hover {
  background: rgba(6, 66, 104, 0.44);
  border-color: rgba(160, 232, 255, 0.45);
}

.tl-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}
.tl-head strong {
  font-size: 13px;
  color: #eaf7ff;
}
.tl-grade {
  padding: 1px 7px;
  border-radius: 7px;
  font-size: 10.5px;
  font-weight: bold;
  background: rgba(1, 30, 50, 0.5);
}
.tl-date {
  margin-left: auto;
  font-size: 11px;
  color: #7fc4e6;
}

.tl-summary {
  font-size: 12.5px;
  color: #b9e6ff;
  line-height: 1.55;
}

.tl-nums {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  font-size: 11.5px;
}

.tl-empty {
  text-align: center;
  padding: 34px 0;
  color: #9fd8f5;
  font-size: 13px;
}
.empty-emoji.small {
  font-size: 30px;
  margin-bottom: 6px;
}

.up { color: #7ee2a8; }
.down { color: #ff8a80; }
.flat { color: #90a4ae; }

/* ================= 班次层 ================= */
.layer {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(1, 18, 32, 0.62);
  backdrop-filter: blur(10px) saturate(120%);
}

.shift-card {
  position: relative;
  width: min(720px, 100%);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  padding: 22px 26px 26px;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.02) 40%, rgba(2, 44, 72, 0.22) 100%),
    rgba(5, 58, 92, 0.94);
  border: 2px solid rgba(160, 232, 255, 0.42);
  box-shadow: 0 30px 80px rgba(0, 10, 20, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.shift-card::-webkit-scrollbar {
  width: 8px;
}
.shift-card::-webkit-scrollbar-thumb {
  background: rgba(126, 226, 255, 0.3);
  border-radius: 8px;
}

.shift-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.s-emoji {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  font-size: 25px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(160, 232, 255, 0.4);
}

.s-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  /* 撑满剩余宽度：这样右上角的 × 永远贴最右，
     不会因为「收益条」在结算阶段被隐藏而挤到名字旁边 */
  flex: 1;
  min-width: 0;
}
.s-meta strong {
  font-size: 15px;
  color: #fff8e1;
}
.s-meta small {
  font-size: 11.5px;
  color: #8fdcff;
}

.s-gain {
  margin-left: auto;
  display: flex;
  gap: 8px;
  font-size: 12px;
  font-weight: bold;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.45);
  border: 1px solid rgba(126, 226, 255, 0.24);
}

.s-close {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 18px;
  line-height: 1;
  color: #b9e6ff;
  background: transparent;
  border: 1px solid rgba(126, 226, 255, 0.3);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
}
.s-close:hover {
  background: rgba(255, 138, 128, 0.2);
  border-color: #ff8a80;
  color: #ffcdd2;
}

.progress {
  height: 5px;
  margin: 14px 0 4px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.55);
  overflow: hidden;
}
.progress i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #7ee2ff, #ffd54f);
  box-shadow: 0 0 12px rgba(255, 213, 79, 0.5);
  transition: width 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.exit-ask {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 138, 128, 0.14);
  border: 1px solid rgba(255, 138, 128, 0.4);
  font-size: 12px;
  color: #ffcdd2;
}
.exit-ask span {
  flex: 1;
  min-width: 160px;
}
.mini {
  padding: 5px 12px;
  border-radius: 9px;
  font-family: inherit;
  font-size: 12px;
  font-weight: bold;
  color: #b9e6ff;
  background: rgba(126, 226, 255, 0.14);
  border: 1px solid rgba(126, 226, 255, 0.35);
  cursor: pointer;
  transition: all 0.2s;
}
.mini:hover {
  background: rgba(126, 226, 255, 0.26);
}
.mini.danger {
  color: #ffcdd2;
  border-color: rgba(255, 138, 128, 0.5);
  background: rgba(255, 138, 128, 0.2);
}

/* ---- 场景 ---- */
.scene-body {
  text-align: center;
  padding-top: 14px;
}

.scene-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 11px;
  color: #9fd8f5;
  margin-bottom: 10px;
}
.scene-tags span {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(126, 226, 255, 0.1);
  border: 1px solid rgba(126, 226, 255, 0.22);
}

.scene-icon {
  font-size: 56px;
  line-height: 1;
  margin: 6px 0 14px;
  filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.35));
  animation: bob 3s ease-in-out infinite;
}

.scene-title {
  font-size: 22px;
  color: #fff8e1;
  letter-spacing: 2px;
  margin-bottom: 10px;
}

.scene-desc {
  max-width: 540px;
  margin: 0 auto 22px;
  font-size: 14px;
  line-height: 1.85;
  color: #cdeeff;
}

.choices {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.choice {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  text-align: left;
  font-family: inherit;
  font-size: 13.5px;
  color: #eaf7ff;
  background: rgba(6, 66, 104, 0.5);
  border: 2px solid rgba(160, 232, 255, 0.28);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.18s;
}

.choice:hover {
  transform: translateX(4px);
  background: rgba(10, 90, 140, 0.62);
  border-color: rgba(255, 213, 79, 0.65);
  box-shadow: 0 6px 22px rgba(0, 20, 36, 0.4), 0 0 20px rgba(255, 213, 79, 0.16);
}

.choice-idx {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: bold;
  color: #5d3700;
  background: #ffd54f;
  border-radius: 8px;
}

.choice-text {
  flex: 1;
  line-height: 1.55;
}

.choice-hint {
  flex-shrink: 0;
  font-size: 10.5px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.5);
  color: #9fd8f5;
}
.tone-money .choice-hint { color: #ffd54f; }
.tone-mood .choice-hint { color: #7ee2a8; }
.tone-perf .choice-hint { color: #7ee2ff; }
.tone-risky .choice-hint { color: #ffab91; }

/* ---- 结果 ---- */
.result-body {
  text-align: center;
  padding-top: 18px;
}

.result-icon {
  font-size: 46px;
  line-height: 1;
  margin-bottom: 12px;
  animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.result-choice {
  font-size: 13px;
  color: #ffd54f;
  margin-bottom: 12px;
}

.result-text {
  max-width: 540px;
  margin: 0 auto 18px;
  font-size: 14px;
  line-height: 1.9;
  color: #cdeeff;
  animation: fadeUp 0.45s ease both;
}

.result-deltas {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 22px;
  font-size: 13px;
  font-weight: bold;
}
.result-deltas span {
  padding: 5px 13px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.5);
  border: 1px solid rgba(126, 226, 255, 0.22);
  animation: pop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.continue {
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
  padding: 13px 0;
  display: block;
  border: none;
  border-radius: 15px;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: bold;
  letter-spacing: 1px;
  color: #4a2c00;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  box-shadow: 0 4px 0 #c17900, 0 10px 24px rgba(255, 152, 0, 0.3);
  cursor: pointer;
  transition: transform 0.14s, box-shadow 0.14s;
}
.continue:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #c17900, 0 14px 30px rgba(255, 152, 0, 0.4);
}
.continue:active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #c17900;
}

/* ---- 结算 ---- */
.summary-body {
  text-align: center;
  padding-top: 10px;
}

.stamp {
  width: 86px;
  height: 86px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  font-size: 40px;
  font-weight: bold;
  border-radius: 26px;
  border: 3px solid currentColor;
  transform: rotate(-6deg);
  animation: stampIn 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.stamp-S { color: #ffd54f; background: rgba(255, 213, 79, 0.1); box-shadow: 0 0 40px rgba(255, 213, 79, 0.4); }
.stamp-A { color: #7ee2ff; background: rgba(126, 226, 255, 0.1); box-shadow: 0 0 34px rgba(126, 226, 255, 0.3); }
.stamp-B { color: #a5d6a7; background: rgba(165, 214, 167, 0.1); }
.stamp-C { color: #b0bec5; background: rgba(176, 190, 197, 0.1); }

.st-title {
  font-size: 23px;
  color: #fff8e1;
  letter-spacing: 2px;
  margin-bottom: 10px;
}

.st-comment {
  max-width: 480px;
  margin: 0 auto 20px;
  font-size: 13.5px;
  line-height: 1.85;
  color: #b9e6ff;
}

.st-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.st-cell {
  padding: 12px 8px;
  border-radius: 15px;
  background: rgba(1, 30, 50, 0.42);
  border: 1px solid rgba(126, 226, 255, 0.2);
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.st-cell span {
  font-size: 11px;
  color: #8fdcff;
}
.st-cell b {
  font-size: 18px;
}
.st-cell small {
  font-size: 10.5px;
  color: #7fc4e6;
}

.levelup {
  margin-bottom: 16px;
  padding: 10px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: bold;
  color: #4a2c00;
  background: linear-gradient(90deg, #ffe082, #ffcc80, #ffe082);
  background-size: 200% 100%;
  animation: shimmer 2.2s linear infinite;
}

.loading-body {
  padding: 50px 0;
  color: #9fd8f5;
}
.spinner {
  width: 34px;
  height: 34px;
  margin: 0 auto 14px;
  border-radius: 50%;
  border: 3px solid rgba(126, 226, 255, 0.22);
  border-top-color: #ffd54f;
  animation: spin 0.9s linear infinite;
}

/* 浮动数值 */
.floats {
  position: absolute;
  left: 50%;
  bottom: 110px;
  transform: translateX(-50%);
  pointer-events: none;
  display: flex;
  gap: 10px;
}
.float {
  font-size: 14px;
  font-weight: bold;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  animation: floatUp 1.5s ease-out forwards;
}
.float.up { color: #7ee2a8; }
.float.down { color: #ff8a80; }

/* ================= 其他 ================= */
.empty {
  text-align: center;
  padding: 60px 0;
}
.empty-emoji {
  font-size: 46px;
  margin-bottom: 12px;
}
.empty-title {
  font-size: 15px;
  color: #eaf7ff;
  margin-bottom: 6px;
}
.empty-sub {
  font-size: 12.5px;
  color: #8fdcff;
  margin-bottom: 18px;
}
.ghost-btn {
  padding: 10px 24px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 13px;
  color: #eaf7ff;
  background: rgba(126, 226, 255, 0.14);
  border: 2px solid rgba(126, 226, 255, 0.45);
  cursor: pointer;
  transition: all 0.2s;
}
.ghost-btn:hover {
  background: rgba(126, 226, 255, 0.26);
}

.skeleton {
  pointer-events: none;
}
.sk {
  display: block;
  border-radius: 9px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.06));
  background-size: 200% 100%;
  animation: shimmer 1.5s linear infinite;
}
.sk-avatar {
  width: 68px;
  height: 68px;
  border-radius: 22px;
  margin-bottom: 16px;
}
.sk-line {
  height: 12px;
  margin-bottom: 10px;
}
.sk-line.w60 { width: 60%; }
.sk-line.w80 { width: 80%; }

.footer {
  margin-top: 46px;
  text-align: center;
  font-size: 11.5px;
  color: rgba(143, 220, 255, 0.6);
  letter-spacing: 1px;
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 40px;
  transform: translateX(-50%);
  z-index: 200;
  padding: 11px 22px;
  border-radius: 999px;
  font-size: 13px;
  color: #04324d;
  background: linear-gradient(180deg, #ffffff, #d8f4ff);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

/* ================= 动画 ================= */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes pop {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}
@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
@keyframes stampIn {
  from { opacity: 0; transform: rotate(-20deg) scale(1.6); }
  to { opacity: 1; transform: rotate(-6deg) scale(1); }
}
@keyframes floatUp {
  0% { opacity: 0; transform: translateY(10px) scale(0.9); }
  25% { opacity: 1; transform: translateY(-6px) scale(1.1); }
  100% { opacity: 0; transform: translateY(-58px) scale(1); }
}

/* 过渡 */
.layer-enter-active, .layer-leave-active { transition: opacity 0.3s; }
.layer-enter-from, .layer-leave-to { opacity: 0; }
.layer-enter-active .shift-card { transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1); }
.layer-enter-from .shift-card { transform: translateY(24px) scale(0.96); }

.swap-enter-active, .swap-leave-active { transition: opacity 0.22s, transform 0.22s; }
.swap-enter-from { opacity: 0; transform: translateX(18px); }
.swap-leave-to { opacity: 0; transform: translateX(-18px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.toast-enter-active, .toast-leave-active { transition: opacity 0.25s, transform 0.25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 14px); }

/* ================= 响应式 ================= */
@media (max-width: 820px) {
  .roles { grid-template-columns: 1fr; }
  .hero-title { font-size: 30px; }
  .st-grid { grid-template-columns: repeat(2, 1fr); }
  .shift-card { padding: 18px 16px 20px; }
  .s-gain { display: none; }
  .scene-icon { font-size: 44px; }
  .scene-title { font-size: 19px; }
}
</style>

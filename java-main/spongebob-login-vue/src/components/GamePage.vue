<template>
  <div class="game" :class="{ hard: mode === 'hard' }">
    <OceanBackdrop />
    <PageNav active="game" :nickname="displayName" @logout="$emit('logout')" />

    <main class="main">
      <!-- ==================== 开始界面 ==================== -->
      <section v-if="view === 'idle'" class="start-screen">
        <p class="kicker">{{ currentMode.icon }} 蟹堡王后厨 · 限时 {{ currentMode.seconds }} 秒</p>
        <h1 class="title">出餐大作战</h1>
        <p class="sub">按订单顺序堆叠食材，手快有，手慢顾客就走了。</p>

        <!-- 难度切换 -->
        <div class="modes">
          <button
            v-for="m in MODE_LIST"
            :key="m.key"
            type="button"
            class="mode-card"
            :class="{ on: mode === m.key }"
            @click="pickMode(m)"
          >
            <span class="m-icon">{{ m.icon }}</span>
            <span class="m-body">
              <strong>{{ m.name }}</strong>
              <small>{{ m.tip }}</small>
            </span>
            <span v-if="m.key === 'hard'" class="m-badge">×2</span>
          </button>
        </div>

        <div class="rules">
          <div class="rule">
            <span class="rule-icon">👆</span>
            <div>
              <strong>照着订单点</strong>
              <p>订单上的食材有顺序，从下往上，一层都不能错。</p>
            </div>
          </div>
          <div class="rule">
            <span class="rule-icon">🔥</span>
            <div>
              <strong>连击有加成</strong>
              <p>连续点对不断，分数越滚越高；点错扣 {{ currentMode.penalty }} 秒并清零连击。</p>
            </div>
          </div>
          <div class="rule">
            <span class="rule-icon">🔢</span>
            <div>
              <strong>也能用键盘</strong>
              <p>数字键 1-6 对应下方六种食材，出餐越多订单越长（最多 {{ currentMode.maxLayers }} 层）。</p>
            </div>
          </div>
        </div>

        <button type="button" class="cta" @click="startGame">
          🍳 开始营业
        </button>

        <div class="board-grid">
          <div class="panel mine">
            <h3>我的记录 <small>{{ currentMode.name }}</small></h3>
            <div v-if="loadingBoard" class="board-loading">正在翻账本…</div>
            <template v-else>
              <div class="big-score">{{ board.bestScore || 0 }}</div>
              <p class="rank-line">
                <span class="rank-icon">{{ board.rankIcon || '🔧' }}</span>
                {{ board.rank || '铁铲学徒' }}
              </p>
              <div class="mine-stats">
                <div><b>{{ board.burgers || 0 }}</b><span>单局最多出餐</span></div>
                <div><b>{{ board.bestCombo || 0 }}</b><span>最高连击</span></div>
                <div><b>{{ board.plays || 0 }}</b><span>游玩次数</span></div>
                <div><b>{{ board.myRank ? '#' + board.myRank : '—' }}</b><span>全服排名</span></div>
              </div>
            </template>
          </div>

          <div class="panel">
            <h3>{{ currentMode.name }}榜 <small>全服前 10</small></h3>
            <div v-if="loadingBoard" class="board-loading">正在搬榜单…</div>
            <ol v-else-if="board.leaderboard && board.leaderboard.length" class="board">
              <li v-for="row in board.leaderboard" :key="row.pos" :class="{ me: row.mine }">
                <span class="pos" :class="'pos-' + row.pos">{{ row.pos }}</span>
                <span class="who">{{ row.nickname }}</span>
                <span class="rk">{{ row.rankIcon }}</span>
                <b class="sc">{{ row.score }}</b>
              </li>
            </ol>
            <p v-else class="board-empty">还没有人上榜。第一个上的就是你。</p>
          </div>
        </div>
      </section>

      <!-- ==================== 游戏进行中 ==================== -->
      <section v-else-if="view === 'playing'" class="play-screen">
        <!-- 顶栏 -->
        <div class="hud">
          <div class="timer" :class="{ urgent: timeLeft <= 10 }">
            <div class="timer-bar">
              <i :style="{ width: (timeLeft / currentMode.seconds) * 100 + '%' }"></i>
            </div>
            <span class="timer-num">{{ timeLeft.toFixed(1) }}s</span>
          </div>

          <div class="hud-stats">
            <div class="hs">
              <span>得分</span>
              <b>{{ score }}</b>
            </div>
            <div class="hs">
              <span>连击</span>
              <b :class="{ lit: combo >= 3 }">{{ combo > 0 ? '×' + combo : '—' }}</b>
            </div>
            <div class="hs">
              <span>出餐</span>
              <b>{{ burgers }}</b>
            </div>
          </div>

          <button type="button" class="quit" @click="endGame(true)">结束本局</button>
        </div>

        <!-- 顾客 -->
        <div class="counter">
          <div class="customer" :class="{ angry: combo === 0 && lastWrong }">
            <span class="c-emoji">{{ customer.emoji }}</span>
            <span class="c-name">{{ customer.name }}</span>
          </div>

          <div class="order" :class="{ bump: orderBump }">
            <span class="order-label">订单</span>
            <div class="order-steps">
              <span class="o-step bun" :class="{ current: step === -1 }">🍞</span>
              <span
                v-for="(ing, i) in recipe"
                :key="i"
                class="o-step"
                :class="{ current: i === step, done: i < step }"
              >{{ ing.emoji }}</span>
              <span class="o-step bun">🍞</span>
            </div>
          </div>

          <div class="combo-flag" :class="{ show: combo >= 3 }">🔥 ×{{ combo }}</div>
        </div>

        <!-- 汉堡台 -->
        <div class="station">
          <div class="mode-flag">{{ currentMode.icon }} {{ currentMode.name }}</div>

          <div class="steam" aria-hidden="true">
            <span v-for="n in 3" :key="n" :style="{ animationDelay: n * 0.7 + 's' }"></span>
          </div>

          <div class="stack-wrap">
            <transition-group name="drop" tag="div" class="stack" :class="{ flying, shake }">
              <div key="bun-bottom" class="layer bun bun-bottom"></div>

              <div
                v-for="(b, i) in built"
                :key="'f-' + i + '-' + b.code"
                class="layer filling"
                :class="'f-' + b.code"
              >
                <span class="f-emoji">{{ b.emoji }}</span>
              </div>

              <div key="bun-top" class="layer bun bun-top" :class="{ sealed: step >= recipe.length }"></div>
            </transition-group>
          </div>

          <div class="plate"></div>

          <!-- 得分飘字 -->
          <div class="popups" aria-hidden="true">
            <span
              v-for="p in popups"
              :key="p.id"
              class="popup"
              :class="p.cls"
              :style="{ left: p.x + '%', animationDelay: (p.delay || 0) + 'ms' }"
            >{{ p.text }}</span>
          </div>
        </div>

        <!-- 食材栏 -->
        <div class="tray">
          <button
            v-for="(ing, i) in INGREDIENTS"
            :key="ing.code"
            type="button"
            class="ing"
            :class="'i-' + ing.code"
            @click="pick(ing)"
          >
            <span class="ing-key">{{ i + 1 }}</span>
            <span class="ing-emoji">{{ ing.emoji }}</span>
            <span class="ing-name">{{ ing.name }}</span>
          </button>
        </div>

        <p class="hint-line">
          现在要放：<b>{{ step >= recipe.length ? '已封顶，等顾客拿走' : recipe[step]?.name }}</b>
        </p>
      </section>

      <!-- ==================== 结算 ==================== -->
      <section v-else class="over-screen">
        <div class="stamp" :class="'stamp-' + grade">{{ grade }}</div>
        <h1 class="over-title">{{ overTitle }}</h1>
        <p class="over-sub">{{ overSub }}</p>
        <p class="over-mode">{{ currentMode.icon }} {{ currentMode.name }} · 限时 {{ currentMode.seconds }} 秒</p>

        <div class="final-score">
          <b>{{ score }}</b>
          <span>分</span>
        </div>

        <div v-if="newRecord" class="record-badge">🎉 新纪录！之前最好 {{ prevBest }} 分</div>

        <div class="result-grid">
          <div><b>{{ burgers }}</b><span>出餐数</span></div>
          <div><b>{{ maxCombo }}</b><span>最高连击</span></div>
          <div><b>{{ accuracy }}%</b><span>准确率</span></div>
          <div><b>{{ board.rankIcon || '🔧' }}</b><span>{{ board.rank || '铁铲学徒' }}</span></div>
        </div>

        <div class="over-actions">
          <button type="button" class="cta" @click="startGame">🍳 再来一局</button>
          <router-link to="/home" class="ghost">回首页逛逛</router-link>
        </div>

        <div class="panel over-board">
          <h3>{{ currentMode.name }}榜 <small>全服前 10</small></h3>
          <ol v-if="board.leaderboard && board.leaderboard.length" class="board">
            <li v-for="row in board.leaderboard" :key="row.pos" :class="{ me: row.mine }">
              <span class="pos" :class="'pos-' + row.pos">{{ row.pos }}</span>
              <span class="who">{{ row.nickname }}</span>
              <span class="rk">{{ row.rankIcon }}</span>
              <b class="sc">{{ row.score }}</b>
            </li>
          </ol>
          <p v-else class="board-empty">榜单还在等第一个名字。</p>
        </div>
      </section>

      <footer class="footer">比奇堡 · 蟹堡王后厨 · 每一个汉堡都算数 🍔</footer>
    </main>

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
import { getGameOverview, submitScore } from '../api/game.js'
import { getNickname } from '../utils/auth.js'
import { sfx } from '../utils/sound.js'

const props = defineProps({
  nickname: { type: String, default: '' }
})

defineEmits(['logout'])

const router = useRouter()
const displayName = computed(() => props.nickname || getNickname() || '海绵宝宝')

// ==================== 模式 ====================
// 两个难度共用一套玩法，只用参数区分。
// gameCode 直接当排行榜的分区键——后端本来就按 gameCode 存最佳成绩，
// 所以加一个难度不需要改表、不需要改接口。
const MODES = {
  normal: {
    key: 'normal',
    code: 'burger',
    name: '普通营业',
    icon: '🍔',
    seconds: 60,
    minLayers: 2,
    step: 3,
    maxLayers: 5,
    penalty: 2,
    multiplier: 1,
    grades: [900, 600, 350],
    tip: '60 秒，订单从 2 层起，点错扣 2 秒'
  },
  hard: {
    key: 'hard',
    code: 'burger_hard',
    name: '地狱出餐',
    icon: '🔥',
    seconds: 30,
    minLayers: 3,
    step: 2,
    maxLayers: 7,
    penalty: 3,
    multiplier: 2,
    grades: [1400, 900, 500],
    tip: '30 秒，订单 3 层起、最多 7 层，点错扣 3 秒，得分 ×2'
  }
}

const mode = ref('normal')
const currentMode = computed(() => MODES[mode.value])
const MODE_LIST = [MODES.normal, MODES.hard]

function pickMode(m) {
  if (mode.value === m.key) return
  mode.value = m.key
  sfx.select()
  // 每个难度是独立的榜：切过去必须重新拉一遍，
  // 否则「我的记录」和排行榜还停在上一个难度上。
  loadBoard()
}

const INGREDIENTS = [
  { code: 'patty', name: '肉饼', emoji: '🥩' },
  { code: 'lettuce', name: '生菜', emoji: '🥬' },
  { code: 'cheese', name: '芝士', emoji: '🧀' },
  { code: 'tomato', name: '番茄', emoji: '🍅' },
  { code: 'pickle', name: '酸黄瓜', emoji: '🥒' },
  { code: 'onion', name: '洋葱', emoji: '🧅' }
]

const CUSTOMERS = [
  { emoji: '🐟', name: '鲭鱼先生' },
  { emoji: '🦐', name: '虾老板' },
  { emoji: '🐠', name: '热带鱼小姐' },
  { emoji: '🦀', name: '一只蟹' },
  { emoji: '🐙', name: '章鱼哥' },
  { emoji: '⭐', name: '派大星' },
  { emoji: '🐿️', name: '珊迪' },
  { emoji: '🐡', name: '河豚大叔' },
  { emoji: '🦑', name: '鱿鱼阿姨' }
]

// ==================== 状态 ====================
const view = ref('idle')       // idle | playing | over
const loadingBoard = ref(true)
const board = ref({ leaderboard: [] })

const score = ref(0)
const combo = ref(0)
const maxCombo = ref(0)
const burgers = ref(0)
const correct = ref(0)
const wrong = ref(0)
const timeLeft = ref(MODES.normal.seconds)

const recipe = ref([])
const step = ref(-1)
const built = ref([])
const customer = ref(CUSTOMERS[0])
const lastWrong = ref(false)
const orderBump = ref(false)
const flying = ref(false)
const shake = ref(false)

const popups = ref([])
let popSeq = 0

const grade = ref('B')
const overTitle = ref('')
const overSub = ref('')
const newRecord = ref(false)
const prevBest = ref(0)

const toast = ref('')
let toastTimer = null

let timer = null
let lastTickSecond = -1

// ==================== 计算 ====================
const accuracy = computed(() => {
  const total = correct.value + wrong.value
  if (!total) return 100
  return Math.round((correct.value / total) * 100)
})

// ==================== 工具 ====================
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2200)
}

function goLogin() {
  router.replace({ name: 'login' })
}

function rand(n) {
  return Math.floor(Math.random() * n)
}

function pushPopup(text, cls = 'good', x = 50) {
  const id = ++popSeq
  popups.value.push({ id, text, cls, x })
  setTimeout(() => {
    popups.value = popups.value.filter((p) => p.id !== id)
  }, 1000)
}

// ==================== 排行榜 ====================
async function loadBoard() {
  loadingBoard.value = true
  const res = await getGameOverview(currentMode.value.code)
  loadingBoard.value = false
  if (!res.ok) {
    if (res.code === 401) return goLogin()
    return
  }
  board.value = res.data || { leaderboard: [] }
}

// ==================== 订单 ====================
function newOrder() {
  const m = currentMode.value

  // 难度曲线：出餐越多，订单越长。
  // 起点、每几单加一层、层数上限，全部由难度参数决定。
  const base = m.minLayers + Math.floor(burgers.value / m.step)
  const len = Math.min(base, m.maxLayers)

  recipe.value = Array.from({ length: len }, () => INGREDIENTS[rand(INGREDIENTS.length)])
  step.value = 0
  built.value = []
  customer.value = CUSTOMERS[rand(CUSTOMERS.length)]
  lastWrong.value = false

  orderBump.value = true
  setTimeout(() => { orderBump.value = false }, 320)
}

// ==================== 游戏流程 ====================
function startGame() {
  sfx.select()

  view.value = 'playing'
  score.value = 0
  combo.value = 0
  maxCombo.value = 0
  burgers.value = 0
  correct.value = 0
  wrong.value = 0
  timeLeft.value = currentMode.value.seconds
  flying.value = false
  popups.value = []
  lastTickSecond = -1

  newOrder()
  startTimer()
}

function startTimer() {
  clearInterval(timer)
  timer = setInterval(() => {
    timeLeft.value = Math.max(0, timeLeft.value - 0.1)

    // 最后 10 秒每秒滴一次
    const sec = Math.ceil(timeLeft.value)
    if (sec <= 10 && sec !== lastTickSecond && sec > 0) {
      lastTickSecond = sec
      sfx.tick()
    }

    if (timeLeft.value <= 0) {
      endGame(false)
    }
  }, 100)
}

function pick(ing) {
  if (view.value !== 'playing') return

  const want = recipe.value[step.value]

  if (!want) return

  if (ing.code === want.code) {
    // ---------- 正确 ----------
    correct.value++
    combo.value++
    maxCombo.value = Math.max(maxCombo.value, combo.value)

    // 每一层的分值随连击涨，封顶 +10；地狱模式整体翻倍
    const gain = (5 + Math.min(combo.value, 10)) * currentMode.value.multiplier
    score.value += gain

    built.value = [...built.value, ing]
    step.value++

    pushPopup(`+${gain}`, 'good', 42 + rand(18))

    if (combo.value >= 3) sfx.combo(combo.value)
    else sfx.good()

    if (step.value >= recipe.value.length) {
      finishBurger()
    }
  } else {
    // ---------- 错误 ----------
    wrong.value++
    combo.value = 0
    lastWrong.value = true

    const pen = currentMode.value.penalty
    timeLeft.value = Math.max(0, timeLeft.value - pen)

    shake.value = true
    setTimeout(() => { shake.value = false }, 400)

    pushPopup(`-${pen}s`, 'bad', 52)
    sfx.bad()

    if (timeLeft.value <= 0) endGame(false)
  }
}

function finishBurger() {
  const len = recipe.value.length
  // 层数越多越值钱；地狱模式的层数上限更高，所以倍率叠上去非常可观
  const bonus = (20 + len * 8 + Math.min(maxCombo.value, 10) * 4) * currentMode.value.multiplier
  score.value += bonus
  burgers.value++

  pushPopup(`🍔 完成 +${bonus}`, 'great', 50)
  sfx.done()

  // 汉堡飞走：420ms 后立刻换新订单。
  // 这个窗口不能太长——台面空着的时候玩家点了没反应，会以为卡住了。
  flying.value = true
  setTimeout(() => {
    flying.value = false
    built.value = []
    newOrder()
  }, 420)
}

function endGame(quit) {
  if (view.value !== 'playing') return

  clearInterval(timer)
  timer = null
  view.value = 'over'

  const s = score.value
  const [sLine, aLine, bLine] = currentMode.value.grades
  if (s >= sLine) grade.value = 'S'
  else if (s >= aLine) grade.value = 'A'
  else if (s >= bLine) grade.value = 'B'
  else grade.value = 'C'

  // 同一份评级，两种难度读起来不该是同一句话
  const COPY = {
    normal: {
      S: ['蟹堡王的传说', '蟹老板看完成绩单，第一次没有提成本。'],
      A: ['像个老手', '节奏稳，手也稳。后厨今天很安静。'],
      B: ['勉强合格', '有几个汉堡散了架，但顾客没看出来。'],
      C: ['明天再练练', '蟹老板把成绩单折起来，什么也没说。']
    },
    hard: {
      S: ['地狱厨房之主', '三十秒，七层汉堡，一次没错。章鱼哥在门口看得忘了收钱。'],
      A: ['硬仗打完了', '手抖了两下，锅没糊。蟹老板说这算加班。'],
      B: ['活着出来了', '至少没把后厨点着，珊迪说这已经算胜利。'],
      C: ['下次别逞强', '地狱模式不认眼泪，只认手速。']
    }
  }[mode.value]
  const copy = COPY[grade.value]
  overTitle.value = copy[0]
  overSub.value = copy[1]

  if (quit) sfx.over()
  else sfx.win()

  submit()
}

async function submit() {
  if (score.value <= 0) {
    prevBest.value = board.value.bestScore || 0
    return
  }

  const before = board.value.bestScore || 0
  const res = await submitScore({
    gameCode: currentMode.value.code,
    score: score.value,
    combo: maxCombo.value,
    burgers: burgers.value
  })

  if (!res.ok) {
    if (res.code === 401) return goLogin()
    showToast('成绩没能记上，但你自己知道就行 🙂')
    return
  }

  const d = res.data
  newRecord.value = Boolean(d.newRecord) && score.value > before
  prevBest.value = d.prevBest ?? before
  board.value = {
    ...board.value,
    bestScore: d.bestScore,
    bestCombo: d.bestCombo,
    plays: d.plays,
    rank: d.rank,
    rankIcon: d.rankIcon,
    myRank: d.myRank,
    leaderboard: d.leaderboard || board.value.leaderboard
  }
}

// ==================== 键盘 ====================
function onKeydown(e) {
  if (view.value !== 'playing') return
  const n = Number(e.key)
  if (n >= 1 && n <= INGREDIENTS.length) {
    e.preventDefault()
    pick(INGREDIENTS[n - 1])
  }
}

onMounted(() => {
  loadBoard()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(toastTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.game {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #eaf7ff;
}

/* 地狱模式：整体压一层暖红，
   让人一眼就知道这不是「随便玩玩」的那一局 */
.game.hard .counter {
  border-color: rgba(255, 138, 128, 0.42);
  background: rgba(74, 20, 24, 0.34);
}
.game.hard .station {
  background:
    radial-gradient(ellipse at 50% 100%, rgba(255, 112, 67, 0.2), transparent 62%),
    linear-gradient(180deg, rgba(56, 18, 20, 0.52), rgba(28, 8, 12, 0.7));
  border-color: rgba(255, 138, 128, 0.26);
}
.game.hard .mode-flag {
  color: #ffab91;
  border-color: rgba(255, 138, 128, 0.42);
  background: rgba(255, 138, 128, 0.12);
}

.main {
  position: relative;
  z-index: 2;
  flex: 1;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 34px 20px 56px;
}

/* ================= 开始界面 ================= */
.start-screen,
.over-screen {
  text-align: center;
  animation: fadeUp 0.5s ease both;
}

.kicker {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 999px;
  background: rgba(126, 226, 255, 0.14);
  border: 1px solid rgba(126, 226, 255, 0.35);
  color: #b9e6ff;
  font-size: 12.5px;
  margin-bottom: 14px;
}

.title {
  font-size: 42px;
  letter-spacing: 5px;
  color: #fff8e1;
  text-shadow: 0 3px 0 rgba(0, 0, 0, 0.3), 0 0 30px rgba(255, 213, 79, 0.42);
}

.sub {
  margin-top: 8px;
  color: #b9e6ff;
  font-size: 14px;
}

/* ---------- 难度切换 ---------- */
.modes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-width: 660px;
  margin: 24px auto 0;
  text-align: left;
}

.mode-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 16px;
  font-family: inherit;
  text-align: left;
  border-radius: 17px;
  background: rgba(6, 66, 104, 0.34);
  border: 2px solid rgba(160, 232, 255, 0.26);
  cursor: pointer;
  transition: transform 0.16s, border-color 0.2s, background 0.2s, box-shadow 0.2s;
}
.mode-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 213, 79, 0.55);
}
.mode-card:active {
  transform: translateY(1px);
}
.mode-card.on {
  border-color: #ffd54f;
  background: rgba(255, 213, 79, 0.13);
  box-shadow: 0 0 0 1px rgba(255, 213, 79, 0.28), 0 10px 26px rgba(255, 152, 0, 0.2);
}

.m-icon {
  font-size: 22px;
  line-height: 1;
  flex-shrink: 0;
}

.m-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.m-body strong {
  font-size: 13.5px;
  letter-spacing: 1px;
  color: #eaf7ff;
}
.mode-card.on .m-body strong {
  color: #ffd54f;
}
.m-body small {
  font-size: 10.5px;
  line-height: 1.5;
  color: #8fdcff;
}

.m-badge {
  align-self: flex-start;
  margin-left: auto;
  flex-shrink: 0;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: bold;
  color: #4a2c00;
  background: linear-gradient(180deg, #ffe082, #ffb300);
}

.rules {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 26px 0 22px;
  text-align: left;
}

.rule {
  display: flex;
  gap: 11px;
  padding: 15px 16px;
  border-radius: 18px;
  background: rgba(6, 66, 104, 0.34);
  border: 1px solid rgba(160, 232, 255, 0.26);
}
.rule-icon {
  font-size: 20px;
  line-height: 1.2;
}
.rule strong {
  font-size: 13px;
  color: #ffd54f;
  display: block;
  margin-bottom: 4px;
}
.rule p {
  font-size: 11.5px;
  line-height: 1.65;
  color: #a8d9f0;
}

.cta {
  display: inline-block;
  padding: 14px 46px;
  border: none;
  border-radius: 17px;
  font-family: inherit;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 2px;
  color: #4a2c00;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  box-shadow: 0 5px 0 #c17900, 0 12px 30px rgba(255, 152, 0, 0.36);
  cursor: pointer;
  transition: transform 0.14s, box-shadow 0.14s;
}
.cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 7px 0 #c17900, 0 16px 36px rgba(255, 152, 0, 0.46);
}
.cta:active {
  transform: translateY(3px);
  box-shadow: 0 2px 0 #c17900;
}

/* ================= 面板 ================= */
.board-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 14px;
  margin-top: 30px;
  text-align: left;
}

.panel {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(6, 66, 104, 0.32);
  border: 1px solid rgba(160, 232, 255, 0.26);
}

.panel h3 {
  font-size: 13.5px;
  color: #eaf7ff;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.panel h3 small {
  font-size: 11px;
  color: #7fc4e6;
  font-weight: normal;
  margin-left: 6px;
}

.board-loading,
.board-empty {
  font-size: 12px;
  color: #7fc4e6;
  padding: 18px 0;
  text-align: center;
}

.big-score {
  font-size: 38px;
  font-weight: bold;
  color: #ffd54f;
  line-height: 1;
  text-shadow: 0 0 22px rgba(255, 193, 7, 0.45);
}

.rank-line {
  margin-top: 6px;
  font-size: 12.5px;
  color: #b9e6ff;
}
.rank-icon {
  font-size: 15px;
  margin-right: 3px;
}

.mine-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 14px;
}
.mine-stats div {
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(1, 30, 50, 0.36);
}
.mine-stats b {
  font-size: 15px;
  color: #eaf7ff;
}
.mine-stats span {
  font-size: 10.5px;
  color: #7fc4e6;
  margin-top: 1px;
}

/* 排行榜 */
.board {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.board li {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 10px;
  border-radius: 11px;
  font-size: 12.5px;
  background: rgba(1, 30, 50, 0.3);
  transition: background 0.2s;
}
.board li.me {
  background: rgba(255, 213, 79, 0.14);
  border: 1px solid rgba(255, 213, 79, 0.4);
}
.pos {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  font-size: 11px;
  font-weight: bold;
  background: rgba(126, 226, 255, 0.14);
  color: #b9e6ff;
  flex-shrink: 0;
}
.pos-1 { background: linear-gradient(180deg, #ffe082, #ffb300); color: #5d3700; }
.pos-2 { background: linear-gradient(180deg, #e0e0e0, #b0bec5); color: #37474f; }
.pos-3 { background: linear-gradient(180deg, #ffcc80, #d1884a); color: #4e2a00; }

.who {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #dff2ff;
}
.rk {
  font-size: 13px;
}
.sc {
  color: #ffd54f;
  font-size: 13px;
  min-width: 44px;
  text-align: right;
}

/* ================= 游戏界面 ================= */
.play-screen {
  animation: fadeUp 0.4s ease both;
}

.hud {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.timer {
  flex: 1;
  min-width: 0;
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}
.timer-bar {
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background: rgba(1, 30, 50, 0.55);
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.35);
}
.timer-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #4dd0e1, #7ee2ff);
  transition: width 0.1s linear, background 0.3s;
}
.timer.urgent .timer-bar i {
  background: linear-gradient(90deg, #ff7043, #ffca28);
  animation: blink 0.7s ease-in-out infinite;
}
.timer-num {
  font-size: 14px;
  font-weight: bold;
  color: #b9e6ff;
  min-width: 54px;
  text-align: right;
}
.timer.urgent .timer-num {
  color: #ff8a80;
}

.hud-stats {
  display: flex;
  gap: 8px;
}
.hs {
  min-width: 62px;
  padding: 6px 12px;
  border-radius: 13px;
  background: rgba(6, 66, 104, 0.42);
  border: 1px solid rgba(126, 226, 255, 0.28);
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.2;
}
.hs span {
  font-size: 10px;
  color: #8fdcff;
}
.hs b {
  font-size: 17px;
  color: #eaf7ff;
}
.hs b.lit {
  color: #ffd54f;
  text-shadow: 0 0 14px rgba(255, 193, 7, 0.65);
  animation: pulse 1s ease-in-out infinite;
}

.quit {
  padding: 8px 16px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: bold;
  color: #b9e6ff;
  background: transparent;
  border: 2px solid rgba(126, 226, 255, 0.4);
  cursor: pointer;
  transition: all 0.2s;
}
.quit:hover {
  border-color: #ff8a80;
  color: #ffcdd2;
  background: rgba(255, 138, 128, 0.14);
}

/* 顾客与订单 */
.counter {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-radius: 22px;
  background: rgba(6, 66, 104, 0.34);
  border: 1px solid rgba(160, 232, 255, 0.28);
  margin-bottom: 14px;
}

.customer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  width: 72px;
}
.c-emoji {
  font-size: 34px;
  line-height: 1;
  animation: bob 2.6s ease-in-out infinite;
}
.customer.angry .c-emoji {
  animation: shakeX 0.4s ease;
}
.c-name {
  font-size: 10.5px;
  color: #8fdcff;
}

.order {
  flex: 1;
  position: relative;
  padding: 10px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  color: #04324d;
  box-shadow: 0 6px 18px rgba(0, 20, 36, 0.3);
}
.order::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%);
  border: 8px solid transparent;
  border-right-color: rgba(255, 255, 255, 0.94);
  border-left: none;
}
.order.bump {
  animation: bump 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.order-label {
  display: block;
  font-size: 10.5px;
  color: #5b8ba3;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.order-steps {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.o-step {
  font-size: 20px;
  line-height: 1;
  padding: 3px 5px;
  border-radius: 8px;
  transition: all 0.18s;
  opacity: 0.72;
}
/* 已放入的层：只淡一点、轻微去色，仍要一眼看出放了什么 */
.o-step.done {
  opacity: 0.5;
  filter: grayscale(0.3);
  position: relative;
}
.o-step.done::after {
  content: '✓';
  position: absolute;
  right: -1px;
  bottom: -3px;
  font-size: 9px;
  font-weight: bold;
  color: #2e7d32;
  background: #fff;
  border-radius: 50%;
  width: 10px;
  height: 10px;
  line-height: 10px;
  text-align: center;
  box-shadow: 0 0 0 1px rgba(46, 125, 50, 0.35);
}
.o-step.current {
  opacity: 1;
  background: #ffd54f;
  box-shadow: 0 0 0 2px #ffb300, 0 0 14px rgba(255, 193, 7, 0.7);
  transform: scale(1.22);
  animation: pulse 1s ease-in-out infinite;
}
.o-step.bun {
  opacity: 0.5;
  font-size: 15px;
}

.combo-flag {
  flex-shrink: 0;
  width: 74px;
  text-align: center;
  font-size: 14px;
  font-weight: bold;
  color: #ffab91;
  opacity: 0;
  transform: scale(0.6);
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.combo-flag.show {
  opacity: 1;
  transform: scale(1);
  animation: pulse 0.9s ease-in-out infinite;
}

/* 汉堡台 */
.station {
  position: relative;
  height: 330px;
  margin-bottom: 14px;
  border-radius: 24px;
  background:
    radial-gradient(ellipse at 50% 100%, rgba(255, 213, 79, 0.14), transparent 62%),
    linear-gradient(180deg, rgba(2, 40, 66, 0.5), rgba(1, 28, 48, 0.66));
  border: 1px solid rgba(160, 232, 255, 0.2);
  overflow: hidden;
  display: grid;
  place-items: end center;
}

/* 当前难度角标：贴在汉堡台左上角，不抢镜但随时可见 */
.mode-flag {
  position: absolute;
  top: 12px;
  left: 14px;
  z-index: 2;
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 10.5px;
  letter-spacing: 0.5px;
  color: #b9e6ff;
  background: rgba(6, 66, 104, 0.5);
  border: 1px solid rgba(160, 232, 255, 0.3);
  pointer-events: none;
}

.steam {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 26px;
}
.steam span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  animation: steamUp 2.4s ease-in-out infinite;
}

.stack-wrap {
  position: relative;
  padding-bottom: 26px;
  display: flex;
  justify-content: center;
}

.stack {
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
  transition: transform 0.5s cubic-bezier(0.5, -0.4, 0.7, 1), opacity 0.4s;
}
.stack.flying {
  transform: translate(120px, -180px) scale(0.4) rotate(14deg);
  opacity: 0;
}
.stack.shake {
  animation: shakeX 0.4s ease;
}

.layer {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bun {
  width: 186px;
  background: linear-gradient(180deg, #e8b06a, #c98a45);
  box-shadow: inset 0 3px 0 rgba(255, 255, 255, 0.35), 0 3px 6px rgba(0, 0, 0, 0.25);
}
.bun-bottom {
  height: 26px;
  border-radius: 8px 8px 20px 20px;
  margin-bottom: 2px;
}
.bun-top {
  height: 44px;
  border-radius: 50% 50% 10px 10px / 70% 70% 10px 10px;
  margin-top: 2px;
  opacity: 0.32;
  transition: opacity 0.3s;
}
.bun-top.sealed {
  opacity: 1;
}
.bun-top::before {
  content: '';
  position: absolute;
  inset: 8px 22px auto 22px;
  height: 22px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 20% 30%, #fff5d6 1.6px, transparent 1.8px),
    radial-gradient(circle at 42% 62%, #fff5d6 1.6px, transparent 1.8px),
    radial-gradient(circle at 64% 24%, #fff5d6 1.6px, transparent 1.8px),
    radial-gradient(circle at 80% 58%, #fff5d6 1.6px, transparent 1.8px),
    radial-gradient(circle at 30% 74%, #fff5d6 1.6px, transparent 1.8px);
}

.filling {
  height: 20px;
  width: 176px;
  border-radius: 10px;
  box-shadow: inset 0 2px 0 rgba(255, 255, 255, 0.3), 0 2px 4px rgba(0, 0, 0, 0.22);
  margin: 1px 0;
}

.f-patty {
  height: 22px;
  width: 172px;
  background: linear-gradient(180deg, #8d5a33, #5e3a1e);
  border-radius: 11px;
}
.f-lettuce {
  height: 15px;
  width: 184px;
  background: linear-gradient(180deg, #a5d66a, #6f9e3a);
  border-radius: 8px 8px 14px 14px;
}
.f-cheese {
  height: 13px;
  width: 180px;
  background: linear-gradient(180deg, #ffd93d, #f0a500);
  border-radius: 4px;
  clip-path: polygon(0 0, 100% 0, 96% 100%, 78% 60%, 60% 100%, 42% 55%, 24% 100%, 4% 62%);
}
.f-tomato {
  height: 12px;
  width: 170px;
  background: linear-gradient(180deg, #ff6b5b, #c62828);
  border-radius: 6px;
}
.f-pickle {
  height: 10px;
  width: 156px;
  background: linear-gradient(180deg, #c5e17a, #8aa83f);
  border-radius: 5px;
}
.f-onion {
  height: 11px;
  width: 162px;
  background: linear-gradient(180deg, #f5f0f7, #cbb8d8);
  border-radius: 6px;
}

.f-emoji {
  font-size: 11px;
  opacity: 0.55;
  filter: brightness(0.4);
}

.plate {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 250px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(200, 225, 240, 0.7));
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35);
}

/* 得分飘字 */
.popups {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.popup {
  position: absolute;
  bottom: 40%;
  transform: translateX(-50%);
  font-size: 17px;
  font-weight: bold;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.6);
  animation: popUp 1s ease-out forwards;
}
.popup.good { color: #7ee2a8; }
.popup.great { color: #ffd54f; font-size: 19px; }
.popup.bad { color: #ff8a80; }

/* 食材栏 */
.tray {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 9px;
}

.ing {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 12px 6px 10px;
  font-family: inherit;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.04));
  border: 2px solid rgba(160, 232, 255, 0.32);
  cursor: pointer;
  transition: transform 0.12s, border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.ing:hover {
  transform: translateY(-4px);
  border-color: #ffd54f;
  background: rgba(255, 213, 79, 0.14);
  box-shadow: 0 8px 20px rgba(0, 20, 36, 0.35), 0 0 20px rgba(255, 213, 79, 0.2);
}
.ing:active {
  transform: translateY(2px) scale(0.96);
}

.ing-key {
  position: absolute;
  top: 5px;
  right: 7px;
  font-size: 9.5px;
  color: #7fc4e6;
  opacity: 0.8;
}

.ing-emoji {
  font-size: 26px;
  line-height: 1;
}
.ing-name {
  font-size: 11px;
  font-weight: bold;
  color: #dff2ff;
}

.hint-line {
  margin-top: 12px;
  text-align: center;
  font-size: 12.5px;
  color: #9fd8f5;
}
.hint-line b {
  color: #ffd54f;
}

/* ================= 结算 ================= */
.stamp {
  width: 92px;
  height: 92px;
  margin: 4px auto 16px;
  display: grid;
  place-items: center;
  font-size: 44px;
  font-weight: bold;
  border-radius: 28px;
  border: 3px solid currentColor;
  transform: rotate(-6deg);
  animation: stampIn 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.stamp-S { color: #ffd54f; background: rgba(255, 213, 79, 0.1); box-shadow: 0 0 44px rgba(255, 213, 79, 0.42); }
.stamp-A { color: #7ee2ff; background: rgba(126, 226, 255, 0.1); box-shadow: 0 0 34px rgba(126, 226, 255, 0.3); }
.stamp-B { color: #a5d6a7; background: rgba(165, 214, 167, 0.1); }
.stamp-C { color: #b0bec5; background: rgba(176, 190, 197, 0.1); }

.over-title {
  font-size: 26px;
  letter-spacing: 3px;
  color: #fff8e1;
}
.over-sub {
  margin-top: 7px;
  font-size: 13.5px;
  color: #b9e6ff;
}

.over-mode {
  display: inline-block;
  margin-top: 11px;
  padding: 4px 15px;
  border-radius: 999px;
  font-size: 11.5px;
  color: #ffd54f;
  background: rgba(255, 213, 79, 0.12);
  border: 1px solid rgba(255, 213, 79, 0.35);
}

.final-score {
  margin: 16px 0 6px;
}
.final-score b {
  font-size: 62px;
  line-height: 1;
  color: #ffd54f;
  text-shadow: 0 0 34px rgba(255, 193, 7, 0.5);
}
.final-score span {
  font-size: 16px;
  color: #b9e6ff;
  margin-left: 4px;
}

.record-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 7px 18px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: bold;
  color: #4a2c00;
  background: linear-gradient(90deg, #ffe082, #ffcc80, #ffe082);
  background-size: 200% 100%;
  animation: shimmer 2.2s linear infinite;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin: 20px 0;
}
.result-grid div {
  padding: 13px 8px;
  border-radius: 15px;
  background: rgba(1, 30, 50, 0.4);
  border: 1px solid rgba(126, 226, 255, 0.2);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.result-grid b {
  font-size: 19px;
  color: #eaf7ff;
}
.result-grid span {
  font-size: 11px;
  color: #8fdcff;
}

.over-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}
.ghost {
  font-size: 13px;
  color: #b9e6ff;
  text-decoration: none;
  padding: 12px 20px;
  border-radius: 15px;
  border: 2px solid rgba(126, 226, 255, 0.35);
  transition: all 0.2s;
}
.ghost:hover {
  background: rgba(126, 226, 255, 0.14);
  border-color: rgba(126, 226, 255, 0.65);
}

.over-board {
  max-width: 460px;
  margin: 0 auto;
  text-align: left;
}

.footer {
  margin-top: 40px;
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
@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.72; transform: scale(1.06); }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}
@keyframes bump {
  0% { transform: scale(1); }
  45% { transform: scale(1.045); }
  100% { transform: scale(1); }
}
@keyframes shakeX {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(7px); }
  60% { transform: translateX(-5px); }
  80% { transform: translateX(3px); }
}
@keyframes steamUp {
  0% { opacity: 0; transform: translateY(0) scale(0.7); }
  30% { opacity: 0.5; }
  100% { opacity: 0; transform: translateY(-52px) scale(1.4); }
}
@keyframes popUp {
  0% { opacity: 0; transform: translate(-50%, 0) scale(0.8); }
  20% { opacity: 1; transform: translate(-50%, -12px) scale(1.15); }
  100% { opacity: 0; transform: translate(-50%, -78px) scale(1); }
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@keyframes stampIn {
  from { opacity: 0; transform: rotate(-20deg) scale(1.6); }
  to { opacity: 1; transform: rotate(-6deg) scale(1); }
}

/* 食材层落下 */
.drop-enter-active {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s;
}
.drop-enter-from {
  transform: translateY(-46px) scale(1.08);
  opacity: 0;
}

.toast-enter-active, .toast-leave-active { transition: opacity 0.25s, transform 0.25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 14px); }

/* ================= 响应式 ================= */
@media (max-width: 860px) {
  .modes { grid-template-columns: 1fr; }
  .rules { grid-template-columns: 1fr; }
  .board-grid { grid-template-columns: 1fr; }
  .title { font-size: 32px; }
  .hud { flex-wrap: wrap; }
  .timer { width: 100%; flex: 1 1 100%; }
  .hud-stats { margin-left: auto; }
  .station { height: 290px; }
  .tray { grid-template-columns: repeat(3, 1fr); }
  .result-grid { grid-template-columns: repeat(2, 1fr); }
  .counter { flex-wrap: wrap; }
  .combo-flag { width: auto; }
}
</style>

<template>
  <div class="home">
    <!-- 海底场景：光柱、礁石、珊瑚、海草、鱼群、气泡 -->
    <OceanBackdrop />

    <!-- ================= 导航栏 ================= -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand">
          <span class="brand-logo">🍍</span>
          <span class="brand-text">
            <strong>比奇堡</strong>
            <em>BIKINI BOTTOM</em>
          </span>
        </div>

        <nav class="nav-links">
          <button
            v-for="item in navItems"
            :key="item.key"
            type="button"
            :class="['nav-link', { active: item.key === activeNav }]"
            @click="handleNav(item)"
          >{{ item.label }}</button>

          <router-link to="/work" class="nav-link play" title="海绵宝宝与章鱼哥的打工日常">
            🧽 打工
          </router-link>
          <router-link to="/game" class="nav-link play" title="蟹堡王出餐大作战">
            🍔 游戏
          </router-link>
        </nav>

        <div class="nav-right">
          <div class="user-chip" :title="nickname">
            <span class="avatar">🧽</span>
            <span class="user-name">{{ nickname }}</span>
          </div>
          <button class="logout" type="button" @click="$emit('logout')">退出</button>
        </div>
      </div>
      <div class="nav-bubble-line"></div>
    </header>

    <!-- ================= 主内容 ================= -->
    <main class="main">
      <!-- 潜水艇观察窗：欢迎 + 搜索 -->
      <section class="hero">
        <div class="hero-panel">
          <span class="port-bolt bolt-1"></span>
          <span class="port-bolt bolt-2"></span>
          <span class="port-bolt bolt-3"></span>
          <span class="port-bolt bolt-4"></span>

          <p class="hero-kicker">🌊 深度 −18 m · 比奇堡海域</p>
          <h1 class="hero-title">
            欢迎回来，<span class="hl">{{ nickname }}</span>！
          </h1>
          <p class="hero-sub">海底这么大，今天想去哪个角落？搜搜看吧～</p>

          <form class="search" @submit.prevent="submitSearch">
            <span class="search-icon">🔍</span>
            <input
              v-model="keyword"
              type="search"
              class="search-input"
              placeholder="搜索蟹黄堡、水母田、菠萝屋…"
              aria-label="搜索比奇堡"
              @input="onKeywordInput"
            >
            <button v-if="keyword" class="clear" type="button" aria-label="清空" @click="clearSearch">×</button>
            <button class="search-btn" type="submit">搜 索</button>
          </form>

          <div class="tags">
            <span class="tags-label">大家都在搜：</span>
            <button
              v-for="tag in hotKeywords"
              :key="tag"
              type="button"
              class="tag"
              @click="pickTag(tag)"
            >#{{ tag }}</button>
          </div>

          <div class="stats">
            <div v-for="s in stats" :key="s.label" class="stat">
              <span class="stat-icon">{{ s.icon }}</span>
              <span class="stat-label">{{ s.label }}</span>
              <strong class="stat-value">{{ s.value }}</strong>
            </div>
          </div>
        </div>
      </section>

      <!-- 卡片区 -->
      <section ref="listEl" class="section">
        <!-- 分级分类筛选 -->
        <div class="filters">
          <div class="filter-row">
            <span class="filter-label">分类</span>
            <button
              type="button"
              class="chip"
              :class="{ active: !activeTop }"
              @click="selectTop('')"
            >全部 <small>{{ totalAll }}</small></button>
            <button
              v-for="c in categories"
              :key="c.id"
              type="button"
              class="chip"
              :class="{ active: activeTop === c.code }"
              @click="selectTop(c.code)"
            >{{ c.icon }} {{ c.name }} <small>{{ c.placeCount }}</small></button>
          </div>

          <div v-if="currentChildren.length" class="filter-row sub">
            <span class="filter-label">二级</span>
            <button
              type="button"
              class="chip sm"
              :class="{ active: !activeSub }"
              @click="selectSub('')"
            >全部</button>
            <button
              v-for="c in currentChildren"
              :key="c.id"
              type="button"
              class="chip sm"
              :class="{ active: activeSub === c.code }"
              @click="selectSub(c.code)"
            >{{ c.icon }} {{ c.name }} <small>{{ c.placeCount }}</small></button>
          </div>

          <div class="filter-row">
            <span class="filter-label">分级</span>
            <button
              type="button"
              class="chip"
              :class="{ active: !grade }"
              @click="selectGrade('')"
            >不限</button>
            <button
              v-for="g in grades"
              :key="g.value"
              type="button"
              class="chip"
              :class="['g-' + g.value, { active: grade === g.value }]"
              @click="selectGrade(g.value)"
              :title="g.label"
            >{{ g.value }} 级 <small>{{ g.count }}</small></button>

            <span class="filter-divider"></span>

            <span class="filter-label">排序</span>
            <button
              v-for="s in sorts"
              :key="s.value"
              type="button"
              class="chip"
              :class="{ active: sort === s.value }"
              @click="selectSort(s.value)"
            >{{ s.label }}</button>
          </div>
        </div>

        <div class="section-head">
          <h2>
            {{ heading }}
            <small>共 {{ total }} 个</small>
          </h2>
          <button v-if="hasFilter" type="button" class="reset" @click="resetFilters">清空筛选</button>
        </div>

        <!-- 加载骨架 -->
        <div v-if="loading" class="grid">
          <div v-for="n in 4" :key="n" class="card skeleton">
            <span class="sk sk-emoji"></span>
            <span class="sk sk-line w60"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line w80"></span>
          </div>
        </div>

        <!-- 出错 -->
        <div v-else-if="error" class="empty">
          <div class="empty-emoji">🐙</div>
          <p class="empty-title">{{ error }}</p>
          <p class="empty-sub">可能是后端没启动，或者这个接口没连上</p>
          <button type="button" class="reset big" @click="retry">重试一次</button>
        </div>

        <!-- 列表 -->
        <div v-else-if="list.length" class="grid">
          <article
            v-for="(item, i) in list"
            :key="item.id"
            class="card"
            :style="{ animationDelay: Math.min(i, 12) * 0.05 + 's' }"
            @click="openDetail(item)"
          >
            <span class="card-gloss"></span>
            <span class="card-grade" :class="'badge-' + item.grade">{{ item.grade }}</span>
            <div class="card-emoji">{{ item.emoji }}</div>
            <h3 class="card-title">{{ item.name }}</h3>
            <p class="card-desc">{{ item.summary }}</p>
            <div class="card-tags">
              <span v-for="t in item.tags" :key="t" class="pill">{{ t }}</span>
            </div>
            <div class="card-meta">
              <span>{{ item.parentCategoryName }}<template v-if="item.categoryName"> · {{ item.categoryName }}</template></span>
              <span class="card-rating">⭐ {{ item.rating }}</span>
            </div>
          </article>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty">
          <div class="empty-emoji">🐚</div>
          <p class="empty-title">海底没有找到{{ keyword.trim() ? `「${keyword.trim()}」` : '符合条件的结果' }}</p>
          <p class="empty-sub">换个词试试？比如 “蟹黄堡” 或者 “水母”</p>
          <button type="button" class="reset big" @click="resetFilters">回到推荐</button>
        </div>

        <div v-if="!loading && !error && hasNext" class="more-wrap">
          <button type="button" class="load-more" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '正在下潜…' : `继续下潜 · 还有 ${total - list.length} 个` }}
          </button>
        </div>
      </section>
    </main>

    <footer class="footer">
      比奇堡 · 数据来自 Spring Boot + MySQL 🍍
    </footer>

    <!-- 详情弹窗 -->
    <transition name="modal">
      <div v-if="detailOpen" class="modal-mask" @click.self="closeDetail">
        <div class="modal-card">
          <button type="button" class="modal-close" aria-label="关闭" @click="closeDetail">×</button>

          <div v-if="detailLoading" class="modal-loading">正在打捞资料…</div>

          <template v-else-if="detail">
            <div class="modal-head">
              <div class="modal-emoji">{{ detail.emoji }}</div>
              <div>
                <h3 class="modal-title">
                  {{ detail.name }}
                  <span class="card-grade" :class="'badge-' + detail.grade">{{ detail.grade }}</span>
                </h3>
                <p class="modal-path">{{ detail.parentCategoryName }} · {{ detail.categoryName }}</p>
              </div>
            </div>

            <div class="modal-stats">
              <span>评分 <b>{{ detail.rating }}</b></span>
              <span>热度 <b>{{ detail.heat }}</b></span>
              <span>浏览 <b>{{ detail.viewCount }}</b></span>
              <span>{{ detail.gradeLabel }}</span>
            </div>

            <p class="modal-summary">{{ detail.summary }}</p>
            <p class="modal-detail">{{ detail.detail }}</p>

            <div class="modal-tags">
              <span v-for="t in detail.tags" :key="t" class="pill">{{ t }}</span>
            </div>
          </template>

          <div v-else class="modal-loading">这个角落暂时打不开 🐚</div>
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
import { getOverview, getPlace, searchPlaces } from '../api/home.js'

defineProps({
  nickname: { type: String, default: '海绵宝宝' }
})

defineEmits(['logout'])

const router = useRouter()

// ==================== 状态 ====================
const categories = ref([])      // 一级分类（带 children）
const grades = ref([])          // 分级统计 S/A/B/C
const hotKeywords = ref([])     // 热门搜索词
const stats = ref([])           // 统计条

const list = ref([])            // 当前列表
const total = ref(0)
const page = ref(1)
const size = 8
const hasNext = ref(false)

const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')

const keyword = ref('')
const activeTop = ref('')       // 选中的一级分类 code
const activeSub = ref('')       // 选中的二级分类 code
const grade = ref('')           // 选中的分级
const sort = ref('hot')

const sorts = [
  { value: 'hot', label: '热度' },
  { value: 'rating', label: '评分' },
  { value: 'new', label: '最新' }
]

const toast = ref('')
let toastTimer = null
let searchTimer = null

const listEl = ref(null)

// ==================== 计算属性 ====================
const navItems = computed(() => [
  { key: 'all', label: '首页' },
  ...categories.value.map((c) => ({ key: c.code, label: c.name })),
  { key: 'about', label: '关于' }
])

const activeNav = computed(() => activeTop.value || 'all')

const currentChildren = computed(
  () => categories.value.find((c) => c.code === activeTop.value)?.children || []
)

const totalAll = computed(() =>
  categories.value.reduce((sum, c) => sum + (c.placeCount || 0), 0)
)

const heading = computed(() => {
  const kw = keyword.value.trim()
  if (kw) return `“${kw}” 的搜索结果`
  const sub = currentChildren.value.find((c) => c.code === activeSub.value)
  if (sub) return sub.name
  const top = categories.value.find((c) => c.code === activeTop.value)
  if (top) return top.name
  return '为你精选'
})

const hasFilter = computed(
  () => Boolean(keyword.value.trim() || activeTop.value || grade.value || sort.value !== 'hot')
)

// ==================== 数据加载 ====================
function showToast(text) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2200)
}

function goLogin() {
  router.replace({ name: 'login' })
}

async function init() {
  loading.value = true
  error.value = ''

  const r = await getOverview()
  if (!r.ok) {
    loading.value = false
    if (r.code === 401) return goLogin()
    error.value = r.message
    return
  }

  const d = r.data || {}
  categories.value = d.categories || []
  grades.value = d.grades || []
  hotKeywords.value = d.hotKeywords || []
  stats.value = d.stats || []

  await fetchList()
}

/** 拉列表；append=true 表示「加载更多」 */
async function fetchList({ append = false } = {}) {
  const targetPage = append ? page.value + 1 : 1

  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    error.value = ''
  }

  const r = await searchPlaces({
    keyword: keyword.value.trim(),
    categoryCode: activeSub.value || activeTop.value,
    grade: grade.value,
    sort: sort.value,
    page: targetPage,
    size
  })

  if (append) loadingMore.value = false
  else loading.value = false

  if (!r.ok) {
    if (r.code === 401) return goLogin()
    error.value = r.message
    if (!append) {
      list.value = []
      total.value = 0
      hasNext.value = false
    }
    return
  }

  const d = r.data || {}
  page.value = d.page || targetPage
  total.value = d.total || 0
  hasNext.value = Boolean(d.hasNext)
  list.value = append ? [...list.value, ...(d.list || [])] : (d.list || [])
}

function retry() {
  init()
}

// ==================== 交互 ====================
function onKeywordInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchList(), 320)
}

function submitSearch() {
  if (!keyword.value.trim()) {
    showToast('先输入点什么再搜嘛 🐚')
    return
  }
  clearTimeout(searchTimer)
  fetchList()
  scrollToList()
}

function pickTag(tag) {
  clearTimeout(searchTimer)
  keyword.value = tag
  activeTop.value = ''
  activeSub.value = ''
  grade.value = ''
  fetchList()
  scrollToList()
}

function clearSearch() {
  clearTimeout(searchTimer)
  keyword.value = ''
  fetchList()
}

function selectTop(code) {
  activeTop.value = code
  activeSub.value = ''
  fetchList()
}

function selectSub(code) {
  activeSub.value = code
  fetchList()
}

function selectGrade(value) {
  grade.value = grade.value === value ? '' : value
  fetchList()
}

function selectSort(value) {
  sort.value = value
  fetchList()
}

function resetFilters() {
  clearTimeout(searchTimer)
  keyword.value = ''
  activeTop.value = ''
  activeSub.value = ''
  grade.value = ''
  sort.value = 'hot'
  fetchList()
}

function handleNav(item) {
  if (item.key === 'about') {
    showToast('这个页面还在施工中，先把海底逛完吧 🔨')
    return
  }
  if (item.key === 'all') {
    resetFilters()
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  selectTop(item.key)
  scrollToList()
}

function loadMore() {
  if (loadingMore.value || !hasNext.value) return
  fetchList({ append: true })
}

function scrollToList() {
  listEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ==================== 详情弹窗 ====================
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function openDetail(item) {
  detail.value = null
  detailOpen.value = true
  detailLoading.value = true

  const r = await getPlace(item.id)
  detailLoading.value = false

  if (!r.ok) {
    if (r.code === 401) {
      detailOpen.value = false
      return goLogin()
    }
    showToast(r.message)
    detailOpen.value = false
    return
  }
  detail.value = r.data
}

function closeDetail() {
  detailOpen.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape' && detailOpen.value) closeDetail()
}

onMounted(() => {
  init()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearTimeout(searchTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ================= 布局 ================= */
.home {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #eaf7ff;
}

/* ================= 导航栏：深水玻璃舱 ================= */
.nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: linear-gradient(180deg, rgba(3, 52, 84, 0.86), rgba(3, 45, 74, 0.74));
  backdrop-filter: blur(14px) saturate(140%);
  box-shadow: 0 6px 24px rgba(1, 26, 44, 0.35);
}
.nav-bubble-line {
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(126, 226, 255, 0.75), rgba(255, 213, 79, 0.85), rgba(126, 226, 255, 0.75), transparent);
  box-shadow: 0 0 12px rgba(126, 226, 255, 0.55);
}

.nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-logo {
  width: 42px; height: 42px;
  display: grid; place-items: center;
  font-size: 22px;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  border: 3px solid #ffd54f;
  border-radius: 14px;
  box-shadow: 0 3px 0 #c17900, 0 0 18px rgba(255, 193, 7, 0.45);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand-text strong {
  color: #fff8e1;
  font-size: 18px;
  letter-spacing: 3px;
  text-shadow: 0 2px 0 rgba(0, 0, 0, 0.25), 0 0 16px rgba(255, 213, 79, 0.45);
}
.brand-text em {
  font-style: normal;
  font-size: 10px;
  letter-spacing: 1.5px;
  color: #8fdcff;
}

.nav-links {
  display: flex;
  gap: 4px;
  margin-left: 10px;
}

.nav-link {
  position: relative;
  border: none;
  background: transparent;
  padding: 8px 16px;
  border-radius: 12px;
  font-family: inherit;
  font-size: 15px;
  font-weight: bold;
  color: #b9e6ff;
  cursor: pointer;
  transition: background .2s, color .2s, transform .15s;
}
.nav-link:hover {
  background: rgba(126, 226, 255, 0.16);
  color: #ffffff;
  transform: translateY(-1px);
}
.nav-link.active {
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  color: #5d3700;
  box-shadow: 0 3px 0 #c17900, 0 0 16px rgba(255, 193, 7, 0.45);
}

/* 首页右上角两个玩法入口：用金色描边和其他分类区分开 */
.nav-link.play {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  text-decoration: none;
  color: #ffe082;
  border: 2px solid rgba(255, 213, 79, 0.5);
  background: rgba(255, 213, 79, 0.1);
  padding: 6px 13px;
  margin-left: 4px;
}
.nav-link.play:hover {
  background: rgba(255, 213, 79, 0.24);
  color: #fff8e1;
  border-color: #ffd54f;
  box-shadow: 0 0 18px rgba(255, 193, 7, 0.32);
  transform: translateY(-1px);
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 14px 5px 6px;
  background: rgba(126, 226, 255, 0.12);
  border: 2px solid rgba(126, 226, 255, 0.45);
  border-radius: 999px;
  max-width: 190px;
}
.avatar {
  width: 28px; height: 28px;
  display: grid; place-items: center;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  flex-shrink: 0;
}
.user-name {
  font-size: 14px;
  font-weight: bold;
  color: #eaf7ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  border: 2px solid rgba(126, 226, 255, 0.45);
  background: transparent;
  color: #b9e6ff;
  padding: 6px 14px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: all .2s;
}
.logout:hover {
  background: rgba(255, 205, 210, 0.18);
  border-color: #ff8a80;
  color: #ffcdd2;
}

/* ================= 主区 ================= */
.main {
  position: relative;
  z-index: 2;
  flex: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 42px 20px 60px;
}

/* ================= 潜水艇观察窗 ================= */
.hero {
  animation: fadeUp .6s ease both;
}

.hero-panel {
  position: relative;
  padding: 34px 32px 26px;
  border-radius: 34px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.03) 44%, rgba(2, 44, 72, 0.16) 100%),
    rgba(6, 66, 104, 0.34);
  border: 2px solid rgba(160, 232, 255, 0.45);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    inset 0 0 70px rgba(126, 226, 255, 0.16),
    inset 0 -34px 60px rgba(1, 30, 50, 0.26),
    0 18px 48px rgba(1, 30, 50, 0.4);
  backdrop-filter: blur(10px) saturate(130%);
  text-align: center;
  overflow: hidden;
}
/* 玻璃高光弧：柔化成一片，不要硬边 */
.hero-panel::before {
  content: '';
  position: absolute;
  top: -70%; left: -6%;
  width: 52%; height: 150%;
  background: linear-gradient(100deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.04) 55%, transparent 78%);
  transform: rotate(12deg);
  filter: blur(18px);
  pointer-events: none;
}
/* 水面反光的一条细弧 */
.hero-panel::after {
  content: '';
  position: absolute;
  top: 10px; left: 12%; right: 12%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  pointer-events: none;
}

/* 舱窗铆钉 */
.port-bolt {
  position: absolute;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #eafcff, #7fc7e0);
  box-shadow: 0 0 8px rgba(160, 232, 255, 0.8);
  opacity: .8;
}
.bolt-1 { top: 14px; left: 16px; }
.bolt-2 { top: 14px; right: 16px; }
.bolt-3 { bottom: 14px; left: 16px; }
.bolt-4 { bottom: 14px; right: 16px; }

.hero-kicker {
  display: inline-block;
  font-size: 12px;
  letter-spacing: 2px;
  color: #bdf0ff;
  padding: 4px 14px;
  border-radius: 999px;
  background: rgba(3, 40, 66, 0.4);
  border: 1px solid rgba(126, 226, 255, 0.4);
  margin-bottom: 14px;
}

.hero-title {
  font-size: 34px;
  letter-spacing: 2px;
  color: #ffffff;
  text-shadow: 0 3px 0 rgba(2, 80, 122, 0.9), 0 8px 24px rgba(1, 30, 50, 0.55);
  margin-bottom: 10px;
}
.hero-title .hl {
  color: #ffe082;
  text-shadow: 0 3px 0 #b26a00, 0 8px 24px rgba(1, 30, 50, 0.5);
}

.hero-sub {
  color: #d6f1ff;
  font-size: 15px;
  margin-bottom: 24px;
  text-shadow: 0 1px 6px rgba(1, 30, 50, 0.5);
}

/* 搜索框：暖色胶囊，和海水形成对比 */
.search {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 640px;
  margin: 0 auto;
  padding: 8px 8px 8px 18px;
  background: rgba(255, 253, 240, 0.97);
  border: 4px solid #f9a825;
  border-radius: 999px;
  box-shadow: 0 7px 0 rgba(150, 92, 0, 0.55), 0 18px 36px rgba(1, 26, 44, 0.45);
  transition: transform .2s, box-shadow .2s;
}
.search:focus-within {
  transform: translateY(-2px);
  box-shadow: 0 9px 0 rgba(150, 92, 0, 0.6), 0 24px 44px rgba(1, 26, 44, 0.5), 0 0 0 6px rgba(126, 226, 255, 0.22);
}

.search-icon {
  font-size: 18px;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 16px;
  color: #4e342e;
  padding: 10px 0;
}
.search-input::placeholder { color: #a1887f; }
.search-input::-webkit-search-cancel-button { display: none; }

.clear {
  border: none;
  background: #ffe0b2;
  color: #8d6e63;
  width: 26px; height: 26px;
  border-radius: 50%;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
}
.clear:hover { background: #ffcc80; color: #5d3700; }

.search-btn {
  flex-shrink: 0;
  border: none;
  padding: 11px 26px;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  color: #5d3700;
  font-family: inherit;
  font-size: 15px;
  font-weight: bold;
  letter-spacing: 3px;
  cursor: pointer;
  box-shadow: 0 4px 0 #c17900;
  transition: transform .1s, box-shadow .1s, filter .2s;
}
.search-btn:hover { filter: brightness(1.05); }
.search-btn:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #c17900;
}

/* 热门标签 */
.tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
}
.tags-label {
  font-size: 13px;
  color: #cdeeff;
}
.tag {
  border: 2px solid rgba(190, 240, 255, 0.65);
  background: rgba(126, 226, 255, 0.16);
  color: #eafcff;
  padding: 5px 13px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
  backdrop-filter: blur(4px);
}
.tag:hover {
  background: #fffde7;
  color: #f57f17;
  border-color: #fffde7;
  transform: translateY(-2px);
}

/* 小数据条 */
.stats {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed rgba(160, 232, 255, 0.32);
}
.stat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(2, 40, 64, 0.35);
  border: 1px solid rgba(126, 226, 255, 0.28);
  font-size: 12px;
  color: #cdeeff;
}
.stat-value {
  color: #ffe082;
  font-size: 13px;
  letter-spacing: .5px;
}

/* ================= 筛选面板 ================= */
.filters {
  margin-top: 30px;
  padding: 14px 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(5, 66, 104, 0.55), rgba(4, 52, 84, 0.5));
  border: 2px solid rgba(160, 232, 255, 0.3);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22), 0 10px 28px rgba(1, 30, 50, 0.3);
  backdrop-filter: blur(10px) saturate(130%);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.filter-row.sub {
  margin-left: 4px;
  padding-left: 10px;
  border-left: 2px dashed rgba(160, 232, 255, 0.28);
}

.filter-label {
  font-size: 12px;
  letter-spacing: 1px;
  color: #9fd8f5;
  min-width: 34px;
}

.filter-divider {
  width: 1px;
  height: 18px;
  background: rgba(160, 232, 255, 0.28);
  margin: 0 4px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 2px solid rgba(160, 232, 255, 0.32);
  background: rgba(126, 226, 255, 0.1);
  color: #d9f2ff;
  font-family: inherit;
  font-size: 13px;
  padding: 5px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: background .18s, transform .18s, color .18s, border-color .18s;
}
.chip small { font-size: 11px; opacity: .7; }
.chip:hover {
  background: rgba(126, 226, 255, 0.24);
  transform: translateY(-1px);
  color: #ffffff;
}
.chip.active {
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  border-color: #ffd54f;
  color: #5d3700;
  font-weight: bold;
  box-shadow: 0 3px 0 #c17900;
}
.chip.active small { opacity: .85; }

.chip.sm { padding: 4px 10px; font-size: 12px; }

/* 分级选中态：S 金 / A 青 / B 灰蓝 / C 灰 */
.chip.g-S.active {
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  border-color: #ffd54f;
  color: #5d3700;
  box-shadow: 0 3px 0 #c17900;
}
.chip.g-A.active {
  background: linear-gradient(180deg, #9fe5ff, #4fc3f7);
  border-color: #9fe5ff;
  color: #04334d;
  box-shadow: 0 3px 0 #0277bd;
}
.chip.g-B.active {
  background: linear-gradient(180deg, #dbeaf3, #a9c6d7);
  border-color: #dbeaf3;
  color: #24414f;
  box-shadow: 0 3px 0 #6d8fa3;
}
.chip.g-C.active {
  background: linear-gradient(180deg, #ececec, #c2c2c2);
  border-color: #ececec;
  color: #454545;
  box-shadow: 0 3px 0 #7a7a7a;
}

/* ================= 卡片区 ================= */
.section {
  margin-top: 30px;
  scroll-margin-top: 96px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 18px;
}
.section-head h2 {
  font-size: 20px;
  color: #ffffff;
  letter-spacing: 1px;
  text-shadow: 0 2px 0 rgba(2, 80, 122, 0.9), 0 6px 18px rgba(1, 30, 50, 0.5);
}
.section-head small {
  font-size: 12px;
  font-weight: normal;
  color: #cdeeff;
  opacity: .9;
  margin-left: 6px;
}
.reset {
  margin-left: auto;
  border: 2px solid rgba(190, 240, 255, 0.6);
  background: rgba(126, 226, 255, 0.16);
  color: #eafcff;
  padding: 6px 14px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}
.reset:hover { background: #fffde7; color: #f57f17; border-color: #fffde7; }
.reset.big {
  margin: 18px auto 0;
  border-color: #f9a825;
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  color: #5d3700;
  font-weight: bold;
  padding: 11px 26px;
  font-size: 14px;
  box-shadow: 0 4px 0 #c17900;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(228px, 1fr));
  gap: 18px;
}

/* 气泡卡片 */
.card {
  position: relative;
  padding: 22px 20px 18px;
  border-radius: 24px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.97) 0%, rgba(230, 247, 255, 0.94) 55%, rgba(206, 237, 252, 0.94) 100%);
  border: 3px solid rgba(255, 255, 255, 0.92);
  box-shadow:
    inset 0 2px 0 rgba(255, 255, 255, 0.95),
    0 7px 0 rgba(6, 80, 118, 0.55),
    0 18px 34px rgba(1, 30, 50, 0.35);
  cursor: pointer;
  overflow: hidden;
  animation: cardIn .5s cubic-bezier(.2, 1.3, .4, 1) both;
  transition: transform .18s, box-shadow .18s, border-color .18s;
}
.card:hover {
  transform: translateY(-7px);
  border-color: #9fe5ff;
  box-shadow:
    inset 0 2px 0 rgba(255, 255, 255, 1),
    0 11px 0 rgba(6, 80, 118, 0.6),
    0 26px 44px rgba(1, 30, 50, 0.42),
    0 0 26px rgba(126, 226, 255, 0.45);
}
.card:active { transform: translateY(-2px); }

/* 右上角高光，做出气泡/贝壳的弧度 */
.card-gloss {
  position: absolute;
  top: -46px; right: -34px;
  width: 116px; height: 116px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 40%, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0) 68%);
  pointer-events: none;
}

/* 分级角标 */
.card-grade {
  position: absolute;
  top: 14px; right: 14px;
  z-index: 2;
  font-size: 11px;
  font-weight: bold;
  letter-spacing: 1px;
  padding: 3px 9px;
  border-radius: 999px;
}
.badge-S { background: linear-gradient(180deg, #ffd54f, #ffb300); color: #5d3700; box-shadow: 0 2px 0 #c17900; }
.badge-A { background: linear-gradient(180deg, #9fe5ff, #4fc3f7); color: #04334d; box-shadow: 0 2px 0 #0277bd; }
.badge-B { background: linear-gradient(180deg, #dbeaf3, #a9c6d7); color: #24414f; }
.badge-C { background: linear-gradient(180deg, #ececec, #c2c2c2); color: #454545; }

.card-emoji {
  position: relative;
  font-size: 32px;
  width: 58px; height: 58px;
  display: grid; place-items: center;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 26%, #ffffff 0%, #ffe9a8 45%, #ffc93c 100%);
  border: 3px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 0 rgba(193, 121, 0, 0.45), inset 0 -4px 10px rgba(255, 173, 0, 0.35);
  margin-bottom: 14px;
}

.card-title {
  position: relative;
  font-size: 17px;
  color: #0b6ba3;
  letter-spacing: 1px;
  margin-bottom: 8px;
  text-shadow: 0 1px 0 #ffffff;
}

.card-desc {
  position: relative;
  font-size: 13px;
  line-height: 1.7;
  color: #4a6d80;
  min-height: 44px;
}

.card-tags {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}
.pill {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 213, 79, 0.28);
  color: #9a6a00;
  border: 2px solid rgba(255, 193, 7, 0.5);
}

.card-meta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed rgba(11, 107, 163, 0.2);
  font-size: 11px;
  color: #5d7f92;
}
.card-rating {
  margin-left: auto;
  color: #c17900;
  font-weight: bold;
}

/* 骨架屏 */
.skeleton {
  pointer-events: none;
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: none;
  animation: none;
}
.sk {
  display: block;
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0.18));
  background-size: 200% 100%;
  animation: shimmer 1.3s linear infinite;
}
.sk-emoji { width: 58px; height: 58px; border-radius: 50%; margin-bottom: 16px; }
.sk-line { height: 12px; margin-bottom: 10px; }
.sk-line.w60 { width: 60%; }
.sk-line.w80 { width: 80%; }

/* 加载更多 */
.more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.load-more {
  border: 2px solid rgba(190, 240, 255, 0.6);
  background: rgba(126, 226, 255, 0.14);
  color: #eafcff;
  font-family: inherit;
  font-size: 13px;
  padding: 10px 26px;
  border-radius: 999px;
  cursor: pointer;
  transition: all .2s;
}
.load-more:hover:not(:disabled) {
  background: #fffde7;
  color: #f57f17;
  border-color: #fffde7;
}
.load-more:disabled { opacity: .6; cursor: default; }

/* 空状态 */
.empty {
  text-align: center;
  padding: 54px 20px;
  border-radius: 26px;
  background: rgba(4, 58, 92, 0.45);
  border: 3px dashed rgba(160, 232, 255, 0.5);
  backdrop-filter: blur(8px);
}
.empty-emoji { font-size: 52px; margin-bottom: 10px; }
.empty-title {
  font-size: 17px;
  font-weight: bold;
  color: #ffe082;
  margin-bottom: 6px;
}
.empty-sub { font-size: 13px; color: #cdeeff; }

/* ================= 详情弹窗 ================= */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(1, 20, 34, 0.62);
  backdrop-filter: blur(5px);
}
.modal-card {
  position: relative;
  width: min(540px, 100%);
  max-height: 82vh;
  overflow: auto;
  padding: 28px 26px 24px;
  border-radius: 26px;
  background: linear-gradient(160deg, #fffdf5 0%, #eaf8ff 60%, #d9f1ff 100%);
  border: 3px solid #f9a825;
  box-shadow: 0 10px 0 rgba(150, 92, 0, 0.5), 0 30px 60px rgba(1, 20, 34, 0.5);
  color: #20404f;
}
.modal-close {
  position: absolute;
  top: 12px; right: 12px;
  width: 30px; height: 30px;
  border: none;
  border-radius: 50%;
  background: #ffe0b2;
  color: #8d6e63;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
.modal-close:hover { background: #ffcc80; color: #5d3700; }

.modal-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-right: 30px;
}
.modal-emoji {
  width: 64px; height: 64px;
  flex-shrink: 0;
  display: grid; place-items: center;
  font-size: 34px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 26%, #ffffff 0%, #ffe9a8 45%, #ffc93c 100%);
  border: 3px solid #ffffff;
  box-shadow: 0 4px 0 rgba(193, 121, 0, 0.45);
}
.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 21px;
  color: #0b6ba3;
  letter-spacing: 1px;
}
.modal-title .card-grade {
  position: static;
  font-size: 11px;
}
.modal-path {
  font-size: 12px;
  color: #6b8b9c;
  margin-top: 6px;
}

.modal-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 16px 0 12px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(126, 226, 255, 0.14);
  font-size: 13px;
  color: #3c6376;
}
.modal-stats b { color: #c17900; }

.modal-summary {
  font-size: 14px;
  line-height: 1.8;
  color: #37596b;
  margin-bottom: 12px;
  font-weight: bold;
}
.modal-detail {
  font-size: 13px;
  line-height: 1.95;
  color: #4a6d80;
  padding: 12px 14px;
  border-left: 4px solid #7ed6ff;
  border-radius: 0 12px 12px 0;
  background: rgba(126, 226, 255, 0.1);
}
.modal-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
}
.modal-loading {
  padding: 40px 0;
  text-align: center;
  color: #6b8b9c;
  font-size: 14px;
}

.modal-enter-active, .modal-leave-active { transition: opacity .22s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .modal-card { transition: transform .24s cubic-bezier(.2, 1.3, .4, 1); }
.modal-enter-from .modal-card { transform: translateY(18px) scale(.96); }

/* ================= 页脚 ================= */
.footer {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 18px 20px 26px;
  font-size: 12px;
  color: #cdeeff;
  opacity: .8;
  text-shadow: 0 1px 6px rgba(1, 30, 50, 0.6);
}

/* ================= 轻提示 ================= */
.toast {
  position: fixed;
  left: 50%;
  bottom: 40px;
  transform: translateX(-50%);
  z-index: 99;
  padding: 12px 22px;
  background: rgba(255, 253, 240, 0.98);
  border: 3px solid #f9a825;
  border-radius: 999px;
  color: #5d3700;
  font-size: 14px;
  font-weight: bold;
  box-shadow: 0 8px 24px rgba(1, 26, 44, 0.4);
  max-width: 88vw;
  text-align: center;
}
.toast-enter-active, .toast-leave-active { transition: opacity .25s, transform .25s; }
.toast-enter-from, .toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 12px);
}

/* ================= 动画 ================= */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(18px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(.96); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ================= 响应式 ================= */
@media (max-width: 860px) {
  .nav-links { display: none; }
  .hero-title { font-size: 26px; }
  .hero-panel { padding: 26px 18px 20px; border-radius: 26px; }
  .main { padding: 30px 14px 50px; }
  .user-name { display: none; }
  .user-chip { padding: 5px; }
  .search { border-width: 3px; padding-left: 14px; }
  .search-btn { padding: 10px 18px; letter-spacing: 1px; }
  .filters { padding: 12px 12px; }
  .filter-label { min-width: 30px; }
  .chip { font-size: 12px; padding: 4px 10px; }
}

@media (max-width: 520px) {
  .grid { grid-template-columns: 1fr; }
  .section-head { flex-wrap: wrap; }
  .hero-sub { font-size: 13px; }
  .stats { gap: 6px; }
  .stat { padding: 5px 10px; font-size: 11px; }
  .filter-row.sub { margin-left: 0; padding-left: 8px; }
  .filter-divider { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .hero, .card { animation: none; }
  .sk { animation: none; }
}
</style>

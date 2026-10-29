/**
 * 极简音效：用 Web Audio 现场合成，不加载任何音频文件。
 *
 * 好处是零资源依赖、体积极小；代价是音色比较「电子」——
 * 但这正好适合这种卡通风格的小游戏。
 *
 * 浏览器要求先有用户交互才能出声，所以所有函数内部都做了懒初始化。
 */

let ctx = null
let muted = false

function ensureCtx() {
  if (typeof window === 'undefined') return null
  if (!ctx) {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return null
    ctx = new Ctx()
  }
  if (ctx.state === 'suspended') ctx.resume()
  return ctx
}

/**
 * 播放一个音符
 * @param {number} freq 频率
 * @param {number} dur 时长（秒）
 * @param {OscillatorType} type 波形
 * @param {number} vol 音量 0-1
 * @param {number} delay 延迟（秒）
 */
function tone(freq, dur = 0.12, type = 'sine', vol = 0.06, delay = 0) {
  if (muted) return
  const ac = ensureCtx()
  if (!ac) return

  const t0 = ac.currentTime + delay
  const osc = ac.createOscillator()
  const gain = ac.createGain()

  osc.type = type
  osc.frequency.setValueAtTime(freq, t0)
  // 快速起音 + 指数衰减，听起来像「叮」而不是「呜」
  gain.gain.setValueAtTime(0.0001, t0)
  gain.gain.exponentialRampToValueAtTime(vol, t0 + 0.012)
  gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur)

  osc.connect(gain)
  gain.connect(ac.destination)
  osc.start(t0)
  osc.stop(t0 + dur + 0.02)
}

/** 频率滑音，用于「失败」「下坠」这类效果 */
function slide(from, to, dur = 0.22, type = 'sawtooth', vol = 0.05) {
  if (muted) return
  const ac = ensureCtx()
  if (!ac) return

  const t0 = ac.currentTime
  const osc = ac.createOscillator()
  const gain = ac.createGain()

  osc.type = type
  osc.frequency.setValueAtTime(from, t0)
  osc.frequency.exponentialRampToValueAtTime(Math.max(40, to), t0 + dur)

  gain.gain.setValueAtTime(vol, t0)
  gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur)

  osc.connect(gain)
  gain.connect(ac.destination)
  osc.start(t0)
  osc.stop(t0 + dur + 0.02)
}

export const sfx = {
  /** 普通点击 */
  click() {
    tone(520, 0.06, 'triangle', 0.05)
  },
  /** 选项确认 */
  select() {
    tone(660, 0.09, 'triangle', 0.06)
    tone(880, 0.1, 'triangle', 0.04, 0.05)
  },
  /** 得到东西（金币、加分） */
  coin() {
    tone(1046, 0.07, 'square', 0.035)
    tone(1568, 0.1, 'square', 0.03, 0.06)
  },
  /** 正确 */
  good() {
    tone(784, 0.08, 'sine', 0.06)
    tone(1046, 0.1, 'sine', 0.05, 0.06)
  },
  /** 错误 */
  bad() {
    slide(220, 90, 0.2, 'sawtooth', 0.045)
  },
  /** 完成一个订单 */
  done() {
    ;[523, 659, 784, 1046].forEach((f, i) => tone(f, 0.12, 'triangle', 0.05, i * 0.055))
  },
  /** 连击提升 */
  combo(level = 3) {
    const base = 600 + Math.min(level, 12) * 60
    tone(base, 0.07, 'square', 0.035)
    tone(base * 1.5, 0.09, 'square', 0.03, 0.05)
  },
  /** 结算：成功 */
  win() {
    ;[523, 659, 784, 1046, 1318].forEach((f, i) => tone(f, 0.16, 'triangle', 0.05, i * 0.09))
  },
  /** 结算：收工 */
  over() {
    ;[440, 392, 330].forEach((f, i) => tone(f, 0.2, 'sine', 0.05, i * 0.13))
  },
  /** 倒计时最后几秒 */
  tick() {
    tone(880, 0.04, 'square', 0.03)
  },
  /** 是否静音 */
  isMuted() {
    return muted
  },
  toggle() {
    muted = !muted
    if (!muted) tone(660, 0.08, 'sine', 0.05)
    return muted
  }
}

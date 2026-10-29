/** 小游戏接口 */
import { request } from './client.js'

/** 我的最佳 + 排行榜 */
export function getGameOverview(gameCode = 'burger') {
  return request('/game/overview', { params: { gameCode } })
}

/** 提交一局成绩 */
export function submitScore({ score, combo, burgers, gameCode = 'burger' }) {
  return request('/game/score', { method: 'POST', body: { gameCode, score, combo, burgers } })
}

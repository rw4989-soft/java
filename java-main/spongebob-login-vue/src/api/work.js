/** 打工日常接口 */
import { request } from './client.js'

/** 打工大厅：角色档案 + 今日状态 + 最近打卡 */
export function getWorkOverview() {
  return request('/work/overview')
}

/** 抽一次班次（5 幕剧情） */
export function drawShift(roleCode) {
  return request('/work/shift/draw', { params: { roleCode } })
}

/**
 * 结算班次
 * @param {string} roleCode
 * @param {{sceneId:string, choiceIndex:number}[]} picks
 */
export function settleShift(roleCode, picks) {
  return request('/work/shift/settle', { method: 'POST', body: { roleCode, picks } })
}

/** 打卡时间轴 */
export function getShifts(roleCode, limit = 10) {
  return request('/work/shifts', { params: { roleCode, limit } })
}

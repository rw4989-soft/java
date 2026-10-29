/**
 * 首页接口封装（对接 Spring Boot /api/home/**）。
 *
 * 这组接口需要登录：每次请求自动带上 Authorization: Bearer <token>。
 * token 失效时后端返回 401，这里顺手清掉本地登录态，由调用方跳回登录页。
 */
import { authHeader, clearSession } from '../utils/auth.js'

const BASE = '/api'

/** 过滤掉空值，避免拼出 ?keyword=&grade= 这种脏参数 */
function buildQuery(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      search.append(key, value)
    }
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

async function request(path, params) {
  try {
    const headers = {}
    const auth = authHeader()
    if (auth) headers.Authorization = auth

    const res = await fetch(BASE + path + buildQuery(params), { headers })
    const json = await res.json().catch(() => ({}))

    if (res.status === 401 || json.code === 401) {
      clearSession()
      return { ok: false, code: 401, message: json.message || '登录状态已失效，请重新登录' }
    }
    if (json.code === 200) {
      return { ok: true, code: 200, message: json.message, data: json.data }
    }
    return { ok: false, code: json.code, message: json.message || '请求失败，蟹堡王后厨开小差了' }
  } catch (e) {
    return { ok: false, code: -1, message: '网络异常，检查一下后端是不是没启动' }
  }
}

/** 首页聚合：分类树 + 分级统计 + 热门词 + 推荐 + 统计条 */
export function getOverview() {
  return request('/home/overview')
}

/** 只取分类树 */
export function getCategories() {
  return request('/home/categories')
}

/**
 * 分级分类搜索
 * @param {{keyword?:string, categoryCode?:string, categoryId?:number, grade?:string, sort?:string, page?:number, size?:number}} params
 */
export function searchPlaces(params) {
  return request('/home/places', params)
}

/** 条目详情（后端会把浏览次数 +1） */
export function getPlace(id) {
  return request(`/home/places/${id}`)
}

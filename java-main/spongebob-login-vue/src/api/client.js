/**
 * 统一的接口请求封装（带 token、统一处理 401）。
 *
 * home.js 里那份是早期写的，行为一致；新模块统一用这里，
 * 以后要加请求头、加超时、加重试，只改这一个文件。
 */
import { authHeader, clearSession } from '../utils/auth.js'

const BASE = '/api'

/** 过滤空值，避免拼出 ?keyword=&grade= 这种脏参数 */
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

/**
 * @param {string} path 形如 '/work/overview'
 * @param {{method?:string, params?:object, body?:object}} options
 * @returns {Promise<{ok:boolean, code:number, message:string, data:any}>}
 */
export async function request(path, { method = 'GET', params, body } = {}) {
  const headers = {}
  const auth = authHeader()
  if (auth) headers.Authorization = auth
  if (body) headers['Content-Type'] = 'application/json'

  try {
    const res = await fetch(BASE + path + buildQuery(params), {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined
    })
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

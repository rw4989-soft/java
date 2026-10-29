// 与 Spring Boot 后端对接的认证接口封装
// 后端基础路径 /api（开发态由 Vite 代理转发到 localhost:8081，
// 生产态把构建产物放到 Spring Boot 的 static 下即为同源，同样走 /api）
const BASE = '/api'

async function postJson(path, body) {
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const json = await res.json().catch(() => ({}))
  // 后端统一返回 Result: { code, message, data }
  if (json.code === 200) {
    return { ok: true, message: json.message, data: json.data }
  }
  return { ok: false, message: json.message || '请求失败，蟹堡王后厨开小差了' }
}

// 登录：POST /api/auth/login  { username, password }
// 成功时 data = { token, tokenType, expiresIn, username, nickname }
export function login(username, password) {
  return postJson('/auth/login', { username, password })
}

// 注册：POST /api/auth/register  { username, password, nickname }
export function register(username, password, nickname) {
  return postJson('/auth/register', { username, password, nickname })
}

// 校验 token：GET /api/auth/me（可选，刷新页面时想确认 token 还有效就用它）
export async function me(token) {
  try {
    const res = await fetch(BASE + '/auth/me', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const json = await res.json().catch(() => ({}))
    return { ok: json.code === 200, message: json.message, data: json.data }
  } catch (e) {
    return { ok: false, message: '网络异常' }
  }
}

// 注销：POST /api/auth/logout（让后端把 token 作废）
export function logout(token) {
  return fetch(BASE + '/auth/logout', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
}

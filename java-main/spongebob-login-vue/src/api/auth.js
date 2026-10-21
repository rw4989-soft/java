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
export function login(username, password) {
  return postJson('/auth/login', { username, password })
}

// 注册：POST /api/auth/register  { username, password, nickname }
export function register(username, password, nickname) {
  return postJson('/auth/register', { username, password, nickname })
}

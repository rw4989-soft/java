/**
 * 登录态的本地存储封装。
 *
 * 只在这里读写 localStorage，其他地方一律调这几个函数，
 * 以后要从 localStorage 换成 sessionStorage / cookie 时只改这一个文件。
 */

const TOKEN_KEY = 'sb_token'
const NICKNAME_KEY = 'sb_nickname'
const USERNAME_KEY = 'sb_username'

/** 读取 token，没有则返回空字符串 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 是否已登录（只判断本地有没有 token，真正是否有效由后端说了算） */
export function isLoggedIn() {
  return Boolean(getToken())
}

/** 登录成功后写入整份会话 */
export function setSession({ token, nickname, username }) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  if (nickname) localStorage.setItem(NICKNAME_KEY, nickname)
  if (username) localStorage.setItem(USERNAME_KEY, username)
}

/** 昵称，首页展示用 */
export function getNickname() {
  return localStorage.getItem(NICKNAME_KEY) || ''
}

/** 用户名（记住我时才有） */
export function getUsername() {
  return localStorage.getItem(USERNAME_KEY) || ''
}

/** 清空登录态 */
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(NICKNAME_KEY)
  // 注意：sb_username 是「记住我」用的，退出登录时保留，方便下次自动填充
}

/** 请求头里带 token 用，例：{ Authorization: authHeader() } */
export function authHeader() {
  const token = getToken()
  return token ? `Bearer ${token}` : ''
}

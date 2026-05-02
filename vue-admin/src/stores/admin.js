import { reactive } from 'vue'
import { login as apiLogin, logoutApi, setToken, clearToken, fetchCurrentUser } from '../api/index.js'

export const adminState = reactive({
  // 认证
  currentUser: null,   // { username, role, displayName }
  token: '',

  // 业务数据
  dashboard: null,
  rooms: [],
  orders: [],
  notices: [],
  loading: false,

  // Toast
  toastMessage: '',
  toastVisible: false,
  toastTimer: null
})

export function showToast(message) {
  adminState.toastMessage = message
  adminState.toastVisible = true
  clearTimeout(adminState.toastTimer)
  adminState.toastTimer = setTimeout(() => {
    adminState.toastVisible = false
  }, 2200)
}

// 从 localStorage 恢复登录态
export function restoreSession() {
  try {
    const stored = localStorage.getItem('hotel_token')
    if (stored) {
      adminState.token = stored
      setToken(stored)
      return true
    }
  } catch { /* 静默 */ }
  return false
}

export async function handleLogin(username, password) {
  const result = await apiLogin(username, password)
  adminState.token = result.token
  adminState.currentUser = {
    username: result.username,
    role: result.role,
    displayName: result.displayName
  }
  setToken(result.token)
  return result
}

export async function handleLogout(router) {
  try { await logoutApi() } catch { /* 忽略 */ }
  adminState.token = ''
  adminState.currentUser = null
  adminState.dashboard = null
  adminState.rooms = []
  adminState.orders = []
  adminState.notices = []
  adminState.loading = false
  clearToken()
  if (router) router.push('/login')
}

export async function refreshUser() {
  try {
    const user = await fetchCurrentUser()
    adminState.currentUser = user
  } catch {
    // 未登录或过期，不做跳转（由路由守卫处理）
  }
}

export function isAdmin() {
  return adminState.currentUser?.role === 'ADMIN'
}


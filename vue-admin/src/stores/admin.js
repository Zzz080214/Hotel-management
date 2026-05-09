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
  users: [],
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
  adminState.users = []
  adminState.loading = false
  clearToken()
  if (router) router.push('/login')
}

export async function refreshUser() {
  try {
    const user = await fetchCurrentUser()
    adminState.currentUser = user
    return user
  } catch (error) {
    adminState.token = ''
    adminState.currentUser = null
    clearToken()
    throw error
  }
}

export function isAdmin() {
  return adminState.currentUser?.role === 'ADMIN'
}

export function isManager() {
  return adminState.currentUser?.role === 'MANAGER'
}

export function isBackOfficeRole(role = adminState.currentUser?.role) {
  return role === 'ADMIN' || role === 'MANAGER'
}

export function roleHome(role = adminState.currentUser?.role) {
  if (role === 'ADMIN') return '/users'
  if (role === 'MANAGER') return '/dashboard'
  return '/staff/room-board'
}

export function roleLabel(role = adminState.currentUser?.role) {
  if (role === 'ADMIN') return '系统管理员'
  if (role === 'MANAGER') return '经理'
  if (role === 'STAFF') return '前台员工'
  return '未识别角色'
}

export function roleDefaultName(role = adminState.currentUser?.role) {
  if (role === 'ADMIN') return '系统管理员'
  if (role === 'MANAGER') return '赵经理'
  return '赵前台'
}

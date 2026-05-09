const API_BASE_URL = '/api'

function getToken() {
  try {
    const stored = localStorage.getItem('hotel_token')
    return stored || ''
  } catch {
    return ''
  }
}

export function setToken(token) {
  try {
    localStorage.setItem('hotel_token', token)
  } catch { /* 无痕模式等场景静默失败 */ }
}

export function clearToken() {
  try {
    localStorage.removeItem('hotel_token')
  } catch { /* 同上 */ }
}

async function apiRequest(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers,
    ...options
  })

  // 先读文本，避免空响应导致 JSON parse 崩溃
  const text = await response.text()
  if (!text) {
    throw new Error('后端服务未响应，请确认 Spring Boot 已启动（端口 8080）')
  }

  let body
  try {
    body = JSON.parse(text)
  } catch {
    throw new Error('后端返回格式异常，请检查后端日志')
  }

  if (body.code === 401) {
    clearToken()
    throw new Error(body.message || '登录已过期，请重新登录')
  }
  if (!response.ok || (body.code !== undefined && body.code !== 200 && body.code !== 0)) {
    throw new Error(body.message || '接口请求失败')
  }
  return body.data === undefined ? body : body.data
}

async function downloadRequest(path) {
  const token = getToken()
  const headers = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const response = await fetch(`${API_BASE_URL}${path}`, { headers })
  if (!response.ok) {
    if (response.status === 401) {
      clearToken()
    }
    throw new Error('文件下载失败')
  }
  return response.blob()
}

// 工作台
export function fetchDashboard() {
  return apiRequest('/admin/dashboard')
}

export function exportOperationsReport() {
  return downloadRequest('/admin/dashboard/operations/export')
}

// 房型
export function fetchRoomTypes() {
  return apiRequest('/admin/room-types')
}

// 订单
export function fetchOrders(keyword, status) {
  const params = new URLSearchParams()
  if (keyword) params.append('keyword', keyword)
  if (status && status !== 'all') params.append('status', status)
  const qs = params.toString()
  return apiRequest(`/admin/orders${qs ? '?' + qs : ''}`)
}

export function createOrder(data) {
  return apiRequest('/admin/orders', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function checkInOrder(id, data) {
  return apiRequest(`/admin/orders/${encodeURIComponent(id)}/check-in`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function confirmOrderPayment(id) {
  return apiRequest(`/admin/orders/${encodeURIComponent(id)}/confirm-payment`, {
    method: 'POST'
  })
}

export function cancelOrder(id) {
  return apiRequest(`/admin/orders/${encodeURIComponent(id)}/cancel`, {
    method: 'POST'
  })
}

export function checkOutOrder(id) {
  return apiRequest(`/admin/orders/${encodeURIComponent(id)}/check-out`, {
    method: 'POST'
  })
}

// 公告
export function fetchNotices() {
  return apiRequest('/admin/notices')
}

export function publishNotice(data) {
  return apiRequest('/admin/notices', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 系统用户
export function fetchUsers() {
  return apiRequest('/admin/users')
}

export function createUser(data) {
  return apiRequest('/admin/users', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function updateUser(id, data) {
  return apiRequest(`/admin/users/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 认证
export function login(username, password) {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
}

export function logoutApi() {
  return apiRequest('/auth/logout', { method: 'POST' }).catch(() => {})
}

export function fetchCurrentUser() {
  return apiRequest('/auth/me')
}

// 工具函数
export function money(value) {
  return `¥${Number(value || 0).toFixed(0)}`
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

export const orderStatusMeta = {
  upcoming: { text: '待入住', className: 'status pending' },
  staying: { text: '在住', className: 'status active' },
  finished: { text: '已完成', className: 'status finished' },
  cancelled: { text: '已取消', className: 'status cancelled' }
}

export function normalizeRoomStatus(status) {
  if (status === 'budget') return 'low'
  return status || 'steady'
}

export function roomTagClass(status) {
  const s = normalizeRoomStatus(status)
  if (s === 'hot' || s === 'luxury') return 'success'
  if (s === 'low') return 'warning'
  return 'brand'
}

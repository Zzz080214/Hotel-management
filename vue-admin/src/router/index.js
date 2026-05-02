import { createRouter, createWebHashHistory } from 'vue-router'
import { adminState, restoreSession, refreshUser, showToast } from '../stores/admin.js'

async function loadData() {
  const api = await import('../api/index.js')
  try {
    const [dashboard, rooms, orders, notices] = await Promise.all([
      api.fetchDashboard().catch(() => null),
      api.fetchRoomTypes().catch(() => []),
      api.fetchOrders().catch(() => []),
      api.fetchNotices().catch(() => [])
    ])
    adminState.dashboard = dashboard
    adminState.rooms = rooms
    adminState.orders = orders
    adminState.notices = notices
  } catch {
    // 后端未启动或网络异常时静默失败，页面展示空状态
  }
}

// ══════════════════════════════════════════════
// 管理员路由（经营管理）
// ══════════════════════════════════════════════
const adminRoutes = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '经营概览', subtitle: '今日营收、入住率、订单趋势与运营指标' }
  },
  {
    path: '/room-types',
    name: 'RoomTypes',
    component: () => import('../views/RoomTypes.vue'),
    meta: { title: '房型管理', subtitle: '房型配置、价格调整、库存管理与房态监控' }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue'),
    meta: { title: '订单管理', subtitle: '全部订单查询、筛选、取消与支付状态追踪' }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('../views/admin/Reports.vue'),
    meta: { title: '营业报表', subtitle: '营收统计、入住率分析、房型收入与经营指标' }
  },
  {
    path: '/notices',
    name: 'Notices',
    component: () => import('../views/Notices.vue'),
    meta: { title: '系统公告', subtitle: '发布与管理酒店公告、活动通知' }
  }
]

// ══════════════════════════════════════════════
// 前台员工路由（前台操作）
// ══════════════════════════════════════════════
const staffRoutes = [
  {
    path: '/staff/room-board',
    name: 'RoomBoard',
    component: () => import('../views/staff/RoomBoard.vue'),
    meta: { title: '房态看板', subtitle: '实时房间状态：空净房、已入住、待清扫、维修中' }
  },
  {
    path: '/staff/checkin',
    name: 'StaffCheckin',
    component: () => import('../views/staff/StaffCheckin.vue'),
    meta: { title: '入住登记', subtitle: '填写住客信息、分配房间、办理入住' }
  },
  {
    path: '/staff/checkout',
    name: 'StaffCheckout',
    component: () => import('../views/staff/StaffCheckout.vue'),
    meta: { title: '退房结算', subtitle: '搜索在住客人、核对账单、办理退房' }
  }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { noAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

function applyRoutes(role) {
  const existingNames = router.getRoutes().map(r => r.name)
  const targetRoutes = role === 'ADMIN' ? adminRoutes : staffRoutes
  for (const r of targetRoutes) {
    if (!existingNames.includes(r.name)) {
      router.addRoute(r)
    }
  }
  // 首页重定向
  const homeRoute = router.getRoutes().find(r => r.path === '/')
  if (homeRoute) router.removeRoute(homeRoute.name || '/')
  router.addRoute({
    path: '/',
    redirect: role === 'ADMIN' ? '/dashboard' : '/staff/room-board'
  })
}

router.beforeEach(async (to, from, next) => {
  // 登录页无需认证
  if (to.meta.noAuth) { next(); return }

  // 未登录 → 恢复会话或跳回登录
  if (!adminState.currentUser) {
    const restored = restoreSession()
    if (!restored) { next('/login'); return }
    try { await refreshUser() } catch { next('/login'); return }
  }

  // 确保当前角色路由已注册（换角色登录时需重新注册）
  // 用角色独有路由名判断，而非 count，防止前一角色的路由残留导致跳过
  const role = adminState.currentUser.role
  const needRoute = role === 'ADMIN' ? 'Dashboard' : 'RoomBoard'
  const hasRoleRoute = router.getRoutes().some(r => r.name === needRoute)
  if (!hasRoleRoute) {
    applyRoutes(role)
    // Vue Router 4：addRoute 后 current to 的 matched 为空，需强制重匹配
    return next({ path: to.path, replace: true })
  }

  // ---- 角色隔离守卫 ----
  const adminPaths = ['/dashboard', '/room-types', '/orders', '/reports', '/notices']
  const staffPaths = ['/staff/room-board', '/staff/checkin', '/staff/checkout']

  if (adminState.currentUser.role === 'STAFF' && adminPaths.includes(to.path)) {
    showToast('当前账号为前台员工，无权访问管理功能')
    return next('/staff/room-board')
  }
  if (adminState.currentUser.role === 'ADMIN' && staffPaths.includes(to.path)) {
    return next('/dashboard')
  }

  // ---- 未匹配路由兜底 ----
  const homePath = adminState.currentUser.role === 'ADMIN' ? '/dashboard' : '/staff/room-board'
  if (to.matched.length === 0) {
    return next(homePath)
  }

  // ---- 首次数据加载 ----
  if (!adminState.dashboard && !adminState.rooms.length) {
    adminState.loading = true
    await loadData()
    adminState.loading = false
  }
  next()
})

export default router

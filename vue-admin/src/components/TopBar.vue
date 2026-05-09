<template>
  <header class="topbar">
    <div class="page-intro">
      <h2>{{ route.meta.title || '工作台' }}</h2>
      <p>{{ route.meta.subtitle || '查看酒店今日运营概览、订单趋势和入住情况。' }}</p>
    </div>

    <div class="topbar-tools">
      <input
        class="search-box"
        type="text"
        placeholder="搜索订单号 / 用户 / 房型"
        v-model="searchKeyword"
        @keydown.enter="doSearch"
      />
      <div class="topbar-card">
        <strong>{{ displayName }}</strong>
        <div class="subtle">{{ roleText }}</div>
      </div>
      <div class="topbar-card">
        <strong>{{ clock }}</strong>
        <div class="subtle">系统时间</div>
      </div>
      <button class="button primary" @click="syncData">同步今日数据</button>
      <button class="button" @click="doLogout">退出登录</button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  adminState, showToast, handleLogout, isBackOfficeRole,
  roleDefaultName, roleHome, roleLabel
} from '../stores/admin.js'
import {
  fetchDashboard, fetchRoomTypes, fetchOrders, fetchNotices
} from '../api/index.js'

const route = useRoute()
const router = useRouter()
const searchKeyword = ref('')
const clock = ref('')
let clockTimer = null

const displayName = computed(() => adminState.currentUser?.displayName || roleDefaultName())
const roleText = computed(() => roleLabel())

function updateClock() {
  const now = new Date()
  clock.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function doSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    router.push(roleHome())
    return
  }
  // 管理员/经理跳订单管理，员工跳退房搜索
  if (isBackOfficeRole()) {
    router.push('/orders')
  } else {
    router.push('/staff/checkout')
  }
  showToast(isBackOfficeRole() ? '已跳转到订单管理' : '已跳转到退房结算页搜索')
}

const loadAdminData = async (showMessage = false) => {
  try {
    const [dashboard, rooms, orders, notices] = await Promise.all([
      fetchDashboard(),
      fetchRoomTypes(),
      fetchOrders(),
      fetchNotices()
    ])
    adminState.dashboard = dashboard
    adminState.rooms = rooms
    adminState.orders = orders
    adminState.notices = notices
    if (showMessage) showToast('后端数据已同步')
  } catch (error) {
    showToast(`后端连接失败：${error.message}`)
  }
}

async function syncData() {
  await loadAdminData(true)
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 30000)
  // 首次加载已在路由守卫中完成，此处不再重复加载
  // 用户可手动点击"同步今日数据"按钮刷新
  if (!adminState.dashboard && !adminState.rooms.length) {
    loadAdminData(true)
  }
})

async function doLogout() {
  await handleLogout(router)
}

onUnmounted(() => {
  clearInterval(clockTimer)
})
</script>

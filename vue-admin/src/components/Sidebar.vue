<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-badge">YQ</div>
      <h1>Yueqi Reserve</h1>
      <p>悦栖酒店 · {{ isAdmin() ? '管理后台' : '前台系统' }}</p>
    </div>

    <!-- 角色标签 -->
    <div class="role-badge" :class="isAdmin() ? 'admin' : 'staff'">
      {{ isAdmin() ? '管理员' : '前台员工' }}
    </div>

    <nav class="menu">
      <div v-if="isAdmin()" class="menu-section-label">经营管理</div>
      <button
        v-for="item in menuItems"
        :key="item.path"
        class="menu-item"
        :class="{ active: isActive(item.path) }"
        @click="navigate(item.path)"
      >
        <span class="menu-icon">{{ item.icon }}</span>
        <span class="menu-label">
          <strong>{{ item.label }}</strong>
          <small>{{ item.subtitle }}</small>
        </span>
      </button>
    </nav>

    <div class="user-card">
      <strong>{{ displayName }}</strong>
      <p>{{ roleDesc }}</p>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { adminState, isAdmin } from '../stores/admin.js'

const router = useRouter()
const route = useRoute()

const adminMenuItems = [
  { path: '/dashboard',  label: '经营概览', subtitle: '营收 · 入住率 · 运营指标', icon: '📊' },
  { path: '/room-types', label: '房型管理', subtitle: '价格配置 · 库存管理 · 房态', icon: '🏨' },
  { path: '/orders',     label: '订单管理', subtitle: '查询 · 取消 · 支付状态', icon: '📋' },
  { path: '/reports',    label: '营业报表', subtitle: '收入统计 · 入住率分析', icon: '📈' },
  { path: '/notices',    label: '系统公告', subtitle: '发布与管理酒店通知', icon: '📢' }
]

const staffMenuItems = [
  { path: '/staff/room-board', label: '房态看板', subtitle: '房间状态 · 一键入住/退房', icon: '🏠' },
  { path: '/staff/checkin',    label: '入住登记', subtitle: '客人信息 · 房间分配', icon: '✅' },
  { path: '/staff/checkout',   label: '退房结算', subtitle: '账单核对 · 办理退房', icon: '💰' }
]

const menuItems = computed(() => isAdmin() ? adminMenuItems : staffMenuItems)

const displayName = computed(() => adminState.currentUser?.displayName || (isAdmin() ? '赵经理' : '赵前台'))

const roleDesc = computed(() => {
  if (isAdmin()) return '负责酒店经营分析、房型配置、订单审核与报表管理。'
  return '负责前台日常操作：房态巡查、入住登记与退房结算。'
})

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function navigate(path) {
  router.push(path)
}
</script>

<style scoped>
.role-badge {
  margin: 0 16px 4px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  letter-spacing: 1px;
}
.role-badge.admin { background: var(--brand-soft); color: var(--brand); }
.role-badge.staff  { background: var(--success-soft); color: var(--success); }

.menu-section-label {
  margin: 8px 20px 2px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: var(--muted);
}

.menu-icon {
  font-size: 18px;
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}
</style>

<template>
  <div>
    <!-- 房型总览 -->
    <section class="panel">
      <div class="section-head">
        <div>
          <h3>房型总览</h3>
          <p>支持演示房型信息、库存余量和不同房型的经营状态</p>
        </div>
        <div class="action-row">
          <button class="button primary" @click="toast('已打开新增房型流程示意，后续可接弹窗表单。')">新增房型</button>
          <button class="button" @click="toast('房型表导出成功示意，可接 Excel 导出接口。')">导出房型表</button>
        </div>
      </div>

      <div class="summary-grid">
        <article class="summary-card">
          <div class="summary-head">
            <h3>{{ enabledRooms.length }}</h3>
            <span class="tag brand">房型数</span>
          </div>
          <p class="helper">当前启用 {{ enabledRooms.length }} 种房型，可在后台统一维护价格和库存。</p>
        </article>
        <article class="summary-card">
          <div class="summary-head">
            <h3>{{ totalAvailable }}</h3>
            <span class="tag success">可售房</span>
          </div>
          <p class="helper">当前可售房间共 {{ totalAvailable }} 间。</p>
        </article>
        <article class="summary-card">
          <div class="summary-head">
            <h3>{{ totalOccupied }}</h3>
            <span class="tag warning">已占用</span>
          </div>
          <p class="helper">当前已预订或在住房间共 {{ totalOccupied }} 间。</p>
        </article>
        <article class="summary-card">
          <div class="summary-head">
            <h3>{{ disabledRooms.length }}</h3>
            <span class="tag danger">停售中</span>
          </div>
          <p class="helper">后端已支持房型下架，删除操作会改为停售。</p>
        </article>
      </div>
    </section>

    <!-- 筛选 -->
    <section class="filter-panel">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="chip-row">
            <button
              v-for="f in filters"
              :key="f.key"
              class="chip"
              :class="{ active: activeFilter === f.key }"
              @click="activeFilter = f.key"
            >{{ f.text }}</button>
          </div>
        </div>
        <div class="toolbar-right">
          <span class="helper">当前显示 <strong>{{ visibleCount }}</strong> 个房型卡片</span>
        </div>
      </div>
    </section>

    <!-- 房型卡片网格 -->
    <section class="room-grid">
      <article
        v-for="room in filteredRooms"
        :key="room.id"
        class="room-card"
      >
        <div class="inline-head">
          <strong>{{ room.name }}</strong>
          <span :class="'tag ' + roomTagClass(room.status)">{{ room.tag || room.status || '启用' }}</span>
        </div>
        <p>{{ room.summary || '暂无房型说明' }}</p>
        <div class="progress"><span :style="{ width: Math.min(roomRate(room), 100) + '%' }"></span></div>
        <div class="room-meta">
          <span>入住率 {{ roomRate(room) }}%</span>
          <span>标准价 {{ money(room.price) }}</span>
        </div>
        <div class="room-meta">
          <span>总房间 {{ room.totalRooms || 0 }}</span>
          <span>可售 {{ room.availableRooms || 0 }}</span>
          <span>{{ room.bed || '-' }}</span>
        </div>
      </article>
      <div v-if="filteredRooms.length === 0" class="room-card" style="text-align: center;">
        <p>暂无匹配的房型数据。</p>
      </div>
    </section>

    <!-- 房型配置表 + 房间状态板 -->
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>房型配置表</h3>
            <p>适合后续直接替换为房型管理列表页</p>
          </div>
        </div>
        <table>
          <thead>
            <tr>
              <th>房型名称</th>
              <th>床型</th>
              <th>价格</th>
              <th>早餐</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in enabledRooms" :key="room.id">
              <td>{{ room.name }}</td>
              <td>{{ room.bed || '-' }}</td>
              <td>{{ money(room.price) }}</td>
              <td>{{ room.breakfast || '无' }}</td>
              <td>
                <span :class="'status ' + (room.enabled !== false ? 'active' : 'cancelled')">
                  {{ room.enabled !== false ? '启用中' : '已停售' }}
                </span>
              </td>
            </tr>
            <tr v-if="enabledRooms.length === 0">
              <td colspan="5" style="text-align: center; color: var(--muted)">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>房间状态板</h3>
            <p>8楼房间实时状态：可入住、待清扫、检修中</p>
          </div>
        </div>
        <div class="room-list">
          <div class="task-item">
            <div class="inline-head">
              <strong>{{ roomStatus.available }} · 豪华大床房</strong>
              <span class="tag success">可入住</span>
            </div>
            <p>已完成清扫与检查，可直接分配给今日待入住订单。</p>
          </div>
          <div class="task-item">
            <div class="inline-head">
              <strong>{{ roomStatus.cleaning }} · 标准双床房</strong>
              <span class="tag warning">待清扫</span>
            </div>
            <p>上一位住客已退房 20 分钟，预计 30 分钟后可售。</p>
          </div>
          <div class="task-item">
            <div class="inline-head">
              <strong>{{ roomStatus.maintenance }} · 行政大床房</strong>
              <span class="tag danger">检修中</span>
            </div>
            <p>空调检修未完成，后台已自动暂停该房间在线售卖。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { adminState, showToast } from '../stores/admin.js'
import { money, roomTagClass, normalizeRoomStatus } from '../api/index.js'

const activeFilter = ref('all')
const filters = [
  { key: 'all', text: '全部房型' },
  { key: 'hot', text: '热门高入住' },
  { key: 'steady', text: '稳定售卖' },
  { key: 'low', text: '待优化' }
]

const enabledRooms = computed(() => adminState.rooms.filter(r => r.enabled !== false))
const disabledRooms = computed(() => adminState.rooms.filter(r => r.enabled === false))

const totalAvailable = computed(() =>
  enabledRooms.value.reduce((s, r) => s + Number(r.availableRooms || 0), 0)
)

const totalRooms = computed(() =>
  enabledRooms.value.reduce((s, r) => s + Number(r.totalRooms || 0), 0)
)

const totalOccupied = computed(() => Math.max(totalRooms.value - totalAvailable.value, 0))

const filteredRooms = computed(() => {
  if (activeFilter.value === 'all') return adminState.rooms
  return adminState.rooms.filter(r => normalizeRoomStatus(r.status) === activeFilter.value)
})

const visibleCount = computed(() => filteredRooms.value.length)

function roomRate(room) {
  const total = Number(room.totalRooms || 0)
  if (total <= 0) return 0
  const available = Number(room.availableRooms || 0)
  return Math.round(((total - available) / total) * 1000) / 10
}

// ─── 房间号体系（与 CheckIn 一致）─────────────────────
const ROOM_PREFIX = { '豪华大床房': 8100, '标准双床房': 8200, '亲子家庭房': 8600, '行政大床房': 8800 }

function roomNum(name, offset) {
  const room = adminState.rooms.find(r => r.name === name)
  if (!room) return '----'
  const total = Number(room.totalRooms) || 1
  const avail = Number(room.availableRooms) || 1
  const p = ROOM_PREFIX[name] || 8000
  return String(p + Math.max(1, total - avail) + offset)
}

const roomStatus = computed(() => ({
  available:   roomNum('豪华大床房', 0),
  cleaning:    roomNum('标准双床房', 0),
  maintenance: roomNum('行政大床房', -1)
}))

function toast(msg) {
  showToast(msg)
}
</script>

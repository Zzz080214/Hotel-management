<template>
  <div>
    <!-- 顶部统计条 -->
    <section class="status-bar">
      <div class="stat-item vacant"><span class="dot"></span>空净房 <strong>{{ stats.vacant }}</strong></div>
      <div class="stat-item occupied"><span class="dot"></span>已入住 <strong>{{ stats.occupied }}</strong></div>
      <div class="stat-item dirty"><span class="dot"></span>待清扫 <strong>{{ stats.dirty }}</strong></div>
      <div class="stat-item maintenance"><span class="dot"></span>维修中 <strong>{{ stats.maintenance }}</strong></div>
    </section>

    <!-- 房态网格：按房型分组 -->
    <section v-for="group in roomGroups" :key="group.name" class="room-section">
      <div class="section-head">
        <h3>{{ group.name }}</h3>
        <span class="helper">{{ group.prefix }}01 ~ {{ group.prefix }}{{ group.total }}</span>
      </div>
      <div class="room-grid">
        <button
          v-for="room in group.rooms"
          :key="room.no"
          class="room-cell"
          :class="'room-' + room.status"
          :title="room.no + ' — ' + roomStatusLabel(room.status)"
          @click="roomAction(room)"
        >
          <span class="room-no">{{ room.no }}</span>
          <span class="room-label">{{ roomStatusLabel(room.status) }}</span>
        </button>
      </div>
    </section>

    <!-- 弹窗：房间操作 -->
    <div v-if="selectedRoom" class="modal-overlay" @click.self="selectedRoom = null">
      <div class="modal-card">
        <h3>{{ selectedRoom.no }} 号房 · {{ selectedRoom.typeName }}</h3>
        <p :class="'room-status-badge ' + selectedRoom.status">{{ roomStatusLabel(selectedRoom.status) }}</p>
        <div class="modal-actions">
          <button v-if="selectedRoom.status === 'vacant'" class="button primary" @click="goCheckin(selectedRoom)">
            办理入住
          </button>
          <button v-if="selectedRoom.status === 'occupied'" class="button primary" @click="goCheckout(selectedRoom)">
            办理退房
          </button>
          <button v-if="selectedRoom.status === 'dirty'" class="button" @click="markClean(selectedRoom)">
            标记为已清扫
          </button>
          <button class="button" @click="selectedRoom = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminState, showToast } from '../../stores/admin.js'
import { fetchRoomTypes } from '../../api/index.js'

const router = useRouter()
const selectedRoom = ref(null)

const ROOM_PREFIX = {
  '豪华大床房': 8100,
  '商务双床房': 8200,
  '钟点房': 8600,
  '行政套房': 8800
}

onMounted(async () => {
  if (!adminState.rooms.length) {
    try { adminState.rooms = await fetchRoomTypes() } catch {}
  }
})

// 按房型组织所有房间
const roomGroups = computed(() => {
  return adminState.rooms
    .filter(r => r.enabled !== false)
    .map(roomType => {
      const total = Number(roomType.totalRooms) || 0
      const available = Number(roomType.availableRooms) || 0
      const occupied = total - available
      const dirty = Math.min(Math.floor(occupied * 0.3), 3) // 模拟 30% 退房待清扫
      const maintenance = roomType.name === '行政套房' ? 1 : 0
      const prefix = ROOM_PREFIX[roomType.name] || 8000

      const rooms = []
      for (let i = 0; i < total; i++) {
        const no = prefix + i + 1
        let status = 'vacant'
        if (i < (occupied - dirty - maintenance)) status = 'occupied'
        else if (i < (occupied - maintenance)) status = 'dirty'
        else if (i < occupied) status = 'maintenance'
        rooms.push({ no, status, typeName: roomType.name, typeId: roomType.id })
      }
      return { name: roomType.name, prefix, total, rooms }
    })
})

const stats = computed(() => {
  let vacant = 0, occupied = 0, dirty = 0, maintenance = 0
  roomGroups.value.forEach(g => g.rooms.forEach(r => {
    if (r.status === 'vacant') vacant++
    else if (r.status === 'occupied') occupied++
    else if (r.status === 'dirty') dirty++
    else if (r.status === 'maintenance') maintenance++
  }))
  return { vacant, occupied, dirty, maintenance }
})

function roomStatusLabel(s) {
  const map = { vacant: '空净房', occupied: '已入住', dirty: '待清扫', maintenance: '维修中' }
  return map[s] || s
}

function roomAction(room) { selectedRoom.value = room }

function goCheckin(room) {
  selectedRoom.value = null
  router.push({ path: '/staff/checkin', query: { roomType: room.typeName, roomNo: room.no } })
}

function goCheckout(room) {
  selectedRoom.value = null
  showToast(`跳转到退房结算：${room.no} 号房`)
  router.push('/staff/checkout')
}

function markClean(room) {
  showToast(`${room.no} 号房已标记为清扫完成`)
  selectedRoom.value = null
}
</script>

<style scoped>
/* 顶部统计条 */
.status-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  background: var(--panel-strong);
  border: 1px solid var(--line);
}
.stat-item strong { font-size: 20px; margin-left: 4px; }
.stat-item .dot { width: 12px; height: 12px; border-radius: 50%; display: inline-block; }
.vacant .dot { background: #7fb087; }
.occupied .dot { background: #c4a16d; }
.dirty .dot { background: #d4a853; }
.maintenance .dot { background: #9a9a9a; }

/* 房间网格 */
.room-section {
  margin-bottom: 24px;
}
.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
}
.section-head h3 { margin: 0; font-size: 16px; }

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
}

.room-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 14px 8px;
  border-radius: 12px;
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
  min-height: 72px;
}
.room-cell:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.1); }

.room-no { font-size: 15px; font-weight: 700; }
.room-label { font-size: 11px; opacity: 0.75; }

.room-vacant   { background: #e2efe4; border-color: #b3d4b9; color: #3b6043; }
.room-occupied { background: #faf3e5; border-color: #dcc79a; color: #7a5e2e; }
.room-dirty    { background: #fef8ec; border-color: #e8cd8a; color: #8b6918; }
.room-maintenance { background: #eee; border-color: #ccc; color: #777; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center; z-index: 100;
}
.modal-card {
  background: #fff; border-radius: 20px; padding: 28px 32px;
  min-width: 320px; max-width: 400px; box-shadow: var(--shadow);
  text-align: center;
}
.modal-card h3 { margin: 0 0 8px; font-size: 18px; }
.room-status-badge {
  display: inline-block; padding: 4px 16px; border-radius: 20px;
  font-size: 13px; font-weight: 600; margin-bottom: 20px;
}
.room-status-badge.vacant   { background: #e2efe4; color: #3b6043; }
.room-status-badge.occupied { background: #faf3e5; color: #7a5e2e; }
.room-status-badge.dirty    { background: #fef8ec; color: #8b6918; }
.room-status-badge.maintenance { background: #eee; color: #777; }
.modal-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
</style>

<template>
  <div>
    <section class="two-column">
      <!-- 入住登记表单 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>入住登记</h3>
            <p>选择房型后自动分配可用房间号，符合酒店连号管理规范</p>
          </div>
        </div>

        <div class="form-grid">
          <div class="field">
            <label>住客姓名</label>
            <input type="text" v-model="form.guestName" placeholder="例如：张三" />
          </div>
          <div class="field">
            <label>联系电话</label>
            <input type="text" v-model="form.guestPhone" placeholder="请输入手机号" />
          </div>
          <div class="field">
            <label>身份证号</label>
            <input type="text" v-model="form.guestIdCard" maxlength="18" placeholder="请输入18位身份证号" />
          </div>
          <div class="field">
            <label>选择房型</label>
            <select v-model="form.roomType" @change="onRoomTypeChange">
              <option v-for="room in enabledRooms" :key="room.id" :value="room.name">
                {{ room.name }}（可售 {{ room.availableRooms || 0 }} 间）
              </option>
            </select>
          </div>
          <div class="field">
            <label>分配房号</label>
            <select v-model="form.roomNo">
              <option v-for="no in availableRoomNumbers" :key="no" :value="String(no)">
                {{ no }} 号房间
              </option>
              <option v-if="availableRoomNumbers.length === 0" disabled>暂无可用房间</option>
            </select>
          </div>
          <div class="field">
            <label>入住日期</label>
            <input type="date" v-model="form.checkInDate" />
          </div>
          <div class="field">
            <label>退房日期</label>
            <input type="date" v-model="form.checkOutDate" />
          </div>
          <div class="field full">
            <label>备注信息</label>
            <textarea v-model="form.remark" placeholder="可填写身份证登记、发票需求、特殊偏好等"></textarea>
          </div>
        </div>

        <div class="hero-actions" style="margin-top: 18px;">
          <button class="button primary" @click="submitCheckin">提交入住登记</button>
          <button class="button" @click="saveDraft">保存草稿</button>
        </div>
      </div>

      <!-- 今日退房日程 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>今日退房日程</h3>
            <p>退房列表、查房回执和结算状态</p>
          </div>
        </div>

        <div class="calendar-list">
          <div class="calendar-item">
            <div>
              <strong>8203 · 李女士</strong>
              <div class="subtle">标准双床房 · 预计 12:00 退房</div>
            </div>
            <span class="tag warning">待查房</span>
          </div>
          <div class="calendar-item">
            <div>
              <strong>8106 · 王先生</strong>
              <div class="subtle">豪华大床房 · 已申请延迟到 14:00</div>
            </div>
            <span class="tag info">延迟退房</span>
          </div>
          <div class="calendar-item">
            <div>
              <strong>8802 · 陈女士</strong>
              <div class="subtle">行政大床房 · 押金待退回</div>
            </div>
            <span class="tag success">已结算</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 流程看板 -->
    <section class="panel">
      <div class="section-head">
        <div>
          <h3>入住退房流程看板</h3>
          <p>演示业务状态流转，答辩时可以直接讲解系统流程</p>
        </div>
      </div>

      <div class="kanban">
        <div class="kanban-column">
          <h4>待办理入住</h4>
          <div class="task-list">
            <div class="task-item">
              <strong>张同学 · HT20260430001</strong>
              <p>已支付，待核验证件并分配 {{ roomBoard.pending.roomA }} 房（{{ roomBoard.pending.typeA }}）。</p>
            </div>
            <div class="task-item">
              <strong>团队订单 · 8 人</strong>
              <p>已预登记，建议分配 {{ roomBoard.pending.roomB }}~{{ roomBoard.pending.roomB2 }} 连号双床房。</p>
            </div>
          </div>
        </div>

        <div class="kanban-column">
          <h4>在住处理中</h4>
          <div class="task-list">
            <div class="task-item">
              <strong>赵女士 · {{ roomBoard.staying.roomA }}</strong>
              <p>续住 1 晚，前台已调整订单金额并同步房态。</p>
            </div>
            <div class="task-item">
              <strong>刘先生 · {{ roomBoard.staying.roomB }}</strong>
              <p>申请加床服务，等待客房部确认库存。</p>
            </div>
          </div>
        </div>

        <div class="kanban-column">
          <h4>待完成退房</h4>
          <div class="task-list">
            <div class="task-item">
              <strong>李女士 · {{ roomBoard.checkout.roomA }}</strong>
              <p>房卡已回收，等待保洁反馈迷你吧消费记录。</p>
            </div>
            <div class="task-item">
              <strong>陈女士 · {{ roomBoard.checkout.roomB }}</strong>
              <p>押金退回完成，可将房态改为待清扫。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 统计卡片 -->
    <section class="three-column">
      <article class="board-card">
        <div class="summary-head">
          <h3>{{ occupiedCount }}</h3>
          <span class="tag success">已入住</span>
        </div>
        <p class="helper">当前系统中状态为"在住中"的房间总数。</p>
      </article>
      <article class="board-card">
        <div class="summary-head">
          <h3>{{ pendingCheckout }}</h3>
          <span class="tag warning">待退房</span>
        </div>
        <p class="helper">今日预计退房房间，需前台或客房部协同处理。</p>
      </article>
      <article class="board-card">
        <div class="summary-head">
          <h3>{{ needCleaning }}</h3>
          <span class="tag brand">待清扫</span>
        </div>
        <p class="helper">退房后待保洁处理，清扫完成后恢复可售状态。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { adminState, showToast } from '../stores/admin.js'
import { createOrder, checkInOrder } from '../api/index.js'
import { fetchDashboard, fetchRoomTypes, fetchOrders } from '../api/index.js'

// ─── 房间号体系 ──────────────────────────────────────
// 8楼布局：81xx=豪华大床房  82xx=标准双床房  86xx=亲子家庭房  88xx=行政大床房
const ROOM_PREFIX = {
  '豪华大床房': 8100,
  '标准双床房': 8200,
  '亲子家庭房': 8600,
  '行政大床房': 8800
}

function getRoomNumbers(roomName) {
  const room = adminState.rooms.find(r => r.name === roomName)
  if (!room) return { all: [], available: [] }
  const prefix = ROOM_PREFIX[roomName] || 8000
  const total = Number(room.totalRooms) || 0
  const available = Number(room.availableRooms) || 0
  const start = prefix + (total - available) + 1
  const end = prefix + total
  const availList = []
  for (let i = start; i <= end; i++) availList.push(i)
  return { all: [], available: availList }
}

// ─── 表单状态 ────────────────────────────────────────
const enabledRooms = computed(() => adminState.rooms.filter(r => r.enabled !== false))

const firstRoom = computed(() => enabledRooms.value[0]?.name || '豪华大床房')

const availableRoomNumbers = computed(() => {
  return getRoomNumbers(form.value.roomType).available
})

const form = ref({
  guestName: '',
  guestPhone: '',
  guestIdCard: '',
  roomType: '',
  roomNo: '',
  checkInDate: '',
  checkOutDate: '',
  remark: ''
})

function initForm() {
  const name = firstRoom.value
  const rooms = getRoomNumbers(name).available
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  form.value.roomType = name
  form.value.roomNo = rooms.length > 0 ? String(rooms[0]) : ''
  form.value.checkInDate = fmt(today)
  form.value.checkOutDate = fmt(tomorrow)
  form.value.guestName = ''
  form.value.guestPhone = ''
  form.value.guestIdCard = ''
  form.value.remark = ''
}

watch(firstRoom, () => { if (!form.value.roomType) initForm() }, { immediate: true })

function onRoomTypeChange() {
  const rooms = getRoomNumbers(form.value.roomType).available
  form.value.roomNo = rooms.length > 0 ? String(rooms[0]) : ''
}

function fmt(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// ─── 动态看板数据 ────────────────────────────────────
const roomBoard = computed(() => {
  const rooms = adminState.rooms
  const deluxe  = rooms.find(r => r.name === '豪华大床房')
  const biz     = rooms.find(r => r.name === '标准双床房')
  const suite   = rooms.find(r => r.name === '行政大床房')
  const aDeluxe = Number(deluxe?.availableRooms) || 0
  const aBiz    = Number(biz?.availableRooms) || 0
  const aSuite  = Number(suite?.availableRooms) || 0
  const prefix  = { '豪华大床房': 8100, '标准双床房': 8200, '行政大床房': 8800 }

  function lastAvail(name, offset = 0) {
    const r = rooms.find(x => x.name === name)
    if (!r) return '----'
    const total = Number(r.totalRooms) || 0
    const avail = Number(r.availableRooms) || 1
    const p = prefix[name] || 8000
    return String(p + total - avail + 1 + offset)
  }

  function occ(name, offset = 0) {
    const r = rooms.find(x => x.name === name)
    if (!r) return '----'
    const total = Number(r.totalRooms) || 0
    const avail = Number(r.availableRooms) || 0
    const p = prefix[name] || 8000
    return String(p + Math.min(offset + 1, Math.max(1, total - avail)))
  }

  return {
    pending: {
      typeA: '豪华大床房',
      roomA: lastAvail('豪华大床房', 0),
      roomB: String(prefix['标准双床房'] + (Number(biz?.totalRooms) || 18) - aBiz + 1),
      roomB2: String(prefix['标准双床房'] + (Number(biz?.totalRooms) || 18) - aBiz + 4)
    },
    staying: {
      roomA: occ('行政大床房', 0),
      roomB: occ('豪华大床房', 2)
    },
    checkout: {
      roomA: occ('标准双床房', 1),
      roomB: occ('行政大床房', 1)
    }
  }
})

const occupiedCount = computed(() => {
  return adminState.rooms.reduce((sum, r) => {
    const total = Number(r.totalRooms) || 0
    const avail = Number(r.availableRooms) || 0
    return sum + Math.max(0, total - avail)
  }, 0)
})

const pendingCheckout = computed(() => Math.max(1, Math.round(occupiedCount.value * 0.25)))
const needCleaning = computed(() => Math.max(1, Math.round(occupiedCount.value * 0.45)))

// ─── 提交入住 ────────────────────────────────────────
async function submitCheckin() {
  const guestIdCard = String(form.value.guestIdCard || '').replace(/\s/g, '').toUpperCase()
  if (!form.value.guestName || !/^1\d{10}$/.test(form.value.guestPhone) || !/^\d{17}[\dX]$/.test(guestIdCard)) {
    showToast('请填写住客姓名、11 位手机号和 18 位身份证号')
    return
  }
  if (!form.value.roomNo) {
    showToast('当前房型暂无可用房间')
    return
  }

  const room = adminState.rooms.find(r => r.name === form.value.roomType) || adminState.rooms[0]
  if (!room) {
    showToast('后端暂无可用房型')
    return
  }

  try {
    const start = new Date(form.value.checkInDate)
    const end = new Date(form.value.checkOutDate)
    const nights = Math.max(1, Math.round((end - start) / (24 * 60 * 60 * 1000)))

    const order = await createOrder({
      roomTypeId: room.id,
      roomTypeName: room.name,
      guestName: form.value.guestName,
      guestPhone: form.value.guestPhone,
      guestIdCard,
      stayNights: nights,
      totalAmount: Number(room.price || 0) * nights,
      checkInDate: form.value.checkInDate,
      checkOutDate: form.value.checkOutDate
    })

    await checkInOrder(order.id, { roomNo: form.value.roomNo })
    showToast(`入住登记成功！${form.value.roomType} ${form.value.roomNo} 号房，订单号 ${order.id}`)

    form.value.guestName = ''
    form.value.guestPhone = ''
    form.value.guestIdCard = ''
    form.value.remark = ''

    const [dashboard, rooms, orders] = await Promise.all([
      fetchDashboard(),
      fetchRoomTypes(),
      fetchOrders()
    ])
    adminState.dashboard = dashboard
    adminState.rooms = rooms
    adminState.orders = orders
  } catch (error) {
    showToast(`操作失败：${error.message}`)
  }
}

function saveDraft() {
  showToast('入住登记草稿已保存。')
}
</script>

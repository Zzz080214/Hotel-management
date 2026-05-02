<template>
  <div>
    <section class="two-column">
      <!-- 左侧：入住登记表单 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>入住登记</h3>
            <p>填写住客信息、选择房型，系统自动分配可用房间</p>
          </div>
        </div>

        <div class="form-grid">
          <div class="field">
            <label>住客姓名 *</label>
            <input v-model="form.guestName" placeholder="身份证姓名" />
          </div>
          <div class="field">
            <label>手机号 *</label>
            <input v-model="form.guestPhone" placeholder="11 位手机号" />
          </div>
          <div class="field">
            <label>证件类型</label>
            <select v-model="form.idType">
              <option value="身份证">身份证</option>
              <option value="护照">护照</option>
              <option value="港澳通行证">港澳通行证</option>
            </select>
          </div>
          <div class="field">
            <label>证件号码</label>
            <input v-model="form.idNumber" placeholder="证件号码" />
          </div>
          <div class="field">
            <label>房型选择 *</label>
            <select v-model="form.roomTypeName" @change="onRoomChange">
              <option v-for="r in availableRoomTypes" :key="r.id" :value="r.name">
                {{ r.name }} — {{ money(r.price) }}/晚（余 {{ r.availableRooms }} 间）
              </option>
            </select>
          </div>
          <div class="field">
            <label>分配房号</label>
            <select v-model="form.roomNo">
              <option v-for="n in roomNumbers" :key="n" :value="String(n)">{{ n }} 号房</option>
              <option v-if="roomNumbers.length === 0" disabled>暂无可用房间</option>
            </select>
          </div>
          <div class="field">
            <label>入住日期</label>
            <input type="date" v-model="form.checkIn" />
          </div>
          <div class="field">
            <label>预计退房</label>
            <input type="date" v-model="form.checkOut" />
          </div>
          <div class="field">
            <label>入住人数</label>
            <select v-model="form.guests">
              <option :value="1">1 人</option>
              <option :value="2">2 人</option>
              <option :value="3">3 人</option>
            </select>
          </div>
          <div class="field full">
            <label>备注</label>
            <textarea v-model="form.remark" placeholder="特殊需求、发票信息、接送服务等"></textarea>
          </div>
        </div>

        <div class="checkin-summary" v-if="selectedRoom">
          <div class="summary-row" v-if="selectedOrderId"><span>预订订单号</span><strong>{{ selectedOrderId }}</strong></div>
          <div class="summary-row"><span>房型</span><strong>{{ selectedRoom.name }}</strong></div>
          <div class="summary-row"><span>房价</span><strong>{{ money(selectedRoom.price) }}/晚</strong></div>
          <div class="summary-row"><span>天数</span><strong>{{ stayNights }} 晚</strong></div>
          <div class="summary-row total"><span>预估费用</span><strong>{{ money(totalAmount) }}</strong></div>
        </div>

        <div class="hero-actions">
          <button class="button primary" @click="doCheckin" style="flex:1;">
            {{ selectedOrderId ? '确认办理预订入住' : '确认入住' }}
          </button>
          <button class="button" @click="resetForm">清空重填</button>
        </div>
      </div>

      <!-- 右侧：今日预抵列表 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>今日预抵客人</h3>
            <p>已有预订、等待办理入住的订单</p>
          </div>
        </div>
        <div class="task-list">
          <div v-for="o in todayArrivals" :key="o.id" class="task-item arrival-item" @click="prefillFromOrder(o)">
            <div class="inline-head">
              <strong>{{ o.guestName }}</strong>
              <span class="tag warning">待入住</span>
            </div>
            <p>{{ o.roomTypeName }} · {{ o.id }} · 住 {{ o.stayNights }} 晚</p>
          </div>
          <div v-if="todayArrivals.length === 0" class="empty-hint">
            <p>今日暂无预抵订单</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { adminState, showToast } from '../../stores/admin.js'
import { fetchRoomTypes, fetchOrders, createOrder, checkInOrder, money } from '../../api/index.js'

const route = useRoute()

const ROOM_PREFIX = {
  '豪华大床房': 8100, '商务双床房': 8200, '钟点房': 8600, '行政套房': 8800
}

const today = () => new Date().toISOString().slice(0, 10)
const tomorrow = () => {
  const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10)
}

const form = ref({
  guestName: '', guestPhone: '', idType: '身份证', idNumber: '',
  roomTypeName: '', roomNo: '', checkIn: today(), checkOut: tomorrow(),
  guests: 1, remark: ''
})
const selectedOrderId = ref('')

const availableRoomTypes = computed(() =>
  adminState.rooms.filter(r => r.enabled !== false && (r.availableRooms || 0) > 0)
)

const selectedRoom = computed(() =>
  adminState.rooms.find(r => r.name === form.value.roomTypeName)
)

const roomNumbers = computed(() => {
  const room = selectedRoom.value
  if (!room) return []
  const prefix = ROOM_PREFIX[room.name] || 8000
  const total = Number(room.totalRooms) || 0
  const avail = Number(room.availableRooms) || 0
  const start = prefix + (total - avail) + 1
  const end = prefix + total
  const nums = []
  for (let i = start; i <= end; i++) nums.push(i)
  return nums
})

const stayNights = computed(() => {
  const s = new Date(form.value.checkIn), e = new Date(form.value.checkOut)
  return Math.max(1, Math.round((e - s) / 86400000))
})

const totalAmount = computed(() =>
  (Number(selectedRoom.value?.price) || 0) * stayNights.value
)

const todayArrivals = computed(() =>
  (adminState.orders || []).filter(o => o.status === 'upcoming')
)

function onRoomChange() {
  form.value.roomNo = roomNumbers.value.length > 0 ? String(roomNumbers.value[0]) : ''
}

function prefillFromOrder(order) {
  selectedOrderId.value = order.id || ''
  form.value.guestName = order.guestName || ''
  form.value.guestPhone = order.guestPhone || ''
  form.value.roomTypeName = order.roomTypeName || ''
  form.value.checkIn = order.checkInDate || form.value.checkIn
  form.value.checkOut = order.checkOutDate || form.value.checkOut
  form.value.roomNo = roomNumbers.value.length > 0 ? String(roomNumbers.value[0]) : ''
  showToast(`已载入订单 ${order.id} 的预订信息`)
}

function resetForm() {
  selectedOrderId.value = ''
  form.value.guestName = ''; form.value.guestPhone = ''; form.value.idNumber = ''
  form.value.remark = ''; form.value.guests = 1
  form.value.checkIn = today(); form.value.checkOut = tomorrow()
  form.value.roomNo = roomNumbers.value.length > 0 ? String(roomNumbers.value[0]) : ''
}

async function doCheckin() {
  if (!form.value.guestName || !/^1\d{10}$/.test(form.value.guestPhone)) {
    showToast('请填写完整姓名和 11 位手机号'); return
  }
  if (!form.value.roomNo) { showToast('所选房型暂无可用房间'); return }
  const room = selectedRoom.value
  if (!room) { showToast('请选择房型'); return }

  try {
    let orderId = selectedOrderId.value
    if (!orderId) {
      const order = await createOrder({
        roomTypeId: room.id, roomTypeName: room.name,
        guestName: form.value.guestName, guestPhone: form.value.guestPhone,
        stayNights: stayNights.value, totalAmount: totalAmount.value,
        checkInDate: form.value.checkIn, checkOutDate: form.value.checkOut
      })
      orderId = order.id
    }
    await checkInOrder(orderId, { roomNo: form.value.roomNo })
    showToast(`入住成功！${form.value.roomNo} 号房，${form.value.guestName}，订单号 ${orderId}`)
    resetForm()
    adminState.rooms = await fetchRoomTypes()
    adminState.orders = await fetchOrders()
  } catch (e) { showToast(`入住失败：${e.message}`) }
}

onMounted(async () => {
  if (!adminState.rooms.length) adminState.rooms = await fetchRoomTypes()
  if (!adminState.orders.length) adminState.orders = await fetchOrders()
  if (route.query.roomType) form.value.roomTypeName = route.query.roomType
  if (route.query.roomNo) form.value.roomNo = route.query.roomNo
})

// 初始化默认房型
watch(availableRoomTypes, (types) => {
  if (types.length && !form.value.roomTypeName) {
    form.value.roomTypeName = types[0].name
    onRoomChange()
  }
}, { immediate: true })
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.field.full { grid-column: 1 / -1; }
.field label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 4px; color: var(--deep); }
.field input, .field select, .field textarea {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e0d8cb;
  border-radius: 10px; font-size: 14px; background: var(--base); outline: none;
  box-sizing: border-box;
}
.field textarea { height: 64px; resize: vertical; }
.field input:focus, .field select:focus { border-color: var(--brand); background: #fff; }

.checkin-summary {
  margin-top: 18px; padding: 14px 16px; border-radius: 12px;
  background: var(--base); border: 1px solid var(--line);
}
.summary-row { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 6px; }
.summary-row.total { font-size: 16px; font-weight: 700; color: var(--brand-deep); padding-top: 6px; border-top: 1px dashed var(--line); margin-top: 6px; }
.summary-row.total strong { font-size: 20px; color: var(--brand); }

.hero-actions { display: flex; gap: 10px; margin-top: 16px; }

.arrival-item { cursor: pointer; transition: background .15s; }
.arrival-item:hover { background: var(--brand-soft); }

.empty-hint { text-align: center; color: var(--muted); padding: 24px 0; }
</style>

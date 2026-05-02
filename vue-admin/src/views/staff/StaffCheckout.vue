<template>
  <div>
    <section class="two-column">
      <!-- 左侧：在住客人搜索 + 结算 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>退房结算</h3>
            <p>搜索在住客人，核对账单并办理退房</p>
          </div>
        </div>

        <div class="search-bar">
          <input
            class="search-box" type="text" v-model="searchText"
            placeholder="输入房号 / 客人姓名 / 订单号搜索"
          />
        </div>

        <div class="task-list" v-if="filteredStaying.length">
          <div
            v-for="order in filteredStaying" :key="order.id"
            class="task-item checkout-item"
            :class="{ expanded: expandedId === order.id }"
            @click="expandedId = expandedId === order.id ? null : order.id"
          >
            <div class="inline-head">
              <strong>{{ order.roomNo || '未分配' }} · {{ order.guestName }}</strong>
              <span class="tag active">在住中</span>
            </div>
            <p>{{ order.roomTypeName }} · 入住 {{ order.checkInDate }} · {{ order.stayNights }} 晚</p>

            <!-- 展开结算详情 -->
            <div v-if="expandedId === order.id" class="bill-detail">
              <div class="bill-row"><span>订单号</span><strong>{{ order.id }}</strong></div>
              <div class="bill-row"><span>房型</span><strong>{{ order.roomTypeName }}</strong></div>
              <div class="bill-row"><span>入住日期</span><strong>{{ order.checkInDate }}</strong></div>
              <div class="bill-row"><span>退房日期</span><strong>{{ order.checkOutDate }}</strong></div>
              <div class="bill-row"><span>入住天数</span><strong>{{ order.stayNights }} 晚</strong></div>
              <div class="bill-divider"></div>
              <div class="bill-row total"><span>应收金额</span><strong>{{ money(order.totalAmount) }}</strong></div>
              <div class="bill-row" v-if="order.deposit"><span>已收押金</span><strong>{{ money(order.deposit) }}</strong></div>
              <div class="bill-actions">
                <button class="button primary" @click.stop="doCheckout(order)">确认退房</button>
                <button class="button" @click.stop="expandedId = null">取消</button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint"><p>未找到匹配的在住客人</p></div>
      </div>

      <!-- 右侧：今日预离列表 -->
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>今日预离客人</h3>
            <p>预计今天退房的订单</p>
          </div>
        </div>
        <div class="task-list">
          <div v-for="order in todayDepartures" :key="order.id" class="task-item">
            <div class="inline-head">
              <strong>{{ order.roomNo || '-' }} · {{ order.guestName }}</strong>
              <span class="tag info">今日退房</span>
            </div>
            <p>{{ order.roomTypeName }} · {{ money(order.totalAmount) }} · {{ order.stayNights }} 晚</p>
          </div>
          <div v-if="todayDepartures.length === 0" class="empty-hint">
            <p>今日暂无预离订单</p>
          </div>
        </div>

        <!-- 快速统计 -->
        <div class="quick-stats">
          <div class="qs-item">
            <strong>{{ stayingOrders.length }}</strong>
            <span>当前在住</span>
          </div>
          <div class="qs-item">
            <strong>{{ todayDepartures.length }}</strong>
            <span>今日预离</span>
          </div>
          <div class="qs-item">
            <strong>{{ checkedOutToday.length }}</strong>
            <span>今日已退</span>
          </div>
        </div>

        <!-- 退房来源统计 -->
        <div class="quick-stats" style="margin-top: 10px; padding-top: 0; border-top: none;">
          <div class="qs-item" style="background: rgba(79,118,101,0.06);">
            <strong>{{ frontDeskCount }}</strong>
            <span>前台退房</span>
          </div>
          <div class="qs-item" style="background: rgba(155,155,155,0.06);">
            <strong>{{ autoCheckoutCount }}</strong>
            <span>自动退房</span>
          </div>
        </div>

        <!-- 最近退房记录 -->
        <div v-if="checkedOutToday.length" class="checked-out-list">
          <div class="section-head" style="margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line);">
            <h3>已退房记录</h3>
          </div>
          <div
            v-for="order in checkedOutToday.slice(0, 5)" :key="'done-' + order.id"
            class="task-item done-item"
          >
            <div class="inline-head">
              <strong>{{ order.roomNo || '-' }} · {{ order.guestName }}</strong>
              <span :class="'tag ' + checkOutTag(order).css">{{ checkOutTag(order).label }}</span>
            </div>
            <p>{{ order.roomTypeName }} · {{ money(order.totalAmount) }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminState, showToast } from '../../stores/admin.js'
import { fetchOrders, money, checkOutOrder } from '../../api/index.js'

const searchText = ref('')
const expandedId = ref(null)
const lastCheckout = ref(null) // 最近一笔退房记录

onMounted(async () => {
  if (!adminState.orders.length) adminState.orders = await fetchOrders()
})

const stayingOrders = computed(() =>
  (adminState.orders || []).filter(o => o.status === 'staying')
)

const todayStr = () => new Date().toISOString().slice(0, 10)

const todayDepartures = computed(() =>
  (adminState.orders || []).filter(o =>
    o.status === 'staying' && o.checkOutDate === todayStr()
  )
)

const checkedOutToday = computed(() =>
  (adminState.orders || []).filter(o =>
    o.status === 'finished'
  ).sort((a, b) => {
    // 最近退房的排前面
    const da = a.checkOutAt || a.createdAt || ''
    const db = b.checkOutAt || b.createdAt || ''
    return db.localeCompare(da)
  })
)

const filteredStaying = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return stayingOrders.value
  return stayingOrders.value.filter(o => {
    const s = `${o.roomNo} ${o.guestName} ${o.id} ${o.roomTypeName}`.toLowerCase()
    return s.includes(kw)
  })
})

// 退房来源统计
const frontDeskCount = computed(() =>
  checkedOutToday.value.filter(o => o.checkOutSource === 'FRONT_DESK').length
)
const autoCheckoutCount = computed(() =>
  checkedOutToday.value.filter(o => o.checkOutSource === 'AUTO_CHECKOUT').length
)

// 退房来源标签（每条记录只显示一个标签）
function checkOutTag(order) {
  if (order.checkOutSource === 'AUTO_CHECKOUT') {
    return { label: '自动退房', css: 'info' }
  }
  if (order.checkOutSource === 'FRONT_DESK') {
    return { label: '前台退房', css: 'success' }
  }
  // 旧数据无来源字段
  return { label: '已退房', css: 'success' }
}

async function doCheckout(order) {
  try {
    const result = await checkOutOrder(order.id)
    lastCheckout.value = result
    showToast(`${order.guestName} 退房成功，${order.roomNo || ''} 号房已退房`)
    // 刷新订单列表
    adminState.orders = await fetchOrders()
    expandedId.value = null
  } catch (e) {
    showToast(`退房失败：${e.message}`)
  }
}
</script>

<style scoped>
.search-bar { margin-bottom: 16px; }
.search-box {
  width: 100%; padding: 10px 14px; border: 1.5px solid #e0d8cb;
  border-radius: 12px; font-size: 14px; background: var(--base); outline: none;
  box-sizing: border-box;
}
.search-box:focus { border-color: var(--brand); background: #fff; }

.checkout-item { cursor: pointer; transition: background .15s; }
.checkout-item:hover { background: var(--brand-soft); }
.checkout-item.expanded { background: #fdfbf7; border: 1px solid var(--line); border-radius: 12px; padding: 14px; }

.bill-detail { margin-top: 14px; padding-top: 14px; border-top: 1px dashed var(--line); }
.bill-row { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 6px; }
.bill-row.total { font-size: 16px; font-weight: 700; color: var(--brand-deep); }
.bill-row.total strong { color: var(--brand); font-size: 18px; }
.bill-divider { height: 1px; background: var(--line); margin: 10px 0; }
.bill-actions { display: flex; gap: 8px; margin-top: 14px; }

.empty-hint { text-align: center; color: var(--muted); padding: 32px 0; }

.quick-stats {
  display: flex; gap: 12px; margin-top: 20px; padding-top: 16px;
  border-top: 1px solid var(--line);
}
.qs-item {
  flex: 1; text-align: center; padding: 12px 8px;
  background: var(--base); border-radius: 10px;
}
.qs-item strong { display: block; font-size: 22px; color: var(--brand-deep); }
.qs-item span { font-size: 12px; color: var(--muted); }

.checked-out-list { margin-top: 8px; }
.done-item { opacity: 0.82; background: rgba(79, 118, 101, 0.04); border-radius: 10px; padding: 10px 12px; margin-bottom: 6px; }
</style>

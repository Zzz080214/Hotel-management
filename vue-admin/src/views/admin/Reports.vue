<template>
  <div>
    <!-- 营收概览卡片 -->
    <section class="kpi-grid">
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ money(kpi.todayRevenue) }}</h3>
          <span class="tag brand">今日营收</span>
        </div>
        <p>今日入住 + 在住订单的当日房费收入。</p>
      </article>
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ money(kpi.weekRevenue) }}</h3>
          <span class="tag success">本周营收</span>
        </div>
        <p>近 7 天累计订单金额（含在住与已完成）。</p>
      </article>
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ money(kpi.monthRevenue) }}</h3>
          <span class="tag info">本月营收</span>
        </div>
        <p>当月全部非取消订单的房费总额。</p>
      </article>
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ kpi.occupancyRate }}%</h3>
          <span class="tag warning">入住率</span>
        </div>
        <p>当前已入住房间数占总可售房间的比例。</p>
      </article>
    </section>

    <!-- 房型营收明细表 -->
    <section class="panel">
      <div class="section-head">
        <div>
          <h3>房型营收明细</h3>
          <p>各房型当前收入贡献与入住情况</p>
        </div>
        <button class="button" @click="exportReport">导出报表</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>房型</th>
            <th>总房间</th>
            <th>已入住</th>
            <th>可售</th>
            <th>入住率</th>
            <th>单价</th>
            <th>预估收入</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in roomDetails" :key="r.id">
            <td><strong>{{ r.name }}</strong></td>
            <td>{{ r.totalRooms }}</td>
            <td>{{ r.occupied }}</td>
            <td>{{ r.availableRooms }}</td>
            <td>
              <span :class="r.rate >= 80 ? 'status active' : r.rate >= 50 ? 'status warning' : 'status cancelled'">
                {{ r.rate }}%
              </span>
            </td>
            <td>{{ money(r.price) }}</td>
            <td>{{ money(r.estRevenue) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 订单状态分布 -->
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>订单状态分布</h3>
            <p>当前全部订单的状态占比</p>
          </div>
        </div>
        <div class="bar-group">
          <div class="bar-item">
            <div class="stat-row"><span>待入住</span><span>{{ orderStats.upcomingCount }} 单</span></div>
            <div class="bar-track"><div class="bar-fill" :style="{ width: orderStats.upcomingPct + '%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>在住中</span><span>{{ orderStats.stayingCount }} 单</span></div>
            <div class="bar-track"><div class="bar-fill success" :style="{ width: orderStats.stayingPct + '%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>已完成</span><span>{{ orderStats.finishedCount }} 单</span></div>
            <div class="bar-track"><div class="bar-fill muted" :style="{ width: orderStats.finishedPct + '%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>已取消</span><span>{{ orderStats.cancelledCount }} 单</span></div>
            <div class="bar-track"><div class="bar-fill danger" :style="{ width: orderStats.cancelledPct + '%' }"></div></div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>经营指标</h3>
            <p>关键绩效数据汇总</p>
          </div>
        </div>
        <div class="metric-list">
          <div class="metric-item">
            <span>ADR（平均房价）</span>
            <strong>{{ money(kpi.adr) }}</strong>
          </div>
          <div class="metric-item">
            <span>RevPAR（单房收益）</span>
            <strong>{{ money(kpi.revpar) }}</strong>
          </div>
          <div class="metric-item">
            <span>总订单数</span>
            <strong>{{ allOrders.length }} 单</strong>
          </div>
          <div class="metric-item">
            <span>取消率</span>
            <strong>{{ orderStats.cancelledPct }}%</strong>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { adminState, showToast } from '../../stores/admin.js'
import { fetchDashboard, fetchRoomTypes, fetchOrders, money, exportOperationsReport } from '../../api/index.js'

onMounted(async () => {
  if (!adminState.rooms.length) adminState.rooms = await fetchRoomTypes()
  if (!adminState.orders.length) adminState.orders = await fetchOrders()
})

const allOrders = computed(() => adminState.orders || [])
const allRooms = computed(() => adminState.rooms || [])

// KPI 计算
const kpi = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10)
  const monthStart = today.slice(0, 7) + '-01'

  let todayRevenue = 0, weekRevenue = 0, monthRevenue = 0

  allOrders.value.forEach(o => {
    if (o.status === 'cancelled') return
    const amt = Number(o.totalAmount) || 0
    const inDate = o.checkInDate || ''
    if (inDate === today) todayRevenue += amt
    if (inDate >= weekAgo) weekRevenue += amt
    if (inDate >= monthStart) monthRevenue += amt
  })

  const totalRooms = allRooms.value.reduce((s, r) => s + Number(r.totalRooms || 0), 0)
  const occupied = allRooms.value.reduce((s, r) => s + Math.max(0, (Number(r.totalRooms) || 0) - (Number(r.availableRooms) || 0)), 0)
  const occupancyRate = totalRooms > 0 ? Math.round((occupied / totalRooms) * 100) : 0

  const totalPrices = allRooms.value.reduce((s, r) => s + Number(r.price || 0), 0)
  const adr = allRooms.value.length > 0 ? Math.round(totalPrices / allRooms.value.length) : 0
  const revpar = totalRooms > 0 ? Math.round((adr * occupied) / totalRooms) : 0

  return { todayRevenue, weekRevenue, monthRevenue, occupancyRate, adr, revpar }
})

// 房型明细
const roomDetails = computed(() =>
  allRooms.value.map(r => {
    const total = Number(r.totalRooms) || 0
    const avail = Number(r.availableRooms) || 0
    const occupied = total - avail
    const rate = total > 0 ? Math.round((occupied / total) * 100) : 0
    const estRevenue = occupied * (Number(r.price) || 0)
    return { ...r, occupied, rate, estRevenue }
  })
)

// 订单状态
const orderStats = computed(() => {
  const total = allOrders.value.length || 1
  const upcoming = allOrders.value.filter(o => o.status === 'upcoming').length
  const staying = allOrders.value.filter(o => o.status === 'staying').length
  const finished = allOrders.value.filter(o => o.status === 'finished').length
  const cancelled = allOrders.value.filter(o => o.status === 'cancelled').length
  return {
    upcomingCount: upcoming, stayingCount: staying, finishedCount: finished, cancelledCount: cancelled,
    upcomingPct: Math.round((upcoming / total) * 100),
    stayingPct: Math.round((staying / total) * 100),
    finishedPct: Math.round((finished / total) * 100),
    cancelledPct: Math.round((cancelled / total) * 100)
  }
})

function exportReport() {
  exportOperationsReport().then(blob => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `营业报表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click(); URL.revokeObjectURL(url)
    showToast('营业报表已导出')
  }).catch(() => showToast('导出失败，请确认后端已启动'))
}
</script>

<style scoped>
.bar-fill.success { background: var(--success); }
.bar-fill.muted   { background: var(--muted); opacity: 0.5; }
.bar-fill.danger  { background: var(--danger); }

.metric-list { display: flex; flex-direction: column; gap: 12px; }
.metric-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; border-radius: 10px; background: var(--base);
}
.metric-item span { font-size: 14px; color: var(--muted); }
.metric-item strong { font-size: 18px; color: var(--deep); }
</style>

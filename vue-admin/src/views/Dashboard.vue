<template>
  <div>
    <!-- 数据加载中 -->
    <div v-if="adminState.loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载经营数据...</p>
    </div>

    <!-- Hero 区域 -->
    <template v-else>
    <section class="hero">
      <div>
        <div class="hero-kicker">Premium Service Console</div>
        <h3>悦栖酒店高端运营后台</h3>
        <p>
          面向中高端住客服务场景，这里集中管理房态、订单节奏、入住接待与品牌公告。
          页面既保留演示所需的完整业务路径，也强调更符合精品商务酒店定位的秩序感与服务质感。
        </p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span>今日均房价</span>
            <strong>¥428</strong>
          </div>
          <div class="hero-stat">
            <span>会员复购率</span>
            <strong>41%</strong>
          </div>
          <div class="hero-stat">
            <span>贵宾好评率</span>
            <strong>96%</strong>
          </div>
        </div>
        <div class="hero-actions">
          <button class="button primary" @click="$router.push('/orders')">查看全部订单</button>
          <button class="button" @click="$router.push('/orders')">查看当日订单</button>
        </div>
      </div>

      <div class="hero-side">
        <div class="trend-card">
          <small>今日营业额</small>
          <div class="trend-row">
            <strong>{{ money(metrics.todayRevenue) }}</strong>
            <span class="metric-tag success">日均监控</span>
          </div>
          <div class="trend-graph" aria-hidden="true">
            <svg viewBox="0 0 300 80" preserveAspectRatio="none">
              <path d="M0,58 C28,52 40,28 70,30 C100,32 110,46 138,42 C164,38 177,12 206,18 C234,24 248,50 300,20"
                fill="none" stroke="#9b5b28" stroke-width="4" stroke-linecap="round"></path>
              <path d="M0,80 L0,58 C28,52 40,28 70,30 C100,32 110,46 138,42 C164,38 177,12 206,18 C234,24 248,50 300,20 L300,80 Z"
                fill="rgba(185, 107, 44, 0.18)"></path>
            </svg>
          </div>
        </div>

        <div class="trend-card">
          <small>房态提示</small>
          <div class="trend-row">
            <strong>{{ metrics.occupancyRate || '0%' }}</strong>
            <span class="metric-tag brand">入住率</span>
          </div>
          <p class="mini-note">豪华大床房接近满房，标准双床房周末需求稳定，建议优先清扫待售房。</p>
        </div>
      </div>
    </section>

    <!-- 指标卡片 -->
    <section class="metrics">
      <article class="metric-card">
        <div class="metric-top">
          <span class="metric-note">今日订单</span>
          <span class="metric-tag brand">实时</span>
        </div>
        <strong>{{ metrics.todayOrders ?? orders.length }}</strong>
        <span class="mini-note">其中微信小程序预订为主力渠道</span>
      </article>

      <article class="metric-card">
        <div class="metric-top">
          <span class="metric-note">在住客房</span>
          <span class="metric-tag success">稳定</span>
        </div>
        <strong>{{ metrics.occupancyRate || '0%' }}</strong>
        <span class="mini-note">当前入住率，系统实时计算</span>
      </article>

      <article class="metric-card">
        <div class="metric-top">
          <span class="metric-note">待处理退房</span>
          <span class="metric-tag warning">关注</span>
        </div>
        <strong>{{ metrics.pendingCheckout ?? 0 }}</strong>
        <span class="mini-note">其中部分待查房确认</span>
      </article>

      <article class="metric-card">
        <div class="metric-top">
          <span class="metric-note">异常订单</span>
          <span class="metric-tag danger">优先</span>
        </div>
        <strong>{{ metrics.exceptionOrders ?? 0 }}</strong>
        <span class="mini-note">超时未支付、重复预订需核对</span>
      </article>
    </section>

    <!-- 双栏布局 -->
    <section class="dashboard">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>房型入住概览</h3>
            <p>适合后续对接房型表、房间表、订单表统计结果</p>
          </div>
          <a class="section-link" @click="$router.push('/room-types')">查看全部</a>
        </div>

        <div class="grid-two">
          <div v-for="room in dashboardRooms" :key="room.id" class="mini-panel">
            <div class="occupancy-header">
              <strong>{{ room.name }}</strong>
              <span>{{ occupied(room) }} / {{ room.totalRooms || 0 }}</span>
            </div>
            <div class="progress"><span :style="{ width: Math.min(rate(room), 100) + '%' }"></span></div>
            <div class="split">
              <span>入住率 {{ rate(room) }}%</span>
              <span>均价 {{ money(room.price) }}</span>
            </div>
          </div>
          <div v-if="dashboardRooms.length === 0" class="mini-panel">
            <p class="mini-note">暂无房型数据，请检查后端服务。</p>
          </div>
        </div>

        <div class="section-head" style="margin-top: 24px">
          <div>
            <h3>最新订单</h3>
            <p>后台首页常见展示区，可以直接迁移成表格组件</p>
          </div>
          <span class="table-meta">更新时间：{{ now }}</span>
        </div>

        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>房型</th>
              <th>入住日期</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in latestOrders" :key="order.id">
              <td>{{ order.id }}</td>
              <td>{{ order.guestName || '住客' }}</td>
              <td>{{ order.roomTypeName || '-' }}</td>
              <td>{{ order.checkInDate || '-' }}</td>
              <td>
                <span :class="orderStatusMeta[order.status]?.className || 'status pending'">
                  {{ orderStatusMeta[order.status]?.text || order.status || '待入住' }}
                </span>
              </td>
            </tr>
            <tr v-if="latestOrders.length === 0">
              <td colspan="5" style="text-align: center; color: var(--muted)">暂无订单数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>今日运营提醒</h3>
            <p>适合展示入住、退房、公告和异常提示</p>
          </div>
        </div>

        <div class="timeline">
          <div class="timeline-item">
            <div class="time-box">10:00</div>
            <div>
              <strong>3 间客房待退房确认</strong>
              <div class="mini-note">前台已发起退房，等待客房部查房并回传房态。</div>
            </div>
          </div>
          <div class="timeline-item">
            <div class="time-box">11:30</div>
            <div>
              <strong>团队订单即将到店</strong>
              <div class="mini-note">8 位住客已完成预登记，建议提前分配连号房间。</div>
            </div>
          </div>
          <div class="timeline-item">
            <div class="time-box">14:00</div>
            <div>
              <strong>周末房价策略待发布</strong>
              <div class="mini-note">管理员已保存草稿，公告发布后同步小程序首页。</div>
            </div>
          </div>
        </div>

        <div class="section-head" style="margin-top: 26px">
          <div>
            <h3>系统公告</h3>
            <p>后面可以接公告管理模块</p>
          </div>
          <a class="section-link" @click="$router.push('/notices')">进入公告统计</a>
        </div>

        <div class="announce-list">
          <div v-for="notice in adminState.notices.slice(0, 2)" :key="notice.id" class="announce">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.content }}</p>
          </div>
          <div v-if="adminState.notices.length === 0" class="announce">
            <strong>暂无公告</strong>
            <p>系统公告将在此显示。</p>
          </div>
        </div>
      </div>
    </section>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { adminState } from '../stores/admin.js'
import { money, orderStatusMeta } from '../api/index.js'

const metrics = computed(() => adminState.dashboard?.metrics || {})
const orders = computed(() => adminState.orders || [])
const dashboardRooms = computed(() => {
  return (adminState.dashboard?.roomTypes || adminState.rooms || []).slice(0, 4)
})
const latestOrders = computed(() => {
  return (adminState.dashboard?.latestOrders || adminState.orders || []).slice(0, 5)
})

function occupied(room) {
  const total = Number(room.totalRooms || 0)
  const available = Number(room.availableRooms || 0)
  return Math.max(total - available, 0)
}

function rate(room) {
  const total = Number(room.totalRooms || 0)
  if (total <= 0) return 0
  return Math.round((occupied(room) / total) * 1000) / 10
}

const now = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
</script>

<style scoped>
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--muted);
}
.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--line);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

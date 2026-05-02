<template>
  <div>
    <!-- 订单筛选 -->
    <section class="panel">
      <div class="section-head">
        <div>
          <h3>订单筛选</h3>
          <p>支持按订单号、住客姓名和状态进行演示过滤</p>
        </div>
        <button v-if="isAdmin()" class="button primary" @click="$router.push('/room-types')">房型与库存</button>
      </div>

      <div class="toolbar">
        <div class="toolbar-left">
          <input class="search-box" type="text" v-model="keyword" placeholder="输入订单号 / 用户名 / 房型" />
          <select class="select-box" v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="upcoming">待入住</option>
            <option value="staying">在住</option>
            <option value="finished">已完成</option>
            <option value="cancelled">已取消</option>
          </select>
        </div>
        <div class="toolbar-right">
          <button class="button" @click="reset">重置筛选</button>
          <span class="helper">当前显示 <strong>{{ filteredOrders.length }}</strong> 条订单</span>
        </div>
      </div>
    </section>

    <!-- KPI 卡片 -->
    <section class="kpi-grid">
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ orders.length }}</h3>
          <span class="tag brand">总订单</span>
        </div>
        <p class="helper">当前系统全部订单数量。</p>
      </article>
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ orders.filter(o => o.status === 'upcoming').length }}</h3>
          <span class="tag warning">待入住</span>
        </div>
        <p class="helper">超时未支付订单建议在系统里自动取消或短信提醒。</p>
      </article>
      <article class="kpi-card">
        <div class="summary-head">
          <h3>{{ orders.filter(o => o.status === 'cancelled').length }}</h3>
          <span class="tag danger">异常/取消</span>
        </div>
        <p class="helper">包含重复预订、价格异常和已取消订单。</p>
      </article>
    </section>

    <!-- 订单列表 -->
    <section class="panel">
      <div class="section-head">
        <div>
          <h3>订单列表</h3>
          <p>表格已支持前端过滤，后面可以直接绑定接口数据</p>
        </div>
        <span class="table-meta">共 {{ orders.length }} 条数据</span>
      </div>

      <table>
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户</th>
            <th>房型</th>
            <th>金额</th>
            <th>入住日期</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td>{{ order.id }}</td>
            <td>{{ order.guestName || '住客' }}</td>
            <td>{{ order.roomTypeName || '-' }}</td>
            <td>{{ money(order.totalAmount) }}</td>
            <td>{{ order.checkInDate || '-' }} 至 {{ order.checkOutDate || '-' }}</td>
            <td>
              <span :class="orderStatusMeta[order.status]?.className || 'status pending'">
                {{ orderStatusMeta[order.status]?.text || order.status }}
              </span>
            </td>
            <td>
              <button
                v-if="isAdmin() && (order.status === 'upcoming' || order.status === 'staying')"
                class="button"
                style="min-height: 34px; padding: 0 14px; font-size: 13px;"
                @click="handleCancel(order.id)"
              >取消</button>
              <span v-else-if="!isAdmin() && (order.status === 'upcoming' || order.status === 'staying')" class="subtle">--</span>
              <span v-else class="subtle">--</span>
            </td>
          </tr>
          <tr v-if="filteredOrders.length === 0">
            <td colspan="7" style="text-align: center; color: var(--muted); padding: 40px 0;">
              暂无匹配的订单数据
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 支付来源 & 待跟进任务 -->
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>支付与来源分布</h3>
            <p>适合后续接支付状态统计与渠道来源分析</p>
          </div>
        </div>
        <div class="bar-group">
          <div class="bar-item">
            <div class="stat-row"><span>微信支付</span><span>71%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 71%;"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>到店支付</span><span>18%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 18%;"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>OTA 渠道</span><span>11%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 11%;"></div></div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>待跟进任务</h3>
            <p>用来展示订单异常、退款与客服跟进事项</p>
          </div>
        </div>
        <div class="task-list">
          <div class="task-item">
            <div class="inline-head">
              <strong>HT20260430018</strong>
              <span class="tag danger">退款中</span>
            </div>
            <p>用户重复下单后申请取消，需审核支付流水后原路退款。</p>
          </div>
          <div class="task-item">
            <div class="inline-head">
              <strong>HT20260430027</strong>
              <span class="tag warning">未支付</span>
            </div>
            <p>订单已保留 25 分钟，若仍未付款建议释放库存。</p>
          </div>
          <div class="task-item">
            <div class="inline-head">
              <strong>HT20260430030</strong>
              <span class="tag info">信息补录</span>
            </div>
            <p>住客手机号缺失，入住前需补录身份信息和联系电话。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { adminState, showToast, isAdmin } from '../stores/admin.js'
import { money, orderStatusMeta, cancelOrder } from '../api/index.js'
import { fetchOrders } from '../api/index.js'

const keyword = ref('')
const statusFilter = ref('all')

const orders = computed(() => adminState.orders || [])

const filteredOrders = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const st = statusFilter.value
  return orders.value.filter(order => {
    const statusMatch = st === 'all' || order.status === st
    const searchText = `${order.id} ${order.guestName} ${order.roomTypeName} ${order.guestPhone}`.toLowerCase()
    const keywordMatch = !kw || searchText.includes(kw)
    return statusMatch && keywordMatch
  })
})

function reset() {
  keyword.value = ''
  statusFilter.value = 'all'
  showToast('订单筛选已重置')
}

async function handleCancel(orderId) {
  try {
    await cancelOrder(orderId)
    showToast(`订单 ${orderId} 已取消`)
    adminState.orders = await fetchOrders()
  } catch (error) {
    showToast(`取消失败：${error.message}`)
  }
}
</script>

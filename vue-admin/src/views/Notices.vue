<template>
  <div>
    <!-- 经营统计 -->
    <section class="chart-grid">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>经营统计</h3>
            <p>适合后续接后台统计接口，展示营收和房型表现</p>
          </div>
          <button class="button" @click="downloadReport">导出报表</button>
        </div>

        <div class="bar-group">
          <div class="bar-item">
            <div class="stat-row"><span>本周平均入住率</span><span>84%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 84%;"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>小程序转化率</span><span>67%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 67%;"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>复住用户比例</span><span>32%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 32%;"></div></div>
          </div>
          <div class="bar-item">
            <div class="stat-row"><span>好评订单占比</span><span>93%</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: 93%;"></div></div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>本周数据卡</h3>
            <p>适合答辩时强调系统统计分析功能</p>
          </div>
        </div>
        <div class="stat-list">
          <div class="stat-item">
            <div class="summary-head">
              <h3>¥ 126,400</h3>
              <span class="tag success">周营收</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="summary-head">
              <h3>312</h3>
              <span class="tag brand">有效订单</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="summary-head">
              <h3>18</h3>
              <span class="tag warning">待处理咨询</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 公告发布 + 已发布公告 -->
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>公告发布</h3>
            <p>模拟后台发布公告并同步到小程序首页的功能</p>
          </div>
        </div>

        <div class="field-stack" style="display: grid;">
          <div class="field">
            <label>公告标题</label>
            <input type="text" v-model="noticeForm.title" placeholder="请输入公告标题" />
          </div>
          <div class="field">
            <label>公告级别</label>
            <select v-model="noticeForm.level">
              <option>普通通知</option>
              <option>重要提醒</option>
              <option>营销活动</option>
            </select>
          </div>
          <div class="field">
            <label>公告内容</label>
            <textarea v-model="noticeForm.content" placeholder="请输入公告内容"></textarea>
          </div>
          <div class="hero-actions">
            <button class="button primary" @click="publish">发布公告</button>
            <button class="button" @click="toast('公告草稿已保存。')">保存草稿</button>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>已发布公告</h3>
            <p>可直接扩展成公告管理列表</p>
          </div>
        </div>
        <div class="notice-grid">
          <div v-for="notice in adminState.notices" :key="notice.id" class="notice-card">
            <div class="inline-head">
              <strong>{{ notice.title }}</strong>
              <span :class="'tag ' + (notice.level && notice.level.includes('重要') ? 'warning' : 'brand')">
                {{ notice.level || '公告' }}
              </span>
            </div>
            <p>{{ notice.content }}</p>
          </div>
          <div v-if="adminState.notices.length === 0" class="notice-card">
            <div class="inline-head"><strong>暂无公告</strong></div>
            <p>尚未发布任何公告，发布后将在此展示。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { adminState, showToast } from '../stores/admin.js'
import { exportOperationsReport, fetchNotices, publishNotice } from '../api/index.js'

const noticeForm = ref({
  title: '',
  level: '普通通知',
  content: ''
})

async function publish() {
  if (!noticeForm.value.title || !noticeForm.value.content) {
    showToast('请填写公告标题和内容')
    return
  }
  try {
    await publishNotice({
      title: noticeForm.value.title,
      level: noticeForm.value.level,
      content: noticeForm.value.content,
      published: true
    })
    showToast('公告已发布到后端并同步小程序')
    noticeForm.value = { title: '', level: '普通通知', content: '' }
    adminState.notices = await fetchNotices()
  } catch (error) {
    showToast(`操作失败：${error.message}`)
  }
}

async function downloadReport() {
  try {
    const blob = await exportOperationsReport()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `经营统计报表-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    showToast('经营统计报表已导出')
  } catch (error) {
    showToast(`导出失败：${error.message}`)
  }
}

function toast(msg) {
  showToast(msg)
}
</script>

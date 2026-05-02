<template>
  <!-- 未登录：只渲染登录页 -->
  <template v-if="!adminState.currentUser">
    <router-view />
    <Toast />
  </template>

  <!-- 已登录：标准后台布局 -->
  <div v-else class="shell">
    <Sidebar />
    <div class="content-shell">
      <TopBar />
      <main class="main">
        <router-view />
      </main>
      <div class="footer-note">毕业设计演示页 · 基于 Vue 3 + Spring Boot · 悦栖酒店管理后台</div>
    </div>
    <Toast />
  </div>
</template>

<script setup>
import { adminState } from './stores/admin.js'
import Sidebar from './components/Sidebar.vue'
import TopBar from './components/TopBar.vue'
import Toast from './components/Toast.vue'
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-badge">YQ</div>
        <h1>悦栖酒店管理系统</h1>
        <p>管理员 · 前台员工 统一登录入口</p>
      </div>

      <form class="login-form" @submit.prevent="doLogin">
        <div class="field">
          <label>账号</label>
          <input
            v-model="username"
            type="text"
            placeholder="输入登录账号"
            autocomplete="username"
          />
        </div>
        <div class="field">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="输入登录密码"
            autocomplete="current-password"
          />
        </div>
        <p v-if="errorMsg" class="login-error">{{ errorMsg }}</p>
        <button class="button primary login-btn" :disabled="logging">
          {{ logging ? '验证中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { handleLogin } from '../stores/admin.js'

const router = useRouter()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const logging = ref(false)

async function doLogin() {
  errorMsg.value = ''
  if (!username.value.trim()) { errorMsg.value = '请输入账号'; return }
  if (!password.value) { errorMsg.value = '请输入密码'; return }

  logging.value = true
  try {
    const result = await handleLogin(username.value.trim(), password.value)
    username.value = ''
    password.value = ''
    // 后端返回 role，系统自动判断跳转
    if (result.role === 'ADMIN') {
      router.push('/dashboard')
    } else {
      router.push('/staff/room-board')
    }
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请检查账号密码'
  } finally {
    logging.value = false
  }
}
</script>

<style scoped>
.login-shell {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 30% 20%, #f7f1e8 0%, #ece3d5 60%, #d9ceba 100%);
}

.login-card {
  width: 420px;
  max-width: 92vw;
  background: #fff;
  border-radius: 28px;
  box-shadow: 0 20px 60px rgba(0,0,0,.12);
  padding: 48px 40px 36px;
}

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}
.login-brand .brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(145deg, #2a6f97 0%, #4fb3a5 48%, #f2c66d 100%);
  color: #fff;
  font-weight: 800;
  font-size: 22px;
  text-shadow: 0 2px 10px rgba(20, 45, 65, 0.34);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.42),
    0 14px 30px rgba(20, 80, 105, 0.24);
  margin-bottom: 14px;
}
.login-brand h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--deep);
  margin: 0 0 6px;
}
.login-brand p {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--deep);
  margin-bottom: 6px;
}
.field input {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid #e0d8cb;
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  background: var(--base);
  transition: border-color .2s;
  box-sizing: border-box;
}
.field input:focus {
  border-color: var(--brand);
  background: #fff;
}
.login-error {
  color: var(--danger);
  font-size: 13px;
  margin: 0;
  text-align: center;
}
.login-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  margin-top: 6px;
  background: linear-gradient(135deg, #050505 0%, #343434 54%, #f4f4f4 100%);
  color: #fff;
  text-shadow: 0 1px 8px rgba(0,0,0,.42);
  box-shadow: 0 14px 28px rgba(0,0,0,.18);
}
.login-btn:disabled {
  opacity: .7;
  cursor: not-allowed;
}
</style>

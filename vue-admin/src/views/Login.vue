<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-badge">YQ</div>
        <h1>悦栖酒店管理系统</h1>
        <p>系统管理员 · 经理 · 前台员工 统一登录入口</p>
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
import { handleLogin, roleHome } from '../stores/admin.js'

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
    router.push(roleHome(result.role))
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
  background:
    radial-gradient(ellipse at 18% 12%, rgba(216, 184, 136, 0.22), transparent 38%),
    radial-gradient(ellipse at 82% 88%, rgba(28, 38, 50, 0.10), transparent 32%),
    linear-gradient(180deg, #f3f1ec 0%, #e9e7e1 60%, #ddd9d2 100%);
}

.login-card {
  position: relative;
  width: 440px;
  max-width: 92vw;
  background: linear-gradient(180deg, rgba(253, 251, 246, 0.98), rgba(244, 240, 233, 0.96));
  border: 1px solid rgba(28, 38, 50, 0.08);
  border-radius: 24px;
  box-shadow: 0 32px 80px rgba(15, 29, 42, 0.16);
  padding: 56px 44px 40px;
  overflow: hidden;
}

.login-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(216, 184, 136, 0.95), transparent);
}

.login-brand {
  text-align: center;
  margin-bottom: 36px;
}
.login-brand .brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: linear-gradient(145deg, #d8b888 0%, #b8945c 56%, #8a6a3e 100%);
  color: #14202c;
  font-family: "Cormorant Garamond", "Georgia", "Songti SC", serif;
  font-weight: 600;
  font-size: 24px;
  letter-spacing: 3px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.42),
    0 14px 28px rgba(120, 90, 50, 0.32);
  margin-bottom: 22px;
}
.login-brand h1 {
  font-family: "Cormorant Garamond", "Playfair Display", "Georgia", "Songti SC", serif;
  font-size: 28px;
  font-weight: 500;
  color: #0f1d2a;
  letter-spacing: 0.10em;
  margin: 0 0 10px;
}
.login-brand p {
  font-size: 11px;
  color: #6a7785;
  margin: 0;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.field label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #6a7785;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  margin-bottom: 8px;
}
.field input {
  width: 100%;
  padding: 13px 16px;
  border: 1px solid rgba(28, 38, 50, 0.12);
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  background: rgba(255, 253, 248, 0.96);
  color: #14202c;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
}
.field input:focus {
  border-color: rgba(184, 148, 92, 0.42);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(184, 148, 92, 0.10);
}
.login-error {
  color: #a05a56;
  font-size: 12px;
  margin: 0;
  text-align: center;
  letter-spacing: 0.04em;
}
.login-btn {
  width: 100%;
  padding: 14px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.18em;
  border-radius: 12px;
  margin-top: 8px;
  background: linear-gradient(135deg, #1d3146 0%, #0f1d2a 100%);
  color: #f4e8d2;
  border: 1px solid rgba(216, 184, 136, 0.18);
  box-shadow: 0 14px 28px rgba(15, 29, 42, 0.22);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 32px rgba(15, 29, 42, 0.26);
}
.login-btn:disabled {
  opacity: .7;
  cursor: not-allowed;
  transform: none;
}
</style>

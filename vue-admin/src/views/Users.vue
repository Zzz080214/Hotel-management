<template>
  <div>
    <section class="summary-grid">
      <article class="summary-card">
        <div class="summary-head">
          <h3>{{ users.length }}</h3>
          <span class="tag brand">后台账号</span>
        </div>
        <p class="helper">管理员、经理、前台员工统一在这里维护。</p>
      </article>
      <article class="summary-card">
        <div class="summary-head">
          <h3>{{ enabledCount }}</h3>
          <span class="tag success">启用中</span>
        </div>
        <p class="helper">启用账号可登录后台系统。</p>
      </article>
      <article class="summary-card">
        <div class="summary-head">
          <h3>{{ adminCount }}</h3>
          <span class="tag warning">系统管理员</span>
        </div>
        <p class="helper">负责账号权限和最高级系统维护。</p>
      </article>
      <article class="summary-card">
        <div class="summary-head">
          <h3>{{ managerCount }}</h3>
          <span class="tag info">经理账号</span>
        </div>
        <p class="helper">负责经营数据、订单和公告等管理工作。</p>
      </article>
    </section>

    <section class="two-column users-layout">
      <div class="panel">
        <div class="section-head">
          <div>
            <h3>账号列表</h3>
            <p>禁用账号会保留历史操作痕迹，但无法继续登录。</p>
          </div>
          <button class="button primary" @click="resetForm">新增账号</button>
        </div>

        <table>
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.username }}</td>
              <td>{{ user.displayName || '-' }}</td>
              <td><span :class="'tag ' + roleClass(user.role)">{{ roleText(user.role) }}</span></td>
              <td>
                <span :class="'status ' + (user.enabled === false ? 'cancelled' : 'active')">
                  {{ user.enabled === false ? '已禁用' : '启用中' }}
                </span>
              </td>
              <td>
                <div class="table-actions">
                  <button class="button table-button" @click="editUser(user)">编辑</button>
                  <button
                    class="button table-button"
                    :class="{ danger: user.enabled !== false }"
                    :disabled="user.username === currentUsername"
                    @click="toggleEnabled(user)"
                  >{{ user.enabled === false ? '启用' : '禁用' }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="5" class="empty-cell">暂无后台账号数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel">
        <div class="section-head">
          <div>
            <h3>{{ selectedId ? '编辑账号' : '新增账号' }}</h3>
            <p>{{ selectedId ? '密码留空时不会修改原密码。' : '新账号默认可立即登录后台。' }}</p>
          </div>
        </div>

        <div class="form-grid user-form">
          <div class="field">
            <label>登录账号 *</label>
            <input v-model.trim="form.username" placeholder="如 manager" />
          </div>
          <div class="field">
            <label>显示姓名</label>
            <input v-model.trim="form.displayName" placeholder="如 王经理" />
          </div>
          <div class="field">
            <label>{{ selectedId ? '重置密码' : '初始密码 *' }}</label>
            <input v-model="form.password" type="password" placeholder="编辑时留空表示不修改" />
          </div>
          <div class="field">
            <label>角色 *</label>
            <select v-model="form.role" :disabled="isEditingSelf">
              <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </div>
          <div class="field full">
            <label>账号状态</label>
            <select v-model="form.enabled" :disabled="isEditingSelf">
              <option :value="true">启用</option>
              <option :value="false">禁用</option>
            </select>
          </div>
        </div>

        <div class="hero-actions">
          <button class="button primary" :disabled="saving" @click="saveUser">
            {{ saving ? '保存中...' : selectedId ? '保存修改' : '创建账号' }}
          </button>
          <button class="button" @click="resetForm">清空</button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="section-head">
        <div>
          <h3>角色功能设计</h3>
          <p>四类用户分工清晰：管理员管权限，经理管经营，前台管接待，客户走小程序。</p>
        </div>
      </div>
      <div class="role-grid">
        <div class="task-item">
          <div class="inline-head">
            <strong>系统管理员</strong>
            <span class="tag warning">ADMIN</span>
          </div>
          <p>账号创建、角色分配、启停员工账号，并可进入所有经营管理模块。</p>
        </div>
        <div class="task-item">
          <div class="inline-head">
            <strong>经理</strong>
            <span class="tag brand">MANAGER</span>
          </div>
          <p>查看经营概览、订单、房型、报表和公告，不能维护后台账号。</p>
        </div>
        <div class="task-item">
          <div class="inline-head">
            <strong>前台员工</strong>
            <span class="tag success">STAFF</span>
          </div>
          <p>使用房态看板、入住登记和退房结算，聚焦日常接待流程。</p>
        </div>
        <div class="task-item">
          <div class="inline-head">
            <strong>客户</strong>
            <span class="tag info">CUSTOMER</span>
          </div>
          <p>通过微信小程序授权登录，完成订房、查看订单、刷脸入住和自助退房。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminState, showToast } from '../stores/admin.js'
import { createUser, fetchUsers, updateUser } from '../api/index.js'

const roleOptions = [
  { value: 'ADMIN', label: '系统管理员' },
  { value: 'MANAGER', label: '经理' },
  { value: 'STAFF', label: '前台员工' }
]

const selectedId = ref(null)
const editingOriginalUsername = ref('')
const saving = ref(false)
const form = ref(defaultForm())

const users = computed(() => adminState.users || [])
const currentUsername = computed(() => adminState.currentUser?.username || '')
const isEditingSelf = computed(() => editingOriginalUsername.value === currentUsername.value)

const enabledCount = computed(() => users.value.filter(u => u.enabled !== false).length)
const adminCount = computed(() => users.value.filter(u => u.role === 'ADMIN').length)
const managerCount = computed(() => users.value.filter(u => u.role === 'MANAGER').length)

function defaultForm() {
  return {
    username: '',
    displayName: '',
    password: '',
    role: 'MANAGER',
    enabled: true
  }
}

function roleText(role) {
  const found = roleOptions.find(item => item.value === role)
  return found ? found.label : role
}

function roleClass(role) {
  if (role === 'ADMIN') return 'warning'
  if (role === 'MANAGER') return 'brand'
  return 'success'
}

async function loadUsers() {
  try {
    adminState.users = await fetchUsers()
  } catch (error) {
    showToast(`账号列表加载失败：${error.message}`)
  }
}

function resetForm() {
  selectedId.value = null
  editingOriginalUsername.value = ''
  form.value = defaultForm()
}

function editUser(user) {
  selectedId.value = user.id
  editingOriginalUsername.value = user.username
  form.value = {
    username: user.username || '',
    displayName: user.displayName || '',
    password: '',
    role: user.role || 'MANAGER',
    enabled: user.enabled !== false
  }
}

async function saveUser() {
  if (!form.value.username) {
    showToast('请填写登录账号')
    return
  }
  if (!selectedId.value && !form.value.password) {
    showToast('请为新账号设置初始密码')
    return
  }
  const payload = {
    username: form.value.username,
    password: form.value.password,
    role: isEditingSelf.value ? 'ADMIN' : form.value.role,
    displayName: form.value.displayName,
    enabled: isEditingSelf.value ? true : form.value.enabled
  }

  saving.value = true
  try {
    if (selectedId.value) {
      await updateUser(selectedId.value, payload)
      showToast('账号信息已更新')
    } else {
      await createUser(payload)
      showToast('新账号已创建')
    }
    await loadUsers()
    resetForm()
  } catch (error) {
    showToast(`保存失败：${error.message}`)
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(user) {
  if (user.username === currentUsername.value) {
    showToast('不能禁用当前登录账号')
    return
  }
  try {
    await updateUser(user.id, {
      username: user.username,
      password: '',
      role: user.role,
      displayName: user.displayName,
      enabled: user.enabled === false
    })
    showToast(user.enabled === false ? '账号已启用' : '账号已禁用')
    await loadUsers()
  } catch (error) {
    showToast(`操作失败：${error.message}`)
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.users-layout {
  align-items: start;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.table-button {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  font-size: 13px;
}

.button.danger {
  color: var(--danger);
  background: var(--danger-soft);
}

.button:disabled {
  opacity: 0.52;
  cursor: not-allowed;
  transform: none;
}

.user-form {
  grid-template-columns: 1fr 1fr;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.empty-cell {
  text-align: center;
  color: var(--muted);
  padding: 36px 0;
}

@media (max-width: 1280px) {
  .role-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .user-form,
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>

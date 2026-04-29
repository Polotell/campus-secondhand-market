<template>
  <div class="me">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="card">
          <template #header>
            <div class="card-head"><el-icon :size="18"><User /></el-icon><span>我的账号</span></div>
          </template>
          <div class="kv"><span>用户名：</span><b>{{ userStore.user?.username }}</b></div>
          <div class="kv"><span>真实姓名：</span>{{ userStore.user?.realName || '-' }}</div>
          <div class="kv"><span>角色：</span>
            <el-tag :type="roleColor" size="small">{{ roleLabel }}</el-tag>
          </div>
          <div class="kv"><span>审核状态：</span>
            <el-tag :type="statusColor" size="small">{{ statusLabel }}</el-tag>
          </div>
          <div class="kv"><span>手机号：</span>{{ userStore.user?.phone || '-' }}</div>
          <div class="kv"><span>邮箱：</span>{{ userStore.user?.email || '-' }}</div>
          <div class="kv" v-if="userStore.role === 'MERCHANT'">
            <span>店铺：</span><b>{{ userStore.user?.shopName || '-' }}</b>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="card">
          <template #header>
            <div class="card-head"><el-icon :size="18"><Wallet /></el-icon><span>钱包 & 积分</span></div>
          </template>
          <div class="wallet-num">¥ {{ userStore.user?.balance ?? '0.00' }}</div>
          <div class="kv"><span>当前余额（元）</span></div>
          <el-divider />
          <div class="wallet-num" style="color:#E6A23C">{{ userStore.user?.points ?? 0 }}</div>
          <div class="kv"><span>我的积分（100 积分 = 1 元）</span></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="card">
          <template #header>
            <div class="card-head"><el-icon :size="18"><Connection /></el-icon><span>后端连通性</span></div>
          </template>
          <div class="kv"><span>/hello/ping：</span><b :style="pingStyle">{{ pingRes || '检测中...' }}</b></div>
          <div class="kv"><span>/hello/me：</span><b :style="meStyle">{{ meRes || '检测中...' }}</b></div>
          <el-button size="small" type="primary" plain @click="runChecks">重新检测</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User, Wallet, Connection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()

const roleLabel = computed(() => ({
  USER: '普通用户', MERCHANT: '商家', ADMIN: '管理员'
}[userStore.role] || '访客'))
const roleColor = computed(() => ({
  USER: 'info', MERCHANT: 'success', ADMIN: 'danger'
}[userStore.role] || ''))
const statusLabel = computed(() => ({
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', BANNED: '封禁'
}[userStore.user?.status] || '-'))
const statusColor = computed(() => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', BANNED: 'danger'
}[userStore.user?.status] || ''))

const pingRes = ref('')
const meRes = ref('')
const pingStyle = computed(() => pingRes.value.startsWith('✓') ? { color: '#67C23A' } : { color: '#F56C6C' })
const meStyle   = computed(() => meRes.value.startsWith('✓')   ? { color: '#67C23A' } : { color: '#F56C6C' })

async function runChecks() {
  pingRes.value = ''; meRes.value = ''
  try {
    const r = await request.get('/hello/ping')
    pingRes.value = `✓ ${r.message} (${r.app})`
  } catch (e) { pingRes.value = '✗ ' + (e.message || '失败') }
  try {
    const r = await request.get('/hello/me')
    meRes.value = `✓ uid=${r.userId} role=${r.role}`
  } catch (e) { meRes.value = '✗ ' + (e.message || '失败') }
}

onMounted(() => { userStore.refreshMe().catch(() => {}); runChecks() })
</script>

<style scoped>
.me { padding: 4px; }
.card { min-height: 230px; }
.card-head { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.kv { line-height: 1.9; color: #606266; font-size: 14px; }
.kv span { color: #909399; display: inline-block; width: 96px; }
.kv b { color: #303133; }
.wallet-num { font-size: 28px; color: #409EFF; font-weight: 700; line-height: 1.4; }
</style>

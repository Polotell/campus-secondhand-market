<template>
  <div>
    <el-alert type="success" show-icon :closable="false">
      <template #title>
        管理员登录成功！JWT + 后端鉴权 + 路由守卫（仅允许 ADMIN 角色）全部打通。
      </template>
    </el-alert>

    <el-row :gutter="20" class="cards">
      <el-col :span="6" @click="$router.push('/audit/users')" class="clickable">
        <stat-card title="待审核用户" :value="stats.pendingUsers" color="#409EFF" icon="User" />
      </el-col>
      <el-col :span="6" @click="$router.push('/audit/merchants')" class="clickable">
        <stat-card title="待审核商家" :value="stats.pendingMerchants" color="#67C23A" icon="Shop" />
      </el-col>
      <el-col :span="6" @click="$router.push('/audit/products')" class="clickable">
        <stat-card title="待审核商品" :value="stats.pendingProducts" color="#E6A23C" icon="Goods" />
      </el-col>
      <el-col :span="6"><stat-card title="今日订单"   :value="stats.todayOrders"      color="#F56C6C" icon="List" /></el-col>
    </el-row>

    <el-row :gutter="20" class="cards">
      <el-col :span="16">
        <el-card shadow="never" class="card">
          <template #header>
            <div class="card-head"><el-icon :size="18"><Connection /></el-icon><span>联调状态</span></div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="后端地址">http://localhost:8080/api</el-descriptions-item>
            <el-descriptions-item label="用户端">http://localhost:5173</el-descriptions-item>
            <el-descriptions-item label="管理端">http://localhost:5174（当前）</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 2023011308</el-descriptions-item>
            <el-descriptions-item label="/hello/ping">
              <el-tag :type="pingOk ? 'success' : 'danger'" size="small">{{ pingRes || '检测中...' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="/hello/admin">
              <el-tag :type="adminOk ? 'success' : 'danger'" size="small">{{ adminRes || '检测中...' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-button size="small" type="primary" plain style="margin-top:12px" @click="runChecks">重新检测</el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="card">
          <template #header>
            <div class="card-head"><el-icon :size="18"><Trophy /></el-icon><span>当前登录管理员</span></div>
          </template>
          <div class="kv"><span>ID：</span>{{ store.user?.id }}</div>
          <div class="kv"><span>账号：</span>{{ store.user?.username }}</div>
          <div class="kv"><span>姓名：</span>{{ store.user?.realName || '-' }}</div>
          <div class="kv"><span>手机：</span>{{ store.user?.phone || '-' }}</div>
          <div class="kv"><span>邮箱：</span>{{ store.user?.email || '-' }}</div>
          <div class="kv"><span>登录时间：</span>{{ loginTime }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="todo">
      <template #header>
        <div class="card-head">
          <el-icon :size="18"><Calendar /></el-icon>
          <span>下一阶段开发清单</span>
        </div>
      </template>
      <ol>
        <li><b>用户审核</b>：待审列表 → 通过 / 驳回（填驳回原因）</li>
        <li><b>商家审核</b>：查看营业执照 + 身份证 → 通过 / 驳回</li>
        <li><b>商品审核</b>：商家发布的新商品需审核通过后才能上架</li>
        <li><b>订单监控</b>：托管资金、纠纷处理、退货仲裁</li>
        <li><b>数据统计</b>：交易额、活跃用户、商家等级自动升级</li>
      </ol>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Connection, Trophy, Calendar } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import StatCard from '@/components/StatCard.vue'
import { listUsers } from '@/api/admin'
import { listProducts } from '@/api/product'

const store = useUserStore()
const loginTime = new Date().toLocaleString('zh-CN')

// 占位数据（后续替换为后端 /admin/stats 返回）
const stats = reactive({
  pendingUsers: '--',
  pendingMerchants: '--',
  pendingProducts: '--',
  todayOrders: '--'
})

const pingRes  = ref('')
const adminRes = ref('')
const pingOk   = computed(() => pingRes.value.startsWith('OK'))
const adminOk  = computed(() => adminRes.value.startsWith('OK'))

async function runChecks() {
  pingRes.value = ''
  adminRes.value = ''
  try {
    const r = await request.get('/hello/ping')
    pingRes.value = `OK  ${r.message} (${r.app})`
  } catch (e) { pingRes.value = 'FAIL ' + (e.message || '') }

  try {
    const r = await request.get('/hello/admin')
    adminRes.value = `OK  ${r}`
  } catch (e) { adminRes.value = 'FAIL ' + (e.message || '') }
}

async function loadStats() {
  try {
    const [u, m, p] = await Promise.all([
      listUsers({ role: 'USER',     status: 'PENDING', pageNum: 1, pageSize: 1 }),
      listUsers({ role: 'MERCHANT', status: 'PENDING', pageNum: 1, pageSize: 1 }),
      listProducts({ status: 'PENDING', pageNum: 1, pageSize: 1 })
    ])
    stats.pendingUsers     = u.total ?? 0
    stats.pendingMerchants = m.total ?? 0
    stats.pendingProducts  = p.total ?? 0
  } catch {}
}

onMounted(() => {
  store.refreshMe().catch(() => {})
  runChecks()
  loadStats()
})
</script>

<style scoped>
.cards { margin-top: 18px; }
.card { min-height: 220px; }
.card-head { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.kv { line-height: 2.0; color: #606266; font-size: 14px; }
.kv span { color: #909399; display: inline-block; width: 80px; }
.todo { margin-top: 20px; }
.todo ol { line-height: 2.1; color: #606266; margin: 0; padding-left: 22px; }
.clickable { cursor: pointer; transition: transform .15s; }
.clickable:hover { transform: translateY(-2px); }
</style>

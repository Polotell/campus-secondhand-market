<template>
  <div class="audit-page">
    <el-card shadow="never" class="toolbar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="角色">
          <el-radio-group v-model="query.role" @change="reload(1)">
            <el-radio-button :value="roleFilter">{{ roleFilter === 'USER' ? '普通用户' : '商家' }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width:140px" @change="reload(1)">
            <el-option label="待审核 PENDING" value="PENDING" />
            <el-option label="已通过 APPROVED" value="APPROVED" />
            <el-option label="已驳回 REJECTED" value="REJECTED" />
            <el-option label="封禁 BANNED" value="BANNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/姓名/手机号"
                    clearable style="width:200px" @keyup.enter="reload(1)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="reload(1)">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" stripe style="width:100%" border
                :header-cell-style="{ background:'#fafafa' }">
        <el-table-column prop="id" label="ID" width="200" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="真实姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="city" label="城市" width="80" />

        <el-table-column v-if="roleFilter === 'MERCHANT'" label="店铺 & 资质"
                         min-width="240" align="center">
          <template #default="{ row }">
            <div class="merchant-info">
              <div class="shop-name">{{ row.shopName || '-' }}</div>
              <div class="imgs">
                <el-image v-if="row.businessLicense"
                          :src="resolveImg(row.businessLicense)"
                          :preview-src-list="buildPreview(row)"
                          :initial-index="0"
                          fit="cover" class="thumb" preview-teleported />
                <el-image v-if="row.idCardFront"
                          :src="resolveImg(row.idCardFront)"
                          :preview-src-list="buildPreview(row)"
                          :initial-index="1"
                          fit="cover" class="thumb" preview-teleported />
                <el-image v-if="row.idCardBack"
                          :src="resolveImg(row.idCardBack)"
                          :preview-src-list="buildPreview(row)"
                          :initial-index="2"
                          fit="cover" class="thumb" preview-teleported />
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="query.status === 'REJECTED'" prop="rejectReason"
                         label="驳回原因" min-width="160" show-overflow-tooltip />

        <el-table-column prop="createdAt" label="注册时间" width="170" />

        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" :icon="Check" @click="onApprove(row)">通过</el-button>
              <el-button size="small" type="danger"  :icon="Close" @click="onReject(row)">驳回</el-button>
            </template>
            <template v-else>
              <span class="muted">无可用操作</span>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="reload()"
        @size-change="reload(1)" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check, Close } from '@element-plus/icons-vue'
import { listUsers, approveUser, rejectUser } from '@/api/admin'

const route = useRoute()

// 路由 meta.role 决定这个页面看的是 USER 还是 MERCHANT
const roleFilter = computed(() => route.meta?.role || 'USER')

const query = reactive({
  role: roleFilter.value,
  status: 'PENDING',
  keyword: ''
})
const records = ref([])
const total   = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

// 路由切换时（菜单点"用户审核"和"商家审核"之间切换）要重新拉数据
watch(roleFilter, (v) => {
  query.role = v
  resetQuery()
})

function resetQuery() {
  query.status = 'PENDING'
  query.keyword = ''
  reload(1)
}

async function reload(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listUsers({
      role: query.role,
      status: query.status,
      keyword: query.keyword || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function onApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确定通过 ${row.role === 'MERCHANT' ? '商家' : '用户'}「${row.username} / ${row.realName || '-'}」的申请吗？`,
      '审核通过', { type: 'success' }
    )
  } catch { return }
  await approveUser(String(row.id))
  ElMessage.success('审核通过成功')
  reload()
}

async function onReject(row) {
  let reason
  try {
    const ret = await ElMessageBox.prompt('请填写驳回原因（2~200 字）', '审核驳回', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
      inputPattern: /^.{2,200}$/,
      inputErrorMessage: '长度 2~200 字'
    })
    reason = ret.value
  } catch { return }
  await rejectUser(String(row.id), reason)
  ElMessage.success('已驳回')
  reload()
}

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', BANNED: '封禁' }[s] || s
}
function statusColor(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', BANNED: 'danger' }[s] || ''
}

function resolveImg(path) {
  if (!path) return ''
  if (/^https?:/.test(path)) return path
  return (import.meta.env.VITE_API_BASE || '/api') + path
}
function buildPreview(row) {
  return [row.businessLicense, row.idCardFront, row.idCardBack]
    .filter(Boolean)
    .map(resolveImg)
}

onMounted(() => reload(1))
</script>

<style scoped>
.audit-page { display: flex; flex-direction: column; gap: 16px; }
.toolbar :deep(.el-form-item) { margin-bottom: 4px; }
.table-card { }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }
.muted { color: #909399; font-size: 12px; }
.merchant-info { display: flex; flex-direction: column; gap: 6px; align-items: center; }
.shop-name { font-weight: 600; color: #303133; }
.imgs { display: flex; gap: 6px; }
.thumb {
  width: 52px; height: 52px; border-radius: 4px;
  border: 1px solid #dcdfe6; cursor: zoom-in; object-fit: cover;
}
</style>

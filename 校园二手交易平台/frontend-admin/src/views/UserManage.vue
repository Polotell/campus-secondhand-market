<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="用户名/姓名/手机" clearable style="width:220px" @clear="load(1)" />
        <el-select v-model="roleFilter" placeholder="角色" clearable style="width:140px" @change="load(1)">
          <el-option label="普通用户" value="USER" />
          <el-option label="商家" value="MERCHANT" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>

      <el-table :data="records" border stripe v-loading="loading" class="mt">
        <el-table-column prop="id" label="ID" width="160" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="phone" label="手机" width="120" />
        <el-table-column prop="balance" label="余额" width="100" />
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column prop="merchantLevel" label="商家等级" width="90" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openRecharge(row)">充值</el-button>
            <el-button v-if="row.role === 'MERCHANT'" link type="warning" @click="openLevel(row)">等级</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load()"
      />
    </el-card>

    <el-dialog v-model="editVisible" title="编辑用户" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="用户名"><span>{{ editForm.username }}</span></el-form-item>
        <el-form-item label="姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.city" /></el-form-item>
        <el-form-item label="性别"><el-input v-model="editForm.gender" placeholder="MALE/FEMALE/OTHER" /></el-form-item>
        <el-form-item label="银行账号"><el-input v-model="editForm.bankAccount" maxlength="16" /></el-form-item>
        <el-form-item v-if="editForm.role === 'MERCHANT'" label="店铺名"><el-input v-model="editForm.shopName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeVisible" title="钱包充值" width="400px" destroy-on-close>
      <el-form :model="rechargeForm" label-width="80px">
        <el-form-item label="金额">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="doRecharge">充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="levelVisible" title="商家等级" width="360px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="等级 1-5">
          <el-input-number v-model="levelVal" :min="1" :max="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" :loading="levelLoading" @click="saveLevel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminUser,
  listUsers,
  rechargeUser,
  setMerchantLevel,
  updateAdminUser,
  getAdminUser
} from '@/api/admin'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)
const keyword = ref('')
const roleFilter = ref('')

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
  id: null, username: '', role: '', realName: '', phone: '', email: '', city: '', gender: '', bankAccount: '', shopName: ''
})

const rechargeVisible = ref(false)
const recharging = ref(false)
const rechargeRow = ref(null)
const rechargeForm = reactive({ amount: 100 })

const levelVisible = ref(false)
const levelLoading = ref(false)
const levelRow = ref(null)
const levelVal = ref(1)

async function load(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listUsers({
      role: roleFilter.value || undefined,
      status: 'APPROVED',
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function openEdit(row) {
  const u = await getAdminUser(String(row.id))
  Object.assign(editForm, {
    id: u.id,
    username: u.username,
    role: u.role,
    realName: u.realName || '',
    phone: u.phone || '',
    email: u.email || '',
    city: u.city || '',
    gender: u.gender || '',
    bankAccount: u.bankAccount || '',
    shopName: u.shopName || ''
  })
  editVisible.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await updateAdminUser(String(editForm.id), {
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email,
      city: editForm.city,
      gender: editForm.gender,
      bankAccount: editForm.bankAccount,
      shopName: editForm.shopName
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openRecharge(row) {
  rechargeRow.value = row
  rechargeForm.amount = 100
  rechargeVisible.value = true
}

async function doRecharge() {
  recharging.value = true
  try {
    await rechargeUser(String(rechargeRow.value.id), { amount: rechargeForm.amount })
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    await load()
  } finally {
    recharging.value = false
  }
}

function openLevel(row) {
  levelRow.value = row
  levelVal.value = row.merchantLevel || 1
  levelVisible.value = true
}

async function saveLevel() {
  levelLoading.value = true
  try {
    await setMerchantLevel(String(levelRow.value.id), { level: levelVal.value })
    ElMessage.success('等级已更新')
    levelVisible.value = false
    await load()
  } finally {
    levelLoading.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？（软删除）`, '提示', { type: 'warning' })
  } catch { return }
  await deleteAdminUser(String(row.id))
  ElMessage.success('已删除')
  await load()
}

onMounted(() => load(1))
</script>

<style scoped>
.page { padding: 4px; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.mt { margin-top: 14px; }
.pager { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>

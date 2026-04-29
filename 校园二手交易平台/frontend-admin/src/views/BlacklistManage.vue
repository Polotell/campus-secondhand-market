<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openAdd">平台拉黑买家</el-button>
        <span class="hint">被拉黑的买家将无法向任意商家下单或加购（平台级）</span>
      </div>

      <el-table :data="records" border stripe v-loading="loading" class="mt">
        <el-table-column prop="id" label="记录ID" width="100" />
        <el-table-column prop="userId" label="买家用户ID" width="120" />
        <el-table-column prop="buyerName" label="买家" width="120" />
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="操作人ID" width="100" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="onRemove(row)">解除</el-button>
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

    <el-dialog v-model="addVisible" title="平台拉黑买家" width="440px" destroy-on-close>
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="买家用户ID" required>
          <el-input-number v-model="addForm.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="addForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="doAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPlatformBlacklist, addPlatformBlacklist, removePlatformBlacklist } from '@/api/admin'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)

const addVisible = ref(false)
const adding = ref(false)
const addForm = reactive({ userId: null, reason: '' })

async function load(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listPlatformBlacklist({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function openAdd() {
  addForm.userId = null
  addForm.reason = ''
  addVisible.value = true
}

async function doAdd() {
  if (!addForm.userId) {
    ElMessage.warning('请填写买家用户ID')
    return
  }
  adding.value = true
  try {
    await addPlatformBlacklist({
      userId: addForm.userId,
      reason: addForm.reason?.trim() || undefined
    })
    ElMessage.success('已拉黑')
    addVisible.value = false
    await load(1)
  } finally {
    adding.value = false
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`解除对买家 #${row.userId} 的平台拉黑？`, '提示', { type: 'warning' })
    await removePlatformBlacklist(row.id)
    ElMessage.success('已解除')
    await load()
  } catch {}
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.hint { font-size: 13px; color: #909399; }
.mt { margin-top: 16px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

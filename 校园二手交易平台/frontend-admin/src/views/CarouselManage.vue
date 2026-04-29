<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreate">新增轮播</el-button>
        <span class="hint">图片地址可为完整 URL，或与用户端一致的相对路径（如 /files/xxx）</span>
      </div>

      <el-table :data="records" border stripe v-loading="loading" class="mt">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="预览" width="140">
          <template #default="{ row }">
            <el-image :src="fullImg(row.imageUrl)" fit="cover" class="thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="imageUrl" label="图片地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="linkUrl" label="跳转" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="72" />
        <el-table-column prop="status" label="状态" width="88" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dlgVisible" :title="isEdit ? '编辑轮播' : '新增轮播'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="图片地址" required>
          <el-input v-model="form.imageUrl" placeholder="https:// 或 /files/..." />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="可选：商品路径或完整 URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ON">上架</el-radio-button>
            <el-radio-button value="OFF">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCarouselsAdmin, createCarousel, updateCarousel, deleteCarousel
} from '@/api/admin'

const apiBase = import.meta.env.VITE_API_BASE || '/api'
function fullImg(relative) {
  if (!relative) return ''
  if (/^https?:/i.test(relative)) return relative
  return apiBase + relative
}

const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)

const dlgVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = reactive({
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 'ON'
})

async function load(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listCarouselsAdmin({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.imageUrl = ''
  form.linkUrl = ''
  form.sort = 0
  form.status = 'ON'
  dlgVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  form.imageUrl = row.imageUrl || ''
  form.linkUrl = row.linkUrl || ''
  form.sort = row.sort ?? 0
  form.status = row.status || 'ON'
  dlgVisible.value = true
}

async function save() {
  if (!form.imageUrl?.trim()) {
    ElMessage.warning('请填写图片地址')
    return
  }
  saving.value = true
  try {
    const payload = {
      imageUrl: form.imageUrl.trim(),
      linkUrl: form.linkUrl?.trim() || undefined,
      sort: form.sort,
      status: form.status
    }
    if (isEdit.value) await updateCarousel(editId.value, payload)
    else await createCarousel(payload)
    ElMessage.success('已保存')
    dlgVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除轮播 #${row.id}？`, '提示', { type: 'warning' })
    await deleteCarousel(row.id)
    ElMessage.success('已删除')
    await load()
  } catch {}
}

onMounted(() => load(1))
</script>

<style scoped>
.page { }
.toolbar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.hint { font-size: 13px; color: #909399; }
.mt { margin-top: 16px; }
.thumb { width: 120px; height: 56px; border-radius: 4px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

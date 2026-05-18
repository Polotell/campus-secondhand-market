<template>
  <div class="my-products">
    <div class="top">
      <h2>我的商品</h2>
      <el-button type="primary" :icon="Plus" @click="$router.push('/merchant/product/new')">
        发布新商品
      </el-button>
    </div>

    <el-card shadow="never" class="toolbar">
      <el-radio-group v-model="query.status" @change="reload(1)">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="ON_SALE">在售</el-radio-button>
        <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
        <el-radio-button value="LOCKED">锁定中</el-radio-button>
        <el-radio-button value="SOLD">已售出</el-radio-button>
      </el-radio-group>
    </el-card>

    <el-card shadow="never" class="tbl-card">
      <el-table :data="records" v-loading="loading" stripe border
                :header-cell-style="{ background:'#fafafa' }">
        <el-table-column label="商品" min-width="320">
          <template #default="{ row }">
            <div class="p-cell">
              <el-image :src="imgUrl(row.mainImage)" fit="cover" class="p-img">
                <template #error><div class="p-err"><el-icon><PictureRounded /></el-icon></div></template>
              </el-image>
              <div class="p-info">
                <div class="p-name">
                  <router-link :to="`/product/${row.id}`" class="link">{{ row.name }}</router-link>
                </div>
                <div class="p-meta">{{ row.categoryName || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <span class="price">¥{{ row.discountPrice }}</span>
            <span v-if="row.originalPrice > row.discountPrice" class="price-old">
              ¥{{ row.originalPrice }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="salesCount" label="销量" width="70" align="center" />

        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            <el-tooltip v-if="row.status === 'REJECTED' && row.rejectReason"
                        :content="row.rejectReason" placement="top">
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="发布时间" width="160" />

        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ON_SALE'" size="small" type="warning"
                       :icon="BottomLeft" @click="onOff(row)">下架</el-button>
            <el-button v-else-if="row.status === 'OFF_SHELF'" size="small" type="success"
                       :icon="TopRight" @click="onOn(row)">上架</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <template #empty><el-empty description="还没发布过商品" /></template>
      </el-table>

      <el-pagination class="pager"
                     v-model:current-page="pageNum"
                     v-model:page-size="pageSize"
                     :page-sizes="[10,20,50]"
                     :total="total"
                     layout="total, sizes, prev, pager, next, jumper"
                     background
                     @current-change="reload()"
                     @size-change="reload(1)" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, PictureRounded, InfoFilled, BottomLeft, TopRight
} from '@element-plus/icons-vue'
import { listMyProducts, offShelfProduct, onShelfProduct } from '@/api/product'

const query = reactive({ status: undefined })
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

const apiBase = import.meta.env.VITE_API_BASE || '/api'
const imgUrl = src => !src ? '' : /^https?:/i.test(src) ? src : apiBase + src

const STATUS_LABEL = {
  DRAFT: '草稿', PENDING: '待审核', ON_SALE: '在售', LOCKED: '锁定中',
  SOLD: '已售出', OFF_SHELF: '已下架', REJECTED: '已驳回'
}
const STATUS_COLOR = {
  DRAFT: 'info', PENDING: 'warning', ON_SALE: 'success',
  LOCKED: 'warning', SOLD: 'info', OFF_SHELF: 'info', REJECTED: 'danger'
}
const statusLabel = v => STATUS_LABEL[v] || v
const statusColor = v => STATUS_COLOR[v] || ''

async function reload(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listMyProducts({
      status: query.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

async function onOff(row) {
  try { await ElMessageBox.confirm(`确定下架「${row.name}」吗？`, '下架', { type: 'warning' }) }
  catch { return }
  await offShelfProduct(String(row.id))
  ElMessage.success('已下架')
  reload()
}
async function onOn(row) {
  await onShelfProduct(String(row.id))
  ElMessage.success('已重新上架')
  reload()
}

onMounted(() => reload(1))
</script>

<style scoped>
.my-products { padding: 4px; }
.top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.top h2 { margin: 0; font-size: 20px; }
.toolbar { margin-bottom: 14px; }
.tbl-card { }
.p-cell { display: flex; align-items: center; gap: 10px; }
.p-img { width: 60px; height: 60px; border-radius: 4px; border: 1px solid #ebeef5; }
.p-err {
  width: 60px; height: 60px; display: flex; align-items: center;
  justify-content: center; background: #f5f7fa; color: #c0c4cc;
}
.p-name { font-weight: 500; color: #303133; }
.p-meta { color: #909399; font-size: 12px; margin-top: 2px; }
.link { color: #303133; text-decoration: none; }
.link:hover { color: #409EFF; }
.price { color: #F56C6C; font-weight: 600; }
.price-old { color: #c0c4cc; font-size: 12px; text-decoration: line-through; margin-left: 6px; }
.muted { color: #c0c4cc; font-size: 12px; }
.info-icon { margin-left: 4px; color: #E6A23C; cursor: pointer; }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }
</style>

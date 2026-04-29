<template>
  <div class="audit">
    <el-card shadow="never" class="toolbar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width:140px" @change="reload(1)">
            <el-option label="待审核 PENDING" value="PENDING" />
            <el-option label="在售 ON_SALE" value="ON_SALE" />
            <el-option label="已驳回 REJECTED" value="REJECTED" />
            <el-option label="已下架 OFF_SHELF" value="OFF_SHELF" />
            <el-option label="锁定中 LOCKED" value="LOCKED" />
            <el-option label="已售出 SOLD" value="SOLD" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称"
                    clearable style="width:220px" @keyup.enter="reload(1)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="reload(1)">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" v-loading="loading" stripe border
                :header-cell-style="{ background:'#fafafa' }">
        <el-table-column label="商品" min-width="340">
          <template #default="{ row }">
            <div class="p-cell">
              <el-image :src="imgUrl(row.mainImage)" fit="cover" class="p-img"
                        :preview-src-list="[imgUrl(row.mainImage)]"
                        preview-teleported
                        @click.stop>
                <template #error><div class="p-err"><el-icon><PictureRounded /></el-icon></div></template>
              </el-image>
              <div class="p-info">
                <div class="p-name">{{ row.name }}</div>
                <div class="p-meta">
                  ID: {{ row.id }} · {{ row.categoryName || '-' }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="shopName" label="店铺" width="140" />

        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <span class="price">¥{{ row.discountPrice }}</span>
            <span v-if="row.originalPrice > row.discountPrice" class="price-old">
              ¥{{ row.originalPrice }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="stock"      label="库存" width="64" align="center" />
        <el-table-column prop="salesCount" label="销量" width="64" align="center" />

        <el-table-column label="成色" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ conditionLabel(row.conditionLevel) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="query.status === 'REJECTED'" prop="rejectReason"
                         label="驳回原因" min-width="180" show-overflow-tooltip />

        <el-table-column prop="createdAt" label="发布时间" width="165" />

        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" :icon="View" @click="viewDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" :icon="Check" @click="onApprove(row)">通过</el-button>
              <el-button size="small" type="danger"  :icon="Close" @click="onReject(row)">驳回</el-button>
            </template>
          </template>
        </el-table-column>

        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>

      <el-pagination class="pager"
                     v-model:current-page="pageNum"
                     v-model:page-size="pageSize"
                     :page-sizes="[10, 20, 50]"
                     :total="total"
                     layout="total, sizes, prev, pager, next, jumper"
                     background
                     @current-change="reload()"
                     @size-change="reload(1)" />
    </el-card>

    <!-- 商品详情抽屉：全图 + 描述 -->
    <el-drawer v-model="drawerVisible" title="商品详情" size="640px" destroy-on-close>
      <div v-if="detail" class="drawer">
        <h2 class="d-name">{{ detail.name }}</h2>
        <div class="d-tags">
          <el-tag :type="statusColor(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          <el-tag size="small" effect="plain">{{ conditionLabel(detail.conditionLevel) }}</el-tag>
          <el-tag size="small" type="info" effect="plain">{{ detail.categoryName }}</el-tag>
        </div>

        <div class="d-price">
          ¥<span>{{ detail.discountPrice }}</span>
          <span class="old" v-if="detail.originalPrice > detail.discountPrice">
            ¥{{ detail.originalPrice }}
          </span>
        </div>

        <el-image-viewer v-if="previewIdx !== null"
                         :url-list="detail.images.map(imgUrl)"
                         :initial-index="previewIdx"
                         @close="previewIdx = null" />

        <div class="d-imgs">
          <el-image v-for="(src, i) in detail.images" :key="i"
                    :src="imgUrl(src)" fit="cover" class="d-img"
                    @click="previewIdx = i" />
        </div>

        <el-descriptions :column="2" border size="small" class="d-desc">
          <el-descriptions-item label="店铺">{{ detail.shopName }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
          <el-descriptions-item label="销量">{{ detail.salesCount }}</el-descriptions-item>
          <el-descriptions-item label="议价">{{ detail.negotiable ? '可议价' : '不议价' }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ detail.sizeInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
            {{ detail.rejectReason }}
          </el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="d-card">
          <template #header><b>商品描述</b></template>
          <div class="d-text">{{ detail.description || '（无）' }}</div>
        </el-card>

        <div class="d-actions" v-if="detail.status === 'PENDING'">
          <el-button type="success" size="large" :icon="Check"
                     @click="onApprove(detail); drawerVisible=false">通过</el-button>
          <el-button type="danger"  size="large" :icon="Close"
                     @click="onReject(detail); drawerVisible=false">驳回</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, View, Check, Close, PictureRounded
} from '@element-plus/icons-vue'
import { listProducts, approveProduct, rejectProduct, getProductDetail } from '@/api/product'

const apiBase = import.meta.env.VITE_API_BASE || '/api'
const imgUrl = src => !src ? '' : /^https?:/i.test(src) ? src : apiBase + src

const query = reactive({ status: 'PENDING', keyword: '' })
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

const drawerVisible = ref(false)
const detail = ref(null)
const previewIdx = ref(null)

const STATUS_LABEL = {
  DRAFT: '草稿', PENDING: '待审核', ON_SALE: '在售', LOCKED: '锁定中',
  SOLD: '已售出', OFF_SHELF: '已下架', REJECTED: '已驳回'
}
const STATUS_COLOR = {
  DRAFT: 'info', PENDING: 'warning', ON_SALE: 'success',
  LOCKED: 'warning', SOLD: 'info', OFF_SHELF: 'info', REJECTED: 'danger'
}
const CONDITION = { NEW: '全新', NINETY: '9 成新', EIGHTY: '8 成新', SEVENTY: '7 成新', OTHER: '其他' }
const statusLabel = v => STATUS_LABEL[v] || v
const statusColor = v => STATUS_COLOR[v] || ''
const conditionLabel = v => CONDITION[v] || v

function resetQuery() {
  query.status = 'PENDING'
  query.keyword = ''
  reload(1)
}

async function reload(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listProducts({
      status: query.status,
      keyword: query.keyword || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

async function viewDetail(row) {
  drawerVisible.value = true
  detail.value = null
  detail.value = await getProductDetail(row.id)
}

async function onApprove(row) {
  try {
    await ElMessageBox.confirm(`确定审核通过「${row.name}」吗？`, '审核通过', { type: 'success' })
  } catch { return }
  await approveProduct(row.id)
  ElMessage.success('审核通过')
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
  await rejectProduct(row.id, reason)
  ElMessage.success('已驳回')
  reload()
}

onMounted(() => reload(1))
</script>

<style scoped>
.audit { display: flex; flex-direction: column; gap: 16px; }
.toolbar :deep(.el-form-item) { margin-bottom: 4px; }
.p-cell { display: flex; align-items: center; gap: 10px; }
.p-img { width: 60px; height: 60px; border-radius: 4px; border: 1px solid #ebeef5; cursor: zoom-in; }
.p-err {
  width: 60px; height: 60px; display: flex; align-items: center;
  justify-content: center; background: #f5f7fa; color: #c0c4cc;
}
.p-name { font-weight: 500; color: #303133; line-height: 1.4; }
.p-meta { color: #909399; font-size: 12px; margin-top: 2px; }
.price { color: #F56C6C; font-weight: 600; }
.price-old { color: #c0c4cc; font-size: 12px; text-decoration: line-through; margin-left: 6px; }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }

.drawer { padding: 0 20px 20px; }
.d-name { margin: 0 0 8px; font-size: 20px; }
.d-tags { display: flex; gap: 8px; margin-bottom: 14px; }
.d-price {
  background: #fdf6ec; padding: 12px 16px; border-radius: 6px;
  color: #F56C6C; font-weight: 700; font-size: 14px;
  display: flex; align-items: baseline; gap: 8px;
}
.d-price span:first-of-type { font-size: 28px; }
.d-price .old { color: #c0c4cc; font-size: 13px; text-decoration: line-through; font-weight: 400; }
.d-imgs { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 14px 0; }
.d-img { width: 100%; aspect-ratio: 1/1; border-radius: 4px; border: 1px solid #ebeef5; cursor: zoom-in; }
.d-desc { margin: 12px 0; }
.d-card { }
.d-text { white-space: pre-wrap; color: #606266; line-height: 1.8; min-height: 60px; }
.d-actions { display: flex; gap: 12px; margin-top: 20px; }
.d-actions .el-button { flex: 1; }
</style>

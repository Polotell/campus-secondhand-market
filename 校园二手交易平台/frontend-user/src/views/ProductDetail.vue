<template>
  <div v-loading="loading" class="detail">
    <div v-if="p" class="wrap">
      <el-page-header @back="$router.back()" class="back">
        <template #content><span class="bc">{{ p.categoryName }} / {{ p.name }}</span></template>
      </el-page-header>

      <div class="body">
        <div class="left">
          <div class="main-img">
            <el-image v-if="currentImg" :src="imgUrl(currentImg)" fit="contain" class="big"
                      :preview-src-list="p.images.map(imgUrl)"
                      :initial-index="currentIdx"
                      preview-teleported />
            <div v-else class="big empty"><el-icon :size="60"><PictureRounded /></el-icon></div>
          </div>
          <div class="thumbs">
            <div v-for="(src, i) in p.images" :key="i" class="thumb"
                 :class="{ active: i === currentIdx }" @click="currentIdx = i">
              <el-image :src="imgUrl(src)" fit="cover" />
            </div>
          </div>
        </div>

        <div class="right">
          <div v-if="p.status !== 'ON_SALE'" class="status-banner">
            <el-tag :type="statusColor" size="large">{{ statusLabel }}</el-tag>
            <span v-if="p.rejectReason" class="rr">驳回原因：{{ p.rejectReason }}</span>
          </div>

          <h1 class="name">{{ p.name }}</h1>
          <div class="price-box">
            <span class="now">¥{{ p.discountPrice }}</span>
            <span v-if="p.originalPrice > p.discountPrice" class="old">¥{{ p.originalPrice }}</span>
            <el-tag v-if="p.negotiable" type="warning" size="small" effect="dark" round>可议价</el-tag>
          </div>

          <div class="kv-table">
            <div class="kv"><span>成色</span><b>{{ conditionLabel(p.conditionLevel) }}</b></div>
            <div class="kv"><span>库存</span><b>{{ p.stock }}</b></div>
            <div class="kv"><span>销量</span><b>{{ p.salesCount }}</b></div>
            <div class="kv" v-if="p.sizeInfo"><span>规格</span><b>{{ p.sizeInfo }}</b></div>
            <div class="kv"><span>分类</span><b>{{ p.categoryName }}</b></div>
            <div class="kv"><span>发布时间</span><b>{{ p.createdAt }}</b></div>
          </div>

          <el-card shadow="never" class="shop-card">
            <div class="shop">
              <el-icon :size="24" color="#409EFF"><Shop /></el-icon>
              <div class="shop-info">
                <div class="shop-name">{{ p.shopName }}</div>
                <div class="shop-meta">
                  好评率 {{ p.goodRate ? (p.goodRate * 100).toFixed(1) + '%' : '-' }}
                  · 评分 {{ p.avgRating || '暂无' }}
                </div>
              </div>
            </div>
          </el-card>

          <div class="actions">
            <el-input-number v-model="buyQty" :min="1" :max="Math.max(1, Number(p.stock || 1))" />
            <el-button type="primary" size="large" :icon="ShoppingCart"
                       :disabled="!canBuy" @click="onAddCart">加入购物车</el-button>
            <el-button type="danger"  size="large" :icon="Wallet"
                       :disabled="!canBuy" @click="onBuyNow">立即购买</el-button>
          </div>
        </div>
      </div>

      <el-card shadow="never" class="desc-card">
        <template #header><b>商品描述</b></template>
        <div class="desc">{{ p.description || '卖家很懒，没有填写描述' }}</div>
      </el-card>

      <el-card shadow="never" class="desc-card">
        <template #header><b>买家评价</b></template>
        <div v-loading="revLoading">
          <div v-for="r in revRecords" :key="r.id" class="rev-item">
            <div class="rev-head">
              <span>{{ r.buyerName }}</span>
              <el-rate :model-value="r.rating" disabled />
            </div>
            <div class="rev-body">{{ r.content }}</div>
            <div class="rev-time">{{ r.createdAt }}</div>
          </div>
          <el-empty v-if="!revLoading && !revRecords.length" description="暂无评价" />
          <el-pagination
            v-if="revTotal > 0"
            v-model:current-page="revPage"
            :page-size="revPageSize"
            :total="revTotal"
            layout="prev, pager, next"
            class="rev-pager"
            @current-change="loadReviews"
          />
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Shop, PictureRounded, ShoppingCart, Wallet } from '@element-plus/icons-vue'
import { getProductDetail, listProductReviews } from '@/api/product'
import { addCartItem } from '@/api/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const p = ref(null)
const loading = ref(true)
const currentIdx = ref(0)
const buyQty = ref(1)
const revLoading = ref(false)
const revRecords = ref([])
const revTotal = ref(0)
const revPage = ref(1)
const revPageSize = 5

const apiBase = import.meta.env.VITE_API_BASE || '/api'
const imgUrl = (src) => !src ? '' : /^https?:/i.test(src) ? src : apiBase + src

const currentImg = computed(() => p.value?.images?.[currentIdx.value])

const CONDITION = { NEW: '全新', NINETY: '9 成新', EIGHTY: '8 成新', SEVENTY: '7 成新', OTHER: '其他' }
const conditionLabel = v => CONDITION[v] || v

const statusLabel = computed(() => ({
  ON_SALE: '在售', LOCKED: '订单锁定中', SOLD: '已售出', OFF_SHELF: '已下架',
  PENDING: '待审核', REJECTED: '审核未通过', DRAFT: '草稿'
}[p.value?.status] || ''))
const statusColor = computed(() => ({
  ON_SALE: 'success', LOCKED: 'warning', SOLD: 'info',
  OFF_SHELF: 'info', PENDING: 'warning', REJECTED: 'danger', DRAFT: 'info'
}[p.value?.status] || ''))

const canBuy = computed(() => p.value?.status === 'ON_SALE' && p.value?.stock > 0)

async function loadReviews() {
  const pid = route.params.id
  if (!pid) return
  revLoading.value = true
  try {
    const res = await listProductReviews(pid, { pageNum: revPage.value, pageSize: revPageSize })
    revRecords.value = res.records || []
    revTotal.value = res.total || 0
  } catch {
    revRecords.value = []
    revTotal.value = 0
  } finally {
    revLoading.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    p.value = await getProductDetail(route.params.id)
    currentIdx.value = 0
    buyQty.value = 1
    revPage.value = 1
    loadReviews()
  } catch { p.value = null }
  finally { loading.value = false }
}

watch(() => route.params.id, (id) => { if (id) loadDetail() })
onMounted(loadDetail)

function ensureLogin() {
  if (userStore.isLoggedIn) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function onAddCart() {
  if (!ensureLogin() || !p.value) return
  const qty = Math.max(1, Math.min(Number(buyQty.value || 1), Number(p.value.stock || 1)))
  await addCartItem({ productId: p.value.id, quantity: qty })
  ElMessage.success('已加入购物车')
}

async function onBuyNow()  {
  if (!ensureLogin() || !p.value) return
  const qty = Math.max(1, Math.min(Number(buyQty.value || 1), Number(p.value.stock || 1)))
  const res = await addCartItem({ productId: p.value.id, quantity: qty })
  router.push({ path: '/checkout', query: { cartItemIds: String(res.id) } })
}
</script>

<style scoped>
.detail { padding: 4px; min-height: 400px; }
.back { margin-bottom: 14px; }
.bc { color: #606266; font-size: 14px; }

.wrap { background: #fff; padding: 20px 24px; border-radius: 10px; }

.body { display: grid; grid-template-columns: 420px 1fr; gap: 30px; }

.main-img { background: #f5f7fa; border-radius: 6px; overflow: hidden; }
.big { width: 420px; height: 420px; background: #f5f7fa; }
.big.empty {
  display: flex; align-items: center; justify-content: center;
  color: #c0c4cc;
}
.thumbs { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.thumb {
  width: 66px; height: 66px; cursor: pointer; border-radius: 4px; overflow: hidden;
  border: 2px solid transparent; transition: border-color .15s;
}
.thumb :deep(.el-image) { width: 100%; height: 100%; }
.thumb.active { border-color: #409EFF; }

.status-banner {
  background: #fdf6ec; border-left: 4px solid #E6A23C;
  padding: 8px 14px; border-radius: 4px; margin-bottom: 12px;
  display: flex; align-items: center; gap: 10px;
}
.rr { color: #E6A23C; font-size: 13px; }

.name { margin: 0 0 12px; font-size: 22px; color: #303133; line-height: 1.4; }

.price-box {
  background: #fdf6ec; padding: 16px 18px; border-radius: 6px;
  display: flex; align-items: center; gap: 12px;
}
.now { color: #F56C6C; font-size: 32px; font-weight: 700; }
.old { color: #c0c4cc; font-size: 14px; text-decoration: line-through; }

.kv-table {
  margin-top: 18px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px 20px;
}
.kv { display: flex; }
.kv span { color: #909399; min-width: 70px; font-size: 13px; }
.kv b { color: #303133; font-weight: 500; }

.shop-card { margin-top: 18px; }
.shop { display: flex; align-items: center; gap: 12px; }
.shop-name { font-weight: 600; color: #303133; font-size: 15px; }
.shop-meta { color: #909399; font-size: 12px; margin-top: 2px; }

.actions { margin-top: 22px; display: flex; gap: 12px; }
.actions :deep(.el-input-number) { width: 130px; }
.actions .el-button { flex: 1; }

.desc-card { margin-top: 20px; }
.desc {
  white-space: pre-wrap; word-break: break-word;
  color: #606266; line-height: 1.8; min-height: 80px;
}
.rev-item { padding: 10px 0; border-bottom: 1px solid #ebeef5; }
.rev-head { display: flex; align-items: center; gap: 12px; font-size: 14px; }
.rev-body { color: #606266; margin: 6px 0; line-height: 1.6; }
.rev-time { font-size: 12px; color: #909399; }
.rev-pager { margin-top: 12px; justify-content: center; }
</style>

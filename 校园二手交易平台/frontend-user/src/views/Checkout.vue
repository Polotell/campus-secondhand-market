<template>
  <div class="checkout" v-loading="loading">
    <el-page-header @back="$router.back()" />
    <el-card shadow="never">
      <template #header><b>结算预览</b></template>
      <template v-if="preview">
        <div class="shop">
          <el-icon><Shop /></el-icon>
          <span>{{ preview.shopName }}</span>
        </div>

        <el-table :data="preview.items || []" border stripe :header-cell-style="{ background: '#fafafa' }">
          <el-table-column prop="productName" label="商品" min-width="320" />
          <el-table-column prop="unitPrice" label="单价" width="120" align="center" />
          <el-table-column prop="quantity" label="数量" width="100" align="center" />
          <el-table-column prop="subtotal" label="小计" width="140" align="center" />
        </el-table>

        <el-form label-width="110px" class="form">
          <el-form-item label="使用积分">
            <div class="pts-row">
              <el-slider v-model="pointsUsed" :min="0" :max="maxPoints" :step="100" show-stops
                         :disabled="maxPoints <= 0" style="flex:1" />
              <span class="pts-hint">可用 {{ preview.pointsUsable || 0 }} 分（100 分抵 1 元）</span>
            </div>
          </el-form-item>
          <el-form-item label="订单备注">
            <el-input v-model="remark" maxlength="255" show-word-limit placeholder="给商家的备注（可选）" />
          </el-form-item>
          <el-form-item label="线下见面地点">
            <el-input v-model="meetPlace" maxlength="200" placeholder="如：图书馆门口 / 宿舍楼号（实验报告必选）" />
          </el-form-item>
          <el-form-item label="线下见面时间">
            <el-input v-model="meetTime" maxlength="100" placeholder="如：本周六 15:00" />
          </el-form-item>
        </el-form>

        <div class="summary">
          <div>商品总额：<b>¥{{ preview.totalAmount }}</b></div>
          <div>积分抵扣：<b>-¥{{ pointsDeduction }}</b></div>
          <div>余额：<b>¥{{ preview.buyerBalance }}</b></div>
          <div class="pay">
            实付：<span>¥{{ displayActual }}</span>
          </div>
          <el-button type="primary" size="large" :loading="creating" @click="onSubmit">
            提交订单
          </el-button>
        </div>
      </template>
      <el-empty v-else description="没有可结算的商品" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import { createOrder, previewOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const preview = ref(null)
const remark = ref('')
const meetPlace = ref('')
const meetTime = ref('')
const pointsUsed = ref(0)

const cartItemIds = computed(() => {
  const raw = String(route.query.cartItemIds || '')
  if (!raw) return []
  return raw.split(',').map(v => Number(v)).filter(v => Number.isFinite(v) && v > 0)
})

const maxPoints = computed(() => preview.value?.pointsUsable || 0)

const pointsDeduction = computed(() => {
  const n = Number(pointsUsed.value || 0) / 100
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
})

const displayActual = computed(() => {
  const t = Number(preview.value?.totalAmount || 0)
  const d = Number(pointsDeduction.value)
  const a = Math.max(0, t - d)
  return a.toFixed(2)
})

watch(preview, (pv) => {
  if (pv && typeof pv.pointsUsable === 'number') {
    pointsUsed.value = 0
  }
})

async function loadPreview() {
  if (!cartItemIds.value.length) {
    preview.value = null
    return
  }
  loading.value = true
  try {
    preview.value = await previewOrder(cartItemIds.value)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!cartItemIds.value.length) return
  creating.value = true
  try {
    const res = await createOrder({
      cartItemIds: cartItemIds.value,
      pointsUsed: pointsUsed.value,
      remark: remark.value || undefined,
      meetPlace: meetPlace.value || undefined,
      meetTime: meetTime.value || undefined
    })
    ElMessage.success('下单成功')
    router.replace({ path: '/orders', query: { highlight: res.id } })
  } finally {
    creating.value = false
  }
}

onMounted(loadPreview)
</script>

<style scoped>
.checkout { display: flex; flex-direction: column; gap: 12px; }
.shop { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; font-weight: 600; }
.form { margin-top: 14px; }
.pts-row { display: flex; align-items: center; gap: 12px; width: 100%; }
.pts-hint { font-size: 12px; color: #909399; white-space: nowrap; }
.summary {
  margin-top: 10px;
  display: flex; justify-content: flex-end; align-items: center; gap: 18px; flex-wrap: wrap;
}
.summary b { color: #303133; }
.pay span { color: #f56c6c; font-size: 28px; font-weight: 700; }
</style>

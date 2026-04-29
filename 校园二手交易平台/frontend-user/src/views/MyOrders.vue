<template>
  <div class="orders">
    <div class="top">
      <h2>我的订单</h2>
      <el-radio-group v-model="status" @change="loadOrders(1)">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PAID">待发货</el-radio-button>
        <el-radio-button label="SHIPPED">待收货</el-radio-button>
        <el-radio-button label="RECEIVED">已收货</el-radio-button>
        <el-radio-button label="RETURN_APPLYING">退货中</el-radio-button>
        <el-radio-button label="RETURNED">已退货</el-radio-button>
        <el-radio-button label="COMPLETED">已完成</el-radio-button>
        <el-radio-button label="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table :data="records" border stripe :header-cell-style="{ background: '#fafafa' }">
        <el-table-column prop="orderNo" label="订单号" min-width="220" />
        <el-table-column prop="shopName" label="店铺" min-width="140" />
        <el-table-column label="商品摘要" min-width="280">
          <template #default="{ row }">
            <div class="summary">
              <div v-for="(i, idx) in row.items || []" :key="idx" class="line">
                {{ i.productName }} x{{ i.quantity }}
              </div>
              <div v-if="row.itemCount > 3" class="more">... 共 {{ row.itemCount }} 件</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="实付" width="120" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 'PAID'" link type="danger" @click="onCancel(row)">取消</el-button>
            <el-button v-if="row.status === 'SHIPPED'" link type="success" @click="onConfirmReceive(row)">
              确认收货
            </el-button>
            <el-button v-if="row.status === 'RECEIVED'" link type="warning" @click="onApplyReturn(row)">
              申请退货
            </el-button>
            <el-button v-if="row.status === 'COMPLETED'" link type="primary" @click="openReview(row)">
              评价
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10,20,50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadOrders()"
        @size-change="loadOrders(1)"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="订单详情" size="700px">
      <div v-if="detail" class="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="店铺">{{ detail.shopName }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="商品总额">¥{{ detail.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="使用积分">{{ detail.pointsUsed || 0 }}（抵 ¥{{ detail.pointsDeduction || 0 }}）</el-descriptions-item>
          <el-descriptions-item label="实付">¥{{ detail.actualAmount }}</el-descriptions-item>
          <el-descriptions-item label="见面地点">{{ detail.meetPlace || '-' }}</el-descriptions-item>
          <el-descriptions-item label="见面时间">{{ detail.meetTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台费率">{{ detail.platformFeeRate }}</el-descriptions-item>
          <el-descriptions-item label="平台手续费">¥{{ detail.platformFee }}</el-descriptions-item>
          <el-descriptions-item label="商家应得">¥{{ detail.merchantIncome }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-table :data="detail.items || []" border>
          <el-table-column prop="productName" label="商品" min-width="300" />
          <el-table-column prop="unitPrice" label="单价" width="120" align="center" />
          <el-table-column prop="quantity" label="数量" width="100" align="center" />
          <el-table-column prop="subtotal" label="小计" width="120" align="center" />
        </el-table>

        <template v-if="detail.returnRecord">
          <el-divider />
          <h3 style="margin:0">退货记录</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="退货状态">
              <el-tag :type="returnTagType(detail.returnRecord.auditStatus)">
                {{ returnLabel(detail.returnRecord.auditStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ detail.returnRecord.applyTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退货原因" :span="2">
              {{ detail.returnRecord.reason }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.returnRecord.auditStatus !== 'PENDING'" label="审核时间">
              {{ detail.returnRecord.auditTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.returnRecord.auditStatus === 'REJECTED'" label="拒绝理由">
              {{ detail.returnRecord.auditRemark || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>

        <div v-if="detail.canApplyReturn" style="margin-top:12px">
          <el-alert
            type="info"
            :closable="false"
            title="收到货后 24 小时内可申请退货"
            :description="`截止 ${detail.returnDeadline}`"
          />
          <el-button
            type="warning"
            style="margin-top:10px"
            @click="onApplyReturn({ id: detail.id, orderNo: detail.orderNo })"
          >申请退货</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewDialogVisible" title="订单评价" width="560px" destroy-on-close>
      <template v-if="reviewOrder">
        <el-alert type="info" :closable="false" title="交易完成后可评价商品（五星+文字）及商家服务态度；可分开提交。" style="margin-bottom:12px" />
        <div v-for="(line, idx) in reviewLines" :key="line.orderItemId" class="rev-line">
          <div class="pn">{{ line.productName }}</div>
          <el-rate v-model="line.rating" />
          <el-input v-model="line.content" type="textarea" :rows="2" maxlength="500" placeholder="评价内容" />
        </div>
        <el-button v-if="reviewLines.length" type="primary" :loading="reviewProdLoading" @click="submitProductPart">
          提交商品评价
        </el-button>
        <el-divider />
        <div class="pn">商家服务态度</div>
        <el-rate v-model="merchantSvcRating" />
        <el-input v-model="merchantSvcContent" type="textarea" :rows="2" maxlength="500" placeholder="对商家沟通、发货等的评价" />
        <el-button type="success" :loading="reviewMerLoading" style="margin-top:8px" @click="submitMerchantPart">
          提交商家服务评价
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogVisible" title="申请退货" width="500px">
      <el-alert
        type="warning"
        :closable="false"
        title="提交后等待商家审核：同意将原路退款 + 商品库存恢复；拒绝将进入已完成。"
        style="margin-bottom:14px"
      />
      <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="84px">
        <el-form-item label="订单号">
          <span>{{ returnForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退货原因" prop="reason">
          <el-input
            v-model="returnForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述退货原因，例如：商品与描述不符 / 收到时已损坏 …"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="returnSubmitting" @click="submitReturn">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyReturn,
  cancelOrder,
  confirmReceiveOrder,
  getOrderDetail,
  listMyOrders,
  submitMerchantServiceReview,
  submitProductReviews
} from '@/api/order'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')

const detailVisible = ref(false)
const detail = ref(null)

const STATUS_LABEL = {
  PAID: '待发货',
  SHIPPED: '待收货',
  RECEIVED: '已收货',
  RETURN_APPLYING: '退货申请中',
  RETURN_APPROVED: '退货审批通过',
  RETURN_REJECTED: '退货被拒',
  RETURNED: '已退货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}
function statusLabel(s) { return STATUS_LABEL[s] || s }
function statusTag(s) {
  if (s === 'PAID') return 'warning'
  if (s === 'SHIPPED') return 'primary'
  if (s === 'RECEIVED' || s === 'COMPLETED') return 'success'
  if (s === 'CANCELLED' || s === 'RETURNED') return 'info'
  if (s === 'RETURN_APPLYING' || s === 'RETURN_REJECTED') return 'danger'
  return ''
}

const RETURN_LABEL = { PENDING: '商家审核中', APPROVED: '已同意（已退款）', REJECTED: '已拒绝' }
function returnLabel(s) { return RETURN_LABEL[s] || s }
function returnTagType(s) {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'info'
  return 'warning'
}

async function loadOrders(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listMyOrders({
      status: status.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  detailVisible.value = true
  detail.value = await getOrderDetail(id)
}

async function onCancel(row) {
  try {
    await ElMessageBox.confirm(`确认取消订单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await cancelOrder(row.id)
  ElMessage.success('已取消')
  await loadOrders()
}

async function onConfirmReceive(row) {
  await confirmReceiveOrder(row.id)
  ElMessage.success('已确认收货，24 小时内可申请退货')
  await loadOrders()
}

const returnDialogVisible = ref(false)
const returnSubmitting = ref(false)
const returnFormRef = ref(null)
const returnForm = reactive({ orderId: null, orderNo: '', reason: '' })
const returnRules = {
  reason: [
    { required: true, message: '请填写退货原因', trigger: 'blur' },
    { min: 4, max: 500, message: '原因 4-500 字', trigger: 'blur' }
  ]
}
function onApplyReturn(row) {
  returnForm.orderId = row.id
  returnForm.orderNo = row.orderNo
  returnForm.reason = ''
  returnDialogVisible.value = true
}
async function submitReturn() {
  await returnFormRef.value?.validate()
  returnSubmitting.value = true
  try {
    await applyReturn(returnForm.orderId, { reason: returnForm.reason })
    ElMessage.success('已提交退货申请，等待商家审核')
    returnDialogVisible.value = false
    if (detailVisible.value) detail.value = await getOrderDetail(returnForm.orderId)
    await loadOrders()
  } finally {
    returnSubmitting.value = false
  }
}

const reviewDialogVisible = ref(false)
const reviewOrder = ref(null)
const reviewLines = ref([])
const merchantSvcRating = ref(5)
const merchantSvcContent = ref('')
const reviewProdLoading = ref(false)
const reviewMerLoading = ref(false)

async function openReview(row) {
  const d = await getOrderDetail(row.id)
  reviewOrder.value = d
  reviewLines.value = (d.items || []).filter((it) => !it.reviewed).map((it) => ({
    orderItemId: it.id,
    productName: it.productName,
    rating: 5,
    content: ''
  }))
  merchantSvcRating.value = 5
  merchantSvcContent.value = ''
  reviewDialogVisible.value = true
}

async function submitProductPart() {
  if (!reviewLines.value.length) return
  reviewProdLoading.value = true
  try {
    await submitProductReviews(reviewOrder.value.id, {
      items: reviewLines.value.map(({ orderItemId, rating, content }) => ({
        orderItemId,
        rating,
        content: content || '好评'
      }))
    })
    ElMessage.success('商品评价已提交')
    reviewOrder.value = await getOrderDetail(reviewOrder.value.id)
    reviewLines.value = (reviewOrder.value.items || []).filter((it) => !it.reviewed).map((it) => ({
      orderItemId: it.id,
      productName: it.productName,
      rating: 5,
      content: ''
    }))
    if (detailVisible.value && detail.value?.id === reviewOrder.value.id) {
      detail.value = reviewOrder.value
    }
    await loadOrders()
  } finally {
    reviewProdLoading.value = false
  }
}

async function submitMerchantPart() {
  reviewMerLoading.value = true
  try {
    await submitMerchantServiceReview(reviewOrder.value.id, {
      rating: merchantSvcRating.value,
      content: merchantSvcContent.value || '服务满意'
    })
    ElMessage.success('商家服务评价已提交')
    reviewDialogVisible.value = false
    await loadOrders()
  } finally {
    reviewMerLoading.value = false
  }
}

onMounted(async () => {
  await loadOrders(1)
  const highlight = Number(route.query.highlight || 0)
  if (highlight) showDetail(highlight).catch(() => {})
})
</script>

<style scoped>
.orders { display: flex; flex-direction: column; gap: 16px; }
.top { display: flex; justify-content: space-between; align-items: center; }
.top h2 { margin: 0; }
.summary .line { color: #606266; }
.summary .more { font-size: 12px; color: #909399; margin-top: 4px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
.detail { display: flex; flex-direction: column; gap: 14px; }
.rev-line { margin-bottom: 14px; border-bottom: 1px solid #ebeef5; padding-bottom: 10px; }
.rev-line .pn { font-weight: 600; margin-bottom: 6px; }
</style>

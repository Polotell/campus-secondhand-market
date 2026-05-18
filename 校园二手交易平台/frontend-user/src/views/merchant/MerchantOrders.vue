<template>
  <div class="orders">
    <div class="top">
      <h2>店铺订单</h2>
      <el-radio-group v-model="status" @change="loadOrders(1)">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PAID">待发货</el-radio-button>
        <el-radio-button label="SHIPPED">已发货</el-radio-button>
        <el-radio-button label="RECEIVED">已收货</el-radio-button>
        <el-radio-button label="RETURN_APPLYING">待审退货</el-radio-button>
        <el-radio-button label="RETURNED">已退货</el-radio-button>
        <el-radio-button label="COMPLETED">已完成</el-radio-button>
        <el-radio-button label="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table :data="records" border stripe :header-cell-style="{ background: '#fafafa' }">
        <el-table-column prop="orderNo" label="订单号" min-width="220" />
        <el-table-column prop="buyerName" label="买家" min-width="120" />
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
        <el-table-column prop="actualAmount" label="买家实付" width="110" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="360" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button v-if="row.buyerId" link type="warning" @click="onBlacklistBuyer(row)">
              拉黑买家
            </el-button>
            <el-button v-if="row.status === 'PAID'" link type="success" @click="onShip(row)">
              发货
            </el-button>
            <template v-if="row.status === 'RETURN_APPLYING'">
              <el-button link type="success" @click="onApprove(row)">同意退货</el-button>
              <el-button link type="danger" @click="onReject(row)">拒绝</el-button>
            </template>
            <el-button v-if="row.status === 'COMPLETED'" link type="primary" @click="openBuyerReview(row)">
              评价买家
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

    <el-drawer v-model="detailVisible" title="订单详情" size="720px">
      <div v-if="detail" class="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="付款时间">{{ detail.paidAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ detail.shippedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="自动确认">{{ detail.autoConfirmAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货时间">{{ detail.receivedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品总额">¥{{ detail.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="买家实付">¥{{ detail.actualAmount }}</el-descriptions-item>
          <el-descriptions-item label="使用积分">{{ detail.pointsUsed || 0 }}（抵 ¥{{ detail.pointsDeduction || 0 }}）</el-descriptions-item>
          <el-descriptions-item label="见面地点">{{ detail.meetPlace || '-' }}</el-descriptions-item>
          <el-descriptions-item label="见面时间">{{ detail.meetTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台费率">{{ formatRate(detail.platformFeeRate) }}</el-descriptions-item>
          <el-descriptions-item label="平台手续费">¥{{ detail.platformFee }}</el-descriptions-item>
          <el-descriptions-item label="店铺应得" :span="2">
            <span class="income">¥{{ detail.merchantIncome }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>商品明细</el-divider>

        <el-table :data="detail.items || []" border>
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="prod-cell">
                <el-image
                  v-if="row.productImage"
                  :src="row.productImage"
                  fit="cover"
                  style="width: 56px; height: 56px; border-radius: 4px;" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="unitPrice" label="单价" width="100" align="center" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="subtotal" label="小计" width="100" align="center" />
        </el-table>

        <template v-if="detail.returnRecord">
          <el-divider>退货申请</el-divider>
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

        <div class="actions">
          <el-button v-if="detail.status === 'PAID'"
                     type="primary" size="large" @click="onShip(detail)">
            标记发货
          </el-button>
          <template v-if="detail.status === 'RETURN_APPLYING'">
            <el-button type="success" size="large" @click="onApprove(detail)">
              同意退货并退款
            </el-button>
            <el-button type="danger" size="large" @click="onReject(detail)">
              拒绝退货
            </el-button>
          </template>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="buyerRevVisible" title="评价买家" width="480px" destroy-on-close>
      <el-alert type="info" :closable="false" title="交易完成后对买家信用进行评价，影响对方「买家好评率」。" style="margin-bottom:12px" />
      <div v-if="buyerRevOrder">订单 {{ buyerRevOrder.orderNo }} · 买家 {{ buyerRevOrder.buyerName }}</div>
      <el-rate v-model="buyerRevRating" class="mt" />
      <el-input v-model="buyerRevContent" type="textarea" :rows="3" maxlength="500" placeholder="例如：沟通顺畅、按时取货" class="mt" />
      <template #footer>
        <el-button @click="buyerRevVisible = false">取消</el-button>
        <el-button type="primary" :loading="buyerRevLoading" @click="submitBuyerRev">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝退货" width="500px">
      <el-alert
        type="warning"
        :closable="false"
        title="拒绝后订单将进入「已完成」，买家不可再次申请退货。"
        style="margin-bottom: 14px"
      />
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="订单号">
          <span>{{ rejectForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退货原因">
          <span style="color:#909399">{{ rejectForm.reason }}</span>
        </el-form-item>
        <el-form-item label="拒绝理由" prop="remark">
          <el-input
            v-model="rejectForm.remark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请说明拒绝原因，例如：包装完好，无质量问题"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveReturn,
  getMerchantOrderDetail,
  listMerchantOrders,
  rejectReturn,
  shipOrder,
  submitBuyerReview
} from '@/api/order'
import { addMerchantBlacklist } from '@/api/merchantBlacklist'

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
  SHIPPED: '已发货',
  RECEIVED: '已收货',
  RETURN_APPLYING: '退货申请中',
  RETURN_APPROVED: '退货已批准',
  RETURN_REJECTED: '退货已拒绝',
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

const RETURN_LABEL = { PENDING: '待审核', APPROVED: '已同意（已退款）', REJECTED: '已拒绝' }
function returnLabel(s) { return RETURN_LABEL[s] || s }
function returnTagType(s) {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'info'
  return 'warning'
}
function formatRate(r) {
  if (r == null) return '-'
  return (Number(r) * 100).toFixed(1) + '%'
}

async function loadOrders(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listMerchantOrders({
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
  detail.value = await getMerchantOrderDetail(String(id))
}

async function onBlacklistBuyer(row) {
  if (!row.buyerId) {
    ElMessage.warning('缺少买家信息')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      '可选：填写拉黑原因',
      `拉黑买家 ${row.buyerName || ''}（用户 ID ${row.buyerId}）`,
      {
        confirmButtonText: '确定拉黑',
        cancelButtonText: '取消',
        inputPlaceholder: '原因（可留空）',
        inputType: 'textarea'
      }
    )
    await addMerchantBlacklist({
      userId: String(row.buyerId),
      reason: (value || '').trim() || undefined
    })
    ElMessage.success('已拉黑该买家')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
}

async function onShip(row) {
  try {
    await ElMessageBox.confirm(
      `确认发货 ${row.orderNo} 吗？\n发货后 7 天内买家未确认收货将自动确认。`,
      '发货确认',
      { type: 'warning', confirmButtonText: '发货', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  await shipOrder(String(row.id))
  ElMessage.success('已发货')
  detailVisible.value = false
  await loadOrders()
}

async function onApprove(row) {
  try {
    await ElMessageBox.confirm(
      `同意退货将立即「退款 ¥${row.actualAmount} 给买家 + 恢复商品库存」，操作不可撤销。\n确认继续？`,
      '同意退货',
      { type: 'warning', confirmButtonText: '同意并退款', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  await approveReturn(String(row.id))
  ElMessage.success('已同意退货，款项与库存已归还')
  detailVisible.value = false
  await loadOrders()
}

const rejectDialogVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({ orderId: null, orderNo: '', reason: '', remark: '' })
const rejectRules = {
  remark: [
    { required: true, message: '请填写拒绝理由', trigger: 'blur' },
    { min: 4, max: 500, message: '拒绝理由 4-500 字', trigger: 'blur' }
  ]
}
async function onReject(row) {
  rejectForm.orderId = String(row.id)
  rejectForm.orderNo = row.orderNo
  rejectForm.remark = ''
  // 列表页 VO 不带 returnRecord，行级触发时先拿详情把买家原因展示出来
  let reason = row.returnRecord?.reason
  if (!reason) {
    try {
      const d = await getMerchantOrderDetail(String(row.id))
      reason = d?.returnRecord?.reason || '-'
    } catch { reason = '-' }
  }
  rejectForm.reason = reason
  rejectDialogVisible.value = true
}
async function submitReject() {
  await rejectFormRef.value?.validate()
  rejectSubmitting.value = true
  try {
    await rejectReturn(String(rejectForm.orderId), { remark: rejectForm.remark })
    ElMessage.success('已拒绝退货，订单进入已完成')
    rejectDialogVisible.value = false
    detailVisible.value = false
    await loadOrders()
  } finally {
    rejectSubmitting.value = false
  }
}

const buyerRevVisible = ref(false)
const buyerRevOrder = ref(null)
const buyerRevRating = ref(5)
const buyerRevContent = ref('')
const buyerRevLoading = ref(false)

function openBuyerReview(row) {
  buyerRevOrder.value = { id: String(row.id), orderNo: row.orderNo, buyerName: row.buyerName }
  buyerRevRating.value = 5
  buyerRevContent.value = ''
  buyerRevVisible.value = true
}

async function submitBuyerRev() {
  if (!buyerRevOrder.value) return
  buyerRevLoading.value = true
  try {
    await submitBuyerReview(String(buyerRevOrder.value.id), {
      rating: buyerRevRating.value,
      content: buyerRevContent.value || '交易顺利'
    })
    ElMessage.success('已提交对买家的评价')
    buyerRevVisible.value = false
    await loadOrders()
  } finally {
    buyerRevLoading.value = false
  }
}

onMounted(() => loadOrders(1))
</script>

<style scoped>
.orders { display: flex; flex-direction: column; gap: 16px; }
.top { display: flex; justify-content: space-between; align-items: center; }
.top h2 { margin: 0; }
.summary .line { color: #606266; }
.summary .more { font-size: 12px; color: #909399; margin-top: 4px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
.detail { display: flex; flex-direction: column; gap: 14px; }
.prod-cell { display: flex; align-items: center; gap: 10px; }
.income { color: #67c23a; font-weight: 600; font-size: 16px; }
.actions { display: flex; justify-content: flex-end; padding-top: 8px; }
.mt { margin-top: 10px; }
</style>

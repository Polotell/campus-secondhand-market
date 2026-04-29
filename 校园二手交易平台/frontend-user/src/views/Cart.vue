<template>
  <div class="cart">
    <div class="head">
      <h2>我的购物车</h2>
      <el-space>
        <el-button @click="onSelectAll(true)">全选</el-button>
        <el-button @click="onSelectAll(false)">全不选</el-button>
        <el-button type="danger" plain @click="onClearSelected">删除已选</el-button>
      </el-space>
    </div>

    <el-card v-loading="loading" shadow="never">
      <template v-if="view.groups?.length">
        <div v-for="g in view.groups" :key="g.merchantId" class="shop-block">
          <div class="shop-title">
            <el-icon><Shop /></el-icon>
            <span>{{ g.shopName || '未知店铺' }}</span>
          </div>

          <el-table :data="g.items" border stripe :header-cell-style="{ background: '#fafafa' }">
            <el-table-column label="勾选" width="66" align="center">
              <template #default="{ row }">
                <el-checkbox
                  :model-value="row.selected === 1"
                  :disabled="!row.available"
                  @change="onToggleSelected(row, $event)"
                />
              </template>
            </el-table-column>

            <el-table-column label="商品" min-width="340">
              <template #default="{ row }">
                <div class="p-cell">
                  <el-image :src="imgUrl(row.productImage)" class="p-img" fit="cover" />
                  <div class="p-info">
                    <router-link class="name" :to="`/product/${row.productId}`">
                      {{ row.productName }}
                    </router-link>
                    <div class="meta">
                      <el-tag size="small" effect="plain">{{ row.productStatus }}</el-tag>
                      <span>库存 {{ row.stock }}</span>
                    </div>
                    <div v-if="!row.available" class="warn">{{ row.unavailableReason }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="单价" width="120" align="center">
              <template #default="{ row }">
                <span class="price">¥{{ row.unitPrice }}</span>
              </template>
            </el-table-column>

            <el-table-column label="数量" width="150" align="center">
              <template #default="{ row }">
                <el-input-number
                  :model-value="row.quantity"
                  :min="1"
                  :max="999"
                  size="small"
                  @change="onChangeQty(row, $event)"
                />
              </template>
            </el-table-column>

            <el-table-column label="小计" width="140" align="center">
              <template #default="{ row }">
                <span class="subtotal">¥{{ row.subtotal }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button type="danger" link @click="onRemove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      <el-empty v-else description="购物车空空如也，先去逛逛吧" />
    </el-card>

    <div class="bar">
      <div class="left">
        已选 <b>{{ view.selectedCount || 0 }}</b> 件
      </div>
      <div class="right">
        合计：<span class="total">¥{{ view.selectedTotal || '0.00' }}</span>
        <el-button type="primary" size="large" :disabled="!selectedIds.length" @click="goCheckout">
          去结算
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import {
  clearSelectedCartItems,
  getCart,
  removeCartItem,
  selectAllCartItems,
  updateCartItem
} from '@/api/cart'

const router = useRouter()
const loading = ref(false)
const view = reactive({
  groups: [],
  selectedCount: 0,
  selectedTotal: '0.00',
  totalCount: 0
})

const apiBase = import.meta.env.VITE_API_BASE || '/api'
const imgUrl = (src) => !src ? '' : /^https?:/i.test(src) ? src : apiBase + src

const selectedIds = computed(() => {
  const ids = []
  for (const g of view.groups || []) {
    for (const i of g.items || []) {
      if (i.selected === 1 && i.available) ids.push(i.id)
    }
  }
  return ids
})

async function loadCart() {
  loading.value = true
  try {
    const r = await getCart()
    view.groups = r.groups || []
    view.selectedCount = r.selectedCount || 0
    view.selectedTotal = r.selectedTotal || '0.00'
    view.totalCount = r.totalCount || 0
  } finally {
    loading.value = false
  }
}

async function onToggleSelected(row, val) {
  await updateCartItem(row.id, { selected: val ? 1 : 0 })
  await loadCart()
}

async function onChangeQty(row, val) {
  if (!val) return
  await updateCartItem(row.id, { quantity: Number(val) })
  await loadCart()
}

async function onRemove(row) {
  await removeCartItem(row.id)
  ElMessage.success('已删除')
  await loadCart()
}

async function onSelectAll(selected) {
  await selectAllCartItems(selected)
  await loadCart()
}

async function onClearSelected() {
  try {
    await ElMessageBox.confirm('确认删除所有已勾选商品吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await clearSelectedCartItems()
  ElMessage.success('已清理')
  await loadCart()
}

function goCheckout() {
  if (!selectedIds.value.length) return
  router.push({ path: '/checkout', query: { cartItemIds: selectedIds.value.join(',') } })
}

onMounted(loadCart)
</script>

<style scoped>
.cart { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; align-items: center; justify-content: space-between; }
.head h2 { margin: 0; }
.shop-block { margin-bottom: 16px; }
.shop-title {
  display: flex; align-items: center; gap: 6px;
  font-weight: 600; margin-bottom: 8px;
}
.p-cell { display: flex; gap: 10px; align-items: center; }
.p-img { width: 64px; height: 64px; border-radius: 4px; border: 1px solid #ebeef5; }
.p-info .name { color: #303133; text-decoration: none; font-weight: 500; }
.p-info .name:hover { color: #409eff; }
.meta { margin-top: 4px; color: #909399; font-size: 12px; display: flex; gap: 8px; align-items: center; }
.warn { margin-top: 3px; color: #f56c6c; font-size: 12px; }
.price, .subtotal { color: #f56c6c; font-weight: 600; }
.bar {
  position: sticky; bottom: 0;
  background: #fff; border: 1px solid #ebeef5; border-radius: 8px;
  padding: 12px 16px; display: flex; justify-content: space-between; align-items: center;
}
.bar b { color: #409eff; }
.right { display: flex; align-items: center; gap: 12px; }
.total { color: #f56c6c; font-size: 24px; font-weight: 700; }
</style>

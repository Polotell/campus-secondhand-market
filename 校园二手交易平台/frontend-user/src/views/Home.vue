<template>
  <div class="home">
    <div class="hero">
      <div class="hero-left">
        <h1>🎓 校园二手交易平台</h1>
        <p>人人都是二手掌柜，省钱又环保</p>
      </div>
      <div class="hero-right">
        <el-input v-model="query.keyword" :prefix-icon="Search" size="large"
                  placeholder="搜索你想要的宝贝" clearable
                  @keyup.enter="reload(1)" @clear="reload(1)" />
      </div>
    </div>

    <div v-if="carousels.length" class="banner-wrap">
      <el-carousel height="220px" indicator-position="outside" :interval="5200">
        <el-carousel-item v-for="c in carousels" :key="c.id">
          <div class="banner-item" role="button" tabindex="0" @click="onCarouselClick(c)" @keyup.enter="onCarouselClick(c)">
            <el-image :src="imgUrl(c.imageUrl)" fit="cover" class="banner-img">
              <template #error>
                <div class="banner-fallback"><el-icon :size="48"><PictureRounded /></el-icon></div>
              </template>
            </el-image>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <el-row :gutter="20" class="main">
      <el-col :span="5">
        <el-card shadow="never" class="sidebar">
          <template #header>
            <div class="side-title">
              <el-icon><Collection /></el-icon><span>分类</span>
            </div>
          </template>
          <div class="cat-item" :class="{ active: query.categoryId === null }"
               @click="onPickCategory(null)">全部商品</div>

          <template v-for="p in categories" :key="p.id">
            <div class="cat-parent">{{ p.name }}</div>
            <div v-for="c in p.children" :key="c.id"
                 class="cat-item child"
                 :class="{ active: query.categoryId === c.id }"
                 @click="onPickCategory(c.id)">{{ c.name }}</div>
          </template>
        </el-card>
      </el-col>

      <el-col :span="19">
        <div class="list-toolbar">
          <el-radio-group v-model="query.sort" @change="reload(1)" size="default">
            <el-radio-button value="latest">最新发布</el-radio-button>
            <el-radio-button value="sales_desc">销量优先</el-radio-button>
            <el-radio-button value="price_asc">价格从低到高</el-radio-button>
            <el-radio-button value="price_desc">价格从高到低</el-radio-button>
            <el-radio-button value="rating_desc">好评优先</el-radio-button>
          </el-radio-group>
          <span class="count">共 <b>{{ total }}</b> 件</span>
        </div>

        <div v-loading="loading" class="grid" element-loading-text="加载中...">
          <template v-if="records.length">
            <div v-for="p in records" :key="p.id" class="product-card"
                 @click="goProduct(p.id)">
              <div class="img-box">
                <el-image :src="imgUrl(p.mainImage)"
                          fit="cover" class="img"
                          :lazy="true">
                  <template #error>
                    <div class="img-err"><el-icon :size="40"><PictureRounded /></el-icon></div>
                  </template>
                </el-image>
                <div v-if="p.conditionLevel" class="condition">{{ conditionLabel(p.conditionLevel) }}</div>
              </div>
              <div class="info">
                <div class="name" :title="p.name">{{ p.name }}</div>
                <div class="price">
                  <span class="now">¥{{ p.discountPrice }}</span>
                  <span v-if="p.originalPrice > p.discountPrice" class="old">¥{{ p.originalPrice }}</span>
                </div>
                <div class="meta">
                  <span class="shop">{{ p.shopName || '-' }}</span>
                  <span class="sold">已售 {{ p.salesCount || 0 }}</span>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无商品，去别的分类看看吧" />
        </div>

        <el-pagination class="pager"
                       v-model:current-page="pageNum"
                       v-model:page-size="pageSize"
                       :page-sizes="[12, 20, 40]"
                       :total="total"
                       layout="total, sizes, prev, pager, next, jumper"
                       background
                       @current-change="reload()"
                       @size-change="reload(1)" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Collection, PictureRounded } from '@element-plus/icons-vue'
import { getCategoryTree } from '@/api/category'
import { listProducts } from '@/api/product'
import { listCarousels } from '@/api/home'

const router = useRouter()

const query = reactive({
  categoryId: null,
  keyword: '',
  sort: 'latest'
})
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref([])
const categories = ref([])
const loading = ref(false)
const carousels = ref([])

const apiBase = import.meta.env.VITE_API_BASE || '/api'
function imgUrl(relative) {
  if (!relative) return ''
  if (/^https?:/i.test(relative)) return relative
  return apiBase + relative
}

const CONDITION_MAP = {
  NEW: '全新', NINETY: '9 成新', EIGHTY: '8 成新',
  SEVENTY: '7 成新', OTHER: '其他'
}
function conditionLabel(v) { return CONDITION_MAP[v] || v }

function onCarouselClick(c) {
  const u = (c.linkUrl || '').trim()
  if (!u) return
  if (/^https?:\/\//i.test(u)) window.open(u, '_blank', 'noopener,noreferrer')
  else router.push(u.startsWith('/') ? u : `/${u}`)
}

async function loadCarousels() {
  try {
    carousels.value = await listCarousels() || []
  } catch {
    carousels.value = []
  }
}

async function loadCategories() {
  try { categories.value = await getCategoryTree() } catch {}
}

async function reload(p) {
  if (p) pageNum.value = p
  loading.value = true
  try {
    const res = await listProducts({
      categoryId: query.categoryId || undefined,
      keyword: query.keyword || undefined,
      sort: query.sort,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

function onPickCategory(id) {
  query.categoryId = id
  reload(1)
}

function goProduct(id) {
  router.push(`/product/${id}`).catch(() => {})
}

onMounted(() => {
  loadCarousels()
  loadCategories()
  reload(1)
})
</script>

<style scoped>
.home { padding: 4px; }

.hero {
  background: linear-gradient(135deg, #409EFF 0%, #67C23A 100%);
  color: #fff; padding: 28px 32px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 18px;
}
.hero-left h1 { margin: 0; font-size: 26px; font-weight: 700; }
.hero-left p  { margin: 6px 0 0; opacity: 0.85; font-size: 14px; }
.hero-right   { width: 380px; }
.hero-right :deep(.el-input__wrapper) { border-radius: 999px; }

.banner-wrap {
  margin-bottom: 18px; border-radius: 10px; overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.banner-wrap :deep(.el-carousel__container) { border-radius: 10px; }
.banner-item {
  height: 220px; cursor: pointer; background: #e8ecf1;
}
.banner-img { width: 100%; height: 220px; display: block; }
.banner-fallback {
  width: 100%; height: 220px; display: flex; align-items: center; justify-content: center;
  color: #c0c4cc; background: #f0f2f5;
}

.main { }
.sidebar { }
.side-title { display: flex; align-items: center; gap: 6px; font-weight: 600; }
.cat-parent {
  margin: 10px 0 4px; font-size: 13px; color: #909399;
  font-weight: 600; letter-spacing: 0.5px;
}
.cat-item {
  padding: 6px 10px; border-radius: 4px; cursor: pointer;
  color: #303133; font-size: 14px; line-height: 1.8;
  transition: background .15s, color .15s;
}
.cat-item.child { padding-left: 18px; font-size: 13px; }
.cat-item:hover  { background: #ecf5ff; color: #409EFF; }
.cat-item.active { background: #409EFF; color: #fff; font-weight: 600; }

.list-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 0 12px;
}
.count { color: #909399; font-size: 13px; }
.count b { color: #F56C6C; font-size: 16px; margin: 0 2px; }

.grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  min-height: 300px;
}
.product-card {
  background: #fff; border-radius: 8px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,.04); cursor: pointer;
  transition: transform .2s, box-shadow .2s;
  border: 1px solid #ebeef5;
}
.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(64,158,255,.15);
}
.img-box { position: relative; padding-top: 100%; background: #f5f7fa; }
.img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.img-err {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  background: #f5f7fa; color: #c0c4cc;
}
.condition {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,.6); color: #fff;
  padding: 2px 8px; border-radius: 4px; font-size: 12px;
}
.info { padding: 10px 12px; }
.name {
  font-size: 14px; color: #303133; line-height: 1.4;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  height: 2.8em;
}
.price { margin-top: 8px; }
.now { color: #F56C6C; font-size: 20px; font-weight: 700; }
.old { color: #c0c4cc; font-size: 13px; text-decoration: line-through; margin-left: 6px; }
.meta {
  display: flex; justify-content: space-between;
  font-size: 12px; color: #909399; margin-top: 4px;
}

.pager { margin-top: 18px; display: flex; justify-content: center; }
</style>

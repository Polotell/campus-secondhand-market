<template>
  <div class="editor">
    <el-page-header @back="$router.push('/merchant/products')" class="back">
      <template #content>发布新商品</template>
    </el-page-header>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px"
               v-loading="submitting">
        <el-form-item label="商品图片" prop="images" required>
          <div>
            <multi-image-upload v-model="form.images" :max="8" />
            <div class="form-hint">第一张为主图，1~8 张。单图 ≤ 5MB。</div>
          </div>
        </el-form-item>

        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit
                    placeholder="例如：二手 ThinkPad X1 Carbon 2021" />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-cascader v-model="catPath" :options="catOptions"
                       :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children' }"
                       placeholder="请选择分类（二级）"
                       clearable
                       @change="onCatChange"
                       style="width: 360px" />
        </el-form-item>

        <el-form-item label="原价 (元)" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :min="0.01" :max="9999999.99"
                           :precision="2" :step="10" controls-position="right" />
        </el-form-item>

        <el-form-item label="售价 (元)" prop="discountPrice">
          <el-input-number v-model="form.discountPrice" :min="0.01" :max="9999999.99"
                           :precision="2" :step="10" controls-position="right" />
          <span v-if="discountRate" class="form-hint rate">({{ discountRate }} 折)</span>
        </el-form-item>

        <el-form-item label="成色" prop="conditionLevel">
          <el-radio-group v-model="form.conditionLevel">
            <el-radio-button value="NEW">全新</el-radio-button>
            <el-radio-button value="NINETY">9 成新</el-radio-button>
            <el-radio-button value="EIGHTY">8 成新</el-radio-button>
            <el-radio-button value="SEVENTY">7 成新</el-radio-button>
            <el-radio-button value="OTHER">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="1" :max="9999" :step="1"
                           controls-position="right" />
        </el-form-item>

        <el-form-item label="规格">
          <el-input v-model="form.sizeInfo" placeholder="例如：14 寸 / 39 码 / L 号" maxlength="100" />
        </el-form-item>

        <el-form-item label="支持议价">
          <el-switch v-model="negotiableBool" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="6"
                    maxlength="2000" show-word-limit
                    placeholder="描述你的商品情况，如使用时长、瑕疵、配件等" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :icon="Position" @click="onSubmit">
            提交审核
          </el-button>
          <el-button size="large" @click="$router.push('/merchant/products')">取消</el-button>
        </el-form-item>

        <el-alert type="info" :closable="false" show-icon>
          发布的商品会进入「待审核」队列，管理员审核通过后才会在广场展示。
        </el-alert>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Position } from '@element-plus/icons-vue'
import { getCategoryTree } from '@/api/category'
import { createProduct } from '@/api/product'
import MultiImageUpload from '@/components/MultiImageUpload.vue'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const catOptions = ref([])
const catPath    = ref([])

const form = reactive({
  name: '',
  categoryId: null,
  description: '',
  originalPrice: null,
  discountPrice: null,
  sizeInfo: '',
  conditionLevel: 'NINETY',
  stock: 1,
  negotiable: 0,
  images: []
})
const negotiableBool = computed({
  get: () => form.negotiable === 1,
  set: v => form.negotiable = v ? 1 : 0
})

const discountRate = computed(() => {
  const { originalPrice: o, discountPrice: d } = form
  if (o && d && o > 0 && d <= o) return ((d / o) * 10).toFixed(1)
  return null
})

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 100, message: '最多 100 字' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  originalPrice: [
    { required: true, message: '请填写原价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '必须大于 0' }
  ],
  discountPrice: [
    { required: true, message: '请填写售价', trigger: 'blur' },
    {
      validator(_, v, cb) {
        if (v == null) return cb()
        if (form.originalPrice != null && v > form.originalPrice) {
          return cb(new Error('售价不能大于原价'))
        }
        cb()
      },
      trigger: 'blur'
    }
  ],
  conditionLevel: [{ required: true, message: '请选择成色', trigger: 'change' }],
  stock: [{ required: true, message: '请填写库存', trigger: 'blur' }],
  images: [{
    validator(_, v, cb) {
      if (!v || v.length === 0) return cb(new Error('请至少上传 1 张商品图片'))
      cb()
    },
    trigger: 'change'
  }]
}

function onCatChange(v) {
  // v = [parentId, childId] or []
  form.categoryId = v && v.length ? v[v.length - 1] : null
}

async function loadCategories() {
  const list = await getCategoryTree()
  // cascader 不能有 children=[] 的节点（会显示下拉箭头），过滤成 undefined
  catOptions.value = list.map(p => ({
    ...p,
    children: (p.children || []).length ? p.children : undefined
  }))
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const res = await createProduct({
      name: form.name.trim(),
      categoryId: form.categoryId,
      description: form.description,
      originalPrice: form.originalPrice,
      discountPrice: form.discountPrice,
      sizeInfo: form.sizeInfo,
      conditionLevel: form.conditionLevel,
      stock: form.stock,
      negotiable: form.negotiable,
      images: form.images
    })
    ElMessage.success('发布成功，已提交审核')
    router.push('/merchant/products')
  } finally { submitting.value = false }
}

onMounted(loadCategories)
</script>

<style scoped>
.editor { padding: 4px; max-width: 960px; }
.back { margin-bottom: 14px; }
.form-hint { color: #909399; font-size: 12px; margin-top: 2px; }
.form-hint.rate { color: #F56C6C; margin-left: 10px; font-size: 13px; }
</style>

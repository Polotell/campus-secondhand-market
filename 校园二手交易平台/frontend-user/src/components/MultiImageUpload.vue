<template>
  <div class="multi-upload">
    <div v-for="(url, idx) in list" :key="idx" class="item">
      <img :src="displayUrl(url)" class="preview" />
      <div v-if="idx === 0" class="main-tag">主图</div>
      <div class="mask">
        <el-icon :size="18" class="btn" @click="preview(idx)"><ZoomIn /></el-icon>
        <el-icon v-if="idx > 0" :size="18" class="btn" @click="toMain(idx)"><Star /></el-icon>
        <el-icon :size="18" class="btn" @click="remove(idx)"><Delete /></el-icon>
      </div>
    </div>

    <el-upload v-if="list.length < max"
               class="plus"
               :show-file-list="false"
               :http-request="customUpload"
               :before-upload="beforeUpload"
               accept="image/jpeg,image/png,image/gif,image/webp">
      <div class="placeholder">
        <el-icon :size="26"><Plus /></el-icon>
        <div class="hint">添加图片 ({{ list.length }}/{{ max }})</div>
      </div>
    </el-upload>

    <el-image-viewer v-if="previewIdx !== null"
                     :url-list="list.map(displayUrl)"
                     :initial-index="previewIdx"
                     @close="previewIdx = null" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ZoomIn, Delete, Star } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/file'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 8 },
  maxSizeMB: { type: Number, default: 5 }
})
const emit = defineEmits(['update:modelValue'])

const list = computed(() => props.modelValue || [])
const previewIdx = ref(null)

const apiBase = import.meta.env.VITE_API_BASE || '/api'
const displayUrl = src => /^https?:/i.test(src) ? src : apiBase + src

function beforeUpload(file) {
  if (!/^image\//.test(file.type)) { ElMessage.error('只支持图片文件'); return false }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${props.maxSizeMB} MB`); return false
  }
  return true
}

async function customUpload({ file }) {
  try {
    const { url } = await uploadImage(file)
    emit('update:modelValue', [...list.value, url])
  } catch {}
}

function remove(idx) {
  const next = list.value.slice()
  next.splice(idx, 1)
  emit('update:modelValue', next)
}

function toMain(idx) {
  const next = list.value.slice()
  const [v] = next.splice(idx, 1)
  next.unshift(v)
  emit('update:modelValue', next)
  ElMessage.success('已设为主图')
}

function preview(idx) { previewIdx.value = idx }
</script>

<style scoped>
.multi-upload { display: flex; flex-wrap: wrap; gap: 10px; }
.item, .plus :deep(.el-upload) {
  width: 120px; height: 120px; border-radius: 6px;
  border: 1px dashed #d9d9d9; overflow: hidden; position: relative;
  background: #fafafa;
}
.plus :deep(.el-upload):hover { border-color: #409EFF; }
.placeholder {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #909399;
}
.placeholder .hint { font-size: 12px; margin-top: 4px; }
.item { border-style: solid; border-color: #ebeef5; }
.preview { width: 100%; height: 100%; object-fit: cover; display: block; }
.main-tag {
  position: absolute; top: 6px; left: 6px;
  background: #F56C6C; color: #fff; font-size: 11px;
  padding: 1px 6px; border-radius: 3px;
}
.mask {
  position: absolute; inset: 0; display: flex; align-items: center;
  justify-content: center; gap: 14px;
  background: rgba(0,0,0,.55); color: #fff;
  opacity: 0; transition: opacity .15s;
}
.item:hover .mask { opacity: 1; }
.btn { cursor: pointer; }
.btn:hover { color: #409EFF; }
</style>

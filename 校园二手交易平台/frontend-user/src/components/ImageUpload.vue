<template>
  <el-upload
    class="image-uploader"
    :show-file-list="false"
    :http-request="customUpload"
    :before-upload="beforeUpload"
    accept="image/jpeg,image/png,image/gif,image/webp">
    <img v-if="displayUrl" :src="displayUrl" class="preview" />
    <div v-else class="placeholder">
      <el-icon :size="24"><Plus /></el-icon>
      <div class="hint">点击上传</div>
    </div>
  </el-upload>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  maxSizeMB: { type: Number, default: 5 }
})
const emit = defineEmits(['update:modelValue'])

// 后端返回的是相对路径 /uploads/xxx.jpg，这里补齐前缀 /api 用于 <img> 显示
const displayUrl = computed(() => {
  if (!props.modelValue) return ''
  if (/^https?:/.test(props.modelValue)) return props.modelValue
  return (import.meta.env.VITE_API_BASE || '/api') + props.modelValue
})

function beforeUpload(file) {
  const isImg = /^image\//.test(file.type)
  if (!isImg) { ElMessage.error('只支持图片文件'); return false }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${props.maxSizeMB} MB`)
    return false
  }
  return true
}

async function customUpload({ file }) {
  try {
    const { url } = await uploadImage(file)
    emit('update:modelValue', url)
    ElMessage.success('上传成功')
  } catch (e) {
    // request 拦截器已经弹了 error
  }
}
</script>

<style scoped>
.image-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  width: 160px; height: 110px;
  display: inline-flex; align-items: center; justify-content: center;
  transition: border-color .2s;
  overflow: hidden;
}
.image-uploader :deep(.el-upload:hover) { border-color: #409EFF; }
.preview { width: 100%; height: 100%; object-fit: cover; }
.placeholder {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: #909399;
}
.placeholder .hint { font-size: 12px; margin-top: 4px; }
</style>

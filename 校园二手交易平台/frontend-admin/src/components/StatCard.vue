<template>
  <div class="stat">
    <div class="stat-icon" :style="{ background: color + '22', color }">
      <el-icon :size="28"><component :is="iconMap[icon] || iconMap.User" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-title">{{ title }}</div>
      <div class="stat-value">{{ value }}</div>
    </div>
  </div>
</template>

<script setup>
import { User, Shop, Goods, List } from '@element-plus/icons-vue'

defineProps({
  title: String,
  value: [String, Number],
  color: { type: String, default: '#409EFF' },
  icon: { type: String, default: 'User' }
})

const iconMap = { User, Shop, Goods, List }
</script>

<style scoped>
.stat {
  display: flex; align-items: center; gap: 14px;
  background: #fff; padding: 18px 20px; border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,.05);
  min-height: 100px;
}
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.stat-title { color: #909399; font-size: 13px; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; }
</style>

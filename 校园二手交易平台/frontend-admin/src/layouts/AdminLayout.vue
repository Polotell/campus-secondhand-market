<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Management /></el-icon>
        <span>管理后台</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu"
               background-color="#001529" text-color="#b8c0cc" active-text-color="#409EFF">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-menu-item-group title="用户与商家">
          <el-menu-item index="/audit/users">
            <el-icon><User /></el-icon><span>用户审核</span>
          </el-menu-item>
          <el-menu-item index="/audit/merchants">
            <el-icon><Shop /></el-icon><span>商家审核</span>
          </el-menu-item>
          <el-menu-item index="/users/manage">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="商品与订单">
          <el-menu-item index="/audit/products">
            <el-icon><Goods /></el-icon><span>商品审核</span>
          </el-menu-item>
          <el-menu-item index="/orders" disabled>
            <el-icon><List /></el-icon><span>订单管理（占位）</span>
          </el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="运营与风控">
          <el-menu-item index="/content/carousels">
            <el-icon><Picture /></el-icon><span>首页轮播</span>
          </el-menu-item>
          <el-menu-item index="/risk/blacklist">
            <el-icon><CircleClose /></el-icon><span>平台黑名单</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="bread">{{ $route.meta?.title || '工作台' }}</div>
        <div class="right">
          <el-tag type="danger" size="small" effect="plain">管理员</el-tag>
          <span class="hello">{{ store.user?.realName || store.user?.username }}</span>
          <el-dropdown @command="onCmd">
            <el-avatar :size="32" class="avatar">
              {{ (store.user?.username || 'A').slice(0, 1).toUpperCase() }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Management, DataBoard, User, Shop, Goods, List, Picture, CircleClose
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

async function onCmd(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出管理后台吗？', '提示', { type: 'warning' })
      await store.logout()
      ElMessage.success('已退出')
      router.replace('/login')
    } catch {}
  }
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.aside {
  background: #001529;
  color: #fff;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}
.menu { border-right: 0; }
.menu :deep(.el-menu-item-group__title) { color: #6b7280; padding-left: 20px; }

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}
.bread { font-size: 16px; font-weight: 600; color: #303133; }
.right { display: flex; align-items: center; gap: 12px; }
.right .hello { font-size: 14px; color: #606266; }
.avatar { background: #F56C6C; color: #fff; cursor: pointer; font-weight: 600; }
.main { background: #f0f2f5; padding: 24px; }
</style>

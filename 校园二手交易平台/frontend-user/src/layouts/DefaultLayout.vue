<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="brand" @click="goHome">
        <el-icon :size="22" color="#409EFF"><ShoppingBag /></el-icon>
        <span>校园二手交易平台</span>
      </div>

      <el-menu
        mode="horizontal"
        class="nav"
        :default-active="activeMenu"
        :ellipsis="false"
        @select="onNavSelect"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>商品广场
        </el-menu-item>
        <template v-if="userStore.role === 'USER'">
          <el-menu-item index="/cart">
            <el-icon><ShoppingCart /></el-icon>购物车
          </el-menu-item>
          <el-menu-item index="/orders">
            <el-icon><Tickets /></el-icon>我的订单
          </el-menu-item>
        </template>
        <template v-if="userStore.role === 'MERCHANT'">
          <el-menu-item index="/merchant/products">
            <el-icon><Goods /></el-icon>我的商品
          </el-menu-item>
          <el-menu-item index="/merchant/product/new">
            <el-icon><Plus /></el-icon>发布商品
          </el-menu-item>
          <el-menu-item index="/merchant/orders">
            <el-icon><Tickets /></el-icon>店铺订单
          </el-menu-item>
          <el-menu-item index="/merchant/blacklist">
            <el-icon><CircleClose /></el-icon>买家黑名单
          </el-menu-item>
        </template>
      </el-menu>

      <div class="user-area">
        <template v-if="userStore.isLoggedIn">
          <el-tag :type="roleTagType" size="small" effect="plain">{{ roleLabel }}</el-tag>
          <span class="hello">你好，{{ userStore.user?.realName || userStore.user?.username }}</span>
          <el-dropdown @command="onCmd">
            <el-avatar :size="32" class="avatar">
              {{ (userStore.user?.realName || userStore.user?.username || '?').slice(0, 1) }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="me">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'USER'" command="cart">
                  购物车
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'USER'" command="orders">
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'MERCHANT'" command="my-products">
                  我的商品
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'MERCHANT'" command="merchant-orders">
                  店铺订单
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'MERCHANT'" command="merchant-blacklist">
                  买家黑名单
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text @click="goLogin">登录</el-button>
          <el-button type="primary" @click="goRegister">注册</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ShoppingBag, House, Goods, Plus, ShoppingCart, Tickets, CircleClose } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route  = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => {
  // 商品详情也高亮"商品广场"
  if (route.path.startsWith('/product/')) return '/home'
  if (route.path.startsWith('/checkout')) return '/cart'
  return route.path
})

const roleLabel = computed(() => ({
  USER: '普通用户', MERCHANT: '商家', ADMIN: '管理员'
}[userStore.role] || '访客'))

const roleTagType = computed(() => ({
  USER: 'info', MERCHANT: 'success', ADMIN: 'danger'
}[userStore.role] || ''))

/** 避免 flex 子项 min-width:auto 导致菜单区横向溢出、挡住右侧按钮或下方内容 */
function onNavSelect(index) {
  if (!index) return
  router.push(index).catch(() => {})
}

function goHome() {
  router.push('/home').catch(() => {})
}

function goLogin() {
  router.push('/login').catch(() => {})
}

function goRegister() {
  router.push('/register').catch(() => {})
}

async function onCmd(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.replace('/login').catch(() => {})
    } catch {}
  } else if (cmd === 'me') {
    router.push('/me').catch(() => {})
  } else if (cmd === 'my-products') {
    router.push('/merchant/products').catch(() => {})
  } else if (cmd === 'merchant-orders') {
    router.push('/merchant/orders').catch(() => {})
  } else if (cmd === 'merchant-blacklist') {
    router.push('/merchant/blacklist').catch(() => {})
  } else if (cmd === 'cart') {
    router.push('/cart').catch(() => {})
  } else if (cmd === 'orders') {
    router.push('/orders').catch(() => {})
  }
}
</script>

<style scoped>
.app-layout { min-height: 100vh; background: #f5f7fa; }
.app-header {
  display: flex; align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
  padding: 0 28px; height: 60px;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  font-size: 18px; font-weight: 600; color: #303133;
  cursor: pointer; flex-shrink: 0;
}
.nav {
  flex: 1;
  margin-left: 40px;
  border-bottom: none !important;
  min-width: 0;
}
.nav :deep(.el-menu-item) { display: flex; align-items: center; gap: 4px; }
.user-area { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.user-area .hello { color: #606266; font-size: 14px; }
.user-area .avatar { background: #409EFF; color: #fff; cursor: pointer; font-weight: 600; }
.app-main { padding: 24px; max-width: 1400px; margin: 0 auto; width: 100%; }
</style>

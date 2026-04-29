<template>
  <div class="admin-auth-bg">
    <el-card class="admin-auth-card" shadow="always">
      <h1>管理后台</h1>
      <p class="subtitle">校园二手交易平台 · 仅限管理员登录</p>

      <el-form ref="formRef" :model="form" :rules="rules"
               size="large" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password
                    placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>

        <el-button type="primary" native-type="submit"
                   :loading="loading" style="width:100%" size="large" @click="onSubmit">
          登录
        </el-button>

        <el-alert type="info" :closable="false" style="margin-top:18px"
                  title="测试账号：admin / admin123"
                  description="用户端请访问 http://localhost:5173" />
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route  = useRoute()
const store  = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const res = await store.login({ ...form })
    ElMessage.success(`欢迎，${res.user.realName || res.user.username}`)
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 非 ADMIN 走到这，error message 已经被 store.login 抛出
    if (e.message && !/业务错误/.test(e.message)) ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

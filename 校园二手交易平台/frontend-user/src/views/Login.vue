<template>
  <div class="auth-bg">
    <el-card class="auth-card" shadow="always">
      <h1>校园二手交易平台</h1>
      <p class="subtitle">用户端登录</p>

      <el-form
        ref="formRef" :model="form" :rules="rules"
        size="large" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable :prefix-icon="User" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="请输入密码"
                    type="password" show-password :prefix-icon="Lock" />
        </el-form-item>

        <el-button type="primary" native-type="submit"
                   :loading="loading" style="width:100%;margin-top:6px"
                   size="large" @click="onSubmit">
          登录
        </el-button>

        <div class="auth-actions">
          <router-link to="/register">普通用户注册</router-link>
          <router-link to="/register-merchant">商家入驻</router-link>
        </div>

        <el-divider style="margin: 18px 0 10px">测试账号</el-divider>
        <div class="tips">
          <div>管理员：<b>admin / admin123</b>（登录后会跳到用户首页占位；
            <span style="color:#e6a23c">后台请在 5174 端口登录</span>）</div>
          <div>普通用户：<b>student01 / admin123</b></div>
          <div>商 家：<b>merchant01 / admin123</b></div>
        </div>
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
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const res = await userStore.login({ ...form })
    ElMessage.success(`欢迎回来，${res.user.realName || res.user.username}`)
    const redirect = route.query.redirect || '/home'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tips { font-size: 12px; color: #909399; line-height: 1.9; }
.tips b { color: #409EFF; }
</style>

<template>
  <div class="auth-bg">
    <el-card class="auth-card register-card" shadow="always">
      <h1>普通用户注册</h1>
      <p class="subtitle">注册后需等待管理员审核通过方可登录</p>

      <el-form
        ref="formRef" :model="form" :rules="rules"
        label-width="96px" size="default" @submit.prevent="onSubmit">

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4~20 位，字母/数字/下划线" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6~32 位" />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="可选" />
        </el-form-item>

        <el-form-item label="所在城市" prop="city">
          <el-input v-model="form.city" placeholder="如：南京" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
            <el-radio value="OTHER">保密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="form.bankAccount" placeholder="16 位数字（模拟）" maxlength="16" />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" style="width:180px" />
            <img v-if="captchaImg" :src="captchaImg" class="captcha-img" alt="验证码"
                 title="点击刷新验证码" @click="loadCaptcha" />
            <el-button link @click="loadCaptcha">换一张</el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" native-type="submit"
                     :loading="loading" style="width:100%" @click="onSubmit">
            注册
          </el-button>
        </el-form-item>

        <div class="auth-actions">
          <router-link to="/login">已有账号？去登录</router-link>
          <router-link to="/register-merchant">我要商家入驻</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { captcha, registerUser } from '@/api/auth'

const router  = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaImg = ref('')

const form = reactive({
  captchaKey: '', captchaCode: '',
  username: '', password: '',
  realName: '', phone: '', email: '', city: '', gender: 'MALE', bankAccount: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4~20 位，字母/数字/下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  bankAccount: [
    { required: true, message: '请输入银行账号', trigger: 'blur' },
    { pattern: /^\d{16}$/, message: '银行账号必须为 16 位数字', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  const res = await captcha()
  form.captchaKey = res.captchaKey
  captchaImg.value = res.imageBase64
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await registerUser({ ...form })
    ElMessage.success('注册成功！请等待管理员审核通过')
    router.replace('/login')
  } catch {
    loadCaptcha()   // 失败自动换码
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.register-card { width: 560px; }
.captcha-row { display: flex; align-items: center; gap: 10px; }
.captcha-img {
  height: 38px; width: 130px;
  border-radius: 4px; cursor: pointer;
  border: 1px solid #dcdfe6;
}
</style>

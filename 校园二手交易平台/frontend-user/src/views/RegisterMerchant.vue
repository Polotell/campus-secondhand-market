<template>
  <div class="auth-bg">
    <el-card class="auth-card merchant-card" shadow="always">
      <h1>商家入驻</h1>
      <p class="subtitle">提交后由管理员审核，默认等级 1 级（平台手续费 0.1%）</p>

      <el-form ref="formRef" :model="form" :rules="rules"
               label-width="110px" size="default" @submit.prevent="onSubmit">

        <el-divider content-position="left">账号信息</el-divider>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4~20 位，字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>

        <el-divider content-position="left">联系人信息</el-divider>
        <el-form-item label="法人姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="所在城市" prop="city"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
            <el-radio value="OTHER">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="form.bankAccount" maxlength="16" placeholder="16 位数字（模拟）" />
        </el-form-item>

        <el-divider content-position="left">店铺 & 资质</el-divider>
        <el-form-item label="店铺名称" prop="shopName"><el-input v-model="form.shopName" /></el-form-item>

        <el-form-item label="营业执照" prop="businessLicense">
          <image-upload v-model="form.businessLicense" />
        </el-form-item>
        <el-form-item label="身份证正面" prop="idCardFront">
          <image-upload v-model="form.idCardFront" />
        </el-form-item>
        <el-form-item label="身份证反面" prop="idCardBack">
          <image-upload v-model="form.idCardBack" />
        </el-form-item>

        <el-divider content-position="left">验证码</el-divider>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" style="width:200px" />
            <img v-if="captchaImg" :src="captchaImg" class="captcha-img"
                 title="点击刷新" @click="loadCaptcha" />
            <el-button link @click="loadCaptcha">换一张</el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" native-type="submit"
                     :loading="loading" style="width:100%" @click="onSubmit">
            提交入驻申请
          </el-button>
        </el-form-item>

        <div class="auth-actions">
          <router-link to="/login">已有账号？去登录</router-link>
          <router-link to="/register">普通用户注册入口</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { captcha, registerMerchant } from '@/api/auth'
import ImageUpload from '@/components/ImageUpload.vue'

const router  = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaImg = ref('')

const form = reactive({
  captchaKey: '', captchaCode: '',
  username: '', password: '',
  realName: '', phone: '', email: '', city: '', gender: 'MALE', bankAccount: '',
  shopName: '', businessLicense: '', idCardFront: '', idCardBack: ''
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
  realName: [{ required: true, message: '请输入法人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  bankAccount: [
    { required: true, message: '请输入银行账号', trigger: 'blur' },
    { pattern: /^\d{16}$/, message: '银行账号必须为 16 位数字', trigger: 'blur' }
  ],
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  businessLicense: [{ required: true, message: '请上传营业执照', trigger: 'change' }],
  idCardFront: [{ required: true, message: '请上传身份证正面', trigger: 'change' }],
  idCardBack: [{ required: true, message: '请上传身份证反面', trigger: 'change' }],
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
    await registerMerchant({ ...form })
    ElMessage.success('入驻申请已提交，请等待管理员审核')
    router.replace('/login')
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.merchant-card { width: 640px; max-height: 92vh; overflow: auto; }
.captcha-row { display: flex; align-items: center; gap: 10px; }
.captcha-img { height: 38px; width: 130px; border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6; }
</style>

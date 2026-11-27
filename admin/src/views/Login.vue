<template>
  <div class="login-container">
    <el-card class="login-card" shadow="never">
      <div class="login-header">
        <el-icon class="login-icon"><Music /></el-icon>
        <h1>Rap Zone Admin</h1>
        <p>تسجيل الدخول إلى لوحة التحكم</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
      >
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            type="email"
            placeholder="البريد الإلكتروني"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="كلمة المرور"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            block
            :loading="loading"
            @click="handleLogin"
            style="height: 44px; font-size: 16px;"
          >
            تسجيل الدخول
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>Rap Zone Music Platform © 2026</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElButton, ElCard, ElMessage } from 'element-plus'
import { Music, User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/services/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  email: 'admin@rapzone.app',
  password: 'Admin@123',
})

const rules = {
  email: [
    { required: true, message: 'الرجاء إدخال البريد الإلكتروني', trigger: 'blur' },
    { type: 'email', message: 'البريد الإلكتروني غير صحيح', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'الرجاء إدخال كلمة المرور', trigger: 'blur' },
    { min: 6, message: 'كلمة المرور يجب أن تكون 6 أحرف على الأقل', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const response = await authApi.login(form.email, form.password)
      const { accessToken, user } = response.data
      
      localStorage.setItem('admin_token', accessToken)
      localStorage.setItem('admin_role', user.role)
      localStorage.setItem('admin_email', user.email)
      
      ElMessage.success('مرحباً بك في لوحة التحكم')
      router.push('/dashboard')
    } catch (error) {
      const message = error.response?.data?.message || 'فشل تسجيل الدخول'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #000000 0%, #121212 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background-color: #121212;
  border: 1px solid #333333;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  padding: 24px 0 16px;
}

.login-icon {
  font-size: 48px;
  color: #FF0000;
  margin-bottom: 16px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #FFFFFF;
}

.login-header p {
  margin: 0;
  color: #B0B0B0;
  font-size: 14px;
}

.login-form {
  padding: 0 24px 16px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__inner) {
  background-color: #1E1E1E;
  border: 1px solid #333333;
  color: #FFFFFF;
}

.login-form :deep(.el-input__inner:focus) {
  border-color: #FF0000;
  box-shadow: 0 0 0 2px rgba(255, 0, 0, 0.2);
}

.login-form :deep(.el-input__prefix) {
  color: #B0B0B0;
}

.login-form :deep(.el-input__suffix) {
  color: #B0B0B0;
}

.login-footer {
  text-align: center;
  padding: 16px 24px 24px;
  border-top: 1px solid #333333;
  margin: 0 24px;
}

.login-footer p {
  margin: 0;
  color: #666666;
  font-size: 12px;
}
</style>
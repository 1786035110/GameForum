<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度应为6-30个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
}

const submitForm = async (formEl) => {
  console.log('开始提交表单', formEl)
  if (!formEl) {
    console.log('表单实例不存在')
    return
  }
  
  try {
    await formEl.validate(async (valid) => {
      console.log('表单验证结果:', valid)
      if (valid) {
        loading.value = true
        try {
          console.log('准备发送注册请求:', {
            username: form.username,
            password: form.password
          })
          
          const response = await apiClient.post('/user/register', {
            username: form.username,
            password: form.password
          })

          console.log('注册请求响应:', response)
          
          // 检查业务状态码
          if (response.data.code === 1 || response.data.success === true) {
            // 注册成功
            ElMessage.success('注册成功！正在跳转到登录页面...')
            setTimeout(() => {
              router.push('/login')
            }, 1500)
          } else {
            // 业务逻辑错误
            const errorMsg = response.data.msg || response.data.message || '注册失败'
            ElMessage.error(errorMsg)
          }
        } catch (error) {
          console.error('注册错误:', error)
          let errorMsg = '注册失败'
          if (error.response && error.response.data) {
            errorMsg = error.response.data.msg || error.response.data.message || errorMsg
          }
          ElMessage.error(errorMsg)
        } finally {
          loading.value = false
        }
      } else {
        console.log('表单验证失败')
      }
    })
  } catch (error) {
    console.error('表单验证发生错误:', error)
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="register-container">
    <div class="register-background"></div>
    <el-card class="register-card">
      <template #header>
        <div class="register-header">
          <h2 class="register-title">加入我们</h2>
          <p class="register-subtitle">创建您的新账号</p>
        </div>
      </template>
      
      <el-form 
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="new-password"
            show-password
            size="large"
            prefix-icon="Lock"
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            show-password
            size="large"
            prefix-icon="Lock"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            native-type="submit" 
            @click="submitForm(formRef)"
            :loading="loading"
            class="submit-btn"
            size="large"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-link">
        已有账号？ <el-button type="text" @click="goToLogin" class="link-btn">立即登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* 与LoginView相似的样式，但使用register-前缀 */
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: var(--gradient-bg);
  position: relative;
  transition: background 0.3s ease;
}

.register-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  opacity: 0.5;
}

.register-card {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
  border-radius: 20px;
  box-shadow: 0 20px 60px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
}

.register-title {
  margin: 0 0 8px 0;
  color: var(--text-color);
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
}

.submit-btn {
  width: 100%;
  margin-top: 20px;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}
</style>
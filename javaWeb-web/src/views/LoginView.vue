<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'

const store = useStore()
const router = useRouter()
const loading = ref(false)

const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ]
}

const handleLoginSuccess = () => {
  // 登录成功后检查是否有保存的帖子重定向
  const redirectPostId = localStorage.getItem('redirectPostId')
  
  if (redirectPostId) {
    localStorage.removeItem('redirectPostId') // 清除保存的ID
    router.push(`/forum/post/${redirectPostId}`) // 重定向到原帖子
  } else {
    router.push('/') // 默认重定向到首页
  }
}

const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await apiClient.post('/user/login', {
          username: form.username,
          password: form.password,
          rememberMe: form.rememberMe
        })
        
        // 登录成功部分
        if (response.data.code === 1 || response.data.success === true) {
          const userData = response.data.data || response.data
          console.log('登录成功，用户数据:', userData)
          
          // 确保token正确保存
          if (userData.token) {
            localStorage.setItem('token', userData.token)
            console.log('保存token:', userData.token)
          } else {
            console.error('登录响应中缺少token!')
          }
          
          // 保存用户信息
          const userInfo = {
            userID: userData.userID || userData.userId || userData.id,
            username: userData.username,
            email: userData.email,
            avatar: userData.avatar
          }
          localStorage.setItem('user', JSON.stringify(userInfo))
          
          // 更新Vuex状态
          store.commit('login', {
            token: userData.token,
            userInfo: userInfo
          })
          
          ElMessage.success('登录成功')
          
          // 检查是否有重定向路径
          const redirectPath = sessionStorage.getItem('redirectPath')
          if (redirectPath) {
            sessionStorage.removeItem('redirectPath')
            console.log('重定向到目标页面:', redirectPath)
            router.push(redirectPath)
          } else {
            // 检查是否有保存的帖子重定向
            const redirectPostId = localStorage.getItem('redirectPostId')
            if (redirectPostId) {
              localStorage.removeItem('redirectPostId')
              router.push(`/forum/post/${redirectPostId}`)
            } else {
              router.push('/')
            }
          }
        } else {
          // 业务逻辑错误
          const errorMsg = response.data.msg || response.data.message || '登录失败'
          ElMessage.error(errorMsg)
        }
      } catch (error) {
        console.error('登录请求出错:', error)
        let errorMsg = '登录失败'
        if (error.response && error.response.data) {
          errorMsg = error.response.data.msg || error.response.data.message || errorMsg
        }
        ElMessage.error(errorMsg)
      } finally {
        loading.value = false
      }
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-container">
    <div class="login-background"></div>
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">登录您的账号</p>
        </div>
      </template>
      
      <el-form 
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent
        class="login-form"
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
            autocomplete="current-password"
            show-password
            size="large"
            prefix-icon="Lock"
            @keyup.enter="submitForm(formRef)"
          />
        </el-form-item>
        
        <el-form-item>
          <div class="remember-forgot">
            <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
          </div>
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
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-link">
        还没有账号？ <el-button type="text" @click="goToRegister" class="link-btn">立即注册</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: var(--gradient-bg);
  position: relative;
  transition: background 0.3s ease;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  opacity: 0.5;
}

.login-card {
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

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 25px 80px var(--shadow-color);
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-title {
  margin: 0 0 8px 0;
  color: var(--text-color);
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
}

.login-form {
  padding: 0 10px;
}

.remember-forgot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
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

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(102, 126, 234, 0.4);
}

.register-link {
  margin-top: 30px;
  text-align: center;
  color: var(--text-secondary);
}

.link-btn {
  color: var(--primary-color);
  font-weight: 600;
  padding: 0;
}

.link-btn:hover {
  color: var(--secondary-color);
}

@media (max-width: 480px) {
  .login-card {
    margin: 10px;
    max-width: none;
  }
}
</style>
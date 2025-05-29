<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import apiClient from '@/services/api'

const store = useStore()
const router = useRouter()

// 更强的登录状态检测 - 双重保险
const isLoggedIn = computed(() => {
  const fromStore = store.getters.isLoggedIn
  const fromLocal = localStorage.getItem('token') !== null
  console.log('登录状态检测:', { fromStore, fromLocal })
  return fromStore || fromLocal
})

// 获取当前用户信息
const currentUser = computed(() => {
  return store.getters.currentUser || {}
})

const darkMode = computed(() => store.state.theme.darkMode)

watch(() => isLoggedIn.value, (newVal) => {
  console.log('导航栏登录状态变化:', newVal)
})

onMounted(() => {
  // 初始化主题
  store.dispatch('initializeTheme')
  
  // 初始化用户状态并强制更新Vuex
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
  
  if (token && userInfo && userInfo.userId) {
    console.log('初始化用户状态:', userInfo)
    store.commit('login', {
      token,
      userId: userInfo.userId,
      username: userInfo.username
    })
  } else {
    console.log('无用户状态')
  }
})

const toggleDarkMode = () => {
  store.commit('toggleDarkMode')
}

const handleLogout = async () => {
  console.log('执行登出操作')
  try {
    // 先发送登出请求到后端
    await apiClient.post('/user/logout')
    console.log('成功调用登出接口')
  } catch (error) {
    console.error('登出接口调用失败:', error)
    // 即使后端请求失败，仍继续前端登出流程
  } finally {
    // 无论接口调用成功与否，都清除前端登录状态
    store.dispatch('logoutUser')
    
    // 添加额外确认，确保状态更新生效
    setTimeout(() => {
      if (localStorage.getItem('token')) {
        console.log('强制清除token')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }
      // 导航到首页
      router.push('/')
    }, 100)
  }
}
</script>

<template>
  <header>
    <div class="wrapper">
      <div class="menu-container">
        <!-- 左侧导航菜单 -->
        <el-menu mode="horizontal" router class="left-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/leaderboard">排行榜</el-menu-item>
          <el-menu-item index="/forum">社区</el-menu-item>
        </el-menu>
        
        <!-- 右侧用户区域 -->
        <div class="right-section">
          <!-- 主题切换按钮 -->
          <div class="theme-toggle-container">
            <el-button
              circle
              @click="toggleDarkMode"
              class="theme-toggle"
            >
              <el-icon v-if="darkMode"><Sunny /></el-icon>
              <el-icon v-else><Moon /></el-icon>
            </el-button>
          </div>
          
          <!-- 用户信息显示区域 - 独立显示在外面 -->
          <div v-if="isLoggedIn" class="user-info">
            <el-icon class="user-icon"><User /></el-icon>
            <span class="username">{{ currentUser.username || '用户' }}</span>
          </div>
          
          <!-- 未登录时的按钮 -->
          <div v-if="!isLoggedIn" class="auth-buttons">
            <el-button @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </div>
          
          <!-- 个人中心下拉菜单 - 只在登录时显示 -->
          <el-dropdown v-if="isLoggedIn" class="user-dropdown">
            <el-button circle class="dropdown-trigger">
              <el-icon><Setting /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/friends')">
                  <el-icon><UserFilled /></el-icon>
                  好友列表
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </header>

  <RouterView />
</template>

<style>
@import './assets/theme.css';

#app {
  max-width: 100%;
  margin: 0;
  padding: 0;
  font-weight: normal;
}

header {
  line-height: 1.5;
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.wrapper {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.menu-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.left-menu {
  flex: 1;
  border: none;
  background: transparent;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 主题切换按钮样式 */
.theme-toggle-container {
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.theme-toggle {
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  background: var(--surface-color) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-color) !important;
}

.theme-toggle:hover {
  transform: scale(1.1);
  background: var(--primary-color) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 用户信息样式 - 独立显示 */
.user-info {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  color: var(--text-color);
  background: var(--background-color);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  transition: all 0.3s ease;
  cursor: default;
}

.user-info:hover {
  background: var(--primary-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.user-icon {
  margin-right: 8px;
  font-size: 16px;
}

.username {
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
}

/* 未登录按钮组 */
.auth-buttons {
  display: flex;
  gap: 8px;
}

.auth-buttons .el-button {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
}

/* 用户下拉菜单 */
.user-dropdown {
  margin-left: 4px;
}

.dropdown-trigger {
  background: var(--surface-color) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-color) !important;
  transition: all 0.3s ease;
}

.dropdown-trigger:hover {
  background: var(--primary-color) !important;
  color: white !important;
  transform: scale(1.05);
}

/* 下拉菜单项样式 */
.el-dropdown-menu {
  background: var(--surface-color) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 8px;
  box-shadow: 0 8px 25px var(--shadow-color);
}

.el-dropdown-menu__item {
  color: var(--text-color) !important;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  transition: all 0.3s ease;
}

.el-dropdown-menu__item:hover {
  background: var(--hover-color) !important;
  color: var(--primary-color) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .wrapper {
    padding: 0 10px;
  }
  
  .username {
    display: none;
  }
  
  .user-info {
    padding: 8px 12px;
  }
  
  .auth-buttons .el-button {
    padding: 6px 12px;
    font-size: 14px;
  }
  
  .right-section {
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .right-section {
    gap: 6px;
  }
  
  .auth-buttons {
    gap: 4px;
  }
}
</style>
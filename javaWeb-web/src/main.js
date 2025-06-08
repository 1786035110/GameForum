import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import apiClient from '@/services/api'
import socketService from '@/services/socket'
import './assets/theme.css'

const app = createApp(App)

// 全局注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 配置axios默认值
app.config.globalProperties.$http = apiClient

app.use(router)
app.use(store)
app.use(ElementPlus)

router.beforeEach(async (to, from, next) => {
  console.log(`路由跳转: ${from.path} -> ${to.path}`)
  
  // 检查token和用户信息
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
  
  console.log('路由守卫检查:', {
    token: token ? 'exists' : 'null',
    userInfo: userInfo,
    targetPath: to.path,
    requiresAuth: to.matched.some(record => record.meta.requiresAuth)
  })
  

  if (token && userInfo && (userInfo.userID || userInfo.userId || userInfo.username)) {
    // 用户已登录，同步状态到Vuex
    if (!store.getters.isLoggedIn) {
      console.log('路由变化时同步用户状态到Vuex')
      
      const userData = {
        token,
        userID: userInfo.userID || userInfo.userId || userInfo.id,
        username: userInfo.username
      }
      
      console.log('同步的用户数据:', userData)
      store.commit('login', userData)
      
      // 用户登录后连接WebSocket
      try {
        socketService.connect()
      } catch (error) {
        console.error('WebSocket连接失败:', error)
      }
    }
  } else if (!token && store.getters.isLoggedIn) {
    console.log('token不存在但Vuex中仍显示登录状态，同步为登出状态')
    store.commit('logout')
    
    // 用户登出后断开WebSocket
    try {
      socketService.disconnect()
    } catch (error) {
      console.error('WebSocket断开失败:', error)
    }
  }
  
  // 检查路由是否需要认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  
  if (requiresAuth) {
    if (!token) {
      console.log('需要登录，跳转到登录页面')
      // 保存目标路径，登录后可以重定向
      sessionStorage.setItem('redirectPath', to.fullPath)
      next('/login')
      return
    }
    
    // 检查用户信息是否完整
    if (!userInfo.username) {
      console.error('用户信息不完整，跳转到登录页面')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      store.commit('logout')
      next('/login')
      return
    }
  }
  
  console.log('路由守卫检查通过，继续导航')
  next()
})

// 路由错误处理
router.onError((error) => {
  console.error('路由错误:', error)
})

app.mount('#app')
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

router.beforeEach((to, from, next) => {
  // 检查并同步Vuex状态
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
  
  // 修复属性名统一性问题
  if (token && userInfo && (userInfo.userID || userInfo.userId) && !store.getters.isLoggedIn) {
    console.log('路由变化时同步用户状态')
    
    // 确保使用正确的属性名
    const userData = {
      token,
      userID: userInfo.userID || userInfo.userId || userInfo.id,
      username: userInfo.username
    }
    
    console.log('同步的用户数据:', userData)
    store.commit('login', userData)
    
    // 用户登录后连接WebSocket
    socketService.connect()
  } else if (!token && store.getters.isLoggedIn) {
    console.log('token不存在但Vuex中仍显示登录状态，同步为登出状态')
    store.commit('logout')
    
    // 用户登出后断开WebSocket
    socketService.disconnect()
  }
  
  next()
})

app.mount('#app')
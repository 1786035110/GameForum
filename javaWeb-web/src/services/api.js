import axios from 'axios' 
import { ElMessage } from 'element-plus'
import router from '../router'
import store from '../store' 

// 创建axios实例
const apiClient = axios.create({
  baseURL: '/api', 
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true 
})

apiClient.interceptors.request.use(config => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  if (token) {
    // 添加token到请求头
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
apiClient.interceptors.response.use(response => {
  return response;
}, error => {
  console.log('API请求错误:', error.config.url, error.response?.status);
  
  const isNonCriticalRequest = 
    error.config.url.includes('/forum') ||   // 所有社区相关
    error.config.url.includes('/leaderboard') ||  // 所有排行榜相关
    !error.config.url.includes('/user/') ||  // 非用户专属接口
    error.config.url.includes('/game/');     // 游戏相关接口
  
  
  if (error.response && error.response.status === 401 && !isNonCriticalRequest) {
    // 检查是否已经登录但收到401，才清除token
    if (localStorage.getItem('token')) {
      console.log('收到401错误，清除登录状态');
      localStorage.removeItem('token');
      
      // 通过事件总线通知Vuex更新状态
      store.dispatch('logoutUser');
      
      // 只有在访问需要登录的页面时才跳转
      if (router.currentRoute.value.meta.requiresAuth) {
        router.push('/login');
        ElMessage.error('您的登录已过期，请重新登录');
      }
    }
  }
  return Promise.reject(error);
});

export default apiClient
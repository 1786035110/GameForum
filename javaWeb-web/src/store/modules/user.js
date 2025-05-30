export default {
  state: () => {
    // 确保每次初始化都从localStorage读取最新状态
    const token = localStorage.getItem('token')
    const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
    
    return {
      isLoggedIn: !!token,
      userInfo: userInfo,
      token: token
    }
  },
  
  mutations: {
    login(state, payload) {
      state.isLoggedIn = true
      state.userInfo = payload.userInfo || payload
      state.token = payload.token || localStorage.getItem('token')
      
      // 同步到localStorage
      if (payload.token) {
        localStorage.setItem('token', payload.token)
      }
      if (payload.userInfo) {
        localStorage.setItem('user', JSON.stringify(payload.userInfo))
      } else if (payload.username) {
        localStorage.setItem('user', JSON.stringify(payload))
      }
    },
    
    logout(state) {
      state.isLoggedIn = false
      state.userInfo = {}
      state.token = null
    },
    
    updateUserInfo(state, userInfo) {
      state.userInfo = { ...state.userInfo, ...userInfo }
      // 同步到localStorage
      localStorage.setItem('user', JSON.stringify(state.userInfo))
    },
    
    setToken(state, token) {
      state.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },
    
    // 从localStorage同步状态
    syncFromStorage(state) {
      const token = localStorage.getItem('token')
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      
      state.token = token
      state.isLoggedIn = !!token
      state.userInfo = userInfo
    }
  },
  
  actions: {
    loginUser({ commit }, payload) {
      commit('login', payload)
    },
    
    logoutUser({ commit }) {
      commit('logout')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    
    updateProfile({ commit }, userInfo) {
      commit('updateUserInfo', userInfo)
    },
    
    // 初始化用户状态（从localStorage恢复）
    initializeUser({ commit }) {
      commit('syncFromStorage')
    },
    
    // 检查登录状态
    checkAuthStatus({ commit, state }) {
      const token = localStorage.getItem('token')
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      
      if (token && userInfo.userID) {
        if (!state.isLoggedIn) {
          commit('login', { token, userInfo })
        }
        return true
      } else {
        if (state.isLoggedIn) {
          commit('logout')
        }
        return false
      }
    }
  },
  
  getters: {
    isLoggedIn: state => state.isLoggedIn,
    currentUser: state => state.userInfo,
    userToken: state => state.token,
    userId: state => state.userInfo.userID || state.userInfo.userId,
    username: state => state.userInfo.username,
    userAvatar: state => state.userInfo.avatar,
    userEmail: state => state.userInfo.email
  }
}
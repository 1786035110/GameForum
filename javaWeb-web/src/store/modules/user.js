export default {
  state: () => {
    // 确保每次初始化都从localStorage读取最新状态
    const token = localStorage.getItem('token')
    const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
    
    return {
      isLoggedIn: !!token && !!userInfo.username,
      userInfo: userInfo,
      token: token
    }
  },
  
  mutations: {
    login(state, payload) {
      console.log('用户登录 mutation，payload:', payload)
      
      state.isLoggedIn = true
      
      // 处理不同的数据结构
      if (payload.userInfo) {
        state.userInfo = payload.userInfo
      } else {
        // 直接使用 payload 作为用户信息
        state.userInfo = {
          userID: payload.userID || payload.userId || payload.id,
          username: payload.username,
          email: payload.email,
          avatar: payload.avatar
        }
      }
      
      state.token = payload.token || localStorage.getItem('token')
      
      // 同步到localStorage
      if (state.token) {
        localStorage.setItem('token', state.token)
      }
      if (state.userInfo) {
        localStorage.setItem('user', JSON.stringify(state.userInfo))
      }
      
      console.log('登录状态更新完成:', {
        isLoggedIn: state.isLoggedIn,
        userInfo: state.userInfo,
        token: state.token ? 'exists' : 'null'
      })
    },
    
    logout(state) {
      console.log('用户登出 mutation')
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
      state.isLoggedIn = !!token && !!userInfo.username
      state.userInfo = userInfo
      
      console.log('从localStorage同步状态:', {
        isLoggedIn: state.isLoggedIn,
        userInfo: state.userInfo,
        token: state.token ? 'exists' : 'null'
      })
    }
  },
  
  actions: {
    loginUser({ commit }, payload) {
      console.log('loginUser action:', payload)
      commit('login', payload)
    },
    
    logoutUser({ commit }) {
      console.log('logoutUser action')
      commit('logout')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    
    updateProfile({ commit }, userInfo) {
      commit('updateUserInfo', userInfo)
    },
    
    // 初始化用户状态
    initializeUser({ commit }) {
      commit('syncFromStorage')
    },
    
    // 检查登录状态
    checkAuthStatus({ commit, state }) {
      const token = localStorage.getItem('token')
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      
      console.log('检查认证状态:', {
        token: token ? 'exists' : 'null',
        userInfo: userInfo,
        currentState: state.isLoggedIn
      })
      
      if (token && userInfo.username) {
        if (!state.isLoggedIn) {
          console.log('发现有效的登录信息，更新状态')
          commit('login', { token, userInfo })
        }
        return true
      } else {
        if (state.isLoggedIn) {
          console.log('登录信息无效，清除状态')
          commit('logout')
        }
        return false
      }
    }
  },
  
  getters: {
    isLoggedIn: state => {
      // 双重检查：既检查state，也检查localStorage
      const stateLogin = state.isLoggedIn
      const token = localStorage.getItem('token')
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      const localLogin = !!token && !!userInfo.username
      
      return stateLogin && localLogin
    },
    currentUser: state => state.userInfo,
    userToken: state => state.token,
    userId: state => state.userInfo.userID || state.userInfo.userId,
    username: state => state.userInfo.username,
    userAvatar: state => state.userInfo.avatar,
    userEmail: state => state.userInfo.email
  }
}
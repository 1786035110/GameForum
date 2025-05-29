export default {
    state: () => {
        // 确保每次初始化都从localStorage读取最新状态
        const token = localStorage.getItem('token')
        const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
        
        return {
          isLoggedIn: token !== null,
          userInfo: userInfo
        }
      },
    mutations: {
    login(state, userData) {
        console.log('正在更新用户状态:', userData)
        // 保存到localStorage
        localStorage.setItem('token', userData.token)
        localStorage.setItem('user', JSON.stringify({
            userId: userData.userId,
            username: userData.username
        }))
        
        // 更新状态
        state.isLoggedIn = true
        state.userInfo = {
            userId: userData.userId,
            username: userData.username
        }
        },
      logout(state) {
        // 清除localStorage
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        
        // 更新状态
        state.isLoggedIn = false
        state.userInfo = {}
      }
    },
    actions: {
      loginUser({ commit }, userData) {
        commit('login', userData)
      },
      logoutUser({ commit }) {
        commit('logout')
      }
    },
    getters: {
      isLoggedIn: state => state.isLoggedIn,
      currentUser: state => state.userInfo
    }
  }
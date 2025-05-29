import apiClient from '@/services/api'

export default {
  state: () => ({
    friendsList: [],
    friendRequests: [],
    loading: false
  }),
  
  mutations: {
    setFriendsList(state, friends) {
      state.friendsList = friends
    },
    setFriendRequests(state, requests) {
      state.friendRequests = requests
    },
    setLoading(state, status) {
      state.loading = status
    },
    addFriend(state, friend) {
      state.friendsList.push(friend)
    },
    removeFriend(state, friendId) {
      state.friendsList = state.friendsList.filter(friend => friend.id !== friendId)
    },
    removeRequest(state, requestId) {
      state.friendRequests = state.friendRequests.filter(request => request.id !== requestId)
    }
  },
  
  actions: {
    async fetchFriends({ commit }) {
      commit('setLoading', true)
      try {
        const response = await apiClient.get('/friends')
        console.log('获取好友响应:', response.data)
        
        // 关键修复：正确解析嵌套数据结构
        // 从response.data.data中获取实际的好友列表数组
        if (response.data.code === 1) {
          commit('setFriendsList', response.data.data || [])
        } else {
          console.error('获取好友失败:', response.data.msg)
          commit('setFriendsList', [])
        }
      } catch (error) {
        console.error('获取好友列表出错:', error)
        commit('setFriendsList', [])
      } finally {
        commit('setLoading', false)
      }
    },
    
    async fetchFriendRequests({ commit }) {
      commit('setLoading', true)
      try {
        const response = await apiClient.get('/friends/requests')
        console.log('获取好友请求响应:', response.data)


        if (response.data.code === 1) {
          commit('setFriendRequests', response.data.data || [])
        } else {
          console.error('获取好友失败:', response.data.msg)
          commit('setFriendRequests', [])
        }
      } catch (error) {
        console.error('获取好友请求失败', error)
        commit('setFriendsRequsets', [])
      } finally {
        commit('setLoading', false)
      }
    },
    
    async sendFriendRequest({ commit }, username) {
      try {
        const params = new URLSearchParams();
        params.append('username', username);
        
        const response = await apiClient.post('/friends/request', params, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        });
        
        console.log('发送好友请求响应:', response.data)
        
        // 处理返回的状态码和消息
        if (response.data.code === 1) {
          // 成功发送请求
          return {
            success: true,
            message: response.data.msg || '请求已发送'
          }
        } else {
          // 业务逻辑错误，如用户不存在、已是好友等
          return {
            success: false,
            message: response.data.msg || '发送请求失败'
          }
        }
      } catch (error) {
        console.error('发送好友请求失败', error)
        return {
          success: false,
          message: error.response?.data?.msg || '网络错误，发送失败'
        }
      }
    },
    
    async acceptFriendRequest({ commit }, requestId) {
      try {
        const response = await apiClient.post(`/friends/requests/${requestId}/accept`)
        
        if (response.data.code === 1) {
          // 如果后端返回了新添加的好友数据
          if (response.data.data) {
            commit('addFriend', response.data.data)
          } else {
            // 如果后端没返回数据，刷新好友列表
            this.dispatch('fetchFriends')
          }
          commit('removeRequest', requestId)
          return {
            success: true,
            message: response.data.msg || '已接受好友请求'
          }
        } else {
          return {
            success: false,
            message: response.data.msg || '接受请求失败'
          }
        }
      } catch (error) {
        console.error('接受好友请求失败', error)
        return {
          success: false,
          message: error.response?.data?.msg || '网络错误，操作失败'
        }
      }
    },
    
    async rejectFriendRequest({ commit }, requestId) {
      try {
        await apiClient.post(`/friends/requests/${requestId}/reject`)
        commit('removeRequest', requestId)
        return true
      } catch (error) {
        console.error('拒绝好友请求失败', error)
        return false
      }
    },
    
    async removeFriend({ commit }, friendId) {
      try {
        await apiClient.delete(`/friends/${friendId}`)
        commit('removeFriend', friendId)
        return true
      } catch (error) {
        console.error('删除好友失败', error)
        return false
      }
    }
  },
  
  getters: {
    getFriendsList: (state) => state.friendsList,
    getFriendRequests: (state) => state.friendRequests,
    isFriendsLoading: (state) => state.loading,
    getPendingRequestsCount: (state) => state.friendRequests.length
  }
}
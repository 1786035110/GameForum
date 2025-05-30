import apiClient from '@/services/api'

export default {
  namespaced: true,
  state: () => ({
    friendsList: [],
    friendRequests: [],
    searchResults: []
  }),
  
  mutations: {
    setFriendsList(state, friends) {
      state.friendsList = friends
    },
    
    setFriendRequests(state, requests) {
      state.friendRequests = requests
    },
    
    setSearchResults(state, results) {
      state.searchResults = results
    },
    
    addFriend(state, friend) {
      const exists = state.friendsList.find(f => f.userId === friend.userId)
      if (!exists) {
        state.friendsList.push(friend)
      }
    },
    
    removeFriend(state, userId) {
      state.friendsList = state.friendsList.filter(f => f.userId !== userId)
    },
    
    addFriendRequest(state, request) {
      const exists = state.friendRequests.find(r => r.id === request.id)
      if (!exists) {
        state.friendRequests.push(request)
      }
    },
    
    removeFriendRequest(state, requestId) {
      state.friendRequests = state.friendRequests.filter(r => r.id !== requestId)
    }
  },
  
  actions: {
    async loadFriends({ commit }) {
      try {
        console.log('开始加载真实好友列表...')
        const response = await apiClient.get('/friends')
        
        console.log('后端返回的好友数据:', response.data)
        
        // 根据后端实际返回的数据格式进行转换
        let friends = []
        
        if (Array.isArray(response.data)) {
          // 如果直接返回数组
          friends = response.data.map(friend => ({
            userId: friend.id || friend.userId,  // 适配两种可能的字段名
            username: friend.username,
            avatar: friend.avatar || null
          }))
        } else if (response.data && response.data.data) {

          friends = response.data.data.map(friend => ({
            userId: friend.id || friend.userId,
            username: friend.username,
            avatar: friend.avatar || null
          }))
        } else {
          console.warn('无法解析好友列表数据格式:', response.data)
          friends = []
        }
        
        console.log('转换后的好友列表:', friends)
        commit('setFriendsList', friends)
      } catch (error) {
        console.error('加载好友列表失败:', error)
        
        // 如果API调用失败，使用空数组
        commit('setFriendsList', [])
        throw error
      }
    },
    
    async loadFriendRequests({ commit }) {
      try {
        console.log('开始加载好友请求列表...')
        const response = await apiClient.get('/friends/requests')
        
        console.log('后端返回的好友请求数据:', response.data)
        
        // 根据后端实际返回的数据格式进行转换
        let requests = []
        
        if (Array.isArray(response.data)) {
          requests = response.data.map(request => ({
            id: request.id || request.requestId,
            username: request.username,
            requestTime: request.requestTime || request.createdTime,
            requestId: request.requestId || request.id
          }))
        } else if (response.data && response.data.data) {
          requests = response.data.data.map(request => ({
            id: request.id || request.requestId,
            username: request.username,
            requestTime: request.requestTime || request.createdTime,
            requestId: request.requestId || request.id
          }))
        } else {
          console.warn('无法解析好友请求数据格式:', response.data)
          requests = []
        }
        
        console.log('转换后的好友请求:', requests)
        commit('setFriendRequests', requests)
      } catch (error) {
        console.error('加载好友请求失败:', error)
        commit('setFriendRequests', [])
        throw error
      }
    },
    
    async searchUsers({ commit }, username) {
      try {
        console.log('搜索用户:', username)
        commit('setSearchResults', [])
      } catch (error) {
        console.error('搜索用户失败:', error)
        commit('setSearchResults', [])
      }
    },
    
    async sendFriendRequest({ commit }, username) {
      try {
        console.log('发送好友请求给用户:', username)
        
        // 根据后端接口调整请求格式
        const response = await apiClient.post('/friends/request', null, {
          params: { username }
        })
        
        console.log('好友请求发送成功:', response.data)
        return true
      } catch (error) {
        console.error('发送好友请求失败:', error)
        throw error
      }
    },
    
    async acceptFriendRequest({ commit }, requestId) {
      try {
        console.log('接受好友请求:', requestId)
        
        // 根据后端接口调整请求格式
        const response = await apiClient.post(`/friends/requests/${requestId}/accept`)
        
        console.log('好友请求接受成功:', response.data)
        commit('removeFriendRequest', requestId)
        return true
      } catch (error) {
        console.error('接受好友请求失败:', error)
        throw error
      }
    },
    
    async rejectFriendRequest({ commit }, requestId) {
      try {
        console.log('拒绝好友请求:', requestId)
        
        // 根据后端接口调整请求格式
        const response = await apiClient.post(`/friends/requests/${requestId}/reject`)
        
        console.log('好友请求拒绝成功:', response.data)
        commit('removeFriendRequest', requestId)
        return true
      } catch (error) {
        console.error('拒绝好友请求失败:', error)
        throw error
      }
    },
    
    async deleteFriend({ commit }, userId) {
      try {
        console.log('删除好友:', userId)
        
        // 根据后端接口调整请求格式
        const response = await apiClient.delete(`/friends/${userId}`)
        
        console.log('好友删除成功:', response.data)
        commit('removeFriend', userId)
        return true
      } catch (error) {
        console.error('删除好友失败:', error)
        throw error
      }
    }
  },
  
  getters: {
    getFriendsList: state => state.friendsList,
    getFriendRequests: state => state.friendRequests,
    getSearchResults: state => state.searchResults,
    getFriendById: state => userId => {
      return state.friendsList.find(friend => friend.userId === userId)
    },
    getFriendsCount: state => state.friendsList.length,
    getRequestsCount: state => state.friendRequests.length
  }
}
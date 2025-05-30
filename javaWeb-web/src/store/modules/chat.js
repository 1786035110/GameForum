export default {
  namespaced: true,
  state: () => ({
    connected: false,
    messages: [], // 公共聊天消息
    privateChats: {}, // 私聊对话记录 { userId: { messages: [], unreadCount: 0 } }
    onlineUsers: [],
    currentChatUserId: null, // 当前私聊的用户ID
    connectionStatus: 'disconnected' // 'disconnected', 'connecting', 'connected', 'error'
  }),
  
  mutations: {
    setConnectionStatus(state, status) {
      state.connected = status
      state.connectionStatus = status ? 'connected' : 'disconnected'
    },
    
    setConnectionState(state, status) {
      state.connectionStatus = status
      state.connected = status === 'connected'
    },
    
    addMessage(state, message) {
      // 确保消息有唯一ID
      if (!message.id) {
        message.id = Date.now() + Math.random()
      }
      state.messages.push(message)
      
      // 限制消息数量，避免内存过多占用
      if (state.messages.length > 1000) {
        state.messages = state.messages.slice(-500)
      }
    },
    
    addPrivateMessage(state, message) {
      const { senderId, receiverId } = message
      const currentUserId = JSON.parse(localStorage.getItem('user') || '{}').userID
      
      // 确定对话的用户ID（
      const chatUserId = senderId === currentUserId ? receiverId : senderId
      
      // 初始化私聊对话记录
      if (!state.privateChats[chatUserId]) {
        state.privateChats[chatUserId] = {
          messages: [],
          unreadCount: 0
        }
      }
      
      // 确保消息有唯一ID
      if (!message.id) {
        message.id = Date.now() + Math.random()
      }
      
      // 添加消息
      state.privateChats[chatUserId].messages.push(message)
      
      if (senderId !== currentUserId && state.currentChatUserId !== chatUserId) {
        state.privateChats[chatUserId].unreadCount++
        console.log(`用户 ${chatUserId} 有新消息，未读数: ${state.privateChats[chatUserId].unreadCount}`)
      }
      
      // 限制私聊消息数量
      if (state.privateChats[chatUserId].messages.length > 500) {
        state.privateChats[chatUserId].messages = state.privateChats[chatUserId].messages.slice(-250)
      }
    },
    
    setCurrentChatUser(state, userId) {
      state.currentChatUserId = userId
      // 清除当前用户的未读消息数
      if (state.privateChats[userId]) {
        console.log(`进入与用户 ${userId} 的聊天，清除未读消息数`)
        state.privateChats[userId].unreadCount = 0
      }
    },
    
    clearCurrentChatUser(state) {
      state.currentChatUserId = null
    },
    
    setOnlineUsers(state, users) {
      state.onlineUsers = users || []
    },
    
    updateUserStatus(state, { userId, username, online }) {
      const existingUserIndex = state.onlineUsers.findIndex(user => user.userId === userId)
      
      if (online) {
        if (existingUserIndex === -1) {
          state.onlineUsers.push({ userId, username })
        }
      } else {
        if (existingUserIndex !== -1) {
          state.onlineUsers.splice(existingUserIndex, 1)
        }
      }
    },
    
    markMessagesAsRead(state, userId) {
      if (state.privateChats[userId]) {
        console.log(`标记用户 ${userId} 的消息为已读`)
        state.privateChats[userId].unreadCount = 0
      }
    },
    
    clearMessages(state) {
      state.messages = []
    },
    
    clearPrivateChats(state) {
      state.privateChats = {}
    },
    
    clearAllChatData(state) {
      state.messages = []
      state.privateChats = {}
      state.onlineUsers = []
      state.currentChatUserId = null
    }
  },
  
  actions: {
    async enterPrivateChat({ commit }, userId) {
      console.log(`进入与用户 ${userId} 的私聊`)
      commit('setCurrentChatUser', userId)
      commit('markMessagesAsRead', userId)
    },
    
    leavePrivateChat({ commit }) {
      console.log('离开私聊')
      commit('clearCurrentChatUser')
    },
    
    async connectWebSocket({ commit }) {
      commit('setConnectionState', 'connecting')
    },
    
    async disconnectWebSocket({ commit }) {
      commit('setConnectionState', 'disconnected')
      commit('clearAllChatData')
    },
    
    markAsRead({ commit }, userId) {
      commit('markMessagesAsRead', userId)
    }
  },
  
  getters: {
    isConnected: state => state.connected,
    connectionStatus: state => state.connectionStatus,
    publicMessages: state => state.messages,
    onlineUsers: state => state.onlineUsers,
    currentChatUser: state => state.currentChatUserId,
    
    totalUnreadCount: state => 0,
    
    getPrivateMessages: state => userId => {
      return state.privateChats[userId]?.messages || []
    },
    
    getUnreadCount: state => userId => {
      const count = state.privateChats[userId]?.unreadCount || 0
      console.log(`获取用户 ${userId} 的未读消息数: ${count}`)
      return count
    },
    
    // 获取带有未读消息数的好友列表
    getFriendsWithUnread: (state, getters, rootState) => {
      const friends = rootState.friends?.friendsList || []
      return friends.map(friend => ({
        ...friend,
        unreadCount: getters.getUnreadCount(friend.userId),
        isOnline: state.onlineUsers.some(user => user.userId === friend.userId)
      }))
    },
    
    getOnlineUsersCount: state => state.onlineUsers.length,
    
    hasUnreadMessages: state => {
      return Object.values(state.privateChats).some(chat => chat.unreadCount > 0)
    }
  }
}
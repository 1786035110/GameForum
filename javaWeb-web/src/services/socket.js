import store from '@/store'

class SocketService {
  constructor() {
    this.socket = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
  }

  // 获取当前用户信息的方法
  getCurrentUser() {
    // 方法1: 从 localStorage 获取
    let user = JSON.parse(localStorage.getItem('user') || '{}')
    console.log('从 localStorage 获取的用户信息:', user)
    
    // 方法2: 从 Vuex store 获取
    if ((!user.userID || !user.username) && store) {
      const storeUser = store.getters.currentUser
      console.log('从 Vuex store 获取的用户信息:', storeUser)
      if (storeUser.userID) {
        user = storeUser
      }
    }
    
    // 方法3: 检查不同的属性名
    if (!user.userID && user.userId) {
      user.userID = user.userId
    }
    if (!user.userID && user.id) {
      user.userID = user.id
    }
    
    console.log('最终获取的用户信息:', user)
    return user
  }

  connect() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      console.log('🔄 WebSocket已连接')
      return
    }

    const token = localStorage.getItem('token')
    if (!token) {
      console.error('❌ 没有找到token，无法连接WebSocket')
      return
    }

    try {
      const wsUrl = `ws://localhost:8080/ws/chat?token=${token}`
      console.log('🔄 尝试连接WebSocket...')
      console.log('尝试建立WebSocket连接，token:', token.substring(0, 10) + '...')
      console.log('WebSocket连接URL:', wsUrl)

      this.socket = new WebSocket(wsUrl)

      this.socket.onopen = () => {
        console.log('✅ WebSocket连接成功')
        this.reconnectAttempts = 0
        store.commit('chat/setConnectionStatus', true)
      }

      this.socket.onmessage = (event) => {
        console.log('📨 收到WebSocket消息:', event.data)
        try {
          const data = JSON.parse(event.data)
          this.handleMessage(data)
        } catch (error) {
          console.error('❌ 解析WebSocket消息失败:', error)
        }
      }

      this.socket.onclose = (event) => {
        console.log('❌ WebSocket连接关闭', event)
        store.commit('chat/setConnectionStatus', false)
        this.attemptReconnect()
      }

      this.socket.onerror = (error) => {
        console.error('❌ WebSocket连接错误:', error)
        store.commit('chat/setConnectionStatus', false)
      }

    } catch (error) {
      console.error('❌ 创建WebSocket连接失败:', error)
    }
  }

  disconnect() {
    if (this.socket) {
      console.log('🔌 主动断开WebSocket连接')
      this.socket.close()
      this.socket = null
      store.commit('chat/setConnectionStatus', false)
    }
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`🔄 尝试重连WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        this.connect()
      }, this.reconnectDelay)
    } else {
      console.error('❌ WebSocket重连失败，已达到最大重试次数')
    }
  }

  // 处理接收到的消息
  handleMessage(data) {
    console.log('🔄 处理消息类型:', data.type)
    
    if (!store) {
      console.error('❌ Store 未定义，无法处理消息')
      return
    }

    switch (data.type) {
      case 'welcome':
        console.log('🎉 收到欢迎消息:', data.message)
        break
        
      case 'testResponse':
        console.log('🧪 测试响应:', data.response, '原消息:', data.originalMessage)
        break
        
      case 'newMessage':
        console.log('💬 收到新消息:', data.content, '发送者:', data.senderName)
        try {
          store.commit('chat/addMessage', data)
        } catch (error) {
          console.error('❌ 添加公共消息失败:', error)
        }
        break
        
      case 'privateMessage':
        console.log('📩 收到私聊消息:', data.content, '发送者:', data.senderName, '发送者ID:', data.senderId, '接收者ID:', data.receiverId)
        try {
          store.commit('chat/addPrivateMessage', data)
        } catch (error) {
          console.error('❌ 添加私聊消息失败:', error)
        }
        break
        
      case 'userOnline':
        console.log('👋 用户上线:', data.username)
        try {
          store.commit('chat/updateUserStatus', { ...data, online: true })
        } catch (error) {
          console.error('❌ 更新用户在线状态失败:', error)
        }
        break
        
      case 'userOffline':
        console.log('👋 用户下线:', data.username)
        try {
          store.commit('chat/updateUserStatus', { ...data, online: false })
        } catch (error) {
          console.error('❌ 更新用户离线状态失败:', error)
        }
        break
        
      case 'onlineUsers':
        console.log('👥 在线用户列表:', data.users)
        try {
          store.commit('chat/setOnlineUsers', data.users)
        } catch (error) {
          console.error('❌ 设置在线用户列表失败:', error)
        }
        break
        
      case 'error':
        console.error('❌ 服务器错误:', data.message)
        break
        
      default:
        console.log('❓ 未知消息类型:', data.type, data)
    }
  }

  // 发送公共聊天消息
  sendMessage(content, roomId = 'global') {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket未连接，无法发送消息')
      return false
    }

    const message = {
      type: 'message',
      content: content,
      roomId: roomId
    }

    console.log('📤 发送公共消息:', message)
    this.socket.send(JSON.stringify(message))
    return true
  }

  // 发送私聊消息
  sendPrivateMessage(content, targetUserId) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket未连接，无法发送私聊消息')
      return false
    }

    const currentUser = this.getCurrentUser()
    
    console.log('准备发送私聊消息，当前用户信息:', currentUser)
    
    if (!currentUser.userID || !currentUser.username) {
      console.error('❌ 无法获取当前用户信息，currentUser:', currentUser)
      console.error('❌ localStorage user:', localStorage.getItem('user'))
      console.error('❌ Vuex user:', store ? store.getters.currentUser : 'Store not available')
      return false
    }

    const message = {
      type: 'privateMessage',
      content: content,
      targetUserId: targetUserId,
      senderId: currentUser.userID,
      senderName: currentUser.username
    }

    console.log('📤 发送私聊消息:', message)
    this.socket.send(JSON.stringify(message))

    // 立即在本地添加这条消息到聊天记录中（显示为已发送）
    const localMessage = {
      id: Date.now(),
      type: 'privateMessage',
      senderId: currentUser.userID,
      senderName: currentUser.username,
      receiverId: targetUserId,
      content: content,
      timestamp: new Date().toISOString()
    }

    console.log('📝 本地添加发送的私聊消息:', localMessage)
    
    if (store) {
      try {
        store.commit('chat/addPrivateMessage', localMessage)
      } catch (error) {
        console.error('❌ 本地添加私聊消息失败:', error)
      }
    } else {
      console.error('❌ Store 未定义，无法本地添加私聊消息')
    }

    return true
  }

  // 发送测试消息
  sendTestMessage() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket未连接，无法发送测试消息')
      return false
    }

    const message = {
      type: 'test',
      message: 'Hello WebSocket!'
    }

    console.log('📤 发送测试消息:', message)
    this.socket.send(JSON.stringify(message))
    return true
  }

  get connected() {
    return this.socket && this.socket.readyState === WebSocket.OPEN
  }
}

export default new SocketService()
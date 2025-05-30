<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <el-tabs v-model="activeTab" class="chat-tabs">
        <el-tab-pane label="公共聊天" name="public">
          <div class="public-chat-tab">
            <el-icon><ChatDotRound /></el-icon>
            全局聊天室
          </div>
        </el-tab-pane>
        <el-tab-pane label="好友列表" name="friends">
          <div class="friends-list">
            <div 
              v-for="friend in friendsWithUnread" 
              :key="friend.userId"
              class="friend-item"
              :class="{ active: currentChatUserId === friend.userId }"
              @click="selectFriend(friend)"
            >
              <el-avatar :size="40" :src="friend.avatar">
                {{ friend.username.charAt(0) }}
              </el-avatar>
              <div class="friend-info">
                <div class="friend-name">{{ friend.username }}</div>
                <div class="friend-status">
                  <el-tag :type="isUserOnline(friend.userId) ? 'success' : 'info'" size="small">
                    {{ isUserOnline(friend.userId) ? '在线' : '离线' }}
                  </el-tag>
                </div>
              </div>
              <el-badge 
                v-if="friend.unreadCount > 0" 
                :value="friend.unreadCount" 
                class="unread-badge"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="chat-main">
      <!-- 公共聊天区域 -->
      <div v-if="activeTab === 'public'" class="chat-area">
        <div class="chat-header">
          <h3>全局聊天室</h3>
          <el-tag type="info">{{ onlineUsers.length }} 人在线</el-tag>
        </div>
        
        <div class="messages-container" ref="publicMessagesContainer">
          <div 
            v-for="message in publicMessages" 
            :key="message.id"
            class="message-item"
            :class="{ 'own-message': isOwnMessage(message.senderId) }"
          >
            <div class="message-avatar">
              <el-avatar :size="32">
                {{ message.senderName.charAt(0) }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="sender-name">{{ message.senderName }}</span>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-text">{{ message.content }}</div>
            </div>
          </div>
        </div>

        <div class="message-input">
          <el-input
            v-model="publicMessageInput"
            placeholder="输入消息..."
            @keyup.enter="sendPublicMessage"
            :disabled="!connected"
          >
            <template #append>
              <el-button @click="sendPublicMessage" :disabled="!connected">
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 私聊区域 -->
      <div v-else-if="activeTab === 'friends' && selectedFriend" class="chat-area">
        <div class="chat-header">
          <h3>与 {{ selectedFriend.username }} 的对话</h3>
          <el-tag :type="isUserOnline(selectedFriend.userId) ? 'success' : 'info'">
            {{ isUserOnline(selectedFriend.userId) ? '在线' : '离线' }}
          </el-tag>
        </div>
        
        <div class="messages-container" ref="privateMessagesContainer">
          <div 
            v-for="message in privateMessages" 
            :key="message.id"
            class="message-item"
            :class="{ 'own-message': isOwnMessage(message.senderId) }"
          >
            <div class="message-avatar">
              <el-avatar :size="32">
                {{ message.senderName.charAt(0) }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="sender-name">{{ message.senderName }}</span>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-text">{{ message.content }}</div>
            </div>
          </div>
        </div>

        <div class="message-input">
          <el-input
            v-model="privateMessageInput"
            placeholder="输入私聊消息..."
            @keyup.enter="sendPrivateMessage"
            :disabled="!connected"
          >
            <template #append>
              <el-button @click="sendPrivateMessage" :disabled="!connected">
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 未选择好友时的提示 -->
      <div v-else-if="activeTab === 'friends'" class="no-selection">
        <el-empty description="请选择一个好友开始聊天" />
      </div>
    </div>
  </div>
</template>

<!-- filepath: g:\work\JavaWeb\javaWeb-web\src\views\ChatView.vue -->
<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import socketService from '@/services/socket'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

const store = useStore()
const route = useRoute()

// 响应式数据
const activeTab = ref('public')
const publicMessageInput = ref('')
const privateMessageInput = ref('')
const selectedFriend = ref(null)
const publicMessagesContainer = ref(null)
const privateMessagesContainer = ref(null)

// 计算属性
const connected = computed(() => store.state.chat.connected)
const publicMessages = computed(() => store.state.chat.messages)
const onlineUsers = computed(() => store.state.chat.onlineUsers)
const currentChatUserId = computed(() => store.state.chat.currentChatUserId)

// 修复用户信息获取
const currentUser = computed(() => {
  // 优先从 Vuex 获取
  let user = store.getters.currentUser
  
  // 如果 Vuex 中没有，从 localStorage 获取
  if (!user || !user.userID) {
    const localUser = JSON.parse(localStorage.getItem('user') || '{}')
    console.log('从 localStorage 获取用户信息:', localUser)
    user = localUser
  }
  
  // 兼容不同的属性名
  if (user && !user.userID) {
    if (user.userId) {
      user.userID = user.userId
    } else if (user.id) {
      user.userID = user.id
    }
  }
  
  console.log('ChatView 当前用户信息:', user)
  return user || {}
})

const friendsWithUnread = computed(() => {
  return store.getters['chat/getFriendsWithUnread']
})

const privateMessages = computed(() => {
  if (!selectedFriend.value) return []
  const messages = store.getters['chat/getPrivateMessages'](selectedFriend.value.userId)
  console.log(`获取用户 ${selectedFriend.value.userId} 的私聊消息:`, messages)
  return messages
})

// 方法
const isOwnMessage = (senderId) => {
  const isOwn = senderId === currentUser.value.userID
  console.log(`检查消息是否为自己发送: senderId=${senderId}, currentUserId=${currentUser.value.userID}, isOwn=${isOwn}`)
  return isOwn
}

const isUserOnline = (userId) => {
  return onlineUsers.value.some(user => user.userId === userId)
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return formatDistanceToNow(date, { addSuffix: true, locale: zhCN })
}

const selectFriend = async (friend) => {
  console.log('选择好友进行私聊:', friend)
  selectedFriend.value = friend
  activeTab.value = 'friends'
  
  // 进入私聊，清除未读消息数
  await store.dispatch('chat/enterPrivateChat', friend.userId)
  
  // 滚动到底部
  nextTick(() => {
    scrollToBottom('private')
  })
}

const sendPublicMessage = () => {
  if (!publicMessageInput.value.trim() || !connected.value) return
  
  console.log('发送公共消息:', publicMessageInput.value)
  socketService.sendMessage(publicMessageInput.value)
  publicMessageInput.value = ''
  
  nextTick(() => {
    scrollToBottom('public')
  })
}

const sendPrivateMessage = () => {
  console.log('=== 发送私聊消息检查 ===')
  console.log('消息内容:', privateMessageInput.value)
  console.log('连接状态:', connected.value)
  console.log('选中的好友:', selectedFriend.value)
  console.log('当前用户:', currentUser.value)
  
  if (!privateMessageInput.value.trim()) {
    console.warn('消息内容为空')
    return
  }
  
  if (!connected.value) {
    console.warn('WebSocket未连接')
    return
  }
  
  if (!selectedFriend.value) {
    console.warn('未选择好友')
    return
  }
  
  if (!currentUser.value.userID) {
    console.error('当前用户信息无效:', currentUser.value)
    // 尝试重新同步用户信息
    store.dispatch('checkAuthStatus')
    return
  }
  
  console.log('所有检查通过，准备发送私聊消息')
  console.log('发送私聊消息:', {
    content: privateMessageInput.value,
    targetUserId: selectedFriend.value.userId,
    targetUsername: selectedFriend.value.username,
    currentUser: currentUser.value
  })
  
  const success = socketService.sendPrivateMessage(privateMessageInput.value, selectedFriend.value.userId)
  
  if (success) {
    console.log('私聊消息发送成功')
    privateMessageInput.value = ''
    
    nextTick(() => {
      scrollToBottom('private')
    })
  } else {
    console.error('发送私聊消息失败')
  }
}

const scrollToBottom = (type) => {
  const container = type === 'public' ? publicMessagesContainer.value : privateMessagesContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

// 生命周期
onMounted(async () => {
  
  // 检查用户登录状态
  const isLoggedIn = store.dispatch('checkAuthStatus')
  if (!isLoggedIn) {
    console.error('用户未登录')
    return
  }
  
  console.log('当前用户信息:', currentUser.value)
  
  // 连接WebSocket
  if (!connected.value) {
    socketService.connect()
  }
  
  // 加载好友列表
  await store.dispatch('friends/loadFriends')
  
  // 检查是否从好友列表跳转过来
  const targetUserId = route.query.userId
  if (targetUserId) {
    const friend = friendsWithUnread.value.find(f => f.userId == targetUserId)
    if (friend) {
      console.log('从好友列表跳转，自动选择好友:', friend)
      await selectFriend(friend)
    }
  }
  
  // 滚动到底部
  nextTick(() => {
    scrollToBottom('public')
  })
})

onUnmounted(() => {
  // 离开私聊
  store.dispatch('chat/leavePrivateChat')
})

// 监听消息变化，自动滚动到底部
watch([publicMessages, privateMessages], () => {
  nextTick(() => {
    if (activeTab.value === 'public') {
      scrollToBottom('public')
    } else {
      scrollToBottom('private')
    }
  })
})

// 监听私聊消息变化，用于调试
watch(privateMessages, (newMessages) => {
  console.log('私聊消息列表更新:', newMessages)
}, { deep: true })

// 监听当前用户变化
watch(currentUser, (newUser) => {
  console.log('当前用户信息变化:', newUser)
}, { deep: true })
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 60px);
  background: var(--surface-color);
}

.chat-sidebar {
  width: 300px;
  border-right: 1px solid var(--border-color);
  background: var(--surface-color);
}

.chat-tabs {
  height: 100%;
}

.friends-list {
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color-light);
  transition: background-color 0.2s;
  position: relative;
}

.friend-item:hover {
  background: var(--hover-color);
}

.friend-item.active {
  background: var(--primary-color-light);
}

.friend-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.friend-name {
  font-weight: 500;
  color: var(--text-color);
  margin-bottom: 4px;
}

.unread-badge {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-color);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-color);
}

.message-item {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message-item.own-message {
  flex-direction: row-reverse;
}

.message-item.own-message .message-content {
  align-items: flex-end;
}

.message-item.own-message .message-text {
  background: var(--primary-color);
  color: white;
}

.message-avatar {
  margin: 0 8px;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-header {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--text-color-light);
}

.sender-name {
  font-weight: 500;
  margin-right: 8px;
}

.message-text {
  background: var(--surface-color);
  padding: 8px 12px;
  border-radius: 12px;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-input {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--surface-color);
}

.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.public-chat-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: var(--text-color);
}
</style>
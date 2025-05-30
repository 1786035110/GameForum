<template>
  <div class="friends-container">
    <div v-if="errorMessage" class="error-message">
      <el-alert
        :title="errorMessage"
        type="error"
        show-icon
        @close="errorMessage = ''"
      />
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <h2>好友管理</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="好友列表" name="friends">
          <div class="friends-search">
            <el-input
              v-model="searchUsername"
              placeholder="输入用户名添加好友"
              clearable
              class="search-input"
            >
              <template #append>
                <el-button 
                  @click="sendRequest" 
                  :loading="sendingRequest" 
                  :disabled="!searchUsername.trim()"
                >
                  发送请求
                </el-button>
              </template>
            </el-input>
          </div>
          
          <el-divider>我的好友</el-divider>
          
          <div v-loading="loading">
            <div v-if="friends.length === 0" class="empty-list">
              暂无好友，快去添加吧！
            </div>
            
            <el-table v-else :data="friends" style="width: 100%">
              <el-table-column label="用户名" prop="username"></el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="isUserOnline(scope.row.userId) ? 'success' : 'info'" size="small">
                    {{ isUserOnline(scope.row.userId) ? '在线' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button type="primary" size="small" plain @click="startChat(scope.row)">
                    发消息
                  </el-button>
                  <el-button type="danger" size="small" plain @click="removeFriend(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="好友请求" name="requests">
          <div v-loading="loading">
            <div v-if="friendRequests.length === 0" class="empty-list">
              暂无好友请求
            </div>
            
            <el-card v-else v-for="request in friendRequests" :key="request.id" class="request-card">
              <div class="request-info">
                <span class="request-username">{{ request.username }}</span>
                <span class="request-time">{{ request.requestTime }}</span>
              </div>
              <div class="request-actions">
                <el-button type="primary" size="small" @click="acceptRequest(request.id)">接受</el-button>
                <el-button type="danger" size="small" plain @click="rejectRequest(request.id)">拒绝</el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useStore()
const router = useRouter()
const activeTab = ref('friends')
const searchUsername = ref('')
const sendingRequest = ref(false)
const errorMessage = ref('')
const loading = ref(false)

// 修正计算属性
const friends = computed(() => store.getters['friends/getFriendsList'])
const friendRequests = computed(() => store.getters['friends/getFriendRequests'])
const onlineUsers = computed(() => store.state.chat.onlineUsers)

onMounted(() => {
  loadFriendData()
})

const loadFriendData = async () => {
  loading.value = true
  try {
    await store.dispatch('friends/loadFriends')
    await store.dispatch('friends/loadFriendRequests')
  } catch (error) {
    console.error('加载好友数据失败:', error)
    errorMessage.value = '加载好友数据失败'
  } finally {
    loading.value = false
  }
}

const isUserOnline = (userId) => {
  return onlineUsers.value.some(user => user.userId === userId)
}

const startChat = (friend) => {
  // 跳转到聊天页面并选择该好友
  router.push({
    name: 'chat',
    query: { userId: friend.userId }
  })
}

const sendRequest = async () => {
  if (!searchUsername.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  
  sendingRequest.value = true
  try {
    await store.dispatch('friends/sendFriendRequest', searchUsername.value)
    ElMessage.success('好友请求已发送')
    searchUsername.value = ''
    // 刷新好友请求列表
    await store.dispatch('friends/loadFriendRequests')
  } catch (error) {
    ElMessage.error('发送好友请求失败')
    console.error('发送好友请求失败:', error)
  } finally {
    sendingRequest.value = false
  }
}

const acceptRequest = async (requestId) => {
  try {
    await store.dispatch('friends/acceptFriendRequest', requestId)
    ElMessage.success('已接受好友请求')
    // 刷新数据
    loadFriendData()
  } catch (error) {
    ElMessage.error('接受好友请求失败')
    console.error('接受好友请求失败:', error)
  }
}

const rejectRequest = async (requestId) => {
  try {
    await store.dispatch('friends/rejectFriendRequest', requestId)
    ElMessage.success('已拒绝好友请求')
    // 刷新数据
    await store.dispatch('friends/loadFriendRequests')
  } catch (error) {
    ElMessage.error('拒绝好友请求失败')
    console.error('拒绝好友请求失败:', error)
  }
}

const removeFriend = async (friend) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除好友 ${friend.username} 吗？`,
      '删除好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await store.dispatch('friends/deleteFriend', friend.userId)
    ElMessage.success('好友已删除')
    // 刷新好友列表
    await store.dispatch('friends/loadFriends')
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除好友失败:', e)
      ElMessage.error('删除好友失败')
    }
  }
}
</script>

<style scoped>
.error-message {
  margin-bottom: 16px;
}

.friends-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.friends-container .el-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.friends-search {
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
}

.empty-list {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 16px;
}

.request-card {
  margin-bottom: 16px;
  background: var(--background-color);
  border-radius: 8px;
}

.request-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.request-username {
  font-weight: bold;
  color: var(--text-color);
}

.request-time {
  color: var(--text-secondary);
  font-size: 0.9em;
}

.request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 表格美化 */
.el-table {
  background: var(--background-color);
  color: var(--text-color);
  border-radius: 8px;
  overflow: hidden;
}

.el-table :deep(.el-table__header-wrapper) {
  background: var(--surface-color);
}

.el-table :deep(.el-table__row) {
  background: var(--background-color);
}

.el-table :deep(.el-table__row:hover) {
  background: var(--hover-color);
}
</style>
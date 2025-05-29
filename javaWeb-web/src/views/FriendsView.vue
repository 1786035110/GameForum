<script setup>
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'


const store = useStore()
const activeTab = ref('friends')
const searchUsername = ref('')
const sendingRequest = ref(false)

const friends = computed(() => store.getters.getFriendsList)
const friendRequests = computed(() => store.getters.getFriendRequests)
const loading = computed(() => store.getters.isFriendsLoading)
// 添加一个错误消息变量
const errorMessage = ref('')


onMounted(() => {
  loadFriendData()
})

const loadFriendData = () => {
  store.dispatch('fetchFriends')
  store.dispatch('fetchFriendRequests')
}

const sendRequest = async () => {
  if (!searchUsername.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  
  sendingRequest.value = true
  try {
    const result = await store.dispatch('sendFriendRequest', searchUsername.value)
    
    if (result.success) {
      ElMessage.success(result.message)
      searchUsername.value = ''
      // 成功发送请求后刷新好友请求列表
      store.dispatch('fetchFriendRequests')
    } else {
      // 显示具体的错误消息
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('发送请求时出现错误')
  } finally {
    sendingRequest.value = false
  }
}

const acceptRequest = async (requestId) => {
  const result = await store.dispatch('acceptFriendRequest', requestId)
  if (result.success) {
    ElMessage.success(result.message)
    // 刷新好友列表和请求列表
    loadFriendData()
  } else {
    ElMessage.error(result.message)
  }
}

const rejectRequest = async (requestId) => {
  const success = await store.dispatch('rejectFriendRequest', requestId)
  if (success) {
    ElMessage.success('已拒绝好友请求')
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
    
    const success = await store.dispatch('removeFriend', friend.id)
    if (success) {
      ElMessage.success('好友已删除')
    }
  } catch (e) {
    // 用户取消操作
  }
}
</script>

<template>
  <div v-if="errorMessage" class="error-message">
    <el-alert
      :title="errorMessage"
      type="error"
      show-icon
      @close="errorMessage = ''"
    />
  </div>

  <div class="friends-container">
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
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <!-- <el-button type="primary" size="small" plain @click="$router.push(`/chat/${scope.row.id}`)">
                    发消息
                  </el-button> -->
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

<style scoped>
.error-message {
  margin-bottom: 16px;
}

.friends-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  color: #909399;
}

.request-card {
  margin-bottom: 16px;
}

.request-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.request-username {
  font-weight: bold;
}

.request-time {
  color: #909399;
  font-size: 0.9em;
}

.request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
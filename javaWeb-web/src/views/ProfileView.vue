<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils/dateFormat';

const store = useStore()
const router = useRouter()
const userInfo = ref({
  username: '',
  score: 0,
  lastLogin: '',
  gameHistories: []
})

// 添加日期格式化函数
// const formatDate = (dateString) => {
//   if (!dateString) return '';
  
//   // 替换ISO日期中的T并格式化时间
//   return dateString.replace('T', ' ')
//                   .replace(/\.\d+Z$/, '') // 移除可能存在的毫秒和Z
//                   .replace(/Z$/, '');     // 移除可能存在的Z
// }

const loading = ref(true)
const activeTab = ref('profile')

onMounted(async () => {
  try {
    // 其余代码保持不变
    
    // 正常的axios请求
    const response = await apiClient.get('/user/profile')
    console.log('请求成功:', response)
    userInfo.value = response.data.data || response.data
    
    // 如果获取到了用户信息和游戏历史，确保日期格式化
    if (userInfo.value.lastLogin) {
      userInfo.value.lastLogin = formatDate(userInfo.value.lastLogin)
    }
    
    console.log('用户信息:', userInfo.value)
  } catch (error) {
    // 错误处理代码保持不变
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="profile-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="profile-header">
          <h2>个人中心</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人信息" name="profile">
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" disabled></el-input>
            </el-form-item>
            
            
            <el-form-item label="总分数">
              <el-input v-model="userInfo.score" disabled></el-input>
            </el-form-item>
            
            <el-form-item label="上次登录时间">
              <el-input v-model="userInfo.lastLogin" disabled></el-input>
            </el-form-item>
            
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="游戏记录" name="history">
          <el-table :data="userInfo.gameHistories" style="width: 100%">
            <el-table-column prop="date" label="日期" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.date) }}
              </template>
            </el-table-column>
            <el-table-column prop="score" label="分数" width="180"></el-table-column>
            <el-table-column prop="duration" label="持续时间">
              <template #default="scope">
                {{ scope.row.duration }} 秒
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.profile-container .el-card {
  border-radius: 20px;
  box-shadow: 0 15px 40px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(15px);
  transition: all 0.3s ease;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
}

.profile-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 28px;
  font-weight: 600;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.el-form-item {
  margin-bottom: 24px;
}

.el-table {
  background: var(--background-color);
  color: var(--text-color);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px var(--shadow-color);
}
</style>
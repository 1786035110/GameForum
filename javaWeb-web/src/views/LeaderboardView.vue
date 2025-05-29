<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'

const leaderboardData = ref([])
const loading = ref(true)
const timeRange = ref('all')

const fetchLeaderboard = async () => {
  loading.value = true
  try {
    const response = await apiClient.get(`/leaderboard?timeRange=${timeRange.value}`)
    
    console.log('排行榜响应:', response.data)
    
    // 正确提取嵌套的数据数组
    if (response.data && response.data.code === 1) {
      leaderboardData.value = response.data.data || []
    } else {
      leaderboardData.value = []
      ElMessage.error(response.data?.msg || '获取排行榜数据格式错误')
    }
  } catch (error) {
    ElMessage.error('获取排行榜数据失败')
    console.error(error)
    leaderboardData.value = []
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  fetchLeaderboard()
})

const handleTimeRangeChange = () => {
  fetchLeaderboard()
}
</script>

<template>
  <div class="leaderboard-container">
    <el-card>
      <template #header>
        <div class="leaderboard-header">
          <h2>游戏排行榜</h2>
          <el-select v-model="timeRange" @change="handleTimeRangeChange">
            <el-option label="所有时间" value="all" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
          </el-select>
        </div>
      </template>
      
      <el-table :data="leaderboardData" style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="score" label="分数" sortable />
        <el-table-column prop="date" label="日期" sortable />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.leaderboard-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.leaderboard-container .el-card {
  border-radius: 20px;
  box-shadow: 0 15px 40px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(15px);
}

.leaderboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
}

.leaderboard-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 28px;
  font-weight: 600;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.el-table {
  background: var(--background-color);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px var(--shadow-color);
}

/* 排名徽章样式 */
.el-table :deep(.el-table__cell:first-child) {
  font-weight: bold;
}

.el-table :deep(tr:nth-child(1) .el-table__cell:first-child) {
  background: linear-gradient(45deg, #ffd700, #ffed4a);
  color: #8b5a2b;
}

.el-table :deep(tr:nth-child(2) .el-table__cell:first-child) {
  background: linear-gradient(45deg, #c0c0c0, #e8e8e8);
  color: #666;
}

.el-table :deep(tr:nth-child(3) .el-table__cell:first-child) {
  background: linear-gradient(45deg, #cd7f32, #daa520);
  color: #654321;
}
</style>
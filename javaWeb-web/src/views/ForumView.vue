<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '@/services/api'
import { ElMessage, ElTabs, ElTabPane } from 'element-plus'
import { watch } from 'vue'
import { formatDate } from '@/utils/dateFormat';

const router = useRouter()

const activeTab = ref('all')
const categories = ref([])
const posts = ref([])
const friendPosts = ref([])
const loading = ref(true)
const friendsLoading = ref(false)
const currentCategory = ref('') // 修改初始值，不再需要'all'选项
// 分页相关数据
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(async () => {
  await Promise.all([
    fetchCategories(),
    fetchPosts()
  ])
  loading.value = false
})

watch(activeTab, (newTab) => {
  console.log('标签值变化为:', newTab);
  if (newTab === 'friends') {
    console.log('监听到切换到好友标签，获取好友帖子');
    fetchFriendPosts();
  }
});

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/forum/categories')
    console.log('获取分类响应:', response.data)
    
    // 修正：正确解析返回的分类数据
    if (response.data && response.data.code === 1) {
      categories.value = response.data.data || []
    } else {
      ElMessage.error(response.data?.msg || '获取分类失败')
      categories.value = []
    }
  } catch (error) {
    ElMessage.error('获取分类失败')
    console.error(error)
    categories.value = []
  }
}

const fetchPosts = async (categoryId = '') => {
  try {
    loading.value = true
    // 如果没有指定分类ID或为空，获取所有帖子
    const url = !categoryId 
      ? '/forum/posts' 
      : `/forum/categories/${categoryId}/posts`
    
    const response = await apiClient.get(url, {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    
    console.log('获取帖子响应:', response.data)
    
    // 正确处理嵌套数据结构
    if (response.data && response.data.code === 1) {
      posts.value = response.data.data.records || []
      total.value = response.data.data.total || 0
      
    } else {
      ElMessage.error(response.data?.msg || '获取帖子失败')
      posts.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error('获取帖子失败')
    console.error(error)
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const fetchFriendPosts = async () => {
  try {
    friendsLoading.value = true
    const token = localStorage.getItem('token')
    
    if (!token) {
      ElMessage.warning('请先登录以查看好友帖子')
      return
    }
    
    const response = await apiClient.get('/forum/friends/posts', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    // 修复数据结构处理
    if (response.data && response.data.code === 1) {
      friendPosts.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    } else {
      ElMessage.error(response.data?.msg || '获取好友帖子失败')
      friendPosts.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error('获取好友帖子失败')
    console.error(error)
    friendPosts.value = []
  } finally {
    friendsLoading.value = false
  }
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  if (activeTab.value === 'all') {
    // 传递当前选择的分类ID
    fetchPosts(currentCategory.value)
  } else if (activeTab.value === 'friends') {
    fetchFriendPosts()
  }
}

// 处理每页显示数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 重置为第一页
  if (activeTab.value === 'all') {
    // 传递当前选择的分类ID
    fetchPosts(currentCategory.value)
  } else if (activeTab.value === 'friends') {
    fetchFriendPosts()
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
  currentPage.value = 1 // 切换标签时重置页码
  if (tab === 'friends') {
    fetchFriendPosts()
  }
}

const changeCategory = (categoryId) => { // 修改changeCategory函数
  currentCategory.value = categoryId
  currentPage.value = 1 // 切换分类时重置页码
  fetchPosts(categoryId)
}

const goToPostDetail = (postId) => {
  // 修改：检查登录状态，未登录时阻止跳转并提示
  if (!isLoggedIn.value) {
    ElMessage({
      message: '请先登录后才能查看帖子详情',
      type: 'warning',
      duration: 3000,
      showClose: true
    })
    return
  }
  
  console.log('跳转到帖子详情，ID:', postId)
  router.push(`/forum/post/${postId}`)
}

const goToNewPost = () => {
  const token = localStorage.getItem('token')
    
  if (!token) {
    ElMessage.warning('请先登录以发布帖子')
    return
  }

  router.push('/new-post')
}

const goToLogin = () => {
  router.push('/login')
}

// 检查用户是否已登录
const isLoggedIn = computed(() => {
  return localStorage.getItem('token') !== null
})

// 添加获取分类名称的辅助函数
const getCategoryName = (categoryId) => {
  const category = categories.value.find(cat => cat.id.toString() === categoryId)
  return category ? category.name : '未知'
}
</script>

<template>
  <div class="forum-container">
    <el-card>
      <template #header>
        <div class="forum-header">
          <h2>游戏社区</h2>
          <el-button type="primary" @click="goToNewPost">发布帖子</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-click="changeTab">
        <el-tab-pane label="全部帖子" name="all">
          <div class="forum-content" v-loading="loading">
            
            <div class="forum-categories">
              <el-menu 
                :default-active="currentCategory" 
                @select="changeCategory"
                class="category-menu"
              >
                <div class="category-header">
                  <h3>分类</h3>
                  <p class="category-tip">点击分类查看相关帖子</p>
                </div>

                <el-menu-item index="">全部帖子</el-menu-item>
                <el-menu-item 
                  v-for="category in categories" 
                  :key="category.id" 
                  :index="category.id.toString()"
                >
                  {{ category.name }}
                </el-menu-item>
              </el-menu>
            </div>
            
            <div class="forum-posts">
              <!-- 在没有帖子时显示特定分类的提示 -->
              <div v-if="posts.length === 0 && !loading" class="empty-posts">
                <template v-if="currentCategory">
                  暂无「{{ getCategoryName(currentCategory) }}」分类下的帖子，快来发布第一个帖子吧！
                </template>
                <template v-else>
                  暂无帖子，快来发布第一个帖子吧！
                </template>
              </div>
              
              <el-card 
                v-for="post in posts" 
                :key="post.id" 
                class="post-card"
                :class="{ 'login-required-post': !isLoggedIn }"
                @click="goToPostDetail(post.id)"
              >
                <!-- 未登录状态提示遮罩 -->
                <div v-if="!isLoggedIn" class="login-required-overlay">
                  <div class="login-prompt">
                    <el-icon><Lock /></el-icon>
                    <span>登录后查看详情</span>
                  </div>
                </div>

                <div class="post-header">
                  <h3 class="post-title">{{ post.title }}</h3>
                  <div class="post-category">
                    <el-tag size="small">{{ post.categoryName }}</el-tag>
                  </div>
                </div>
                
                <div class="post-info">
                  <span class="post-author">作者: {{ post.authorName }}</span>
                  <span class="post-date">{{ formatDate(post.createTime) }}</span>
                </div>
                
                <div class="post-summary">
                  {{ post.summary || post.content }}
                </div>
                
                <div class="post-stats">
                  <span><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
                  <span><el-icon><Star /></el-icon> {{ post.likeCount || 0 }}</span>
                </div>
              </el-card>

              <!-- 添加分页组件 -->
              <div class="pagination-container" v-if="total > 0">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="total"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="好友动态" name="friends">
          <div class="friends-notice" v-if="!isLoggedIn">
            <el-alert
              title="需要登录"
              description="请先登录以查看好友的帖子动态"
              type="info"
              show-icon
              :closable="false"
            >
              <template #default>
                <div class="login-actions">
                  <el-button type="primary" @click="goToLogin">
                    立即登录
                  </el-button>
                </div>
              </template>
            </el-alert>
          </div>
          
          <div v-else v-loading="friendsLoading">
            <div v-if="friendPosts.length === 0 && !friendsLoading" class="empty-posts">
              暂无好友帖子，快去添加好友吧！
            </div>
            
            <el-card 
              v-for="post in friendPosts" 
              :key="post.id" 
              class="post-card friend-post-card"
              @click="goToPostDetail(post.id)"
            >
              <div class="friend-tag">
                <el-tag type="success" size="small">好友</el-tag>
              </div>

              <div class="post-header">
                <h3 class="post-title">{{ post.title }}</h3>
                <div class="post-category">
                  <el-tag size="small">{{ post.categoryName }}</el-tag>
                </div>
              </div>
              
              <div class="post-info">
                <span class="post-author friend-name">作者: {{ post.authorName }}</span>
                <span class="post-date">{{ formatDate(post.createTime) }}</span>
              </div>
              
              <div class="post-summary">
                {{ post.summary || post.content }}
              </div>
              
              <div class="post-stats">
                <span><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
                <span><el-icon><Star /></el-icon> {{ post.likeCount || 0 }}</span>
              </div>
            </el-card>

            <!-- 好友帖子分页 -->
            <div class="pagination-container" v-if="total > 0">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.forum-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.forum-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.forum-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 28px;
  font-weight: 600;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.forum-header .el-button {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 25px;
  padding: 12px 24px;
  font-weight: 500;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.forum-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

/* 美化卡片 */
.forum-container .el-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
}

.forum-container .el-card .el-card__header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  border-bottom: none;
}

/* 标签页美化 */
.forum-container :deep(.el-tabs__header) {
  margin: 0 0 20px;
  background: white;
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.forum-container :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.forum-container :deep(.el-tabs__item) {
  border-radius: 8px;
  margin: 0 4px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.forum-container :deep(.el-tabs__item.is-active) {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.forum-container :deep(.el-tabs__active-bar) {
  display: none;
}

.forum-content {
  display: flex;
  gap: 20px;
  justify-content: center;
  align-items: flex-start;
}

.forum-categories {
  width: 200px;
  flex-shrink: 0;
}

/* 分类菜单美化 */
.category-menu {
  border-right: none;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.category-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 8px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.category-menu :deep(.el-menu-item:hover) {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateX(4px);
}

.category-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.forum-posts {
  flex-grow: 1;
  max-width: 800px;
  margin: 0 auto;
}

.empty-posts {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
  background: var(--background-color);
  border-radius: 12px;
  margin-bottom: 20px;
}

/* 帖子卡片美化 */
.post-card {
  margin-bottom: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: var(--background-color);
  box-shadow: 0 4px 15px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.post-card:last-child {
  margin-bottom: 40px;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
}

.post-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
}

/* 未登录状态下的帖子卡片样式 */
.login-required-post {
  position: relative;
  overflow: hidden;
}

.login-required-post:hover {
  transform: none;
  box-shadow: 0 4px 15px var(--shadow-color);
}

.login-required-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.login-required-post:hover .login-required-overlay {
  opacity: 1;
}

.login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.login-prompt .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.post-summary {
  color: var(--text-secondary);
  margin: 18px 0;
  padding: 0 24px;
  line-height: 1.7;
  font-size: 15px;
  text-align: justify;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.friend-post-card {
  border-left: 4px solid #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.friend-post-card::before {
  background: linear-gradient(45deg, #67c23a 0%, #85ce61 100%);
}

.friend-tag {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1;
}

.friend-tag .el-tag {
  border-radius: 20px;
  padding: 4px 12px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 24px 24px 0;
}

.post-title {
  color: var(--text-color);
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  margin-right: 16px;
}

.post-category .el-tag {
  border-radius: 20px;
  padding: 4px 12px;
  font-weight: 500;
}

.post-info {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #8492a6;
  padding: 0 24px;
}

.post-author {
  font-weight: 500;
  color: #5a67d8;
}

.friend-name {
  color: #67c23a;
  font-weight: 600;
}

.post-stats {
  display: flex;
  gap: 24px;
  color: #8492a6;
  font-size: 14px;
  padding: 18px 24px 22px;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.3s ease;
}

.post-stats span:hover {
  color: #667eea;
}

.friends-notice {
  margin: 24px 0;
}

.friends-notice :deep(.el-alert) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.2);
}

.login-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.login-actions .el-button {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
}

/* 分页美化 */
.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.pagination-container :deep(.el-pagination) {
  background: white;
  border-radius: 25px;
  padding: 12px 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.pagination-container :deep(.el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
  transition: all 0.3s ease;
}

.pagination-container :deep(.el-pager li.is-active) {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.pagination-container :deep(.btn-prev),
.pagination-container :deep(.btn-next) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.pagination-container :deep(.btn-prev:hover),
.pagination-container :deep(.btn-next:hover) {
  background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.category-header {
  padding: 0 12px 12px;
}

.category-header h3 {
  margin: 0 0 8px;
  color: var(--text-color);
  font-size: 16px;
  font-weight: 600;
}

.category-tip {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .forum-container {
    padding: 16px;
  }
  
  .forum-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .forum-categories {
    width: 100%;
  }
  
  .forum-posts {
    max-width: none;
  }
  
  .category-menu {
    overflow-x: auto;
  }
  
  .category-menu :deep(.el-menu-item) {
    white-space: nowrap;
  }
  
  .post-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .post-stats {
    gap: 16px;
  }
}
</style>
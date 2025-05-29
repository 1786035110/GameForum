<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import apiClient from '@/services/api'
import { ElMessage, ElTabs, ElTabPane } from 'element-plus'
import { watch } from 'vue'
import { formatDate } from '@/utils/dateFormat';

const router = useRouter()
const route = useRoute()
const store = useStore()

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
  // 检查用户是否已登录
  if (isLoggedIn.value) {
    router.push(`/forum/post/${postId}`)
  } else {
    // 保存用户想要访问的帖子ID
    localStorage.setItem('redirectPostId', postId)
    ElMessage({
      message: '登录后才能查看帖子详情',
      type: 'warning',
      duration: 3000,
      showClose: true,
      onClose: () => {
        router.push('/login')
      }
    })
  }
}

const goToNewPost = () => {
  router.push('/forum/new-post')
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
        
        <!-- 在没有帖子时显示特定分类的提示 -->
        <div v-if="posts.length === 0" class="empty-posts">
          <template v-if="currentCategory">
            暂无「{{ getCategoryName(currentCategory) }}」分类下的帖子，快来发布第一个帖子吧！
          </template>
          <template v-else>
            暂无帖子，快来发布第一个帖子吧！
          </template>
        </div>
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
              <div v-if="posts.length === 0" class="empty-posts">
                暂无帖子，快来发布第一个帖子吧！
              </div>
              
              <el-card 
                v-for="post in posts" 
                :key="post.id" 
                class="post-card"
                @click="goToPostDetail(post.id)"
              >
                <div class="post-header">
                  <h3 class="post-title">{{ post.title }}</h3>
                  <div class="post-category">
                    <el-tag size="small">{{ post.categoryName }}</el-tag>
                  </div>
                </div>
                
                <div class="post-info">
                  <span class="post-author">{{ post.authorName }}</span>
                  <span class="post-date">{{ formatDate(post.createTime) }}</span>
                </div>
                
                <div class="post-summary">
                  {{ post.summary }}
                </div>
                
                <div class="post-stats">
                  <span><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
                  <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
                </div>
              </el-card>

              <!-- 添加分页组件 -->
              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  background
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="total"
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
              title="查看好友动态需要先登录"
              type="warning"
              description="请登录后查看您好友的最新帖子"
              show-icon
              :closable="false"
            />
          </div>
          
          <div v-else v-loading="friendsLoading">
            <div v-if="friendPosts.length === 0" class="empty-posts">
              暂无好友帖子，去添加好友或鼓励好友发帖吧！
            </div>
            
            <el-card 
              v-for="post in friendPosts" 
              :key="post.id" 
              class="post-card friend-post-card"
              @click="goToPostDetail(post.id)"
            >
              <div class="friend-tag">
                <el-tag type="success" effect="dark">好友</el-tag>
              </div>
              
              <div class="post-header">
                <h3 class="post-title">{{ post.title }}</h3>
                <div class="post-category">
                  <el-tag size="small">{{ post.categoryName }}</el-tag>
                </div>
              </div>
              
              <div class="post-info">
                <span class="post-author friend-name">{{ post.authorName }}</span>
                <span class="post-date">{{ formatDate(post.createTime) }}</span>
              </div>
              
              <div class="post-summary">
                {{ post.summary }}
              </div>
              
              <div class="post-stats">
                <span><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
                <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
              </div>
            </el-card>
            <!-- 添加分页组件 -->
            <div class="pagination-container" v-if="friendPosts.length > 0">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                background
                layout="total, sizes, prev, pager, next, jumper"
                :total="total"
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
.post-detail-container {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
}

.login-required-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.login-required {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  text-align: center;
}

.login-icon {
  color: var(--primary-color);
  margin-bottom: 24px;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-required h2 {
  font-size: 24px;
  margin-bottom: 16px;
  color: var(--text-color);
}

.login-required p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.login-actions {
  display: flex;
  gap: 16px;
}

/* 美化主按钮 */
.login-actions .el-button--primary {
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-weight: 500;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.login-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

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
  justify-content: center; /* 添加居中对齐 */
  align-items: flex-start;
}

.forum-categories {
  width: 200px; /* 减少分类菜单宽度 */
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
  max-width: 800px; /* 限制帖子区域最大宽度，使其更居中 */
  margin: 0 auto; /* 在可用空间内居中 */
}
.empty-posts {
  background: var(--background-color);
  color: var(--text-secondary);
}

/* 帖子卡片美化 */
.post-card {
  margin-bottom: 30px; /* 从20px增加到30px */
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

.post-summary {
  color: var(--text-secondary);
  margin: 18px 0; /* 上下边距 */
  padding: 0 24px; /* 添加左右内边距，与标题和作者信息对齐 */
  line-height: 1.7; /* 行高 */
  font-size: 15px; /* 设置适当的字体大小 */
  text-align: justify; /* 两端对齐，使文本更整齐 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 限制最多显示两行 */
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

.post-summary {
  color: var(--text-secondary);
  margin: 18px 0; /* 上下边距增加 */
  line-height: 1.7; /* 增加行高，提高可读性 */
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
  box-shadow: 0 4px 15px rgba(230, 162, 60, 0.2);
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

/* 响应式设计 */
@media (max-width: 768px) {
  .forum-container {
    padding: 16px;
  }
  
  .forum-content {
    flex-direction: column;
    gap: 16px;
    justify-content: stretch; /* 移动端拉伸布局 */
  }
  
  .forum-categories {
    width: 100%;
  }
  
  .forum-posts {
    max-width: none; /* 移动端取消最大宽度限制 */
    margin: 0; /* 移动端取消margin */
  }
  
  .category-menu {
    display: flex;
    overflow-x: auto;
    border-radius: 12px;
  }
  
  .category-menu :deep(.el-menu-item) {
    white-space: nowrap;
    margin: 4px;
    min-width: 80px; /* 为移动端菜单项设置最小宽度 */
  }
  
  .post-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .post-stats {
    gap: 16px;
    flex-wrap: wrap;
  }
}

/* 加载动画 */
@keyframes shimmer {
  0% {
    background-position: -468px 0;
  }
  100% {
    background-position: 468px 0;
  }
}

.loading-shimmer {
  animation: shimmer 1.5s ease-in-out infinite;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 1000px 100%;
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

/* 高亮当前选中的分类 */
.current-category {
  font-weight: bold;
  color: var(--primary-color);
}
</style>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/dateFormat';

const route = useRoute()
const postId = route.params.id
const post = ref(null)
const comments = ref([])
const loading = ref(true)
const submittingComment = ref(false)
const commentFormRef = ref(null)
const commentForm = ref({
  content: ''
})
const commentsLoading = ref(false)

onMounted(async () => {
  await fetchPostDetail()
  await fetchComments() 
})

const fetchPostDetail = async () => {
  try {
    loading.value = true
    const response = await apiClient.get(`/forum/posts/${postId}`)
    
    console.log('帖子详情响应:', response.data)
    
    // 正确处理嵌套的数据结构
    if (response.data && response.data.code === 1) {
      post.value = response.data.data
      
      // 删除额外的点赞状态请求，直接使用帖子返回的isLiked值
      
      // 处理可能为null的字段
      if (!post.value.authorName) {
        post.value.authorName = '未知用户'
      }
      
      if (!post.value.categoryName) {
        post.value.categoryName = '未分类'
      }
      
    } else {
      ElMessage.error(response.data?.msg || '获取帖子详情失败')
      router.push('/forum') // 获取失败时返回论坛首页
    }
  } catch (error) {
    ElMessage.error('获取帖子详情失败')
    console.error(error)
    router.push('/forum') // 异常时返回论坛首页
  } finally {
    loading.value = false
  }
}


const submitComment = async () => {
  if (!commentForm.value.content || !commentForm.value.content.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  
  submittingComment.value = true
  try {
    // 创建FormData对象
    const formData = new FormData()
    formData.append('content', commentForm.value.content)
    
    // 发送表单数据
    const response = await apiClient.post(
      `/forum/posts/${postId}/comments`, 
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data', // 显式设置为表单格式
        }
      }
    )
    
    // 处理响应
    if (response.data && response.data.code === 1) {
      // 显示成功消息
      ElMessage({
        message: '评论发表成功！',
        type: 'success',
        duration: 2000
      })
      
      // 重新获取评论列表以显示最新评论
      await fetchComments()
      
      // 清空输入栏
      commentForm.value.content = ''
      
      // 滚动到评论区
      scrollToComments()
    } else {
      // 显示失败消息
      ElMessage.error(response.data?.msg || '评论发表失败')
    }
  } catch (error) {
    console.error('评论发表失败:', error)
    ElMessage.error('评论发表失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submittingComment.value = false
  }
}

const fetchComments = async () => {
  commentsLoading.value = true
  try {
    console.log('获取帖子评论列表...')
    const response = await apiClient.get(`/forum/posts/${postId}/comments`)
    
    console.log('评论列表响应:', response.data)
    
    if (response.data && response.data.code === 1) {
      comments.value = response.data.data || []
    } else {
      console.error('获取评论失败:', response.data?.msg)
      comments.value = []
    }
  } catch (error) {
    console.error('获取评论出错:', error)
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

const scrollToComments = () => {
  setTimeout(() => {
    const commentsHeader = document.querySelector('.comment-section h3:nth-of-type(2)')
    if (commentsHeader) {
      commentsHeader.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 300)
}

const toggleLike = async () => {
  try {
    if (post.value.isLiked) {
      // 如果已点赞，则取消点赞
      const response = await apiClient.post(`/forum/posts/${postId}/like`)
      if (response.data && response.data.code === 1) {
        post.value.likeCount = Math.max(0, post.value.likeCount - 1)
        post.value.isLiked = false
        ElMessage.success('已取消点赞')
      }
    } else {
      // 如果未点赞，则点赞
      const response = await apiClient.post(`/forum/posts/${postId}/like`)
      if (response.data && response.data.code === 1) {
        post.value.likeCount++
        post.value.isLiked = true
        ElMessage.success('点赞成功')
      }
    }
  } catch (error) {
    ElMessage.error(post.value.isLiked ? '取消点赞失败' : '点赞失败')
    console.error('点赞操作失败:', error)
  }
}
</script>

<template>
  <div class="post-detail-container">
    <div v-loading="loading">
      <el-card v-if="post" class="post-card">
        <div class="post-header">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <el-tag size="small">{{ post.categoryName }}</el-tag>
            <span class="post-author">作者: {{ post.authorName }}</span>
            <span class="post-date">发布于: {{ formatDate(post.createTime) }}</span>
            <span class="post-views"><el-icon><View /></el-icon> {{ post.viewCount }}</span>
          </div>
        </div>
        
        <div class="post-content">
          <div v-html="post.content"></div>
        </div>
        
        <div class="post-actions">
          <el-button 
            :type="post.isLiked ? 'primary' : 'default'" 
            @click="toggleLike"
            class="like-button"
          >
            <el-icon><Star :style="{ color: post.isLiked ? '#fff' : '#409eff' }" /></el-icon>
            {{ post.isLiked ? '已点赞' : '点赞' }} ({{ post.likeCount }})
          </el-button>
        </div>
      </el-card>
      
      <div class="comment-section" v-if="post">
        <h3>发表评论</h3>
        <el-form 
          ref="commentFormRef" 
          :model="commentForm" 
          @submit.prevent="submitComment"
          class="comment-form"
        >
          <el-form-item prop="content">
            <el-input
              v-model="commentForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入评论内容"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button 
              type="primary" 
              native-type="submit"
              :loading="submittingComment"
              :disabled="!commentForm.content || !commentForm.content.trim()"
              class="submit-btn"
            >
              发表评论
            </el-button>
          </el-form-item>
        </el-form>
        
        <h3>全部评论 ({{ comments.length }})</h3>
          <div v-loading="commentsLoading" class="comments-list">
            <div v-if="comments.length === 0 && !commentsLoading" class="empty-comments">
              暂无评论，快来发表第一条评论吧！
            </div>
            
            <el-card v-for="comment in comments" :key="comment.id" class="comment-card">
            <div class="comment-header">
              <span class="comment-author">{{ comment.authorName }}</span>
              <span class="comment-date">{{ formatDate(comment.createTime) }}</span>
            </div>
            <div class="comment-content">
              {{ comment.content }}
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.post-card {
  margin-bottom: 20px;
  background: var(--card-bg) !important;
  color: var(--text-color) !important;
  transition: all 0.3s ease;
}

.post-header {
  margin-bottom: 20px;
}

.post-title {
  margin-bottom: 10px;
  color: var(--text-color) !important;
}

.post-meta {
  display: flex;
  gap: 16px;
  color: var(--text-secondary) !important;
  align-items: center;
  flex-wrap: wrap;
}

.post-content {
  margin-bottom: 20px;
  line-height: 1.6;
  color: var(--text-color) !important;
}

.post-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.comment-section {
  margin-top: 30px;
}

.comment-form {
  background: var(--surface-color) !important;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px var(--shadow-color);
  transition: all 0.3s ease;
}

.comment-section h3 {
  margin: 24px 0 16px;
  color: var(--text-color) !important;
  font-size: 18px;
  font-weight: 600;
}

.submit-btn {
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 500;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  float: right;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.empty-comments {
  text-align: center;
  padding: 20px;
  color: var(--text-secondary) !important;
}

.comment-card {
  margin-bottom: 16px;
  background: var(--card-bg) !important;
  color: var(--text-color) !important;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: bold;
  color: var(--text-color) !important;
}

.comment-date {
  color: var(--text-secondary) !important;
  font-size: 0.9em;
}

.comment-content {
  line-height: 1.5;
  color: var(--text-color) !important;
}

.like-button {
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 10px 20px;
}

.like-button.el-button--primary {
  background: linear-gradient(45deg, #ff9a9e 0%, #fad0c4 99%, #fad0c4 100%);
  border-color: transparent;
}

.like-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 154, 158, 0.4);
}

.like-button.el-button--primary:hover {
  background: linear-gradient(45deg, #ff8a8e 0%, #f9c0b4 99%, #f9c0b4 100%);
}
</style>
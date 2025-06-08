<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const postFormRef = ref(null)

const postForm = ref({
  title: '',
  categoryId: '',
  content: '',
  summary: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入帖子内容', trigger: 'blur' },
    { min: 10, message: '内容不能少于 10 个字符', trigger: 'blur' }
  ]
}

onMounted(async () => {
  await fetchCategories()
})

const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await apiClient.get('/forum/categories')

    if (response.data && response.data.code === 1) {
      categories.value = response.data.data || []
      
      if (categories.value.length > 0) {
        console.log('成功加载', categories.value.length, '个分类')
      } else {
        console.warn('没有找到任何分类')
      }
    } else {
      ElMessage.error(response.data?.msg || '获取分类失败')
      categories.value = []
    }
  } catch (error) {
    ElMessage.error('获取分类失败')
    console.error('分类加载错误:', error)
    categories.value = []
  } finally {
    loading.value = false
  }
}

const submitPost = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (!postForm.value.summary) {
          postForm.value.summary = postForm.value.content.length > 15 
            ? postForm.value.content.substring(0, 15) + '...' 
            : postForm.value.content
        }
        
        const response = await apiClient.post('/forum/posts', postForm.value)
        
        console.log('发布帖子响应:', response.data)
        
        if (response.data && response.data.code === 1) {
          ElMessage.success('帖子发布成功')
          const postId = response.data.data.id
          
          if (postId) {
            console.log('跳转到新发布的帖子，ID:', postId)
            router.push(`/forum/post/${postId}`)
          } else {
            console.error('无法获取新发布帖子的ID')
            router.push('/forum')
          }
        } else {
          ElMessage.error(response.data?.msg || '发布失败')
        }
      } catch (error) {
        console.error('发布帖子失败:', error)
        ElMessage.error('发布失败: ' + (error.response?.data?.message || error.message))
      } finally {
        submitting.value = false
      }
    }
  })
}

const cancelPost = () => {
  router.push('/forum')
}
</script>

<template>
  <div class="new-post-container">
    <el-card class="post-card">
      <template #header>
        <div class="post-header">
          <h2>发布新帖</h2>
        </div>
      </template>
      
      <el-form 
        ref="postFormRef" 
        :model="postForm" 
        :rules="rules"
        label-position="top"
        v-loading="loading"
        class="post-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input 
            v-model="postForm.title" 
            placeholder="请输入帖子标题"
            maxlength="100"
            show-word-limit
            clearable
          ></el-input>
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="postForm.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入帖子摘要，不填写将自动从内容中提取"
            maxlength="200"
            show-word-limit
          ></el-input>
          <span class="form-tip">摘要将显示在帖子列表中，让其他用户快速了解帖子内容</span>
        </el-form-item>
        
        <el-form-item label="分类" prop="categoryId">
          <el-select 
            v-model="postForm.categoryId" 
            placeholder="请选择分类" 
            style="width: 100%"
            filterable
          >
            <el-option 
              v-for="category in categories" 
              :key="category.id" 
              :label="category.name" 
              :value="category.id"
            ></el-option>
          </el-select>
          <!-- 添加加载中提示 -->
          <div v-if="loading && categories.length === 0" class="category-loading">
            正在加载分类...
          </div>
          <!-- 添加未找到分类提示 -->
          <div v-else-if="!loading && categories.length === 0" class="category-empty">
            未找到任何分类，请稍后再试
          </div>
        </el-form-item>
        
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入帖子内容"
            show-word-limit
          ></el-input>
        </el-form-item>
        
        <el-form-item class="form-actions">
          <el-button 
            type="primary" 
            @click="submitPost(postFormRef)" 
            :loading="submitting"
            :disabled="loading || categories.length === 0"
          >
            发布帖子
          </el-button>
          <el-button @click="cancelPost">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.new-post-container {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
}

.post-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: none;
  background: var(--card-bg);
  backdrop-filter: blur(10px);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.post-form {
  padding: 20px 15px;
}

.form-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
}

.category-loading, .category-empty {
  padding: 10px 0;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  font-style: italic;
}

.category-empty {
  color: #f56c6c;
}

.form-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 5px;
  display: block;
}

/* 提交按钮美化 */
.el-button{
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-weight: 500;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.el-button--primary {
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  color: #fff;
  border: none;
}

/* 恢复次要按钮样式 */
.el-button--default {
  background: var(--background-color);
  color: var(--text-color);
  border: 1px solid var(--border-color);
}
</style>
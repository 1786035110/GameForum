<script setup>
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '@/services/api'

// 游戏设置
const gameCanvas = ref(null)
const canvasContext = ref(null)
const canvasWidth = 600
const canvasHeight = 400
const gridSize = 20
const gameSpeed = ref(120) // 移动间隔(ms)
const isGameRunning = ref(false)
const score = ref(0)
const highScore = ref(0)
const gameInterval = ref(null)
const gameOver = ref(false)
const showCountdown = ref(false)
const countdown = ref(0.5)
const submittingScore = ref(false)
const gameStartTime = ref(null)  // 游戏开始时间
const gameEndTime = ref(null)    // 游戏结束时间 
const gameDuration = ref(0)      // 游戏持续时间(秒)

// 贪吃蛇设置
const snake = reactive({
  body: [
    { x: 5, y: 1 }, // 蛇头
    { x: 4, y: 1 },
    { x: 3, y: 1 }
  ],
  direction: 'right',
  nextDirection: 'right',
})

// 食物位置
const food = reactive({ x: 10, y: 10 })

// 方向键映射及其对立方向（防止直接反向移动）
const oppositeDirections = {
  'up': 'down',
  'down': 'up',
  'left': 'right',
  'right': 'left'
}

// 初始化游戏
onMounted(() => {
  initGame()
  window.addEventListener('keydown', handleKeyPress)
  loadHighScore()
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyPress)
  if (gameInterval.value) clearInterval(gameInterval.value)
})

// 初始化游戏
const initGame = () => {
  if (!gameCanvas.value) return
  
  canvasContext.value = gameCanvas.value.getContext('2d')
  resetGame()
  drawGame()
}

/// 重置游戏状态
const resetGame = () => {
  snake.body = [
    { x: 5, y: 1 },
    { x: 4, y: 1 },
    { x: 3, y: 1 }
  ]
  snake.direction = 'right'
  snake.nextDirection = 'right'
  generateFood()
  score.value = 0
  gameOver.value = false
  submittingScore.value = false
  gameStartTime.value = null
  gameEndTime.value = null
  gameDuration.value = 0
}

// 开始游戏
const startGame = () => {
  if (isGameRunning.value || gameOver.value) return
  
  // 开始倒计时
  showCountdown.value = true
  countdown.value = 0.5
  
  const countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      showCountdown.value = false
      
      // 倒计时结束后开始游戏
      isGameRunning.value = true
      gameStartTime.value = new Date() // 记录游戏开始时间
      gameInterval.value = setInterval(gameLoop, gameSpeed.value)
    }
  }, 1000)
}



// 暂停游戏
const pauseGame = () => {
  if (!isGameRunning.value) return
  isGameRunning.value = false
  clearInterval(gameInterval.value)
}

// 重新开始游戏
const restartGame = () => {
  pauseGame()
  resetGame()
  drawGame()
}

// 游戏循环
const gameLoop = () => {
  moveSnake()
  checkCollision()
  drawGame()
}

// 移动蛇
const moveSnake = () => {
  // 更新方向（防止一次更新多次方向导致的自我碰撞）
  snake.direction = snake.nextDirection
  
  // 计算蛇头下一个位置
  const head = { ...snake.body[0] }
  
  switch (snake.direction) {
    case 'up':
      head.y -= 1
      break
    case 'down':
      head.y += 1
      break
    case 'left':
      head.x -= 1
      break
    case 'right':
      head.x += 1
      break
  }
  
  // 将新头部添加到身体最前面
  snake.body.unshift(head)
  
  // 检查是否吃到食物
  if (head.x === food.x && head.y === food.y) {
    score.value += 10
    generateFood()
    
    // 增加难度
    if (score.value % 50 === 0 && gameSpeed.value > 50) {
      gameSpeed.value -= 10
      pauseGame()
      gameInterval.value = setInterval(gameLoop, gameSpeed.value)
      isGameRunning.value = true
    }
  } else {
    // 如果没吃到食物，移除尾部
    snake.body.pop()
  }
}

// 生成食物
const generateFood = () => {
  let newFood
  let valid = false
  
  // 确保食物不生成在蛇身上
  while (!valid) {
    newFood = {
      x: Math.floor(Math.random() * (canvasWidth / gridSize)),
      y: Math.floor(Math.random() * (canvasHeight / gridSize))
    }
    
    valid = true
    
    // 检查是否与蛇身重叠
    for (const segment of snake.body) {
      if (segment.x === newFood.x && segment.y === newFood.y) {
        valid = false
        break
      }
    }
  }
  
  food.x = newFood.x
  food.y = newFood.y
}

// 碰撞检测
const checkCollision = () => {
  const head = snake.body[0]
  
  // 检查墙壁碰撞
  if (
    head.x < 0 || 
    head.y < 0 || 
    head.x >= canvasWidth / gridSize || 
    head.y >= canvasHeight / gridSize
  ) {
    handleGameOver()
    return
  }
  
  // 检查自身碰撞 (从第2个身体部分开始检查)
  for (let i = 1; i < snake.body.length; i++) {
    if (head.x === snake.body[i].x && head.y === snake.body[i].y) {
      handleGameOver()
      return
    }
  }
}

// 游戏结束处理
const handleGameOver = () => {
  isGameRunning.value = false
  gameOver.value = true
  clearInterval(gameInterval.value)
  
  // 记录游戏结束时间和持续时间
  gameEndTime.value = new Date()
  if (gameStartTime.value) {
    // 计算游戏持续时间(秒)
    gameDuration.value = Math.floor((gameEndTime.value - gameStartTime.value) / 1000)
  }
  
  if (score.value > highScore.value) {
    highScore.value = score.value
    saveHighScore()
  }
  
  
  // 自动提交分数到排行榜
  if (score.value > 0) {
    submitScore()
  }
}

const drawGrid = (ctx) => {
  ctx.strokeStyle = '#e0e0e0'
  ctx.lineWidth = 0.5
  
  for (let x = 0; x < canvasWidth; x += gridSize) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, canvasHeight)
    ctx.stroke()
  }
  
  for (let y = 0; y < canvasHeight; y += gridSize) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(canvasWidth, y)
    ctx.stroke()
  }
}

// 绘制游戏
const drawGame = () => {
  if (!canvasContext.value) return
  
  const ctx = canvasContext.value
  
  // 清空画布
  ctx.fillStyle = '#f0f0f0'
  ctx.fillRect(0, 0, canvasWidth, canvasHeight)
  
  // 只在游戏初始化时绘制网格，而不是每一帧都绘制
  if (!isGameRunning.value && !gameOver.value && !showCountdown.value) {
    drawGrid(ctx)
  }
  
  for (let x = 0; x < canvasWidth; x += gridSize) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, canvasHeight)
    ctx.stroke()
  }
  
  for (let y = 0; y < canvasHeight; y += gridSize) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(canvasWidth, y)
    ctx.stroke()
  }
  
  // 绘制食物
  ctx.fillStyle = '#e74c3c'
  drawRoundedRect(
    ctx, 
    food.x * gridSize, 
    food.y * gridSize, 
    gridSize, 
    gridSize, 
    8
  )
  
  // 绘制蛇
  snake.body.forEach((segment, index) => {
    // 蛇头用不同颜色
    if (index === 0) {
      ctx.fillStyle = '#2ecc71'
    } else {
      // 身体渐变色
      const greenValue = Math.floor(120 - (index * 3))
      ctx.fillStyle = `rgb(46, ${Math.max(greenValue, 50)}, 113)`
    }
    
    // 绘制圆角矩形
    drawRoundedRect(
      ctx, 
      segment.x * gridSize, 
      segment.y * gridSize, 
      gridSize, 
      gridSize, 
      index === 0 ? 8 : 4  // 蛇头圆角更大
    )
    
    // 绘制蛇眼睛（只在蛇头上）
    if (index === 0) {
      ctx.fillStyle = '#000'
      
      // 根据方向绘制眼睛
      const eyeRadius = 2
      const eyeOffset = 5
      
      let leftEyeX, leftEyeY, rightEyeX, rightEyeY
      
      switch (snake.direction) {
        case 'right':
          leftEyeX = segment.x * gridSize + gridSize - eyeOffset
          leftEyeY = segment.y * gridSize + eyeOffset
          rightEyeX = segment.x * gridSize + gridSize - eyeOffset
          rightEyeY = segment.y * gridSize + gridSize - eyeOffset
          break
        case 'left':
          leftEyeX = segment.x * gridSize + eyeOffset
          leftEyeY = segment.y * gridSize + eyeOffset
          rightEyeX = segment.x * gridSize + eyeOffset
          rightEyeY = segment.y * gridSize + gridSize - eyeOffset
          break
        case 'up':
          leftEyeX = segment.x * gridSize + eyeOffset
          leftEyeY = segment.y * gridSize + eyeOffset
          rightEyeX = segment.x * gridSize + gridSize - eyeOffset
          rightEyeY = segment.y * gridSize + eyeOffset
          break
        case 'down':
          leftEyeX = segment.x * gridSize + eyeOffset
          leftEyeY = segment.y * gridSize + gridSize - eyeOffset
          rightEyeX = segment.x * gridSize + gridSize - eyeOffset
          rightEyeY = segment.y * gridSize + gridSize - eyeOffset
          break
      }
      
      ctx.beginPath()
      ctx.arc(leftEyeX, leftEyeY, eyeRadius, 0, 2 * Math.PI)
      ctx.fill()
      
      ctx.beginPath()
      ctx.arc(rightEyeX, rightEyeY, eyeRadius, 0, 2 * Math.PI)
      ctx.fill()
    }
  })
  
  // 绘制游戏结束提示
  if (gameOver.value) {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)'
    ctx.fillRect(0, 0, canvasWidth, canvasHeight)
    
    ctx.font = '30px Arial'
    ctx.fillStyle = '#fff'
    ctx.textAlign = 'center'
    ctx.fillText('游戏结束!', canvasWidth / 2, canvasHeight / 2 - 30)
    ctx.fillText(`得分: ${score.value}`, canvasWidth / 2, canvasHeight / 2 + 10)
    
    // 显示分数提交状态
    if (submittingScore.value) {
      ctx.font = '18px Arial'
      ctx.fillText('正在提交分数...', canvasWidth / 2, canvasHeight / 2 + 50)
    }
    
    ctx.font = '20px Arial'
    ctx.fillText('点击"重新开始"按钮重玩', canvasWidth / 2, canvasHeight / 2 + 80)
  }
  
  // 绘制倒计时
  if (showCountdown.value) {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)'
    ctx.fillRect(0, 0, canvasWidth, canvasHeight)
    
    ctx.font = '80px Arial'
    ctx.fillStyle = '#fff'
    ctx.textAlign = 'center'
    ctx.fillText(countdown.value.toString(), canvasWidth / 2, canvasHeight / 2 + 20)
  }
}

// 辅助函数：绘制圆角矩形
function drawRoundedRect(ctx, x, y, width, height, radius) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.lineTo(x + width - radius, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
  ctx.lineTo(x + width, y + height - radius)
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
  ctx.lineTo(x + radius, y + height)
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
  ctx.fill()
}

// 键盘控制
const handleKeyPress = (e) => {
  if (gameOver.value) return
  
  const key = e.key.toLowerCase()
  
  // 方向键控制
  switch (key) {
    case 'arrowup':
    case 'w':
      if (snake.direction !== oppositeDirections['up']) {
        snake.nextDirection = 'up'
      }
      break
    case 'arrowdown':
    case 's':
      if (snake.direction !== oppositeDirections['down']) {
        snake.nextDirection = 'down'
      }
      break
    case 'arrowleft':
    case 'a':
      if (snake.direction !== oppositeDirections['left']) {
        snake.nextDirection = 'left'
      }
      break
    case 'arrowright':
    case 'd':
      if (snake.direction !== oppositeDirections['right']) {
        snake.nextDirection = 'right'
      }
      break
    case ' ':
      // 空格键暂停/继续
      isGameRunning.value ? pauseGame() : startGame()
      break
  }
  
  // 防止方向键滚动页面
  if (['arrowup', 'arrowdown', 'arrowleft', 'arrowright', ' '].includes(key)) {
    e.preventDefault()
  }
}

// 触摸控制（手机上使用）
let touchStartX = 0
let touchStartY = 0

const handleTouchStart = (e) => {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}

const handleTouchMove = (e) => {
  if (gameOver.value) return
  
  e.preventDefault()
  
  const touchEndX = e.touches[0].clientX
  const touchEndY = e.touches[0].clientY
  
  const diffX = touchEndX - touchStartX
  const diffY = touchEndY - touchStartY
  
  // 需要有足够的滑动距离才触发方向变化
  if (Math.max(Math.abs(diffX), Math.abs(diffY)) < 20) return
  
  // 判断水平还是垂直方向移动更多
  if (Math.abs(diffX) > Math.abs(diffY)) {
    // 水平移动
    if (diffX > 0 && snake.direction !== oppositeDirections['right']) {
      snake.nextDirection = 'right'
    } else if (diffX < 0 && snake.direction !== oppositeDirections['left']) {
      snake.nextDirection = 'left'
    }
  } else {
    // 垂直移动
    if (diffY > 0 && snake.direction !== oppositeDirections['down']) {
      snake.nextDirection = 'down'
    } else if (diffY < 0 && snake.direction !== oppositeDirections['up']) {
      snake.nextDirection = 'up'
    }
  }
  
  // 更新开始点
  touchStartX = touchEndX
  touchStartY = touchEndY
}

// 加载和保存最高分
const loadHighScore = async () => {
  try {
    // 首先尝试从本地存储读取
    const localHighScore = localStorage.getItem('snakeHighScore')
    if (localHighScore) {
      highScore.value = parseInt(localHighScore)
    }
    
  } catch (error) {
    console.error('无法加载最高分', error)
  }
}

const saveHighScore = async () => {
  try {
    // 保存到本地存储
    localStorage.setItem('snakeHighScore', highScore.value)
    
  } catch (error) {
    console.error('无法保存最高分', error)
  }
}

// 自动提交分数到排行榜
const submitScore = async () => {
  try {
    submittingScore.value = true
    const token = localStorage.getItem('token')
    
    if (token) {
      // 从localStorage中获取用户信息
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      
      await apiClient.post('/leaderboard/submit', {
        score: score.value,
        endTime: gameEndTime.value.toISOString(), // ISO格式的结束时间
        userId: userInfo.userId,                  // 用户ID
        duration: gameDuration.value              // 游戏持续时间(秒)
      }, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      ElMessage.success('分数已自动提交到排行榜')
    } else {
      ElMessage.warning('请登录后才能提交分数到排行榜')
    }
  } catch (error) {
    ElMessage.error('提交分数失败')
    console.error(error)
  } finally {
    submittingScore.value = false
  }
}

// 计算当前移动速度
const currentSpeed = computed(() => {
  return Math.floor(10 - ((gameSpeed.value - 50) / 10))
})
</script>

<template>
  <div class="game-container">
    <div class="game-header">
      <h1>贪吃蛇游戏</h1>
      <div class="score-display">
        <div class="score-item">
          <span>当前分数:</span>
          <span class="score">{{ score }}</span>
        </div>
        <div class="score-item">
          <span>最高分数:</span>
          <span class="high-score">{{ highScore }}</span>
        </div>
        <div class="score-item">
          <span>速度等级:</span>
          <span class="speed">{{ currentSpeed }}</span>
        </div>
      </div>
    </div>
    
    <div class="game-area">
      <canvas 
        ref="gameCanvas" 
        :width="canvasWidth" 
        :height="canvasHeight"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
      ></canvas>
    </div>
    
    <div class="game-controls">
      <el-button type="primary" @click="startGame" :disabled="isGameRunning || showCountdown">
        开始游戏
      </el-button>
      <el-button type="warning" @click="pauseGame" :disabled="!isGameRunning">
        暂停游戏
      </el-button>
      <el-button type="danger" @click="restartGame">
        重新开始
      </el-button>
    </div>
    
    <div class="game-instructions">
      <h3>游戏说明</h3>
      <p>使用键盘方向键或 WASD 控制贪吃蛇移动。</p>
      <p>在手机上可以通过滑动屏幕控制方向。</p>
      <p>吃到食物可以获得分数，同时蛇身会变长。</p>
      <p>撞到墙壁或自己的身体游戏结束。</p>
      <p>每得 50 分，游戏速度会加快一级。</p>
      <p>按空格键可以暂停/继续游戏。</p>
      <p>游戏结束后，分数会自动提交到排行榜（需登录）。</p>
    </div>
  </div>
</template>

<style scoped>
.game-container {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
  background: var(--gradient-bg);
  min-height: 100vh;
  transition: background 0.3s ease;
}

.game-header {
  margin-bottom: 30px;
  text-align: center;
  background: var(--card-bg);
  padding: 30px;
  border-radius: 20px;
  box-shadow: 0 10px 30px var(--shadow-color);
  backdrop-filter: blur(15px);
}

.game-header h1 {
  color: var(--text-color);
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(45deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 20px;
}

.score-display {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin: 20px 0;
}

.score-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: var(--background-color);
  border-radius: 15px;
  box-shadow: 0 5px 15px var(--shadow-color);
  min-width: 120px;
  transition: all 0.3s ease;
}

.score-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px var(--shadow-color);
}

.score-item span:first-child {
  color: var(--text-secondary);
  font-weight: 500;
  margin-bottom: 8px;
}

.score, .high-score, .speed {
  font-size: 24px;
  font-weight: bold;
  margin-top: 5px;
}

.game-area {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

canvas {
  border: 3px solid var(--border-color);
  border-radius: 15px;
  box-shadow: 0 10px 30px var(--shadow-color);
  background: var(--background-color);
  transition: all 0.3s ease;
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.game-instructions {
  background: var(--card-bg);
  color: var(--text-color);
  padding: 25px;
  border-radius: 15px;
  margin-top: 20px;
  box-shadow: 0 5px 20px var(--shadow-color);
  backdrop-filter: blur(10px);
}

.game-instructions h3 {
  color: var(--text-color);
  margin-bottom: 15px;
  font-size: 20px;
  font-weight: 600;
}

.game-instructions p {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 10px 0;
}
</style>
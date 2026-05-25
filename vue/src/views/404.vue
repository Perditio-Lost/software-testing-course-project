<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <div class="not-found-content">
        <div class="error-code">404</div>
        <h1 class="error-title">页面未找到</h1>
        <p class="error-description">抱歉，您访问的页面不存在或已被移除</p>
        <div class="error-actions">
          <router-link to="/" class="back-home-btn">
            <el-icon><HomeFilled /></el-icon>
            返回主页
          </router-link>
          <a @click="goBack" class="go-back-btn">
            <el-icon><Back /></el-icon>
            返回上一页
          </a>
        </div>
      </div>
      <div class="illustration">
        <img src="@/assets/imgs/404.jpg" alt="404">
      </div>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled, Back } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 50%, #fff 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.not-found-page::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
}

.not-found-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(64, 169, 255, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

.not-found-container {
  display: flex;
  align-items: center;
  gap: 80px;
  max-width: 1200px;
  position: relative;
  z-index: 1;
}

.not-found-content {
  flex: 1;
  max-width: 500px;
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 24px;
  text-shadow: 0 8px 24px rgba(24, 144, 255, 0.2);
}

.error-title {
  font-size: 42px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.error-description {
  font-size: 18px;
  color: var(--text-light);
  margin: 0 0 40px 0;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.back-home-btn,
.go-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.back-home-btn {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #ffffff;
  border: none;
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.3);
}

.back-home-btn:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4);
}

.go-back-btn {
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  color: var(--primary-color);
  border: 2px solid var(--border-normal);
}

.go-back-btn:hover {
  background: linear-gradient(135deg, #bae7ff 0%, #e6f7ff 100%);
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.2);
}

.illustration {
  flex: 1;
  max-width: 500px;
}

.illustration img {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 8px 24px rgba(24, 144, 255, 0.15));
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 响应式 */
@media (max-width: 960px) {
  .not-found-container {
    flex-direction: column;
    gap: 40px;
    text-align: center;
  }
  
  .error-code {
    font-size: 90px;
  }
  
  .error-title {
    font-size: 32px;
  }
  
  .error-description {
    font-size: 16px;
  }
  
  .error-actions {
    justify-content: center;
  }
  
  .illustration {
    max-width: 350px;
  }
}
</style>
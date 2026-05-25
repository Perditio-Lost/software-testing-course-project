<template>
  <div class="login-page">
    <!-- 主卡片 -->
    <div class="login-wrapper">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>智考云</span>
        </div>
        
        <div class="welcome-text">
          <h1>欢迎回来</h1>
          <p>登录您的账号，开启智慧学习之旅</p>
        </div>
        
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="feature-info">
              <h4>智能组卷</h4>
              <p>多种题型，灵活组合</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="feature-info">
              <h4>在线考试</h4>
              <p>实时计时，自动提交</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="feature-info">
              <h4>成绩分析</h4>
              <p>数据统计，一目了然</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="form-header">
          <h2>用户登录</h2>
          <p>欢迎回来，请登录您的账号</p>
        </div>
        
        <el-form ref="formRef" :model="data.form" :rules="data.rules" class="login-form">
          <el-form-item prop="role" class="role-select-item">
            <el-radio-group v-model="data.form.role" class="role-radio-group">
              <el-radio-button value="STUDENT">
                <el-icon><User /></el-icon>
                <span>学生登录</span>
              </el-radio-button>
              <el-radio-button value="TEACHER">
                <el-icon><UserFilled /></el-icon>
                <span>教师登录</span>
              </el-radio-button>
              <el-radio-button value="ADMIN">
                <el-icon><Setting /></el-icon>
                <span>管理员</span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item prop="username" class="form-item-label">
            <label class="input-label">账号</label>
            <el-input 
              :prefix-icon="User" 
              size="large" 
              v-model="data.form.username" 
              placeholder="请输入账号"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="password" class="form-item-label">
            <label class="input-label">密码</label>
            <el-input 
              show-password 
              :prefix-icon="Lock" 
              size="large" 
              v-model="data.form.password" 
              placeholder="请输入密码"
            ></el-input>
          </el-form-item>
          
          <div class="form-options">
            <a href="/forgot-password" class="forgot-link">忘记密码?</a>
          </div>
          
          <el-form-item>
            <el-button 
              size="large" 
              type="primary" 
              class="login-btn" 
              @click="showVerify"
            >
              <el-icon><Unlock /></el-icon>
              <span>登 录</span>
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <span>还没有账号？</span>
          <a href="/register">立即注册</a>
        </div>
      </div>
    </div>

    <!-- 拼图验证组件 -->
    <PuzzleVerify 
      :visible="data.verifyVisible" 
      @close="handleVerifyClose" 
      @success="handleVerifySuccess"
      ref="puzzleVerifyRef"
    />

    <!-- 底部版权 -->
    <div class="footer-copyright">
      浙江工业大学 © 2025 智考云-基于教考分离的智能在线考试系统
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { User, Lock, UserFilled, Setting, Unlock, Reading, Clock, TrendCharts } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";
import PuzzleVerify from "@/components/PuzzleVerify.vue";

const data = reactive({
  form: {
    username: '',
    password: '',
    role: 'STUDENT'
  },
  verifyVisible: false,
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ],
    role: [
      { required: true, message: '请选择角色', trigger: 'change' }
    ],
  }
})

const formRef = ref()
const puzzleVerifyRef = ref()

// 显示验证码
const showVerify = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.verifyVisible = true
      setTimeout(() => {
        if (puzzleVerifyRef.value) {
          puzzleVerifyRef.value.initPuzzle()
        }
      }, 100)
    }
  })
}

// 关闭验证弹窗
const handleVerifyClose = () => {
  data.verifyVisible = false
}

// 验证成功后执行登录
const handleVerifySuccess = () => {
  data.verifyVisible = false
  login()
}

// 登录请求
const login = () => {
  request.post('/login', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('登录成功')
      localStorage.setItem('xm-user', JSON.stringify(res.data))
      setTimeout(() => {
        if ('STUDENT' === res.data.role) {
          location.href = '/front/home'
        } else {
          location.href = '/manager/home'
        }
      }, 500)
    } else {
      ElMessage.error(res.msg)
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 50%, #fff 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
}

.login-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(64, 169, 255, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

.login-wrapper {
  display: flex;
  width: 920px;
  min-height: 560px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 40px rgba(24, 144, 255, 0.15);
  overflow: hidden;
  position: relative;
  z-index: 1;
  border: 1px solid rgba(24, 144, 255, 0.1);
}

/* 左侧品牌区域 */
.brand-section {
  width: 400px;
  padding: 48px 40px;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 50%, #69c0ff 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  top: -100px;
  right: -100px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
  position: relative;
  z-index: 1;
}

.brand-logo svg {
  width: 38px;
  height: 38px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.1));
}

.brand-logo span {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.welcome-text {
  margin-bottom: 48px;
  position: relative;
  z-index: 1;
}

.welcome-text h1 {
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 16px 0;
  line-height: 1.2;
}

.welcome-text p {
  font-size: 16px;
  opacity: 0.95;
  margin: 0;
  line-height: 1.6;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 32px;
  position: relative;
  z-index: 1;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.28);
  transform: translateX(8px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.feature-icon .el-icon {
  font-size: 22px;
}

.feature-info h4 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.feature-info p {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  padding: 56px 64px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 30px;
  font-weight: 800;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 12px 0;
}

.form-header p {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.login-form {
  flex: 1;
}

/* 角色选择 */
.role-select-item {
  margin-bottom: 32px;
}

.role-radio-group {
  width: 100%;
  display: flex;
  gap: 8px;
}

.role-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.role-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 0;
  border-radius: 10px;
  border: 2px solid #e0f2fe;
  font-weight: 600;
  background: #ffffff;
  color: #3b82f6;
  transition: all 0.3s ease;
}

.role-radio-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 10px;
}

.role-radio-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 10px;
}

.role-radio-group :deep(.el-radio-button__inner:hover) {
  border-color: #1890ff;
  background: #e6f7ff;
}

.role-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border-color: #1890ff;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

/* 表单项带标签 */
.form-item-label {
  margin-bottom: 24px;
}

.input-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1e3a8a;
  margin-bottom: 10px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 2px #e0f2fe inset;
  padding: 6px 14px;
  transition: all 0.3s ease;
  background: #ffffff;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px #bfdbfe inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #1890ff inset;
  background: #f0f5ff;
}

/* 表单选项行 */
.form-options {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 28px;
}

.form-options :deep(.el-checkbox__label) {
  color: #64748b;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #1890ff;
  border-color: #1890ff;
}

.forgot-link {
  font-size: 14px;
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
}

.forgot-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 700;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.login-btn:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn .el-icon {
  font-size: 20px;
}

/* 底部链接 */
.form-footer {
  text-align: center;
  font-size: 15px;
  color: #64748b;
  margin-top: 32px;
}

.form-footer a {
  color: #1890ff;
  text-decoration: none;
  font-weight: 600;
  margin-left: 6px;
  transition: all 0.3s ease;
}

.form-footer a:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 底部版权 */
.footer-copyright {
  margin-top: 40px;
  font-size: 14px;
  color: #94a3b8;
  position: relative;
  z-index: 1;
}

/* 响应式 */
@media (max-width: 960px) {
  .login-wrapper {
    width: 100%;
    max-width: 480px;
    flex-direction: column;
  }
  
  .brand-section {
    width: auto;
    padding: 40px 32px;
  }
  
  .welcome-text h1 {
    font-size: 28px;
  }
  
  .feature-list {
    display: none;
  }
  
  .form-section {
    padding: 40px 32px;
  }
}
</style>
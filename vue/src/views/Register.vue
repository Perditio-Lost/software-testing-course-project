<template>
  <div class="register-page">
    <!-- 主卡片 -->
    <div class="register-wrapper">
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
          <h1>学生注册</h1>
          <p>创建您的账号，开启智慧学习之旅</p>
        </div>
        
        <div class="feature-list">
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
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <div class="feature-info">
              <h4>注册须知</h4>
              <p>注册后需等待管理员审核</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="feature-info">
              <h4>账号安全</h4>
              <p>请使用真实邮箱接收验证码</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Message /></el-icon>
            </div>
            <div class="feature-info">
              <h4>邮箱验证</h4>
              <p>验证码60秒内有效</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧注册表单 -->
      <div class="form-section">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>请填写以下信息完成注册</p>
        </div>
        
        <el-form ref="formRef" :model="data.form" :rules="data.rules" class="register-form">
          <el-form-item prop="username" class="form-item-label">
            <label class="input-label">账号</label>
            <el-input 
              :prefix-icon="User" 
              size="large" 
              v-model="data.form.username" 
              placeholder="请输入账号（3-20个字符）"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="name" class="form-item-label">
            <label class="input-label">真实姓名</label>
            <el-input 
              :prefix-icon="User" 
              size="large" 
              v-model="data.form.name" 
              placeholder="请输入真实姓名"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="email" class="form-item-label">
            <label class="input-label">邮箱</label>
            <el-input 
              :prefix-icon="Message" 
              size="large" 
              v-model="data.form.email" 
              placeholder="请输入邮箱地址"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="verificationCode" class="form-item-label">
            <label class="input-label">验证码</label>
            <div class="verify-code-row">
              <el-input 
                size="large"
                v-model="data.form.verificationCode" 
                placeholder="请输入6位验证码"
                class="verify-input"
              ></el-input>
              <el-button 
                @click="sendVerificationCode"
                :disabled="countdown > 0"
                size="large"
                class="verify-btn"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          
          <el-form-item prop="password" class="form-item-label">
            <label class="input-label">密码</label>
            <el-input 
              show-password 
              :prefix-icon="Lock" 
              size="large" 
              v-model="data.form.password" 
              placeholder="请输入密码（6-20个字符）"
            ></el-input>
          </el-form-item>
          
          <el-form-item prop="confirmPassword" class="form-item-label">
            <label class="input-label">确认密码</label>
            <el-input 
              show-password 
              :prefix-icon="Lock" 
              size="large" 
              v-model="data.form.confirmPassword" 
              placeholder="请再次输入密码"
            ></el-input>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button 
              size="large" 
              type="primary" 
              class="register-btn" 
              @click="register" 
              :loading="loading"
            >
              <el-icon><Check /></el-icon>
              <span>立即注册</span>
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <span>已有账号？</span>
          <a href="/login">返回登录</a>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="footer-copyright">
      浙江工业大学 © 2025 智考云-基于教考分离的智能在线考试系统
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onUnmounted } from "vue";
import { User, Lock, Message, Check, InfoFilled, Reading, Clock, TrendCharts } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else {
    if (value !== data.form.password) {
      callback(new Error("确认密码跟原密码不一致!"))
    }
    callback()
  }
}

const data = reactive({
  form: { 
    username: '',
    name: '',
    email: '',
    verificationCode: '',
    password: '',
    confirmPassword: ''
  },
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入真实姓名', trigger: 'blur' }
    ],
    email: [
      { required: true, message: '请输入邮箱地址', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    verificationCode: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 6, message: '验证码长度为6位数字', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
      { validator: validatePass, trigger: 'blur' }
    ]
  }
})

const formRef = ref()
const loading = ref(false)
const countdown = ref(0)
let timer = null

// 发送验证码
const sendVerificationCode = async () => {
  if (!data.form.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(data.form.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }
  
  try {
    const response = await request.post('/user/sendCode', null, {
      params: { email: data.form.email }
    })
    
    if (response.code === '200') {
      ElMessage.success('验证码发送成功，请查收邮件')
      startCountdown()
    } else {
      ElMessage.error(response.msg || '验证码发送失败')
    }
  } catch (error) {    ElMessage.error('验证码发送失败，请重试')
  }
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const register = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await request.post('/user/emailRegister', {
          email: data.form.email,
          password: data.form.password,
          name: data.form.name,
          username: data.form.username
        }, {
          params: { verificationCode: data.form.verificationCode }
        })
        
        if (response.code === '200') {
          ElMessage.success('注册成功！请等待管理员审核后登录')
          setTimeout(() => {
            location.href = '/login'
          }, 2000)
        } else {
          ElMessage.error(response.msg || '注册失败')
        }
      } catch (error) {        ElMessage.error('注册失败，请重试')
      } finally {
        loading.value = false
      }
    }
  })
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.register-page {
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

.register-page::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: -150px;
  right: -150px;
}

.register-page::after {
  content: '';
  position: absolute;
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(64, 169, 255, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -80px;
  left: -80px;
}

.register-wrapper {
  display: flex;
  width: 960px;
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
  width: 360px;
  flex-shrink: 0;
  padding: 24px 28px;
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
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  top: -80px;
  right: -80px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.brand-logo svg {
  width: 32px;
  height: 32px;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.1));
}

.brand-logo span {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.welcome-text {
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.welcome-text h1 {
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 8px 0;
  line-height: 1.2;
}

.welcome-text p {
  font-size: 14px;
  opacity: 0.95;
  margin: 0;
  line-height: 1.5;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 28px;
  position: relative;
  z-index: 1;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.28);
  transform: translateX(6px);
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.feature-icon .el-icon {
  font-size: 20px;
}

.feature-info h4 {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.feature-info p {
  font-size: 13px;
  opacity: 0.9;
  margin: 0;
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  padding: 24px 36px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
}

.form-header {
  text-align: center;
  margin-bottom: 20px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px 0;
}

.form-header p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.register-form {
  flex: 1;
}

/* 表单项带标签 */
.form-item-label {
  margin-bottom: 12px;
}

.form-item-label :deep(.el-form-item__error) {
  padding-top: 2px;
  font-size: 11px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1e3a8a;
  margin-bottom: 6px;
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 2px #e0f2fe inset;
  padding: 2px 10px;
  transition: all 0.3s ease;
  background: #ffffff;
}

.register-form :deep(.el-input--large .el-input__inner) {
  height: 36px;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px #bfdbfe inset;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #1890ff inset;
  background: #f0f5ff;
}

/* 验证码行 */
.verify-code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.verify-input {
  flex: 1;
}

.verify-btn {
  width: 120px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border: none;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.25);
}

.verify-btn:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.verify-btn:disabled {
  background: linear-gradient(135deg, #a0cfff 0%, #bfdbfe 100%);
  color: #fff;
  transform: none;
  box-shadow: none;
}

/* 提交按钮 */
.submit-item {
  margin-top: 16px;
  margin-bottom: 0;
}

.register-btn {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.register-btn:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4);
}

.register-btn:active {
  transform: translateY(0);
}

.register-btn .el-icon {
  font-size: 18px;
}

/* 底部链接 */
.form-footer {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-top: 12px;
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
  margin-top: 20px;
  font-size: 12px;
  color: #94a3b8;
  position: relative;
  z-index: 1;
}

/* 响应式 */
@media (max-width: 960px) {
  .register-wrapper {
    width: 100%;
    max-width: 480px;
    flex-direction: column;
  }
  
  .brand-section {
    width: auto;
    padding: 32px 24px;
  }
  
  .welcome-text h1 {
    font-size: 26px;
  }
  
  .feature-list {
    display: none;
  }
  
  .form-section {
    padding: 32px 28px;
  }
}
</style>
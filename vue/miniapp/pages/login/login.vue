<template>
  <view class="login-container">
    <view class="login-box">
      <view class="title">智考云</view>
      <view class="subtitle">学生成绩查询</view>
      
      <view class="form">
        <view class="form-item">
          <text class="label">用户名</text>
          <input 
            class="input" 
            v-model="formData.username" 
            placeholder="请输入用户名"
            placeholder-style="color: #999"
          />
        </view>
        
        <view class="form-item">
          <text class="label">密码</text>
          <input 
            class="input" 
            v-model="formData.password" 
            password
            placeholder="请输入密码"
            placeholder-style="color: #999"
          />
        </view>
        
        <button class="login-btn" @click="handleLogin" :loading="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      formData: {
        username: '',
        password: '',
        role: 'STUDENT'
      },
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      if (!this.formData.username) {
        uni.showToast({
          title: '请输入学号',
          icon: 'none'
        })
        return
      }
      if (!this.formData.password) {
        uni.showToast({
          title: '请输入密码',
          icon: 'none'
        })
        return
      }
      
      this.loading = true
      try {
        const res = await request({
          url: '/login',
          method: 'POST',
          data: this.formData
        })
        
        // 保存token和用户信息
        uni.setStorageSync('token', res.data.token)
        uni.setStorageSync('userInfo', res.data)
        
        uni.showToast({
          title: '登录成功',
          icon: 'success'
        })
        
        // 跳转到成绩列表页
        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/scoreList/scoreList'
          })
        }, 1000)
      } catch (err) {
        console.error('登录失败', err)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #bae7ff 0%, #e6f7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.login-box {
  width: 100%;
  background-color: white;
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #303133;
  text-align: center;
  margin-bottom: 10rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #909399;
  text-align: center;
  margin-bottom: 60rpx;
}

.form {
  width: 100%;
}

.form-item {
  margin-bottom: 40rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 16rpx;
  font-weight: 500;
}

.input {
  width: 100%;
  height: 88rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.login-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  color: white;
  border: none;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: bold;
  margin-top: 40rpx;
  line-height: 88rpx;
}

.login-btn::after {
  border: none;
}
</style>

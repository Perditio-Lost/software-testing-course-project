<template>
  <view class="score-list-container">
    <!-- 顶部统计卡片 -->
    <view class="header-card">
      <view class="user-info">
        <text class="greeting">你好，{{ userInfo.name || '学生' }}</text>
        <text class="student-no">学号：{{ userInfo.username }}</text>
      </view>
      <view class="stat-info">
        <view class="stat-item">
          <text class="stat-value">{{ total }}</text>
          <text class="stat-label">考试总数</text>
        </view>
      </view>
    </view>

    <!-- 考试列表 -->
    <view class="exam-list">
      <view v-if="examList.length === 0" class="empty-state">
        <text class="empty-icon">📝</text>
        <text class="empty-text">暂无考试记录</text>
      </view>
      
      <view 
        v-for="(item, index) in examList" 
        :key="index"
        class="exam-card"
        @click="viewPaper(item)"
      >
        <view class="exam-header">
          <text class="exam-name">{{ (item.test && item.test.name) || '-' }}</text>
          <view :class="['status-badge', getStatusClass(item)]">
            {{ item.status }}
          </view>
        </view>
        
        <view class="exam-info">
          <view class="info-row">
            <text class="info-label">课程：</text>
            <text class="info-value">{{ (item.test && item.test.courseName) || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">教师：</text>
            <text class="info-value">{{ (item.test && item.test.teacherName) || '-' }}</text>
          </view>
        </view>
        
        <view class="exam-footer">
          <view class="score-info">
            <text class="score-label">成绩：</text>
            <text :class="['score-value', getScoreClass(item)]">
              {{ item.score || '-' }}
            </text>
          </view>
          
          <view class="action-area">
            <text 
              v-if="item.cheat === 1" 
              class="cheat-warning"
            >
              ⚠️ 作弊记录
            </text>
            <text 
              v-else-if="item.status !== '已批改' || !item.weatherCheck"
              class="disabled-text"
            >
              暂不可查看
            </text>
            <text v-else class="view-text">
              查看详情 →
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 分页 -->
    <view v-if="total > pageSize" class="pagination">
      <button 
        class="page-btn" 
        :disabled="pageNum === 1"
        @click="prevPage"
      >
        上一页
      </button>
      <text class="page-info">{{ pageNum }} / {{ totalPages }}</text>
      <button 
        class="page-btn" 
        :disabled="pageNum === totalPages"
        @click="nextPage"
      >
        下一页
      </button>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      userInfo: {},
      examList: [],
      pageNum: 1,
      pageSize: 10,
      total: 0
    }
  },
  computed: {
    totalPages() {
      return Math.ceil(this.total / this.pageSize)
    }
  },
  onLoad() {
    this.userInfo = uni.getStorageSync('userInfo') || {}
    this.loadScoreList()
  },
  onPullDownRefresh() {
    this.pageNum = 1
    this.loadScoreList(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    async loadScoreList(callback) {
      try {
        uni.showLoading({
          title: '加载中...'
        })
        
        const res = await request({
          url: '/score/selectPage',
          method: 'GET',
          data: {
            pageNum: this.pageNum,
            pageSize: this.pageSize
          }
        })
        
        this.examList = res.data.list || []
        this.total = res.data.total || 0
        
        uni.hideLoading()
        callback && callback()
      } catch (err) {
        uni.hideLoading()
        console.error('加载成绩列表失败', err)
      }
    },
    
    getStatusClass(item) {
      if (item.status === '已批改') {
        return 'status-success'
      }
      return 'status-warning'
    },
    
    getScoreClass(item) {
      if (!item.score) return ''
      const score = parseFloat(item.score)
      if (score >= 90) return 'score-excellent'
      if (score >= 80) return 'score-good'
      if (score >= 60) return 'score-pass'
      return 'score-fail'
    },
    
    viewPaper(item) {
      if (item.cheat === 1) {
        uni.showToast({
          title: '本次考试有作弊行为，请联系管理员',
          icon: 'none'
        })
        return
      }
      
      if (item.status !== '已批改' || !item.weatherCheck) {
        uni.showToast({
          title: '试卷尚未批改完成',
          icon: 'none'
        })
        return
      }
      
      uni.navigateTo({
        url: `/pages/paperDetail/paperDetail?id=${item.id}`
      })
    },
    
    prevPage() {
      if (this.pageNum > 1) {
        this.pageNum--
        this.loadScoreList()
      }
    },
    
    nextPage() {
      if (this.pageNum < this.totalPages) {
        this.pageNum++
        this.loadScoreList()
      }
    }
  }
}
</script>

<style scoped>
.score-list-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}

.header-card {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  padding: 40rpx 30rpx;
  color: white;
}

.user-info {
  display: flex;
  flex-direction: column;
  margin-bottom: 30rpx;
}

.greeting {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}

.student-no {
  font-size: 26rpx;
  opacity: 0.9;
}

.stat-info {
  display: flex;
  justify-content: space-around;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 16rpx;
  padding: 24rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 48rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 24rpx;
  opacity: 0.9;
}

.exam-list {
  padding: 20rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #909399;
}

.exam-card {
  background-color: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.exam-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  flex: 1;
  margin-right: 20rpx;
}

.status-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  white-space: nowrap;
}

.status-success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.status-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.exam-info {
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
  font-size: 26rpx;
}

.info-label {
  color: #909399;
  margin-right: 12rpx;
}

.info-value {
  color: #606266;
}

.exam-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.score-info {
  display: flex;
  align-items: baseline;
}

.score-label {
  font-size: 26rpx;
  color: #909399;
  margin-right: 12rpx;
}

.score-value {
  font-size: 40rpx;
  font-weight: bold;
}

.score-excellent {
  color: #67c23a;
}

.score-good {
  color: #409eff;
}

.score-pass {
  color: #e6a23c;
}

.score-fail {
  color: #f56c6c;
}

.action-area {
  display: flex;
  align-items: center;
}

.cheat-warning {
  font-size: 24rpx;
  color: #f56c6c;
}

.disabled-text {
  font-size: 24rpx;
  color: #c0c4cc;
}

.view-text {
  font-size: 26rpx;
  color: #409eff;
  font-weight: 500;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 20rpx;
  gap: 20rpx;
}

.page-btn {
  padding: 16rpx 32rpx;
  background-color: white;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #606266;
}

.page-btn:disabled {
  opacity: 0.5;
}

.page-btn::after {
  border: none;
}

.page-info {
  font-size: 26rpx;
  color: #606266;
  padding: 0 20rpx;
}
</style>

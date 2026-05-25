<template>
  <div class="proctoring-container">
    <!-- 页面标题和控制区 -->
    <div class="proctoring-header">
      <div class="header-left">
        <h2>
          <el-icon><Monitor /></el-icon>
          监考大屏
        </h2>
        <el-tag v-if="connectedCount > 0" type="success">
          {{ connectedCount }}/{{ students.length }} 在线
        </el-tag>
      </div>
      <div class="header-right">
        <el-select 
          v-model="selectedExamId" 
          placeholder="请选择考试"
          style="width: 250px"
          @change="onExamChange"
        >
          <el-option
            v-for="exam in examList"
            :key="exam.id"
            :label="exam.name"
            :value="exam.id"
          />
        </el-select>
        <el-button type="success" plain @click="refreshStudents" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新学生列表
        </el-button>
      </div>
    </div>

    <!-- 学生视频网格 -->
    <div class="video-grid" v-if="students.length > 0">
      <div 
        v-for="student in students" 
        :key="student.studentId"
        class="student-card"
        :class="{ 'connected': studentStatus[student.studentId]?.connected }"
      >
        <div class="card-header">
          <span class="student-name">{{ student.studentName }}</span>
          <span class="student-number">{{ student.studentNumber }}</span>
          <el-tag 
            :type="studentStatus[student.studentId]?.connected ? 'success' : 'info'" 
            size="small"
          >
            {{ studentStatus[student.studentId]?.connected ? '在线' : '离线' }}
          </el-tag>
        </div>
        
        <div class="video-container">
          <!-- 电脑摄像头 -->
          <div class="video-box">
            <div class="video-label">电脑摄像头</div>
            <video 
              :ref="el => setVideoRef(el, student.studentId, 'computer')"
              autoplay 
              playsinline 
              muted
              class="video-element"
            ></video>
            <div class="video-overlay" v-if="!studentStatus[student.studentId]?.computerConnected">
              <el-icon class="waiting-icon"><Loading /></el-icon>
              <span>等待连接...</span>
            </div>
          </div>
          
          <!-- 手机摄像头 -->
          <div class="video-box">
            <div class="video-label">手机摄像头</div>
            <video 
              :ref="el => setVideoRef(el, student.studentId, 'phone')"
              autoplay 
              playsinline 
              muted
              class="video-element"
            ></video>
            <div class="video-overlay" v-if="!studentStatus[student.studentId]?.phoneConnected">
              <el-icon class="waiting-icon"><Loading /></el-icon>
              <span>等待连接...</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <span class="clazz-name">{{ student.clazzName }}</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else-if="!loading">
      <el-empty description="请选择考试查看学生监控" />
    </div>

    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <span>正在加载...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Monitor, Refresh, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Room, RoomEvent } from 'livekit-client'
import request from '@/utils/request'

// 状态
const selectedExamId = ref(null)
const examList = ref([])
const students = ref([])
const loading = ref(false)
const connectedCount = ref(0)

// 每个学生的连接状态
const studentStatus = reactive({})

// 视频元素引用
const videoRefs = reactive({})

// LiveKit 房间实例（每个学生一个）
const rooms = reactive({})

// 设置视频元素引用
const setVideoRef = (el, studentId, type) => {
  if (!videoRefs[studentId]) {
    videoRefs[studentId] = {}
  }
  videoRefs[studentId][type] = el
}

// 加载考试列表
const loadExams = async () => {
  try {
    const res = await request.get('/test/selectAll')
    if (res.code === '200') {
      examList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载考试列表失败')
  }
}

// 考试选择变化
const onExamChange = () => {
  if (selectedExamId.value) {
    refreshStudents()
  }
}

// 刷新学生列表
const refreshStudents = async () => {
  if (!selectedExamId.value) {
    ElMessage.warning('请先选择考试')
    return
  }

  loading.value = true
  
  // 先断开所有现有连接
  disconnectAll()

  try {
    // 获取教师监考Token
    const res = await request.get('/livekit/teacherTokens', {
      params: { examId: selectedExamId.value }
    })

    if (res.code === '200') {
      const data = res.data
      students.value = data.students || []
      
      // 初始化每个学生的状态
      for (const student of students.value) {
        studentStatus[student.studentId] = {
          connected: false,
          computerConnected: false,
          phoneConnected: false
        }
      }

      // 连接到每个学生的房间
      for (const student of students.value) {
        await connectToStudentRoom(student, data.wsUrl)
      }
    } else {
      ElMessage.error(res.msg || '获取学生列表失败')
    }
  } catch (error) {
    ElMessage.error('刷新学生列表失败')
  } finally {
    loading.value = false
  }
}

// 连接到学生房间
const connectToStudentRoom = async (student, wsUrl) => {
  try {
    const room = new Room({
      adaptiveStream: true,
      dynacast: true
    })

    // 监听远程轨道订阅
    room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
      
      if (track.kind === 'video') {
        const studentId = student.studentId
        
        // 根据参与者身份判断是电脑还是手机
        if (participant.identity.startsWith('computer-')) {
          if (studentStatus[studentId]) {
            studentStatus[studentId].computerConnected = true
            studentStatus[studentId].connected = true
          }
          // 附加到电脑视频元素
          if (videoRefs[studentId]?.computer) {
            track.attach(videoRefs[studentId].computer)
          }
        } else if (participant.identity.startsWith('phone-')) {
          if (studentStatus[studentId]) {
            studentStatus[studentId].phoneConnected = true
            studentStatus[studentId].connected = true
          }
          // 附加到手机视频元素
          if (videoRefs[studentId]?.phone) {
            track.attach(videoRefs[studentId].phone)
          }
        }
        
        updateConnectedCount()
      }
    })

    room.on(RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
      if (track.kind === 'video') {
        track.detach()
      }
    })

    room.on(RoomEvent.ParticipantDisconnected, (participant) => {
      const studentId = student.studentId
      
      if (participant.identity.startsWith('computer-')) {
        if (studentStatus[studentId]) {
          studentStatus[studentId].computerConnected = false
        }
      } else if (participant.identity.startsWith('phone-')) {
        if (studentStatus[studentId]) {
          studentStatus[studentId].phoneConnected = false
        }
      }
      
      // 两个都断开才算离线
      if (studentStatus[studentId] && 
          !studentStatus[studentId].computerConnected && 
          !studentStatus[studentId].phoneConnected) {
        studentStatus[studentId].connected = false
      }
      
      updateConnectedCount()
    })

    room.on(RoomEvent.Disconnected, () => {
    })

    // 使用ws://连接LiveKit（HTTP页面只能用ws://）
    const host = 'localhost'
    const localWsUrl = `ws://${host}:7880`
    await room.connect(localWsUrl, student.token)
    
    rooms[student.studentId] = room

  } catch (error) {
  }
}

// 更新连接计数
const updateConnectedCount = () => {
  let count = 0
  for (const studentId in studentStatus) {
    if (studentStatus[studentId]?.connected) {
      count++
    }
  }
  connectedCount.value = count
}

// 断开所有连接
const disconnectAll = () => {
  for (const studentId in rooms) {
    if (rooms[studentId]) {
      rooms[studentId].disconnect()
      delete rooms[studentId]
    }
  }
  connectedCount.value = 0
}

onMounted(() => {
  loadExams()
})

onUnmounted(() => {
  disconnectAll()
})
</script>

<style scoped>
.proctoring-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.proctoring-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.student-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
}

.student-card.connected {
  border-color: #67c23a;
  box-shadow: 0 4px 16px rgba(103, 194, 58, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.student-name {
  font-size: 16px;
  font-weight: 600;
}

.student-number {
  font-size: 12px;
  opacity: 0.8;
}

.video-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px;
  background: #1f2937;
}

.video-box {
  position: relative;
  height: 180px;
  background: #0f172a;
}

.video-label {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  font-size: 11px;
  border-radius: 4px;
  z-index: 1;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(15, 23, 42, 0.9);
  color: #64748b;
  font-size: 12px;
}

.waiting-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.card-footer {
  padding: 10px 16px;
  background: #f8fafc;
  border-top: 1px solid #e4e7ed;
}

.clazz-name {
  font-size: 13px;
  color: #6b7280;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #ffffff;
  border-radius: 12px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 400px;
  color: #6b7280;
}

.loading-icon {
  animation: spin 1s linear infinite;
}
</style>

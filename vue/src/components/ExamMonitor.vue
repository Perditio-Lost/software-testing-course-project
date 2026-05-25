<template>
  <div class="exam-monitor-container">
    <div class="monitor-header">
      <span class="monitor-title">
        <el-icon><VideoCamera /></el-icon>
        监考摄像头
      </span>
      <div class="status-dot" :class="statusClass"></div>
    </div>
    
    <div class="monitor-video-wrapper">
      <video ref="videoRef" autoplay playsinline muted class="monitor-video"></video>
      <canvas ref="canvasRef" style="display: none;"></canvas>
      
      <!-- 状态覆盖层 -->
      <div class="monitor-overlay" v-if="!cameraReady">
        <el-icon class="loading-icon" :size="24"><Loading /></el-icon>
        <span>正在启动摄像头...</span>
      </div>
    </div>
    
    <div class="monitor-footer">
      <div class="monitor-info">
        <span class="next-capture">下次检测: {{ nextCaptureTime }}s</span>
      </div>
      <div class="monitor-status">
        <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
        <span v-if="warningCount > 0" class="warning-badge">{{ warningCount }}/3</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { VideoCamera, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Room, RoomEvent, VideoPresets, createLocalVideoTrack } from 'livekit-client'
import request from '@/utils/request'

const props = defineProps({
  examId: {
    type: [Number, String],
    required: true
  },
  studentId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['abnormal'])

const videoRef = ref(null)
const canvasRef = ref(null)
const cameraReady = ref(false)
const warningCount = ref(0)
const nextCaptureTime = ref(10)
const status = ref('normal') // normal, warning, abnormal, error
const streamingStatus = ref('disconnected') // disconnected, connecting, connected

let mediaStream = null
let captureInterval = null
let countdownInterval = null
let livekitRoom = null
let localVideoTrack = null

// 计算属性
const statusClass = computed(() => {
  switch (status.value) {
    case 'normal': return 'status-normal'
    case 'warning': return 'status-warning'
    case 'abnormal': return 'status-abnormal'
    case 'error': return 'status-error'
    default: return 'status-normal'
  }
})

const statusTagType = computed(() => {
  switch (status.value) {
    case 'normal': return 'success'
    case 'warning': return 'warning'
    case 'abnormal': return 'danger'
    case 'error': return 'info'
    default: return 'success'
  }
})

const statusText = computed(() => {
  switch (status.value) {
    case 'normal': return '正常监考中'
    case 'warning': return '人脸异常警告'
    case 'abnormal': return '检测异常'
    case 'error': return '摄像头错误'
    default: return '正常监考中'
  }
})

// 初始化摄像头
const initCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 320 },
        height: { ideal: 240 },
        facingMode: 'user'
      },
      audio: false
    })
    
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      videoRef.value.onloadedmetadata = () => {
        cameraReady.value = true
        startMonitoring()
        // 摄像头就绪后，连接 LiveKit 推流
        connectToLiveKit()
      }
    }
  } catch (error) {    status.value = 'error'
    ElMessage.error('无法访问摄像头，请检查权限设置')
  }
}

// 连接 LiveKit 并推流
const connectToLiveKit = async () => {
  try {
    // 验证props
    if (!props.examId || !props.studentId) {      return
    }
    
    streamingStatus.value = 'connecting'    // 获取电脑端发布者 Token
    const res = await request.get('/livekit/computerToken', {
      params: {
        examId: props.examId,
        studentId: props.studentId
      }
    })
    
    if (res.code !== '200') {      streamingStatus.value = 'disconnected'
      return
    }
    
    const { token, wsUrl, roomName } = res.data    // 创建 LiveKit Room 实例
    livekitRoom = new Room({
      adaptiveStream: true,
      dynacast: true
    })
    
    // 监听房间事件
    livekitRoom.on(RoomEvent.Connected, () => {      streamingStatus.value = 'connected'
    })
    
    livekitRoom.on(RoomEvent.Disconnected, (reason) => {      streamingStatus.value = 'disconnected'
    })
    
    livekitRoom.on(RoomEvent.ConnectionQualityChanged, (quality, participant) => {    })
    
    // 使用ws://连接LiveKit（HTTP页面只能用ws://）
    const host = 'localhost'
    const localWsUrl = `ws://${host}:7880`
    await livekitRoom.connect(localWsUrl, token)    // 创建并发布本地视频轨道
    localVideoTrack = await createLocalVideoTrack({
      facingMode: 'user',
      resolution: VideoPresets.h360.resolution
    })
    
    await livekitRoom.localParticipant.publishTrack(localVideoTrack)  } catch (error) {    streamingStatus.value = 'disconnected'
  }
}

// 开始监控
const startMonitoring = () => {
  // 倒计时更新
  countdownInterval = setInterval(() => {
    nextCaptureTime.value--
    if (nextCaptureTime.value <= 0) {
      nextCaptureTime.value = 10
    }
  }, 1000)
  
  // 每10秒进行一次人脸识别
  captureInterval = setInterval(() => {
    captureAndVerify()
  }, 10000)
  
  // 延迟3秒后进行第一次识别
  setTimeout(() => {
    captureAndVerify()
  }, 3000)
}

// 捕获并验证人脸
const captureAndVerify = async () => {
  if (!cameraReady.value || !videoRef.value || !canvasRef.value) return
  
  try {
    const canvas = canvasRef.value
    const video = videoRef.value
    const ctx = canvas.getContext('2d')
    
    // 设置canvas尺寸
    canvas.width = video.videoWidth || 320
    canvas.height = video.videoHeight || 240
    
    // 绘制当前视频帧
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    
    // 获取图片数据
    const imageData = canvas.toDataURL('image/jpeg', 0.8)
    
    // 发送到后端进行人脸识别
    const res = await request.post('/monitor/faceVerify', {
      examId: props.examId,
      studentId: props.studentId,
      imageData: imageData
    })
    
    if (res.code === '200') {
      if (res.data.match) {
        // 人脸匹配成功
        warningCount.value = 0
        status.value = 'normal'
      } else {
        // 人脸不匹配
        warningCount.value++
        status.value = 'warning'
        ElMessage.warning(`人脸识别不匹配 (${warningCount.value}/3)`)
        
        if (warningCount.value >= 3) {
          // 连续3次不匹配，判定为异常
          status.value = 'abnormal'
          emit('abnormal', {
            examId: props.examId,
            studentId: props.studentId,
            warningCount: warningCount.value
          })
          
          ElMessageBox.alert(
            '您的人脸识别连续3次不匹配，考试已被标记为异常，请联系监考老师。',
            '考试异常警告',
            {
              confirmButtonText: '我知道了',
              type: 'error'
            }
          )
        }
      }
    } else {    }
    
  } catch (error) {  }
  
  // 重置倒计时
  nextCaptureTime.value = 10
}

// 停止监控
const stopMonitoring = () => {
  if (captureInterval) {
    clearInterval(captureInterval)
    captureInterval = null
  }
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  // 清理 LiveKit 资源
  if (localVideoTrack) {
    localVideoTrack.stop()
    localVideoTrack = null
  }
  if (livekitRoom) {
    livekitRoom.disconnect()
    livekitRoom = null
  }
}

onMounted(() => {
  initCamera()
})

onUnmounted(() => {
  stopMonitoring()
})
</script>

<style scoped>
.exam-monitor-container {
  width: 100%;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
}

.monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.monitor-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-normal {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-warning {
  background: #f59e0b;
  box-shadow: 0 0 8px #f59e0b;
}

.status-abnormal {
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.status-error {
  background: #6b7280;
  box-shadow: 0 0 8px #6b7280;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.monitor-video-wrapper {
  position: relative;
  width: 100%;
  height: 165px;
  background: #1f2937;
}

.monitor-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* 镜像显示 */
}

.monitor-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(31, 41, 55, 0.9);
  color: #ffffff;
  font-size: 12px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.monitor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.monitor-info {
  font-size: 11px;
  color: #6b7280;
}

.monitor-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.warning-badge {
  background: #fef3c7;
  color: #d97706;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}
</style>

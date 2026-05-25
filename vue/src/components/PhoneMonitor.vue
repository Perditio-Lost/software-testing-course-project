<template>
  <div class="phone-monitor-container">
    <div class="monitor-header">
      <span class="monitor-title">
        <el-icon><Iphone /></el-icon>
        手机监考
      </span>
      <div class="status-dot" :class="statusClass"></div>
    </div>
    
    <div class="monitor-content">
      <!-- 未连接时显示二维码 -->
      <div v-if="!phoneConnected" class="qr-section">
        <div v-if="loading" class="loading-state">
          <el-icon class="loading-icon" :size="24"><Loading /></el-icon>
          <span>正在生成二维码...</span>
        </div>
        <div v-else-if="qrCode" class="qr-wrapper">
          <img :src="qrCode" alt="扫描二维码" class="qr-image" />
          <p class="qr-hint">请用手机扫描二维码</p>
        </div>
        <div v-else class="error-state">
          <el-button size="small" @click="fetchRoomInfo">重新获取</el-button>
        </div>
      </div>
      
      <!-- 已连接时显示视频 -->
      <div v-else class="video-section">
        <video ref="remoteVideoRef" autoplay playsinline class="remote-video"></video>
      </div>
    </div>
    
    <div class="monitor-footer">
      <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Iphone, Loading } from '@element-plus/icons-vue'
import { Room, RoomEvent } from 'livekit-client'
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

// 定义事件
const emit = defineEmits(['connected'])

const remoteVideoRef = ref(null)
const loading = ref(false)
const qrCode = ref('')
const phoneConnected = ref(false)
const status = ref('waiting') // waiting, connecting, connected, error

let room = null

const statusClass = computed(() => {
  switch (status.value) {
    case 'connected': return 'status-normal'
    case 'connecting': return 'status-warning'
    case 'error': return 'status-error'
    default: return 'status-waiting'
  }
})

const statusTagType = computed(() => {
  switch (status.value) {
    case 'connected': return 'success'
    case 'connecting': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (status.value) {
    case 'connected': return '手机已连接'
    case 'connecting': return '正在连接...'
    case 'error': return '连接失败'
    default: return '等待手机连接'
  }
})

const fetchRoomInfo = async () => {
  // 验证props
  if (!props.examId || !props.studentId) {    return
  }
  
  loading.value = true
  try {
    const res = await request.get('/livekit/roomInfo', {
      params: {
        examId: props.examId,
        studentId: props.studentId
      }
    })
    
    if (res.code === '200') {
      qrCode.value = res.data.qrCode
      // 使用ws://连接LiveKit（HTTP页面只能用ws://）
      const host = 'localhost'
      const localWsUrl = `ws://${host}:7880`
      await connectToRoom(localWsUrl, res.data.viewerToken)
    } else {      status.value = 'error'
    }
  } catch (error) {    status.value = 'error'
  } finally {
    loading.value = false
  }
}

const connectToRoom = async (wsUrl, token) => {
  try {
    status.value = 'connecting'
    
    room = new Room({
      adaptiveStream: true,
      dynacast: true,
      // 添加 WebRTC 配置
      rtcConfig: {
        iceServers: [
          {
            urls: 'stun:stun.l.google.com:19302'
          }
        ],
        iceTransportPolicy: 'all'
      }
    })
    
    // 监听远程轨道订阅
    room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {      // 只处理来自手机端的视频轨道（phone- 前缀）
      // 忽略来自电脑端的视频轨道（computer- 前缀）
      if (track.kind === 'video' && participant.identity.startsWith('phone-')) {
        phoneConnected.value = true
        status.value = 'connected'
        // 通知父组件手机已连接
        emit('connected')
        
        // 使用 nextTick 等待 DOM 更新后再附加视频
        // 因为 v-if 切换需要等待 DOM 渲染
        import('vue').then(({ nextTick }) => {
          nextTick(() => {
            if (remoteVideoRef.value) {
              track.attach(remoteVideoRef.value)            } else {            }
          })
        })
      }
    })
    
    room.on(RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
      if (track.kind === 'video') {
        track.detach()
      }
    })
    
    room.on(RoomEvent.ParticipantConnected, (participant) => {    })
    
    room.on(RoomEvent.ParticipantDisconnected, (participant) => {      if (participant.identity.startsWith('phone-')) {
        phoneConnected.value = false
        status.value = 'waiting'
      }
    })
    
    room.on(RoomEvent.Disconnected, () => {
      phoneConnected.value = false
      status.value = 'waiting'
    })
    
    await room.connect(wsUrl, token)  } catch (error) {    status.value = 'error'
  }
}

onMounted(() => {
  fetchRoomInfo()
})

onUnmounted(() => {
  if (room) {
    room.disconnect()
  }
})
</script>

<style scoped>
.phone-monitor-container {
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
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
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

.status-error {
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.status-waiting {
  background: #6b7280;
  box-shadow: 0 0 8px #6b7280;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.monitor-content {
  height: 165px;
  background: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-section {
  text-align: center;
  color: #ffffff;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.qr-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.qr-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  background: #ffffff;
  padding: 4px;
}

.qr-hint {
  font-size: 11px;
  color: #9ca3af;
  margin: 0;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.video-section {
  width: 100%;
  height: 100%;
}

.remote-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.monitor-footer {
  display: flex;
  justify-content: center;
  padding: 8px 12px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}
</style>

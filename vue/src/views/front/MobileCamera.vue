<template>
  <div class="mobile-camera-container">
    <div class="header">
      <el-icon><VideoCamera /></el-icon>
      <span>手机监考摄像头</span>
    </div>
    
    <div class="video-wrapper">
      <video ref="videoRef" autoplay playsinline muted class="video"></video>
      
      <div class="overlay" v-if="!cameraReady">
        <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
        <span>正在启动摄像头...</span>
      </div>
      
      <div class="overlay success" v-else-if="connected">
        <el-icon :size="24"><Check /></el-icon>
        <span>正在推流到考试系统</span>
      </div>
    </div>
    
    <div class="status-bar">
      <el-tag :type="statusType" size="large">{{ statusText }}</el-tag>
    </div>
    
    <div class="instructions" v-if="!connected">
      <p>请将手机放置在侧后方，拍摄您的答题环境</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { VideoCamera, Loading, Check } from '@element-plus/icons-vue'
import { Room, RoomEvent, VideoPresets, createLocalVideoTrack } from 'livekit-client'

// 从URL获取参数 (支持 history 模式和 hash 模式)
const urlParams = new URLSearchParams(window.location.search || window.location.hash.split('?')[1] || '')
const token = urlParams.get('token') || ''
const wsUrl = urlParams.get('wsUrl') || ''
const roomName = urlParams.get('room') || ''

const videoRef = ref(null)
const cameraReady = ref(false)
const connected = ref(false)
const status = ref('init') // init, camera, connecting, connected, error
const errorMsg = ref('')

let room = null
let localVideoTrack = null

const statusType = computed(() => {
  switch (status.value) {
    case 'connected': return 'success'
    case 'error': return 'danger'
    case 'camera':
    case 'connecting': return 'warning'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (status.value) {
    case 'connected': return '已连接 - 正在监控中'
    case 'error': return '连接失败: ' + errorMsg.value
    case 'camera': return '摄像头已启动'
    case 'connecting': return '正在连接服务器...'
    default: return '初始化中...'
  }
})

const initCamera = async () => {
  try {
    status.value = 'init'
    
    // 创建本地视频轨道
    localVideoTrack = await createLocalVideoTrack({
      facingMode: 'environment', // 使用后置摄像头
      resolution: VideoPresets.h360.resolution
    })
    
    // 显示预览
    if (videoRef.value) {
      localVideoTrack.attach(videoRef.value)
    }
    
    cameraReady.value = true
    status.value = 'camera'
    
    return localVideoTrack
  } catch (error) {
    status.value = 'error'
    errorMsg.value = '无法访问摄像头'
    throw error
  }
}

const connectToRoom = async () => {
  if (!token || !wsUrl) {
    status.value = 'error'
    errorMsg.value = '缺少连接参数'
    return
  }
  
  try {
    status.value = 'connecting'
    
    room = new Room({
      adaptiveStream: true,
      dynacast: true
    })
    
    room.on(RoomEvent.Connected, () => {
      connected.value = true
      status.value = 'connected'
    })
    
    room.on(RoomEvent.Disconnected, (reason) => {
      connected.value = false
      status.value = 'error'
      errorMsg.value = '连接断开'
    })
    
    room.on(RoomEvent.ConnectionQualityChanged, (quality, participant) => {
    })
    
    // 连接到房间
    await room.connect(wsUrl, token)
    
    // 发布已创建的本地视频轨道
    if (localVideoTrack) {
      await room.localParticipant.publishTrack(localVideoTrack)
    } else {
      // 如果还没有创建轨道，使用 enableCameraAndMicrophone
      await room.localParticipant.setCameraEnabled(true)
    }
    
  } catch (error) {
    status.value = 'error'
    errorMsg.value = error.message || '连接失败'
  }
}

onMounted(async () => {
  try {
    await initCamera()
    await connectToRoom()
  } catch (error) {
  }
})

onUnmounted(() => {
  if (localVideoTrack) {
    localVideoTrack.stop()
  }
  if (room) {
    room.disconnect()
  }
})
</script>

<style scoped>
.mobile-camera-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  padding: 16px;
  color: #ffffff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  padding: 16px 0;
}

.video-wrapper {
  position: relative;
  flex: 1;
  max-height: 60vh;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  margin: 16px 0;
}

.video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.7);
  font-size: 14px;
}

.overlay.success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-bar {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.instructions {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 16px;
}
</style>

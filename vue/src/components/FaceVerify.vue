<template>
  <div class="face-verify-wrapper">
    <!-- 人脸识别预览 -->
    <div class="face-preview">
      <video 
        ref="videoRef"
        autoplay 
        playsinline 
        muted
        class="face-video"
      ></video>
      <canvas ref="canvasRef" style="display: none;"></canvas>
      
      <!-- 状态覆盖层 -->
      <div class="face-overlay" v-if="!cameraReady">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>正在启动摄像头...</span>
      </div>
      
      <!-- 验证中覆盖层 -->
      <div class="face-overlay verifying" v-if="verifying">
        <el-icon class="loading-icon rotating"><Loading /></el-icon>
        <span>正在进行人脸识别...</span>
      </div>
      
      <!-- 验证成功覆盖层 -->
      <div class="face-overlay success" v-if="verifySuccess">
        <el-icon class="success-icon"><CircleCheck /></el-icon>
        <span>人脸识别成功！</span>
      </div>
    </div>
    
    <!-- 验证按钮 -->
    <el-button 
      type="primary"
      :disabled="!cameraReady || verifying || verifySuccess"
      :loading="verifying"
      @click="verifyFace"
      style="width: 100%; margin-top: 12px"
    >
      {{ buttonText }}
    </el-button>
    
    <!-- 提示信息 -->
    <div class="face-tips" v-if="!verifySuccess">
      <p><el-icon><InfoFilled /></el-icon> 请确保面部清晰可见</p>
      <p><el-icon><InfoFilled /></el-icon> 保持光线充足</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
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

const emit = defineEmits(['verified'])

const videoRef = ref(null)
const canvasRef = ref(null)
const cameraReady = ref(false)
const verifying = ref(false)
const verifySuccess = ref(false)

let mediaStream = null

// 按钮文字
const buttonText = computed(() => {
  if (verifySuccess.value) return '验证成功'
  if (verifying.value) return '识别中...'
  if (!cameraReady.value) return '摄像头启动中...'
  return '开始人脸识别'
})

// 初始化摄像头
const initCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: 'user',
        width: { ideal: 640 },
        height: { ideal: 480 }
      },
      audio: false
    })

    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      cameraReady.value = true
    }
  } catch (error) {
    let errorMsg = '摄像头启动失败'
    
    if (error.name === 'NotAllowedError') {
      errorMsg = '请允许访问摄像头权限'
    } else if (error.name === 'NotFoundError') {
      errorMsg = '未检测到摄像头设备'
    } else if (error.name === 'NotReadableError') {
      errorMsg = '摄像头被其他程序占用'
    }
    
    ElMessage.error(errorMsg)
  }
}

// 进行人脸识别
const verifyFace = async () => {
  if (!cameraReady.value || verifying.value || verifySuccess.value) return
  
  verifying.value = true
  
  try {
    const canvas = canvasRef.value
    const video = videoRef.value
    const ctx = canvas.getContext('2d')
    
    // 设置canvas尺寸
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480
    
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
        verifySuccess.value = true
        ElMessage.success('人脸识别成功！')
        emit('verified', true)
      } else {
        // 人脸不匹配
        ElMessage.error(res.data.message || '人脸识别失败，请重试')
      }
    } else {
      ElMessage.error(res.msg || '人脸识别失败')
    }
  } catch (error) {
    ElMessage.error('人脸识别失败：' + error.message)
  } finally {
    verifying.value = false
  }
}

// 停止摄像头
const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
}

onMounted(() => {
  initCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.face-verify-wrapper {
  width: 100%;
}

.face-preview {
  position: relative;
  width: 100%;
  height: 240px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.face-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.face-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  gap: 12px;
  font-size: 14px;
}

.face-overlay.verifying {
  background: rgba(59, 130, 246, 0.8);
}

.face-overlay.success {
  background: rgba(16, 185, 129, 0.9);
}

.loading-icon {
  font-size: 32px;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.success-icon {
  font-size: 48px;
  color: #fff;
}

.face-tips {
  margin-top: 12px;
  padding: 12px;
  background: #eff6ff;
  border-radius: 6px;
  font-size: 13px;
  color: #1e40af;
}

.face-tips p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0;
}

.face-tips .el-icon {
  color: #3b82f6;
}
</style>

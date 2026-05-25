<template>
  <div class="puzzle-verify-overlay" v-if="visible" @click.self="close">
    <div class="puzzle-verify-container">
      <div class="puzzle-header">
        <span>请完成安全验证</span>
        <el-icon class="close-icon" @click="close"><Close /></el-icon>
      </div>
      
      <!-- 图形校验区域 -->
      <div class="check" :style="{ backgroundImage: `url(${backgroundImage})` }">
        <div class="check-before" :style="{ top: puzzleY + 'px', left: puzzleX + 'px' }"></div>
        <div class="check-child" ref="checkChild"></div>
      </div>
      
      <!-- 拖动条 -->
      <div class="drag">
        <div class="drag-bg" :style="{ width: dragBgWidth + 'px' }"></div>
        <div class="drag-tips" v-show="!verifySuccess">
          <span>{{ tipText }}</span>
        </div>
        <div class="drag-tips success-tips" v-show="verifySuccess">
          <el-icon><CircleCheck /></el-icon>
          <span>验证成功</span>
        </div>
        <div class="drag-child" ref="dragChild" @mousedown="dragMouseDown" @touchstart="dragTouchStart">
          <el-icon><DArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { Close, DArrowRight, CircleCheck } from '@element-plus/icons-vue'
// 导入本地图片
import pintuImg1 from '@/assets/imgs/pintu-1.png'
import pintuImg2 from '@/assets/imgs/pintu-2.png'
import pintuImg3 from '@/assets/imgs/pintu-3.png'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'success'])

// 背景图片列表 - 使用本地图片
const backgroundImages = [
  pintuImg1,
  pintuImg2,
  pintuImg3
]

const backgroundImage = ref('')
const checkChild = ref(null)
const dragChild = ref(null)
const verifySuccess = ref(false)
const tipText = ref('按住左边按钮向右拖动完成验证')
const dragBgWidth = ref(0)

// 拼图位置
const puzzleX = ref(0)
const puzzleY = ref(0)

// 拖动相关
const isDragging = ref(false)
const startX = ref(0)
const currentX = ref(0)

// 初始化拼图
const initPuzzle = () => {
  // 随机选择背景图片
  backgroundImage.value = backgroundImages[Math.floor(Math.random() * backgroundImages.length)]
  
  // 随机生成拼图位置 - 确保Y轴位置合理
  puzzleX.value = 250 + Math.floor(Math.random() * 100) // 250-350之间
  puzzleY.value = 80 + Math.floor(Math.random() * 80)   // 80-160之间，确保不超出范围
  
  verifySuccess.value = false
  currentX.value = 0
  dragBgWidth.value = 0
  tipText.value = '按住左边按钮向右拖动完成验证'
  
  // 等待DOM更新后设置位置
  setTimeout(() => {
    if (checkChild.value) {
      checkChild.value.style.transform = 'translateX(0px)'
      checkChild.value.style.top = `${puzzleY.value}px`
      // 设置拼图块的背景图片和位置，使其显示对应区域
      checkChild.value.style.backgroundImage = `url(${backgroundImage.value})`
      checkChild.value.style.backgroundPosition = `-${puzzleX.value}px -${puzzleY.value}px`
      checkChild.value.style.backgroundSize = '400px 250px'
    }
    if (dragChild.value) {
      dragChild.value.style.transform = 'translateX(0px)'
    }
  }, 50)
}

// 鼠标按下事件
const dragMouseDown = (event) => {
  if (verifySuccess.value) return
  isDragging.value = true
  startX.value = event.pageX
  document.addEventListener('mousemove', dragMouseMove)
  document.addEventListener('mouseup', dragMouseUp)
}

// 触摸开始事件
const dragTouchStart = (event) => {
  if (verifySuccess.value) return
  isDragging.value = true
  startX.value = event.touches[0].pageX
  document.addEventListener('touchmove', dragTouchMove)
  document.addEventListener('touchend', dragTouchEnd)
}

// 鼠标移动事件
const dragMouseMove = (event) => {
  if (!isDragging.value) return
  
  const moveX = event.pageX - startX.value
  
  if (moveX < 0 || moveX > 350) {
    return
  }
  
  currentX.value = moveX
  dragBgWidth.value = moveX
  dragChild.value.style.transform = `translateX(${moveX}px)`
  checkChild.value.style.transform = `translateX(${moveX}px)`
}

// 触摸移动事件
const dragTouchMove = (event) => {
  if (!isDragging.value) return
  
  const moveX = event.touches[0].pageX - startX.value
  
  if (moveX < 0 || moveX > 350) {
    return
  }
  
  currentX.value = moveX
  dragBgWidth.value = moveX
  dragChild.value.style.transform = `translateX(${moveX}px)`
  checkChild.value.style.transform = `translateX(${moveX}px)`
}

// 鼠标松开事件
const dragMouseUp = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', dragMouseMove)
  document.removeEventListener('mouseup', dragMouseUp)
  checkVerify()
}

// 触摸结束事件
const dragTouchEnd = () => {
  isDragging.value = false
  document.removeEventListener('touchmove', dragTouchMove)
  document.removeEventListener('touchend', dragTouchEnd)
  checkVerify()
}

// 检查验证结果
const checkVerify = () => {
  const tolerance = 10 // 容差范围
  
  if (Math.abs(currentX.value - puzzleX.value) <= tolerance) {
    // 验证成功
    verifySuccess.value = true
    tipText.value = '验证成功'
    
    setTimeout(() => {
      emit('success')
      close()
    }, 800)
  } else {
    // 验证失败，重置
    tipText.value = '验证失败，请重试'
    dragChild.value.style.transition = 'transform 0.5s ease-in-out'
    checkChild.value.style.transition = 'transform 0.5s ease-in-out'
    
    setTimeout(() => {
      dragChild.value.style.transform = 'translateX(0px)'
      checkChild.value.style.transform = 'translateX(0px)'
      dragBgWidth.value = 0
      currentX.value = 0
      
      setTimeout(() => {
        dragChild.value.style.transition = ''
        checkChild.value.style.transition = ''
        tipText.value = '按住左边按钮向右拖动完成验证'
      }, 500)
    }, 300)
  }
}

// 关闭弹窗
const close = () => {
  emit('close')
}

// 监听visible变化，重新初始化
onMounted(() => {
  if (props.visible) {
    initPuzzle()
  }
})

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', dragMouseMove)
  document.removeEventListener('mouseup', dragMouseUp)
  document.removeEventListener('touchmove', dragTouchMove)
  document.removeEventListener('touchend', dragTouchEnd)
})

// 暴露初始化方法
defineExpose({
  initPuzzle
})
</script>

<style scoped>
.puzzle-verify-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.puzzle-verify-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.puzzle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.close-icon {
  cursor: pointer;
  font-size: 20px;
  color: #999;
}

.close-icon:hover {
  color: #333;
}

/* 图形拼图验证码 */
.check {
  width: 400px;
  height: 250px;
  background-repeat: no-repeat;
  background-size: 100% 100%;
  position: relative;
  border-radius: 4px;
  overflow: hidden;
  border: 2px solid #e4e7eb;
}

.check-before {
  content: '';
  width: 50px;
  height: 50px;
  background: rgba(0, 0, 0, 0.5);
  border: 2px solid #fff;
  position: absolute;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
  border-radius: 2px;
}

.check-child {
  width: 50px;
  height: 50px;
  border: 2px solid #fff;
  position: absolute;
  left: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: move;
  border-radius: 2px;
  background-repeat: no-repeat;
}

/* 拖动条 */
.drag {
  width: 400px;
  height: 50px;
  background-color: #f7f9fa;
  margin-top: 15px;
  position: relative;
  border-radius: 4px;
  border: 1px solid #e4e7eb;
}

.drag-bg {
  height: 100%;
  background-color: #d1e9ff;
  border-radius: 4px 0 0 4px;
  transition: background-color 0.2s;
}

/* 可拖动的盒子 */
.drag-child {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 10;
  position: absolute;
  top: 0;
  left: 0;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: box-shadow 0.2s;
}

.drag-child:active {
  cursor: grabbing;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.drag-child .el-icon {
  font-size: 24px;
  color: #fff;
}

/* 提示文字说明 */
.drag-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 14px;
  color: #45494c;
  user-select: none;
  z-index: 1;
  position: absolute;
  top: 0;
  left: 0;
}

.success-tips {
  color: #67c23a;
  font-weight: bold;
}

.success-tips .el-icon {
  margin-right: 5px;
  font-size: 18px;
}

@keyframes move {
  to {
    transform: translateX(0);
  }
}
</style>

<template>
  <div class="exam-prep-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>考前准备</h1>
      <p class="subtitle">请仔细阅读考试信息和规则，完成手机监控连接后开始考试</p>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：考试信息和规则 -->
      <el-col :span="16">
        <!-- 考试信息卡片 -->
        <div class="info-card">
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>考试信息</span>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <div class="info-item">
                <label>考试名称</label>
                <span>{{ examInfo.name || '--' }}</span>
              </div>
              <div class="info-item">
                <label>考试科目</label>
                <span>{{ examInfo.courseName || '--' }}</span>
              </div>
              <div class="info-item">
                <label>授课教师</label>
                <span>{{ examInfo.teacherName || '--' }}</span>
              </div>
              <div class="info-item">
                <label>考试时长</label>
                <span class="highlight">{{ examInfo.duration || '--' }} 分钟</span>
              </div>
              <div class="info-item">
                <label>开始时间</label>
                <span>{{ examInfo.startTime || '--' }}</span>
              </div>
              <div class="info-item">
                <label>结束时间</label>
                <span>{{ examInfo.endTime || '--' }}</span>
              </div>
              <div class="info-item">
                <label>题目数量</label>
                <span>{{ examInfo.questionCount || '--' }} 题</span>
              </div>
              <div class="info-item">
                <label>试卷总分</label>
                <span class="highlight">{{ examInfo.totalScore || '--' }} 分</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 考试规则卡片 -->
        <div class="rules-card">
          <div class="card-header">
            <el-icon><Warning /></el-icon>
            <span>考试规则</span>
          </div>
          <div class="card-body">
            <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
              <template #title>
                {{ examInfo.weatherDualCamera ? '本次考试采用双机位监控，请认真阅读以下规则' : '请认真阅读以下考试规则' }}
              </template>
            </el-alert>
            <ul class="rules-list">
              <li v-if="examInfo.weatherDualCamera">
                <el-icon class="rule-icon"><Monitor /></el-icon>
                <span><strong>双机位监控</strong>：电脑摄像头进行人脸识别，手机摄像头监控考试环境</span>
              </li>
              <li v-else-if="examInfo.weatherFace">
                <el-icon class="rule-icon"><Monitor /></el-icon>
                <span><strong>人脸识别</strong>：电脑摄像头进行人脸识别验证</span>
              </li>
              <li v-if="examInfo.weatherDualCamera">
                <el-icon class="rule-icon"><Iphone /></el-icon>
                <span><strong>手机放置</strong>：请将手机放置于侧后方45°位置，确保能同时拍摄到电脑屏幕和考生</span>
              </li>
              <li v-if="examInfo.weatherFace || examInfo.weatherDualCamera">
                <el-icon class="rule-icon"><Timer /></el-icon>
                <span><strong>人脸验证</strong>：考试过程中每60秒进行一次人脸识别验证</span>
              </li>
              <li v-if="examInfo.weatherFace || examInfo.weatherDualCamera">
                <el-icon class="rule-icon"><CircleClose /></el-icon>
                <span><strong>违规处理</strong>：连续3次人脸验证失败将自动提交试卷，成绩记为0分并标记作弊</span>
              </li>
              <li v-if="examInfo.weatherCopy">
                <el-icon class="rule-icon"><Warning /></el-icon>
                <span><strong>防复制粘贴</strong>：本次考试禁止复制粘贴</span>
              </li>
              <li v-if="examInfo.weatherScreenSwitch">
                <el-icon class="rule-icon"><Connection /></el-icon>
                <span><strong>切屏监控</strong>：禁止切屏，3次自动违规交卷</span>
              </li>
              <li>
                <el-icon class="rule-icon"><Connection /></el-icon>
                <span><strong>网络要求</strong>：请确保网络畅通</span>
              </li>
              <li>
                <el-icon class="rule-icon"><Clock /></el-icon>
                <span><strong>时间提醒</strong>：考试时间结束后系统将自动提交试卷</span>
              </li>
            </ul>
          </div>
        </div>
      </el-col>

      <!-- 右侧：摄像头检测和操作 -->
      <el-col :span="8">
        <!-- 人脸识别验证卡片 -->
        <div class="camera-card" v-if="examInfo.weatherFace || examInfo.weatherDualCamera">
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>人脸识别验证</span>
            <el-tag v-if="faceVerified" type="success" size="small" style="margin-left: auto">已验证</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left: auto">待验证</el-tag>
          </div>
          <div class="card-body">
            <FaceVerify
              :examId="testId"
              :studentId="studentId"
              @verified="onFaceVerified"
            />
          </div>
        </div>

        <!-- 手机连接卡片 -->
        <div class="connect-card" v-if="examInfo.weatherDualCamera">
          <div class="card-header">
            <el-icon><Iphone /></el-icon>
            <span>连接手机监控</span>
            <el-tag v-if="phoneConnected" type="success" size="small" style="margin-left: auto">已连接</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left: auto">未连接</el-tag>
          </div>
          <div class="card-body">
            <PhoneMonitor
              :examId="testId"
              :studentId="studentId"
              @connected="onPhoneConnected"
            />
            <div class="connect-tips">
              <p><strong>连接步骤：</strong></p>
              <ol>
                <li>使用手机扫描上方二维码</li>
                <li>允许浏览器访问摄像头权限</li>
                <li>等待连接成功提示</li>
                <li>将手机固定在合适位置</li>
              </ol>
            </div>
          </div>
        </div>

        <!-- 开始考试按钮 -->
        <div class="action-card">
          <el-checkbox v-model="rulesAccepted" style="margin-bottom: 16px">
            我已阅读并同意遵守以上考试规则
          </el-checkbox>
          
          <el-button 
            type="primary" 
            plain
            size="large" 
            :disabled="!canStartExam"
            :loading="startButtonText.includes('等待')"
            @click="startExam"
            style="width: 100%"
          >
            {{ startButtonText }}
          </el-button>

          <div v-if="!phoneConnected && examInfo.weatherDualCamera" class="action-hint">
            <el-icon><InfoFilled /></el-icon>
            请先完成手机监控连接
          </div>
          <div v-else-if="!faceVerified && (examInfo.weatherFace || examInfo.weatherDualCamera)" class="action-hint">
            <el-icon><InfoFilled /></el-icon>
            请先完成人脸识别验证
          </div>
          <div v-else-if="!rulesAccepted" class="action-hint">
            <el-icon><InfoFilled /></el-icon>
            请勾选同意考试规则
          </div>
        </div>

        <!-- 返回按钮 -->
        <el-button 
          type="default" 
          plain
          size="large" 
          @click="goBack"
          style="width: 100%; margin-top: 12px"
        >
          返回首页
        </el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, Warning, Monitor, Iphone, Timer, 
  CircleClose, Connection, Clock, InfoFilled 
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import PhoneMonitor from '@/components/PhoneMonitor.vue'
import FaceVerify from '@/components/FaceVerify.vue'

const router = useRouter()
const route = useRoute()

// 考试ID
const testId = ref(route.query.testId || '')
// 学生信息
const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
const studentId = ref(user.id || '')

// 考试信息
const examInfo = ref({})
// 手机连接状态
const phoneConnected = ref(false)
// 人脸识别验证状态
const faceVerified = ref(false)
// 规则确认
const rulesAccepted = ref(false)
// 加载状态
const loading = ref(false)

// 是否可以开始考试
const canStartExam = computed(() => {
  // 根据考试配置决定是否需要人脸识别和手机监控
  const needFace = examInfo.value.weatherFace || false
  const needDualCamera = examInfo.value.weatherDualCamera || false
  
  // 如果需要人脸识别或双机位监考，则必须通过人脸验证
  const faceOk = (needFace || needDualCamera) ? faceVerified.value : true
  
  // 如果需要双机位监考，则必须连接手机
  const phoneOk = needDualCamera ? phoneConnected.value : true
  
  return faceOk && phoneOk && rulesAccepted.value
})

// 开始按钮文字
const startButtonText = computed(() => {
  const needFace = examInfo.value.weatherFace || false
  const needDualCamera = examInfo.value.weatherDualCamera || false
  
  if ((needFace || needDualCamera) && !faceVerified.value) return '等待人脸验证...'
  if (needDualCamera && !phoneConnected.value) return '等待手机连接...'
  if (!rulesAccepted.value) return '请先同意考试规则'
  return '开始考试'
})

// 人脸识别验证成功回调
const onFaceVerified = () => {
  faceVerified.value = true
  ElMessage.success('人脸验证成功！')
}

// 加载考试信息
const loadExamInfo = async () => {
  if (!testId.value) {
    ElMessage.error('考试ID无效')
    return
  }
  
  loading.value = true
  try {
    const res = await request.get('/test/selectById/' + testId.value)
    if (res.code === '200') {
      const test = res.data
      examInfo.value = {
        name: test.name,
        courseName: test.testPaper?.courseName,
        teacherName: test.testPaper?.teacherName,
        duration: test.duration,
        startTime: test.start,
        endTime: test.end,
        questionCount: test.testPaper?.questions?.length || 0,
        totalScore: test.testPaper?.totalScore || 100,
        weatherFace: test.weatherFace || false,
        weatherDualCamera: test.weatherDualCamera || false,
        weatherCopy: test.weatherCopy || false,
        weatherScreenSwitch: test.weatherScreenSwitch || false
      }
    } else {
      ElMessage.error(res.msg || '加载考试信息失败')
    }
  } catch (error) {
    ElMessage.error('加载考试信息失败')
  } finally {
    loading.value = false
  }
}

// 手机连接成功回调
const onPhoneConnected = () => {
  phoneConnected.value = true
  ElMessage.success('手机监控连接成功！')
}

// 开始考试
const startExam = () => {
  if (!canStartExam.value) return
  
  // 在进入考试前再次检查是否已提交
  request.get('/testPaper/check/' + testId.value).then(res => {
    if (res.code === '200') {
      // 跳转到考试页面
      router.push('/front/testPaper?testId=' + testId.value)
    } else {
      ElMessage.error(res.msg || '该门考试您已经提交过试卷了，入口已关闭')
      // 跳转回首页
      setTimeout(() => {
        goBack()
      }, 1500)
    }
  })
}

// 返回首页
const goBack = () => {
  router.push('/front/home')
}

onMounted(() => {
  loadExamInfo()
})
</script>

<style scoped>
.exam-prep-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-header .subtitle {
  color: #6b7280;
  font-size: 15px;
}

/* 卡片通用样式 */
.info-card, .rules-card, .connect-card, .action-card, .camera-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.rules-card .card-header {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.connect-card .card-header {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.camera-card .card-header {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.card-body {
  padding: 20px;
}

/* 考试信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 13px;
  color: #6b7280;
}

.info-item span {
  font-size: 15px;
  color: #1f2937;
  font-weight: 500;
}

.info-item .highlight {
  color: #667eea;
  font-weight: 600;
}

/* 考试规则列表 */
.rules-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rules-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.rules-list li:last-child {
  border-bottom: none;
}

.rule-icon {
  color: #f59e0b;
  font-size: 18px;
  margin-top: 2px;
}

.rules-list li span {
  color: #374151;
  line-height: 1.5;
}

/* 连接提示 */
.connect-tips {
  margin-top: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.connect-tips p {
  margin: 0 0 8px 0;
  color: #374151;
}

.connect-tips ol {
  margin: 0;
  padding-left: 20px;
  color: #6b7280;
}

.connect-tips ol li {
  margin-bottom: 6px;
}

/* 操作卡片 */
.action-card {
  padding: 20px;
}

.action-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  color: #6b7280;
  font-size: 13px;
  justify-content: center;
}
</style>

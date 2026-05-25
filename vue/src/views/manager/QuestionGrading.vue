<template>
  <div class="question-grading-page">
    <!-- 顶部导航栏 -->
    <div class="header-bar">
      <el-button type="primary" plain icon="ArrowLeft" @click="goBack">返回</el-button>
      <div class="header-info">
        <div class="question-info">
          <span class="label">当前学生：</span>
          <span class="value">{{ data.currentStudent?.studentName || '-' }}</span>
        </div>
        <div class="progress-info">
          <span class="label">批改进度：</span>
          <span class="value">{{ getGradedCount() }}/{{ data.studentList.length }}</span>
        </div>
      </div>
      <div>
        <el-button 
          type="default"
          plain
          @click="goToPrevious"
          :disabled="data.currentIndex === 0"
        >
          上一份
        </el-button>
        <el-button 
          type="default"
          plain
          @click="goToNext"
          :disabled="data.currentIndex === data.studentList.length - 1"
        >
          下一份
        </el-button>
        <el-button 
          type="success" 
          plain
          @click="saveScore"
          :loading="data.submitting"
        >
          保存
        </el-button>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-container" v-if="data.currentStudent">
      <div class="question-card">
        <!-- 题目头部 -->
        <div class="question-header">
          <div class="question-title">
            <el-tag 
              size="large" 
              :type="getQuestionTypeColor(data.questionInfo?.questionTypeName)"
              effect="dark"
            >
              {{ data.questionInfo?.questionTypeName }}
            </el-tag>
            <span class="question-score">{{ data.questionInfo?.typeScore }} 分</span>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="question-body">
          <div class="question-stem">
            <RichTextContent :content="data.questionInfo?.questionStem" />
          </div>
          
          <!-- 如果是选择题，显示选项 -->
          <div v-if="isChoiceQuestion(data.questionInfo?.questionTypeName) && data.questionInfo?.choiceList" class="choices">
            <div 
              v-for="choice in data.questionInfo.choiceList" 
              :key="choice.id"
              class="choice-item"
              style="display: flex; align-items: flex-start;"
            >
              <span class="choice-label">{{ choice.identification }}.</span>
              <RichTextContent :content="choice.content" style="flex: 1; margin-left: 5px;" />
            </div>
          </div>

          <!-- 如果是复合题，显示所有小问 -->
          <div v-if="data.questionInfo?.questionTypeVariety === 'composite' && data.questionInfo?.questionList" class="sub-questions">
            <div v-for="(subQ, subIdx) in data.questionInfo.questionList" :key="subQ.id" class="sub-question-item">
              <div class="sub-question-header">
                <span class="sub-question-no">小问 {{ subIdx + 1 }}</span>
                <el-tag size="small" :type="getQuestionTypeColor(subQ.questionTypeName)">
                  {{ subQ.questionTypeName }}
                </el-tag>
                <span class="sub-question-score">{{ subQ.typeScore }} 分</span>
              </div>
              <div class="sub-question-stem">
                <RichTextContent :content="subQ.questionStem" />
              </div>
              
              <!-- 小问的选项 -->
              <div v-if="isChoiceQuestion(subQ.questionTypeName) && subQ.choiceList" class="choices sub-choices">
                <div 
                  v-for="choice in subQ.choiceList" 
                  :key="choice.id"
                  class="choice-item"
                  style="display: flex; align-items: flex-start;"
                >
                  <span class="choice-label">{{ choice.identification }}.</span>
                  <RichTextContent :content="choice.content" style="flex: 1; margin-left: 5px;" />
                </div>
              </div>

              <!-- 小问标准答案 -->
              <div class="answer-section">
                <div class="answer-label">标准答案：</div>
                <div class="answer-content">{{ formatAnswer(subQ.answer, subQ) }}</div>
              </div>

              <!-- 小问学生答案 -->
              <div class="student-answer-section">
                <div class="answer-label">学生答案：</div>
                <div class="answer-content">
                  <RichTextContent :content="formatStudentAnswer(subQ.id, subQ) || '（未作答）'" />
                </div>
              </div>

              <!-- 小问评分（只有主观题才能评分） -->
              <div class="sub-scoring-area" v-if="!isObjectiveQuestion(subQ.questionTypeName)">
                <span class="score-label">得分：</span>
                <el-input-number 
                  v-model="data.currentStudent.subScores[subIdx]" 
                  :min="0" 
                  :max="subQ.typeScore" 
                  :step="0.5" 
                  :precision="1"
                  controls-position="right"
                  size="default"
                />
                <span class="score-unit">/ {{ subQ.typeScore }} 分</span>
              </div>
              <div v-else class="auto-score-hint">
                <el-tag type="info" size="small">客观题自动判分：{{ data.currentStudent.subScores[subIdx] || 0 }} 分</el-tag>
              </div>
            </div>
          </div>

          <!-- 非复合题的标准答案 -->
          <div v-if="data.questionInfo?.questionTypeVariety !== 'composite'" class="answer-section">
            <div class="answer-label">标准答案：</div>
            <div class="answer-content">{{ formatAnswer(data.questionInfo?.answer, data.questionInfo) }}</div>
          </div>

          <!-- 非复合题的学生答案 -->
          <div v-if="data.questionInfo?.questionTypeVariety !== 'composite'" class="student-answer-section">
            <div class="answer-label">学生答案：</div>
            <div class="answer-content">
              <RichTextContent :content="formatStudentAnswer(data.questionInfo?.id, data.questionInfo) || '（未作答）'" />
            </div>
          </div>
        </div>

        <!-- 评分区域（非复合题） -->
        <div v-if="data.questionInfo?.questionTypeVariety !== 'composite'" class="scoring-area">
          <template v-if="!isObjectiveQuestion(data.questionInfo?.questionTypeName)">
            <span class="score-label">得分：</span>
            <el-input-number 
              v-model="data.currentStudent.score" 
              :min="0" 
              :max="data.questionInfo?.typeScore" 
              :step="0.5" 
              :precision="1"
              controls-position="right"
              size="large"
            />
            <span class="score-unit">/ {{ data.questionInfo?.typeScore }} 分</span>
          </template>
          <template v-else>
            <el-tag type="info" size="large">客观题自动判分：{{ data.currentStudent.score || 0 }} 分</el-tag>
          </template>
        </div>
        
        <!-- 复合题总分显示 -->
        <div v-else class="composite-total-score">
          <span class="score-label">总分：</span>
          <span class="total-score-value">{{ calculateCompositeTotal() }}</span>
          <span class="score-unit">/ {{ data.questionInfo?.typeScore }} 分</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import RichTextContent from '@/components/RichTextContent.vue'

const route = useRoute()
const router = useRouter()

const data = reactive({
  testId: route.query.testId,
  questionId: route.query.questionId,
  questionTypeName: route.query.questionTypeName,
  questionTypeVariety: route.query.questionTypeVariety,
  questionInfo: null,
  studentList: [],
  currentIndex: 0,
  currentStudent: null,
  submitting: false
})

// 获取题目类型颜色
const getQuestionTypeColor = (typeName) => {
  const colorMap = {
    '单选': 'primary',
    '多选': 'success',
    '判断': 'warning',
    '填空': 'info',
    '简答': 'danger',
    '编程': 'danger',
    '复合': 'info',
    '名词解析': 'danger',
    '论述': 'danger',
    '计算': 'danger',
    '程序': 'danger',
    '资料': 'info',
    '完形填空': 'info',
    '阅读理解': 'info',
    '综合': 'info'
  }
  return colorMap[typeName] || 'info'
}

// 判断是否为选择题
const isChoiceQuestion = (typeName) => {
  return ['单选', '多选', '判断'].includes(typeName)
}

// 判断是否为客观题
const isObjectiveQuestion = (typeName) => {
  return ['单选', '多选', '判断'].includes(typeName)
}

// 格式化答案
const formatAnswer = (answer, question) => {
  if (!answer) return '-'
  
  if (isChoiceQuestion(question?.questionTypeName)) {
    // 选择题：显示选项标识
    if (question.questionTypeName === '多选') {
      // 多选题，answer可能是数组或逗号分隔的字符串
      const answerArray = Array.isArray(answer) ? answer : (answer.split(',') || [])
      return answerArray.map(id => {
        const choice = question.choiceList?.find(c => c.id == id)
        return choice?.identification || id
      }).join(', ')
    } else {
      // 单选或判断，answer是单个选项ID
      const choice = question.choiceList?.find(c => c.id == answer)
      return choice?.identification || answer
    }
  }
  
  return answer
}

// 格式化学生答案
const formatStudentAnswer = (questionId, question) => {
  if (!data.currentStudent) return ''
  
  // 检查是否为空答案（包括富文本空内容）
  const isEmptyAnswer = (ans) => {
    if (!ans) return true
    // 如果不是字符串类型，先转换
    const ansStr = typeof ans === 'string' ? ans : String(ans)
    // 检查是否包含公式、图片等富文本元素
    if (ansStr.includes('data-w-e-type="formula"') || ansStr.includes('<img')) {
      return false
    }
    // 移除HTML标签和空白字符，检查是否为空
    const text = ansStr.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim()
    return text === ''
  }
  
  // 复合题
  if (data.questionInfo?.questionTypeVariety === 'composite') {
    const subIdx = data.questionInfo.questionList?.findIndex(q => q.id == questionId)
    if (subIdx === -1) return ''
    
    const subAnswer = data.currentStudent.subAnswers?.[subIdx]
    if (isEmptyAnswer(subAnswer)) return ''
    
    const subQuestion = data.questionInfo.questionList[subIdx]
    
    if (isChoiceQuestion(subQuestion.questionTypeName)) {
      if (subQuestion.questionTypeName === '多选') {
        const answerArray = Array.isArray(subAnswer) ? subAnswer : (subAnswer.split(',') || [])
        return answerArray.map(id => {
          const choice = subQuestion.choiceList?.find(c => c.id == id)
          return choice?.identification || id
        }).join(', ')
      } else {
        const choice = subQuestion.choiceList?.find(c => c.id == subAnswer)
        return choice?.identification || subAnswer
      }
    }
    
    return subAnswer
  }
  
  // 普通题
  const answer = data.currentStudent.answer
  if (isEmptyAnswer(answer)) return ''
  
  return formatAnswer(answer, question)
}

// 计算复合题总分
const calculateCompositeTotal = () => {
  if (!data.currentStudent?.subScores) return 0
  return data.currentStudent.subScores.reduce((sum, score) => sum + (score || 0), 0)
}

// 计算已批改数量
const getGradedCount = () => {
  if (!data.studentList || data.studentList.length === 0) return 0
  
  return data.studentList.filter(student => {
    // 复合题和普通题都只看父题目的status（批改复合题时会同时标记父题目和子题目）
    return student.status === '已批改'
  }).length
}

// 静默保存（不弹确认框，不提示成功）
const saveScoreQuietly = () => {
  return new Promise((resolve, reject) => {
    const params = {
      testId: data.testId,
      questionId: data.questionId,
      studentId: data.currentStudent.studentId,
      scoreId: data.currentStudent.scoreId,
      questionTypeVariety: data.questionInfo.questionTypeVariety
    }
    
    // 如果是复合题，提交子题分数
    if (data.questionInfo.questionTypeVariety === 'composite') {
      params.subScores = data.currentStudent.subScores
      params.subQuestionIds = data.questionInfo.questionList.map(q => q.id)
    } else {
      params.score = data.currentStudent.score
    }
    
    request.post('/score/saveQuestionScore', params).then(res => {
      if (res.code === '200') {
        // 更新当前学生的status为已批改
        if (data.questionInfo.questionTypeVariety === 'composite') {
          // 复合题：将所有子题标记为已批改（因为后端会批量更新所有子题）
          if (data.currentStudent.subAnswers) {
            data.currentStudent.subAnswers.forEach(subAns => {
              subAns.status = '已批改'
            })
          }
        } else {
          data.currentStudent.status = '已批改'
        }
        resolve()
      } else {
        ElMessage.error(res.msg)
        reject()
      }
    }).catch(() => {
      reject()
    })
  })
}

// 加载题目信息和学生答案列表
const loadData = () => {
  request.get('/score/selectAnswersByQuestion', {
    params: {
      testId: data.testId,
      questionId: data.questionId
    }
  }).then(res => {
    if (res.code === '200') {
      data.questionInfo = res.data.questionInfo
      data.studentList = res.data.studentList || []
      
      // 初始化学生答案数据
      data.studentList.forEach(student => {
        if (data.questionInfo.questionTypeVariety === 'composite') {
          // 复合题：初始化子题分数数组
          if (!student.subScores) {
            student.subScores = data.questionInfo.questionList.map((subQ, idx) => {
              return student.subAnswers?.[idx]?.score || 0
            })
          }
        } else {
          // 普通题
          if (student.score === undefined) {
            student.score = 0
          }
        }
      })
      
      if (data.studentList.length > 0) {
        // 查找第一个未批改的学生（复合题和普通题都只看父题目status）
        let ungradedIndex = data.studentList.findIndex(student => student.status !== '已批改')
        
        // 如果有未批改的，显示第一个未批改的；否则显示第一份
        data.currentIndex = ungradedIndex >= 0 ? ungradedIndex : 0
        data.currentStudent = data.studentList[data.currentIndex]
      }
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 上一份
const goToPrevious = () => {
  if (data.currentIndex > 0) {
    ElMessageBox.confirm('确认保存当前学生的评分并跳转到上一份吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      saveScoreQuietly().then(() => {
        data.currentIndex--
        data.currentStudent = data.studentList[data.currentIndex]
      })
    }).catch(() => {})
  }
}

// 下一份
const goToNext = () => {
  if (data.currentIndex < data.studentList.length - 1) {
    ElMessageBox.confirm('确认保存当前学生的评分并跳转到下一份吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      saveScoreQuietly().then(() => {
        data.currentIndex++
        data.currentStudent = data.studentList[data.currentIndex]
      })
    }).catch(() => {})
  }
}

// 保存成绩
const saveScore = () => {
  ElMessageBox.confirm('确认保存当前学生的评分吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    data.submitting = true
    
    const params = {
      testId: data.testId,
      questionId: data.questionId,
      studentId: data.currentStudent.studentId,
      scoreId: data.currentStudent.scoreId,
      questionTypeVariety: data.questionInfo.questionTypeVariety
    }
    
    // 如果是复合题，提交子题分数
    if (data.questionInfo.questionTypeVariety === 'composite') {
      params.subScores = data.currentStudent.subScores
      params.subQuestionIds = data.questionInfo.questionList.map(q => q.id)
    } else {
      params.score = data.currentStudent.score
    }
    
    request.post('/score/saveQuestionScore', params).then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        // 更新当前学生的status为已批改
        if (data.questionInfo.questionTypeVariety === 'composite') {
          data.currentStudent.subAnswers.forEach(subAns => {
            subAns.status = '已批改'
          })
        } else {
          data.currentStudent.status = '已批改'
        }
      } else {
        ElMessage.error(res.msg)
      }
    }).finally(() => {
      data.submitting = false
    })
  }).catch(() => {})
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.question-grading-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header-bar {
  background: white;
  padding: 16px 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
  margin-left: 24px;
}

.question-info,
.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #606266;
  font-size: 14px;
}

.value {
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.main-container {
  flex: 1;
  overflow: auto;
  padding: 0 24px 24px;
}

.question-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.question-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.question-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-score {
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.question-body {
  margin-bottom: 24px;
}

.question-stem {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.choices {
  margin: 16px 0;
}

.choice-item {
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
  border-radius: 6px;
  display: flex;
  gap: 8px;
}

.choice-label {
  font-weight: 600;
  color: #409eff;
  min-width: 30px;
}

.choice-content {
  flex: 1;
  color: #606266;
}

.sub-questions {
  margin-top: 20px;
}

.sub-question-item {
  padding: 20px;
  margin-bottom: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.sub-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.sub-question-no {
  font-weight: 600;
  color: #409eff;
}

.sub-question-score {
  color: #67c23a;
  font-weight: 500;
  margin-left: auto;
}

.sub-question-stem {
  font-size: 15px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 12px;
}

.sub-choices {
  margin: 12px 0;
}

.answer-section,
.student-answer-section {
  margin: 16px 0;
  padding: 12px;
  background: white;
  border-radius: 6px;
}

.student-answer-section {
  background: #e8f4ff;
}

.answer-label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.answer-content {
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
}

.scoring-area,
.sub-scoring-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: #f0f9ff;
  border-radius: 8px;
  margin-top: 16px;
}

.scoring-area {
  justify-content: center;
}

.score-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.score-unit {
  color: #909399;
  margin-left: 8px;
}

.auto-score-hint {
  margin-top: 12px;
}

.composite-total-score {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  background: #f0f9ff;
  border-radius: 8px;
  margin-top: 20px;
  border: 2px solid #409eff;
}

.total-score-value {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}
</style>

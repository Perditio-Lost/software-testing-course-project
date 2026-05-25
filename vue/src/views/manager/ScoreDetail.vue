<template>
  <div class="score-detail-page">
    <!-- 顶部导航栏 -->
    <div class="header-bar">
      <el-button type="primary" plain icon="ArrowLeft" @click="goBack">返回</el-button>
      <div class="header-info">
        <div class="student-info">
          <el-icon><User /></el-icon>
          <span>{{ data.studentName }}</span>
        </div>
        <div class="paper-info">
          <span class="label">试卷：</span>
          <span class="value">{{ data.paperName }}</span>
        </div>
        <div class="score-info">
          <span class="label">总分：</span>
          <span class="value">{{ data.totalScore }}/{{ data.fullScore }}</span>
        </div>
        <el-tag 
          v-if="data.isGraded" 
          type="success" 
          size="large"
          effect="dark"
        >
          已批改
        </el-tag>
      </div>
      <el-button 
        type="success" 
        plain
        size="large"
        icon="Check" 
        @click="submitScore"
        :loading="data.submitting"
      >
        {{ data.isGraded ? '重新提交阅卷' : '提交阅卷' }}
      </el-button>
    </div>

    <!-- 主体内容区 -->
    <div class="main-container">
      <!-- 左侧答题卡 -->
      <div class="answer-sheet">
        <div class="sheet-header">
          <h3>答题卡</h3>
          <div class="legend">
            <span class="legend-item">
              <span class="dot correct"></span>正确
            </span>
            <span class="legend-item">
              <span class="dot wrong"></span>错误
            </span>
            <span class="legend-item">
              <span class="dot partial"></span>部分正确
            </span>
            <span class="legend-item">
              <span class="dot unanswered"></span>未批改
            </span>
          </div>
        </div>
        
        <div class="sheet-content">
          <div v-for="group in groupedAnswers" :key="group.typeName" class="question-type-group">
            <div class="type-title">{{ group.typeName }}</div>
            <div class="question-numbers">
              <div 
                v-for="(item, idx) in group.questions" 
                :key="item.id"
                class="question-number"
                :class="[
                  { active: data.currentIndex === item.globalIndex },
                  getQuestionStatus(item)
                ]"
                @click="scrollToQuestion(item.globalIndex)"
              >
                {{ item.globalIndex + 1 }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧题目内容 -->
      <div class="question-area">
        <el-scrollbar ref="scrollbarRef">
          <div class="question-list">
            <div 
              v-for="(item, index) in data.answerData" 
              :key="item.id"
              :ref="el => questionRefs[index] = el"
              class="question-card"
              :class="{ active: data.currentIndex === index }"
            >
              <!-- 题目头部 -->
              <div class="question-header">
                <div class="question-title">
                  <el-tag 
                    size="large" 
                    :type="getQuestionTypeColor(item.question?.questionTypeName)"
                    effect="dark"
                  >
                    {{ item.question?.questionTypeName }}
                  </el-tag>
                  <span class="question-no">第 {{ index + 1 }} 题</span>
                </div>
                <div class="question-score-info">
                  <span class="full-score">{{ item.questionScore }} 分</span>
                </div>
              </div>

              <!-- 题目内容 -->
              <div class="question-body">
                <div class="question-stem">
                  <RichTextContent :content="item.question?.questionStem" />
                </div>
                
                <!-- 如果是选择题，显示选项 -->
                <div v-if="isChoiceQuestion(item.question?.questionTypeName) && item.question?.choiceList" class="choices">
                  <div 
                    v-for="choice in item.question.choiceList" 
                    :key="choice.id"
                    class="choice-item"
                    :class="getChoiceClass(choice, item)"
                    style="display: flex; align-items: flex-start;"
                  >
                    <span class="choice-label">{{ choice.choiceLabel }}</span>
                    <RichTextContent :content="choice.choiceContent" style="flex: 1; margin-left: 5px;" />
                    <el-icon v-if="isCorrectChoice(choice, item.question)" class="correct-icon">
                      <CircleCheck />
                    </el-icon>
                  </div>
                </div>

                <!-- 如果是复合题，显示所有小问 -->
                <div v-if="item.question?.questionTypeVariety === 'composite' && item.question?.questionList" class="sub-questions">
                  <div v-for="(subQ, subIdx) in item.question.questionList" :key="subQ.id" class="sub-question-item">
                    <div class="sub-question-header">
                      <span class="sub-question-no">小问 {{ subIdx + 1 }}</span>
                      <el-tag size="small" :type="getQuestionTypeColor(subQ.questionTypeName)">
                        {{ subQ.questionTypeName }}
                      </el-tag>
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
                        :class="getSubQuestionChoiceClass(choice, subQ, item)"
                        style="display: flex; align-items: flex-start;"
                      >
                        <span class="choice-label">{{ choice.choiceLabel }}</span>
                        <RichTextContent :content="choice.choiceContent" style="flex: 1; margin-left: 5px;" />
                        <el-icon v-if="isCorrectChoice(choice, subQ)" class="correct-icon">
                          <CircleCheck />
                        </el-icon>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 答案区域 -->
              <div class="answer-area">
                <el-row :gutter="20">
                  <!-- 学生答案 -->
                  <el-col :span="12">
                    <div class="answer-box student-answer">
                      <div class="answer-label">
                        <el-icon><User /></el-icon>
                        <span>学生答案</span>
                      </div>
                      <div class="answer-content">
                        <template v-if="item.question?.questionTypeVariety === 'composite'">
                          <div v-if="item.subAnswers && item.subAnswers.length">
                            <div v-for="(subAns, idx) in item.subAnswers" :key="idx" class="composite-answer-item">
                              <span class="composite-answer-label">小问{{ idx + 1 }}：</span>
                              <RichTextContent :content="formatAnswerDisplay(subAns, item.question?.questionList?.[idx], false) || '（未作答）'" />
                            </div>
                          </div>
                          <div v-else>
                            <RichTextContent :content="formatAnswerDisplay(item.answer, item.question, false) || '（未作答）'" />
                          </div>
                        </template>
                        <template v-else>
                          <RichTextContent :content="formatAnswerDisplay(item.answer, item.question, false) || '（未作答）'" />
                        </template>
                      </div>
                    </div>
                  </el-col>
                  
                  <!-- 标准答案 -->
                  <el-col :span="12">
                    <div class="answer-box correct-answer">
                      <div class="answer-label">
                        <el-icon><CircleCheck /></el-icon>
                        <span>标准答案</span>
                      </div>
                      <div class="answer-content">
                        <template v-if="item.question?.questionTypeVariety === 'composite'">
                          <div v-if="item.question?.questionList && item.question.questionList.length">
                            <div v-for="(subQ, idx) in item.question.questionList" :key="idx" class="composite-answer-item">
                              <span class="composite-answer-label">小问{{ idx + 1 }}：</span>
                              <span>{{ formatAnswerDisplay(subQ.answer, subQ, true) }}</span>
                            </div>
                          </div>
                          <div v-else>{{ formatAnswerDisplay(item.question?.answer, item.question, true) }}</div>
                        </template>
                        <template v-else>
                          {{ formatAnswerDisplay(item.question?.answer, item.question, true) }}
                        </template>
                      </div>
                    </div>
                  </el-col>
                </el-row>
              </div>

              <!-- 评分区域 -->
              <div class="scoring-area">
                <!-- 如果是复合题，显示子题分数输入 -->
                <template v-if="item.question?.questionTypeVariety === 'composite'">
                  <div class="composite-scoring">
                    <div class="composite-score-title">子题评分：</div>
                    <div v-for="(subQ, subIdx) in item.question?.questionList" :key="subIdx" class="sub-question-score">
                      <span class="sub-score-label">小问{{ subIdx + 1 }}：</span>
                      <el-input-number 
                        v-model="item.subScores[subIdx]" 
                        :min="0" 
                        :max="item.subMaxScores?.[subIdx] || subQ.typeScore || 0" 
                        :step="0.5" 
                        :precision="1"
                        controls-position="right"
                        size="default"
                        :disabled="isObjectiveQuestion(subQ.questionTypeName)"
                        @change="() => { markAsGraded(item); calculateCompositeScore(item); }"
                      />
                      <span class="score-unit">/ {{ item.subMaxScores?.[subIdx] || subQ.typeScore || 0 }} 分</span>
                      <el-tag 
                        v-if="isObjectiveQuestion(subQ.questionTypeName)" 
                        type="info" 
                        size="small"
                        style="margin-left: 10px"
                      >
                        系统自动判分
                      </el-tag>
                    </div>
                    <div class="composite-total-score">
                      <span class="score-label">{{ item.question?.questionTypeName }}总分：</span>
                      <span class="total-score-value">{{ item.score }}</span>
                      <span class="score-unit">/ {{ item.questionScore }} 分</span>
                    </div>
                  </div>
                </template>
                <!-- 普通题目评分 -->
                <template v-else>
                  <div class="score-label">得分：</div>
                  <el-input-number 
                    v-model="item.score" 
                    :min="0" 
                    :max="item.questionScore" 
                    :step="0.5" 
                    :precision="1"
                    controls-position="right"
                    size="large"
                    :disabled="isObjectiveQuestion(item.question?.questionTypeName)"
                    @change="() => { markAsGraded(item); calculateTotalScore(); }"
                  />
                  <span class="score-unit">/ {{ item.questionScore }} 分</span>
                  <el-tag 
                    v-if="isObjectiveQuestion(item.question?.questionTypeName)" 
                    type="info" 
                    size="small"
                    style="margin-left: 10px"
                  >
                    系统自动判分
                  </el-tag>
                </template>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, User, Check, CircleCheck } from '@element-plus/icons-vue'
import RichTextContent from '@/components/RichTextContent.vue'

const route = useRoute()
const router = useRouter()
const scrollbarRef = ref(null)
const questionRefs = ref([])

const data = reactive({
  scoreId: null,
  studentName: '',
  paperName: '',
  courseName: '',
  answerData: [],
  totalScore: 0,
  fullScore: 0,
  currentIndex: 0,
  submitting: false,
  isGraded: false // 是否已批改
})

// 题型颜色映射
const getQuestionTypeColor = (typeName) => {
  const typeMap = {
    '单选': 'primary',
    '多选': 'success',
    '判断': 'warning',
    '填空': 'info',
    '名词解析': 'danger',
    '论述': 'danger',
    '计算': 'danger',
    '程序': 'danger'
  }
  return typeMap[typeName] || ''
}

// 判断是否为选择题
const isChoiceQuestion = (questionTypeName) => {
  return ['单选', '多选', '判断'].includes(questionTypeName)
}

// 判断是否为客观题（单选、多选、判断自动判分，不允许教师手动改分）
const isObjectiveQuestion = (questionTypeName) => {
  return ['单选', '多选', '判断'].includes(questionTypeName)
}

// 格式化题干
const formatQuestionStem = (stem) => {
  if (!stem) return ''
  return stem.replace(/\n/g, '<br/>')
}

// 格式化答案显示（根据题型转换答案格式）
const formatAnswerDisplay = (answer, question, isCorrectAnswer = false) => {
  // 检查是否为空答案（包括富文本空内容）
  if (!answer) return ''
  // 检查是否包含公式、图片等富文本元素
  if (answer.includes('data-w-e-type="formula"') || answer.includes('<img')) {
    return answer
  }
  // 移除HTML标签和空白字符，检查是否为空
  const text = answer.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim()
  if (text === '') return ''
  
  const questionType = question?.questionTypeName
  
  // 选择题、判断题：显示选项标识（A、B、C等）
  if (['单选', '多选', '判断'].includes(questionType)) {
    // 如果是多选题，在字母之间加空格
    if (questionType === '多选') {
      // 先移除所有逗号和空格，只保留字母
      const cleanAnswer = answer.replace(/[,\s]/g, '')
      if (cleanAnswer.length > 1) {
        return cleanAnswer.split('').join(', ')
      }
      return cleanAnswer
    }
    return answer
  }
  
  return answer
}

// 判断选项是否为正确答案
const isCorrectChoice = (choice, question) => {
  if (!question || !question.answer) return false
  return question.answer.includes(choice.choiceLabel)
}

// 获取选项样式类
const getChoiceClass = (choice, item) => {
  const studentAnswer = item.answer || ''
  const correctAnswer = item.question?.answer || ''
  const isStudentSelected = studentAnswer.includes(choice.choiceLabel)
  const isCorrect = correctAnswer.includes(choice.choiceLabel)
  
  if (isStudentSelected && isCorrect) {
    return 'correct'
  } else if (isStudentSelected && !isCorrect) {
    return 'wrong'
  } else if (!isStudentSelected && isCorrect) {
    return 'correct-not-selected'
  }
  return ''
}

// 获取复合题小问的选项样式类
const getSubQuestionChoiceClass = (choice, subQuestion, parentItem) => {
  // 从复合题的子答案数组中获取对应小问的答案
  const subQuestionIndex = parentItem.question?.questionList?.findIndex(q => q.id === subQuestion.id)
  const studentAnswer = parentItem.subAnswers?.[subQuestionIndex] || ''
  const correctAnswer = subQuestion.answer || ''
  
  const isStudentSelected = studentAnswer.includes(choice.choiceLabel)
  const isCorrect = correctAnswer.includes(choice.choiceLabel)
  
  if (isStudentSelected && isCorrect) {
    return 'correct'
  } else if (isStudentSelected && !isCorrect) {
    return 'wrong'
  } else if (!isStudentSelected && isCorrect) {
    return 'correct-not-selected'
  }
  return ''
}

// 获取题目状态（批改状态）- 实时响应分数变化
const getQuestionStatus = (item) => {
  // 如果本地标记为已批改，根据分数显示颜色
  if (item.isGraded) {
    const score = item.score || 0
    const fullScore = item.questionScore || 0
    
    if (score === 0) return 'wrong'
    if (score === fullScore) return 'correct'
    return 'partial'
  }
  
  // 未批改，显示灰色
  return 'ungraded'
}

// 标记题目为已批改（当分数改变时调用）
const markAsGraded = (item) => {
  item.isGraded = true
}

// 按题型分组
const groupedAnswers = computed(() => {
  const groups = {}
  data.answerData.forEach((item, globalIndex) => {
    const typeName = item.question?.questionTypeName || '其他'
    if (!groups[typeName]) {
      groups[typeName] = {
        typeName: typeName,
        questions: []
      }
    }
    groups[typeName].questions.push({
      ...item,
      globalIndex: globalIndex
    })
  })
  return Object.values(groups)
})

// 滚动到指定题目
const scrollToQuestion = (index) => {
  data.currentIndex = index
  nextTick(() => {
    const targetEl = questionRefs.value[index]
    if (targetEl) {
      targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
}

// 计算复合题的总分
const calculateCompositeScore = (item) => {
  // 复合题总分 = 所有子题分数之和
  item.score = item.subScores.reduce((sum, score) => sum + (parseFloat(score) || 0), 0)
  calculateTotalScore()
}

// 计算总分
const calculateTotalScore = () => {
  data.totalScore = data.answerData.reduce((sum, item) => {
    return sum + (parseFloat(item.score) || 0)
  }, 0)
}

// 自动判分 - 单选题
const autoScoreSingleChoice = (item) => {
  const studentAnswer = (item.answer || '').trim().toUpperCase()
  const correctAnswer = (item.question?.answer || '').trim().toUpperCase()
  
  if (!studentAnswer) return 0
  return studentAnswer === correctAnswer ? item.questionScore : 0
}

// 自动判分 - 多选题（新逻辑）
const autoScoreMultipleChoice = (item) => {
  const studentAnswer = (item.answer || '').trim().toUpperCase()
  const correctAnswer = (item.question?.answer || '').trim().toUpperCase()
  const fullScore = item.questionScore || 0
  
  // 如果没有作答，得0分
  if (!studentAnswer) return 0
  
  // 将答案转换为字符数组
  const studentChoices = studentAnswer.split('').sort()
  const correctChoices = correctAnswer.split('').sort()
  
  // 检查是否选择了错误选项（选择了不在正确答案中的选项）
  const hasWrongChoice = studentChoices.some(choice => !correctChoices.includes(choice))
  if (hasWrongChoice) return 0
  
  // 检查是否完全正确
  if (studentChoices.length === correctChoices.length && 
      studentChoices.every((choice, index) => choice === correctChoices[index])) {
    return fullScore
  }
  
  // 部分正确（选择了正确选项的子集，但不完整），得一半分数向上取整
  return Math.ceil(fullScore / 2)
}

// 自动判分 - 判断题
const autoScoreJudgement = (item) => {
  const studentAnswer = (item.answer || '').trim()
  const correctAnswer = (item.question?.answer || '').trim()
  
  if (!studentAnswer) return 0
  return studentAnswer === correctAnswer ? item.questionScore : 0
}

// 自动判分 - 填空题
const autoScoreFillBlank = (item) => {
  const studentAnswer = (item.answer || '').trim()
  const correctAnswer = (item.question?.answer || '').trim()
  
  if (!studentAnswer) return 0
  return studentAnswer === correctAnswer ? item.questionScore : 0
}

// 自动判分所有客观题
const autoScoreObjectiveQuestions = () => {
  data.answerData.forEach(item => {
    const typeName = item.question?.questionTypeName
    
    if (typeName === '单选') {
      item.score = autoScoreSingleChoice(item)
    } else if (typeName === '多选') {
      item.score = autoScoreMultipleChoice(item)
    } else if (typeName === '判断') {
      item.score = autoScoreJudgement(item)
    } else if (typeName === '填空') {
      item.score = autoScoreFillBlank(item)
    }
  })
  
  calculateTotalScore()
}

// 加载答题数据
const loadAnswerData = async () => {
  try {
    const res = await request.get(`/score/selectAnswer/${data.scoreId}`)
    if (res.code === '200') {
      data.answerData = res.data || []
      
      // 处理题目数据，统一字段名
      data.answerData.forEach((item, index) => {
        
        // 根据answer表的status字段初始化批改状态
        item.isGraded = item.status === '已批改'
        
        // 统一选项字段名（后端用identification和content，前端期望choiceLabel和choiceContent）
        if (item.question?.choiceList) {
          item.question.choiceList = item.question.choiceList.map(choice => ({
            ...choice,
            choiceLabel: choice.identification || choice.choiceLabel,
            choiceContent: choice.content || choice.choiceContent
          }))
        }
        
        // 处理复合题的子题选项
        if (item.question?.questionList) {
          item.question.questionList = item.question.questionList.map(subQ => ({
            ...subQ,
            choiceList: subQ.choiceList?.map(choice => ({
              ...choice,
              choiceLabel: choice.identification || choice.choiceLabel,
              choiceContent: choice.content || choice.choiceContent
            }))
          }))
        }
        
        // 处理复合题答案（将答案字符串拆分为数组）
        if (item.question?.questionTypeVariety === 'composite' && item.answer) {
          // 后端已经将子题答案用逗号分隔
          item.subAnswers = item.answer.split(',').map(a => a.trim())
          
          // 初始化子题分数数组
          item.subScores = item.question?.questionList?.map(() => 0) || []
          // 如果有子题分数数据，从answer表加载
          if (item.subScoreData && item.subScoreData.length) {
            item.subScores = item.subScoreData
          }
        }
      })
      
      // 计算满分
      data.fullScore = data.answerData.reduce((sum, item) => {
        return sum + (item.questionScore || 0)
      }, 0)
      
      // 不进行自动判分，教师阅卷页面应由教师手动评分
      // 如果已有分数则计算总分，否则初始化为0
      calculateTotalScore()
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

// 提交阅卷
const submitScore = () => {
  const confirmMessage = data.isGraded 
    ? `确认重新提交阅卷结果吗？这将更新之前的评分。总分：${data.totalScore}/${data.fullScore}`
    : `确认提交阅卷结果吗？总分：${data.totalScore}/${data.fullScore}`
  
  ElMessageBox.confirm(
    confirmMessage,
    '提交确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    data.submitting = true
    try {
      const submitData = {
        id: data.scoreId,
        status: '已批改',
        answers: data.answerData.flatMap(item => {
          // 如果是复合题，需要提交父题和子题的数据
          if (item.question?.questionTypeVariety === 'composite' && item.question?.questionList) {
            const result = [
              // 父题数据（answer设为空字符串，只保存总分）
              {
                id: item.id,
                score: item.score,
                answer: '',  // 复合题父题answer设为空
                questionId: item.questionId,
                questionScore: item.questionScore,
                testPaperQuestionId: item.testPaperQuestionId
              }
            ]
            // 子题数据
            item.question.questionList.forEach((subQ, idx) => {
              result.push({
                id: item.subQuestionAnswerIds?.[idx], // 子题的answer记录ID
                score: item.subScores[idx] || 0,
                answer: item.subAnswers?.[idx] || '',
                questionId: subQ.id,
                questionScore: item.subMaxScores?.[idx] || subQ.typeScore || 0, // 使用test_paper_question表中的分数
                testPaperQuestionId: item.subQuestionTPQIds?.[idx] // 子题的试卷题目ID
              })
            })
            return result
          } else {
            // 普通题目
            return [{
              id: item.id,
              score: item.score,
              answer: item.answer,
              questionId: item.questionId,
              questionScore: item.questionScore,
              testPaperQuestionId: item.testPaperQuestionId
            }]
          }
        })
      }
      
      const res = await request.put('/score/update', submitData)
      if (res.code === '200') {
        ElMessage.success(data.isGraded ? '重新阅卷成功' : '阅卷成功')
        setTimeout(() => {
          router.push('/manager/score')
        }, 500)
      } else {
        ElMessage.error(res.msg || '提交失败')
      }
    } catch (error) {
      ElMessage.error('提交失败')
    } finally {
      data.submitting = false
    }
  }).catch(() => {})
}

// 返回
const goBack = () => {
  ElMessageBox.confirm('确认返回吗？未保存的数据将丢失', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    router.back()
  }).catch(() => {})
}

// 初始化
onMounted(() => {
  const scoreId = route.query.scoreId
  const studentName = route.query.studentName
  const paperName = route.query.paperName
  const courseName = route.query.courseName
  const status = route.query.status // 获取阅卷状态
  
  if (!scoreId) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  
  data.scoreId = scoreId
  data.studentName = studentName || '未知学生'
  data.paperName = paperName || '未知试卷'
  data.courseName = courseName || '未知课程'
  data.isGraded = status === '已批改' // 设置是否已批改
  
  loadAnswerData()
})
</script>

<style scoped>
.score-detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

/* 顶部导航栏 */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  z-index: 100;
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  color: #fff;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.paper-info,
.score-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.label {
  opacity: 0.9;
}

.value {
  font-weight: 600;
}

/* 主体容器 */
.main-container {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 左侧答题卡 */
.answer-sheet {
  width: 280px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sheet-header {
  padding: 20px;
  border-bottom: 2px solid #f0f2f5;
}

.sheet-header h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.correct {
  background: #67c23a;
}

.dot.wrong {
  background: #f56c6c;
}

.dot.partial {
  background: #e6a23c;
}

.dot.unanswered {
  background: #dcdfe6;
}

.sheet-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.question-type-group {
  margin-bottom: 24px;
}

.type-title {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
}

.question-numbers {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.question-number {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid #dcdfe6;
  background: #fff;
  color: #606266;
}

.question-number:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.question-number.active {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.question-number.correct {
  background: #f0f9ff;
  border-color: #67c23a;
  color: #67c23a;
}

.question-number.wrong {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}

.question-number.partial {
  background: #fdf6ec;
  border-color: #e6a23c;
  color: #e6a23c;
}

.question-number.unanswered {
  background: #f5f7fa;
  border-color: #dcdfe6;
  color: #909399;
}

.question-number.ungraded {
  background: #f5f7fa;
  border-color: #dcdfe6;
  color: #909399;
}

/* 右侧题目区域 */
.question-area {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.question-list {
  padding: 24px;
}

.question-card {
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.question-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
}

.question-card.active {
  border-color: #409eff;
  background: #f0f9ff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.2);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.question-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-no {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.question-score-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.full-score {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.question-body {
  margin-bottom: 20px;
}

.question-stem {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 500;
}

.choices {
  margin-top: 16px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 10px;
  background: #f8f9fa;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
  position: relative;
}

.choice-item.correct {
  background: #f0f9ff;
  border-color: #67c23a;
  color: #67c23a;
  font-weight: 600;
}

.choice-item.wrong {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
  font-weight: 600;
}

.choice-item.correct-not-selected {
  background: #f0f9ff;
  border-color: #409eff;
  border-style: dashed;
}

.choice-label {
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}

.choice-content {
  flex: 1;
  line-height: 1.6;
}

.correct-icon {
  color: #67c23a;
  font-size: 20px;
}

.sub-questions {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.sub-question-item {
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.sub-question-item:last-child {
  margin-bottom: 0;
}

.sub-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.sub-question-no {
  font-size: 15px;
  font-weight: 600;
  color: #409eff;
}

.sub-question-stem {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 12px;
}

.sub-choices {
  margin-top: 12px;
}

.composite-answer-item {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  line-height: 1.8;
}

.composite-answer-item:last-child {
  margin-bottom: 0;
}

.composite-answer-label {
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
}

.answer-area {
  margin-bottom: 20px;
}

.answer-box {
  border-radius: 8px;
  padding: 16px;
  min-height: 100px;
}

.student-answer {
  background: #f0f9ff;
  border: 2px solid #409eff;
}

.correct-answer {
  background: #f0f9eb;
  border: 2px solid #67c23a;
}

.answer-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  color: #303133;
}

.answer-content {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.scoring-area {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 2px dashed #e4e7ed;
}

.composite-scoring {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 12px;
}

.composite-score-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.sub-question-score {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.sub-score-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-right: 12px;
  min-width: 60px;
}

.composite-total-score {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 2px solid #dcdfe6;
  margin-top: 8px;
}

.total-score-value {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin: 0 8px;
}

.score-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-right: 12px;
}

.score-unit {
  font-size: 15px;
  color: #909399;
  margin-left: 8px;
}

/* 滚动条样式 */
:deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}
</style>

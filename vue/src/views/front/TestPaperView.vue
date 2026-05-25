<template>
  <div class="test-paper-view">
    <!-- 顶部信息栏 -->
    <div class="header-bar">
      <el-button type="primary" plain icon="ArrowLeft" @click="router.back()">返回</el-button>
      <div class="header-info">
        <div class="paper-title">{{ data.testPaperData.test?.name || '-' }}</div>
        <div class="course-info">
          <span>课程：{{ data.testPaperData.test?.courseName || '-' }}</span>
          <span style="margin: 0 20px">教师：{{ data.testPaperData.test?.teacherName || '-' }}</span>
          <span>得分：<span style="color: #409EFF; font-weight: bold;">{{ data.testPaperData.score || 0 }}</span>/{{ data.totalScore }}</span>
        </div>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-container">
      <!-- 左侧题目区域 -->
      <div class="question-area">
        <el-scrollbar>
          <div class="question-list">
            <div 
              v-for="(item, index) in data.testPaperData.questions" 
              :key="index"
              v-show="item.paperQuestionType !== 'son'"
              class="question-card"
            >
              <!-- 题目头部 -->
              <div class="question-header">
                <el-tag 
                  size="large" 
                  :type="getQuestionTypeStyle(item.questionTypeName).type"
                  :effect="getQuestionTypeStyle(item.questionTypeName).effect"
                >
                  {{ item.questionTypeName }}
                </el-tag>
                <span class="question-number">第 {{ index + 1 }} 题</span>
                <el-tag 
                  v-if="getScoreStatus(item)" 
                  :type="getScoreStatus(item).type"
                  size="large"
                >
                  {{ getScoreStatus(item).text }}
                </el-tag>
              </div>

              <!-- 题干 -->
              <div class="question-stem">
                <RichTextContent :content="item.questionStem" />
              </div>

              <!-- 复合题 - 显示子题目 -->
              <div v-if="item.questionTypeVariety === 'composite' && item.questionList" class="composite-questions">
                <div 
                  v-for="(subItem, subIndex) in item.questionList" 
                  :key="subIndex"
                  class="sub-question-card"
                >
                  <!-- 子题目头部 -->
                  <div class="sub-question-header">
                    <el-tag size="small" :type="getQuestionTypeStyle(subItem.questionTypeName).type" :effect="getQuestionTypeStyle(subItem.questionTypeName).effect">
                      {{ subItem.questionTypeName }}
                    </el-tag>
                    <el-tag 
                      v-if="getScoreStatus(subItem)" 
                      :type="getScoreStatus(subItem).type"
                      size="small"
                    >
                      {{ getScoreStatus(subItem).text }}
                    </el-tag>
                  </div>

                  <!-- 子题目题干（序号在左侧） -->
                  <div class="sub-question-stem-wrapper">
                    <span class="sub-question-number">{{ subIndex + 1 }})</span>
                    <div class="sub-question-stem">
                      <RichTextContent :content="subItem.questionStem" />
                    </div>
                  </div>

                  <!-- 子题目选项区域 -->
                  <div class="sub-question-options">
                    <!-- 单选题 -->
                    <div v-if="subItem.questionTypeName === '单选'">
                      <el-radio-group v-model="subItem.newAnswer" disabled size="small">
                        <el-radio v-for="c in subItem.choiceList" :key="c.id" :value="c.identification" 
                                  :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                          <span>{{ c.identification }}.</span>
                          <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                        </el-radio>
                      </el-radio-group>
                    </div>

                    <!-- 多选题 -->
                    <div v-if="subItem.questionTypeName === '多选'">
                      <el-checkbox-group v-model="subItem.checkList" disabled size="small">
                        <el-checkbox v-for="c in subItem.choiceList" :key="c.id" :label="c.identification"
                                     :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                          <span>{{ c.identification }}.</span>
                          <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                        </el-checkbox>
                      </el-checkbox-group>
                    </div>

                    <!-- 判断题 -->
                    <div v-if="subItem.questionTypeName === '判断'">
                      <el-radio-group v-model="subItem.newAnswer" disabled size="small">
                        <el-radio v-for="c in subItem.choiceList" :key="c.id" :value="c.identification"
                                  :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                          <span>{{ c.identification }}.</span>
                          <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                        </el-radio>
                      </el-radio-group>
                    </div>

                    <!-- 填空题 -->
                    <div v-if="subItem.questionTypeName === '填空'">
                      <div class="answer-box student-answer">
                        <div class="answer-label">学生答案：</div>
                        <div class="answer-content">{{ isEmptyAnswer(subItem.newAnswer) ? '（未作答）' : subItem.newAnswer }}</div>
                      </div>
                    </div>

                    <!-- 文本题（除填空外） -->
                    <div v-if="subItem.questionTypeVariety === 'text' && subItem.questionTypeName !== '填空'">
                      <div class="answer-box student-answer">
                        <div class="answer-label">学生答案：</div>
                        <div class="answer-content">
                          <RichTextContent v-if="!isEmptyAnswer(subItem.newAnswer)" :content="subItem.newAnswer" />
                          <span v-else>（未作答）</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 子题目答案和解析 -->
                  <div class="sub-answer-section">
                    <!-- 只有选择题显示正确答案 -->
                    <div v-if="subItem.questionTypeVariety === 'choice'" class="correct-answer">
                      <el-icon><CircleCheck /></el-icon>
                      <span class="label">正确答案：</span>
                      <span class="content">{{ getCorrectAnswers(subItem) }}</span>
                    </div>
                    <div v-if="subItem.answer" class="answer-analysis">
                      <el-icon><Document /></el-icon>
                      <span class="label">解析：</span>
                      <span class="content">{{ subItem.answer }}</span>
                    </div>
                  </div>

                  <!-- 子题目得分显示 -->
                  <div class="sub-score-display" v-if="subItem.answerScore !== undefined">
                    <span class="label">得分：</span>
                    <span class="score-value" :class="{ 'full-score': subItem.answerScore >= subItem.typeScore }">
                      {{ subItem.answerScore }}
                    </span>
                    <span class="total">/ {{ subItem.typeScore }} 分</span>
                  </div>
                </div>
              </div>

              <!-- 非复合题 - 选项区域 -->
              <div v-if="item.questionTypeVariety !== 'composite'" class="question-options">
                <!-- 单选题 -->
                <div v-if="item.questionTypeName === '单选'">
                  <el-radio-group v-model="item.newAnswer" disabled>
                    <el-radio v-for="c in item.choiceList" :key="c.id" :value="c.identification" 
                              :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                      <span>{{ c.identification }}.</span>
                      <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                    </el-radio>
                  </el-radio-group>
                </div>

                <!-- 多选题 -->
                <div v-if="item.questionTypeName === '多选'">
                  <el-checkbox-group v-model="item.checkList" disabled>
                    <el-checkbox v-for="c in item.choiceList" :key="c.id" :label="c.identification"
                                 :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                      <span>{{ c.identification }}.</span>
                      <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                    </el-checkbox>
                  </el-checkbox-group>
                </div>

                <!-- 判断题 -->
                <div v-if="item.questionTypeName === '判断'">
                  <el-radio-group v-model="item.newAnswer" disabled>
                    <el-radio v-for="c in item.choiceList" :key="c.id" :value="c.identification"
                              :class="{ 'correct-choice': c.flag }" style="display: flex; align-items: flex-start;">
                      <span>{{ c.identification }}.</span>
                      <RichTextContent :content="c.content" style="flex: 1; margin-left: 5px;" />
                    </el-radio>
                  </el-radio-group>
                </div>

                <!-- 填空题 -->
                <div v-if="item.questionTypeName === '填空'">
                  <div class="answer-box student-answer">
                    <div class="answer-label">学生答案：</div>
                    <div class="answer-content">{{ isEmptyAnswer(item.newAnswer) ? '（未作答）' : item.newAnswer }}</div>
                  </div>
                </div>

                <!-- 文本题（除填空外的名词解析、论述、计算、程序等） -->
                <div v-if="item.questionTypeVariety === 'text' && item.questionTypeName !== '填空'">
                  <div class="answer-box student-answer">
                    <div class="answer-label">学生答案：</div>
                    <div class="answer-content">
                      <RichTextContent v-if="!isEmptyAnswer(item.newAnswer)" :content="item.newAnswer" />
                      <span v-else>（未作答）</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 非复合题 - 答案和解析 -->
              <div v-if="item.questionTypeName !== '复合'" class="answer-section">
                <!-- 只有单选题、多选题、判断题显示正确答案 -->
                <div v-if="['单选', '多选', '判断'].includes(item.questionTypeName)" class="correct-answer">
                  <el-icon><CircleCheck /></el-icon>
                  <span class="label">正确答案：</span>
                  <span class="content">{{ getCorrectAnswers(item) }}</span>
                </div>
                <div v-if="item.answer" class="answer-analysis">
                  <el-icon><Document /></el-icon>
                  <span class="label">解析：</span>
                  <span class="content">{{ item.answer }}</span>
                </div>
              </div>

              <!-- 得分显示 -->
              <div class="score-display" v-if="item.answerScore !== undefined">
                <span class="label">得分：</span>
                <span class="score-value" :class="{ 'full-score': item.answerScore >= item.typeScore }">
                  {{ item.answerScore }}
                </span>
                <span class="total">/ {{ item.typeScore }} 分</span>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 右侧AI分析区域 -->
      <div class="ai-analysis-area">
        <div class="analysis-card">
          <div class="analysis-header">
            <h3>
              <el-icon><TrendCharts /></el-icon>
              AI错题分析与学习建议
            </h3>
            <el-button 
              type="primary" 
              plain
              @click="getAIAnalysis" 
              :loading="data.aiLoading"
              :disabled="data.wrongCount === 0 && data.halfCorrect === 0">
              {{ data.aiAnalysis ? '重新分析' : '获取AI分析' }}
            </el-button>
          </div>
          
          <div class="analysis-content">
            <div v-if="data.aiAnalysis" v-html="data.aiAnalysisHtml" class="ai-analysis-html"></div>
            
            <div v-else-if="data.wrongCount === 0 && data.halfCorrect === 0" class="empty-state success">
              <el-icon><SuccessFilled /></el-icon>
              <p>恭喜！本次考试全部正确，无需AI分析。</p>
            </div>
            
            <div v-else class="empty-state">
              <el-icon><Document /></el-icon>
              <p>点击上方按钮获取AI智能分析和学习建议</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import {reactive, onMounted} from "vue";
import request from "@/utils/request.js";
import router from "@/router/index.js";
import {ElMessage} from "element-plus";
import { marked } from 'marked';
import { getQuestionTypeStyle } from '@/utils/questionTypeStyles';
import RichTextContent from '@/components/RichTextContent.vue';

const data = reactive({
  scoreId: router.currentRoute.value.query.id,
  testPaperData: {},
  totalScore: 0,
  fullCorrect: 0,
  halfCorrect: 0,
  wrongCount: 0,
  aiAnalysis: '', // AI分析原始文本
  aiAnalysisHtml: '', // AI分析HTML
  aiLoading: false // AI分析加载状态
})

// 检查是否为空答案（包括富文本空内容）
const isEmptyAnswer = (answer) => {
  if (!answer) return true
  // 检查是否包含公式、图片等富文本元素
  if (answer.includes('data-w-e-type="formula"') || answer.includes('<img')) {
    return false
  }
  // 移除HTML标签和空白字符，检查是否为空
  const text = answer.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim()
  return text === ''
}

// 获取正确答案列表（从flag=true的选项中提取）
const getCorrectAnswers = (item) => {
  if (!item.choiceList) return item.answer || '未设置';
  const correctChoices = item.choiceList.filter(c => c.flag === true || c.flag === 1);
  return correctChoices.map(c => c.identification).join(', ') || item.answer || '未设置';
}

// 获取题目得分状态
const getScoreStatus = (item) => {
  if (item.answerScore === undefined || item.answerScore === null) return null
  const score = item.answerScore
  const fullScore = item.typeScore || 0
  
  if (score >= fullScore) {
    return { type: 'success', text: '全对' }
  } else if (score > 0) {
    return { type: 'warning', text: '半对' }
  } else {
    return { type: 'danger', text: '错误' }
  }
}

const loadTestPaper = () => {
  data.scoreId = router.currentRoute.value.query.id
  request.get('/score/selectById/' + data.scoreId).then(res => {
    if (res.code === '200') {
      data.testPaperData = res.data
      // 统计题目正确性
      calcStatistics()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 计算成绩统计
const calcStatistics = () => {
  let full = 0, half = 0, wrong = 0, total = 0
  data.testPaperData.questions.forEach(q => {
    // 子题目不计入统计
    if (q.paperQuestionType === 'son') {
      return
    }
    
    total += q.typeScore || 0
    
    // 根据学生得分和题目分值判断对错
    const answerScore = q.answerScore || 0  // 学生该题获得的分数
    const typeScore = q.typeScore || 0      // 该题的满分分值
    
    if (typeScore > 0) {
      if (answerScore >= typeScore) {
        // 满分则全对
        full++
      } else if (answerScore > 0) {
        // 得分大于0但不满分则半对
        half++
      } else {
        // 0分则全错
        wrong++
      }
    } else {
      // 题目分值为0或未设置，按0分处理
      wrong++
    }
  })
  data.fullCorrect = full
  data.halfCorrect = half
  data.wrongCount = wrong
  data.totalScore = total
}

// 获取AI分析
const getAIAnalysis = () => {
  data.aiLoading = true
  request.get('/score/aiAnalysis/' + data.scoreId).then(res => {
    data.aiLoading = false
    if (res.code === '200') {
      data.aiAnalysis = res.data
      // 删除所有连续换行符，只保留单个换行
      let cleanedText = res.data.replace(/\n+/g, '\n')
      // 将markdown转换为HTML
      data.aiAnalysisHtml = marked.parse(cleanedText)
      // 删除HTML中的所有空段落标签
      data.aiAnalysisHtml = data.aiAnalysisHtml.replace(/<p>\s*<\/p>/g, '')
      // 删除连续的<br>标签
      data.aiAnalysisHtml = data.aiAnalysisHtml.replace(/(<br\s*\/?>\s*)+/gi, '')
      // 删除段落之间的多余空白
      data.aiAnalysisHtml = data.aiAnalysisHtml.replace(/>\s+</g, '><')
      ElMessage.success('AI分析完成')
    } else {
      ElMessage.error(res.msg || 'AI分析失败')
    }
  }).catch(err => {
    data.aiLoading = false
    ElMessage.error('AI分析请求失败')
  })
}

loadTestPaper()

</script>
<style scoped>
.test-paper-view {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* 顶部信息栏 */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}

.header-info {
  flex: 1;
  margin: 0 30px;
  color: white;
}

.paper-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
}

.course-info {
  font-size: 14px;
  opacity: 0.95;
}

/* 主体容器 */
.main-container {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
}

/* 左侧题目区域 */
.question-area {
  flex: 1;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  overflow: hidden;
}

.question-list {
  padding: 20px;
}

.question-card {
  background-color: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.question-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.question-number {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.question-stem {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  line-height: 1.8;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border-left: 4px solid #409EFF;
  border-radius: 4px;
}

.question-options {
  margin: 16px 0;
}

/* 复合题子题目样式 */
.composite-questions {
  margin: 16px 0;
}

.sub-question-card {
  background-color: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}

.sub-question-card:last-child {
  margin-bottom: 0;
}

.sub-question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.sub-question-stem-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.sub-question-number {
  font-size: 15px;
  font-weight: 700;
  color: #409EFF;
  white-space: nowrap;
  line-height: 1.6;
  min-width: 30px;
}

.sub-question-stem {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.6;
}

.sub-question-options {
  margin: 12px 0;
}

.sub-answer-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
}

.sub-answer-section .correct-answer,
.sub-answer-section .answer-analysis {
  font-size: 13px;
  padding: 8px;
  margin: 8px 0;
}

.sub-score-display {
  margin-top: 12px;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  text-align: right;
  font-size: 14px;
}

.sub-score-display .label {
  color: #606266;
  margin-right: 6px;
}

.sub-score-display .score-value {
  font-size: 18px;
  font-weight: bold;
  color: #e6a23c;
  margin: 0 4px;
}

.sub-score-display .score-value.full-score {
  color: #67c23a;
}

.sub-score-display .total {
  color: #909399;
}

.answer-box {
  margin: 12px 0;
  padding: 12px;
  border-radius: 6px;
  background-color: #f9fafb;
}

.answer-box .answer-label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.answer-box .answer-content {
  color: #303133;
  line-height: 1.6;
}

.answer-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
}

.correct-answer,
.answer-analysis {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 10px 0;
  padding: 10px;
  border-radius: 6px;
}

.correct-answer {
  background-color: #f0f9ff;
  color: #67c23a;
}

.answer-analysis {
  background-color: #fef0f0;
  color: #409EFF;
}

.correct-answer .label,
.answer-analysis .label {
  font-weight: 600;
  white-space: nowrap;
}

.correct-answer .content,
.answer-analysis .content {
  flex: 1;
  line-height: 1.6;
}

.score-display {
  margin-top: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border-radius: 6px;
  text-align: right;
  font-size: 16px;
}

.score-display .label {
  color: #606266;
  margin-right: 8px;
}

.score-display .score-value {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
  margin: 0 4px;
}

.score-display .score-value.full-score {
  color: #67c23a;
}

.score-display .total {
  color: #909399;
}

/* 右侧AI分析区域 */
.ai-analysis-area {
  width: 400px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.analysis-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.analysis-header {
  padding: 20px;
  border-bottom: 2px solid #e4e7ed;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.analysis-header h3 {
  margin: 0 0 12px 0;
  color: white;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.analysis-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state.success {
  color: #67c23a;
}

.empty-state .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 14px;
  line-height: 1.6;
}

.ai-analysis-html {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}

.el-radio-group {
  display: block;
}
.el-radio {
  display: block;
  margin: 8px 0;
}
.el-checkbox {
  display: block;
  margin: 8px 0;
}
.correct-choice {
  color: #67c23a !important;
  font-weight: bold;
}

/* AI分析内容样式 */
:deep(.ai-analysis-html) h1,
:deep(.ai-analysis-html) h2,
:deep(.ai-analysis-html) h3 {
  color: #303133;
  margin-top: 12px;
  margin-bottom: 6px;
}

:deep(.ai-analysis-html) h1 {
  font-size: 18px;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 6px;
}

:deep(.ai-analysis-html) h2 {
  font-size: 16px;
  color: #409EFF;
}

:deep(.ai-analysis-html) h3 {
  font-size: 14px;
}

:deep(.ai-analysis-html) ul,
:deep(.ai-analysis-html) ol {
  padding-left: 20px;
  margin: 6px 0;
}

:deep(.ai-analysis-html) li {
  margin: 4px 0;
  line-height: 1.4;
}

:deep(.ai-analysis-html) p {
  margin: 6px 0;
  line-height: 1.5;
}

:deep(.ai-analysis-html) strong {
  color: #409EFF;
  font-weight: 600;
}

:deep(.ai-analysis-html) code {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

:deep(.ai-analysis-html) blockquote {
  border-left: 4px solid #409EFF;
  padding-left: 15px;
  margin: 10px 0;
  color: #606266;
  background-color: #f5f7fa;
  padding: 8px 15px;
  border-radius: 4px;
}

:deep(.ai-analysis-content) p {
  margin: 6px 0;
  line-height: 1.5;
}

:deep(.ai-analysis-content) strong {
  color: #409EFF;
  font-weight: 600;
}

:deep(.ai-analysis-content) code {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

:deep(.ai-analysis-content) blockquote {
  border-left: 4px solid #409EFF;
  padding-left: 15px;
  margin: 10px 0;
  color: #606266;
  background-color: #f5f7fa;
  padding: 8px 15px;
  border-radius: 4px;
}
</style>

<template>
  <div class="exam-container">
    <div class="leave-btn">
      <el-button type="danger" plain size="small" @click="leaveExam">离开考试</el-button>
    </div>
    <el-row :gutter="20">
      <!-- 左侧：题目区 -->
      <el-col :span="18">
        <div class="paper-header">
          <div class="paper-title">{{ data.testPaperData.name }}</div>
          <div class="paper-meta">
            <span>课程：{{ data.testPaperData.courseName }}</span>
            <span>授课：{{ data.testPaperData.teacherName }}</span>
            <span>时长：{{ data.testInfo.duration }} 分钟</span>
          </div>
        </div>

        <div class="questions">
          <div class="watermark" :style="questionsStyle"></div>

          <!-- 单题模式 -->
          <div v-if="!previewMode">
            <div class="single-nav">
              <el-button plain size="small" @click="prevQuestion" :disabled="currentIndex === 0">上一题</el-button>
              <el-button plain size="small" @click="handleNextOrPreview" :disabled="totalQuestions === 0">{{ currentIndex < totalQuestions - 1 ? '下一题' : '整卷预览' }}</el-button>
              <div class="nav-info">第 {{ currentIndex + 1 }} / {{ totalQuestions }} 题</div>
            </div>

            <div v-if="currentQuestion" :key="currentIndex" :id="'q-' + currentIndex" class="question-card">
              <div class="question-header">
                <div style="display: flex; align-items: flex-start; gap: 10px;">
                  <strong style="flex-shrink: 0;">题 {{ currentIndex + 1 }}.</strong> 
                  <RichTextContent :content="currentQuestion.questionStem" style="flex: 1;" />
                </div>
                <el-tag size="small" v-if="isAnswered(currentQuestion)" type="success">已答</el-tag>
                <el-tag size="small" v-else type="info">未答</el-tag>
              </div>

              <div class="question-body">
                <div v-if="currentQuestion.questionTypeName === '单选'">
                  <el-radio-group v-model="currentQuestion.newAnswer" @change="saveAnswers">
                    <div v-for="c in currentQuestion.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-radio :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-radio>
                    </div>
                  </el-radio-group>
                </div>
                <div v-if="currentQuestion.questionTypeName === '多选'">
                  <el-checkbox-group v-model="currentQuestion.checkList" @change="saveAnswers">
                    <div v-for="c in currentQuestion.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-checkbox :label="c.identification" :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-checkbox>
                    </div>
                  </el-checkbox-group>
                </div>
                <div v-if="currentQuestion.questionTypeName === '判断'">
                  <el-radio-group v-model="currentQuestion.newAnswer" @change="saveAnswers">
                    <div v-for="c in currentQuestion.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-radio :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-radio>
                    </div>
                  </el-radio-group>
                </div>
                <!-- 填空题使用普通输入框 -->
                <div v-if="currentQuestion.questionTypeName === '填空'">
                  <el-input type="textarea" :rows="6" v-model="currentQuestion.newAnswer" placeholder="请输入您的答案" @blur="saveAnswers"></el-input>
                </div>
                <!-- 除填空外的text类型题使用富文本编辑器 -->
                <div v-if="currentQuestion.questionTypeVariety === 'text' && currentQuestion.questionTypeName !== '填空'">
                  <RichTextEditor v-model="currentQuestion.newAnswer" placeholder="请输入您的答案" height="300px" :noPaste="noCopyEnabled" />
                  <div style="margin-top: 10px;">
                    <el-button type="primary" plain size="small" @click="saveAnswers">保存答案</el-button>
                  </div>
                </div>
                <div v-if="currentQuestion.questionTypeVariety === 'composite'">
                    <div v-for="(subQ, subIndex) in currentQuestion.questionList" :key="subQ.id" style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 10px;">
                        <div style="display: flex; align-items: flex-start; gap: 8px; font-weight: bold; margin-bottom: 10px;">
                            <span style="flex-shrink: 0;">({{ subIndex + 1 }})</span>
                            <RichTextContent :content="subQ.questionStem" style="flex: 1;" />
                            <el-tag size="small" :type="getQuestionTypeStyle(subQ.questionTypeName).type" :effect="getQuestionTypeStyle(subQ.questionTypeName).effect">{{ subQ.questionTypeName }}</el-tag>
                        </div>
                        
                        <div v-if="subQ.questionTypeName === '单选'">
                            <el-radio-group v-model="subQ.newAnswer" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 8px;">
                                  <el-radio :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-radio>
                                </div>
                            </el-radio-group>
                        </div>
                        <div v-if="subQ.questionTypeName === '多选'">
                             <el-checkbox-group v-model="subQ.checkList" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 8px;">
                                  <el-checkbox :label="c.identification" :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-checkbox>
                                </div>
                             </el-checkbox-group>
                        </div>
                        <div v-if="subQ.questionTypeName === '判断'">
                             <el-radio-group v-model="subQ.newAnswer" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 8px;">
                                  <el-radio :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-radio>
                                </div>
                             </el-radio-group>
                        </div>
                        <!-- 填空题使用普通输入框 -->
                        <div v-if="subQ.questionTypeName === '填空'">
                             <el-input type="textarea" :rows="3" v-model="subQ.newAnswer" placeholder="请输入您的答案" @blur="saveAnswers"></el-input>
                        </div>
                        <!-- 除填空外的text类型题使用富文本编辑器 -->
                        <div v-if="subQ.questionTypeVariety === 'text' && subQ.questionTypeName !== '填空'">
                             <RichTextEditor v-model="subQ.newAnswer" placeholder="请输入您的答案" height="200px" :noPaste="noCopyEnabled" />
                             <div style="margin-top: 10px;">
                               <el-button type="primary" plain size="small" @click="saveAnswers">保存答案</el-button>
                             </div>
                        </div>
                    </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 整卷预览模式 -->
          <div v-else>
            <!-- 占位导航条，保持布局一致 -->
            <div class="single-nav preview-placeholder">
              <span style="color: #595959; font-weight: 500;">整卷预览模式</span>
            </div>
            
            <div v-for="(item, index) in data.testPaperData.questions" :key="index" :id="'q-' + index" class="question-card">
              <div class="question-header">
                <div :class="{ 'no-copy': noCopyEnabled }" style="flex: 1;"><strong>题 {{ index + 1 }}.</strong> <RichTextContent :content="item.questionStem" /></div>
                <el-tag size="small" v-if="isAnswered(item)" type="success">已答</el-tag>
                <el-tag size="small" v-else type="info">未答</el-tag>
              </div>

              <div class="question-body">
                <div v-if="item.questionTypeName === '单选'">
                  <el-radio-group v-model="item.newAnswer" @change="saveAnswers">
                    <div v-for="c in item.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-radio :class="{ 'no-copy': noCopyEnabled }" :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-radio>
                    </div>
                  </el-radio-group>
                </div>
                <div v-if="item.questionTypeName === '多选'">
                  <el-checkbox-group v-model="item.checkList" @change="saveAnswers">
                    <div v-for="c in item.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-checkbox :class="{ 'no-copy': noCopyEnabled }" :label="c.identification" :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-checkbox>
                    </div>
                  </el-checkbox-group>
                </div>
                <div v-if="item.questionTypeName === '判断'">
                  <el-radio-group v-model="item.newAnswer" @change="saveAnswers">
                    <div v-for="c in item.choiceList" :key="c.id" style="margin-bottom: 10px;">
                      <el-radio :class="{ 'no-copy': noCopyEnabled }" :value="c.identification" style="display: inline-flex; align-items: center;">
                        <span>{{ c.identification }}. </span>
                        <RichTextContent :content="c.content" style="margin-left: 5px;" />
                      </el-radio>
                    </div>
                  </el-radio-group>
                </div>
                <!-- 填空题使用普通输入框 -->
                <div v-if="item.questionTypeName === '填空'">
                  <el-input type="textarea" :rows="6" v-model="item.newAnswer" placeholder="请输入您的答案" @blur="saveAnswers"></el-input>
                </div>
                <!-- 除填空外的text类型题使用富文本编辑器 -->
                <div v-if="item.questionTypeVariety === 'text' && item.questionTypeName !== '填空'">
                  <RichTextEditor v-model="item.newAnswer" height="300px" placeholder="请输入您的答案" @blur="saveAnswers" :noPaste="noCopyEnabled" />
                </div>
                <div v-if="item.questionTypeVariety === 'composite'">
                    <div v-for="(subQ, subIndex) in item.questionList" :key="subQ.id" style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 10px;">
                        <div :class="{ 'no-copy': noCopyEnabled }" style="font-weight: bold; margin-bottom: 5px; display: flex; align-items: flex-start;">
                            <span>({{ subIndex + 1 }})</span>
                            <RichTextContent :content="subQ.questionStem" style="flex: 1; margin-left: 5px;" />
                            <el-tag size="small" :type="getQuestionTypeStyle(subQ.questionTypeName).type" :effect="getQuestionTypeStyle(subQ.questionTypeName).effect" style="margin-left: 10px;">{{ subQ.questionTypeName }}</el-tag>
                        </div>
                        
                        <div v-if="subQ.questionTypeName === '单选'">
                            <el-radio-group v-model="subQ.newAnswer" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 10px;">
                                  <el-radio :class="{ 'no-copy': noCopyEnabled }" :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-radio>
                                </div>
                            </el-radio-group>
                        </div>
                        <div v-if="subQ.questionTypeName === '多选'">
                             <el-checkbox-group v-model="subQ.checkList" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 10px;">
                                  <el-checkbox :class="{ 'no-copy': noCopyEnabled }" :label="c.identification" :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-checkbox>
                                </div>
                             </el-checkbox-group>
                        </div>
                        <div v-if="subQ.questionTypeName === '判断'">
                             <el-radio-group v-model="subQ.newAnswer" @change="saveAnswers">
                                <div v-for="c in subQ.choiceList" :key="c.id" style="margin-bottom: 10px;">
                                  <el-radio :class="{ 'no-copy': noCopyEnabled }" :value="c.identification" style="display: inline-flex; align-items: center;">
                                    <span>{{ c.identification }}. </span>
                                    <RichTextContent :content="c.content" style="margin-left: 5px;" />
                                  </el-radio>
                                </div>
                             </el-radio-group>
                        </div>
                        <!-- 填空题使用普通输入框 -->
                        <div v-if="subQ.questionTypeName === '填空'">
                             <el-input type="textarea" :rows="3" v-model="subQ.newAnswer" placeholder="请输入您的答案" @blur="saveAnswers"></el-input>
                        </div>
                        <!-- 除填空外的text类型题使用富文本编辑器 -->
                        <div v-if="subQ.questionTypeVariety === 'text' && subQ.questionTypeName !== '填空'">
                             <RichTextEditor v-model="subQ.newAnswer" height="200px" placeholder="请输入您的答案" @blur="saveAnswers" :noPaste="noCopyEnabled" />
                        </div>
                    </div>
                </div>
              </div>
            </div>
          </div>

          <div class="submit-area" v-if="previewMode">
            <el-button type="primary" plain size="large" @click="confirmSubmit">提交试卷</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：侧边栏 — 题目总览、剩余时间 -->
      <el-col :span="6">
        <div class="sidebar">
          <!-- 人脸识别监考区 -->
          <ExamMonitor 
            v-if="data.testId && user.id && (data.testInfo.weatherFace || data.testInfo.weatherDualCamera)"
            :examId="data.testId" 
            :studentId="user.id" 
            @abnormal="handleExamAbnormal"
          />
          
          <!-- 手机监考区 -->
          <PhoneMonitor
            v-if="data.testId && user.id && data.testInfo.weatherDualCamera"
            :examId="data.testId"
            :studentId="user.id"
          />

          <div class="timer">
            <div class="timer-title">剩余时间</div>
            <div class="timer-value">{{ data.hour }}:{{ pad(data.minutes) }}:{{ pad(data.seconds) }}</div>
          </div>

          <div style="margin-bottom:10px">
            <el-button size="small" type="primary" plain @click="previewMode = !previewMode">{{ previewMode ? '退出预览' : '整卷预览' }}</el-button>
          </div>

          <div class="overview">
            <div class="overview-header">题目总览</div>
            <div class="overview-stats">
              <el-tag type="primary">总题：{{ totalQuestions }}</el-tag>
              <el-tag type="success">已答：{{ answeredCount }}</el-tag>
              <el-tag type="info">未答：{{ unansweredCount }}</el-tag>
            </div>

            <div class="question-list">
              <div v-for="group in groupedQuestions" :key="group.typeName" class="question-group">
                <div class="group-header">
                  {{ group.label }}：共 {{ group.questions.length }} 道，合计 {{ group.totalScore }} 分
                </div>
                <div class="group-buttons">
                  <el-button v-for="q in group.questions" :key="q.__index" size="mini"
                             plain
                             :type="isAnswered(data.testPaperData.questions[q.__index]) ? 'success' : 'warning'"
                             @click="goToQuestion(q.__index)"
                             style="margin: 6px">
                    {{ q.__index + 1 }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import {reactive, ref, onMounted, onBeforeUnmount, computed, watch} from "vue";
import request from "@/utils/request.js";
import router from "@/router/index.js";
import {ElMessage, ElMessageBox} from "element-plus";
import ExamMonitor from "@/components/ExamMonitor.vue";
import PhoneMonitor from "@/components/PhoneMonitor.vue";
import { getQuestionTypeStyle } from '@/utils/questionTypeStyles.js';
import RichTextEditor from '@/components/RichTextEditor.vue';
import RichTextContent from '@/components/RichTextContent.vue';

const data = reactive({
  testId: null,
  testInfo: {},
  testPaperData: {},
  hour: 0,
  minutes: 0,
  seconds: 0,
})

// 计算属性：是否启用防复制
const noCopyEnabled = computed(() => data.testInfo?.weatherCopy === true || data.testInfo?.weatherCopy === 1)

// 获取答题记录的存储key
const getAnswerStorageKey = (testId) => {
  const userId = JSON.parse(localStorage.getItem('xm-user') || '{}').id
  return `exam-answers-${userId}-${testId}`
}

// 保存答题记录到localStorage和后端
const saveAnswersToLocal = () => {
  if (!data.testId || !data.testPaperData.questions) return
  
  const answers = {}
  data.testPaperData.questions.forEach((q) => {
    // 使用题目ID作为键而不是索引
    const questionId = q.id || q.questionId
    if (!questionId) return
    
    if (q.questionTypeVariety === 'composite' && q.questionList) {
      // 保存复合题的子题答案
      const subAnswers = {}
      q.questionList.forEach((subQ) => {
        const subQuestionId = subQ.id || subQ.questionId
        if (!subQuestionId) return
        
        if (subQ.questionTypeName === '多选') {
          // 保存选项ID而不是显示标识
          const choiceIds = (subQ.checkList || []).map(displayId => {
            const choice = subQ.choiceList?.find(c => c.identification === displayId)
            return choice ? choice.id : displayId
          })
          subAnswers[subQuestionId] = { checkList: choiceIds }
        } else {
          // 对于单选和判断，保存选项ID
          let answerValue = subQ.newAnswer || ''
          if (answerValue && subQ.choiceList) {
            const choice = subQ.choiceList.find(c => c.identification === answerValue)
            answerValue = choice ? choice.id : answerValue
          }
          subAnswers[subQuestionId] = { newAnswer: answerValue }
        }
      })
      answers[questionId] = { subAnswers }
    } else if (q.questionTypeName === '多选') {
      // 保存选项ID而不是显示标识
      const choiceIds = (q.checkList || []).map(displayId => {
        const choice = q.choiceList?.find(c => c.identification === displayId)
        return choice ? choice.id : displayId
      })
      answers[questionId] = { checkList: choiceIds }
    } else {
      // 对于单选和判断，保存选项ID
      let answerValue = q.newAnswer || ''
      if (answerValue && q.choiceList) {
        const choice = q.choiceList.find(c => c.identification === answerValue)
        answerValue = choice ? choice.id : answerValue
      }
      answers[questionId] = { newAnswer: answerValue }
    }
  })
  
  localStorage.setItem(getAnswerStorageKey(data.testId), JSON.stringify(answers))
}

// 保存到后端服务器
const saveAnswersToServer = () => {
  if (!data.testId || !data.testPaperData.questions) return
  
  const answers = {}
  data.testPaperData.questions.forEach((q) => {
    // 使用题目ID作为键而不是索引
    const questionId = q.id || q.questionId
    if (!questionId) return
    
    if (q.questionTypeVariety === 'composite' && q.questionList) {
      const subAnswers = {}
      q.questionList.forEach((subQ) => {
        const subQuestionId = subQ.id || subQ.questionId
        if (!subQuestionId) return
        
        if (subQ.questionTypeName === '多选') {
          // 保存选项ID而不是显示标识
          const choiceIds = (subQ.checkList || []).map(displayId => {
            const choice = subQ.choiceList?.find(c => c.identification === displayId)
            return choice ? choice.id : displayId
          })
          subAnswers[subQuestionId] = { checkList: choiceIds }
        } else {
          // 对于单选和判断，保存选项ID
          let answerValue = subQ.newAnswer || ''
          if (answerValue && subQ.choiceList) {
            const choice = subQ.choiceList.find(c => c.identification === answerValue)
            answerValue = choice ? choice.id : answerValue
          }
          subAnswers[subQuestionId] = { newAnswer: answerValue }
        }
      })
      answers[questionId] = { subAnswers }
    } else if (q.questionTypeName === '多选') {
      // 保存选项ID而不是显示标识
      const choiceIds = (q.checkList || []).map(displayId => {
        const choice = q.choiceList?.find(c => c.identification === displayId)
        return choice ? choice.id : displayId
      })
      answers[questionId] = { checkList: choiceIds }
    } else {
      // 对于单选和判断，保存选项ID
      let answerValue = q.newAnswer || ''
      if (answerValue && q.choiceList) {
        const choice = q.choiceList.find(c => c.identification === answerValue)
        answerValue = choice ? choice.id : answerValue
      }
      answers[questionId] = { newAnswer: answerValue }
    }
  })
  
  request.post('/draft/save', {
    testId: data.testId,
    draftData: JSON.stringify(answers)
  }).catch(err => {  })
}

// 混合保存：同时保存到localStorage和服务器
const saveAnswers = () => {
  saveAnswersToLocal()
  saveAnswersToServer()
  ElMessage.success('答案已保存')
}

// 从localStorage恢复答题记录
const restoreAnswersFromLocal = () => {
  if (!data.testId || !data.testPaperData.questions) return
  
  const savedAnswers = localStorage.getItem(getAnswerStorageKey(data.testId))
  if (!savedAnswers) return
  
  try {
    const answers = JSON.parse(savedAnswers)
    data.testPaperData.questions.forEach((q) => {
      // 使用题目ID作为键而不是索引
      const questionId = q.id || q.questionId
      if (!questionId) return
      
      const savedAnswer = answers[questionId]
      if (!savedAnswer) return
      
      if (q.questionTypeVariety === 'composite' && q.questionList && savedAnswer.subAnswers) {
        // 恢复复合题的子题答案
        q.questionList.forEach((subQ) => {
          const subQuestionId = subQ.id || subQ.questionId
          if (!subQuestionId) return
          
          const subAnswer = savedAnswer.subAnswers[subQuestionId]
          if (!subAnswer) return
          
          if (subQ.questionTypeName === '多选') {
            // 从选项ID恢复为当前显示标识
            const choiceIds = subAnswer.checkList || []
            subQ.checkList = choiceIds.map(choiceId => {
              const choice = subQ.choiceList?.find(c => c.id === choiceId)
              return choice ? choice.identification : choiceId
            })
          } else {
            // 从选项ID恢复为当前显示标识
            let answerValue = subAnswer.newAnswer || ''
            if (answerValue && subQ.choiceList) {
              const choice = subQ.choiceList.find(c => c.id === answerValue)
              answerValue = choice ? choice.identification : answerValue
            }
            subQ.newAnswer = answerValue
          }
        })
      } else if (q.questionTypeName === '多选') {
        // 从选项ID恢复为当前显示标识
        const choiceIds = savedAnswer.checkList || []
        q.checkList = choiceIds.map(choiceId => {
          const choice = q.choiceList?.find(c => c.id === choiceId)
          return choice ? choice.identification : choiceId
        })
      } else {
        // 从选项ID恢复为当前显示标识
        let answerValue = savedAnswer.newAnswer || ''
        if (answerValue && q.choiceList) {
          const choice = q.choiceList.find(c => c.id === answerValue)
          answerValue = choice ? choice.identification : answerValue
        }
        q.newAnswer = answerValue
      }
    })
    
    return true
  } catch (e) {    return false
  }
}

// 从服务器恢复答题记录
const restoreAnswersFromServer = async () => {
  if (!data.testId || !data.testPaperData.questions) return
  
  try {
    const res = await request.get('/draft/get/' + data.testId)
    if (res.code === '200' && res.data) {
      const answers = JSON.parse(res.data)
      data.testPaperData.questions.forEach((q) => {
        // 使用题目ID作为键而不是索引
        const questionId = q.id || q.questionId
        if (!questionId) return
        
        const savedAnswer = answers[questionId]
        if (!savedAnswer) return
        
        if (q.questionTypeVariety === 'composite' && q.questionList && savedAnswer.subAnswers) {
          q.questionList.forEach((subQ) => {
            const subQuestionId = subQ.id || subQ.questionId
            if (!subQuestionId) return
            
            const subAnswer = savedAnswer.subAnswers[subQuestionId]
            if (!subAnswer) return
            
            if (subQ.questionTypeName === '多选') {
              // 从选项ID恢复为当前显示标识
              const choiceIds = subAnswer.checkList || []
              subQ.checkList = choiceIds.map(choiceId => {
                const choice = subQ.choiceList?.find(c => c.id === choiceId)
                return choice ? choice.identification : choiceId
              })
            } else {
              // 从选项ID恢复为当前显示标识
              let answerValue = subAnswer.newAnswer || ''
              if (answerValue && subQ.choiceList) {
                const choice = subQ.choiceList.find(c => c.id === answerValue)
                answerValue = choice ? choice.identification : answerValue
              }
              subQ.newAnswer = answerValue
            }
          })
        } else if (q.questionTypeName === '多选') {
          // 从选项ID恢复为当前显示标识
          const choiceIds = savedAnswer.checkList || []
          q.checkList = choiceIds.map(choiceId => {
            const choice = q.choiceList?.find(c => c.id === choiceId)
            return choice ? choice.identification : choiceId
          })
        } else {
          // 从选项ID恢复为当前显示标识
          let answerValue = savedAnswer.newAnswer || ''
          if (answerValue && q.choiceList) {
            const choice = q.choiceList.find(c => c.id === answerValue)
            answerValue = choice ? choice.identification : answerValue
          }
          q.newAnswer = answerValue
        }
      })
      return true
    }
  } catch (e) {  }
  return false
}

// 混合恢复：优先localStorage，没有则从服务器获取
const restoreAnswers = async () => {
  const localRestored = restoreAnswersFromLocal()
  if (localRestored) {
    ElMessage.success('已恢复上次答题记录（本地）')
  } else {
    const serverRestored = await restoreAnswersFromServer()
    if (serverRestored) {
      ElMessage.success('已恢复上次答题记录（云端）')
    }
  }
}

// 清除答题记录
const clearAnswersFromLocal = () => {
  if (!data.testId) return
  localStorage.removeItem(getAnswerStorageKey(data.testId))
}

const clearAnswersFromServer = () => {
  if (!data.testId) return
  request.delete('/draft/clear/' + data.testId).catch(err => {  })
}

const clearAllAnswers = () => {
  clearAnswersFromLocal()
  clearAnswersFromServer()
}

let time
const pad = (n) => (n < 10 ? '0' + n : n)

const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
const watermarkText = `${user.name || ''}${user.name && user.username ? ' / ' : ''}${user.username || ''}`

const watermarkDataUrl = computed(() => {
  // 文本回退为“考生考试中”以便在缺少用户信息时仍能看到水印
  const text = watermarkText || '考生考试中'
  // 使用纯黑色填充，由 .watermark 的 opacity 控制透明度，兼容性更好
  const svg = `
    <svg xmlns='http://www.w3.org/2000/svg' width='210' height='105'>
      <g>
        <text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='#000' font-family='sans-serif' font-size='21' letter-spacing='-1.2' transform='rotate(-30 105 52)'>${text}</text>
      </g>
    </svg>`
  return `url("data:image/svg+xml;utf8,${encodeURIComponent(svg)}")`
})

const questionsStyle = computed(() => {
  const url = watermarkDataUrl.value
  if (!url) return {}
  // 使用与 .watermark CSS 中相同的尺寸，使 SVG 字体大小与间距生效
  return { backgroundImage: url, backgroundRepeat: 'repeat', backgroundSize: '210px 105px' }
})

// 单题/整卷预览控制
const currentIndex = ref(0)
const previewMode = ref(false)
const currentQuestion = computed(() => (data.testPaperData.questions || [])[currentIndex.value] || null)

const prevQuestion = () => { if (currentIndex.value > 0) currentIndex.value-- }
const nextQuestion = () => { if (currentIndex.value < totalQuestions.value - 1) currentIndex.value++ }
const setQuestion = (index) => { if (index >=0 && index < totalQuestions.value) currentIndex.value = index }

const handleNextOrPreview = () => {
  if (currentIndex.value < totalQuestions.value - 1) {
    nextQuestion()
    return
  }
  // 到最后一题：进入整卷预览
  previewMode.value = true
  // 移除自动滚动，保持当前位置
}

const isAnswered = (item) => {
  if (!item) return false
  const type = item.questionTypeName
  if (type === '综合' || type === '资料' || type === '完形填空' || type === '阅读理解') {
      if (item.questionList && item.questionList.length > 0) {
          return item.questionList.every(subQ => isAnswered(subQ))
      }
      return false
  }
  if (type === '单选' || type === '判断') {
    return item.newAnswer !== null && item.newAnswer !== undefined && String(item.newAnswer).toString().trim() !== ''
  }
  if (type === '多选') {
    return Array.isArray(item.checkList) && item.checkList.length > 0
  }
  if (type === '填空' || type === '名词解析' || type === '论述' || type === '计算' || type === '程序') {
    return item.newAnswer !== null && item.newAnswer !== undefined && String(item.newAnswer).toString().trim() !== ''
  }
  return false
}

const totalQuestions = computed(() => (data.testPaperData.questions || []).length)
const answeredCount = computed(() => (data.testPaperData.questions || []).filter(q => isAnswered(q)).length)
const unansweredCount = computed(() => totalQuestions.value - answeredCount.value)

const goToQuestion = (index) => {
  // 在非预览模式下设置为当前题，在预览模式下滚动到对应题
  if (!previewMode.value) {
    setQuestion(index)
    return
  }
  const el = document.getElementById('q-' + index)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const typeLabel = (typeName) => {
  const map = {
    '单选': '单选',
    '多选': '多选',
    '判断': '判断',
    '填空': '填空',
    '名词解析': '名词解析',
    '论述': '论述',
    '计算': '计算',
    '程序': '程序',
    '资料': '资料',
    '完形填空': '完形填空',
    '阅读理解': '阅读理解',
    '综合': '综合',
    '简答题': '简答',
    '复合题': '复合'
  }
  return map[typeName] || '其它'
}

// 数组乱序函数（Fisher-Yates洗牌算法）
const shuffleArray = (array) => {
  const arr = [...array]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

// 选项乱序函数（打乱选项顺序，但保存原始标识用于提交）
const shuffleChoices = (choiceList) => {
  if (!choiceList || choiceList.length === 0) return
  
  // 保存每个选项的原始identification
  choiceList.forEach(choice => {
    choice.originalIdentification = choice.identification
  })
  
  // 对整个选项数组进行乱序
  const shuffled = shuffleArray(choiceList)
  
  // 重新分配显示的identification为ABCD顺序
  const identifications = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
  shuffled.forEach((choice, index) => {
    choice.identification = identifications[index]
  })
  
  // 将打乱后的数组复制回原数组
  choiceList.length = 0
  choiceList.push(...shuffled)
}

// 题型顺序定义
const questionTypeOrder = {
  '判断题': 1,
  '单选题': 2,
  '多选题': 3,
  '填空题': 4,
  '简答题': 5,
  '复合题': 6
}

const groupedQuestions = computed(() => {
  const groups = {}
  const list = (data.testPaperData.questions || [])
  list.forEach((q, idx) => {
    const t = q.questionTypeName || '其它'
    if (!groups[t]) {
      groups[t] = { typeName: t, label: typeLabel(t), questions: [] }
    }
    // keep reference to original index
    groups[t].questions.push({ ...q, __index: idx })
  })
  // calculate answered/unanswered per group
  const result = Object.values(groups).map(g => {
    const answered = g.questions.filter(q => isAnswered(q)).length
    return { ...g, answered, unanswered: g.questions.length - answered }
  })
  // compute total score per group (如果有score字段的话)
  const withScore = result.map(g => ({ ...g, totalScore: g.questions.reduce((s, q) => s + (Number(q.score) || 0), 0) }))
  
  // 按照题型顺序排序
  return withScore.sort((a, b) => {
    const orderA = questionTypeOrder[a.typeName] || 999
    const orderB = questionTypeOrder[b.typeName] || 999
    return orderA - orderB
  })
})

const loadTestPaper = async () => {
  data.testId = router.currentRoute.value.query.testId
  
  // 首先检查是否已经提交过试卷
  try {
    const checkRes = await request.get('/testPaper/check/' + data.testId)
    if (checkRes.code !== '200') {
      ElMessage.error(checkRes.msg || '该门考试您已经提交过试卷了，入口已关闭')
      setTimeout(() => {
        location.href = '/front/home'
      }, 1500)
      return
    }
  } catch (error) {
    ElMessage.error('检查考试状态失败')
    setTimeout(() => {
      location.href = '/front/home'
    }, 1500)
    return
  }
  
  request.get('/test/selectById/' + data.testId).then(async res => {
    if (res.code === '200') {
      data.testPaperData = res.data.testPaper
      data.testInfo = res.data

      // 初始化多选题的checkList为空数组
      if (data.testPaperData.questions) {
        data.testPaperData.questions.forEach(q => {
          if (q.questionTypeName === '多选') {
            if (!q.checkList) {
              q.checkList = []
            }
          }
          // 处理复合题中的子题
          if (q.questionList && q.questionList.length > 0) {
            q.questionList.forEach(subQ => {
              if (subQ.questionTypeName === '多选') {
                if (!subQ.checkList) {
                  subQ.checkList = []
                }
              }
            })
          }
        })
      }

      // 处理题目乱序
      if (data.testInfo.weatherQuestionUnordered && data.testPaperData.questions) {
        // 按题型分组
        const grouped = {}
        data.testPaperData.questions.forEach(q => {
          const type = q.questionTypeName || 'other'
          if (!grouped[type]) grouped[type] = []
          grouped[type].push(q)
        })
        
        // 对每组题目进行乱序
        Object.keys(grouped).forEach(type => {
          grouped[type] = shuffleArray(grouped[type])
        })
        
        // 按题型顺序合并回questions数组
        const orderedTypes = ['判断题', '单选题', '多选题', '填空题', '简答题', '复合题']
        data.testPaperData.questions = []
        orderedTypes.forEach(type => {
          if (grouped[type]) {
            data.testPaperData.questions.push(...grouped[type])
          }
        })
        // 添加其他未在顺序中的题型
        Object.keys(grouped).forEach(type => {
          if (!orderedTypes.includes(type)) {
            data.testPaperData.questions.push(...grouped[type])
          }
        })
      }

      // 处理选项乱序
      if (data.testInfo.weatherChoiceUnordered && data.testPaperData.questions) {
        data.testPaperData.questions.forEach(q => {
          // 判断题不需要乱序
          if (q.questionTypeName !== '判断题') {
            // 处理普通选择题的选项
            if (q.choiceList && q.choiceList.length > 0) {
              shuffleChoices(q.choiceList)
            }
          }
          // 处理复合题中子题的选项
          if (q.questionList && q.questionList.length > 0) {
            q.questionList.forEach(subQ => {
              // 判断题不需要乱序
              if (subQ.questionTypeName !== '判断题' && subQ.choiceList && subQ.choiceList.length > 0) {
                shuffleChoices(subQ.choiceList)
              }
            })
          }
        })
      }

      // 恢复之前的答题记录（混合模式）
      await restoreAnswers()

      // 根据考试配置启用防复制功能
      if (data.testInfo.weatherCopy) {
        antiCopyCleanup = setupAntiCopy()
      }
      
      // 根据考试配置启用切屏监控
      if (data.testInfo.weatherScreenSwitch) {
        document.addEventListener('visibilitychange', handleVisibilityChange)
      }

      // 计时逻辑：使用 localStorage 持久化考试开始时间，使刷新不重置已用时长
      // 剩余时间 = min(距离结束时间的剩余时间, 考试时长剩余时间)
      const maxTime = Number(data.testInfo.duration) * 60 || 0 // duration是分钟，转换为秒
      const startKey = `exam-start-${data.testId}-${user.id || user.username || 'anon'}`

      let startTs = parseInt(localStorage.getItem(startKey))
      if (!startTs) {
        startTs = Date.now()
        try { localStorage.setItem(startKey, String(startTs)) } catch (e) {}
      }

      // 计算考试时长剩余时间
      const durationRemaining = maxTime - Math.floor((Date.now() - startTs) / 1000)
      
      // 计算距离结束时间的剩余时间（秒）
      const endTime = new Date(data.testInfo.end).getTime()
      const endTimeRemaining = Math.floor((endTime - Date.now()) / 1000)
      
      // 取两者最小值作为实际剩余时间
      let remaining = Math.min(durationRemaining, endTimeRemaining)

      if (remaining <= 0) {
        try { localStorage.removeItem(startKey) } catch (e) {}
        clearAllAnswers() // 清除答题记录
        ElMessage.error('时间到')
        submitPaper()
        return
      }

      const updateRemaining = () => {
        data.hour = Math.floor(remaining / 3600)
        const rem = Math.floor(remaining % 3600)
        data.minutes = Math.floor(rem / 60)
        data.seconds = Math.floor(rem % 60)
      }

      updateRemaining()
      clearInterval(time)
      time = setInterval(() => {
        remaining--
        if (remaining >= 0) {
          updateRemaining()
        } else {
          clearInterval(time)
          try { localStorage.removeItem(startKey) } catch (e) {}
          clearAllAnswers() // 清除答题记录
          ElMessage.error('时间到')
          submitPaper()
        }
      }, 1000)

    } else {
      ElMessage.error(res.msg)
    }
  })
}
const submitPaper = (isAbnormal = false) => {
  clearInterval(time)
  // 清理持久化开始时间（若存在）
  try { localStorage.removeItem(`exam-start-${data.testId}-${user.id || user.username || 'anon'}`) } catch (e) {}
  
  // 深拷贝questions用于提交，避免修改原数据
  const questionsToSubmit = JSON.parse(JSON.stringify(data.testPaperData.questions))
  
  // 如果选项被乱序了，需要将学生选择的显示标识转换回原始标识
  if (data.testInfo.weatherChoiceUnordered) {
    questionsToSubmit.forEach((q, qIndex) => {
      const originalQ = data.testPaperData.questions[qIndex]
      
      // 处理普通选择题
      if (originalQ.choiceList && originalQ.choiceList.length > 0 && q.questionTypeName !== '判断题') {
        // 单选题和判断题：转换newAnswer
        if (q.newAnswer && originalQ.choiceList[0].originalIdentification) {
          const selectedChoice = originalQ.choiceList.find(c => c.identification === q.newAnswer)
          if (selectedChoice && selectedChoice.originalIdentification) {
            q.newAnswer = selectedChoice.originalIdentification
          }
        }
        // 多选题：转换checkList
        if (q.checkList && q.checkList.length > 0) {
          q.checkList = q.checkList.map(displayId => {
            const selectedChoice = originalQ.choiceList.find(c => c.identification === displayId)
            return selectedChoice && selectedChoice.originalIdentification ? selectedChoice.originalIdentification : displayId
          })
        }
      }
      
      // 处理复合题的子题
      if (q.questionList && q.questionList.length > 0) {
        q.questionList.forEach((subQ, subIndex) => {
          const originalSubQ = originalQ.questionList[subIndex]
          if (originalSubQ.choiceList && originalSubQ.choiceList.length > 0 && subQ.questionTypeName !== '判断题') {
            // 单选题：转换newAnswer
            if (subQ.newAnswer && originalSubQ.choiceList[0].originalIdentification) {
              const selectedChoice = originalSubQ.choiceList.find(c => c.identification === subQ.newAnswer)
              if (selectedChoice && selectedChoice.originalIdentification) {
                subQ.newAnswer = selectedChoice.originalIdentification
              }
            }
            // 多选题：转换checkList
            if (subQ.checkList && subQ.checkList.length > 0) {
              subQ.checkList = subQ.checkList.map(displayId => {
                const selectedChoice = originalSubQ.choiceList.find(c => c.identification === displayId)
                return selectedChoice && selectedChoice.originalIdentification ? selectedChoice.originalIdentification : displayId
              })
            }
          }
        })
      }
    })
  }
  
  // 提交试卷
  const submitData = {
    id: data.testId,
    testPaper: {
      id: data.testPaperData.id,
      questions: questionsToSubmit
    },
    cheat: isAbnormal ? 1 : 0  // 如果是异常提交则标记作弊
  }
  return request.post('/score/add', submitData).then(res => {
    if (res.code === '200') {
      // 提交成功后清除答题记录
      clearAllAnswers()
      ElMessage.success('提交成功')
      setTimeout(() => {
        location.href = '/front/score'
      }, 500)
    } else {
      ElMessage.error(res.msg)
    }
    return res
  })
}

const confirmSubmit = () => {
  ElMessageBox.confirm('是否确认提交试卷？', '确认', {
    confirmButtonText: '确定', 
    cancelButtonText: '取消', 
    type: 'warning',
    buttonSize: 'default',
    confirmButtonClass: 'el-button--plain',
    cancelButtonClass: 'el-button--plain'
  }).then(() => {
    submitPaper()
  }).catch(() => {})
}
loadTestPaper()

// 防复制功能
let antiCopyCleanup = null
const setupAntiCopy = () => {
  const container = document.querySelector('.questions')
  if (!container) return null
  
  const preventCopy = (e) => {
    e.preventDefault()
    ElMessage.warning('考试期间禁止复制')
    return false
  }
  
  const preventPaste = (e) => {
    e.preventDefault()
    ElMessage.warning('考试期间禁止粘贴')
    return false
  }
  
  container.addEventListener('copy', preventCopy)
  container.addEventListener('cut', preventCopy)
  container.addEventListener('paste', preventPaste)
  container.addEventListener('contextmenu', preventCopy)
  
  return () => {
    container.removeEventListener('copy', preventCopy)
    container.removeEventListener('cut', preventCopy)
    container.removeEventListener('paste', preventPaste)
    container.removeEventListener('contextmenu', preventCopy)
  }
}

// 切屏监控功能
let screenSwitchCount = 0
let lastSwitchTime = 0

const handleVisibilityChange = () => {
  if (!document.hidden) return // 页面变为可见时，不做处理
  
  const now = Date.now()
  if (screenSwitchCount >= 3) return // 已经达到3次，不再计数
  
  // 避免短时间内重复触发
  if (now - lastSwitchTime < 2000) return
  
  lastSwitchTime = now
  screenSwitchCount++
  
  ElMessage.warning(`检测到切屏行为 (${screenSwitchCount}/3次)，请专心答题！`)
  
  if (screenSwitchCount >= 3) {
    ElMessageBox.confirm(
      '您已连续切屏3次，系统将自动提交试卷并记录作弊。',
      '警告',
      {
        confirmButtonText: '确定',
        showCancelButton: false,
        type: 'error',
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    ).then(() => {
      submitPaper(true) // 作弊提交
    })
  }
}

onMounted(() => {
  // 进入考试模式，隐藏顶部公共公告与导航
  document.body.classList.add('exam-mode')
})

onBeforeUnmount(() => {
  // 退出考试模式，恢复顶部展示
  document.body.classList.remove('exam-mode')
  clearInterval(time)
  
  // 清理防复制监听
  if (antiCopyCleanup) {
    antiCopyCleanup()
    antiCopyCleanup = null
  }
  
  // 清理切屏监听
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

const leaveExam = () => {
  ElMessageBox.confirm('离开考试将返回上一页，考试时间会继续计时，是否确认离开？', '提示', {
    confirmButtonText: '确定', 
    cancelButtonText: '取消', 
    type: 'warning',
    buttonSize: 'default',
    confirmButtonClass: 'el-button--plain',
    cancelButtonClass: 'el-button--plain'
  }).then(() => {
    // 不提交试卷，直接返回上一页，考试时间在 localStorage 中继续计时
    try { router.back() } catch (e) { location.href = '/front/home' }
  }).catch(() => {})
}

// 处理考试异常（人脸识别连续3次失败）
const handleExamAbnormal = async (info) => {
  // 向后端报告异常并标记作弊
  await request.post('/monitor/reportAbnormal', {
    examId: info.examId,
    studentId: info.studentId,
    warningCount: info.warningCount,
    reason: '人脸识别连续3次不匹配'
  })
  
  // 自动提交试卷，成绩为0，标记作弊
  ElMessage.warning('检测到考试异常，系统将自动提交试卷')
  
  // 延迟2秒后自动提交
  setTimeout(() => {
    submitPaper(true) // 传入true表示是异常提交
  }, 2000)
}
</script>
<style scoped>
.el-radio-group {
  display: block;
}
.el-radio {
  display: block;
}
.el-checkbox {
  display: block;
}

.exam-container {
  padding: 50px 30px 20px;
  height: calc(100vh - 60px);
  overflow: hidden;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 50%, #fafcff 100%);
}

.paper-header {
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 16px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08);
  border: 1px solid #d9ecff;
}

.paper-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1890ff;
  padding: 12px 16px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e6f7ff 0%, #d9ecff 100%);
  border-left: 4px solid #1890ff;
  display: block;
  width: 100%;
}

.leave-btn {
  position: fixed;
  top: 16px;
  right: 20px;
  z-index: 1100;
  background: rgba(255,255,255,0.95);
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
  backdrop-filter: blur(10px);
}

.paper-meta {
  display: flex;
  gap: 24px;
  padding: 8px 16px;
  background: #f6fbff;
  border-radius: 4px;
}

.paper-meta span {
  color: #595959;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.paper-meta span::before {
  content: '•';
  color: #1890ff;
  font-size: 18px;
}

.questions {
  position: relative;
  background: #ffffff;
  padding: 20px 24px 40px;
  border-radius: 8px;
  background-clip: padding-box;
  height: calc(100vh - 230px);
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08);
  border: 1px solid #d9ecff;
}

.question-card {
  background: transparent;
  padding: 0;
  margin: 0;
}

.questions .question-card + .question-card {
  border-top: 2px solid #e6f7ff;
  padding-top: 16px;
  margin-top: 16px;
}

.question-card:first-child {
  padding-top: 0;
}

.watermark {
  position: absolute;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  opacity: 0.08;
  background-repeat: repeat;
  background-size: 210px 105px;
  background-position: 0 0;
  mix-blend-mode: multiply;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  margin-left: -8px;
  margin-right: -8px;
  padding: 12px 16px 12px 12px;
  background: linear-gradient(135deg, #f6fbff 0%, #e6f7ff 100%);
  border-radius: 6px;
  border-left: 4px solid #1890ff;
}

.question-body {
  padding: 12px 16px;
}

.score {
  color: #8c8c8c;
  font-size: 13px;
  margin-left: 8px;
  font-weight: 500;
}

.sidebar {
  background: linear-gradient(180deg, #e6f7ff 0%, #f0f9ff 50%, #fafcff 100%);
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.12);
  border: 1px solid #d9ecff;
  height: calc(100vh - 80px);
  overflow-y: auto;
  overflow-x: hidden;
  position: sticky;
  top: 20px;
}

.sidebar .timer,
.sidebar .overview {
  background: #ffffff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06);
  border: 1px solid #e6f7ff;
}

.timer {
  text-align: center;
  margin-bottom: 16px;
  padding: 16px;
}

.timer-title {
  color: #595959;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}

.timer-value {
  font-size: 28px;
  color: #ff4d4f;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(255, 77, 79, 0.15);
}

.overview {
  margin-top: 16px;
}

.overview-header {
  font-weight: 700;
  margin-bottom: 12px;
  color: #1890ff;
  font-size: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e6f7ff;
}

.overview-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.overview-stats .el-tag {
  flex: 1;
  justify-content: center;
  min-width: fit-content;
  font-weight: 500;
}

.question-list {
  margin-top: 12px;
}

.question-group {
  margin-bottom: 16px;
  padding: 12px;
  background: #f6fbff;
  border-radius: 6px;
}

.group-header {
  font-weight: 600;
  margin-bottom: 10px;
  color: #262626;
  font-size: 14px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #e6f7ff 0%, #d9ecff 100%);
  border-radius: 4px;
  border-left: 3px solid #1890ff;
}

.group-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.group-buttons .el-button {
  margin: 0 !important;
  min-width: 36px;
  font-weight: 600;
}

.submit-area {
  text-align: center;
  margin: 32px 0 40px;
  padding: 20px;
  background: linear-gradient(135deg, #e6f7ff 0%, #d9ecff 100%);
  border-radius: 8px;
}

.submit-area .el-button {
  padding: 14px 48px;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.single-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  margin-left: -8px;
  margin-right: -8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f6fbff 0%, #e6f7ff 100%);
  border-radius: 6px;
  border: 1px solid #d9ecff;
}

.single-nav .el-button {
  font-weight: 500;
}

.single-nav .nav-info {
  color: #595959;
  font-weight: 500;
  padding: 4px 12px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #d9ecff;
}

/* 防复制样式 */
.no-copy {
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}
</style>
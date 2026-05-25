<template>
  <div class="test-paper-edit">
    <!-- 顶部操作栏 -->
    <div class="header-bar">
      <el-button plain @click="goBack" icon="ArrowLeft">返回</el-button>
      <div class="header-info">
        <span class="paper-title">{{ data.paperName }}</span>
        <span class="course-name">{{ data.courseName }}</span>
        <span class="difficulty-badge" v-if="data.currentDifficulty !== null">
          <el-tag :type="getDifficultyType(data.currentDifficulty)" effect="dark">
            难度系数: {{ data.currentDifficulty.toFixed(2) }}
          </el-tag>
        </span>
      </div>
      <el-button type="primary" plain @click="savePaper" :loading="data.saving">保存试卷</el-button>
    </div>

    <!-- 主体区域 -->
    <div class="main-content">
      <!-- 左侧题库 -->
      <div class="question-bank">
        <div class="bank-header">
          <h3>题库</h3>
          <el-input
            v-model="data.searchKeyword"
            placeholder="搜索题目..."
            prefix-icon="Search"
            clearable
            @input="filterQuestions"
          />
        </div>
        
        <div class="bank-content" v-loading="data.loading">
          <el-empty v-if="!data.filteredQuestions.length" description="暂无题目" />
          
          <!-- 按题型分组显示 -->
          <div v-for="(group, index) in groupedQuestions" :key="index" class="question-group">
            <div class="group-title">
              <span class="group-number">{{ getChineseNumber(index + 1) }}</span>
              <span class="group-name">、{{ group.typeName }}</span>
              <span class="group-count">（共{{ group.questions.length }}题）</span>
            </div>
            
            <div class="question-list">
              <div
                v-for="question in group.questions"
                :key="question.id"
                class="question-item"
                :class="{ selected: isQuestionSelected(question.id), disabled: isQuestionDisabled(question.id) }"
                @click="addQuestion(question)"
              >
                <div class="question-header">
                  <el-tag :type="getQuestionTypeStyle(question.questionTypeName).type" :effect="getQuestionTypeStyle(question.questionTypeName).effect" size="small">
                    {{ question.questionTypeName }}
                  </el-tag>
                  <span class="difficulty">难度: {{ question.level }}</span>
                </div>
                <div class="question-stem" v-html="formatQuestionStem(question.questionStem)"></div>
                
                <!-- 显示选择题所有选项 -->
                <div v-if="question.questionTypeVariety === 'choice' && question.choiceList && question.choiceList.length" class="choices-full">
                  <div v-for="choice in question.choiceList" :key="choice.id" class="choice-full-item">
                    <span class="choice-label">{{ choice.identification }}.</span>
                    <span class="choice-content">{{ choice.content }}</span>
                    <el-tag v-if="choice.flag" type="success" size="small" class="choice-correct-tag">✓</el-tag>
                  </div>
                </div>
                
                <!-- 显示复合题子题 -->
                <div v-if="question.questionTypeVariety === 'composite' && question.questionList && question.questionList.length" class="composites-full">
                  <div v-for="(subQ, idx) in question.questionList" :key="subQ.id" class="composite-sub-item">
                    <div class="sub-title">({{ idx + 1 }}) {{ subQ.questionStem }}</div>
                    <div v-if="subQ.choiceList && subQ.choiceList.length" class="sub-choices">
                      <div v-for="choice in subQ.choiceList" :key="choice.id" class="choice-full-item">
                        <span class="choice-label">{{ choice.identification }}.</span>
                        <span class="choice-content">{{ choice.content }}</span>
                        <el-tag v-if="choice.flag" type="success" size="small" class="choice-correct-tag">✓</el-tag>
                      </div>
                    </div>
                  </div>
                </div>                <div class="question-footer">
                  <el-icon v-if="isQuestionSelected(question.id)" color="#67c23a"><CircleCheck /></el-icon>
                  <span v-else class="add-btn">点击添加</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧试卷预览 -->
      <div class="paper-preview">
        <div class="preview-header">
          <h2 class="preview-title">{{ data.paperName }}</h2>
          <div class="preview-info">
            <span>课程：{{ data.courseName }}</span>
            <span>总分：{{ data.totalScore }}</span>
            <span>题目数：{{ data.selectedQuestions.length }}</span>
          </div>
        </div>

        <div class="preview-content">
          <el-empty v-if="!data.selectedQuestions.length" description="请从左侧题库中选择题目" />
          
          <!-- 循环渲染每个题型分组 -->
          <div v-for="(group, groupIndex) in selectedQuestionsGrouped" :key="group.typeName">
            <!-- 题型标题 - 不参与拖拽 -->
            <div class="selected-group-title">
              <span class="group-number">{{ getChineseNumber(groupIndex + 1) }}</span>
              <span class="group-name">、{{ group.typeName }}</span>
              <span class="group-info">（共{{ group.questions.length }}题）</span>
            </div>
            
            <!-- 该题型下的题目列表 - 可拖拽 -->
            <draggable
              v-model="group.questions"
              :item-key="(item) => item.id"
              handle=".drag-handle"
              animation="200"
              ghost-class="sortable-ghost"
              @end="updateSequenceAfterDrag"
            >
              <template #item="{ element }">
                <div class="selected-question">
                  <div class="drag-handle drag-indicator">
                    <el-icon><Menu /></el-icon>
                  </div>
                  <div class="question-number">
                    <span>{{ element.sequence }}.</span>
                  </div>
                  <div class="question-content">
                    <div class="question-text" v-html="formatQuestionStem(element.questionStem)"></div>
                    
                    <!-- 显示选择题所有选项 -->
                    <div v-if="element.questionTypeVariety === 'choice' && element.choiceList && element.choiceList.length" class="choices">
                      <div v-for="choice in element.choiceList" :key="choice.id" class="choice-item">
                        <span class="choice-label">{{ choice.identification }}.</span>
                        <span class="choice-content">{{ choice.content }}</span>
                        <el-tag v-if="choice.flag" type="success" size="small">正确</el-tag>
                      </div>
                    </div>
                    
                    <!-- 显示复合题子题 -->
                    <div v-if="element.questionTypeVariety === 'composite' && element.questionList && element.questionList.length" class="composites">
                      <div v-for="(composite, cIndex) in element.questionList" :key="composite.id" class="composite-item">
                        <div class="composite-number">({{ cIndex + 1 }})</div>
                        <div class="composite-content">
                          <div v-html="formatQuestionStem(composite.questionStem)"></div>
                          <!-- 复合题子题的选项 -->
                          <div v-if="composite.choiceList && composite.choiceList.length" class="sub-choices">
                            <div v-for="choice in composite.choiceList" :key="choice.id" class="choice-item">
                              <span class="choice-label">{{ choice.identification }}.</span>
                              <span class="choice-content">{{ choice.content }}</span>
                              <el-tag v-if="choice.flag" type="success" size="small">正确</el-tag>
                            </div>
                          </div>
                        </div>
                        <div class="composite-score">
                          <el-input-number
                            v-model="composite.subScore"
                            :min="1"
                            :max="100"
                            size="small"
                            controls-position="right"
                            @change="updateCompositeScore(element)"
                            style="width: 120px"
                          />
                          <span class="score-unit">分</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="question-actions">
                    <div class="action-row">
                      <div class="score-input">
                        <el-input-number
                          v-model="element.score"
                          :min="1"
                          :max="100"
                          size="small"
                          controls-position="right"
                          @change="updateTotalScore"
                        />
                        <span class="score-unit">分</span>
                      </div>
                      <el-button
                        type="danger"
                        size="small"
                        icon="Delete"
                        @click="removeQuestion(element.id)"
                      >删除</el-button>
                    </div>
                  </div>
                </div>
                </template>
            </draggable>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Menu } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import draggable from 'vuedraggable'
import { getQuestionTypeStyle } from '@/utils/questionTypeStyles.js'

const router = useRouter()
const route = useRoute()

const data = reactive({
  courseId: null,
  courseName: '',
  paperName: '',
  paperId: null,
  loading: false,
  saving: false,
  searchKeyword: '',
  allQuestions: [],
  filteredQuestions: [],
  selectedQuestions: [],
  totalScore: 0,
  draggedQuestion: null,
  currentDifficulty: null // 当前试卷难度系数
})

// 题型顺序定义
const questionTypeOrder = {
  '单选': 1,
  '多选': 2,
  '判断': 3,
  '填空': 4,
  '名词解析': 5,
  '论述': 6,
  '计算': 7,
  '程序': 8,
  '资料': 9,
  '完形填空': 10,
  '阅读理解': 11,
  '综合': 12
}

// 中文数字转换
const getChineseNumber = (num) => {
  const chinese = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  return chinese[num - 1] || num
}

// 格式化题干
const formatQuestionStem = (stem) => {
  if (!stem) return ''
  return stem.replace(/\n/g, '<br/>')
}

// 按题型分组题库中的题目
const groupedQuestions = computed(() => {
  const groups = {}
  data.filteredQuestions.forEach(q => {
    if (!groups[q.questionTypeName]) {
      groups[q.questionTypeName] = {
        typeName: q.questionTypeName,
        questions: []
      }
    }
    groups[q.questionTypeName].questions.push(q)
  })
  
  // 按题型顺序排序
  return Object.values(groups).sort((a, b) => {
    const orderA = questionTypeOrder[a.typeName] || 999
    const orderB = questionTypeOrder[b.typeName] || 999
    return orderA - orderB
  })
})

// 按题型分组已选题目
const selectedQuestionsGrouped = computed(() => {
  const groups = {}
  data.selectedQuestions.forEach(q => {
    if (!groups[q.questionTypeName]) {
      groups[q.questionTypeName] = {
        typeName: q.questionTypeName,
        questions: []
      }
    }
    groups[q.questionTypeName].questions.push(q)
  })
  
  // 按题型顺序排序
  return Object.values(groups).sort((a, b) => {
    const orderA = questionTypeOrder[a.typeName] || 999
    const orderB = questionTypeOrder[b.typeName] || 999
    return orderA - orderB
  })
})

// 判断题目是否已选
const isQuestionSelected = (questionId) => {
  return data.selectedQuestions.some(q => q.id === questionId)
}

// 判断题目是否禁用（复合题的子题不能单独选择）
const isQuestionDisabled = (questionId) => {
  const question = data.allQuestions.find(q => q.id === questionId)
  return question && question.flag === 0
}

// 搜索过滤题目
const filterQuestions = () => {
  if (!data.searchKeyword.trim()) {
    data.filteredQuestions = data.allQuestions.filter(q => q.flag === 1 || q.flag === true)
    return
  }
  const keyword = data.searchKeyword.toLowerCase()
  data.filteredQuestions = data.allQuestions.filter(q => {
    return (q.flag === 1 || q.flag === true) && (
      q.questionStem.toLowerCase().includes(keyword) ||
      q.questionTypeName.toLowerCase().includes(keyword)
    )
  })
}

// 添加题目
const addQuestion = (question) => {
  if (isQuestionDisabled(question.id)) {
    ElMessage.warning('该题目不能单独选择')
    return
  }
  
  if (isQuestionSelected(question.id)) {
    ElMessage.info('该题目已添加')
    return
  }
  
  // 添加题目，默认分值为5分
  const newQuestion = {
    ...question,
    score: 5,
    sequence: data.selectedQuestions.length + 1
  }
  
  // 如果是复合题，初始化子题分值
  if (question.questionTypeVariety === 'composite' && question.questionList && question.questionList.length) {
    const subQuestionCount = question.questionList.length
    const averageScore = Math.floor(5 / subQuestionCount)
    const remainder = 5 % subQuestionCount
    
    newQuestion.questionList = question.questionList.map((subQ, index) => ({
      ...subQ,
      subScore: averageScore + (index === subQuestionCount - 1 ? remainder : 0)
    }))
  }
  
  data.selectedQuestions.push(newQuestion)
  sortSelectedQuestions() // 添加后自动按题型顺序排序
  updateTotalScore()
  ElMessage.success('添加成功')
}

// 移除题目
const removeQuestion = (questionId) => {
  const index = data.selectedQuestions.findIndex(q => q.id === questionId)
  if (index > -1) {
    data.selectedQuestions.splice(index, 1)
    updateSequence()
    updateTotalScore()
  }
}

// 更新序号
const updateSequence = () => {
  data.selectedQuestions.forEach((q, index) => {
    q.sequence = index + 1
  })
}

// 按题型顺序排序已选题目
const sortSelectedQuestions = () => {
  data.selectedQuestions.sort((a, b) => {
    const orderA = questionTypeOrder[a.questionTypeName] || 999
    const orderB = questionTypeOrder[b.questionTypeName] || 999
    if (orderA !== orderB) {
      return orderA - orderB
    }
    // 同类型题目保持相对顺序（按sequence排序）
    return (a.sequence || 0) - (b.sequence || 0)
  })
  updateSequence()
}

// 拖拽结束后更新序号（从分组数据同步回selectedQuestions）
const updateSequenceAfterDrag = () => {
  // 从分组数据重建selectedQuestions数组
  const newSelectedQuestions = []
  selectedQuestionsGrouped.value.forEach(group => {
    newSelectedQuestions.push(...group.questions)
  })
  data.selectedQuestions = newSelectedQuestions
  updateSequence()
}

// 更新复合题总分（根据子题分值求和）
const updateCompositeScore = (question) => {
  if (question.questionTypeVariety === 'composite' && question.questionList) {
    question.score = question.questionList.reduce((sum, subQ) => sum + (subQ.subScore || 0), 0)
    updateTotalScore()
  }
}

// 更新总分
const updateTotalScore = () => {
  data.totalScore = data.selectedQuestions.reduce((sum, q) => sum + (q.score || 0), 0)
  calculateDifficulty()
}

// 计算试卷难度系数
const calculateDifficulty = () => {
  if (data.selectedQuestions.length === 0 || data.totalScore === 0) {
    data.currentDifficulty = null
    return
  }
  
  let weightedDifficulty = 0
  for (const q of data.selectedQuestions) {
    if (q.level && q.score) {
      const weight = q.score / data.totalScore
      weightedDifficulty += q.level * weight
    }
  }
  
  data.currentDifficulty = weightedDifficulty
}

// 难度系数标签类型
const getDifficultyType = (difficulty) => {
  if (difficulty < 0.3) return 'danger' // 高难度
  if (difficulty < 0.5) return 'warning' // 中等偏难
  if (difficulty < 0.7) return '' // 中等
  return 'success' // 简单
}

// 加载题库
const loadQuestions = async () => {
  data.loading = true
  try {
    const res = await request.get('/question/selectAll', {
      params: { courseId: data.courseId }
    })
    if (res.code === '200') {
      data.allQuestions = res.data || []
      
      // 只显示可以独立选择的题目（flag=1或flag=true）
      data.filteredQuestions = data.allQuestions.filter(q => q.flag === 1 || q.flag === true)
      
      // 如果是编辑模式，加载已有题目
      if (data.paperId) {
        await loadPaperQuestions()
      }
    } else {
      ElMessage.error(res.msg || '加载题目失败')
    }
  } catch (error) {
    ElMessage.error('加载题目失败')
  } finally {
    data.loading = false
  }
}

// 加载试卷已有题目（编辑模式）
const loadPaperQuestions = async () => {
  try {
    const res = await request.get('/testPaper/selectById/' + data.paperId)
    if (res.code === '200' && res.data.questions) {
      // 将后端返回的题目映射到selectedQuestions，使用typeScore作为分值
      data.selectedQuestions = res.data.questions.map((q, index) => {
        const question = {
          ...q,
          score: q.typeScore || 5,  // 使用后端返回的typeScore
          sequence: index + 1
        }
        
        // 如果是复合题，初始化子题的分值
        if (q.questionTypeVariety === 'composite' && q.questionList && q.questionList.length) {
          question.questionList = q.questionList.map(subQ => ({
            ...subQ,
            subScore: subQ.typeScore || 1  // 使用后端返回的子题分值
          }))
        }
        
        return question
      })
      sortSelectedQuestions() // 加载后按题型顺序排序
      updateTotalScore()
    }
  } catch (error) {  }
}

// 保存试卷
const savePaper = async () => {
  if (!data.selectedQuestions.length) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  
  ElMessageBox.confirm(
    `确认保存试卷吗？共${data.selectedQuestions.length}道题，总分${data.totalScore}分`,
    '保存确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    data.saving = true
    try {
      // 构建试卷数据
      const paperData = {
        id: data.paperId,
        name: data.paperName,
        courseId: data.courseId,
        type: '手动选题',
        level: data.currentDifficulty, // 保存难度系数到数据库
        questionList: data.selectedQuestions.map(q => {
          const item = {
            questionId: q.id,
            sequence: q.sequence,
            score: q.score
          }
          
          // 如果是复合题，添加子题分值信息
          if (q.questionTypeVariety === 'composite' && q.questionList && q.questionList.length) {
            item.subQuestions = q.questionList.map(subQ => ({
              questionId: subQ.id,
              score: subQ.subScore || 1
            }))
          }
          
          return item
        })
      }
      
      let res
      if (data.paperId) {
        // 编辑模式：调用更新接口
        res = await request.put('/testPaper/update', paperData)
      } else {
        // 新增模式：调用新增接口
        res = await request.post('/testPaper/add', paperData)
      }
      
      if (res.code === '200') {
        ElMessage.success('保存成功')
        setTimeout(() => {
          router.push('/manager/testPaper')
        }, 500)
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      data.saving = false
    }
  }).catch(() => {})
}

// 返回
const goBack = () => {
  if (data.selectedQuestions.length) {
    ElMessageBox.confirm('有未保存的内容，确认返回吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

// 初始化
onMounted(() => {
  // 从路由参数获取信息
  data.courseId = route.query.courseId
  data.courseName = route.query.courseName
  data.paperName = route.query.paperName
  data.paperId = route.query.paperId // 编辑模式会传入
  
  if (!data.courseId || !data.paperName) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  
  loadQuestions()
})
</script>

<style scoped>
.test-paper-edit {
  height: calc(100vh - 65px);
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

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
  gap: 24px;
}

.paper-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.course-name {
  font-size: 14px;
  color: #fff;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
}

.difficulty-badge {
  display: flex;
  align-items: center;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 左侧题库样式 */
.question-bank {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.bank-header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}

.bank-header h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #303133;
}

.bank-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.question-group {
  margin-bottom: 24px;
}

.group-title {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  font-weight: 600;
}

.group-number {
  font-size: 18px;
}

.group-name {
  font-size: 16px;
}

.group-count {
  margin-left: auto;
  font-size: 14px;
  opacity: 0.9;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  padding: 16px;
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.question-item:hover:not(.disabled):not(.selected) {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.question-item.selected {
  background: #f0f9ff;
  border-color: #67c23a;
}

.question-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.difficulty {
  font-size: 12px;
  color: #909399;
}

.question-stem {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.choices-full {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.choice-full-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
}

.choice-full-item:last-child {
  margin-bottom: 0;
}

.choice-full-item:hover {
  background: #f5f7fa;
}

.choice-label {
  font-weight: 600;
  min-width: 24px;
  color: #409eff;
}

.choice-content {
  flex: 1;
  line-height: 1.6;
}

.choice-correct-tag {
  margin-left: auto;
}

.composites-full {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.composite-sub-item {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}

.composite-sub-item:last-child {
  margin-bottom: 0;
}

.sub-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.6;
}

.sub-choices {
  margin-top: 8px;
}

.question-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.add-btn {
  font-size: 12px;
  color: #409eff;
}

/* 右侧预览样式 */
.paper-preview {
  width: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.preview-header {
  padding: 24px;
  border-bottom: 2px solid #e4e7ed;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
}

.preview-title {
  margin: 0 0 16px 0;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
}

.preview-info {
  display: flex;
  justify-content: space-around;
  font-size: 14px;
  opacity: 0.95;
}

.preview-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
}

.preview-content::-webkit-scrollbar {
  width: 6px;
}

.preview-content::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.preview-content::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}

.selected-group {
  margin-bottom: 32px;
}

.selected-group-title {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-left: 4px solid #409eff;
  margin-bottom: 16px;
  font-weight: 600;
  color: #303133;
}

.group-info {
  margin-left: auto;
  font-size: 14px;
  color: #909399;
}

.selected-question {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.selected-question:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.drag-indicator {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 18px;
  cursor: move;
  transition: color 0.3s;
}

.drag-indicator:hover {
  color: #409eff;
}

.question-number {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #606266;
  font-weight: 600;
  min-width: 40px;
}

.question-content {
  flex: 1;
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.question-text {
  margin-bottom: 12px;
}

.choices {
  margin-top: 12px;
  padding-left: 16px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.choice-label {
  font-weight: 600;
  min-width: 20px;
}

.composites {
  margin-top: 12px;
  padding-left: 16px;
}

.composite-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: flex-start;
}

.composite-number {
  font-weight: 600;
  color: #606266;
  min-width: 30px;
}

.composite-content {
  flex: 1;
  color: #606266;
}

.composite-score {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 140px;
}

.question-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 180px;
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: stretch;
}

.score-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f2f5;
  border-radius: 8px;
  justify-content: space-between;
}

.score-unit {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* vuedraggable 拖拽样式 */
.sortable-ghost {
  opacity: 0.5;
  transform: rotate(2deg);
  background: #e8f4ff;
}

.sortable-drag {
  opacity: 0.8;
  cursor: grabbing !important;
}

.drag-handle {
  cursor: move;
}
</style>

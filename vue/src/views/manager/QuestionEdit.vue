<template>
  <div class="question-edit">
    <!-- 顶部操作栏 -->
    <div class="header-bar">
      <el-button type="warning" plain @click="goBack" icon="ArrowLeft">返回</el-button>
      <div class="header-info">
        <span class="paper-title">{{ data.form.id ? '编辑题目' : '新增题目' }}</span>
      </div>
      <div style="display: flex; gap: 10px; align-items: center;">
        <el-tooltip content="AI识别可能不准确，请人工审核" placement="top">
          <el-icon :size="16" style="color: #ffffff; cursor: help"><QuestionFilled /></el-icon>
        </el-tooltip>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleImageUpload"
          accept="image/*"
        >
          <el-button type="success" plain icon="Picture" :loading="data.recognizing">
            <span v-if="!data.recognizing">AI识别题目</span>
            <span v-else>识别中...</span>
          </el-button>
        </el-upload>
        <el-button type="primary" plain @click="save" :loading="data.saving">保存题目</el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="main-content">
      <!-- 左侧出题表单 -->
      <div class="form-panel">
        <div class="panel-header">
          <h3>题目信息</h3>
        </div>
        
        <el-scrollbar class="panel-content">
          <el-form
            ref="formRef"
            :model="data.form"
            label-width="100px"
            style="padding: 20px"
          >
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item prop="courseId" label="选择课程">
                  <el-select v-model="data.form.courseId" placeholder="请选择课程" style="width: 100%" filterable>
                    <el-option
                      v-for="item in data.courseData"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="questionTypeId" label="选择题型">
                  <el-select v-model="data.form.questionTypeId" placeholder="请选择题型" @change="handleTypeChange" style="width: 100%">
                    <el-option label="单选题" :value="1" />
                    <el-option label="多选题" :value="2" />
                    <el-option label="判断题" :value="3" />
                    <el-option label="填空题" :value="4" />
                    <el-option label="名词解析" :value="5" />
                    <el-option label="论述题" :value="6" />
                    <el-option label="计算题" :value="7" />
                    <el-option label="程序题" :value="8" />
                    <el-option label="资料题" :value="9" />
                    <el-option label="完形填空" :value="10" />
                    <el-option label="阅读理解" :value="11" />
                    <el-option label="综合题" :value="12" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item prop="level" label="难度系数">
                  <el-input-number v-model="data.form.level" :precision="1" :step="0.1" :min="0.1" :max="1.0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item prop="questionStem" label="题目名称">
              <RichTextEditor 
                v-model="data.form.questionStem" 
                :hide-lists="true" 
                placeholder="请输入题目内容，支持富文本格式、表格、公式等..."
                height="200px"
              />
            </el-form-item>

            <!-- 单选题选项 -->
            <div v-if="data.form.questionTypeId === 1">
              <el-form-item label="题目选项">
                <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
                  <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">
                    <el-icon style="vertical-align: middle; margin-right: 4px"><InfoFilled /></el-icon>
                    请勾选正确答案
                  </div>
                  <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px">
                    <el-radio v-model="data.singleAnswer" :value="choice.identification" @change="updateChoiceFlag">
                      <span style="font-weight: bold; width: 20px; display: inline-block; text-align: center;">{{ choice.identification }}</span>
                    </el-radio>
                    <el-input v-model="choice.identification" placeholder="标识" style="width: 70px" />
                    <div style="flex: 1;">
                      <RichTextEditor v-model="choice.content" :hide-lists="true" placeholder="请输入选项内容" height="100px" />
                    </div>
                    <el-button type="danger" circle :icon="Delete" size="small" @click="delChoice(idx)" v-if="data.form.choiceList.length > 1" />
                  </div>
                  <div style="margin-top: 10px;">
                    <el-button type="primary" plain size="small" :icon="Plus" @click="addChoice">添加选项</el-button>
                  </div>
                </el-card>
              </el-form-item>
            </div>

            <!-- 多选题选项 -->
            <div v-if="data.form.questionTypeId === 2">
              <el-form-item label="题目选项">
                <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
                  <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">
                    <el-icon style="vertical-align: middle; margin-right: 4px"><InfoFilled /></el-icon>
                    请勾选正确答案（可多选）
                  </div>
                  <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px">
                    <el-checkbox v-model="choice.flag">
                      <span style="font-weight: bold; width: 20px; display: inline-block; text-align: center;">{{ choice.identification }}</span>
                    </el-checkbox>
                    <el-input v-model="choice.identification" placeholder="标识" style="width: 70px" />
                    <div style="flex: 1;">
                      <RichTextEditor v-model="choice.content" :hide-lists="true" placeholder="请输入选项内容" height="100px" />
                    </div>
                    <el-button type="danger" circle :icon="Delete" size="small" @click="delChoice(idx)" v-if="data.form.choiceList.length > 1" />
                  </div>
                  <div style="margin-top: 10px;">
                    <el-button type="primary" plain size="small" :icon="Plus" @click="addChoice">添加选项</el-button>
                  </div>
                </el-card>
              </el-form-item>
            </div>

            <!-- 判断题选项 -->
            <div v-if="data.form.questionTypeId === 3">
              <el-form-item label="题目选项">
                <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
                  <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">请选择正确答案</div>
                  <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 20px; margin-bottom: 5px">
                    <el-radio v-model="data.singleAnswer" :value="choice.identification" @change="updateChoiceFlag">
                      <el-tag :type="choice.identification === '√' ? 'success' : 'danger'" size="large" effect="dark">
                        {{ choice.identification }} {{ choice.content }}
                      </el-tag>
                    </el-radio>
                  </div>
                </el-card>
              </el-form-item>
            </div>

            <!-- 复合题 -->
            <div v-if="[9, 10, 11, 12].includes(data.form.questionTypeId)">
              <el-form-item label="子题目">
                <div style="width: 100%">
                  <div style="margin-bottom: 15px; color: #606266; font-size: 14px; display: flex; align-items: center; justify-content: space-between;">
                    <span><el-icon style="vertical-align: middle; margin-right: 4px"><Menu /></el-icon> 子题目列表（可拖拽调整顺序）</span>
                    <el-button type="success" plain size="small" :icon="Plus" @click="addSubQuestion">添加子题目</el-button>
                  </div>
                  
                  <div v-if="data.form.questionList.length === 0" style="text-align: center; color: #909399; padding: 20px; background: #f5f7fa; border-radius: 4px;">
                    暂无子题目，请点击右上角添加
                  </div>

                  <draggable
                    v-model="data.form.questionList"
                    :item-key="(item) => item.tempId || item.id || Math.random()"
                    handle=".drag-handle"
                    animation="200"
                    :disabled="false"
                  >
                    <template #item="{ element: subQ, index: idx }">
                      <div style="margin-bottom: 20px;">
                        <el-card shadow="hover" :body-style="{ padding: '15px' }">
                          <template #header>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                              <div style="display: flex; align-items: center; gap: 8px;">
                                <el-icon class="drag-handle" style="cursor: move; color: #909399; font-size: 18px;">
                                  <Menu />
                                </el-icon>
                                <span style="font-weight: bold; color: #409EFF;">子题目 {{ idx + 1 }}</span>
                              </div>
                              <el-button type="danger" link :icon="Delete" @click="delSubQuestion(idx)">删除此题</el-button>
                            </div>
                          </template>
                          
                          <el-row :gutter="15">
                            <el-col :span="12">
                              <el-form-item label="题型" label-width="60px" style="margin-bottom: 15px;">
                                <el-select v-model="subQ.questionTypeId" placeholder="请选择题型" @change="handleSubTypeChange(idx)" style="width: 100%;">
                                  <el-option label="单选题" :value="1" />
                                  <el-option label="多选题" :value="2" />
                                  <el-option label="判断题" :value="3" />
                                  <el-option label="填空题" :value="4" />
                                  <el-option label="名词解析" :value="5" />
                                  <el-option label="论述题" :value="6" />
                                  <el-option label="计算题" :value="7" />
                                  <el-option label="程序题" :value="8" />
                                </el-select>
                              </el-form-item>
                            </el-col>
                            <el-col :span="24">
                              <el-form-item label="题目" label-width="60px" style="margin-bottom: 15px;">
                                <RichTextEditor v-model="subQ.questionStem" :hide-lists="true" placeholder="请输入子题目内容" height="150px" />
                              </el-form-item>
                            </el-col>
                          </el-row>
                          
                          <!-- 子题目选项区域 -->
                          <div style="background-color: #fafafa; padding: 15px; border-radius: 4px;">
                            <!-- 单选题选项 -->
                            <div v-if="subQ.questionTypeId === 1">
                              <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">选项配置 (勾选为正确答案)</div>
                              <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                                <el-radio v-model="subQ.singleAnswer" :value="choice.identification" @change="updateSubChoiceFlag(idx)">
                                  <span style="width: 10px;"></span>
                                </el-radio>
                                <el-input v-model="choice.identification" placeholder="标识" style="width: 60px" />
                                <div style="flex: 1;">
                                  <RichTextEditor v-model="choice.content" :hide-lists="true" placeholder="选项内容" height="80px" />
                                </div>
                                <el-button type="danger" circle :icon="Delete" size="small" @click="delSubChoice(idx, cIdx)" v-if="subQ.choiceList.length > 1" />
                              </div>
                              <el-button type="primary" link size="small" :icon="Plus" @click="addSubChoice(idx)">添加选项</el-button>
                            </div>
                            
                            <!-- 多选题选项 -->
                            <div v-if="subQ.questionTypeId === 2">
                              <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">选项配置 (勾选为正确答案)</div>
                              <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                                <el-checkbox v-model="choice.flag"><span style="width: 10px;"></span></el-checkbox>
                                <el-input v-model="choice.identification" placeholder="标识" style="width: 60px" />
                                <div style="flex: 1;">
                                  <RichTextEditor v-model="choice.content" :hide-lists="true" placeholder="选项内容" height="80px" />
                                </div>
                                <el-button type="danger" circle :icon="Delete" size="small" @click="delSubChoice(idx, cIdx)" v-if="subQ.choiceList.length > 1" />
                              </div>
                              <el-button type="primary" link size="small" :icon="Plus" @click="addSubChoice(idx)">添加选项</el-button>
                            </div>
                            
                            <!-- 判断题选项 -->
                            <div v-if="subQ.questionTypeId === 3">
                              <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">正确答案</div>
                              <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                                <el-radio v-model="subQ.singleAnswer" :value="choice.identification" @change="updateSubChoiceFlag(idx)">
                                  <el-tag :type="choice.identification === '√' ? 'success' : 'danger'" effect="dark">
                                    {{ choice.identification }} {{ choice.content }}
                                  </el-tag>
                                </el-radio>
                              </div>
                            </div>
                            
                            <!-- 填空题/文本题答案 -->
                            <div v-if="[4, 5, 6, 7, 8].includes(subQ.questionTypeId)">
                              <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">参考答案</div>
                              <el-input v-if="subQ.questionTypeId === 4" v-model="subQ.answer" placeholder="请输入参考答案" />
                              <el-input v-else type="textarea" :rows="3" v-model="subQ.answer" placeholder="请输入参考答案" />
                            </div>
                          </div>

                          <!-- 解析 -->
                          <div style="margin-top: 15px;" v-if="[1,2,3].includes(subQ.questionTypeId)">
                            <el-form-item label="解析" label-width="60px" style="margin-bottom: 0;">
                              <el-input type="textarea" :rows="1" v-model="subQ.answer" placeholder="请输入题目解析（选填）" />
                            </el-form-item>
                          </div>
                        </el-card>
                      </div>
                    </template>
                  </draggable>
                  
                  <div v-if="data.form.questionList.length > 0" style="margin-top: 15px; text-align: center;">
                    <el-button type="success" plain @click="addSubQuestion" style="width: 100%; border-style: dashed;">+ 继续添加子题目</el-button>
                  </div>
                </div>
              </el-form-item>
            </div>

            <!-- 单选题、多选题、判断题的题目解析 -->
            <el-form-item
              prop="answer"
              label="题目解析"
              v-if="data.form.questionTypeId === 1 || data.form.questionTypeId === 2 || data.form.questionTypeId === 3"
            >
              <el-input type="textarea" :rows="3" v-model="data.form.answer" placeholder="请输入题目解析（选填）" />
            </el-form-item>

            <!-- 填空题答案 -->
            <el-form-item prop="answer" label="参考答案" v-if="data.form.questionTypeId === 4">
              <el-input v-model="data.form.answer" placeholder="请输入参考答案" />
            </el-form-item>

            <!-- 名词解析、论述题、计算题、程序题答案 -->
            <el-form-item prop="answer" label="参考答案" v-if="[5, 6, 7, 8].includes(data.form.questionTypeId)">
              <el-input type="textarea" :rows="5" v-model="data.form.answer" placeholder="请输入参考答案" />
            </el-form-item>
          </el-form>
        </el-scrollbar>
      </div>

      <!-- 右侧预览 -->
      <div class="preview-panel">
        <div class="preview-header">
          <h2 class="preview-title">题目预览</h2>
        </div>

        <el-scrollbar class="preview-content">
          <el-empty v-if="!data.form.questionTypeId" description="请在左侧选择题型开始出题" />
          
          <div v-else class="question-preview">
            <!-- 题目信息 -->
            <div class="preview-info">
              <el-tag :type="getQuestionTypeTag()" size="large">{{ getQuestionTypeName() }}</el-tag>
              <span class="difficulty">难度: {{ data.form.level || '未设置' }}</span>
            </div>

            <!-- 题干 -->
            <div class="question-stem">
              <div class="stem-label">题目：</div>
              <RichTextContent :content="data.form.questionStem" />
            </div>

            <!-- 选择题选项预览 -->
            <div v-if="(data.form.questionTypeId === 1 || data.form.questionTypeId === 2 || data.form.questionTypeId === 3) && data.form.choiceList.length" class="choices-preview">
              <div v-for="choice in data.form.choiceList" :key="choice.identification" class="choice-item">
                <span class="choice-label">{{ choice.identification }}.</span>
                <RichTextContent :content="choice.content" style="flex: 1;" />
                <el-tag v-if="choice.flag" type="success" size="small" class="correct-tag">✓ 正确答案</el-tag>
              </div>
            </div>

            <!-- 复合题子题预览 -->
            <div v-if="[9, 10, 11, 12].includes(data.form.questionTypeId) && data.form.questionList.length" class="composite-preview">
              <div v-for="(subQ, idx) in data.form.questionList" :key="idx" class="sub-question">
                <div class="sub-header">
                  <span class="sub-number">({{ idx + 1 }})</span>
                  <el-tag size="small">{{ getSubQuestionTypeName(subQ.questionTypeId) }}</el-tag>
                </div>
                <RichTextContent :content="subQ.questionStem" class="sub-stem" />
                
                <div v-if="subQ.choiceList && subQ.choiceList.length" class="sub-choices">
                  <div v-for="choice in subQ.choiceList" :key="choice.identification" class="choice-item">
                    <span class="choice-label">{{ choice.identification }}.</span>
                    <RichTextContent :content="choice.content" style="flex: 1;" />
                    <el-tag v-if="choice.flag" type="success" size="small">✓</el-tag>
                  </div>
                </div>
                
                <div v-if="subQ.answer && [4, 5, 6, 7, 8].includes(subQ.questionTypeId)" class="sub-answer">
                  <span class="answer-label">参考答案：</span>
                  <span>{{ subQ.answer }}</span>
                </div>
              </div>
            </div>

            <!-- 填空题/文本题答案预览 -->
            <div v-if="[4, 5, 6, 7, 8].includes(data.form.questionTypeId) && data.form.answer" class="answer-preview">
              <div class="answer-label">参考答案：</div>
              <div class="answer-content">{{ data.form.answer }}</div>
            </div>

            <!-- 解析预览 -->
            <div v-if="data.form.answer && (data.form.questionTypeId === 1 || data.form.questionTypeId === 2 || data.form.questionTypeId === 3)" class="analysis-preview">
              <div class="analysis-label">题目解析：</div>
              <div class="analysis-content">{{ data.form.answer }}</div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, InfoFilled, Menu, QuestionFilled } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import draggable from 'vuedraggable'
import RichTextEditor from '@/components/RichTextEditor.vue'
import RichTextContent from '@/components/RichTextContent.vue'

const router = useRouter()
const route = useRoute()

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  saving: false,
  recognizing: false,
  form: {
    courseId: '',
    questionStem: '',
    questionTypeId: '',
    level: 0.5,
    answer: '',
    choiceList: [],
    questionList: []
  },
  singleAnswer: '',
  courseData: []
})

// 返回
const goBack = () => {
  router.back()
}

// AI识别题目照片
const handleImageUpload = async (uploadFile) => {
  const file = uploadFile.raw
  
  // 验证文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }
  
  // 验证文件大小（限制10MB）
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return
  }

  try {
    data.recognizing = true
    ElMessage.info('正在识别题目，请稍候...')
    
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)
    
    // 调用后端接口
    const res = await request.post('/question/recognizeQuestion', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res.code === '200') {
      const aiResult = res.data
      
      // 确认是否应用识别结果
      ElMessageBox.confirm(
        '识别成功！是否应用AI识别的结果到表单？',
        '提示',
        {
          confirmButtonText: '应用',
          cancelButtonText: '取消',
          type: 'success',
        }
      ).then(() => {
        applyAIResult(aiResult)
        ElMessage.success('已应用AI识别结果')
      }).catch(() => {
        ElMessage.info('已取消应用')
      })
    } else {
      ElMessage.error(res.msg || 'AI识别失败')
    }
  } catch (error) {    ElMessage.error('识别失败：' + (error.message || '未知错误'))
  } finally {
    data.recognizing = false
  }
}

// 应用AI识别结果到表单
const applyAIResult = (aiResult) => {
  // 设置题型（但不调用handleTypeChange，避免覆盖AI数据）
  if (aiResult.questionTypeId) {
    data.form.questionTypeId = aiResult.questionTypeId
    // 初始化为空数组，后面会根据AI结果填充
    data.form.choiceList = []
    data.form.questionList = []
  }
  
  // 设置题干
  if (aiResult.questionStem) {
    data.form.questionStem = aiResult.questionStem
  }
  
  // 设置选项（选择题、判断题）
  if (aiResult.choiceList && aiResult.choiceList.length > 0) {
    data.form.choiceList = aiResult.choiceList.map(choice => ({
      identification: choice.identification || '',
      content: choice.content || '',
      flag: choice.flag || false
    }))
    
    // 如果是单选题或判断题，设置singleAnswer
    if (data.form.questionTypeId === 1 || data.form.questionTypeId === 3) {
      const correctChoice = data.form.choiceList.find(c => c.flag)
      if (correctChoice) {
        data.singleAnswer = correctChoice.identification
      }
    }
  }
  
  // 设置答案（文本题类型：4,5,6,7,8）
  if (aiResult.answer && [4, 5, 6, 7, 8].includes(data.form.questionTypeId)) {
    data.form.answer = aiResult.answer
  }
  
  // 设置解析（选择题）
  if (aiResult.answer && [1, 2, 3].includes(data.form.questionTypeId)) {
    data.form.answer = aiResult.answer
  }
  
  // 设置复合题的子题目（9,10,11,12）
  if (aiResult.questionList && aiResult.questionList.length > 0 && [9, 10, 11, 12].includes(data.form.questionTypeId)) {
    data.form.questionList = aiResult.questionList.map(subQ => {
      const subQuestion = {
        tempId: Date.now() + Math.random(),
        questionTypeId: subQ.questionTypeId || 1,
        questionStem: subQ.questionStem || '',
        answer: subQ.answer || '',
        singleAnswer: '',
        choiceList: []
      }
      
      // 设置子题选项
      if (subQ.choiceList && subQ.choiceList.length > 0) {
        subQuestion.choiceList = subQ.choiceList.map(choice => ({
          identification: choice.identification || '',
          content: choice.content || '',
          flag: choice.flag || false
        }))
        
        // 如果是单选题或判断题，设置singleAnswer
        if (subQuestion.questionTypeId === 1 || subQuestion.questionTypeId === 3) {
          const correctChoice = subQuestion.choiceList.find(c => c.flag)
          if (correctChoice) {
            subQuestion.singleAnswer = correctChoice.identification
          }
        }
      }
      
      return subQuestion
    })
  }
}

// 获取题型标签颜色
const getQuestionTypeTag = () => {
  const typeMap = {
    1: 'success',   // 单选
    2: 'primary',   // 多选
    3: 'warning',   // 判断
    4: 'info',      // 填空
    5: 'danger',    // 名词解析
    6: '',          // 论述
    7: 'success',   // 计算
    8: 'primary',   // 程序
    9: 'warning',   // 资料
    10: 'info',     // 完形填空
    11: 'danger',   // 阅读理解
    12: ''          // 综合
  }
  return typeMap[data.form.questionTypeId] || ''
}

// 获取题型名称
const getQuestionTypeName = () => {
  const typeMap = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题',
    5: '名词解析',
    6: '论述题',
    7: '计算题',
    8: '程序题',
    9: '资料题',
    10: '完形填空',
    11: '阅读理解',
    12: '综合题'
  }
  return typeMap[data.form.questionTypeId] || ''
}

// 获取子题目题型名称
const getSubQuestionTypeName = (typeId) => {
  const typeMap = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题',
    5: '名词解析',
    6: '论述题',
    7: '计算题',
    8: '程序题'
  }
  return typeMap[typeId] || ''
}

// 格式化题干
const formatQuestionStem = (stem) => {
  if (!stem) return '<span style="color: #909399;">请输入题目内容</span>'
  return stem.replace(/\n/g, '<br/>')
}

// 题型改变
const handleTypeChange = () => {
  data.singleAnswer = ''
  // 选择题类型(1,2,3)
  if (data.form.questionTypeId === 1 || data.form.questionTypeId === 2) {
    data.form.choiceList = [
      { identification: 'A', content: '', flag: false },
      { identification: 'B', content: '', flag: false },
      { identification: 'C', content: '', flag: false },
      { identification: 'D', content: '', flag: false }
    ]
    data.form.questionList = []
  } else if (data.form.questionTypeId === 3) {
    data.form.choiceList = [
      { identification: '√', content: '正确', flag: false },
      { identification: '×', content: '错误', flag: false }
    ]
    data.form.questionList = []
  } 
  // 复合题类型(9,10,11,12)
  else if ([9, 10, 11, 12].includes(data.form.questionTypeId)) {
    data.form.choiceList = []
    data.form.questionList = []
  } 
  // 文本题类型(4,5,6,7,8)
  else {
    data.form.choiceList = []
    data.form.questionList = []
  }
}

// 更新选项标记
const updateChoiceFlag = () => {
  if (data.form.choiceList) {
    data.form.choiceList.forEach(choice => {
      choice.flag = choice.identification === data.singleAnswer
    })
  }
}

// 更新子题目选项标记
const updateSubChoiceFlag = (subIdx) => {
  const subQ = data.form.questionList[subIdx]
  if (subQ && subQ.choiceList) {
    subQ.choiceList.forEach(choice => {
      choice.flag = choice.identification === subQ.singleAnswer
    })
  }
}

// 添加选项
const addChoice = () => {
  const nextChar = String.fromCharCode(65 + data.form.choiceList.length)
  data.form.choiceList.push({
    identification: nextChar,
    content: '',
    flag: false
  })
}

// 删除选项
const delChoice = (idx) => {
  data.form.choiceList.splice(idx, 1)
}

// 添加子题目
const addSubQuestion = () => {
  data.form.questionList.push({
    tempId: Date.now() + Math.random(),
    questionTypeId: 1,
    questionStem: '',
    answer: '',
    singleAnswer: '',
    choiceList: [
      { identification: 'A', content: '', flag: false },
      { identification: 'B', content: '', flag: false },
      { identification: 'C', content: '', flag: false },
      { identification: 'D', content: '', flag: false }
    ]
  })
}

// 删除子题目
const delSubQuestion = (idx) => {
  data.form.questionList.splice(idx, 1)
}

// 子题目题型改变
const handleSubTypeChange = (idx) => {
  const subQ = data.form.questionList[idx]
  subQ.singleAnswer = ''
  if (subQ.questionTypeId === 1 || subQ.questionTypeId === 2) {
    subQ.choiceList = [
      { identification: 'A', content: '', flag: false },
      { identification: 'B', content: '', flag: false },
      { identification: 'C', content: '', flag: false },
      { identification: 'D', content: '', flag: false }
    ]
  } else if (subQ.questionTypeId === 3) {
    subQ.choiceList = [
      { identification: '√', content: '正确', flag: false },
      { identification: '×', content: '错误', flag: false }
    ]
  } else {
    subQ.choiceList = []
  }
}

// 添加子题目选项
const addSubChoice = (subIdx) => {
  const subQ = data.form.questionList[subIdx]
  const nextChar = String.fromCharCode(65 + subQ.choiceList.length)
  subQ.choiceList.push({
    identification: nextChar,
    content: '',
    flag: false
  })
}

// 删除子题目选项
const delSubChoice = (subIdx, cIdx) => {
  data.form.questionList[subIdx].choiceList.splice(cIdx, 1)
}

// 加载课程数据
const loadCourse = () => {
  // 教师只能选择自己教授的课程
  request.get('/course/selectByTeacher', {
    params: {
      teacherId: data.user.id
    }
  }).then(res => {
    if (res.code === '200') {
      data.courseData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 加载题目详情（编辑模式）
const loadQuestion = () => {
  const id = route.query.id
  if (id) {
    request.get('/question/selectById/' + id).then(res => {
      if (res.code === '200') {
        data.form = JSON.parse(JSON.stringify(res.data))
        if (!data.form.choiceList) data.form.choiceList = []
        if (!data.form.questionList) data.form.questionList = []
        
        // 初始化singleAnswer
        if (data.form.questionTypeId === 1 || data.form.questionTypeId === 3) {
          const correctChoice = data.form.choiceList.find(c => c.flag === true || c.flag === 1)
          if (correctChoice) {
            data.singleAnswer = correctChoice.identification
          }
        }
        
        // 复合题初始化
        if (data.form.questionTypeId === 6 && data.form.questionList) {
          data.form.questionList.forEach(subQ => {
            if (!subQ.tempId && !subQ.id) {
              subQ.tempId = Date.now() + Math.random()
            }
            if (!subQ.choiceList) subQ.choiceList = []
            if (subQ.questionTypeId === 1 || subQ.questionTypeId === 3) {
              const correctChoice = subQ.choiceList.find(c => c.flag === true || c.flag === 1)
              subQ.singleAnswer = correctChoice ? correctChoice.identification : ''
            }
          })
        }
      } else {
        ElMessage.error(res.msg || '获取题目详情失败')
      }
    })
  }
}

// 保存
const save = () => {
  if (!data.form.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  if (!data.form.questionTypeId) {
    ElMessage.warning('请选择题型')
    return
  }
  if (!data.form.questionStem) {
    ElMessage.warning('请输入题目名称')
    return
  }
  
  data.saving = true
  data.form.teacherId = data.user.id
  
  const url = data.form.id ? '/question/update' : '/question/add'
  const method = data.form.id ? 'put' : 'post'
  
  request[method](url, data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      router.back()
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.saving = false
  })
}

onMounted(() => {
  loadCourse()
  loadQuestion()
})
</script>

<style scoped>
.question-edit {
  height: 100%;
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
}

.paper-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.form-panel {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-panel {
  width: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.preview-header {
  padding: 24px;
  border-bottom: 2px solid #e4e7ed;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
}

.preview-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
}

.preview-content {
  flex: 1;
  padding: 20px;
}

.question-preview {
  background: #fafbfc;
  border-radius: 8px;
  padding: 20px;
}

.preview-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.difficulty {
  color: #606266;
  font-size: 14px;
}

.question-stem {
  margin-bottom: 20px;
}

.stem-label {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stem-content {
  color: #606266;
  line-height: 1.8;
  font-size: 15px;
}

.choices-preview {
  margin-top: 15px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.choice-label {
  font-weight: 600;
  min-width: 30px;
  color: #409eff;
}

.choice-content {
  flex: 1;
  color: #606266;
}

.correct-tag {
  margin-left: auto;
}

.composite-preview {
  margin-top: 20px;
}

.sub-question {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
}

.sub-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.sub-number {
  font-weight: 600;
  color: #409eff;
}

.sub-stem {
  color: #303133;
  margin-bottom: 10px;
  line-height: 1.6;
}

.sub-choices {
  margin-top: 10px;
}

.sub-answer {
  margin-top: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}

.answer-label {
  font-weight: 600;
  color: #606266;
}

.answer-preview, .analysis-preview {
  margin-top: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
}

.answer-content, .analysis-content {
  margin-top: 8px;
  color: #606266;
  line-height: 1.8;
}

/* 拖拽样式 */
.drag-handle {
  cursor: move;
  transition: color 0.3s;
}

.drag-handle:hover {
  color: #409eff;
}

:global(.sortable-ghost) {
  opacity: 0.5;
  transform: rotate(2deg);
}
</style>

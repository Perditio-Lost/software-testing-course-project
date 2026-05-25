<template>
  <div style="padding: 20px">
    <div class="card" style="margin-bottom: 10px">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
        <div style="font-size: 20px; font-weight: bold">题目详情</div>
        <el-button type="primary" plain @click="goBack">返回列表</el-button>
      </div>
      <div style="color: #666; margin-bottom: 10px">
        <span style="margin-right: 30px">所属课程：{{ data.question.courseName }}</span>
        <span style="margin-right: 30px">授课教师：{{ data.question.teacherName }}</span>
        <span style="margin-right: 30px">难度系数：{{ data.question.level }}</span>
      </div>
    </div>

    <div class="card">
      <!-- 题型标签 -->
      <div style="margin-bottom: 20px">
        <el-tag :type="getQuestionTypeStyle(data.question.questionTypeName).type" :effect="getQuestionTypeStyle(data.question.questionTypeName).effect" size="large">{{ data.question.questionTypeName }}</el-tag>
      </div>

      <!-- 题目内容 -->
      <div style="font-size: 16px; font-weight: bold; margin-bottom: 15px">
        {{ data.question.questionStem }}
      </div>

      <!-- 选择题显示选项 -->
      <div v-if="data.question.questionTypeVariety === 'choice' && data.question.choiceList && data.question.choiceList.length > 0" 
           style="margin: 20px 0">
        <div style="font-weight: bold; margin-bottom: 10px; color: #333">选项：</div>
        <div v-for="choice in data.question.choiceList" :key="choice.id" 
             style="margin: 10px 0; padding: 10px; background-color: #f9f9f9; border-radius: 4px"
             :class="{ 'correct-option': isCorrectChoice(choice.identification) }">
          <span style="font-weight: bold; margin-right: 10px">{{ choice.identification }}.</span>
          <span>{{ choice.content }}</span>
        </div>
      </div>

      <!-- 正确答案显示（选择题从选项中获取） -->
      <div v-if="data.question.questionTypeVariety === 'choice'" 
           style="margin-top: 20px; padding: 15px; background-color: #e8f4fd; border-radius: 4px; border-left: 4px solid #67c23a">
        <div style="font-weight: bold; color: #67c23a; font-size: 16px; margin-bottom: 5px">
          正确答案：<span style="color: #333">{{ getCorrectAnswers() }}</span>
        </div>
      </div>
      
      <!-- 题目解析区域 -->
      <div v-if="data.question.answer && ['单选', '多选', '判断'].includes(data.question.questionTypeName)" style="margin-top: 15px; padding: 15px; background-color: #f0f9ff; border-radius: 4px; border-left: 4px solid #409EFF">
        <div style="font-weight: bold; color: #409EFF; font-size: 16px; margin-bottom: 5px">
          题目解析：<span style="color: #333; font-weight: normal;">{{ data.question.answer }}</span>
        </div>
      </div>
      
      <!-- 填空题、简答题等其他题型的解析 -->
      <div v-if="data.question.answer && data.question.questionTypeVariety === 'text'" 
           style="margin-top: 15px; padding: 15px; background-color: #f0f9ff; border-radius: 4px; border-left: 4px solid #409EFF">
        <div style="font-weight: bold; color: #409EFF; font-size: 16px; margin-bottom: 5px">
          题目解析：<span style="color: #333; font-weight: normal;">{{ data.question.answer }}</span>
        </div>
      </div>

      <!-- 复合题显示子题目 -->
      <div v-if="data.question.questionTypeVariety === 'composite' && data.question.questionList && data.question.questionList.length > 0" 
           style="margin-top: 30px">
        <div style="font-size: 18px; font-weight: bold; margin-bottom: 20px; color: #333; border-bottom: 2px solid #409EFF; padding-bottom: 10px">
          子题目列表
        </div>
        
        <div v-for="(subQuestion, index) in data.question.questionList" :key="subQuestion.id" 
             style="margin-bottom: 25px; padding: 20px; background-color: #fafafa; border-radius: 8px; border: 1px solid #e0e0e0">
          
          <!-- 子题目标题 -->
          <div style="display: flex; align-items: baseline; margin-bottom: 15px">
            <el-tag :type="getQuestionTypeStyle(subQuestion.questionTypeName).type" :effect="getQuestionTypeStyle(subQuestion.questionTypeName).effect" size="small" style="margin-right: 10px">{{ subQuestion.questionTypeName }}</el-tag>
            <span style="font-weight: bold; margin-right: 10px; color: #666">第 {{ index + 1 }} 题</span>
            <span style="font-size: 15px; color: #333">{{ subQuestion.questionStem }}</span>
          </div>

          <!-- 子题目选项（选择题） -->
          <div v-if="subQuestion.questionTypeVariety === 'choice' && subQuestion.choiceList && subQuestion.choiceList.length > 0" 
               style="margin: 15px 0 15px 20px">
            <div v-for="choice in subQuestion.choiceList" :key="choice.id" 
                 style="margin: 8px 0; padding: 8px; background-color: #fff; border-radius: 4px"
                 :class="{ 'correct-option': isSubQuestionCorrectChoice(subQuestion.answer, choice.identification, subQuestion) }">
              <span style="font-weight: bold; margin-right: 10px">{{ choice.identification }}.</span>
              <span>{{ choice.content }}</span>
            </div>
          </div>

          <!-- 子题目正确答案（选择题） -->
          <div v-if="subQuestion.questionTypeVariety === 'choice'" 
               style="margin-top: 15px; padding: 12px; background-color: #e8f4fd; border-radius: 4px; border-left: 3px solid #67c23a">
            <span style="font-weight: bold; color: #67c23a">正确答案：</span>
            <span style="color: #333">{{ getSubQuestionCorrectAnswers(subQuestion) }}</span>
          </div>
          <!-- 子题目解析（所有题型统一显示） -->
          <div v-if="subQuestion.answer" 
               style="margin-top: 10px; padding: 12px; background-color: #f0f9ff; border-radius: 4px; border-left: 3px solid #409EFF">
            <span style="font-weight: bold; color: #409EFF">题目解析：</span>
            <span style="color: #333">{{ subQuestion.answer }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue";
import request from "@/utils/request.js";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getQuestionTypeStyle } from '@/utils/questionTypeStyles'

const route = useRoute();
const router = useRouter();

const data = reactive({
  question: {
    choiceList: [],
    questionList: []
  }
})

// 判断选项是否为正确答案（使用flag字段）
const isCorrectChoice = (identification) => {
  if (!data.question.choiceList) return false;
  const choice = data.question.choiceList.find(c => c.identification === identification);
  return choice && (choice.flag === true || choice.flag === 1);
}

// 获取正确答案列表（从flag=true的选项中提取）
const getCorrectAnswers = () => {
  if (!data.question.choiceList) return '';
  const correctChoices = data.question.choiceList.filter(c => c.flag === true || c.flag === 1);
  return correctChoices.map(c => c.identification).join(', ') || '未设置';
}

// 判断子题目选项是否为正确答案（使用flag字段）
const isSubQuestionCorrectChoice = (answer, identification, subQuestion) => {
  if (!subQuestion || !subQuestion.choiceList) return false;
  const choice = subQuestion.choiceList.find(c => c.identification === identification);
  return choice && (choice.flag === true || choice.flag === 1);
}

// 获取子题目正确答案列表
const getSubQuestionCorrectAnswers = (subQuestion) => {
  if (!subQuestion || !subQuestion.choiceList) return '未设置';
  const correctChoices = subQuestion.choiceList.filter(c => c.flag === true || c.flag === 1);
  return correctChoices.map(c => c.identification).join(', ') || '未设置';
}

const load = () => {
  let id = route.query.id
  if (id) {
    request.get('/question/selectById/' + id).then(res => {
      if (res.code === '200') {
        data.question = res.data
        if (!data.question.choiceList) {
          data.question.choiceList = []
        }
        if (!data.question.questionList) {
          data.question.questionList = []
        }
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
}

const goBack = () => {
  const fromPage = route.query.fromPage
  if (fromPage) {
    router.push({
      path: '/manager/question',
      query: { page: fromPage }
    })
  } else {
    router.back()
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.correct-option {
  background-color: #f0f9ff !important;
  border: 1px solid #67c23a;
  color: #67c23a;
  font-weight: bold;
}

.complex-tag {
  background-color: #9c27b0;
  color: #fff;
  border-color: #9c27b0;
}
</style>

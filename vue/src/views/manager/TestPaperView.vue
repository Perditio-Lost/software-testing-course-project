<template>
  <div class="test-paper-view">
    <div class="card" style="margin-bottom: 10px">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
        <div style="font-size: 20px; font-weight: bold">{{ data.testPaper.name }}</div>
        <el-button type="primary" plain @click="router.back()">返回列表</el-button>
      </div>
      <div style="color: #666; margin-bottom: 10px">
        <span style="margin-right: 30px">所属课程：{{ data.testPaper.courseName }}</span>
        <span style="margin-right: 30px">授课教师：{{ data.testPaper.teacherName }}</span>
        <span style="margin-right: 30px">试卷类型：{{ data.testPaper.type }}</span>
      </div>
    </div>

    <div class="card">
      <div v-for="(item, index) in data.testPaper.questions" :key="item.id" style="margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid #f0f0f0">
        <template v-if="item.questionTypeVariety !== 'composite'">
          <div style="display: flex; align-items: baseline; margin-bottom: 10px">
            <el-tag :type="getQuestionTypeStyle(item.questionTypeName).type" :effect="getQuestionTypeStyle(item.questionTypeName).effect" style="margin-right: 10px">{{ item.questionTypeName }}</el-tag>
            <span style="font-size: 16px; font-weight: bold; margin-right: 10px">第 {{ index + 1 }} 题</span>
            <div style="font-size: 16px; flex: 1;">
              <RichTextContent :content="item.questionStem" />
            </div>
          </div>

          <!-- Choice Options -->
          <div v-if="item.questionTypeVariety === 'choice' && item.choiceList" style="margin-left: 20px">
            <div v-for="choice in item.choiceList" :key="choice.id" 
                 style="margin: 8px 0; display: flex; align-items: flex-start;" 
                 :class="{ 'correct-option': choice.flag === true || choice.flag === 1 }">
              <span style="font-weight: bold; margin-right: 10px">{{ choice.identification }}.</span>
              <RichTextContent :content="choice.content" style="flex: 1;" />
            </div>
          </div>

          <div v-if="item.questionTypeVariety === 'choice'" style="margin-top: 15px; padding: 10px; background-color: #e8f4fd; border-radius: 4px; border-left: 4px solid #67c23a">
            <div style="font-weight: bold; color: #67c23a; margin-bottom: 5px">正确答案：{{ getCorrectAnswers(item) }}</div>
          </div>
          <div v-if="item.answer" style="margin-top: 10px; padding: 10px; background-color: #f8f9fa; border-radius: 4px; border-left: 4px solid #409EFF">
            <div style="font-weight: bold; color: #409EFF; margin-bottom: 5px">
              解析：{{ item.answer }}
            </div>
          </div>

          <div style="margin-top: 15px; display: flex; gap: 20px; font-size: 14px; color: #666; align-items: center">
            <span style="cursor: pointer" @click="showStudents(item, 'total')">考试人数：<span style="color: #333; font-weight: bold">{{ item.totalCount || 0 }}</span></span>
            <span style="cursor: pointer" @click="showStudents(item, 'right')">正确人数：<span style="color: #67c23a; font-weight: bold">{{ item.rightCount || 0 }}</span></span>
            <span style="cursor: pointer" @click="showStudents(item, 'error')">错误人数：<span style="color: #f56c6c; font-weight: bold">{{ item.errorCount || 0 }}</span></span>
            <span v-if="item.questionTypeName === '多选' || item.questionTypeVariety === 'text'" style="cursor: pointer" @click="showStudents(item, 'part')">半对人数：<span style="color: #e6a23c; font-weight: bold">{{ item.partRightCount || 0 }}</span></span>
            <el-button type="danger" plain size="small" @click="showStudents(item, 'total')" style="margin-left: auto">查看详情</el-button>
          </div>
        </template>

        <template v-else>
          <div style="display: flex; align-items: baseline; margin-bottom: 10px">
            <el-tag :type="getQuestionTypeStyle(item.questionTypeName).type" :effect="getQuestionTypeStyle(item.questionTypeName).effect" style="margin-right: 10px">{{ item.questionTypeName }}</el-tag>
            <span style="font-size: 16px; font-weight: bold; margin-right: 10px">第 {{ index + 1 }} 题</span>
            <div style="font-size: 16px; flex: 1;">
              <RichTextContent :content="item.questionStem" />
            </div>
            <span style="margin-left: 10px; color: #666; font-size: 14px" v-if="item.questionList && item.questionList.length">
              (包含 {{ item.questionList.length }} 道子题目)
            </span>
            <span style="margin-left: 10px; color: #f56c6c; font-size: 14px" v-else>
              (暂无子题目)
            </span>
          </div>
          
          <div v-if="item.questionList && item.questionList.length" style="background: #fcfcfc; padding: 20px; border-radius: 8px; border: 1px solid #ebeef5">
            <div v-for="(subItem, subIndex) in item.questionList" :key="subItem.id" style="margin-bottom: 25px; border-bottom: 1px dashed #eee; padding-bottom: 15px">
              <div style="display: flex; align-items: baseline; margin-bottom: 10px">
                <el-tag :type="getQuestionTypeStyle(subItem.questionTypeName).type" :effect="getQuestionTypeStyle(subItem.questionTypeName).effect" size="small" style="margin-right: 10px">{{ subItem.questionTypeName }}</el-tag>
                <span style="font-size: 15px; font-weight: bold; margin-right: 10px">({{ subIndex + 1 }})</span>
                <div style="font-size: 15px; flex: 1;">
                  <RichTextContent :content="subItem.questionStem" />
                </div>
              </div>

              <div v-if="subItem.questionTypeVariety === 'choice' && subItem.choiceList" style="margin-left: 20px">
                <div v-for="choice in subItem.choiceList" :key="choice.id" 
                     style="margin: 8px 0; display: flex; align-items: flex-start;" 
                     :class="{ 'correct-option': choice.flag === true || choice.flag === 1 }">
                  <span style="font-weight: bold; margin-right: 10px">{{ choice.identification }}.</span>
                  <RichTextContent :content="choice.content" style="flex: 1;" />
                </div>
              </div>

              <div v-if="subItem.questionTypeVariety === 'choice'" style="margin-top: 10px; padding: 10px; background-color: #e8f4fd; border-radius: 4px; border-left: 4px solid #67c23a">
                <div style="font-weight: bold; color: #67c23a; margin-bottom: 5px">正确答案：{{ getCorrectAnswers(subItem) }}</div>
              </div>
              <div v-if="subItem.answer" style="margin-top: 10px; padding: 10px; background-color: #f8f9fa; border-radius: 4px; border-left: 4px solid #409EFF">
                <div style="font-weight: bold; color: #409EFF; margin-bottom: 5px">
                  解析：{{ subItem.answer }}
                </div>
              </div>

              <div style="margin-top: 15px; display: flex; gap: 20px; font-size: 14px; color: #666; align-items: center">
                <span style="cursor: pointer" @click="showStudents(subItem, 'total')">考试人数：<span style="color: #333; font-weight: bold">{{ subItem.totalCount || 0 }}</span></span>
                <span style="cursor: pointer" @click="showStudents(subItem, 'right')">正确人数：<span style="color: #67c23a; font-weight: bold">{{ subItem.rightCount || 0 }}</span></span>
                <span style="cursor: pointer" @click="showStudents(subItem, 'error')">错误人数：<span style="color: #f56c6c; font-weight: bold">{{ subItem.errorCount || 0 }}</span></span>
                <span v-if="subItem.questionTypeName === '多选' || subItem.questionTypeVariety === 'text'" style="cursor: pointer" @click="showStudents(subItem, 'part')">半对人数：<span style="color: #e6a23c; font-weight: bold">{{ subItem.partRightCount || 0 }}</span></span>
                <el-button type="danger" plain size="small" @click="showStudents(subItem, 'total')" style="margin-left: auto">查看详情</el-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <el-dialog v-model="data.dialogVisible" title="学生答题详情" width="70%">
      <div v-if="data.currentQuestion" style="margin-bottom: 20px; padding: 15px; background-color: #fcfcfc; border: 1px solid #eee; border-radius: 4px">
        <div style="display: flex; align-items: baseline; margin-bottom: 10px">
          <el-tag :type="getQuestionTypeStyle(data.currentQuestion.questionTypeName).type" :effect="getQuestionTypeStyle(data.currentQuestion.questionTypeName).effect" style="margin-right: 10px">{{ data.currentQuestion.questionTypeName }}</el-tag>
          <span style="font-size: 16px">{{ data.currentQuestion.questionStem }}</span>
        </div>
        
        <div v-if="data.currentQuestion.questionTypeVariety === 'choice' && data.currentQuestion.choiceList" style="margin-left: 20px">
          <div v-for="choice in data.currentQuestion.choiceList" :key="choice.id" 
               style="margin: 8px 0" 
               :class="{ 'correct-option': choice.flag === true || choice.flag === 1 }">
            <span style="font-weight: bold; margin-right: 10px">{{ choice.identification }}.</span>{{ choice.content }}
          </div>
        </div>

        <div v-if="data.currentQuestion.questionTypeVariety === 'choice'" style="margin-top: 15px; padding: 10px; background-color: #e8f4fd; border-radius: 4px; border-left: 4px solid #67c23a">
          <div style="font-weight: bold; color: #67c23a; margin-bottom: 5px">正确答案：{{ getCorrectAnswers(data.currentQuestion) }}</div>
        </div>
        <div v-if="data.currentQuestion.answer" style="margin-top: 10px; padding: 10px; background-color: #f8f9fa; border-radius: 4px; border-left: 4px solid #409EFF">
          <div style="font-weight: bold; color: #409EFF; margin-bottom: 5px">
            解析：{{ data.currentQuestion.answer }}
          </div>
        </div>
      </div>

      <div style="margin-bottom: 10px">
        <el-radio-group v-model="data.filterStatus" @change="handleFilterChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="正确">正确</el-radio-button>
          <el-radio-button label="错误">错误</el-radio-button>
          <el-radio-button label="半对">半对</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="data.filteredStudentList" stripe border height="500">
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="studentNumber" label="学号" width="120" />
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.status === '正确'" type="success">正确</el-tag>
            <el-tag v-else-if="scope.row.status === '错误'" type="danger">错误</el-tag>
            <el-tag v-else-if="scope.row.status === '半对'" type="warning">半对</el-tag>
            <el-tag v-else type="info">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="answerContent" label="作答内容" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue";
import request from "@/utils/request.js";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getQuestionTypeStyle } from "@/utils/questionTypeStyles";
import RichTextContent from '@/components/RichTextContent.vue';

const route = useRoute();
const router = useRouter();

const data = reactive({
  testPaper: { questions: [] },
  dialogVisible: false,
  currentStudentList: [],
  filteredStudentList: [],
  filterStatus: 'all',
  currentQuestion: null
})

// 获取正确答案列表（从flag=true的选项中提取）
const getCorrectAnswers = (item) => {
  if (!item || !item.choiceList) return '未设置';
  const correctChoices = item.choiceList.filter(c => c.flag === true || c.flag === 1);
  return correctChoices.map(c => c.identification).join(', ') || '未设置';
}

const showStudents = (question, type) => {
  data.currentQuestion = question
  if (!question.studentList) {
    ElMessage.warning('暂无学生数据')
    return
  }

  data.currentStudentList = question.studentList
  if (type === 'total') {
    data.filterStatus = 'all'
  } else if (type === 'right') {
    data.filterStatus = '正确'
  } else if (type === 'error') {
    data.filterStatus = '错误'
  } else if (type === 'part') {
    data.filterStatus = '半对'
  }
  handleFilterChange()
  data.dialogVisible = true
}

const handleFilterChange = () => {
  if (data.filterStatus === 'all') {
    data.filteredStudentList = data.currentStudentList
  } else {
    data.filteredStudentList = data.currentStudentList.filter(s => s.status === data.filterStatus)
  }
}

const load = () => {
  let id = route.query.id
  let testId = route.query.testId

  if (testId && id) {
    request.get('/testPaper/analysis', { params: { testPaperId: id, testId: testId } }).then(res => {
      if (res.code === '200') {
        data.testPaper = res.data
        if (!data.testPaper.questions) {
          data.testPaper.questions = []
        }
      } else {
        ElMessage.error(res.msg)
      }
    })
  } else if (id) {
    request.get('/testPaper/selectById/' + id).then(res => {
      if (res.code === '200') {
        data.testPaper = res.data
        if (!data.testPaper.questions) {
          data.testPaper.questions = []
        }
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.test-paper-view {
  height: calc(100vh - 65px);
  padding: 20px;
  overflow-y: auto;
  box-sizing: border-box;
}

.correct-option {
  color: #67c23a;
  font-weight: bold;
}
</style>

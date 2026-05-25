<template>
  <div>
    <div class="card" style="margin-bottom: 5px; display: flex; align-items: center;">
      <el-select v-model="data.courseId" placeholder="请选择课程" style="width: 200px; margin-right: 10px" clearable @change="handleCourseChange">
        <el-option v-for="course in data.courseList" :key="course.id" :label="course.name" :value="course.id" />
      </el-select>
      <el-select v-model="data.clazzId" placeholder="请选择班级" style="width: 200px; margin-right: 10px" clearable @change="handleClazzChange">
        <el-option v-for="clazz in data.clazzList" :key="clazz.id" :label="clazz.name" :value="clazz.id" />
      </el-select>
      <el-select v-model="data.testId" placeholder="请选择考试" style="width: 200px; margin-right: 10px" clearable @change="handleTestChange">
        <el-option v-for="test in data.testList" :key="test.id" :label="test.name" :value="test.id" />
      </el-select>
      <el-radio-group v-if="data.user.role === 'TEACHER'" v-model="data.gradingMode" style="margin-right: 10px" @change="handleGradingModeChange">
        <el-radio value="student">按学生批阅</el-radio>
        <el-radio value="question">按题目批阅</el-radio>
      </el-radio-group>
      <div style="flex: 1;"></div>
      <el-button type="success" plain @click="handleExport">导出成绩</el-button>
    </div>

    <!-- 按学生批阅 -->
    <div v-if="data.gradingMode === 'student' || data.user.role === 'ADMIN'" class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" v-if="data.testId">
        <el-table-column label="试卷名称" show-overflow-tooltip>
          <template v-slot="scope">
            {{ scope.row.test?.testPaper?.name }}
          </template>
        </el-table-column>
        <el-table-column label="课程名称" show-overflow-tooltip>
          <template v-slot="scope">
            {{ scope.row.test?.testPaper?.courseName }}
          </template>
        </el-table-column>
        <el-table-column label="授课教师" show-overflow-tooltip v-if="data.user.role === 'ADMIN'">
          <template v-slot="scope">
            {{ scope.row.test?.testPaper?.teacherName }}
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="学生姓名" show-overflow-tooltip/>
        <el-table-column label="试卷状态" show-overflow-tooltip>
          <template v-slot="scope">
            <span v-if="!scope.row.status" style="color: red;">无状态 ({{ typeof scope.row.status }})</span>
            <el-tag v-else-if="scope.row.status === '已批改'" type="success">{{ scope.row.status }}</el-tag>
            <el-tag v-else-if="scope.row.status === '待批改'" type="danger">{{ scope.row.status }}</el-tag>
            <el-tag v-else type="info">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" show-overflow-tooltip/>
        <el-table-column label="操作" width="140" fixed="right" v-if="data.user.role === 'TEACHER'">
          <template v-slot="scope">
            <el-tooltip 
              v-if="scope.row.cheat === 1"
              content="该试卷存在作弊行为"
              placement="top"
            >
              <el-button type="primary" disabled>
                {{ scope.row.status === '已批改' ? '查看/编辑' : '阅卷' }}
              </el-button>
            </el-tooltip>
            <el-button 
              v-else
              type="primary" 
              plain
              @click="handleEdit(scope.row)"
            >
              {{ scope.row.status === '已批改' ? '查看/编辑' : '阅卷' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="color: #909399; padding: 20px; text-align: center;" v-else>
        请先选择课程、班级和考试
      </div>
    </div>

    <!-- 按题目批阅 -->
    <div v-if="data.gradingMode === 'question' && data.user.role === 'TEACHER'" class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.questionList" v-if="data.testId">
        <el-table-column label="题目类型" width="120">
          <template v-slot="scope">
            <el-tag :type="getQuestionTypeColor(scope.row.questionTypeName)">
              {{ scope.row.questionTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题目题干" show-overflow-tooltip>
          <template v-slot="scope">
            <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
              {{ scope.row.questionStem }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="批阅进度" width="150">
          <template v-slot="scope">
            <span :style="{color: scope.row.gradedCount === scope.row.totalCount ? '#67C23A' : '#E6A23C'}">
              {{ scope.row.gradedCount }}/{{ scope.row.totalCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template v-slot="scope">
            <el-button 
              type="primary" 
              plain
              @click="handleQuestionGrading(scope.row)"
              :disabled="scope.row.totalCount === 0"
            >
              批阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="color: #909399; padding: 20px; text-align: center;" v-else>
        请先选择课程、班级和考试
      </div>
    </div>

    <div class="card" v-if="data.total && data.gradingMode === 'student'">
      <el-pagination @current-change="loadStudentScores" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>
  </div>
</template>

<script setup>
import {reactive, onMounted} from "vue";
import { useRouter } from 'vue-router'
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const router = useRouter()

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  gradingMode: 'student', // 默认按学生批阅
  courseList: [],
  clazzList: [],
  testList: [],
  courseId: null,
  clazzId: null,
  testId: null,
  tableData: [],
  questionList: [],
  pageNum: 1,
  pageSize: 10,
  total: 0
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

// 加载课程列表
const loadCourses = () => {
  if (data.user.role === 'TEACHER') {
    // 教师：从所教的班级中获取课程列表
    request.get('/course/selectByTeacher', {
      params: { teacherId: data.user.id }
    }).then(res => {
      if (res.code === '200') {
        data.courseList = res.data || []
      }
    })
  } else {
    // 管理员：查询所有课程
    request.get('/course/selectAll').then(res => {
      if (res.code === '200') {
        data.courseList = res.data || []
      }
    })
  }
}

// 课程变化
const handleCourseChange = () => {
  data.clazzId = null
  data.testId = null
  data.clazzList = []
  data.testList = []
  data.tableData = []
  data.questionList = []
  data.total = 0
  
  // 保存筛选条件
  saveFilterConditions()
  
  if (data.courseId) {
    loadClazzes()
  }
}

// 加载班级列表
const loadClazzes = () => {
  const params = {
    courseId: data.courseId
  }
  // 不传teacherId，显示该课程的所有班级（包括不是自己教的）
  return request.get('/clazz/selectByCourseAndTeacher', { params }).then(res => {
    if (res.code === '200') {
      data.clazzList = res.data || []
    }
  })
}

// 班级变化
const handleClazzChange = () => {
  data.testId = null
  data.testList = []
  data.tableData = []
  data.questionList = []
  data.total = 0
  
  // 保存筛选条件
  saveFilterConditions()
  
  if (data.clazzId) {
    loadTests()
  }
}

// 加载考试列表（只显示已结束的考试）
const loadTests = () => {
  return request.get('/test/selectByClazz', {
    params: {
      clazzId: data.clazzId,
      onlyFinished: true  // 只获取已结束的考试
    }
  }).then(res => {
    if (res.code === '200') {
      data.testList = res.data || []
    }
  })
}

// 考试变化
const handleTestChange = () => {
  data.pageNum = 1
  
  // 保存筛选条件
  saveFilterConditions()
  
  if (data.testId) {
    if (data.gradingMode === 'student') {
      loadStudentScores()
    } else {
      loadQuestionList()
    }
  } else {
    data.tableData = []
    data.questionList = []
    data.total = 0
  }
}

// 批阅模式变化
const handleGradingModeChange = () => {
  // 保存筛选条件
  saveFilterConditions()
  
  if (data.testId) {
    if (data.gradingMode === 'student') {
      loadStudentScores()
    } else {
      loadQuestionList()
    }
  }
}

// 加载学生成绩列表
const loadStudentScores = () => {
  if (!data.testId) return
  
  request.get('/score/selectByTest', {
    params: {
      testId: data.testId,
      clazzId: data.clazzId,  // 添加班级ID过滤
      pageNum: data.pageNum,
      pageSize: data.pageSize
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 加载题目列表（按题目批阅）
const loadQuestionList = () => {
  if (!data.testId) return
  
  request.get('/score/selectQuestionsByTest', {
    params: {
      testId: data.testId,
      clazzId: data.clazzId  // 添加班级ID过滤
    }
  }).then(res => {
    if (res.code === '200') {
      data.questionList = res.data || []
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const handleEdit = (row) => {
  // 跳转到独立的阅卷页面
  router.push({
    path: '/manager/scoreDetail',
    query: {
      scoreId: row.id,
      studentName: row.studentName,
      paperName: row.test?.testPaper?.name,
      courseName: row.test?.testPaper?.courseName,
      status: row.status // 传递阅卷状态
    }
  })
}

// 按题目批阅
const handleQuestionGrading = (question) => {
  router.push({
    path: '/manager/questionGrading',
    query: {
      testId: data.testId,
      questionId: question.id,
      questionTypeName: question.questionTypeName,
      questionTypeVariety: question.questionTypeVariety
    }
  })
}

// 保存筛选条件到localStorage
const saveFilterConditions = () => {
  const filters = {
    courseId: data.courseId,
    clazzId: data.clazzId,
    testId: data.testId,
    gradingMode: data.gradingMode
  }
  localStorage.setItem('score-filter-conditions', JSON.stringify(filters))
}

// 从localStorage恢复筛选条件
const restoreFilterConditions = () => {
  const saved = localStorage.getItem('score-filter-conditions')
  if (saved) {
    const filters = JSON.parse(saved)
    // 管理员强制使用学生批阅模式
    data.gradingMode = data.user.role === 'ADMIN' ? 'student' : (filters.gradingMode || 'student')
    
    // 如果有保存的课程ID，级联加载数据
    if (filters.courseId) {
      data.courseId = filters.courseId
      loadClazzes().then(() => {
        if (filters.clazzId) {
          data.clazzId = filters.clazzId
          loadTests().then(() => {
            if (filters.testId) {
              data.testId = filters.testId
              // 触发数据加载
              if (data.gradingMode === 'student') {
                loadStudentScores()
              } else {
                loadQuestionList()
              }
            }
          })
        }
      })
    }
  }
}

// 导出成绩Excel
const handleExport = () => {
  // 检查筛选条件是否全部填写
  if (!data.clazzId || !data.testId) {
    ElMessage.warning('请先选择要导出成绩的班级与考试')
    return
  }

  // 使用axios直接调用以获取完整响应（包括headers）
  const baseURL = import.meta.env.VITE_BASE_URL || 'http://localhost:8080'
  const token = JSON.parse(localStorage.getItem('xm-user') || '{}').token || ''
  
  fetch(`${baseURL}/score/exportGradedScores?clazzId=${data.clazzId}&testId=${data.testId}`, {
    method: 'GET',
    headers: {
      'token': token
    }
  }).then(async response => {
    
    const contentType = response.headers.get('content-type')
    
    // 检查是否是JSON错误响应
    if (contentType && contentType.includes('application/json')) {
      const error = await response.json()
      ElMessage.error(error.msg || '导出失败')
      return
    }
    
    // 获取文件名
    const contentDisposition = response.headers.get('content-disposition')
    let filename = '成绩统计.xlsx'
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/)
      if (filenameMatch) {
        filename = decodeURIComponent(filenameMatch[1])
      }
    }
    
    // 下载文件
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  }).catch(error => {
    ElMessage.error('导出失败')
  })
}

onMounted(() => {
  loadCourses()
  // 页面加载时恢复筛选条件
  setTimeout(() => {
    restoreFilterConditions()
  }, 100)
})
</script>

<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" prefix-icon="Search" style="width: 240px; margin-right: 10px" placeholder="请输入试卷名称查询"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button v-if="data.user.role === 'TEACHER'" type="primary" plain style="margin-left: 10px" @click="handleAdd">出卷</el-button>
      <el-button type="danger" plain style="margin-left: 10px" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="试卷名称">
          <template v-slot="scope">
            <span style="color: #409EFF; cursor: pointer" @click="viewTestPaper(scope.row.id)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" />
        <el-table-column prop="type" label="试卷类型">
          <template v-slot="scope">
            <el-tag v-if="scope.row.type === '手动选题'" type="success">{{ scope.row.type }}</el-tag>
            <el-tag v-if="scope.row.type === '自动组卷'" type="primary">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="难度系数" width="120">
          <template v-slot="scope">
            <el-tag v-if="scope.row.level !== null && scope.row.level !== undefined" 
                    :type="getDifficultyType(scope.row.level)" 
                    effect="plain">
              {{ scope.row.level.toFixed(2) }}
            </el-tag>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="editPaper(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog 
      title="试卷信息" 
      v-model="data.formVisible" 
      width="55%" 
      destroy-on-close 
      :close-on-click-modal="false"
      class="test-paper-dialog"
      style="max-height: 80vh; overflow: hidden;"
    >
      <div style="max-height: 50vh; overflow-y: auto; overflow-x: hidden; padding-right: 10px;">
        <el-form ref="form" :model="data.form" label-width="100px" style="padding: 10px 20px;">
          <el-row :gutter="20">
          <el-col :span="24">
             <el-form-item prop="courseId" label="选择课程">
              <el-select v-model="data.form.courseId" placeholder="请选择课程" @change="loadQuestion" style="width: 100%" filterable>
                <el-option
                    v-for="item in data.courseData"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
             <el-form-item prop="name" label="试卷名称">
              <el-input v-model="data.form.name" placeholder="请输入试卷名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="type" label="出题方式">
              <el-select v-model="data.form.type" placeholder="请选择出题方式" style="width: 100%">
                <el-option label="手动选题" value="手动选题"></el-option>
                <el-option label="自动组卷" value="自动组卷"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 手动选题模式不再显示题目选择，将跳转到编辑页面 -->
        
        <div v-if="data.form.type === '自动组卷'">
           <el-divider content-position="left">试卷难度配置</el-divider>
           <el-form-item label="目标难度系数">
             <el-input-number 
               v-model="data.form.targetDifficulty" 
               :min="0" 
               :max="1" 
               :step="0.1" 
               :precision="2"
               style="width: 60%" 
               controls-position="right"
               placeholder="0.00-1.00，不填则随机选题"
             ></el-input-number>
             <el-tooltip 
               content="难度系数范围0-1，越小越难。不填写则随机选题，填写后会优先选择接近该难度的题目" 
               placement="top"
             >
               <el-icon style="margin-left: 8px; cursor: help;">
                 <QuestionFilled />
               </el-icon>
             </el-tooltip>
           </el-form-item>
           <el-divider content-position="left">题型数量配置</el-divider>
           <el-form-item label="试卷模板">
             <el-select v-model="data.form.templateId" placeholder="请选择模板" style="width: 60%" clearable @change="onTemplateChange">
               <el-option
                   v-for="item in data.templateData"
                   :key="item.id"
                   :label="item.name"
                   :value="item.id">
               </el-option>
             </el-select>
             <span v-if="data.form.templateId" style="margin-left: 10px; color: #909399; font-size: 12px;">使用模板后数量和分值不可修改</span>
           </el-form-item>
           <el-row :gutter="20">
             <el-col :span="12">
                <el-form-item prop="choiceNum" label="单选题数量">
                  <el-input-number v-model="data.form.choiceNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
             </el-col>
             <el-col :span="12">
                <el-form-item prop="multiChoiceNum" label="多选题数量">
                  <el-input-number v-model="data.form.multiChoiceNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
             </el-col>
             <el-col :span="12">
                <el-form-item prop="checkNum" label="判断题数量">
                  <el-input-number v-model="data.form.checkNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
             </el-col>
             <el-col :span="12">
                <el-form-item prop="fillInNum" label="填空题数量">
                  <el-input-number v-model="data.form.fillInNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
             </el-col>
             <el-col :span="12">
                <el-form-item prop="textNum" label="简答题数量">
                  <el-input-number v-model="data.form.textNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
             </el-col>
              <el-col :span="12">
                <el-form-item prop="compositeNum" label="综合题数量">
                  <el-input-number v-model="data.form.compositeNum" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
                </el-form-item>
              </el-col>
            </el-row>
           <el-divider content-position="left">题型分值配置（每题）</el-divider>
           <el-row :gutter="20">
             <el-col :span="12">
               <el-form-item label="单选题分值">
                 <el-input-number v-model="data.form.choiceScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
             <el-col :span="12">
               <el-form-item label="多选题分值">
                 <el-input-number v-model="data.form.multiChoiceScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
             <el-col :span="12">
               <el-form-item label="判断题分值">
                 <el-input-number v-model="data.form.checkScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
             <el-col :span="12">
               <el-form-item label="填空题分值">
                 <el-input-number v-model="data.form.fillInScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
             <el-col :span="12">
               <el-form-item label="简答题分值">
                 <el-input-number v-model="data.form.textScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
             <el-col :span="12">
               <el-form-item label="综合题分值">
                 <el-input-number v-model="data.form.compositeScore" :min="0" :disabled="!!data.form.templateId" style="width: 100%" controls-position="right"></el-input-number>
               </el-form-item>
             </el-col>
           </el-row>
           <div style="text-align: right; margin-top: 10px">
             <el-button type="primary" plain style="width: 160px" @click="openTemplateDialog">保存当前为模板</el-button>
           </div>
        </div>
      </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="danger" plain @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" plain @click="add">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>

  <el-dialog title="保存试卷模板" v-model="data.templateDialogVisible" width="30%" destroy-on-close>
    <el-form :model="data.templateForm" label-width="80px" style="padding: 20px">
      <el-form-item label="模板名称">
        <el-input v-model="data.templateForm.name" placeholder="请输入模板名称"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button type="danger" plain @click="data.templateDialogVisible = false">取 消</el-button>
        <el-button type="primary" plain @click="saveTemplate">保 存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit, QuestionFilled} from "@element-plus/icons-vue";
import {useRouter} from "vue-router";

const router = useRouter();

// 难度系数标签类型
const getDifficultyType = (difficulty) => {
  if (difficulty < 0.3) return 'danger' // 高难度
  if (difficulty < 0.5) return 'warning' // 中等偏难
  if (difficulty < 0.7) return '' // 中等
  return 'success' // 简单
}

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  ids: [],
  courseData: [],
  questionData: [],
  templateData: [],
  templateDialogVisible: false,
  templateForm: {},
  manualScoreMap: {}
})

const viewTestPaper = (id) => {
  router.push('/manager/testPaperView?id=' + id)
}

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

const loadQuestion = (courseId) => {
  request.get('/question/selectAll', {
    params: {
      courseId: courseId
    }
  }).then(res => {
    if (res.code === '200') {
      data.questionData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })

  loadTemplate(courseId)
}

const loadTemplate = (courseId) => {
  request.get('/testPaperTemplate/selectAll', {
    params: {
      courseId: courseId
    }
  }).then(res => {
    if (res.code === '200') {
      data.templateData = res.data || []
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const onTemplateChange = (id) => {
  const item = data.templateData.find(v => v.id === id)
  if (!item) {
    return
  }
  data.form.choiceNum = item.choiceNum
  data.form.multiChoiceNum = item.multiChoiceNum
  data.form.checkNum = item.checkNum
  data.form.fillInNum = item.fillInNum
  data.form.textNum = item.textNum
  data.form.compositeNum = item.compositeNum
  data.form.choiceScore = item.choiceScore
  data.form.multiChoiceScore = item.multiChoiceScore
  data.form.checkScore = item.checkScore
  data.form.fillInScore = item.fillInScore
  data.form.textScore = item.textScore
  data.form.compositeScore = item.compositeScore
}

const getManualQuestions = () => {
  const ids = data.form.idList || []
  if (!ids.length) {
    return []
  }
  return data.questionData.filter(q => ids.includes(q.id))
}

const openTemplateDialog = () => {
  if (!data.form.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  data.templateForm = {
    name: '',
    courseId: data.form.courseId
  }
  data.templateDialogVisible = true
}

const saveTemplate = () => {
  if (!data.templateForm.name) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!data.form.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  const payload = {
    name: data.templateForm.name,
    courseId: data.form.courseId,
    choiceNum: data.form.choiceNum || 0,
    multiChoiceNum: data.form.multiChoiceNum || 0,
    checkNum: data.form.checkNum || 0,
    fillInNum: data.form.fillInNum || 0,
    textNum: data.form.textNum || 0,
    compositeNum: data.form.compositeNum || 0,
    choiceScore: data.form.choiceScore || 0,
    multiChoiceScore: data.form.multiChoiceScore || 0,
    checkScore: data.form.checkScore || 0,
    fillInScore: data.form.fillInScore || 0,
    textScore: data.form.textScore || 0,
    compositeScore: data.form.compositeScore || 0
  }
  request.post('/testPaperTemplate/add', payload).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      data.templateDialogVisible = false
      loadTemplate(data.form.courseId)
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const load = () => {
  request.get('/testPaper/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const handleAdd = () => {
  data.form = {}
  data.manualScoreMap = {}
  data.formVisible = true
}

const editPaper = (row) => {
  // 跳转到试卷编辑页面，传递试卷ID
  router.push({
    path: '/manager/testPaperEdit',
    query: {
      paperId: row.id,
      courseId: row.courseId,
      courseName: row.courseName,
      paperName: row.name
    }
  })
}

const add = () => {
  // 验证必填字段
  if (!data.form.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  if (!data.form.name) {
    ElMessage.warning('请输入试卷名称')
    return
  }
  if (!data.form.type) {
    ElMessage.warning('请选择出题方式')
    return
  }

  // 如果是手动选题，跳转到试卷编辑页面
  if (data.form.type === '手动选题') {
    const course = data.courseData.find(c => c.id === data.form.courseId)
    router.push({
      path: '/manager/testPaperEdit',
      query: {
        courseId: data.form.courseId,
        courseName: course ? course.name : '',
        paperName: data.form.name
      }
    })
    data.formVisible = false
    return
  }

  // 自动组卷继续原有逻辑
  request.post('/testPaper/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/testPaper/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据")
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete("/testPaper/delete/batch", {data: data.ids}).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.id)
}

const reset = () => {
  data.name = null
  load()
}

load()
loadCourse()
</script>

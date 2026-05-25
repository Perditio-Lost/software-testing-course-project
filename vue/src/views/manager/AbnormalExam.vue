<template>
  <div>
    <div class="card" style="margin-bottom: 10px;">
      <el-input v-model="data.testName" placeholder="请输入考试名称" style="width: 200px; margin-right: 10px;" clearable @clear="load"/>
      <el-input v-model="data.studentName" placeholder="请输入学生姓名/用户名" style="width: 200px; margin-right: 10px;" clearable @clear="load"/>
      <el-input v-model="data.testPaperName" placeholder="请输入试卷名称" style="width: 200px; margin-right: 10px;" clearable @clear="load"/>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain @click="reset">重置</el-button>
    </div>

    <div class="card">
      <div style="margin-bottom: 10px; font-weight: bold; font-size: 16px;">
        异常试卷列表
      </div>
      <el-table :data="data.tableData" stripe>
        <el-table-column prop="studentNumber" label="用户名" min-width="120"/>
        <el-table-column prop="studentName" label="学生姓名" min-width="100"/>
        <el-table-column label="考试名称" min-width="180">
          <template #default="scope">
            <div>{{ scope.row.test?.name || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="试卷名称" min-width="180">
          <template #default="scope">
            <div>{{ scope.row.test?.testPaper?.name || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="考试开始时间" min-width="160">
          <template #default="scope">
            <div>{{ scope.row.test?.start || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="clazzName" label="班级" min-width="150"/>
        <el-table-column prop="teacherName" label="授课教师" min-width="100"/>
        <el-table-column label="得分" width="100">
          <template #default="scope">
            <el-tag type="danger">{{ scope.row.score || 0 }}分</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button type="primary" plain size="small" @click="viewStudent(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin: 10px 0">
        <el-pagination
          @current-change="handleCurrentChange"
          :current-page="data.pageNum"
          :page-size="data.pageSize"
          background
          layout="total, prev, pager, next"
          :total="data.total">
        </el-pagination>
      </div>
    </div>

    <!-- 学生详情弹窗 -->
    <el-dialog v-model="data.dialogVisible" title="学生详细信息" width="600px">
      <div v-if="data.studentDetail" style="line-height: 2;">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ data.studentDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ data.studentDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ data.studentDetail.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ data.studentDetail.email }}</el-descriptions-item>
        </el-descriptions>
        
        <!-- 人脸识别照片 -->
        <div v-if="data.currentFacePhotoUrl" style="margin-top: 20px;">
          <div style="font-weight: bold; margin-bottom: 10px; font-size: 14px;">考试期间人脸识别照片：</div>
          <el-image 
            :src="data.currentFacePhotoUrl" 
            fit="contain"
            style="width: 100%; max-height: 300px; border: 1px solid #dcdfe6; border-radius: 4px;"
            :preview-src-list="[data.currentFacePhotoUrl]"
          >
            <template #error>
              <div style="display: flex; justify-content: center; align-items: center; height: 200px; background: #f5f7fa; color: #909399;">
                <el-icon style="font-size: 32px;"><Picture /></el-icon>
                <span style="margin-left: 8px;">暂无照片</span>
              </div>
            </template>
          </el-image>
        </div>
        <div v-else style="margin-top: 20px; padding: 20px; background: #f5f7fa; border-radius: 4px; text-align: center; color: #909399;">
          <el-icon style="font-size: 32px;"><Picture /></el-icon>
          <div style="margin-top: 8px;">暂无人脸识别照片</div>
        </div>
      </div>
      <template #footer>
        <el-button plain @click="data.dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import { Picture } from '@element-plus/icons-vue'

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  testName: '',
  studentName: '',
  testPaperName: '',
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  dialogVisible: false,
  studentDetail: null,
  currentFacePhotoUrl: null  // 当前学生的人脸照片URL
})

const load = () => {
  // 如果是教师角色，传递teacherId；如果是管理员，不传teacherId
  const params = {
    testName: data.testName,
    studentName: data.studentName,
    testPaperName: data.testPaperName,
    pageNum: data.pageNum,
    pageSize: data.pageSize
  }
  
  // 只有教师角色才传递teacherId进行筛选
  if (data.user.role === 'TEACHER') {
    params.teacherId = data.user.id
  }
  
  request.get('/score/selectAbnormal', { params }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data.list
      data.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const reset = () => {
  data.testName = ''
  data.studentName = ''
  data.testPaperName = ''
  data.pageNum = 1
  load()
}

const handleCurrentChange = (pageNum) => {
  data.pageNum = pageNum
  load()
}

const viewStudent = (row) => {
  // 保存人脸照片URL
  data.currentFacePhotoUrl = row.facePhotoUrl || null
  
  request.get('/student/selectById/' + row.studentId).then(res => {
    if (res.code === '200') {
      data.studentDetail = res.data
      data.dialogVisible = true
    } else {
      ElMessage.error(res.msg)
    }
  })
}

load()
</script>

<style scoped>
.card {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08);
  border: 1px solid #d9ecff;
}
</style>

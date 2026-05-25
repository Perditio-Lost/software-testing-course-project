<template>
  <div class="main-content">
    <div style="margin: 30px 0; text-align: center">
      <el-select size="large" clearable @clear="reset" style="width: 200px; margin-right: 5px" v-model="data.clazzId" placeholder="请选择班级">
        <el-option v-for="clazz in data.clazzList" :key="clazz.id" :label="clazz.name" :value="clazz.id"></el-option>
      </el-select>
      <el-input size="large" clearable @clear="reset" style="width: 300px; margin-right: 5px" v-model="data.courseName" placeholder="请输入课程名称查询"></el-input>
      <el-button size="large" type="primary" plain @click="load">查询</el-button>
    </div>
    <div>
      <el-row :gutter="10">
        <el-col :span="6" v-for="item in data.examData" :key="item.id" style="margin-bottom: 15px">
          <div class="card" style="cursor: pointer" @click="navTo(item)">
            <img v-if="item.courseImgUrl" :src="item.courseImgUrl" alt="" style="width: 100%; height: 150px">
            <div v-else style="width: 100%; height: 150px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; color: #999;">暂无封面</div>
            <div class="overflowShow" style="margin-top: 5px; font-size: 15px; color: #333333">{{ item.name }} </div>
            <div style="margin-top: 10px; display: flex; align-items: center">
              <img v-if="item.teacherAvatarUrl" :src="item.teacherAvatarUrl" alt="" style="width: 25px; height: 25px; border-radius: 50%">
              <div v-else style="width: 25px; height: 25px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 10px;">无</div>
              <div style="flex: 1; margin: 0 10px">{{ item.testPaper?.teacherName }}</div>
              <div style="width: 150px; text-align: right">
                <el-tag v-if="item.status === '进行中'" type="success">{{ item.status }}</el-tag>
                <el-tag v-if="item.status === '未开始'" type="warning">{{ item.status }}</el-tag>
                <el-tag v-if="item.status === '已结束'" type="danger">{{ item.status }}</el-tag>
              </div>
            </div>
            <div style="margin-top: 10px">课程名称：{{ item.testPaper?.courseName }}</div>
            <div style="margin-top: 10px">开放时间：{{ item.start }} ~ {{ item.end }}</div>
            <div style="margin-top: 10px">考试时长：{{ item.duration }} 分钟</div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div v-if="data.total" style="margin: 20px 0">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>
  </div>
</template>
<script setup>
import {reactive, onMounted} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const data = reactive({
  courseName: null,
  clazzId: null,
  clazzList: [],
  pageNum: 1,
  pageSize: 8,
  total: 0,
  examData: [],
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

// 加载学生的班级列表
const loadClazzList = () => {
  request.get('/clazz/selectByStudent/' + data.user.id).then(res => {
    if (res.code === '200') {
      data.clazzList = res.data || []
    }
  })
}

const load = () => {
  // 根据学生ID查询其班级的考试
  request.get('/test/selectPageByStudent', {
    params: {
      studentId: data.user.id,
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      courseName: data.courseName,
      clazzId: data.clazzId
    }
  }).then(res => {
    if (res.code === '200') {
      data.examData = res.data.list
      data.total = res.data.total
      // 加载每个考试的课程封面和教师头像
      data.examData.forEach(item => {
        loadCourseImg(item)
        loadTeacherAvatar(item)
      })
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 加载课程封面
const loadCourseImg = (exam) => {
  if (!exam.testPaper?.courseId) return
  request.get('/course/getImgUrl/' + exam.testPaper.courseId).then(res => {
    if (res.code === '200' && res.data) {
      exam.courseImgUrl = res.data + '?t=' + Date.now()
    }
  })
}

// 加载教师头像
const loadTeacherAvatar = (exam) => {
  if (!exam.testPaper?.teacherId) return
  request.get('/teacher/getAvatarUrl/' + exam.testPaper.teacherId).then(res => {
    if (res.code === '200' && res.data) {
      exam.teacherAvatarUrl = res.data + '?t=' + Date.now()
    }
  })
}

const reset = () => {
  data.courseName = null
  data.clazzId = null
  load()
}
const navTo = (item) => {
  if (item.status === '未开始') {
    ElMessage.warning('该考试还未开放')
    return
  }
  if (item.status === '已结束') {
    ElMessage.warning('该考试已结束')
    return;
  }
  // 检查是否已经提交过试卷
  request.get('/testPaper/check/' + item.id).then(res => {
    if (res.code === '200') {
      location.href = '/front/examPreparation?testId=' + item.id
    } else {
      ElMessage.error(res.msg)
    }
  })
}

onMounted(() => {
  // 加载班级列表
  loadClazzList()
  
  // 检查是否从课程页面传来的班级筛选参数
  const query = router.currentRoute.value.query
  if (query.clazzId) {
    data.clazzId = parseInt(query.clazzId)
  }
  
  load()
})
</script>
<style scope>
.overflowShow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
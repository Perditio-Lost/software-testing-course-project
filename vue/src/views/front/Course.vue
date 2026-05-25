<template>
  <div class="main-content">
    <div style="margin: 30px 0; text-align: center">
      <el-input size="large" clearable @clear="reset" style="width: 500px; margin-right: 5px" v-model="data.clazzName" placeholder="请输入班级名称查询"></el-input>
      <el-button size="large" type="primary" plain @click="load">查询</el-button>
    </div>
    <div>
      <el-row :gutter="10">
        <el-col :span="6" v-for="item in data.clazzData" :key="item.id" style="margin-bottom: 15px">
          <div class="card" style="cursor: pointer" @click="navToExam(item)">
            <img v-if="item.courseImgUrl" :src="item.courseImgUrl" alt="" style="width: 100%; height: 150px; object-fit: cover;">
            <div v-else style="width: 100%; height: 150px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; color: #999;">暂无封面</div>
            <div class="overflowShow" style="margin-top: 5px; font-size: 15px; color: #333333">{{ item.name }} </div>
            <div style="margin-top: 10px; display: flex; align-items: center">
              <img v-if="item.teacherAvatarUrl" :src="item.teacherAvatarUrl" alt="" style="width: 25px; height: 25px; border-radius: 50%">
              <div v-else style="width: 25px; height: 25px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 10px;">无</div>
              <div style="flex: 1; margin: 0 10px">{{ item.teacherName }}</div>
            </div>
            <div style="margin-top: 10px">课程：{{ item.courseName }}</div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div v-if="!data.clazzData || data.clazzData.length === 0" style="text-align: center; margin-top: 50px; color: #999;">
      暂无班级信息
    </div>
    <div v-if="data.total" style="margin: 20px 0">
      <el-pagination @current-change="handlePageChange" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>
  </div>
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import router from "@/router/index.js";

const data = reactive({
  clazzName: null,
  clazzData: [],
  pageNum: 1,
  pageSize: 8,
  total: 0,
  allClazzData: [], // 保存所有班级数据用于前端分页
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

const load = () => {
  // 根据学生ID查询其所在的班级
  request.get('/clazz/selectByStudent/' + data.user.id).then(res => {
    if (res.code === '200') {
      data.allClazzData = res.data || []
      // 如果有搜索条件，进行前端筛选（不区分大小写）
      if (data.clazzName) {
        const searchName = data.clazzName.toLowerCase()
        data.allClazzData = data.allClazzData.filter(item => 
          item.name && item.name.toLowerCase().includes(searchName)
        )
      }
      // 更新总数
      data.total = data.allClazzData.length
      // 前端分页
      updatePageData()
    }
  })
}

// 更新当前页的数据
const updatePageData = () => {
  const start = (data.pageNum - 1) * data.pageSize
  const end = start + data.pageSize
  data.clazzData = data.allClazzData.slice(start, end)
  // 加载每个班级的课程封面和教师头像
  data.clazzData.forEach(item => {
    loadCourseImg(item)
    loadTeacherAvatar(item)
  })
}

// 页码改变处理
const handlePageChange = () => {
  updatePageData()
}

// 加载课程封面
const loadCourseImg = (clazz) => {
  if (!clazz.courseId) return
  request.get('/course/getImgUrl/' + clazz.courseId).then(res => {
    if (res.code === '200' && res.data) {
      clazz.courseImgUrl = res.data + '?t=' + Date.now()
    }
  })
}

// 加载教师头像
const loadTeacherAvatar = (clazz) => {
  if (!clazz.teacherId) return
  request.get('/teacher/getAvatarUrl/' + clazz.teacherId).then(res => {
    if (res.code === '200' && res.data) {
      clazz.teacherAvatarUrl = res.data + '?t=' + Date.now()
    }
  })
}

const reset = () => {
  data.clazzName = null
  data.pageNum = 1
  load()
}

// 点击班级卡片，跳转到在线考试页面并自动筛选该班级
const navToExam = (clazz) => {
  router.push({
    path: '/front/exam',
    query: {
      clazzId: clazz.id,
      clazzName: clazz.name
    }
  })
}

load()
</script>

<style scoped>
.overflowShow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  transition: all 0.3s;
  background: white;
}

.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
</style>

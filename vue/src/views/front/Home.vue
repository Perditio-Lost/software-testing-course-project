<template>
  <div style="min-height: 1000px">
    <el-carousel height="300px">
      <el-carousel-item v-for="item in data.carouselData" :key="item">
        <img :src="item" alt="" style="width: 100%; height: 300px">
      </el-carousel-item>
    </el-carousel>
    <div style="width: 80%; margin: 20px auto">
      <div style="font-size: 18px; display: flex">
        <div style="flex: 1">正在进行中的考试</div>
        <div style="width: 100px; text-align: right; color: #666666; font-size: 15px; cursor: pointer" @click="navTo('/front/exam')">查看更多</div>
      </div>
      <div style="margin-top: 15px">
        <el-row :gutter="10">
          <el-col :span="6" v-for="item in data.examData" :key="item.id" style="margin-bottom: 15px">
            <div class="card" style="cursor: pointer" @click="navToTestPaper(item)">
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
            </div>
          </el-col>
        </el-row>
      </div>
      <div style="display: flex; margin-top: 30px">
        <div style="flex: 1">
          <div style="font-size: 18px; display: flex; margin-bottom: 15px">
            <div style="flex: 1">看看大家都在分享什么</div>
            <div style="width: 100px; color: #666666; text-align: right; font-size: 15px; cursor: pointer;" @click="navTo('/front/forum')">查看更多</div>
          </div>
          <el-row :gutter="10">
            <el-col :span="8" v-for="item in data.articleData" :key="item.id" style="margin-bottom: 15px">
              <div class="card" style="cursor: pointer" @click="navTo('/front/forumDetail?id=' + item.id)">
                <img v-if="item.imgUrl" :src="item.imgUrl" alt="" style="width: 100%; height: 150px">
                <div v-else style="width: 100%; height: 150px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; color: #999;">暂无封面</div>
                <div class="overflowShow" style="margin-top: 5px; font-size: 15px; color: #333333">{{ item.title }} </div>
                <div style="margin-top: 10px; display: flex; align-items: center">
                  <img v-if="item.studentAvatarUrl" :src="item.studentAvatarUrl" alt="" style="width: 25px; height: 25px; border-radius: 50%">
                  <div v-else style="width: 25px; height: 25px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 10px;">无</div>
                  <div style="flex: 1; margin: 0 10px">{{ item.studentName }}</div>
                  <div style="width: 150px; text-align: right">{{ item.time }}</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
        <div style="width: 300px; margin-left: 30px">
          <div style="font-size: 18px; margin-bottom: 15px">考试安排</div>
          <div class="card">
            <div @click="viewContent(item)" style="line-height: 43px; padding: 0 10px; cursor: pointer" v-for="item in data.examPlanData" :key="item.id">{{ item.name }}</div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog title="详细信息" v-model="data.dialogVisible" width="40%" destroy-on-close>
      <div style="line-height: 30px">{{ data.form.content }}</div>
      <div style="margin-top: 15px">发布时间：{{ data.form.releaseTime }}</div>
    </el-dialog>
  </div>
</template>
<script setup>
import {reactive} from "vue";
import carousel_1 from "@/assets/imgs/carousel-1.jpg";
import carousel_2 from "@/assets/imgs/carousel-2.jpg";
import carousel_3 from "@/assets/imgs/carousel-3.jpg";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const data = reactive({
  carouselData: [carousel_1, carousel_2, carousel_3],
  examData: [],
  articleData: [],
  examPlanData: [],
  dialogVisible: false,
  form: {},
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

const loadTestPaper = () => {
  // 修改为查询当前学生班级的考试（进行中的）
  request.get('/test/selectPageByStudent', {
    params: {
      studentId: data.user.id,
      pageNum: 1,
      pageSize: 50  // 增加pageSize以确保能获取到所有进行中的考试
    }
  }).then(res => {
    if (res.code === '200') {
      // 只显示进行中的考试，并限制最多显示4个
      data.examData = (res.data.list || [])
        .filter(item => item.status === '进行中')
        .slice(0, 4)  // 最多显示4个
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

const loadArticle = () => {
  request.get('/article/selectRandom').then(res => {
    if (res.code === '200') {
      data.articleData = res.data
      // 加载每个帖子的封面和学生头像
      data.articleData.forEach(item => {
        loadArticleImg(item)
        loadStudentAvatar(item)
      })
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 加载帖子封面
const loadArticleImg = (article) => {
  if (!article.id) return
  request.get('/article/getImgUrl/' + article.id).then(res => {
    if (res.code === '200' && res.data) {
      article.imgUrl = res.data + '?t=' + Date.now()
    }
  })
}

// 加载学生头像
const loadStudentAvatar = (article) => {
  if (!article.studentId) return
  request.get('/student/getAvatarUrl/' + article.studentId).then(res => {
    if (res.code === '200' && res.data) {
      article.studentAvatarUrl = res.data + '?t=' + Date.now()
    }
  })
}

const loadExamPlan = () => {
  // 修改为查询当前学生班级的考试（所有状态）
  request.get('/test/selectPageByStudent', {
    params: {
      studentId: data.user.id,
      pageNum: 1,
      pageSize: 5
    }
  }).then(res => {
    if (res.code === '200') {
      data.examPlanData = res.data.list || []
    } else {
      ElMessage.error(res.msg)
    }
  })
}



const viewContent = (item) => {
  data.form = JSON.parse(JSON.stringify(item))
  data.dialogVisible = true
}
const navTo = (url) => {
  location.href = url
}
const navToTestPaper = (item) => {
  if (item.status === '未开始') {
    ElMessage.warning('该考试还未开放')
    return
  }
  if (item.status === '已结束') {
    ElMessage.warning('该考试已结束')
    return;
  }
  request.get('/testPaper/check/' + item.id).then(res => {
    if (res.code === '200') {
      // 先跳转到考前准备页面，完成手机连接后再进入考试
      location.href = '/front/examPreparation?testId=' + item.id
    } else {
      ElMessage.error(res.msg)
    }
  })
}
loadTestPaper()
loadArticle()
loadExamPlan()
</script>
<style scope>
.overflowShow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
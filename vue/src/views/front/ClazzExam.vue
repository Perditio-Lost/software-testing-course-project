<template>
  <div class="main-content">
    <div style="margin: 30px 0">
      <el-page-header @back="goBack" :icon="ArrowLeft">
        <template #content>
          <div style="font-size: 20px; font-weight: bold">{{ data.clazzName }} - 考试列表</div>
        </template>
      </el-page-header>
    </div>
    <div v-if="data.examData.length === 0" style="text-align: center; padding: 50px 0; color: #999;">
      该班级暂无考试安排
    </div>
    <div v-else>
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
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { ArrowLeft } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const data = reactive({
  clazzId: null,
  clazzName: '',
  examData: []
});

onMounted(() => {
  data.clazzId = route.query.clazzId;
  data.clazzName = route.query.clazzName || '班级';
  if (data.clazzId) {
    loadExams();
  }
});

const loadExams = () => {
  request.get('/test/selectByClazz/' + data.clazzId).then(res => {
    if (res.code === '200') {
      data.examData = res.data || [];
      // 加载每个考试的课程封面和教师头像
      data.examData.forEach(item => {
        loadCourseImg(item);
        loadTeacherAvatar(item);
      });
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 加载课程封面
const loadCourseImg = (exam) => {
  if (!exam.testPaper?.courseId) return;
  request.get('/course/getImgUrl/' + exam.testPaper.courseId).then(res => {
    if (res.code === '200' && res.data) {
      exam.courseImgUrl = res.data + '?t=' + Date.now();
    }
  });
};

// 加载教师头像
const loadTeacherAvatar = (exam) => {
  if (!exam.testPaper?.teacherId) return;
  request.get('/teacher/getAvatarUrl/' + exam.testPaper.teacherId).then(res => {
    if (res.code === '200' && res.data) {
      exam.teacherAvatarUrl = res.data + '?t=' + Date.now();
    }
  });
};

const navTo = (item) => {
  if (item.status === '未开始') {
    ElMessage.warning('该考试还未开放');
    return;
  }
  if (item.status === '已结束') {
    ElMessage.warning('该考试已结束');
    return;
  }
  request.get('/testPaper/check/' + item.id).then(res => {
    if (res.code === '200') {
      // 先跳转到考前准备页面，完成手机连接后再进入考试
      location.href = '/front/examPreparation?testId=' + item.id;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.main-content {
  padding: 20px;
}

.overflowShow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}
</style>

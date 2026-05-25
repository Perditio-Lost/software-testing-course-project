<template>
  <div>
    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <div class="welcome-content">
        <h2>👋 您好！{{ data.user?.name }}</h2>
        <p>欢迎使用智考云-在线考试系统管理后台</p>
      </div>
      <div class="welcome-time">{{ currentTime }}</div>
    </div>

    <!-- 数据统计卡片 -->
    <div class="stats-container">
      <div class="stat-card stat-card-1">
        <div class="stat-icon">
          <el-icon :size="40"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ data.stats.studentCount }}</div>
          <div class="stat-label">学生总数</div>
        </div>
      </div>
      <div class="stat-card stat-card-2">
        <div class="stat-icon">
          <el-icon :size="40"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ data.stats.examCount }}</div>
          <div class="stat-label">考试安排</div>
        </div>
      </div>
      <div class="stat-card stat-card-3">
        <div class="stat-icon">
          <el-icon :size="40"><DocumentCopy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ data.stats.questionCount }}</div>
          <div class="stat-label">题库数量</div>
        </div>
      </div>
      <div class="stat-card stat-card-4">
        <div class="stat-icon">
          <el-icon :size="40"><Bell /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ data.stats.noticeCount }}</div>
          <div class="stat-label">系统公告</div>
        </div>
      </div>
    </div>

    <!-- 时间线展示 -->
    <div style="display: flex; gap: 15px">
      <div class="timeline-card">
        <div class="card-header">
          <el-icon :size="22" color="#2196f3"><Bell /></el-icon>
          <span class="card-title">系统公告</span>
        </div>
        <el-timeline class="custom-timeline">
          <el-timeline-item
            v-for="(item, index) in data.noticeData"
            :key="index"
            :timestamp="item.time"
            color="#2196f3"
          >
            <div class="timeline-content">{{ item.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div class="timeline-card">
        <div class="card-header">
          <el-icon :size="22" color="#4caf50"><Calendar /></el-icon>
          <span class="card-title">考试安排</span>
        </div>
        <el-timeline class="custom-timeline">
          <el-timeline-item
            v-for="(item, index) in data.planData"
            :key="index"
            :timestamp="item.releaseTime"
            color="#4caf50"
          >
            <div class="timeline-content">{{ item.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import {
  User,
  Document,
  DocumentCopy,
  Bell,
  Calendar,
} from "@element-plus/icons-vue";

const currentTime = ref("");

const updateTime = () => {
  const now = new Date();
  const options = {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  };
  currentTime.value = now.toLocaleString("zh-CN", options);
};

let timer = null;

onMounted(() => {
  updateTime();
  timer = setInterval(updateTime, 1000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});

const data = reactive({
  user: JSON.parse(localStorage.getItem("xm-user") || "{}"),
  noticeData: [],
  planData: [],
  stats: {
    studentCount: 0,
    examCount: 0,
    questionCount: 0,
    noticeCount: 0,
  },
});

const loadNotice = () => {
  request.get("/notice/selectAll").then((res) => {
    if (res.code === "200") {
      data.noticeData = res.data;
      data.stats.noticeCount = res.data.length;
      if (data.noticeData.length > 4) {
        data.noticeData = data.noticeData.slice(0, 4);
      }
    } else {
      ElMessage.error(res.msg);
    }
  });
};
const loadExamPlan = () => {
  request.get("/test/selectAll").then((res) => {
    if (res.code === "200") {
      data.planData = res.data;
      data.stats.examCount = res.data.length;
      if (data.planData.length > 4) {
        data.planData = data.planData.slice(0, 4);
      }
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 加载统计数据
const loadStats = () => {
  // 假设后端有对应的接口，如果没有可以通过其他方式获取
  request
    .get("/student/selectAll")
    .then((res) => {
      if (res.code === "200") {
        data.stats.studentCount = res.data.length;
      }
    })
    .catch(() => {
      data.stats.studentCount = "--";
    });

  request
    .get("/question/selectAll")
    .then((res) => {
      if (res.code === "200") {
        data.stats.questionCount = res.data.length;
      }
    })
    .catch(() => {
      data.stats.questionCount = "--";
    });
};

loadNotice();
loadExamPlan();
loadStats();
</script>

<style scoped>
.welcome-card {
  background: linear-gradient(to right, #e8f4f8, #d4e7f5);
  border-radius: 12px;
  padding: 25px 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #d4e7f5;
}

.welcome-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.welcome-content p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.welcome-time {
  font-size: 15px;
  font-weight: 500;
  color: #606266;
}

.stats-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #e8eaed;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card-1 {
  border-left: 4px solid #91c4f2;
}

.stat-card-2 {
  border-left: 4px solid #a5d6a7;
}

.stat-card-3 {
  border-left: 4px solid #ffcc80;
}

.stat-card-4 {
  border-left: 4px solid #ef9a9a;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.stat-card-1 .stat-icon {
  background-color: #e3f2fd;
  color: #2196f3;
}

.stat-card-2 .stat-icon {
  background-color: #e8f5e9;
  color: #4caf50;
}

.stat-card-3 .stat-icon {
  background-color: #fff3e0;
  color: #ff9800;
}

.stat-card-4 .stat-icon {
  background-color: #ffebee;
  color: #f44336;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  font-weight: 400;
}

.timeline-card {
  flex: 1;
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  min-height: 380px;
  border: 1px solid #e8eaed;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8eaed;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.custom-timeline {
  max-width: 100%;
  padding: 10px;
}

.timeline-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  padding: 5px 0;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}
</style>

<template>
  <div class="manager-container">
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/logo.png" alt="" />
        <div class="title">智考云</div>
      </div>
      <div class="manager-header-center">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/manager/home' }">
            <el-icon><HomeFilled /></el-icon>
            首页
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{
            router.currentRoute.value.meta.name
          }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="manager-header-right">
        <!-- 时间显示 -->
        <div class="header-time">
          <el-icon><Clock /></el-icon>
          <span>{{ data.currentTime }}</span>
        </div>

        <!-- 通知中心 -->
        <el-button circle @click="router.push('/manager/notice')">
          <el-icon :size="18"><Bell /></el-icon>
        </el-button>

        <!-- 快捷操作 -->
        <el-tooltip content="快捷操作" placement="bottom">
          <el-dropdown trigger="click">
            <el-button circle>
              <el-icon :size="18"><Operation /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/manager/question')">
                  <el-icon><QuestionFilled /></el-icon>
                  题库管理
                </el-dropdown-item>
                <el-dropdown-item
                  @click="router.push('/manager/student')"
                  v-if="data.user.role === 'ADMIN'"
                >
                  <el-icon><UserFilled /></el-icon>
                  学生管理
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-tooltip>

        <!-- 全屏切换 -->
        <el-tooltip
          :content="data.isFullscreen ? '退出全屏' : '全屏'"
          placement="bottom"
        >
          <el-button circle @click="toggleFullscreen">
            <el-icon :size="18">
              <FullScreen v-if="!data.isFullscreen" />
              <Minus v-else />
            </el-icon>
          </el-button>
        </el-tooltip>

        <!-- 用户信息 -->
        <el-dropdown style="cursor: pointer">
          <div class="user-info-box">
            <el-avatar v-if="data.user.avatar" :size="40" :src="data.user.avatar" class="user-avatar" />
            <div v-else class="user-avatar" style="width: 40px; height: 40px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 12px;">无</div>
            <div class="user-details">
              <span class="user-name">{{ data.user.name }}</span>
              <span class="user-role">{{
                data.user.role === "ADMIN" ? "管理员" : "教师"
              }}</span>
            </div>
            <el-icon color="#606266" :size="16"><arrow-down /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/manager/person')">
                <el-icon><User /></el-icon>
                个人资料
              </el-dropdown-item>
              <el-dropdown-item @click="router.push('/manager/password')">
                <el-icon><Lock /></el-icon>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <!-- 下面部分开始 -->
    <div style="display: flex; flex: 1; min-height: 0;">
      <div class="manager-main-left">
        <el-menu
          :default-active="router.currentRoute.value.path"
          :default-openeds="['1', '2']"
          router
          class="custom-menu"
        >
          <el-menu-item index="/manager/home">
            <el-icon><HomeFilled /></el-icon>
            <span>系统首页</span>
          </el-menu-item>
          <el-sub-menu index="1">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>信息管理</span>
            </template>
            <el-menu-item
              v-if="data.user.role === 'ADMIN'"
              index="/manager/notice"
            >
              <el-icon><Bell /></el-icon>
              系统公告
            </el-menu-item>
            <el-menu-item index="/manager/course">
              <el-icon><Reading /></el-icon>
              课程信息
            </el-menu-item>
            <el-menu-item index="/manager/clazz">
              <el-icon><School /></el-icon>
              班级管理
            </el-menu-item>
            <el-menu-item index="/manager/question">
              <el-icon><QuestionFilled /></el-icon>
              题库信息
            </el-menu-item>
            <el-menu-item index="/manager/testPaper">
              <el-icon><Document /></el-icon>
              试卷信息
            </el-menu-item>
            <el-menu-item index="/manager/test">
              <el-icon><Document /></el-icon>
              考试管理
            </el-menu-item>
            <el-menu-item index="/manager/score">
              <el-icon><Tickets /></el-icon>
              {{ data.user.role === "ADMIN" ? "成绩管理" : "阅卷打分" }}
            </el-menu-item>
            <el-menu-item index="/manager/abnormalExam">
              <el-icon><WarnTriangleFilled /></el-icon>
              异常试卷
            </el-menu-item>
            <el-menu-item index="/manager/proctoring">
              <el-icon><Monitor /></el-icon>
              监考大屏
            </el-menu-item>
            <el-menu-item
              v-if="data.user.role === 'ADMIN'"
              index="/manager/share"
            >
              <el-icon><ChatDotRound /></el-icon>
              交流分享
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="2" v-if="data.user.role === 'ADMIN'">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/manager/admin">
              <el-icon><Avatar /></el-icon>
              管理员信息
            </el-menu-item>
            <el-menu-item index="/manager/teacher">
              <el-icon><User /></el-icon>
              教师信息
            </el-menu-item>
            <el-menu-item index="/manager/student">
              <el-icon><School /></el-icon>
              学生信息
            </el-menu-item>
            <el-menu-item index="/manager/systemLog">
              <el-icon><Document /></el-icon>
              系统日志
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
      <div class="manager-main-right">
        <RouterView @updateUser="updateUser" />
      </div>
    </div>
    <!-- 下面部分结束 -->
  </div>
</template>

<script setup>
import { reactive, onMounted, onUnmounted } from "vue";
import router from "@/router/index.js";
import { ElMessage } from "element-plus";
import request from "@/utils/request.js";
import {
  HomeFilled,
  Grid,
  Bell,
  Calendar,
  CollectionTag,
  Reading,
  QuestionFilled,
  Document,
  Tickets,
  ChatDotRound,
  UserFilled,
  Avatar,
  User,
  School,
  Lock,
  SwitchButton,
  Clock,
  Operation,
  FullScreen,
  Minus,
  Monitor,
  WarnTriangleFilled,
} from "@element-plus/icons-vue";

const userInfo = JSON.parse(localStorage.getItem("xm-user") || "{}");
// 清空数据库的avatar字段，强制从COS加载
userInfo.avatar = null;

const data = reactive({
  user: userInfo,
  currentTime: "",
  noticeCount: 3, // 模拟通知数量
  isFullscreen: false,
});

// 加载头像
const loadAvatar = () => {
  if (!data.user.id) return;
  
  let apiUrl = '';
  if (data.user.role === 'ADMIN') {
    apiUrl = '/admin/getAvatarUrl/' + data.user.id;
  } else if (data.user.role === 'TEACHER') {
    apiUrl = '/teacher/getAvatarUrl/' + data.user.id;
  } else {
    return;
  }
  
  request.get(apiUrl).then(res => {
    if (res.code === '200' && res.data) {
      // 添加时间戳防止浏览器缓存
      data.user.avatar = res.data + '?t=' + Date.now();
      // 同步更新localStorage
      const userInfo = JSON.parse(localStorage.getItem('xm-user') || '{}');
      userInfo.avatar = res.data;
      localStorage.setItem('xm-user', JSON.stringify(userInfo));
    }
  }).catch(err => {
    
  });
};

// 更新时间
const updateTime = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");
  const seconds = String(now.getSeconds()).padStart(2, "0");
  data.currentTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    data.isFullscreen = true;
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
      data.isFullscreen = false;
    }
  }
};

// 监听全屏状态变化
const handleFullscreenChange = () => {
  data.isFullscreen = !!document.fullscreenElement;
};

let timeInterval = null;

onMounted(() => {
  updateTime();
  timeInterval = setInterval(updateTime, 1000);
  document.addEventListener("fullscreenchange", handleFullscreenChange);
  loadAvatar();
});

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval);
  }
  document.removeEventListener("fullscreenchange", handleFullscreenChange);
});

const logout = () => {
  localStorage.removeItem("xm-user");
  router.push("/login");
  ElMessage.success("已退出登录");
};

const updateUser = () => {
  const userInfo = JSON.parse(localStorage.getItem("xm-user") || "{}");
  // 清空数据库的avatar字段，强制从COS加载
  userInfo.avatar = null;
  data.user = userInfo;
  loadAvatar();
};

if (!data.user.id) {
  logout();
  ElMessage.error("请登录！");
}
</script>

<style scoped>
@import "@/assets/css/manager.css";

.manager-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-right: 24px;
}

.header-time {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  padding: 10px 18px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  border-radius: 24px;
  transition: all 0.3s ease;
  border: 1px solid var(--border-light);
}

.header-time:hover {
  background: linear-gradient(135deg, #bae7ff 0%, #e6f7ff 100%);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.header-badge {
  margin-right: 5px;
}

.header-badge :deep(.el-button) {
  background: #ffffff;
  border: 2px solid var(--border-light);
  transition: all 0.3s ease;
}

.header-badge :deep(.el-button:hover) {
  background: var(--bg-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

:deep(.el-badge__content.is-fixed) {
  top: 8px;
  right: 12px;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  border: 2px solid #ffffff;
}

.manager-header-right .el-button.is-circle {
  width: 42px;
  height: 42px;
  background: #ffffff;
  border: 2px solid var(--border-light);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.manager-header-right .el-button.is-circle:hover {
  background: var(--bg-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.user-info-box {
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.3s ease;
  border-left: 2px solid var(--border-light);
  margin-left: 12px;
  cursor: pointer;
}

.user-info-box:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.user-avatar {
  border: 3px solid var(--border-light);
  box-shadow: 0 3px 10px rgba(24, 144, 255, 0.15);
  transition: all 0.3s ease;
}

.user-info-box:hover .user-avatar {
  border-color: var(--primary-lightest);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  color: var(--text-primary);
  font-weight: 700;
  font-size: 15px;
}

.user-role {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.custom-menu {
  padding: 12px 0;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  transition: all 0.3s ease;
  font-weight: 500;
}

:deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(90deg, #e6f7ff 0%, #f0f5ff 100%) !important;
  color: var(--primary-color);
  transform: translateX(4px);
}

:deep(.el-breadcrumb__item) {
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.el-breadcrumb__inner) {
  transition: all 0.3s ease;
}

:deep(.el-breadcrumb__inner:hover) {
  transform: translateX(2px);
}
</style>

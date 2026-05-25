<template>
  <div>
    <div class="front-notice"><el-icon><Bell /></el-icon>公告：{{ data.top }}</div>
    <div class="front-header" :class="{hidden: isExam}">
      <div class="front-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title">智考云</div>
      </div>
      <div class="front-header-center">
        <el-menu :default-active="router.currentRoute.value.path" router mode="horizontal">
          <el-menu-item index="/front/home">首页</el-menu-item>
          <el-menu-item index="/front/course">课程</el-menu-item>
          <el-menu-item index="/front/exam">在线考试</el-menu-item>
          <el-menu-item index="/front/forum">交流论坛</el-menu-item>
          <el-menu-item index="/front/score">我的成绩</el-menu-item>
          <el-menu-item index="/front/myShare">我的分享</el-menu-item>
          <el-menu-item index="/front/person">个人中心</el-menu-item>
        </el-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id">
          <el-button @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
        <div v-else>
          <el-dropdown style="cursor: pointer; height: 60px">
            <div style="display: flex; align-items: center">
              <img v-if="data.user.avatar" style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
              <div v-else style="width: 40px; height: 40px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 12px;">无</div>
              <span style="margin-left: 5px;">{{ data.user.name }}</span><el-icon><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    <div class="main-body">
      <RouterView @updateUser="updateUser" />
    </div>
  </div>
</template>

<script setup>
  import router from "@/router/index.js";
  import { reactive, onMounted } from "vue";
  import request from "@/utils/request.js";

  const userInfo = JSON.parse(localStorage.getItem('xm-user') || '{}');
  // 清空数据库的avatar字段，强制从COS加载
  userInfo.avatar = null;

  const data = reactive({
    user: userInfo,
    top: '',
    noticeData: []
  })

  // 加载头像
  const loadAvatar = () => {
    if (!data.user.id || data.user.role !== 'STUDENT') return;
    
    request.get('/student/getAvatarUrl/' + data.user.id).then(res => {
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
  }

  const logout = () => {
    localStorage.removeItem('xm-user')
    router.push('/login')
  }

  const updateUser = () => {
    const userInfo = JSON.parse(localStorage.getItem('xm-user') || '{}');
    // 清空数据库的avatar字段，强制从COS加载
    userInfo.avatar = null;
    data.user = userInfo;
    loadAvatar()
  }

  const isExam = !!document.body.classList.contains('exam-mode')

  onMounted(() => {
    loadAvatar()
    loadNotice()
  })

  const loadNotice = () => {
    request.get('/notice/selectAll').then(res => {
      data.noticeData = res.data
      let i = 0
      if (data.noticeData && data.noticeData.length) {
        data.top = data.noticeData[0].content
        setInterval(() => {
          data.top = data.noticeData[i].content
          i++
          if (i === data.noticeData.length) {
            i = 0
          }
        }, 2500)
      }
    })
  }
</script>

<style scoped>
@import "../assets/css/front.css";

.hidden {
  display: none !important;
}

.front-header-right .el-button {
  border-radius: 20px;
  font-weight: 600;
  padding: 10px 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid var(--border-light);
}

.front-header-right .el-button:first-child {
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  color: var(--primary-color);
  border-color: var(--border-normal);
}

.front-header-right .el-button:first-child:hover {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.front-header-right .el-button:last-child {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #ffffff;
  border: none;
}

.front-header-right .el-button:last-child:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.front-header-right .el-dropdown {
  height: 65px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.front-header-right .el-dropdown:hover {
  background: var(--bg-light);
}

.front-header-right .el-dropdown img,
.front-header-right .el-dropdown > div > div {
  border: 3px solid var(--border-light);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  transition: all 0.3s ease;
}

.front-header-right .el-dropdown:hover img,
.front-header-right .el-dropdown:hover > div > div {
  border-color: var(--primary-lightest);
  box-shadow: 0 3px 12px rgba(24, 144, 255, 0.25);
}

.front-header-right .el-dropdown span {
  margin-left: 8px;
  font-weight: 600;
  color: var(--text-primary);
}

.main-body {
  min-height: calc(100vh - 105px);
  background: linear-gradient(135deg, #f0f5ff 0%, #fafcff 100%);
}

:deep(.el-dropdown-menu__item) {
  padding: 12px 24px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(90deg, #e6f7ff 0%, #f0f5ff 100%) !important;
  color: var(--primary-color);
  transform: translateX(4px);
}
</style>
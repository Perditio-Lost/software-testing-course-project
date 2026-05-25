<template>
  <div class="main-content">
    <div style="margin: 30px 0; text-align: center">
      <el-input size="large" clearable @clear="reset" style="width: 500px; margin-right: 5px" v-model="data.title" placeholder="请输入帖子标题查询"></el-input>
      <el-button size="large" type="primary" plain @click="load">查询</el-button>
    </div>
    <div>
      <el-row :gutter="10">
       <el-col :span="6" v-for="item in data.articleData" style="margin-bottom: 15px">
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
    <div v-if="data.total" style="margin: 20px 0">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>
  </div>
</template>
<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const data = reactive({
  title: null,
  pageNum: 1,
  pageSize: 8,
  total: 0,
  articleData: []
})

const load = () => {
  request.get('/article/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      title: data.title
    }
  }).then(res => {
    if (res.code === '200') {
      data.articleData = res.data.list
      data.total = res.data.total
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

const reset = () => {
  data.title = null
  load()
}
const navTo = (url) => {
  location.href = url
}
load()
</script>
<style scope>
.overflowShow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
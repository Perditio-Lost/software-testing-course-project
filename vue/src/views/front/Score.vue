<template>
  <div style="width: 60%; margin: 30px auto">
    <div style="font-weight: bold; font-size: 17px">我的考试（{{ data.tableData.length }}）</div>
    <div style="margin: 20px 0">
      <el-table stripe :data="data.tableData">
        <el-table-column label="试卷名称" show-overflow-tooltip>
          <template v-slot="scope">
            {{ scope.row.test?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="课程名称" show-overflow-tooltip>
          <template v-slot="scope">
            {{ scope.row.test?.courseName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="授课教师" show-overflow-tooltip>
          <template v-slot="scope">
            {{ scope.row.test?.teacherName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="试卷状态" show-overflow-tooltip>
          <template v-slot="scope">
            <el-tag v-if="scope.row.status === '已批改'" type="success">{{ scope.row.status }}</el-tag>
            <el-tag v-else type="warning">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" show-overflow-tooltip/>
        <el-table-column label="操作" width="200" fixed="right">
          <template v-slot="scope">
            <el-tooltip 
              v-if="scope.row.cheat === 1"
              content="本次考试有作弊行为，请联系管理员"
              placement="top"
            >
              <el-button type="danger" plain disabled>查看试卷</el-button>
            </el-tooltip>
            <el-button 
              v-else-if="scope.row.status !== '已批改' || !scope.row.weatherCheck"
              type="danger" 
              plain
              disabled
            >
              查看试卷
            </el-button>
            <el-button 
              v-else
              type="success" 
              plain
              @click="navTo('/front/testPaperView?id=' + scope.row.id)"
            >
              查看试卷
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>
  </div>
</template>
<script setup>

import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const data = reactive({
  tableData: [],
  pageNum: 1,
  pageSize: 5,
  total: 0
})

const load = () => {
  request.get('/score/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data.list
      data.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
  })
}
const navTo = (url) => {
  location.href = url
}
load()
</script>
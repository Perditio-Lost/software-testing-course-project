<template>
  <div>
    <div class="card" style="margin-bottom: 10px;">
      <el-select v-model="data.role" placeholder="操作人角色" style="width: 180px; margin-right: 10px;" clearable @clear="load">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="教师" value="TEACHER" />
        <el-option label="学生" value="STUDENT" />
      </el-select>
      <el-input v-model="data.userName" placeholder="请输入操作人姓名" style="width: 200px; margin-right: 10px;" clearable @clear="load"/>
      <el-input v-model="data.operation" placeholder="请输入操作内容" style="width: 200px; margin-right: 10px;" clearable @clear="load"/>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
    </div>

    <div class="card">
      <div style="margin-bottom: 10px; font-weight: bold; font-size: 16px;">
        系统日志列表
      </div>
      <el-table 
        :data="data.tableData" 
        stripe
        :header-cell-style="{background: '#f2f5fc', color: '#555'}"
      >
        <el-table-column label="操作人角色" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.role === 'ADMIN'" type="danger">管理员</el-tag>
            <el-tag v-else-if="scope.row.role === 'TEACHER'" type="warning">教师</el-tag>
            <el-tag v-else-if="scope.row.role === 'STUDENT'" type="success">学生</el-tag>
            <el-tag v-else type="info">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="120">
          <template #default="scope">
            {{ scope.row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作内容" min-width="150"/>
        <el-table-column prop="method" label="请求方法" min-width="180" show-overflow-tooltip/>
        <el-table-column prop="params" label="请求参数" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="ip" label="IP地址" width="130"/>
        <el-table-column prop="location" label="IP所在地" width="100"/>
        <el-table-column prop="time" label="操作时间" width="160"/>
        <el-table-column label="执行时长" width="100">
          <template #default="scope">
            <el-tag :type="getDurationTag(scope.row.duration)">
              {{ scope.row.duration }}ms
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <div class="card" v-if="data.total" style="margin-top: 5px;">
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
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const data = reactive({
  role: '',
  userName: '',
  operation: '',
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const load = () => {
  request.get('/systemLog/selectPage', {
    params: {
      role: data.role,
      userName: data.userName,
      operation: data.operation,
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

const reset = () => {
  data.role = ''
  data.userName = ''
  data.operation = ''
  data.pageNum = 1
  load()
}

const handleCurrentChange = (pageNum) => {
  data.pageNum = pageNum
  load()
}

// 根据执行时长返回标签颜色
const getDurationTag = (duration) => {
  if (duration < 100) return 'success'
  if (duration < 500) return ''
  if (duration < 1000) return 'warning'
  return 'danger'
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

/* 禁用表格和标签的过渡动画 */
:deep(.el-table),
:deep(.el-tag) {
  transition: none !important;
}

:deep(.el-table__body tr),
:deep(.el-table__body td) {
  transition: none !important;
}
</style>

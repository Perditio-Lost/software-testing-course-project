<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" prefix-icon="Search" style="width: 240px; margin-right: 10px" placeholder="请输入标题查询"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button type="primary" plain style="margin-left: 10px" @click="handleAdd">发起考试</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData">
        <el-table-column prop="name" label="考试名称" />
        <el-table-column prop="content" label="考试安排" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" />
        <el-table-column prop="start" label="开始时间" />
        <el-table-column prop="end" label="结束时间" />
        <el-table-column prop="status" label="状态">
          <template v-slot="scope">
            <el-tag v-if="scope.row.status === '未开始'" type="info">{{ scope.row.status }}</el-tag>
            <el-tag v-else-if="scope.row.status === '进行中'" type="success">{{ scope.row.status }}</el-tag>
            <el-tag v-else type="danger">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="releaseTime" label="发布时间" />
        <el-table-column label="操作" width="100" fixed="right">
          <template v-slot="scope">
            <el-button v-if="scope.row.status === '未开始'" type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button v-if="scope.row.status === '未开始'" type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog 
      title="发起考试" 
      v-model="data.formVisible" 
      width="600px" 
      @close="resetForm"
      align-center
      :close-on-click-modal="false"
    >
      <div style="max-height: 60vh; overflow-y: auto; padding-right: 10px;">
        <el-form :model="data.form" label-width="100px">
        <el-form-item label="考试名称" required>
          <el-input v-model="data.form.name" placeholder="请输入考试名称"></el-input>
        </el-form-item>
        <el-form-item label="选择试卷" required>
          <el-select v-model="data.form.testPaperId" placeholder="请选择试卷" style="width: 100%" @change="handleTestPaperChange" :disabled="!!data.form.id">
            <el-option
              v-for="item in data.testPaperData"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发放班级" required v-if="data.clazzData.length > 0">
          <el-select v-model="data.form.clazzIds" placeholder="请选择班级（可多选）" multiple style="width: 100%">
            <el-option
              v-for="item in data.clazzData"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="data.form.testPaperId && data.clazzData.length === 0">
          <el-alert title="该试卷对应的课程暂无班级" type="warning" :closable="false" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="data.form.start"
            type="datetime"
            placeholder="请选择开始时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="validateDuration"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="data.form.end"
            type="datetime"
            placeholder="请选择结束时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="validateDuration"
          />
        </el-form-item>
        <el-form-item label="考试时长" required>
          <el-input 
            v-model="data.form.duration" 
            placeholder="请输入考试时长（分钟）" 
            type="number"
            :max="maxDuration"
            @input="validateDuration"
          >
            <template #append>分钟</template>
          </el-input>
          <div v-if="maxDuration > 0" style="font-size: 12px; color: #909399; margin-top: 4px;">
            最大时长：{{ maxDuration }} 分钟
          </div>
        </el-form-item>
        <el-form-item label="学生可查看">
          <el-switch v-model="data.form.weatherCheck" active-text="是" inactive-text="否"></el-switch>
        </el-form-item>
        <el-form-item label="题目乱序">
          <el-switch v-model="data.form.weatherQuestionUnordered" active-text="是" inactive-text="否"></el-switch>
        </el-form-item>
        <el-form-item label="选项乱序">
          <el-switch v-model="data.form.weatherChoiceUnordered" active-text="是" inactive-text="否"></el-switch>
        </el-form-item>
        <el-form-item label="人脸识别">
          <el-switch v-model="data.form.weatherFace" active-text="开启" inactive-text="关闭" @change="handleFaceChange"></el-switch>
        </el-form-item>
        <el-form-item label="双机位监考">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-switch v-model="data.form.weatherDualCamera" active-text="开启" inactive-text="关闭" @change="handleDualCameraChange"></el-switch>
            <el-tooltip content="开启双机位监考将自动开启人脸识别" placement="top">
              <el-icon :size="16" style="color: #909399; cursor: help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item label="防复制粘贴">
          <el-switch v-model="data.form.weatherCopy" active-text="开启" inactive-text="关闭"></el-switch>
        </el-form-item>
        <el-form-item label="切屏监控">
          <el-switch v-model="data.form.weatherScreenSwitch" active-text="开启" inactive-text="关闭"></el-switch>
        </el-form-item>
        <el-form-item label="考试安排">
          <el-input v-model="data.form.content" type="textarea" :rows="4" placeholder="请输入考试安排内容"></el-input>
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="danger" plain @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" plain @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>

import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit, QuestionFilled } from "@element-plus/icons-vue";

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  testPaperData: [],
  clazzData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  maxDuration: 0
})

const loadTestPapers = () => {
  const params = {};
  // 如果是教师，只加载自己的试卷；如果是管理员，加载所有试卷
  if (data.user.role === 'TEACHER') {
    params.teacherId = data.user.id;
  }
  
  return request.get('/testPaper/selectAll', { params }).then(res => {
    if (res.code === '200') {
      data.testPaperData = res.data || [];
      return res.data || [];
    } else {
      ElMessage.error(res.msg);
      return [];
    }
  });
};

// 处理试卷选择变化，加载对应的班级
const handleTestPaperChange = (testPaperId) => {
  if (!testPaperId) {
    data.clazzData = [];
    data.form.clazzIds = [];
    return;
  }
  
  // 查找选中的试卷
  const selectedPaper = data.testPaperData.find(paper => paper.id === testPaperId);
  if (!selectedPaper || !selectedPaper.courseId) {
    data.clazzData = [];
    data.form.clazzIds = [];
    return;
  }
  
  // 根据试卷的课程ID查询该课程下的所有班级（不限制教师）
  request.get('/clazz/selectByCourseAndTeacher', {
    params: {
      courseId: selectedPaper.courseId
      // 不传teacherId参数，获取该课程下的所有班级
    }
  }).then(res => {
    if (res.code === '200') {
      data.clazzData = res.data || [];
      // 只有在新增时才清空已选择的班级，编辑时保留原有选择
      if (!data.form.id) {
        data.form.clazzIds = [];
      }
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 处理人脸识别开关变化
const handleFaceChange = (val) => {
  // 如果关闭人脸识别，则必须关闭双机位监考
  if (!val) {
    data.form.weatherDualCamera = false;
  }
};

// 处理双机位监考开关变化
const handleDualCameraChange = (val) => {
  // 如果开启双机位监考，则必须开启人脸识别
  if (val) {
    data.form.weatherFace = true;
  }
};

// 验证并限制考试时长
const validateDuration = () => {
  if (!data.form.start || !data.form.end) {
    data.maxDuration = 0;
    return;
  }
  
  const startTime = new Date(data.form.start).getTime();
  const endTime = new Date(data.form.end).getTime();
  
  if (endTime <= startTime) {
    data.maxDuration = 0;
    return;
  }
  
  // 计算最大时长（分钟）
  data.maxDuration = Math.floor((endTime - startTime) / 1000 / 60);
  
  // 如果当前输入的时长超过最大值，自动调整为最大值
  if (data.form.duration && Number(data.form.duration) > data.maxDuration) {
    data.form.duration = data.maxDuration;
    ElMessage.warning(`考试时长已自动调整为最大值 ${data.maxDuration} 分钟`);
  }
};

const load = () => {
  request.get('/test/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

const handleAdd = () => {
  data.form = {
    weatherCheck: true,
    weatherQuestionUnordered: false,
    weatherChoiceUnordered: false,
    weatherFace: false,
    weatherDualCamera: false,
    weatherCopy: false,
    weatherScreenSwitch: false
  };
  data.formVisible = true;
  loadTestPapers();
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
  // 先加载试卷数据，完成后再加载班级数据
  loadTestPapers().then(() => {
    if (data.form.testPaperId) {
      handleTestPaperChange(data.form.testPaperId)
    }
  })
}

const save = () => {
  if (!data.form.name || !data.form.testPaperId || !data.form.start || !data.form.end || !data.form.duration) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  
  // 验证考试时长不能超过开始时间和结束时间的差值
  const startTime = new Date(data.form.start).getTime();
  const endTime = new Date(data.form.end).getTime();
  const timeDiffMinutes = Math.floor((endTime - startTime) / 1000 / 60); // 转换为分钟
  
  if (Number(data.form.duration) > timeDiffMinutes) {
    ElMessage.warning(`考试时长不能超过 ${timeDiffMinutes} 分钟（开始时间到结束时间的时长）`);
    return;
  }
  
  if (startTime >= endTime) {
    ElMessage.warning('结束时间必须晚于开始时间');
    return;
  }
  
  // 如果是教师，必须选择班级
  if (data.user.role === 'TEACHER' && (!data.form.clazzIds || data.form.clazzIds.length === 0)) {
    ElMessage.warning('请至少选择一个班级');
    return;
  }
  
  // 根据是否有id判断是新增还是修改
  const url = data.form.id ? '/test/update' : '/test/add';
  const method = data.form.id ? 'put' : 'post';
  const successMsg = data.form.id ? '修改成功' : '发起考试成功';
  
  request[method](url, data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success(successMsg);
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const resetForm = () => {
  data.form = {};
  data.clazzData = [];
};

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/test/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}

const reset = () => {
  data.name = null
  load()
}

load()
</script>
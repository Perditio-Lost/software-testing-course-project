<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" prefix-icon="Search" style="width: 240px; margin-right: 10px" placeholder="请输入班级名称查询"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button v-if="data.user.role === 'ADMIN'" type="primary" plain style="margin-left: 10px" @click="handleAdd">新增</el-button>
      <el-button v-if="data.user.role === 'ADMIN'" type="danger" plain style="margin-left: 10px" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column v-if="data.user.role === 'ADMIN'" type="selection" width="55"/>
        <el-table-column prop="name" label="班级名称" show-overflow-tooltip/>
        <el-table-column prop="courseName" label="课程名称" show-overflow-tooltip/>
        <el-table-column v-if="data.user.role === 'ADMIN'" prop="teacherName" label="授课教师" show-overflow-tooltip/>
        <el-table-column label="学生名单" width="120">
          <template v-slot="scope">
            <el-button type="info" plain size="small" @click="viewStudents(scope.row)">查看学生</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="data.user.role === 'ADMIN' ? '200' : '120'" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button v-if="data.user.role === 'ADMIN'" type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="data.form.id ? '编辑班级' : '新增班级'" v-model="data.formVisible" width="50%" destroy-on-close>
      <el-form :model="data.form" label-width="100px" style="padding-right: 20px">
        <el-form-item label="班级名称" required>
          <el-input v-model="data.form.name" placeholder="请输入班级名称"></el-input>
        </el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="data.form.courseId" placeholder="请选择课程" style="width: 100%" :disabled="data.user.role === 'TEACHER' && !!data.form.id">
            <el-option v-for="item in data.courseList" :key="item.id" :label="item.name" :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="data.user.role === 'ADMIN'" label="授课教师" required>
          <el-select v-model="data.form.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option v-for="item in data.teacherList" :key="item.id" :label="item.name" :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="班级学生" v-if="data.form.id">
          <div style="margin-bottom: 10px; display: flex; align-items: center; gap: 10px">
            <el-input v-model="data.studentIdInput" placeholder="请输入学生ID" style="width: 200px" type="number"></el-input>
            <el-button type="warning" plain size="small" @click="addStudent">添加学生</el-button>
            <el-upload
              style="display: contents"
              :action="''"
              :before-upload="handleExcelUpload"
              :show-file-list="false"
              accept=".xlsx,.xls"
            >
              <el-button type="success" plain size="small">Excel导入</el-button>
            </el-upload>
            <el-button type="primary" plain size="small" @click="downloadStudentTemplate">下载模板</el-button>
          </div>
          <el-table :data="data.classStudents" border max-height="300">
            <el-table-column prop="id" label="学生ID" width="100"/>
            <el-table-column prop="name" label="姓名"/>
            <el-table-column label="操作" width="100">
              <template v-slot="scope">
                <el-button type="danger" plain size="small" @click="removeStudent(scope.row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" plain @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" plain @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生名单对话框 -->
    <el-dialog title="班级学生名单" v-model="data.studentVisible" width="50%" destroy-on-close>
      <el-table :data="data.viewStudents" border>
        <el-table-column prop="id" label="学生ID" width="100"/>
        <el-table-column prop="name" label="姓名"/>
        <el-table-column prop="phone" label="电话" width="150"/>
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip/>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive, onMounted} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox, ElLoading} from "element-plus";
import {Delete, Edit} from "@element-plus/icons-vue";

const user = JSON.parse(localStorage.getItem('xm-user') || '{}')

const data = reactive({
  user: user,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  ids: [],
  formVisible: false,
  studentVisible: false,
  form: {},
  courseList: [],
  teacherList: [],
  studentList: [],
  classStudents: [],
  viewStudents: [],
  studentIdInput: null
})

const load = () => {
  // 根据角色选择不同的接口
  const url = data.user.role === 'ADMIN' ? '/clazz/selectPage' : '/clazz/selectPageByTeacher'
  const params = {
    pageNum: data.pageNum,
    pageSize: data.pageSize,
    name: data.name
  }
  
  // 如果是教师，添加teacherId参数
  if (data.user.role === 'TEACHER') {
    params.teacherId = data.user.id
  }
  
  request.get(url, { params }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const reset = () => {
  data.name = null
  load()
}

const handleAdd = () => {
  data.form = {}
  // 如果是教师，自动设置teacherId
  if (data.user.role === 'TEACHER') {
    data.form.teacherId = data.user.id
  }
  data.classStudents = []
  data.studentIdInput = null
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.studentIdInput = null
  loadClassStudents(row.id)
  data.formVisible = true
}

const save = () => {
  if (!data.form.name || !data.form.courseId || !data.form.teacherId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  const url = data.form.id ? '/clazz/update' : '/clazz/add'
  const method = data.form.id ? 'put' : 'post'
  
  request[method](url, data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const del = (id) => {
  ElMessageBox.confirm('删除班级将同时删除该班级的所有学生关联，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/clazz/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleSelectionChange = (selection) => {
  data.ids = selection.map(v => v.id)
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning('请选择要删除的班级')
    return
  }
  ElMessageBox.confirm('删除班级将同时删除该班级的所有学生关联，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/clazz/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleExcelUpload = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('clazzId', data.form.id)
  
  const loading = ElLoading.service({
    lock: true,
    text: '正在导入学生，请稍候...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  request.post('/clazz/importStudents', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    loading.close()
    if (res.code === '200') {
      ElMessage.success(res.data || '导入成功')
      loadClassStudents(data.form.id)
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    loading.close()
    ElMessage.error('导入失败')
  })
  
  return false // 阻止自动上传
}

const viewStudents = (row) => {
  request.get('/clazz/getStudents/' + row.id).then(res => {
    if (res.code === '200') {
      data.viewStudents = res.data || []
      data.studentVisible = true
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadClassStudents = (clazzId) => {
  request.get('/clazz/getStudents/' + clazzId).then(res => {
    if (res.code === '200') {
      data.classStudents = res.data || []
    }
  })
}

const addStudent = () => {
  if (!data.studentIdInput) {
    ElMessage.warning('请输入学生ID')
    return
  }
  
  const studentId = parseInt(data.studentIdInput)
  
  // 先检查学生是否存在
  request.get('/student/selectById/' + studentId).then(res => {
    if (res.code === '200' && res.data) {
      // 学生存在，检查是否已在班级中
      const exists = data.classStudents.some(s => s.id === studentId)
      if (exists) {
        ElMessage.warning('学生已在班级内')
        return
      }
      
      // 添加学生到班级
      request.post('/clazz/addStudent', {
        clazzId: data.form.id,
        studentId: studentId
      }).then(res => {
        if (res.code === '200') {
          ElMessage.success('添加成功')
          loadClassStudents(data.form.id)
          data.studentIdInput = null
        } else {
          ElMessage.error(res.msg)
        }
      })
    } else {
      ElMessage.error('学生不存在')
    }
  }).catch(err => {
    ElMessage.error('学生不存在')
  })
}

const removeStudent = (student) => {
  ElMessageBox.confirm('确定要将该学生从班级中移除吗？', '确认', { type: 'warning' }).then(() => {
    request.delete('/clazz/removeStudent/' + data.form.id + '/' + student.id).then(res => {
      if (res.code === '200') {
        ElMessage.success('移除成功')
        loadClassStudents(data.form.id)
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const loadCourses = () => {
  request.get('/course/selectAll').then(res => {
    if (res.code === '200') {
      data.courseList = res.data || []
    }
  })
}

const loadTeachers = () => {
  request.get('/teacher/selectAll').then(res => {
    if (res.code === '200') {
      data.teacherList = res.data || []
    }
  })
}

const loadStudents = () => {
  request.get('/student/selectApproved').then(res => {
    if (res.code === '200') {
      data.studentList = res.data || []
    }
  })
}

const downloadStudentTemplate = () => {
  const link = document.createElement('a')
  link.href = 'https://onlineexamnation-1392474432.cos.ap-guangzhou.myqcloud.com/example-excel/%E5%AD%A6%E7%94%9F%E5%AF%BC%E5%85%A5%E7%8F%AD%E7%BA%A7%E6%A8%A1%E6%9D%BF.xlsx'
  link.download = '学生导入班级模板.xlsx'
  link.target = '_blank'
  link.click()
}

onMounted(() => {
  load()
  loadCourses()
  // 只有管理员才需要加载教师列表
  if (data.user.role === 'ADMIN') {
    loadTeachers()
  }
  loadStudents()
})
</script>

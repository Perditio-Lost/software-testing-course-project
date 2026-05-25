<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" prefix-icon="Search" style="width: 240px; margin-right: 10px" placeholder="请输入名称查询"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button type="primary" plain style="margin-left: 10px" @click="handleAdd">新增</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData">
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="avatar" label="头像">
          <template v-slot="scope">
            <el-image style="width: 40px; height: 40px; border-radius: 50%; display: block" v-if="scope.row.avatarUrl"
                      :src="scope.row.avatarUrl" :preview-src-list="[scope.row.avatarUrl]" preview-teleported></el-image>
            <span v-else style="color: #999;">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="120">
          <template v-slot="scope">
            <el-tag v-if="scope.row.status === '审核通过'" type="success">{{ scope.row.status }}</el-tag>
            <el-tag v-else-if="scope.row.status === '待审核'" type="warning">{{ scope.row.status }}</el-tag>
            <el-tag v-else-if="scope.row.status === '审核不通过'" type="danger">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="学生信息" v-model="data.formVisible" width="50%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="80px" style="padding: 20px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="username" label="用户名">
              <el-input v-model="data.form.username" placeholder="请输入用户名"></el-input>
            </el-form-item>
            <el-form-item prop="name" label="姓名">
              <el-input v-model="data.form.name" placeholder="请输入姓名"></el-input>
            </el-form-item>
            <el-form-item prop="phone" label="电话">
              <el-input v-model="data.form.phone" placeholder="请输入电话"></el-input>
            </el-form-item>
            <el-form-item prop="email" label="邮箱">
              <el-input v-model="data.form.email" placeholder="请输入邮箱"></el-input>
            </el-form-item>
            <el-form-item prop="status" label="状态">
              <el-select v-model="data.form.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="待审核" value="待审核"></el-option>
                <el-option label="审核通过" value="审核通过"></el-option>
                <el-option label="审核不通过" value="审核不通过"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="avatar" label="头像">
              <div>
                <el-upload
                    :auto-upload="false"
                    :on-change="handleAvatarChange"
                    :show-file-list="false"
                    accept="image/*"
                >
                  <el-button type="warning" plain>选择头像</el-button>
                </el-upload>
                <el-image v-if="data.avatarPreview" style="width: 80px; height: 80px; margin-top: 10px; border-radius: 50%;" :src="data.avatarPreview" />
                <el-image v-else-if="data.form.avatarUrl" style="width: 80px; height: 80px; margin-top: 10px; border-radius: 50%;" :src="data.form.avatarUrl" />
                <span v-else style="display: block; margin-top: 10px; color: #999;">无</span>
              </div>
            </el-form-item>
            <el-form-item v-if="data.form.id" prop="photo" label="学生照片">
              <div>
                <div v-if="data.photoLoading" style="width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 6px;">
                  <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
                </div>
                <div v-else-if="data.form.photoUrl">
                  <el-image style="width: 120px; height: 120px; border-radius: 6px;" :src="data.form.photoUrl" :preview-src-list="[data.form.photoUrl]" preview-teleported></el-image>
                  <div style="margin-top: 8px;">
                    <el-button type="danger" plain size="small" @click="deletePhoto">打回照片</el-button>
                  </div>
                </div>
                <span v-else style="display: block; color: #999;">未上传</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
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

import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit, Loading} from "@element-plus/icons-vue";

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  formVisible: false,
  form: {},
  avatarFile: null, // 待上传的头像文件
  avatarPreview: null, // 头像预览URL
  photoLoading: false, // 学生照片加载中
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  ids: []
})

const load = () => {
  request.get('/student/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
      // 加载每个学生的头像
      data.tableData.forEach(student => {
        loadStudentAvatar(student)
      })
    }
  })
}

// 加载学生头像
const loadStudentAvatar = (student) => {
  if (!student.id) return
  request.get('/student/getAvatarUrl/' + student.id).then(res => {
    if (res.code === '200' && res.data) {
      student.avatarUrl = res.data + '?t=' + Date.now()
    }
  })
}

// 加载学生照片
const loadStudentPhoto = () => {
  if (!data.form.id) return
  data.photoLoading = true
  request.get('/student/getPhotoUrl/' + data.form.id).then(res => {
    if (res.code === '200' && res.data) {
      data.form.photoUrl = res.data + '?t=' + Date.now()
    } else {
      data.form.photoUrl = null
    }
    data.photoLoading = false
  }).catch(() => {
    data.photoLoading = false
  })
}

// 删除学生照片（打回）
const deletePhoto = () => {
  ElMessageBox.confirm('确定要打回该学生的照片吗？照片将被删除。', '打回确认', { type: 'warning' }).then(() => {
    request.delete('/student/deletePhoto/' + data.form.id).then(res => {
      if (res.code === '200') {
        ElMessage.success('照片已打回')
        data.form.photoUrl = null
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
  }).catch(() => {})
}

const handleAdd = () => {
  data.form = {}
  data.avatarFile = null
  data.avatarPreview = null
  data.formVisible = true
}
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.avatarFile = null
  data.avatarPreview = null
  data.formVisible = true
  // 加载头像
  if (data.form.id) {
    request.get('/student/getAvatarUrl/' + data.form.id).then(res => {
      if (res.code === '200' && res.data) {
        data.form.avatarUrl = res.data + '?t=' + Date.now()
      }
    })
    // 加载学生照片
    loadStudentPhoto()
  }
}
const add = () => {
  return new Promise((resolve, reject) => {
    request.post('/student/add', data.form).then(res => {
      if (res.code === '200') {
        data.form.id = res.data.id // 获取新创建的id
        resolve()
      } else {
        ElMessage.error(res.msg)
        reject()
      }
    }).catch(reject)
  })
}

const update = () => {
  request.put('/student/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    }
  })
}

// 上传头像
const uploadAvatar = (id) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', data.avatarFile)
    formData.append('id', id)
    
    request.post('/student/uploadAvatar', formData).then(res => {
      if (res.code === '200') {
        resolve()
      } else {
        ElMessage.error(res.msg || '头像上传失败')
        reject()
      }
    }).catch(() => {
      ElMessage.error('头像上传失败')
      reject()
    })
  })
}

const save = async () => {
  if (!data.form.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  
  try {
    if (data.form.id) {
      // 编辑模式：先上传头像（如果有），再更新
      if (data.avatarFile) {
        await uploadAvatar(data.form.id)
      }
      update()
    } else {
      // 新增模式：先添加获取id，再上传头像（如果有）
      await add()
      if (data.avatarFile) {
        await uploadAvatar(data.form.id)
      }
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    }
  } catch (err) {  }
}

const doSave = () => {
  data.form.id ? update() : add()
}

// 选择头像后本地预览
const handleAvatarChange = (file) => {
  data.avatarFile = file.raw
  data.avatarPreview = URL.createObjectURL(file.raw)
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/student/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}
const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据")
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete("/student/delete/batch", {data: data.ids}).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}
const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.id)
}

const reset = () => {
  data.name = null
  load()
}

load()
</script>


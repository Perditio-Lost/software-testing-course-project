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

    <el-dialog title="管理员信息" v-model="data.formVisible" width="40%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="70px" style="padding: 20px">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="data.form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item prop="avatar" label="头像">
          <div>
            <el-upload
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
                accept="image/*"
            >
              <el-button type="warning" plain>选择头像</el-button>
            </el-upload>
            <el-image v-if="data.avatarPreview" style="width: 60px; height: 60px; margin-top: 10px; border-radius: 50%;" :src="data.avatarPreview" />
            <el-image v-else-if="data.form.avatarUrl" style="width: 60px; height: 60px; margin-top: 10px; border-radius: 50%;" :src="data.form.avatarUrl" />
          </div>
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
import {Delete, Edit} from "@element-plus/icons-vue";

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  formVisible: false,
  form: {},
  avatarFile: null, // 待上传的头像文件
  avatarPreview: null, // 头像预览URL
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  ids: []
})

const load = () => {
  request.get('/admin/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
      // 加载每个管理员的头像
      data.tableData.forEach(admin => {
        loadAdminAvatar(admin)
      })
    }
  })
}

// 加载管理员头像
const loadAdminAvatar = (admin) => {
  if (!admin.id) return
  request.get('/admin/getAvatarUrl/' + admin.id).then(res => {
    if (res.code === '200' && res.data) {
      admin.avatarUrl = res.data + '?t=' + Date.now()
    }
  })
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
  // 加载头像
  if (data.form.id) {
    request.get('/admin/getAvatarUrl/' + data.form.id).then(res => {
      if (res.code === '200' && res.data) {
        data.form.avatarUrl = res.data + '?t=' + Date.now()
      }
    })
  }
  data.formVisible = true
}
const add = () => {
  return new Promise((resolve, reject) => {
    request.post('/admin/add', data.form).then(res => {
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
  request.put('/admin/update', data.form).then(res => {
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
    
    request.post('/admin/uploadAvatar', formData).then(res => {
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

// 选择头像后本地预览
const handleFileChange = (file) => {
  data.avatarFile = file.raw
  data.avatarPreview = URL.createObjectURL(file.raw)
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/admin/delete/' + id).then(res => {
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
    request.delete("/admin/delete/batch", {data: data.ids}).then(res => {
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
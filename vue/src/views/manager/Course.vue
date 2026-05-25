<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" prefix-icon="Search" style="width: 240px; margin-right: 10px" placeholder="请输入课程名称查询"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button v-if="data.user.role === 'ADMIN'" type="primary" plain style="margin-left: 10px" @click="handleAdd">新增</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData">
        <el-table-column prop="img" label="课程封面">
          <template v-slot="scope">
            <div v-if="data.imgLoading[scope.row.id]" style="width: 60px; height: 40px; display: flex; align-items: center; justify-content: center;">
              <el-icon class="is-loading" style="font-size: 20px;"><Loading /></el-icon>
            </div>
            <el-image v-else-if="scope.row.img" style="width: 60px; height: 40px; border-radius: 5px; display: block"
                      :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported></el-image>
            <span v-else style="font-size: 12px; color: #999;">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="课程名称" />
        <el-table-column v-if="data.user.role === 'ADMIN'" prop="adminName" label="负责管理员" />
        <el-table-column prop="score" label="课程学分" />
        <el-table-column v-if="data.user.role === 'ADMIN'" label="操作" width="60" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="课程信息" v-model="data.formVisible" width="40%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="70px" style="padding: 20px">
        <el-form-item prop="img" label="课程封面">
          <div>
            <el-upload
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
                accept="image/*"
            >
              <el-button type="warning" plain>选择封面</el-button>
            </el-upload>
            <div v-if="data.imgPreview" style="margin-top: 10px;">
              <img :src="data.imgPreview" style="width: 120px; height: 120px; display: block; border-radius: 6px;" />
            </div>
            <div v-else-if="data.formImgLoading" style="width: 120px; height: 120px; margin-top: 10px; display: flex; align-items: center; justify-content: center; border: 1px dashed #d9d9d9; border-radius: 6px; background: #f5f7fa;">
              <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
            </div>
            <div v-else-if="data.form.img" style="margin-top: 10px;">
              <img :src="data.form.img" style="width: 120px; height: 120px; display: block; border-radius: 6px;" />
            </div>
          </div>
        </el-form-item>
        <el-form-item prop="name" label="课程名称">
          <el-input v-model="data.form.name" placeholder="请输入课程名称"></el-input>
        </el-form-item>
        <el-form-item prop="score" label="课程学分">
          <el-input v-model="data.form.score" placeholder="请输入课程学分"></el-input>
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
import {Delete, Edit, Plus, Loading} from "@element-plus/icons-vue";

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  formVisible: false,
  form: {},
  imgFile: null, // 待上传的封面文件
  imgPreview: null, // 封面预览URL
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  ids: [],
  imgLoading: {},
  formImgLoading: false
})

const load = () => {
  request.get('/course/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
      // 加载每个课程的封面
      data.tableData.forEach(course => {
        loadCourseImgForRow(course)
      })
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 加载课程封面（表格行）
const loadCourseImgForRow = (course) => {
  if (!course.id) return
  data.imgLoading[course.id] = true
  request.get('/course/getImgUrl/' + course.id).then(res => {
    if (res.code === '200') {
      // 添加时间戳防止浏览器缓存
      course.img = res.data ? res.data + '?t=' + Date.now() : null
    }
    data.imgLoading[course.id] = false
  }).catch(() => {
    data.imgLoading[course.id] = false
  })
}

// 加载课程封面（表单）
const loadCourseImg = () => {
  if (!data.form.id) return
  data.formImgLoading = true
  request.get('/course/getImgUrl/' + data.form.id).then(res => {
    if (res.code === '200') {
      // 添加时间戳防止浏览器缓存
      data.form.img = res.data ? res.data + '?t=' + Date.now() : null
    }
    data.formImgLoading = false
  }).catch(() => {
    data.formImgLoading = false
  })
}
const handleAdd = () => {
  data.form = {}
  data.imgFile = null
  data.imgPreview = null
  data.formVisible = true
}
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.imgFile = null
  data.imgPreview = null
  data.formVisible = true
  // 加载封面
  if (data.form.id) {
    loadCourseImg()
  }
}
const add = () => {
  return new Promise((resolve, reject) => {
    request.post('/course/add', data.form).then(res => {
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
  request.put('/course/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    }
  })
}

// 上传封面
const uploadImg = (id) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', data.imgFile)
    formData.append('id', id)
    
    request.post('/course/uploadImg', formData).then(res => {
      if (res.code === '200') {
        resolve()
      } else {
        ElMessage.error(res.msg || '封面上传失败')
        reject()
      }
    }).catch(() => {
      ElMessage.error('封面上传失败')
      reject()
    })
  })
}

const save = async () => {
  if (!data.form.name) {
    ElMessage.warning('请输入课程名称')
    return
  }
  
  try {
    if (data.form.id) {
      // 编辑模式：先上传封面（如果有），再更新
      if (data.imgFile) {
        await uploadImg(data.form.id)
      }
      update()
    } else {
      // 新增模式：先添加获取id，再上传封面（如果有）
      await add()
      if (data.imgFile) {
        await uploadImg(data.form.id)
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

// 选择封面后本地预览
const handleFileChange = (file) => {
  data.imgFile = file.raw
  data.imgPreview = URL.createObjectURL(file.raw)
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/course/delete/' + id).then(res => {
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
    request.delete("/course/delete/batch", {data: data.ids}).then(res => {
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
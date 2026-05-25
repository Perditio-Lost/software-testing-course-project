<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 150px); padding: 20px;">
    <div style="width: 100%; max-width: 600px;" class="card">
      <el-form ref="user" :model="data.user" label-width="70px" style="padding: 20px">
      <el-form-item prop="avatar" label="头像">
        <div>
          <el-upload
              :auto-upload="false"
              :on-change="handleFileChange"
              :show-file-list="false"
              accept="image/*"
          >
            <div v-if="data.avatarLoading" class="avatar" style="display: flex; align-items: center; justify-content: center; background: #f5f7fa; width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 6px;">
              <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
            </div>
            <img v-else-if="data.avatarPreview" :src="data.avatarPreview" class="avatar" style="width: 120px; height: 120px; border-radius: 6px;" />
            <img v-else-if="data.user.avatar" :src="data.user.avatar" class="avatar" style="width: 120px; height: 120px; border-radius: 6px;" />
            <div v-else class="avatar" style="display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f5f7fa; width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 6px; cursor: pointer;">
              <el-icon style="font-size: 28px; color: #8c939d; margin-bottom: 5px;"><Plus /></el-icon>
              <span style="font-size: 12px; color: #999;">未上传</span>
            </div>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item prop="username" label="用户名">
        <el-input disabled v-model="data.user.username" placeholder="请输入用户名"></el-input>
      </el-form-item>
      <el-form-item prop="name" label="姓名">
        <el-input v-model="data.user.name" placeholder="请输入姓名"></el-input>
      </el-form-item>
      <el-form-item prop="phone" label="电话">
        <el-input v-model="data.user.phone" placeholder="请输入电话"></el-input>
      </el-form-item>
      <el-form-item prop="email" label="邮箱">
        <el-input v-model="data.user.email" placeholder="请输入邮箱"></el-input>
      </el-form-item>
      <div style="text-align: center">
        <el-button type="primary" plain @click="update">保 存</el-button>
      </div>
    </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import {Plus, Loading} from '@element-plus/icons-vue';

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  avatarFile: null, // 待上传的头像文件
  avatarPreview: null, // 头像预览URL
  avatarLoading: false
})

// 加载头像
const loadAvatar = () => {
  if (!data.user.id) return
  
  let apiUrl = ''
  if (data.user.role === 'ADMIN') {
    apiUrl = '/admin/getAvatarUrl/' + data.user.id
  } else if (data.user.role === 'TEACHER') {
    apiUrl = '/teacher/getAvatarUrl/' + data.user.id
  } else if (data.user.role === 'STUDENT') {
    apiUrl = '/student/getAvatarUrl/' + data.user.id
  } else {
    return
  }
  
  data.avatarLoading = true
  request.get(apiUrl).then(res => {
    if (res.code === '200') {
      // 添加时间戳防止浏览器缓存
      data.user.avatar = res.data ? res.data + '?t=' + Date.now() : null
    }
    data.avatarLoading = false
  }).catch(() => {
    data.avatarLoading = false
  })
}

// 初始化加载头像
onMounted(() => {
  loadAvatar()
})

// 选择头像后本地预览
const handleFileChange = (file) => {
  data.avatarFile = file.raw
  data.avatarPreview = URL.createObjectURL(file.raw)
}

// 上传头像
const uploadAvatar = () => {
  return new Promise((resolve, reject) => {
    let uploadUrl = ''
    if (data.user.role === 'ADMIN') {
      uploadUrl = '/admin/uploadAvatar'
    } else if (data.user.role === 'TEACHER') {
      uploadUrl = '/teacher/uploadAvatar'
    } else if (data.user.role === 'STUDENT') {
      uploadUrl = '/student/uploadAvatar'
    } else {
      resolve()
      return
    }
    
    const formData = new FormData()
    formData.append('file', data.avatarFile)
    formData.append('id', data.user.id)
    
    request.post(uploadUrl, formData).then(res => {
      if (res.code === '200') {
        resolve()
      } else {
        reject(res.msg || '头像上传失败')
      }
    }).catch(() => {
      reject('头像上传失败')
    })
  })
}

const emit = defineEmits(['updateUser'])
const update = async () => {
  // 如果有新选择的头像，先上传
  if (data.avatarFile) {
    try {
      await uploadAvatar()
    } catch (err) {
      ElMessage.error(err)
      return
    }
  }
  
  // 保存用户信息
  if (data.user.role === 'ADMIN') {
    request.put('/admin/update', data.user).then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        localStorage.setItem('xm-user', JSON.stringify(data.user))
        emit('updateUser')
        // 重置状态
        data.avatarFile = null
        data.avatarPreview = null
        loadAvatar()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
  if (data.user.role === 'TEACHER') {
    request.put('/teacher/update', data.user).then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        localStorage.setItem('xm-user', JSON.stringify(data.user))
        emit('updateUser')
        // 重置状态
        data.avatarFile = null
        data.avatarPreview = null
        loadAvatar()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
}
</script>

<style>
.avatar-uploader {
  height: 120px;
}
.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  display: block;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
}
</style>
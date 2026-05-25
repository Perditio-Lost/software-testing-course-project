<template>
  <div style="width: 40%; margin: 5px auto" class="card">
    <div style="text-align: right; padding: 0 20px">
      <el-button type="warning" plain @click="updateInit">修改密码</el-button>
    </div>
    <el-form ref="user" :model="data.user" label-width="60px" style="padding: 20px">
      <div style="text-align: center; margin-bottom: 20px">
        <div style="display: inline-block; margin-right: 40px; vertical-align: top;">
          <div style="margin-bottom: 10px; font-weight: bold; color: #666">头像</div>
          <el-upload
              :auto-upload="false"
              :on-change="handleAvatarChange"
              :show-file-list="false"
              accept="image/*"
              class="avatar-uploader"
          >
            <div v-if="data.avatarLoading" class="avatar" style="display: flex; align-items: center; justify-content: center; background: #f5f7fa;">
              <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
            </div>
            <img v-else-if="data.avatarPreview" :src="data.avatarPreview" class="avatar" />
            <img v-else-if="data.user.avatar" :src="data.user.avatar" class="avatar" />
            <div v-else class="avatar" style="display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f5f7fa;">
              <el-icon style="font-size: 28px; color: #8c939d; margin-bottom: 5px;"><Plus /></el-icon>
              <span style="font-size: 12px; color: #999;">未上传</span>
            </div>
          </el-upload>
        </div>
        <div style="display: inline-block; vertical-align: top;">
          <div style="margin-bottom: 10px; font-weight: bold; color: #666">学生照片（用于人脸识别）</div>
          <el-upload
              :auto-upload="false"
              :on-change="handlePhotoChange"
              :show-file-list="false"
              accept="image/*"
              class="avatar-uploader"
          >
            <div v-if="data.photoLoading" class="avatar" style="display: flex; align-items: center; justify-content: center; background: #f5f7fa;">
              <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
            </div>
            <img v-else-if="data.photoPreview" :src="data.photoPreview" class="avatar" />
            <img v-else-if="data.user.photo" :src="data.user.photo" class="avatar" />
            <div v-else class="avatar" style="display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f5f7fa;">
              <el-icon style="font-size: 28px; color: #8c939d; margin-bottom: 5px;"><Plus /></el-icon>
              <span style="font-size: 12px; color: #999;">未上传</span>
            </div>
          </el-upload>
        </div>
      </div>
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

    <el-dialog title="修改密码" v-model="data.formVisible" width="40%" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.user" label-width="70px" style="padding: 20px">
        <el-form-item label="原密码" prop="password">
          <el-input v-model="data.user.password" placeholder="请输入原密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="data.user.newPassword" placeholder="请输入新密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="data.user.confirmPassword" placeholder="请确认新密码" show-password></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button plain @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" plain @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import {Plus, Loading} from '@element-plus/icons-vue';
import router from "@/router/index.js";
const formRef = ref()
const baseUrl = import.meta.env.VITE_BASE_URL
const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else {
    if (value !== data.user.newPassword) {
      callback(new Error("确认密码跟新密码不一致!"))
    }
    callback()
  }
}

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  formVisible: false,
  avatarFile: null, // 待上传的头像文件
  avatarPreview: null, // 头像预览URL
  photoFile: null, // 待上传的照片文件
  photoPreview: null, // 照片预览URL
  avatarLoading: true,
  photoLoading: true,
  rules: {
    password: [
      {required: true, message: '请输入原密码', trigger: 'blur'},
    ],
    newPassword: [
      {required: true, message: '请输入新密码', trigger: 'blur'},
    ],
    confirmPassword: [
      {validator: validatePass, trigger: 'blur'}
    ]
  }
})


// 页面加载时查询学生头像
const loadStudentAvatar = () => {
  if (data.user.id) {
    data.avatarLoading = true
    request.get('/student/getAvatarUrl/' + data.user.id).then(res => {
      if (res.code === '200') {
        // 添加时间戳防止浏览器缓存
        data.user.avatar = res.data ? res.data + '?t=' + Date.now() : null
      }
      data.avatarLoading = false
    }).catch(() => {
      data.avatarLoading = false
    })
  }
}

// 页面加载时查询学生照片
const loadStudentPhoto = () => {
  if (data.user.id) {
    data.photoLoading = true
    request.get('/student/getPhotoUrl/' + data.user.id).then(res => {
      if (res.code === '200') {
        // 添加时间戳防止浏览器缓存
        data.user.photo = res.data ? res.data + '?t=' + Date.now() : null
      }
      data.photoLoading = false
    }).catch(() => {
      data.photoLoading = false
    })
  }
}

// 初始化加载头像和照片
loadStudentAvatar()
loadStudentPhoto()

// 选择头像后本地预览
const handleAvatarChange = (file) => {
  data.avatarFile = file.raw
  data.avatarPreview = URL.createObjectURL(file.raw)
}

// 选择照片后本地预览
const handlePhotoChange = (file) => {
  data.photoFile = file.raw
  data.photoPreview = URL.createObjectURL(file.raw)
}


// 上传头像
const uploadAvatar = () => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', data.avatarFile)
    formData.append('id', data.user.id)
    
    request.post('/student/uploadAvatar', formData).then(res => {
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

// 上传照片
const uploadPhoto = () => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', data.photoFile)
    formData.append('id', data.user.id)
    
    request.post('/student/uploadPhoto', formData).then(res => {
      if (res.code === '200') {
        resolve()
      } else {
        reject(res.msg || '照片上传失败')
      }
    }).catch(() => {
      reject('照片上传失败')
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
  
  // 如果有新选择的照片，先上传
  if (data.photoFile) {
    try {
      await uploadPhoto()
    } catch (err) {
      ElMessage.error(err)
      return
    }
  }
  
  // 保存用户信息
  if (data.user.role === 'STUDENT') {
    request.put('/student/update', data.user).then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        localStorage.setItem('xm-user', JSON.stringify(data.user))
        emit('updateUser')
        // 重置状态
        data.avatarFile = null
        data.avatarPreview = null
        data.photoFile = null
        data.photoPreview = null
        loadStudentAvatar()
        loadStudentPhoto()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
}
const updateInit = () => {
  data.formVisible = true
}
const save = () => {
  formRef.value.validate(valid => {
    if (valid) {
      request.put('/updatePassword', data.user).then(res => {
        if (res.code === '200') {
          ElMessage.success('保存成功')
          localStorage.removeItem('xm-user')
          router.push('/login')
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
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
  border-radius: 50%;
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
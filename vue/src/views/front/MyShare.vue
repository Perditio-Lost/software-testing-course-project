<template>
  <div style="width: 60%; margin: 30px auto">
    <div style="display: flex; align-items: center">
      <div style="flex: 1; font-size: 17px; font-weight: bold">我分享的学习经验（{{ data.tableData.length }}）</div>
      <div style="width: 120px"><el-button type="success" plain @click="initAdd">发布学习经验</el-button></div>
    </div>

    <div style="margin: 20px 0">
      <el-table stripe :data="data.tableData">
        <el-table-column prop="img" label="帖子主图" width="100">
          <template v-slot="scope">
            <el-image style="width: 60px; height: 40px; border-radius: 5px; display: block" v-if="scope.row.imgUrl"
                      :src="scope.row.imgUrl" :preview-src-list="[scope.row.imgUrl]" preview-teleported></el-image>
            <span v-else style="color: #999; font-size: 12px;">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="帖子标题" show-overflow-tooltip/>
        <el-table-column prop="content" label="分享内容" width="120">
          <template v-slot="scope">
            <el-button type="info" plain @click="viewContent(scope.row.content)">点击预览</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="发布时间" width="180"/>
        <el-table-column label="操作" width="200" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" plain @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" plain @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="帖子信息" v-model="data.formVisible" width="60%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="70px" style="padding: 20px">
        <el-form-item prop="title" label="帖子标题">
          <el-input v-model="data.form.title" placeholder="请输入帖子标题"></el-input>
        </el-form-item>
        <el-form-item prop="img" label="帖子封面">
          <div>
            <el-upload
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
                accept="image/*"
            >
              <el-button type="primary" plain>选择封面</el-button>
            </el-upload>
            <el-image v-if="data.imgPreview" style="width: 100px; height: 60px; margin-top: 10px; border-radius: 5px;" :src="data.imgPreview" />
            <el-image v-else-if="data.form.imgUrl" style="width: 100px; height: 60px; margin-top: 10px; border-radius: 5px;" :src="data.form.imgUrl" />
          </div>
        </el-form-item>
        <el-form-item prop="content" label="帖子内容">
          <div style="border: 1px solid #ccc; width: 100%">
            <Toolbar
                style="border-bottom: 1px solid #ccc"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
            />
            <Editor
                style="height: 500px; overflow-y: hidden;"
                v-model="data.form.content"
                :mode="mode"
                :defaultConfig="editorConfig"
                @onCreated="handleCreated"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="danger" plain @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" plain @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
    <el-dialog title="帖子信息" v-model="data.viewVisible" width="60%" destroy-on-close>
      <div class="article-content" style="padding: 0 10px" v-html="data.viewContent"></div>
    </el-dialog>
  </div>
</template>
<script setup>
import {onBeforeUnmount, reactive, ref, shallowRef, nextTick} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import '@wangeditor/editor/dist/css/style.css' // 引入 css
import 'katex/dist/katex.min.css' // 引入公式样式
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@/utils/wangEditorConfig.js' // 引入公式插件配置
import katex from 'katex' // 引入KaTeX渲染库

const baseUrl = import.meta.env.VITE_BASE_URL
const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  formVisible: false,
  form: {},
  imgFile: null, // 待上传的图片文件
  imgPreview: null, // 图片预览URL
  tableData: [],
  pageNum: 1,
  pageSize: 5,
  total: 0,
  viewVisible: false,
  viewContent: null,
})

/* wangEditor5 初始化开始 */
const editorRef = shallowRef()  // 编辑器实例，必须用 shallowRef
const mode = 'default'
const toolbarConfig = {
  // 自定义工具栏，排除表情、图片、视频、链接
  excludeKeys: [
    'emotion',           // 排除表情
    'insertImage',       // 排除插入图片
    'uploadImage',       // 排除上传图片
    'deleteImage',       // 排除删除图片
    'editImage',         // 排除编辑图片
    'viewImageLink',     // 排除查看图片链接
    'imageWidth30',      // 排除图片宽度30%
    'imageWidth50',      // 排除图片宽度50%
    'imageWidth100',     // 排除图片宽度100%
    'insertVideo',       // 排除插入视频
    'uploadVideo',       // 排除上传视频
    'deleteVideo',       // 排除删除视频
    'insertLink',        // 排除插入链接
    'editLink',          // 排除编辑链接
    'unLink',            // 排除取消链接
    'viewLink',          // 排除查看链接
    'group-image',       // 排除图片组
    'group-video'        // 排除视频组
  ],
  // 插入公式按钮的配置
  insertKeys: {
    index: 23, // 插入位置，在代码块后面
    keys: ['insertFormula'] // 公式按钮的 key
  }
}
const editorConfig = { 
  MENU_CONF: {},
  placeholder: '请输入内容，支持表格、代码块、列表、标题等格式。点击工具栏的"公式"按钮可插入数学公式(LaTeX格式)。'
}

// 组件销毁时，也及时销毁编辑器，否则可能会造成内存泄漏
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})
// 记录 editor 实例，重要！
const handleCreated = (editor) => {
  editorRef.value = editor
}
/* wangEditor5 初始化结束 */

// 渲染公式
const renderFormulas = () => {
  nextTick(() => {
    const formulas = document.querySelectorAll('[data-w-e-type="formula"]')
    formulas.forEach(elem => {
      const formula = elem.getAttribute('data-value')
      if (formula) {
        try {
          katex.render(formula, elem, {
            throwOnError: false,
            displayMode: !elem.hasAttribute('data-w-e-is-inline')
          })
        } catch (e) {        }
      }
    })
  })
}

const viewContent = (content) => {
  data.viewContent = content
  data.viewVisible = true
  renderFormulas() // 预览时渲染公式
}
const load = () => {
  request.get('/article/selectPage', {
    params: {
      studentId: data.user.id,
      pageNum: data.pageNum,
      pageSize: data.pageSize
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data.list
      data.total = res.data.total
      // 加载每个帖子的封面
      data.tableData.forEach(item => {
        loadArticleImg(item)
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

const initAdd = () => {
  data.form = {}
  data.imgFile = null
  data.imgPreview = null
  data.formVisible = true
}
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.imgFile = null
  data.imgPreview = null
  // 加载表单中的封面
  if (data.form.id) {
    request.get('/article/getImgUrl/' + data.form.id).then(res => {
      if (res.code === '200' && res.data) {
        data.form.imgUrl = res.data + '?t=' + Date.now()
      }
    })
  }
  data.formVisible = true
}
const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/article/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {  })
}

const add = () => {
  return new Promise((resolve, reject) => {
    request.post('/article/add', data.form).then(res => {
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
  request.put('/article/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 上传封面
const uploadImg = (id) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', data.imgFile)
    formData.append('id', id)
    
    request.post('/article/uploadImg', formData).then(res => {
      if (res.code === '200') {
        data.form.imgUrl = res.data
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
  if (!data.form.title) {
    ElMessage.warning('请输入帖子标题')
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

// 选择图片后本地预览
const handleFileChange = (file) => {
  data.imgFile = file.raw
  // 创建本地预览URL
  data.imgPreview = URL.createObjectURL(file.raw)
}

load()
</script>

<style scoped>
/* 富文本内容样式 */
.article-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 15px 0;
}

.article-content :deep(table td),
.article-content :deep(table th) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.article-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: bold;
}

.article-content :deep(table tr:hover) {
  background-color: #fafafa;
}

.article-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 5px;
  overflow-x: auto;
  margin: 15px 0;
}

.article-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.article-content :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 15px;
  margin: 15px 0;
  color: #666;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  padding-left: 30px;
  margin: 10px 0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3),
.article-content :deep(h4),
.article-content :deep(h5),
.article-content :deep(h6) {
  margin: 20px 0 10px 0;
  font-weight: bold;
}

.article-content :deep(p) {
  margin: 10px 0;
  line-height: 1.8;
}

.article-content :deep(hr) {
  margin: 20px 0;
  border: none;
  border-top: 1px solid #ddd;
}
</style>
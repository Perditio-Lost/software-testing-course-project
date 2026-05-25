<template>
  <div style="border: 1px solid #ccc; width: 100%">
    <Toolbar
        style="border-bottom: 1px solid #ccc"
        :editor="editorRef"
        :defaultConfig="toolbarConfig"
        :mode="mode"
    />
    <Editor
        :style="{ height: height, overflowY: 'hidden' }"
        v-model="content"
        :mode="mode"
        :defaultConfig="editorConfig"
        @onCreated="handleCreated"
        @onChange="handleChange"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, shallowRef, computed, watch } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import 'katex/dist/katex.min.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@/utils/wangEditorConfig.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '300px'
  },
  placeholder: {
    type: String,
    default: '请输入内容...'
  },
  // 是否隐藏列表功能（无序列表、有序列表、待办）
  hideLists: {
    type: Boolean,
    default: false
  },
  // 是否禁止粘贴
  noPaste: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const mode = 'default'

// 内部内容状态
const content = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 工具栏配置
const toolbarConfig = computed(() => {
  const excludeKeys = [
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
  ]
  
  // 如果需要隐藏列表功能
  if (props.hideLists) {
    excludeKeys.push(
      'bulletedList',    // 排除无序列表
      'numberedList',    // 排除有序列表
      'todo'             // 排除待办
    )
  }
  
  return {
    excludeKeys,
    insertKeys: {
      index: 23,
      keys: ['insertFormula']
    }
  }
})

const editorConfig = computed(() => ({
  MENU_CONF: {},
  placeholder: props.placeholder,
  // 如果启用禁止粘贴，自定义粘贴处理
  customPaste: props.noPaste ? (editor, event) => {
    event.preventDefault()
    return false
  } : undefined
}))

const handleCreated = (editor) => {
  editorRef.value = editor
  
  // 如果启用了禁止粘贴功能，额外添加DOM事件监听作为双保险
  if (props.noPaste) {
    const editorDom = editor.getEditableContainer()
    if (editorDom) {
      const preventPaste = (e) => {
        e.preventDefault()
        e.stopPropagation()
        return false
      }
      editorDom.addEventListener('paste', preventPaste, true)
      editorDom.addEventListener('drop', preventPaste, true)
    }
  }
}

const handleChange = () => {
  // 内容变化时自动触发
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})
</script>

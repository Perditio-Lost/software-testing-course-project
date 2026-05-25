<template>
  <span class="rich-text-content" v-html="content" ref="containerRef"></span>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { renderFormulas } from '@/utils/renderFormula.js'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const containerRef = ref(null)

// 渲染公式
const render = () => {
  if (containerRef.value) {
    renderFormulas(containerRef.value)
  }
}

// 监听内容变化
watch(() => props.content, () => {
  render()
}, { immediate: true })

onMounted(() => {
  render()
})
</script>

<style scoped>
.rich-text-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 15px 0;
}

.rich-text-content :deep(table td),
.rich-text-content :deep(table th) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.rich-text-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: bold;
}

.rich-text-content :deep(table tr:hover) {
  background-color: #fafafa;
}

.rich-text-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 5px;
  overflow-x: auto;
  margin: 15px 0;
}

.rich-text-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.rich-text-content :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 15px;
  margin: 15px 0;
  color: #666;
}

.rich-text-content :deep(ul),
.rich-text-content :deep(ol) {
  padding-left: 30px;
  margin: 10px 0;
}

.rich-text-content :deep(h1),
.rich-text-content :deep(h2),
.rich-text-content :deep(h3),
.rich-text-content :deep(h4),
.rich-text-content :deep(h5),
.rich-text-content :deep(h6) {
  margin: 20px 0 10px 0;
  font-weight: bold;
}

.rich-text-content :deep(p) {
  margin: 10px 0;
  line-height: 1.8;
}

.rich-text-content :deep(hr) {
  margin: 20px 0;
  border: none;
  border-top: 1px solid #ddd;
}
</style>

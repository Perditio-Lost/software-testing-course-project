<template>
  <div style="width: 60%; margin: 30px auto">
    <div style="font-size: 20px; font-weight: bold; text-align: center">{{ data.articleData.title }}</div>
    <div style="margin-top: 15px; color: #666666; text-align: center">
      <span>发布人：{{ data.articleData.studentName }}</span>
      <span style="margin-left: 30px">发布时间：{{ data.articleData.time }}</span>
    </div>
    <div class="article-content" style="margin-top: 50px" v-html="data.articleData.content"></div>
  </div>
</template>
<script setup>
import {reactive, onMounted, nextTick} from "vue";
import request from "@/utils/request.js";
import router from "@/router/index.js";
import {ElMessage} from "element-plus";
import 'katex/dist/katex.min.css' // 引入公式样式
import katex from 'katex' // 引入KaTeX渲染库

const data = reactive({
  articleId: router.currentRoute.value.query.id,
  articleData: {}
})

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

const loadArticle = () => {
  data.articleId = router.currentRoute.value.query.id
  request.get('/article/selectById/' + data.articleId).then(res => {
    if (res.code === '200') {
      data.articleData = res.data
      renderFormulas() // 加载后渲染公式
    } else {
      ElMessage.error(res.msg)
    }
  })
}
onMounted(() => {
  loadArticle()
})
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
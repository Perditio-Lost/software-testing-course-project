<template>
  <view class="paper-detail-container">
    <!-- 顶部信息 -->
    <view class="header-info">
      <text class="paper-title">{{ (paperData.test && paperData.test.name) || '-' }}</text>
      <view class="course-info">
        <text class="info-text">课程：{{ (paperData.test && paperData.test.courseName) || '-' }}</text>
        <text class="info-text">教师：{{ (paperData.test && paperData.test.teacherName) || '-' }}</text>
      </view>
      <view class="score-display">
        <text class="score-label">得分：</text>
        <text class="score-value">{{ paperData.score || 0 }}</text>
        <text class="score-total"> / {{ totalScore }}</text>
      </view>
    </view>

    <!-- 题目列表 -->
    <view class="question-list">
      <view 
        v-for="(item, index) in paperData.questions" 
        :key="index"
        v-show="item.paperQuestionType !== 'son'"
        class="question-card"
      >
        <!-- 题目头部 -->
        <view class="question-header">
          <view :class="['question-type-tag', 'type-' + getTypeClass(item.questionTypeName)]">
            {{ item.questionTypeName }}
          </view>
          <text class="question-number">第 {{ index + 1 }} 题</text>
          <view v-if="getScoreStatus(item)" :class="['score-badge', getScoreStatus(item).class]">
            {{ getScoreStatus(item).text }}
          </view>
        </view>

        <!-- 题干 -->
        <view class="question-stem">
          <rich-text :nodes="item.questionStem"></rich-text>
        </view>

        <!-- 复合题 - 子题目 -->
        <view v-if="item.questionTypeVariety === 'composite' && item.questionList" class="sub-questions">
          <view 
            v-for="(subItem, subIndex) in item.questionList" 
            :key="subIndex"
            class="sub-question-card"
          >
            <view class="sub-question-header">
              <text class="sub-question-number">{{ subIndex + 1 }})</text>
              <view :class="['sub-question-type', 'type-' + getTypeClass(subItem.questionTypeName)]">
                {{ subItem.questionTypeName }}
              </view>
              <view v-if="getScoreStatus(subItem)" :class="['score-badge-small', getScoreStatus(subItem).class]">
                {{ getScoreStatus(subItem).text }}
              </view>
            </view>

            <view class="sub-question-stem">
              <rich-text :nodes="subItem.questionStem"></rich-text>
            </view>

            <!-- 子题目选项 -->
            <view class="options-area">
              <!-- 单选/多选/判断 -->
              <view v-if="subItem.questionTypeVariety === 'choice'" class="choice-options">
                <view 
                  v-for="c in subItem.choiceList" 
                  :key="c.id"
                  :class="['option-item', {
                    'selected': isSelected(subItem, c.identification),
                    'correct': c.flag
                  }]"
                >
                  <text class="option-label">{{ c.identification }}.</text>
                  <rich-text :nodes="c.content" class="option-content"></rich-text>
                </view>
              </view>

              <!-- 填空/文本题答案 -->
              <view v-else class="text-answer">
                <view class="answer-label">学生答案：</view>
                <view class="answer-content">
                  <rich-text v-if="subItem.newAnswer" :nodes="subItem.newAnswer"></rich-text>
                  <text v-else class="empty-answer">（未作答）</text>
                </view>
              </view>
            </view>

            <!-- 子题目答案解析 -->
            <view class="answer-section">
              <view v-if="subItem.questionTypeVariety === 'choice'" class="correct-answer">
                <text class="answer-title">✓ 正确答案：</text>
                <text class="answer-text">{{ getCorrectAnswers(subItem) }}</text>
              </view>
              <view v-if="subItem.answer" class="answer-analysis">
                <text class="analysis-title">📖 解析：</text>
                <text class="analysis-text">{{ subItem.answer }}</text>
              </view>
              <view v-if="subItem.answerScore !== undefined" class="sub-score">
                <text>得分：</text>
                <text :class="['score', subItem.answerScore >= subItem.typeScore ? 'full' : 'partial']">
                  {{ subItem.answerScore }}
                </text>
                <text> / {{ subItem.typeScore }} 分</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 非复合题 - 选项 -->
        <view v-if="item.questionTypeVariety !== 'composite'" class="options-area">
          <!-- 单选/多选/判断 -->
          <view v-if="item.questionTypeVariety === 'choice'" class="choice-options">
            <view 
              v-for="c in item.choiceList" 
              :key="c.id"
              :class="['option-item', {
                'selected': isSelected(item, c.identification),
                'correct': c.flag
              }]"
            >
              <text class="option-label">{{ c.identification }}.</text>
              <rich-text :nodes="c.content" class="option-content"></rich-text>
            </view>
          </view>

          <!-- 填空/文本题答案 -->
          <view v-else class="text-answer">
            <view class="answer-label">学生答案：</view>
            <view class="answer-content">
              <rich-text v-if="item.newAnswer" :nodes="item.newAnswer"></rich-text>
              <text v-else class="empty-answer">（未作答）</text>
            </view>
          </view>
        </view>

        <!-- 非复合题 - 答案解析 -->
        <view v-if="item.questionTypeName !== '复合'" class="answer-section">
          <view v-if="['单选', '多选', '判断'].includes(item.questionTypeName)" class="correct-answer">
            <text class="answer-title">✓ 正确答案：</text>
            <text class="answer-text">{{ getCorrectAnswers(item) }}</text>
          </view>
          <view v-if="item.answer" class="answer-analysis">
            <text class="analysis-title">📖 解析：</text>
            <text class="analysis-text">{{ item.answer }}</text>
          </view>
          <view v-if="item.answerScore !== undefined" class="score-info">
            <text>得分：</text>
            <text :class="['score', item.answerScore >= item.typeScore ? 'full' : 'partial']">
              {{ item.answerScore }}
            </text>
            <text> / {{ item.typeScore }} 分</text>
          </view>
        </view>
      </view>
    </view>

    <!-- AI分析区域 -->
    <view class="ai-analysis-section">
      <view class="ai-card">
        <view class="ai-header">
          <text class="ai-title">📊 AI错题分析与学习建议</text>
          <button 
            class="ai-btn" 
            @click="getAIAnalysis"
            :loading="aiLoading"
            :disabled="wrongCount === 0 && halfCorrect === 0"
          >
            {{ aiAnalysis ? '重新分析' : '获取分析' }}
          </button>
        </view>
        
        <view class="ai-content">
          <!-- 加载中状态 -->
          <view v-if="aiLoading" class="ai-loading">
            <view class="loading-spinner"></view>
            <text class="loading-text">AI 正在分析中...</text>
          </view>
          
          <!-- AI 分析结果 -->
          <view v-else-if="aiAnalysis" class="ai-result">
            <rich-text :nodes="aiAnalysisHtml"></rich-text>
          </view>
          
          <!-- 全对提示 -->
          <view v-else-if="wrongCount === 0 && halfCorrect === 0" class="ai-empty success">
            <text class="emoji">🎉</text>
            <text class="message">恭喜！本次考试全部正确，无需AI分析。</text>
          </view>
          
          <!-- 默认提示 -->
          <view v-else class="ai-empty">
            <text class="emoji">💡</text>
            <text class="message">点击上方按钮获取AI智能分析和学习建议</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      scoreId: '',
      paperData: {
        test: {},
        questions: []
      },
      totalScore: 0,
      fullCorrect: 0,
      halfCorrect: 0,
      wrongCount: 0,
      aiAnalysis: '',
      aiAnalysisHtml: '',
      aiLoading: false
    }
  },
  onLoad(options) {
    this.scoreId = options.id
    this.loadPaperDetail()
  },
  methods: {
    async loadPaperDetail() {
      try {
        uni.showLoading({
          title: '加载中...'
        })
        
        const res = await request({
          url: `/score/selectById/${this.scoreId}`,
          method: 'GET'
        })
        
        this.paperData = res.data
        this.calcTotalScore()
        
        uni.hideLoading()
      } catch (err) {
        uni.hideLoading()
        console.error('加载试卷详情失败', err)
      }
    },
    
    getTypeClass(typeName) {
      const classMap = {
        '单选': 'single',
        '多选': 'multiple',
        '判断': 'judge',
        '填空': 'blank',
        '复合': 'composite',
        '名词解析': 'noun',
        '论述': 'essay',
        '计算': 'calc',
        '程序': 'program'
      }
      return classMap[typeName] || 'default'
    },
    
    calcTotalScore() {
      let total = 0
      let full = 0, half = 0, wrong = 0
      
      this.paperData.questions.forEach(q => {
        if (q.paperQuestionType !== 'son') {
          total += q.typeScore || 0
          
          const answerScore = q.answerScore || 0
          const typeScore = q.typeScore || 0
          
          if (typeScore > 0) {
            if (answerScore >= typeScore) {
              full++
            } else if (answerScore > 0) {
              half++
            } else {
              wrong++
            }
          }
        }
      })
      
      this.totalScore = total
      this.fullCorrect = full
      this.halfCorrect = half
      this.wrongCount = wrong
    },
    
    async getAIAnalysis() {
      this.aiLoading = true
      try {
        const res = await request({
          url: `/score/aiAnalysis/${this.scoreId}`,
          method: 'GET'
        })
        
        this.aiAnalysis = res.data
        // 简单的 Markdown 转 HTML（小程序环境下）
        this.aiAnalysisHtml = this.convertMarkdownToHtml(res.data)
        
        uni.showToast({
          title: 'AI分析完成',
          icon: 'success'
        })
      } catch (err) {
        console.error('AI分析失败', err)
      } finally {
        this.aiLoading = false
      }
    },
    
    convertMarkdownToHtml(markdown) {
      if (!markdown) return ''
      
      let html = markdown
      // 删除多余换行
      html = html.replace(/\n+/g, '\n')
      // 标题转换
      html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
      html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
      html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
      // 粗体
      html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      // 列表
      html = html.replace(/^\* (.*$)/gim, '<li>$1</li>')
      html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
      // 段落
      html = html.split('\n').map(line => {
        if (line && !line.startsWith('<')) {
          return `<p>${line}</p>`
        }
        return line
      }).join('')
      // 删除空段落
      html = html.replace(/<p>\s*<\/p>/g, '')
      
      return html
    },
    
    getTypeStyle(typeName) {
      const styles = {
        '单选': { backgroundColor: '#e1f3d8', color: '#67c23a' },
        '多选': { backgroundColor: '#fef0f0', color: '#f56c6c' },
        '判断': { backgroundColor: '#fdf6ec', color: '#e6a23c' },
        '填空': { backgroundColor: '#ecf5ff', color: '#409eff' },
        '复合': { backgroundColor: '#f4f4f5', color: '#909399' },
        '名词解析': { backgroundColor: '#f0f9ff', color: '#67c23a' },
        '论述': { backgroundColor: '#fff7e6', color: '#fa8c16' },
        '计算': { backgroundColor: '#e6f7ff', color: '#1890ff' },
        '程序': { backgroundColor: '#f9f0ff', color: '#722ed1' }
      }
      return styles[typeName] || { backgroundColor: '#f4f4f5', color: '#909399' }
    },
    
    getScoreStatus(item) {
      if (item.answerScore === undefined || item.answerScore === null) return null
      const score = item.answerScore
      const fullScore = item.typeScore || 0
      
      if (score >= fullScore) {
        return { text: '全对', class: 'correct' }
      } else if (score > 0) {
        return { text: '半对', class: 'partial' }
      } else {
        return { text: '错误', class: 'wrong' }
      }
    },
    
    isSelected(question, identification) {
      if (question.questionTypeName === '多选') {
        return question.checkList && question.checkList.includes(identification)
      } else {
        return question.newAnswer === identification
      }
    },
    
    getCorrectAnswers(item) {
      if (!item.choiceList) return item.answer || '未设置'
      const correctChoices = item.choiceList.filter(c => c.flag === true || c.flag === 1)
      return correctChoices.map(c => c.identification).join(', ') || item.answer || '未设置'
    }
  }
}
</script>

<style scoped>
.paper-detail-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}

.header-info {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  padding: 40rpx 30rpx;
  color: white;
}

.paper-title {
  font-size: 36rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 20rpx;
}

.course-info {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-bottom: 20rpx;
  font-size: 26rpx;
  opacity: 0.95;
}

.score-display {
  display: flex;
  align-items: baseline;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.3);
}

.score-label {
  font-size: 28rpx;
}

.score-value {
  font-size: 48rpx;
  font-weight: bold;
  margin: 0 8rpx;
}

.score-total {
  font-size: 28rpx;
  opacity: 0.9;
}

.question-list {
  padding: 20rpx;
}

.question-card {
  background-color: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.question-type-tag {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.question-type-tag.type-single {
  background-color: #e1f3d8;
  color: #67c23a;
}

.question-type-tag.type-multiple {
  background-color: #fef0f0;
  color: #f56c6c;
}

.question-type-tag.type-judge {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.question-type-tag.type-blank {
  background-color: #ecf5ff;
  color: #409eff;
}

.question-type-tag.type-composite {
  background-color: #f4f4f5;
  color: #909399;
}

.question-type-tag.type-noun {
  background-color: #f0f9ff;
  color: #67c23a;
}

.question-type-tag.type-essay {
  background-color: #fff7e6;
  color: #fa8c16;
}

.question-number {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
  flex: 1;
}

.score-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.score-badge.correct {
  background-color: #f0f9ff;
  color: #67c23a;
}

.score-badge.partial {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.score-badge.wrong {
  background-color: #fef0f0;
  color: #f56c6c;
}

.question-stem {
  font-size: 28rpx;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 20rpx;
  padding: 20rpx;
  background-color: #f0f9ff;
  border-left: 6rpx solid #409eff;
  border-radius: 8rpx;
}

.sub-questions {
  margin-top: 20rpx;
}

.sub-question-card {
  background-color: #fafafa;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.sub-question-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.sub-question-number {
  font-size: 26rpx;
  font-weight: bold;
  color: #409eff;
}

.sub-question-type {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.sub-question-type.type-single {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  margin-left: auto;
}

.score-badge-small.correct {
  background-color: #f0f9ff;
  color: #67c23a;
}

.score-badge-small.partial {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.score-badge-small.wrong {
  background-color: #fef0f0;
  color: #f56c6c;
}

.sub-question-stem {
  font-size: 26rpx;
  line-height: 1.7;
  color: #303133;
  margin-bottom: 16rpx;
}

.options-area {
  margin: 20rpx 0;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 20rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.option-item.selected {
  background-color: #e1f3d8;
  border-color: #67c23a;
}

.option-item.correct {
  background-color: #f0f9ff;
  border-color: #409eff;
  font-weight: 500;
}

.option-label {
  font-size: 26rpx;
  color: #606266;
  margin-right: 12rpx;
  font-weight: 500;
}

.option-content {
  flex: 1;
  font-size: 26rpx;
  color: #303133;
  line-height: 1.6;
}

.text-answer {
  padding: 20rpx;
  background-color: #fafafa;
  border-radius: 12rpx;
}

.answer-label {
  font-size: 26rpx;
  color: #606266;
  font-weight: 500;
  margin-bottom: 12rpx;
}

.answer-content {
  font-size: 26rpx;
  color: #303133;
  line-height: 1.6;
}

.empty-answer {
  color: #909399;
  font-style: italic;
}

.answer-section {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 2rpx dashed #e4e7ed;
}

.correct-answer,
.answer-analysis {
  padding: 16rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}

.correct-answer {
  background-color: #f0f9ff;
}

.answer-analysis {
  background-color: #fef0f0;
}

.answer-title,
.analysis-title {
  font-size: 26rpx;
  font-weight: 500;
  color: #303133;
  display: block;
  margin-bottom: 8rpx;
}

.answer-text,
.analysis-text {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.6;
}

.score-info,
.sub-score {
  text-align: right;
  font-size: 26rpx;
  color: #606266;
  margin-top: 12rpx;
}

.score {
  font-size: 32rpx;
  font-weight: bold;
  margin: 0 8rpx;
}

.score.full {
  color: #67c23a;
}

.score.partial {
  color: #e6a23c;
}

.sub-question-type.type-single {
  background-color: #e1f3d8;
  color: #67c23a;
}

.sub-question-type.type-multiple {
  background-color: #fef0f0;
  color: #f56c6c;
}

.sub-question-type.type-judge {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.sub-question-type.type-blank {
  background-color: #ecf5ff;
  color: #409eff;
}

.sub-question-type.type-composite {
  background-color: #f4f4f5;
  color: #909399;
}

.sub-question-type.type-noun {
  background-color: #f0f9ff;
  color: #67c23a;
}

.sub-question-type.type-essay {
  background-color: #fff7e6;
  color: #fa8c16;
}

.sub-question-type.type-calc {
  background-color: #e6f7ff;
  color: #1890ff;
}

.sub-question-type.type-program {
  background-color: #f9f0ff;
  color: #722ed1;
}

.sub-question-type.type-default {
  background-color: #f4f4f5;
  color: #909399;
}

.score-badge-small {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.score-badge.correct {
  background-color: #f0f9ff;
  color: #67c23a;
}

.score-badge.partial {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.score-badge.wrong {
  background-color: #fef0f0;
  color: #f56c6c;
}

.question-stem {
  font-size: 28rpx;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 20rpx;
  padding: 20rpx;
  background-color: #f0f9ff;
  border-left: 6rpx solid #409eff;
  border-radius: 8rpx;
}

.sub-questions {
  margin-top: 20rpx;
}

.sub-question-card {
  background-color: #fafafa;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.sub-question-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.sub-question-number {
  font-size: 26rpx;
  font-weight: bold;
  color: #409eff;
}

.sub-question-type {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.score-badge-small {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  margin-left: auto;
}

.score-badge-small.correct {
  background-color: #f0f9ff;
  color: #67c23a;
}

.score-badge-small.partial {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.score-badge-small.wrong {
  background-color: #fef0f0;
  color: #f56c6c;
}

.sub-question-stem {
  font-size: 26rpx;
  line-height: 1.7;
  color: #303133;
  margin-bottom: 16rpx;
}

.options-area {
  margin: 20rpx 0;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 20rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.option-item.selected {
  background-color: #e1f3d8;
  border-color: #67c23a;
}

.option-item.correct {
  background-color: #f0f9ff;
  border-color: #409eff;
  font-weight: 500;
}

.option-label {
  font-size: 26rpx;
  color: #606266;
  margin-right: 12rpx;
  font-weight: 500;
}

.option-content {
  flex: 1;
  font-size: 26rpx;
  color: #303133;
  line-height: 1.6;
}

.text-answer {
  padding: 20rpx;
  background-color: #fafafa;
  border-radius: 12rpx;
}

.answer-label {
  font-size: 26rpx;
  color: #606266;
  font-weight: 500;
  margin-bottom: 12rpx;
}

.answer-content {
  font-size: 26rpx;
  color: #303133;
  line-height: 1.6;
}

.empty-answer {
  color: #909399;
  font-style: italic;
}

.answer-section {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 2rpx dashed #e4e7ed;
}

.correct-answer,
.answer-analysis {
  padding: 16rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}

.correct-answer {
  background-color: #f0f9ff;
}

.answer-analysis {
  background-color: #fef0f0;
}

.answer-title,
.analysis-title {
  font-size: 26rpx;
  font-weight: 500;
  color: #303133;
  display: block;
  margin-bottom: 8rpx;
}

.answer-text,
.analysis-text {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.6;
}

.score-info,
.sub-score {
  text-align: right;
  font-size: 26rpx;
  color: #606266;
  margin-top: 12rpx;
}

.score {
  font-size: 32rpx;
  font-weight: bold;
  margin: 0 8rpx;
}

.score.full {
  color: #67c23a;
}

.score.partial {
  color: #e6a23c;
}

/* AI分析区域 */
.ai-analysis-section {
  padding: 20rpx;
}

.ai-card {
  background-color: white;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.ai-header {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  padding: 30rpx 40rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.ai-title {
  font-size: 30rpx;
  font-weight: bold;
  color: white;
  flex: 1;
}

.ai-btn {
  padding: 12rpx 28rpx;
  background-color: #ecf5ff;
  color: #409eff;
  border: 2rpx solid #b3d8ff;
  border-radius: 8rpx;
  font-size: 24rpx;
  font-weight: normal;
  flex-shrink: 0;
  transition: all 0.3s;
}

.ai-btn::after {
  border: none;
}

.ai-btn:active {
  background-color: #d9ecff;
  border-color: #409eff;
}

.ai-btn::after {
  border: none;
}

.ai-content {
  padding: 30rpx;
  min-height: 300rpx;
}

.ai-result {
  font-size: 26rpx;
  line-height: 1.8;
  color: #303133;
}

.ai-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 40rpx;
  color: #909399;
}

.ai-empty.success {
  color: #67c23a;
}

.ai-empty .emoji {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.ai-empty .message {
  font-size: 26rpx;
  text-align: center;
  line-height: 1.6;
}

/* AI结果样式 */
.ai-result h1,
.ai-result h2,
.ai-result h3 {
  color: #303133;
  margin: 20rpx 0 16rpx;
  font-weight: bold;
}

.ai-result h1 {
  font-size: 32rpx;
  border-bottom: 2rpx solid #409eff;
  padding-bottom: 12rpx;
}

.ai-result h2 {
  font-size: 30rpx;
  color: #409eff;
}

.ai-result h3 {
  font-size: 28rpx;
}

.ai-result ul,
.ai-result ol {
  padding-left: 40rpx;
  margin: 16rpx 0;
}

.ai-result li {
  margin: 12rpx 0;
  line-height: 1.7;
}

.ai-result p {
  margin: 12rpx 0;
  line-height: 1.7;
}

.ai-result strong {
  color: #409eff;
  font-weight: bold;
}

/* AI 加载状态 */
.ai-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 40rpx;
  min-height: 300rpx;
}

.loading-spinner {
  width: 80rpx;
  height: 80rpx;
  border: 6rpx solid #e6f7ff;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin-top: 30rpx;
  font-size: 26rpx;
  color: #606266;
}
</style>

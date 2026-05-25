import { questionTypeColors } from './theme'

/**
 * 获取题型样式类名
 * @param {String} typeName - 题型名称
 * @returns {String}
 */
export function getTypeClass(typeName) {
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
}

/**
 * 获取题型配色
 * @param {String} typeName - 题型名称
 * @returns {Object}
 */
export function getTypeColor(typeName) {
  return questionTypeColors[typeName] || { bg: '#f4f4f5', color: '#909399' }
}

/**
 * 获取题目得分状态
 * @param {Object} question - 题目对象
 * @returns {Object|null}
 */
export function getScoreStatus(question) {
  if (question.answerScore === undefined || question.answerScore === null) {
    return null
  }
  
  const score = question.answerScore
  const fullScore = question.typeScore || 0
  
  if (score >= fullScore) {
    return { text: '全对', class: 'correct' }
  } else if (score > 0) {
    return { text: '半对', class: 'partial' }
  } else {
    return { text: '错误', class: 'wrong' }
  }
}

/**
 * 获取正确答案列表
 * @param {Object} question - 题目对象
 * @returns {String}
 */
export function getCorrectAnswers(question) {
  if (!question.choiceList) {
    return question.answer || '未设置'
  }
  
  const correctChoices = question.choiceList.filter(c => c.flag === true || c.flag === 1)
  return correctChoices.map(c => c.identification).join(', ') || question.answer || '未设置'
}

/**
 * 判断选项是否被选中
 * @param {Object} question - 题目对象
 * @param {String} identification - 选项标识
 * @returns {Boolean}
 */
export function isSelected(question, identification) {
  if (question.questionTypeName === '多选') {
    return question.checkList && question.checkList.includes(identification)
  } else {
    return question.newAnswer === identification
  }
}

/**
 * 简单的 Markdown 转 HTML
 * @param {String} markdown 
 * @returns {String}
 */
export function convertMarkdownToHtml(markdown) {
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
}

/**
 * 格式化时间
 * @param {Date|String|Number} time - 时间
 * @param {String} format - 格式 如 YYYY-MM-DD HH:mm:ss
 * @returns {String}
 */
export function formatTime(time, format = 'YYYY-MM-DD HH:mm:ss') {
  const date = new Date(time)
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hour)
    .replace('mm', minute)
    .replace('ss', second)
}

/**
 * 获取分数等级
 * @param {Number} score - 分数
 * @returns {Object} { level, text, color }
 */
export function getScoreLevel(score) {
  if (score >= 90) {
    return { level: 'excellent', text: '优秀', color: '#67c23a' }
  } else if (score >= 80) {
    return { level: 'good', text: '良好', color: '#409eff' }
  } else if (score >= 70) {
    return { level: 'medium', text: '中等', color: '#e6a23c' }
  } else if (score >= 60) {
    return { level: 'pass', text: '及格', color: '#e6a23c' }
  } else {
    return { level: 'fail', text: '不及格', color: '#f56c6c' }
  }
}

/**
 * 检查是否为空答案（包括富文本）
 * @param {String} answer - 答案内容
 * @returns {Boolean}
 */
export function isEmptyAnswer(answer) {
  if (!answer) return true
  // 检查是否包含公式、图片等富文本元素
  if (answer.includes('data-w-e-type="formula"') || answer.includes('<img')) {
    return false
  }
  // 移除HTML标签和空白字符
  const text = answer.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim()
  return text === ''
}

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {Number} wait - 等待时间(ms)
 * @returns {Function}
 */
export function throttle(func, wait = 300) {
  let timer = null
  return function(...args) {
    if (!timer) {
      timer = setTimeout(() => {
        func.apply(this, args)
        timer = null
      }, wait)
    }
  }
}

/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {Number} wait - 等待时间(ms)
 * @returns {Function}
 */
export function debounce(func, wait = 300) {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      func.apply(this, args)
    }, wait)
  }
}

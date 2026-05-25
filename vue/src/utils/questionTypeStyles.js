/**
 * 题目类型标签样式统一配置
 * 根据题目类型名称返回对应的 Element Plus Tag 样式
 * 同 variety 的题目颜色相近但有所区别
 */

export const getQuestionTypeStyle = (typeName) => {
  const styleMap = {
    // choice 类（选择题系列 - 蓝绿橙色系）
    '单选': { type: 'success', effect: 'dark' },      // 深绿色
    '多选': { type: 'primary', effect: 'dark' },      // 深蓝色
    '判断': { type: 'warning', effect: 'dark' },      // 深橙色
    
    // text 类（主观题系列 - 灰红紫色系）
    '填空': { type: 'info', effect: 'dark' },         // 深灰色
    '名词解析': { type: 'danger', effect: 'dark' },   // 深红色
    '论述': { type: 'danger', effect: 'plain' },      // 浅红色
    '计算': { type: 'info', effect: 'dark' },         // 深灰色
    '程序': { type: 'info', effect: 'plain' },        // 浅灰色
    
    // composite 类（复合题系列 - 紫蓝绿色系）
    '资料': { type: 'warning', effect: 'plain' },     // 浅橙色
    '完形填空': { type: 'primary', effect: 'plain' }, // 浅蓝色
    '阅读理解': { type: 'success', effect: 'plain' }, // 浅绿色
    '综合': { type: 'warning', effect: 'plain' }      // 浅橙色
  }
  
  return styleMap[typeName] || { type: 'info', effect: 'plain' }
}

/**
 * 获取题型标签的 type 属性
 */
export const getQuestionTypeTag = (typeName) => {
  return getQuestionTypeStyle(typeName).type
}

/**
 * 获取题型标签的 effect 属性
 */
export const getQuestionTypeEffect = (typeName) => {
  return getQuestionTypeStyle(typeName).effect
}

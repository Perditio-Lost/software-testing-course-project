// 主题配色
export const theme = {
  // 主色调 - 蓝色系
  primary: '#409eff',
  primaryLight: '#79bbff',
  primaryLighter: '#a0cfff',
  primaryExtraLight: '#c6e2ff',
  primaryBg: '#e6f7ff',
  
  // 成功色 - 绿色
  success: '#67c23a',
  successLight: '#95d475',
  successBg: '#f0f9ff',
  
  // 警告色 - 橙色
  warning: '#e6a23c',
  warningLight: '#ebb563',
  warningBg: '#fdf6ec',
  
  // 危险色 - 红色
  danger: '#f56c6c',
  dangerLight: '#f78989',
  dangerBg: '#fef0f0',
  
  // 信息色 - 灰色
  info: '#909399',
  infoBg: '#f4f4f5',
  
  // 文字颜色
  textPrimary: '#303133',
  textRegular: '#606266',
  textSecondary: '#909399',
  textPlaceholder: '#c0c4cc',
  
  // 边框颜色
  borderBase: '#dcdfe6',
  borderLight: '#e4e7ed',
  borderLighter: '#ebeef5',
  borderExtraLight: '#f2f6fc',
  
  // 背景颜色
  bgWhite: '#ffffff',
  bgBase: '#f5f7fa',
  bgLight: '#fafbfc',
  
  // 渐变色
  gradientBlue: 'linear-gradient(135deg, #bae7ff 0%, #e6f7ff 100%)',
  gradientBlueDeep: 'linear-gradient(135deg, #409eff 0%, #79bbff 100%)'
}

// 题型配色
export const questionTypeColors = {
  '单选': { bg: '#e1f3d8', color: '#67c23a' },
  '多选': { bg: '#fef0f0', color: '#f56c6c' },
  '判断': { bg: '#fdf6ec', color: '#e6a23c' },
  '填空': { bg: '#ecf5ff', color: '#409eff' },
  '复合': { bg: '#f4f4f5', color: '#909399' },
  '名词解析': { bg: '#e6f7ff', color: '#409eff' },
  '论述': { bg: '#e6f7ff', color: '#409eff' },
  '计算': { bg: '#e6f7ff', color: '#1890ff' },
  '程序': { bg: '#e6f7ff', color: '#1890ff' }
}

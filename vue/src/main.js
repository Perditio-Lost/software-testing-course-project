import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '@/assets/css/global.css'
import '@/assets/css/theme-blue.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessageBox } from 'element-plus'

const app = createApp(App)

app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局配置MessageBox按钮样式
const originalConfirm = ElMessageBox.confirm
ElMessageBox.confirm = function(...args) {
  if (args[2]) {
    args[2] = {
      ...args[2],
      cancelButtonClass: 'el-button--danger is-plain',
      confirmButtonClass: 'el-button--primary is-plain'
    }
  } else {
    args[2] = {
      cancelButtonClass: 'el-button--danger is-plain',
      confirmButtonClass: 'el-button--primary is-plain'
    }
  }
  return originalConfirm.apply(this, args)
}

app.mount('#app')

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

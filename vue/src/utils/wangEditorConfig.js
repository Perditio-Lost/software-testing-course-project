// WangEditor 公式插件配置
import { Boot } from '@wangeditor/editor'
import formulaModule from '@wangeditor/plugin-formula'

// 注册公式插件（全局只需注册一次）
Boot.registerModule(formulaModule)

export default {
  Boot
}

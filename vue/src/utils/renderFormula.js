// 富文本公式渲染工具
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * 渲染页面中的所有公式
 * @param {Element|null} container - 容器元素，如果不传则渲染整个文档
 */
export function renderFormulas(container = document) {
  setTimeout(() => {
    const formulas = container.querySelectorAll('[data-w-e-type="formula"]')
    formulas.forEach(elem => {
      const formula = elem.getAttribute('data-value')
      if (formula && !elem.classList.contains('katex-rendered')) {
        try {
          katex.render(formula, elem, {
            throwOnError: false,
            displayMode: !elem.hasAttribute('data-w-e-is-inline')
          })
          elem.classList.add('katex-rendered')
        } catch (e) {
          console.error('KaTeX render error:', e)
        }
      }
    })
  }, 50)
}

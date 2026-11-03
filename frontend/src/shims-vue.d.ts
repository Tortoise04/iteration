/// <reference types="vite/client" />

/**
 * element-plus 的 zh-cn 语言包是 .mjs，包内没有附带类型声明（TS7016）。
 * 这里补一份最小声明，让 vue-tsc 通过；不影响运行时行为。
 */
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const locale: Language
  export default locale
}

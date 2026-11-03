import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 这台机器上 esbuild 处理大 chunk 时会写临时输入文件、删除被系统拒绝
    // （vite:esbuild-transpile -> remove ...esbuild-xxx: Access is denied）。
    // 用 terser 压缩 + 不降级语法，绕开 esbuild 的压缩/转译步骤。
    minify: 'terser',
    target: 'esnext',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})

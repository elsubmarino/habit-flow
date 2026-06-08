import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
      // ⚠️ '/oauth2' 전체를 프록시하면 /oauth2/redirect(프론트 콜백)까지 8080으로 가서 무한 리다이렉트 발생
      '/oauth2/authorization': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

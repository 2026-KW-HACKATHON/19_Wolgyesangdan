import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // 회원 앱(frontend)이 5173을 쓰므로 관리자 웹은 5174로 고정 — 백엔드 CORS 기본값에 이 주소가 들어 있다
  server: { port: 5174, strictPort: true },
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendPort = process.env.E2E_BACKEND_PORT ?? process.env.VITE_BACKEND_PORT ?? '8080'
const backendUrl = `http://localhost:${backendPort}`

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/health': backendUrl,
      '/fix': backendUrl,
      '/orders': backendUrl,
      '/exec-reports': backendUrl,
    },
  },
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Proxy all /api and /oauth2 calls to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        credentials: true,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        credentials: true,
      },
      '/login': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        credentials: true,
      },
    },
  },
})

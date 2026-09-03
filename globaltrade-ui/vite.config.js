import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: false,
    proxy: {
      // Any request to /globaltrade/* gets forwarded to Payara
      // The browser never sees a cross-origin request — no CORS needed
      '/globaltrade': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})


import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/proxy/orders':    { target: 'http://localhost:8081', rewrite: p => p.replace('/proxy/orders', '') },
      '/proxy/inventory': { target: 'http://localhost:8082', rewrite: p => p.replace('/proxy/inventory', '') },
      '/proxy/shipments': { target: 'http://localhost:8084', rewrite: p => p.replace('/proxy/shipments', '') },
    }
  }
})

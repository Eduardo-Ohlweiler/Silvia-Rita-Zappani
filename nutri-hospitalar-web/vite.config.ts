import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import svgr from 'vite-plugin-svgr'
import path from 'node:path'

export default defineConfig({
  // svgr: `import Logo from './logo.svg?react'` vira componente React, o que
  // deixa o SVG herdar a cor do CSS via currentColor — um arquivo serve os
  // dois temas.
  plugins: [react(), tailwindcss(), svgr()],

  resolve: {
    // `import.meta.dirname`, não `__dirname`: o configLoader nativo do Vite 8
    // não define __dirname, e é o que vai virar padrão.
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },

  server: {
    port: 5173,
    // Proxy para a API em desenvolvimento: o front chama /api/... na própria
    // origem, então não há CORS nem URL absoluta espalhada pelo código.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (caminho) => caminho.replace(/^\/api/, ''),
      },
    },
  },
})

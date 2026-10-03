// Gera demo/Roteiros-de-video.pdf a partir de roteiros.html.
//   node demo/roteiros/gerar-pdf.mjs
// Usa o playwright-core que já vem no node_modules do front (dependência do
// Vite) e o Chrome instalado na máquina — nada a instalar. CHROME troca o
// caminho do executável.

import { dirname, join } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const AQUI = dirname(fileURLToPath(import.meta.url))
const { chromium } = await import(pathToFileURL(join(AQUI, '../../nutri-hospitalar-web/node_modules/playwright-core/index.mjs')).href)

const CHROME = process.env.CHROME ?? (process.platform === 'win32'
  ? 'C:/Program Files/Google/Chrome/Application/chrome.exe'
  : '/usr/bin/google-chrome')

const navegador = await chromium.launch({ executablePath: CHROME })
const pagina = await navegador.newPage()
await pagina.goto(pathToFileURL(join(AQUI, 'roteiros.html')).href, { waitUntil: 'load' })
const destino = join(AQUI, '..', 'Roteiros-de-video.pdf')
await pagina.pdf({
  path: destino,
  format: 'A4',
  printBackground: true,
  preferCSSPageSize: true,
  displayHeaderFooter: true,
  headerTemplate: '<span></span>',
  footerTemplate: '<div style="width:100%;font:8px Segoe UI,sans-serif;color:#8a94a3;padding:0 15mm;display:flex;justify-content:space-between"><span>Nutri Hospitalar de Sucesso · roteiros de vídeo</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>',
})
await navegador.close()
console.log('PDF gerado em', destino)

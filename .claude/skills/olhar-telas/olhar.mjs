/**
 * Abre uma rota do sistema num Chrome real, tira screenshot e, se pedido, gera
 * o PDF da folha impressa.
 *
 *   node .claude/skills/olhar-telas/olhar.mjs /app/uti/calculadora tela
 *   node .claude/skills/olhar-telas/olhar.mjs /app/uti/avaliacoes lista --pdf
 *   node .claude/skills/olhar-telas/olhar.mjs /app/pediatria/dashboard ped --360
 *
 * Sai em /tmp/olhar/<nome>.png (e .pdf). Depois ABRA o arquivo — gerar e não
 * olhar não vale de nada.
 */
import { chromium } from '../../../nutri-hospitalar-web/node_modules/playwright-core/index.mjs'
import { mkdirSync } from 'node:fs'

const [, , rota = '/app', nome = 'tela', ...flags] = process.argv
const pdf = flags.includes('--pdf')
const largura = flags.includes('--360') ? 360 : 1440
const SAIDA = '/tmp/olhar'
mkdirSync(SAIDA, { recursive: true })

const b = await chromium.launch({ executablePath: '/usr/bin/google-chrome', args: ['--no-sandbox'] })
const pg = await (await b.newContext({ viewport: { width: largura, height: 1100 } })).newPage()

const erros = []
pg.on('pageerror', (e) => erros.push(String(e)))
pg.on('console', (m) => m.type() === 'error' && erros.push(m.text()))
// Sem isto o window.print() abre diálogo e trava o headless.
await pg.addInitScript(() => { window.print = () => {} })

await pg.goto('http://localhost:5173/app/login', { waitUntil: 'domcontentloaded' })
await pg.fill('input[type="email"]', 'ohlweilereduardo@gmail.com')
await pg.fill('input[type="password"]', 'Admin123@Nutri')
await pg.click('button[type="submit"]')
// `aside` e não `**/app**`: esse padrão casa com a própria /app/login.
await pg.waitForSelector('aside', { timeout: 20000 })

await pg.goto(`http://localhost:5173${rota}`, { waitUntil: 'domcontentloaded' })
// Os gráficos do recharts entram por animação de layout.
await pg.waitForTimeout(1800)

await pg.screenshot({ path: `${SAIDA}/${nome}.png`, fullPage: true })
if (pdf) await pg.pdf({ path: `${SAIDA}/${nome}.pdf`, format: 'A4', printBackground: true })

const m = await pg.evaluate(() => {
  const d = document.documentElement
  let culpado = ''
  if (d.scrollWidth > d.clientWidth + 1)
    for (const e of document.querySelectorAll('*')) {
      const r = e.getBoundingClientRect()
      if (r.right > d.clientWidth + 1 && r.width > 20) {
        culpado = `${e.tagName}.${String(e.className).slice(0, 40)}`
        break
      }
    }
  return { rolagem: d.scrollWidth - d.clientWidth, culpado, folhas: document.querySelectorAll('.folha').length }
})

console.log(`${rota} @ ${largura}px`)
console.log(`  arquivo : ${SAIDA}/${nome}.png${pdf ? ` (+ .pdf)` : ''}`)
console.log(`  rolagem : ${m.rolagem}px ${m.culpado ? `← ${m.culpado}` : ''}`)
console.log(`  folha   : ${m.folhas > 0 ? 'monta' : 'nenhuma'}`)
console.log(`  erros   : ${erros.length ? erros.slice(0, 3).join(' | ') : 'nenhum'}`)
await b.close()

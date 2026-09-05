/**
 * Driver das três vias: PREENCHI → ENVIADO → PERSISTIDO.
 *
 * O `enviados` guarda o corpo de cada POST/PUT que a tela dispara; o `trocas`
 * guarda a resposta ao lado. Comparar os dois com o que foi digitado é a única
 * verificação que build, lint e a suíte não fazem.
 */
import { chromium } from '/home/eduardo/Documentos/sistema silvia/nutri-hospitalar-web/node_modules/playwright-core/index.mjs'
import fs from 'node:fs'

export const linhas = []
let arquivo = '/tmp/olhar/verificacao.txt'
export function saidaEm(f) { arquivo = f }
export function P(...a) {
  const s = a.map((x) => (typeof x === 'string' ? x : JSON.stringify(x))).join(' ')
  linhas.push(s)
  fs.writeFileSync(arquivo, linhas.join('\n') + '\n')
}
export let falhas = 0
export function ok(t, v, extra = '') {
  if (!v) falhas++
  P(`${v ? '  ok  ' : ' FALHA'} ${t}${extra ? ' — ' + extra : ''}`)
  return v
}

export async function abrir() {
  const b = await chromium.launch({ executablePath: '/usr/bin/google-chrome', args: ['--no-sandbox'] })
  const ctx = await b.newContext({ viewport: { width: 1440, height: 1200 } })
  const pg = await ctx.newPage()
  const estado = { erros: [], trocas: [] }
  pg.on('pageerror', (e) => estado.erros.push(String(e).slice(0, 160)))
  pg.on('console', (m) => {
    if (m.type() === 'error' && !m.text().includes('Failed to load resource'))
      estado.erros.push(m.text().slice(0, 160))
  })
  /*
   * Requisição e resposta em DOIS ganchos. Fazer tudo dentro do `response`
   * com um `await r.text()` antes do `push` já custou uma rodada inteira: a
   * troca só entrava no array depois do await, e a verificação lia o array
   * antes — concluindo "nenhuma requisição" diante de um 400 que estava na
   * tela. Registrar no `request` é síncrono e não tem como perder.
   */
  pg.on('request', (q) => {
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(q.method())) return
    // Em dev o front fala com `/api` (proxy do Vite), NUNCA com `:8080`.
    if (!q.url().includes('/api/') || q.url().includes('/auth/')) return
    let env = null
    try { env = JSON.parse(q.postData() || 'null') } catch { env = q.postData() }
    estado.trocas.push({ m: q.method(), u: q.url().replace(/.*?\/api\//, '/'), env, st: null, res: null })
  })
  pg.on('response', async (r) => {
    const t = estado.trocas.find(
      (x) => x.st === null && x.u === r.url().replace(/.*?\/api\//, '/') && x.m === r.request().method(),
    )
    if (!t) return
    t.st = r.status()
    try { t.res = JSON.parse(await r.text()) } catch { t.res = null }
  })
  await pg.addInitScript(() => { window.print = () => {} })
  await pg.goto('http://localhost:5173/app/login', { waitUntil: 'domcontentloaded' })
  await pg.fill('input[type="email"]', 'ohlweilereduardo@gmail.com')
  await pg.fill('input[type="password"]', 'Admin123@Nutri')
  await pg.click('button[type="submit"]')
  await pg.waitForSelector('aside', { timeout: 25000 })
  return { b, pg, estado }
}

/**
 * O controle pelo RÓTULO, não pela posição no DOM.
 *
 * `following::input[1]` funcionava para `TEntry` e `TSelect`, que põem o
 * `<label htmlFor>` como irmão — e falhava calado no `TTextArea`, que
 * **embrulha** o controle: ali o textarea é descendente, não "following", e o
 * xpath ou não acha nada ou acha o campo do rótulo SEGUINTE. Associação por
 * `htmlFor`/embrulho é o que o navegador usa, e é o que `getByLabel` faz.
 *
 * `visible=true` porque as abas escondidas continuam montadas, com os mesmos
 * rótulos.
 */
export const porRotulo = (pg, rot) =>
  pg
    .getByLabel(rot, { exact: true })
    // `aria-labelledby` faz o PAINEL DA ABA responder pelo mesmo rótulo do
    // campo — "Observações" é aba e é campo. Só controle de formulário serve.
    .and(pg.locator('input, textarea, select'))
    .locator('visible=true')
    .first()
export const campo = porRotulo
export const area = porRotulo
export const selec = porRotulo

/**
 * O combo assíncrono. A armadilha: enquanto busca, o `<ul>` tem um `<li>`
 * "Buscando…" SEM `role="option"` — pegar o primeiro `li` clica no nada, a tela
 * segue sem paciente, e o salvar nem chega a disparar requisição.
 */
export async function escolherNoCombo(pg, rot, texto) {
  const i = porRotulo(pg, rot)
  await i.click()
  await i.fill(texto)
  /*
   * A lista JÁ ESTÁ na tela quando se digita — é a pré-carga sem termo — e o
   * filtro só chega depois do debounce de 500 ms. Esperar por
   * `[role="option"]` visível é satisfeito na hora, pelo primeiro nome da
   * lista velha: buscar "Helena" e clicar em "Antônio". Silencioso, e
   * invalida o teste inteiro. Só serve esperar pela opção QUE CASA COM O QUE
   * SE DIGITOU.
   */
  const opcao = pg.locator(`[role="option"]:has-text("${texto}")`).first()
  await opcao.waitFor({ state: 'visible', timeout: 15000 })
  await pg.waitForFunction(
    (t) => {
      const l = [...document.querySelectorAll('[role="option"]')]
      return l.length > 0 && l.every((o) => o.textContent.toLowerCase().includes(t.toLowerCase()))
    },
    texto,
    { timeout: 15000 },
  )
  const nome = (await opcao.innerText()).trim()
  await opcao.click()
  await pg.waitForTimeout(1500)
  return nome
}

export async function aba(pg, nome) {
  await pg.locator(`[role="tab"]:has-text("${nome}")`).first().click()
  await pg.waitForTimeout(600)
}

/** O que a tela mostra num TResult, pelo rótulo. */
export async function resultado(pg, rot) {
  const l = pg.locator('main').locator(`:text-is("${rot}")`).first()
  if (!(await l.count())) return null
  return (await l.locator('xpath=..').innerText()).replace(/\s+/g, ' ').trim()
}

export async function rolagem(pg) {
  return pg.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
}

/**
 * Navegar como o usuário navega: pela rota, dentro da mesma página.
 *
 * `page.goto` recarrega o documento — e o access token vive em MEMÓRIA, então
 * cada recarga gasta um `/auth/refresh`. Sete rotas em vinte segundos estouram
 * o Bucket4j e a sessão cai no login, com a varredura culpando as telas por um
 * 429 que ela mesma provocou. Ninguém abre o sistema assim; o React Router
 * ouve `popstate`.
 */
export async function ir(pg, rota) {
  await pg.evaluate((r) => {
    history.pushState({}, '', r)
    dispatchEvent(new PopStateEvent('popstate'))
  }, rota)
  await pg.waitForTimeout(300)
}

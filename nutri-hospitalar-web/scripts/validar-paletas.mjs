#!/usr/bin/env node
/**
 * Validador das paletas do sistema — docs/05 §11.
 *
 *   npm run paletas
 *
 * Cor de interface aqui não é escolhida a olho, é medida. Este script lê o
 * `theme.css` como o navegador o leria — resolvendo a cascata padrão → paleta,
 * no claro e no escuro — e reprova a paleta que:
 *
 *  1. quebra um par de contraste WCAG da docs/05 §3 (texto, botão, sidebar);
 *  2. tem a escala brand fora de ordem (o degrau 600 mais claro que o 500…);
 *  3. declara no bloco claro um token que o bloco escuro não redeclara — esse
 *     token VAZARIA claro para dentro do tema escuro (ver o cabeçalho das
 *     paletas no theme.css);
 *  4. põe a primária perto demais do vermelho de perigo (um "Salvar" com cara
 *     de "Excluir" — foi o que tirou a Terracota);
 *  5. apaga as séries de gráfico sobre a superfície escura dela;
 *  6. existe num lugar e não nos outros: theme.css, styles/paletas.ts e o enum
 *     PaletaSistema do backend têm de listar as mesmas paletas.
 *
 * A paleta Padrão é a régua, não o réu: ela é relatada e nunca reprovada — o
 * pedido foi não tocar em nada dela (e ela tem um 4,48:1 conhecido no
 * primary-active do escuro).
 *
 * Sem dependência: a conversão sRGB → OKLab é a de Björn Ottosson, em vinte
 * linhas, e o contraste é o da WCAG 2.x.
 */
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const css = readFileSync(resolve(raiz, 'src/styles/theme.css'), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')
const catalogo = readFileSync(resolve(raiz, 'src/styles/paletas.ts'), 'utf8')
const enumJava = readFileSync(
  resolve(raiz, '../nutri-hospitalar-api/src/main/java/com/nutri/hospitalar/configuracao/enums/PaletaSistema.java'),
  'utf8',
)

// ── Cor ──────────────────────────────────────────────────────────────────
const hexParaRgb = (h) => [0, 2, 4].map((i) => parseInt(h.replace('#', '').slice(i, i + 2), 16) / 255)
const linear = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
function luminancia(hex) {
  const [r, g, b] = hexParaRgb(hex).map(linear)
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
function contraste(a, b) {
  const [x, y] = [luminancia(a), luminancia(b)]
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05)
}
function oklab(hex) {
  const [r, g, b] = hexParaRgb(hex).map(linear)
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
  return [
    0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  ]
}
/** ΔE em OKLab ×100 — a mesma escala da docs/05 §10 (piso 15 entre séries). */
const deltaE = (a, b) => Math.hypot(...oklab(a).map((v, i) => v - oklab(b)[i])) * 100

// ── Leitura do CSS ───────────────────────────────────────────────────────
/**
 * Primeiro bloco cujo seletor é exatamente `seletor` — sozinho ou à frente de
 * uma lista (os blocos escuros das paletas também valem para a moldura escura,
 * docs/05 §11.6, e por isso têm dois seletores).
 */
function bloco(seletor) {
  let i = css.indexOf(`${seletor} {`)
  if (i < 0) i = css.indexOf(`${seletor},`)
  if (i < 0) return null
  const ini = css.indexOf('{', i) + 1
  const corpo = css.slice(ini, css.indexOf('}', ini))
  const vars = {}
  for (const [, nome, valor] of corpo.matchAll(/(--[\w-]+)\s*:\s*([^;]+);/g)) vars[nome] = valor.trim()
  return vars
}

const tema = bloco('@theme')
const padraoClaro = bloco(':root,\n[data-theme="light"]') ?? bloco(':root,\r\n[data-theme="light"]')
const padraoEscuro = bloco('[data-theme="dark"]')
if (!tema || !padraoClaro || !padraoEscuro) {
  console.error('Não achei os blocos da paleta padrão no theme.css — o validador precisa ser atualizado.')
  process.exit(1)
}

/** Resolve `var(--x)` e tira o prefixo, para conversar em nomes curtos. */
function resolver(...camadas) {
  const vars = Object.assign({}, ...camadas)
  const valor = (nome) => {
    let v = vars[nome]
    for (let n = 0; v && v.startsWith('var(') && n < 5; n++) v = vars[v.slice(4, -1).trim()]
    return v
  }
  return new Proxy({}, { get: (_, k) => valor(k.startsWith('--') ? k : `--${k}`) })
}

const atributosCss = [...css.matchAll(/\[data-paleta="([a-z]+)"\]\s*\{/g)].map((m) => m[1])

// ── As regras ────────────────────────────────────────────────────────────
/**
 * Distância mínima entre a primária e o vermelho de perigo, em ΔE OKLab ×100.
 *
 * Não é número escolhido: é a menor distância entre dois semânticos do
 * PRÓPRIO sistema no tema escuro — success × info, 12,2. Uma primária mais
 * perto do perigo do que os semânticos ficam uns dos outros faria um "Salvar"
 * se confundir com um "Excluir". Foi o que tirou a Terracota (10,0 no claro) e
 * o que obrigou Café e Malva a terem primária própria no escuro: o perigo de
 * lá é um salmão claro, e marrom e rosa claros caem em cima dele.
 */
const PISO_PERIGO = 12

const ESCALA = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', '950']

function pares(c, e) {
  return [
    // Claro — docs/05 §3
    ['claro', 'texto / superfície', c.txt, c.surface, 7],
    ['claro', 'primária / superfície', c.primary, c.surface, 4.5],
    ['claro', 'texto do botão / primária', c['txt-inverse'], c.primary, 4.5],
    ['claro', 'texto do botão / primária hover', c['txt-inverse'], c['primary-hover'], 4.5],
    ['claro', 'texto do botão / primária active', c['txt-inverse'], c['primary-active'], 4.5],
    ['claro', 'secundário / superfície', c['txt-secondary'], c.surface, 4.5],
    ['claro', 'secundário / surface-alt', c['txt-secondary'], c['surface-alt'], 4.5],
    ['claro', 'secundário / canvas', c['txt-secondary'], c.bg, 4.5],
    ['claro', 'muted (ícone) / superfície', c['txt-muted'], c.surface, 3],
    ['claro', 'sidebar / item ativo', c['sidebar-text'], c['sidebar-active-bg'], 4.5],
    ['claro', 'item ativo: texto / fundo', c['sidebar-active-text'], c['sidebar-active-bg'], 4.5],
    ['claro', 'título da folha / brand-100', c.primary, c['color-brand-100'], 4.5],
    // Escuro
    ['escuro', 'texto / superfície', e.txt, e.surface, 7],
    ['escuro', 'secundário / superfície', e['txt-secondary'], e.surface, 4.5],
    ['escuro', 'secundário / surface-alt', e['txt-secondary'], e['surface-alt'], 4.5],
    ['escuro', 'muted (ícone) / superfície', e['txt-muted'], e.surface, 3],
    ['escuro', 'sidebar: texto / fundo', e['sidebar-text'], e.sidebar, 4.5],
    ['escuro', 'item ativo: texto / fundo', e['sidebar-active-text'], e['sidebar-active-bg'], 4.5],
    // Moldura escura (§11.6): o cabeçalho fica no tom do menu, com o nome do
    // usuário em --txt e o papel e os ícones em --txt-secondary.
    ['escuro', 'cabeçalho da moldura: texto / menu', e.txt, e.sidebar, 7],
    ['escuro', 'cabeçalho da moldura: secundário / menu', e['txt-secondary'], e.sidebar, 4.5],
    ['escuro', 'texto do botão / primária', e['txt-inverse'], e.primary, 4.5],
    ['escuro', 'texto do botão / primária hover', e['txt-inverse'], e['primary-hover'], 4.5],
    ['escuro', 'texto do botão / primária active', e['txt-inverse'], e['primary-active'], 4.5],
    ['escuro', 'primária (link, foco) / superfície', e.primary, e.surface, 3],
    ['escuro', 'superfície / canvas (degrau)', e.surface, e.bg, 1.15],
    ['escuro', 'gráfico série 1 / superfície', e['viz-serie-1'], e.surface, 4],
    ['escuro', 'gráfico série 2 / superfície', e['viz-serie-2'], e.surface, 4],
    ['escuro', 'gráfico série 3 / superfície', e['viz-serie-3'], e.surface, 4],
  ]
}

function validar(nome, claroDaPaleta, escuroDaPaleta) {
  const falhas = []
  const c = resolver(tema, padraoClaro, claroDaPaleta)
  const e = resolver(tema, padraoClaro, padraoEscuro, claroDaPaleta, escuroDaPaleta)

  for (const [lado, rotulo, a, b, minimo] of pares(c, e)) {
    if (!a || !b) {
      falhas.push(`${lado}: ${rotulo} — token ausente`)
      continue
    }
    const r = contraste(a, b)
    if (r < minimo) falhas.push(`${lado}: ${rotulo} ${a} × ${b} = ${r.toFixed(2)}:1 (mínimo ${minimo})`)
  }

  const claridades = ESCALA.map((d) => oklab(c[`color-brand-${d}`])[0])
  claridades.forEach((L, i) => {
    if (i > 0 && L >= claridades[i - 1]) falhas.push(`escala brand fora de ordem no degrau ${ESCALA[i]}`)
  })

  for (const [lado, r] of [['claro', c], ['escuro', e]]) {
    const d = deltaE(r.primary, r.danger)
    if (d < PISO_PERIGO) falhas.push(`${lado}: primária ${r.primary} a ΔE ${d.toFixed(1)} do perigo ${r.danger} (mínimo ${PISO_PERIGO})`)
  }

  if (escuroDaPaleta) {
    const vazam = Object.keys(claroDaPaleta)
      .filter((k) => !k.startsWith('--color-brand-'))
      .filter((k) => !(k in escuroDaPaleta))
    for (const k of vazam) falhas.push(`${k} está no bloco claro e não no escuro — o valor claro vazaria no tema escuro`)
  }

  return falhas
}

// ── Execução ─────────────────────────────────────────────────────────────
let reprovou = false

const ruidoDaPadrao = validar('padrao', {}, null)
console.log(`padrão     régua — ${ruidoDaPadrao.length ? `${ruidoDaPadrao.length} observação(ões), não reprova:` : 'ok'}`)
for (const f of ruidoDaPadrao) console.log(`             · ${f}`)

for (const atributo of atributosCss) {
  const claro = bloco(`[data-paleta="${atributo}"]`)
  const escuro = bloco(`[data-paleta="${atributo}"][data-theme="dark"]`)
  if (!escuro) {
    console.log(`${atributo.padEnd(10)} REPROVADA — sem bloco escuro`)
    reprovou = true
    continue
  }
  const falhas = validar(atributo, claro, escuro)
  console.log(`${atributo.padEnd(10)} ${falhas.length ? 'REPROVADA' : 'ok'}`)
  for (const f of falhas) console.log(`             · ${f}`)
  reprovou ||= falhas.length > 0
}

// As três listas da mesma coisa.
const doCatalogo = [...new Set([...catalogo.matchAll(/atributo:\s*'([a-z]+)'/g)].map((m) => m[1]))]
const valoresEnum = [...enumJava.matchAll(/^\s+([A-Z_]+),?\s*$/gm)].map((m) => m[1])
const basesEnum = valoresEnum.filter((v) => !v.endsWith('_MENU_ESCURO'))
const doEnum = basesEnum.map((v) => v.toLowerCase()).filter((v) => v !== 'padrao')
// Toda base tem a sua variante de menu escuro (docs/05 §11.6), e nenhuma
// variante fica sem base — a variante não tem CSS próprio, usa o da base.
for (const base of basesEnum) {
  if (!valoresEnum.includes(`${base}_MENU_ESCURO`)) {
    console.log(`FALTA no enum: ${base}_MENU_ESCURO`)
    reprovou = true
  }
}
for (const v of valoresEnum.filter((x) => x.endsWith('_MENU_ESCURO'))) {
  if (!basesEnum.includes(v.replace('_MENU_ESCURO', ''))) {
    console.log(`Variante sem base no enum: ${v}`)
    reprovou = true
  }
}
const listas = { 'theme.css': atributosCss, 'styles/paletas.ts': doCatalogo, 'PaletaSistema.java': doEnum }
const todas = [...new Set(Object.values(listas).flat())].sort()
for (const [onde, lista] of Object.entries(listas)) {
  const faltam = todas.filter((p) => !lista.includes(p))
  if (faltam.length) {
    console.log(`FALTA em ${onde}: ${faltam.join(', ')}`)
    reprovou = true
  }
}
if (!enumJava.includes('PADRAO') || !catalogo.includes("chave: 'PADRAO'")) {
  console.log('FALTA a paleta PADRAO no enum ou no catálogo')
  reprovou = true
}

console.log(reprovou ? '\nHá paleta reprovada.' : `\n${atributosCss.length} paletas aprovadas, além da padrão.`)
process.exit(reprovou ? 1 : 0)

---
name: olhar-telas
description: Abrir o sistema num Chrome real e OLHAR as telas — screenshot, PDF da impressão, checagem de 360 px e erros de console. Use sempre que for validar visual, conferir uma folha impressa, testar responsividade, ou dar uma tela por pronta. Build e teste verdes NÃO substituem isto.
---

# Olhar as telas de verdade

Este projeto tem **zero testes de front**. Tudo o que a tela mostra — motivo de
ausência, folha impressa, rolagem em 360 px — só é verificável abrindo. E a
experiência aqui é literal: **cinco defeitos reais passaram por `build`, `lint`
e 355 testes verdes** e só apareceram no navegador. Entre eles um prontuário que
imprimia "100 kcal por 100 ml" ao lado de "880 ml · 616 kcal", conta que não
fecha.

Não existe runner de teste de front, e não se vai criar um por causa disto. O
que existe é o driver abaixo.

## 1. Subir o que precisa estar de pé

Os dois, em background — a API demora ~40 s:

```bash
cd nutri-hospitalar-api && ./run-dev.sh          # :8080
cd nutri-hospitalar-web && npm run dev           # :5173
```

Esperar de verdade, sem `sleep` fixo:

```bash
until ss -ltn | grep -q :8080 && ss -ltn | grep -q :5173; do sleep 3; done
```

Se o 8080 recusar conexão, **quase sempre é uma API antiga ainda no ar**:
`pkill -f "spring-boot:run"` e subir de novo.

## 2. O driver

`playwright-core` **já está** em `nutri-hospitalar-web/node_modules` (dependência
do Vite) — **não instale playwright**. Ele dirige o Chrome do sistema:

```js
import { chromium } from '/home/eduardo/Documentos/sistema silvia/nutri-hospitalar-web/node_modules/playwright-core/index.mjs'

const b = await chromium.launch({
  executablePath: '/usr/bin/google-chrome',   // não há browser baixado pelo playwright
  args: ['--no-sandbox'],
})
const pg = await (await b.newContext({ viewport: { width: 1440, height: 1100 } })).newPage()

const erros = []
pg.on('pageerror', (e) => erros.push(String(e)))
pg.on('console', (m) => m.type() === 'error' && erros.push(m.text()))

// window.print() abriria diálogo e travaria o headless.
await pg.addInitScript(() => { window.print = () => {} })

await pg.goto('http://localhost:5173/app/login', { waitUntil: 'domcontentloaded' })
await pg.fill('input[type="email"]', 'ohlweilereduardo@gmail.com')
await pg.fill('input[type="password"]', 'Admin123@Nutri')
await pg.click('button[type="submit"]')
await pg.waitForSelector('aside', { timeout: 20000 })   // ver armadilha 1

await pg.goto('http://localhost:5173/app/uti/calculadora', { waitUntil: 'domcontentloaded' })
await pg.waitForTimeout(1500)                            // ver armadilha 2
await pg.screenshot({ path: '/tmp/tela.png', fullPage: true })
console.log('erros:', erros.length ? erros : 'nenhum')
await b.close()
```

Rodar com `node arquivo.mjs`, do diretório do scratchpad. **Depois abrir o PNG
com a ferramenta Read** — gerar e não olhar não vale.

## 3. As sete armadilhas que custaram tempo

1. **Esperar por `aside`, nunca por `**/app**`.** O padrão `**/app**` casa com a
   própria `/app/login`, e o script segue achando que logou.
2. **Os gráficos entram por animação do recharts.** Sem `waitForTimeout(1500)`
   o screenshot sai antes deles.
3. **Máscara de centavos: os dígitos entram pela direita.** `fill('6800')` num
   campo de 2 casas vira **68,00**. Confira as casas no `TEntry` da tela — o
   cadastro de fórmula láctea usa `casas={3}`, então `500000` é que dá 500,000.
4. **Combo de paciente ≠ seletor de tenant.** O primeiro `input[placeholder]`
   visível é o **trocador de tenant** do topo. Mire pelo rótulo:
   `pg.locator('label:text-is("Paciente")').locator('xpath=following::input[1]')`.
5. **O rótulo do `<option>` leva a composição junto** — "NAN 2 (73,8 kcal · 1,65
   g / 100 ml)". `selectOption({ label })` falha; case por conteúdo:
   ```js
   await pg.evaluate((txt) => {
     for (const s of document.querySelectorAll('select'))
       for (const o of s.options)
         if (o.textContent.includes(txt)) {
           s.value = o.value
           s.dispatchEvent(new Event('change', { bubbles: true }))
           return o.textContent
         }
   }, 'Nutren Just Protein')
   ```
6. **Campo invisível é campo de outra aba.** As calculadoras são em abas; clique
   nela antes (`button:has-text("Dieta enteral")`). O `fill` falha com "element
   is not visible" e o motivo é esse.
7. **O rate limit responde 429** se você varrer muitas rotas seguidas. É o
   Bucket4j funcionando, não defeito — espace as navegações.

## 4. Conferir a impressão

A folha só existe no `documento` da `TPage`. Gere PDF, que é como o papel sai:

```js
await pg.pdf({ path: '/tmp/folha.pdf', format: 'A4', printBackground: true })
```

Depois **ler o PDF com a ferramenta Read** (`pages: "1-3"`). É assim que se
enxerga página em branco, quebra ruim, traço mudo e legenda que não fecha com o
número ao lado.

Para uma lista, a folha só monta depois de clicar em **Relatório** (que refaz a
consulta sem paginação) — espere ~2,5 s antes do `pdf()`.

## 5. Responsividade — regra 6 do projeto

Nenhuma tela é dada por pronta sem 360 px. O que se mede é a rolagem
horizontal, e vale identificar **o elemento culpado**, não só o sintoma:

```js
await pg.setViewportSize({ width: 360, height: 900 })
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
  return { rolagem: d.scrollWidth - d.clientWidth, culpado }
})
```

## 6. Dado de teste

O banco de desenvolvimento é `nutridb`, e às vezes a tabela que você quer olhar
está vazia. Criar pelo **endpoint**, com o token do login, é mais rápido e mais
fiel que pela tela:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ohlweilereduardo@gmail.com","senha":"Admin123@Nutri"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
```

**Limpe o que criar.** São dados que aparecem no catálogo e nas listas da
nutricionista. `PGPASSWORD=admin psql -h localhost -U postgres -d nutridb`, e
apague só as linhas que você inseriu, por nome ou por data.

> As credenciais acima são do `.env` de desenvolvimento local e servem só para
> entrar no `localhost`. Não vão para lugar nenhum fora desta máquina.

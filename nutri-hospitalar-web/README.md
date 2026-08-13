# Nutri Hospitalar de Sucesso — Web

Frontend da calculadora nutricional hospitalar.
React 19 · TypeScript · Vite · Tailwind v4.

Como rodar o sistema todo: [README da raiz](../README.md)
Padrões de código: [docs/04-arquitetura-frontend.md](../docs/04-arquitetura-frontend.md)
Cores, fontes e marca: [docs/05-identidade-visual.md](../docs/05-identidade-visual.md)

---

## Rodar

```bash
npm install          # só na primeira vez
cp .env.example .env.local
npm run dev          # http://localhost:5173
```

O backend precisa estar no ar em `http://localhost:8080`. O Vite faz proxy de
`/api` para lá (ver [vite.config.ts](vite.config.ts)), então o front chama a
própria origem e não há CORS em desenvolvimento.

| Comando | O que faz |
|---|---|
| `npm run dev` | dev server com HMR em `:5173` |
| `npm run build` | `tsc -b` + build de produção em `dist/` |
| `npm run preview` | serve o `dist/` em `:4173` |
| `npm run lint` | oxlint |

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `VITE_API_URL` | não em dev | base das chamadas à API. Dev: `/api` (proxy). Prod: a URL real. |

Só o prefixo `VITE_` chega ao navegador — e tudo que chega vai no bundle, à vista
de qualquer um. **Nunca** coloque segredo aqui.

---

## Stack

| Item | Escolha |
|---|---|
| Estilo | Tailwind v4 com `@theme` — tokens em [src/styles/theme.css](src/styles/theme.css) |
| Rotas | react-router-dom v7 |
| HTTP | axios, sempre via `services/api.ts` |
| Formulários | react-hook-form + zod |
| Notificação | react-toastify — nunca `alert()` |
| Ícones | SVG inline em `assets/icons` — um módulo, só o que se usa |
| Fontes | IBM Plex Sans e Lato, self-hosted via `@fontsource` |
| SVG | `vite-plugin-svgr` — `import Logo from './logo.svg?react'` |

Sem biblioteca de componentes de terceiros (a biblioteca `T*` é interna), sem
Redux/Zustand (Context API dá conta deste porte) e sem React Query nesta fase.

## Tema

Claro e escuro por `data-theme` no `<html>`, persistido em `localStorage`
(`nutri-tema`), com `prefers-color-scheme` como valor inicial. Um script inline no
[index.html](index.html) aplica o tema **antes** do React montar, evitando o flash
branco ao carregar no escuro.

Cor, fonte, raio e sombra vêm sempre dos tokens. **Nenhum hex solto no JSX.**

```tsx
<div className="rounded-lg bg-surface p-6 text-txt shadow-card">
```

## Sessão e tenant

Três decisões que custaram depuração e não devem ser desfeitas sem motivo.

**Access token em memória, refresh no `localStorage`.**
O que está no storage é legível por qualquer script da página; um XSS levaria a
sessão. O custo é que o F5 perde o access token — o boot o recupera com o
refresh, em `AuthContext`.

**Uma trava só de renovação: `renovarSessao()` em `services/api.ts`.**
O refresh é de uso único com rotação, e reapresentar um token consumido faz o
backend revogar a cadeia inteira, tratando como roubo. Duas chamadas
concorrentes derrubam a própria sessão — e o `StrictMode` monta efeitos duas
vezes em desenvolvimento, então isso acontece de verdade. O interceptor e o
boot compartilham a mesma promessa.

**Nunca recarregar a página ao trocar de tenant.**
`navigate(0)` e `location.assign` descartam o access token em memória. O boot
restaura pelo refresh token, que pertence à sessão do tenant de **origem**, e o
usuário volta de onde saiu. Não é preciso: o `AuthContext` já atualizou a
sessão, e as listas refazem a consulta porque têm `sessao.tenantId` nas
dependências — como sinal de invalidação, não como argumento.

**Ler pode ser global; escrever é dentro do tenant.** `GET /usuarios/global`
atravessa clientes, mas `GET`/`PUT /usuarios/{id}` filtram pelo tenant efetivo.
Por isso abrir um usuário de outro cliente na lista **entra naquele tenant
antes** — com toast avisando e a faixa amarela confirmando.

## A marca

`src/assets/brand/` — SVGs limpos extraídos do export do Figma:

| Arquivo | Uso |
|---|---|
| `logo-full.svg` | marca + tagline — header, login, impressão |
| `logo-mark.svg` | só "Nutri" — header no mobile, sidebar recolhida |
| `logo-simbolo.svg` | estetoscópio + folha — marca d'água, ilustração |

Os três usam `fill="currentColor"`, então **a cor vem do CSS** e o mesmo arquivo
serve os dois temas:

```tsx
import LogoFull from '@/assets/brand/logo-full.svg?react'

<LogoFull className="h-8 text-txt" />
```

Favicon e ícones de app estão em `public/`. O favicon é o "N" do logo sobre um
chip navy — o estetoscópio tem traço fino e desaparece abaixo de 32px.

## Componentes

Biblioteca interna em `src/components/common/`. **Nunca criar input, select,
grid ou modal novo quando já existe equivalente.**

| Componente | Papel |
|---|---|
| `TPage` | container de tela: título, subtítulo, ações |
| `TPanel` | card — sombra no claro, borda no escuro |
| `TDataGrid` | tabela no desktop, **cartões no mobile**; skeleton e estado vazio |
| `TDataGridFooter` | paginação |
| `TEntry` | campo de texto, com rótulo, erro, ajuda e sufixo |
| `TSelect` | `<select>` nativo — lista fixa e curta |
| `TCombo` | select **com busca no servidor** — quando a lista cresce |
| `TButton` | primary · secondary · ghost · danger |
| `TBadge` | status, sempre com rótulo — cor não é o único portador |
| `TThemeToggle` | alterna claro/escuro |

`src/components/layout/`: `Layout` (header + conteúdo), `Sidebar` (fixa em `lg`,
drawer abaixo), `TenantSwitcher` (só superadmin) e `TProtected` (guarda de rota).

### Regras que evitam bug

- **`TSelect` para lista fixa e curta; `TCombo` quando cresce.** Cliente,
  paciente e fórmula pedem busca.
- **Token de tema, não cor fixa.** `hover:bg-surface-alt`, nunca
  `hover:bg-brand-50` — a escala `brand-*` não vira com o tema e ficaria branca
  no escuro. Botão cheio usa `text-txt-inverse`, nunca `text-white`.
- **Nada de recarregar a página ao trocar de tenant.** Ver a seção de sessão.

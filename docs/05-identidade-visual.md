# Identidade Visual — Nutri Hospitalar de Sucesso

> Fonte: arquivo Figma **`SILVIA RITA ZAPPANI`**
> (`figma.com/design/ZbP49XOaICj7NAbIRmyEJ1/SILVIA-RITA-ZAPPANI`), página
> *Conceito Visual*. Referência local: `Captura de tela de 2026-08-10 22-43-51.png`.
>
> Os hexadecimais vêm de **`paletadecores.svg`**, exportado do Figma — fonte de
> verdade, não amostragem. O logo vem de **`logo.svg`**, do mesmo export.

---

## 1. Direção

**Delicado, suave, arejado.** Um sistema de nutrição hospitalar — adulto e
pediátrico — usado por profissionais de saúde em jornadas longas. A interface
precisa ser calma, não corporativa; clínica, não fria; cuidadosa, não infantil.

Cinco decisões que produzem esse tom:

1. **Peso invertido.** A sidebar é clara, não navy. O `#000F27` fica reservado ao
   texto e ao logo. A tela inteira respira em off-white.
2. **Cantos generosos.** Raio de 12 a 20px em cards e painéis. Nada de quina viva.
3. **Sombra difusa, borda quase invisível.** Elevação por sombra suave e larga,
   nunca por borda escura.
4. **Tipografia leve.** Títulos em 500/600, nunca 700. Corpo em 400.
5. **Dois acentos suaves e dessaturados** — verde-sálvia para nutrição, pêssego
   para o contexto pediátrico — usados com parcimônia, sobre a base azul da marca.

O que **não** fazer: gradiente colorido, cor saturada em área grande, ilustração
infantil, emoji, sombra dura, borda de 2px, `font-weight: 800`.

---

## 2. A marca

Logotipo **"Nutri"** em manuscrito, com o "u" desenhado como o tubo de um
**estetoscópio** — a oliva forma o ponto do "i" — e uma **folha** de duas pontas
saindo do "i". Ao lado, em sans-serif bold, a tagline em duas linhas:
**"Hospitalar / de Sucesso"**.

A folha do logo é a origem do verde-sálvia da paleta. O estetoscópio é a origem
do azul. A identidade do sistema é a mesma síntese: clínica + nutrição.

### Regras de aplicação

| Situação | Versão |
|---|---|
| Sidebar e fundo claro | logo em `#000F27` |
| Fundo azul-suave (`#BBD1F3`) | logo em `#000F27` |
| Fundo escuro / tema escuro | logo em `#FAF6FE` |
| Monocromático | uma cor só, sem gradiente |

- Área de respiro mínima: a altura da letra "N" em todos os lados.
- Largura mínima: 120 px em tela, 30 mm impresso.
- Nunca distorcer, rotacionar, aplicar sombra, contorno ou trocar as cores.
- Nunca aplicar sobre foto sem camada de contraste sólida por baixo.

### Arquivos da marca

Extraídos do `logo.svg` exportado do Figma. O export original tinha 15,7 MB — era
o slide inteiro, com uma foto JPEG de 11 MB embutida em base64. Os 8 `<path>` do
logo foram isolados num SVG limpo.

`nutri-hospitalar-web/src/assets/brand/`

| Arquivo | Conteúdo | Uso |
|---|---|---|
| `logo-full.svg` | marca + tagline · 23 KB | header, tela de login, impressão |
| `logo-mark.svg` | só "Nutri" com estetoscópio e folha · 6 KB | sidebar recolhida |
| `logo-simbolo.svg` | estetoscópio + folha, quadrado · 5 KB | marca d'água, ilustração |

Os três usam **`fill="currentColor"`**: a cor vem do CSS, então o mesmo arquivo
serve no tema claro e no escuro. Não existe versão "clara" separada.

```tsx
<LogoFull className="h-8 text-navy dark:text-offwhite" />
```

`nutri-hospitalar-web/public/`

| Arquivo | Uso |
|---|---|
| `favicon.svg` · `favicon-32.png` · `favicon-16.png` | aba do navegador |
| `apple-touch-icon.png` (180) · `icon-512.png` | atalho em mobile, PWA |
| `og-image.png` (1200×630) | prévia em link compartilhado |

O favicon é o **"N"** do logo em off-white sobre um chip navy arredondado. O
estetoscópio foi descartado nesse tamanho: o traço é fino e some abaixo de 32px.
O "N" continua legível a 16px.

---

## 3. Paleta

### Cores da marca

| Token | Hex | Papel |
|---|---|---|
| `navy` | **`#000F27`** | texto principal, logo |
| `blue` | **`#1E4686`** | **primária** — botões, links, foco, item ativo |
| `blue-muted` | **`#5F7FB1`** | ícone secundário, placeholder |
| `blue-soft` | **`#BBD1F3`** | badge, realce, ilustração |
| `offwhite` | **`#FAF6FE`** | canvas da aplicação |
| `silver` | **`#DADADA`** | divisor, superfície neutra |
| `mist` | **`#E9F0F2`** | fundo de apresentação da marca |

O quadro "Prata" do Figma é um **gradiente metálico horizontal**:

```css
background: linear-gradient(90deg, #9D9D9D 0%, #DADADA 52.4%, #9D9D9D 100%);
```

Reservado a detalhes de marca (cabeçalho de relatório impresso). Nunca em área de
leitura.

### Escala do azul

Interpolada a partir dos quatro azuis da marca, que ocupam os degraus 200, 400,
700 e 950. Os intermediários existem para estado — não inventam matiz.

| Degrau | Hex | Uso |
|---|---|---|
| `50` | `#F4F8FD` | canvas alternativo, zebra de grid |
| `100` | `#E6EEFA` | **item ativo da sidebar**, hover de linha |
| **`200`** | **`#BBD1F3`** | marca — badge, realce |
| `300` | `#8DA8D2` | borda em foco suave, texto secundário no escuro |
| **`400`** | **`#5F7FB1`** | marca — ícone secundário, placeholder |
| `500` | `#3F639C` | **texto secundário** (AA) |
| `600` | `#2E5491` | hover do botão primário |
| **`700`** | **`#1E4686`** | marca — **primária** |
| `800` | `#0F2A56` | active do botão primário |
| `900` | `#071C3E` | azul profundo — degrau da escala, não é a superfície do tema escuro |
| **`950`** | **`#000F27`** | marca — texto principal |

### Acentos de contexto

Dessaturados de propósito. Marcam **contexto**, não hierarquia.

| Contexto | Fill | Fundo | Texto (AA) | Contraste do texto |
|---|---|---|---|---|
| **Nutrição / adulto** | `#7FA88F` sálvia | `#EDF3EE` | `#4A7259` | 5,47:1 sobre branco |
| **Pediatria** | `#E0A188` pêssego | `#FAF0EB` | `#9E5334` | 5,61:1 sobre branco |

Onde aparecem:

- Faixa de contexto no topo da ficha do paciente (adulto × pediátrico)
- Ícone e badge do módulo na sidebar
- Borda esquerda de 3px no painel de resultados do módulo correspondente
- Marcador de linha no grid, quando adulto e pediátrico convivem na mesma lista

Onde **não** aparecem: botão, link, fundo de página, header, campo de formulário.
Ação continua sendo azul `#1E4686` em todo o sistema — dois módulos, uma gramática
de interação.

> `#7FA88F` (2,66:1) e `#E0A188` (2,18:1) são **cores de preenchimento**, não de
> texto. Texto sobre eles usa as variantes escuras `#4A7259` e `#9E5334`.

### Cores semânticas

| Papel | Claro | Sobre `#FFFFFF` | Escuro | Sobre `#0B2145` |
|---|---|---|---|---|
| Sucesso | `#0E7C5A` | 5,19:1 | `#46D3A5` | 8,44:1 |
| Alerta | `#B45309` | 5,03:1 | `#F5B44E` | 8,74:1 |
| Erro | `#B42318` | 6,57:1 | `#F58279` | 6,34:1 |
| Informação | `#0B6E8F` | 5,76:1 | `#6FC1E3` | 7,90:1 |

Sucesso é verde-escuro, distinto do sálvia de contexto — as duas informações são
diferentes e não podem se confundir.

### Acessibilidade

| Combinação | Razão | Veredito |
|---|---|---|
| `#000F27` sobre `#FFFFFF` | **19,13:1** | AAA — texto principal |
| `#1E4686` sobre `#FFFFFF` | **9,23:1** | AAA — link, botão primário |
| `#3F639C` sobre `#FFFFFF` | **6,04:1** | AA — texto secundário |
| `#4A7259` sobre `#EDF3EE` | **4,86:1** | AA — texto em contexto adulto |
| `#9E5334` sobre `#FAF0EB` | **5,01:1** | AA — texto em contexto pediátrico |
| `#5F7FB1` sobre `#FFFFFF` | **4,07:1** | ⚠️ só ícone, placeholder e texto ≥18,66px bold |
| `#F2F6FC` sobre `#0B2145` | **14,69:1** | AAA — texto no tema escuro |
| `#9DB4DA` sobre `#0B2145` | **7,57:1** | AA — secundário no escuro |
| branco sobre `#5F8FD8` | **3,28:1** | ❌ **reprova** — por isso botão cheio usa `--txt-inverse` |
| `#00091A` sobre `#5F8FD8` | **6,08:1** | AA — texto do botão no escuro |

**`#5F7FB1` nunca é cor de texto corrido.** Texto secundário é `#3F639C` no tema
claro e `#8DA8D2` no escuro.

Cor nunca é o único portador de informação: status vem sempre com ícone ou rótulo.

---

## 4. Tipografia

| Papel | Fonte | Origem |
|---|---|---|
| **Primária** — UI, títulos, números | **IBM Plex Sans** | `@fontsource/ibm-plex-sans` |
| **Secundária** — texto longo, laudos, impressão | **Lato** | `@fontsource/lato` |

Self-hosted via `@fontsource` — o CSP bloqueia host externo, e é uma requisição de
rede a menos.

Pesos: IBM Plex Sans 400, 500, 600 · Lato 400, 700.
**Sem 700 na interface.** Peso alto endurece a tela; o destaque vem de tamanho,
cor e espaço.

### Escala

| Token | Tamanho / altura | Peso | Uso |
|---|---|---|---|
| `display` | 30 / 40 px | 500 | título do dashboard |
| `h1` | 24 / 32 px | 500 | título de tela |
| `h2` | 18 / 26 px | 500 | título de `TPanel` |
| `h3` | 15 / 22 px | 500 | subtítulo, cabeçalho de grid |
| `body` | 14 / 22 px | 400 | padrão da interface |
| `body-lg` | 16 / 26 px | 400 | texto longo (Lato) |
| `caption` | 12 / 18 px | 400 | rótulo de campo, ajuda |
| `numeric` | 15 / 22 px | 500 | resultado de cálculo |

Entrelinha mais generosa que o usual (22px para corpo de 14px) — é o que faz a
tela parecer calma mesmo cheia de campos.

Resultado de cálculo e coluna numérica usam `font-variant-numeric: tabular-nums`;
sem isso os dígitos dançam de linha em linha.

Rótulo de campo em `caption`, `#3F639C`, sem caixa alta. Caixa alta em rótulo
endurece e prejudica a leitura de siglas clínicas (CB, AJ, VCT).

---

## 5. Tokens Tailwind v4

`src/styles/theme.css`, importado antes de tudo em `main.tsx`.

```css
@import "tailwindcss";

@theme {
  /* ── Marca ─────────────────────────────────────────────────── */
  --color-navy:       #000F27;
  --color-blue:       #1E4686;
  --color-blue-muted: #5F7FB1;
  --color-blue-soft:  #BBD1F3;
  --color-offwhite:   #FAF6FE;
  --color-silver:     #DADADA;
  --color-mist:       #E9F0F2;

  /* ── Escala do azul ────────────────────────────────────────── */
  --color-brand-50:  #F4F8FD;
  --color-brand-100: #E6EEFA;
  --color-brand-200: #BBD1F3;
  --color-brand-300: #8DA8D2;
  --color-brand-400: #5F7FB1;
  --color-brand-500: #3F639C;
  --color-brand-600: #2E5491;
  --color-brand-700: #1E4686;
  --color-brand-800: #0F2A56;
  --color-brand-900: #071C3E;
  --color-brand-950: #000F27;

  /* ── Acentos de contexto ───────────────────────────────────── */
  --color-sage:        #7FA88F;   /* nutrição / adulto — preenchimento */
  --color-sage-bg:     #EDF3EE;
  --color-sage-text:   #4A7259;
  --color-peach:       #E0A188;   /* pediatria — preenchimento */
  --color-peach-bg:    #FAF0EB;
  --color-peach-text:  #9E5334;

  /* ── Prata ─────────────────────────────────────────────────── */
  --color-silver-100: #F2F2F2;
  --color-silver-200: #E8E8E8;
  --color-silver-300: #DADADA;
  --color-silver-500: #9D9D9D;
  --color-mist:       #E9F0F2;

  /* ── Tipografia ────────────────────────────────────────────── */
  --font-sans:  "IBM Plex Sans", ui-sans-serif, system-ui, sans-serif;
  --font-serif: "Lato", ui-sans-serif, system-ui, sans-serif;

  --text-display: 1.875rem;  --text-display--line-height: 2.5rem;
  --text-h1:      1.5rem;    --text-h1--line-height:      2rem;
  --text-h2:      1.125rem;  --text-h2--line-height:      1.625rem;
  --text-h3:      0.9375rem; --text-h3--line-height:      1.375rem;
  --text-body:    0.875rem;  --text-body--line-height:    1.375rem;
  --text-caption: 0.75rem;   --text-caption--line-height: 1.125rem;

  /* ── Raio — generoso ───────────────────────────────────────── */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-pill: 9999px;

  /* ── Sombra — difusa, nunca dura ───────────────────────────── */
  --shadow-xs:    0 1px 2px rgb(0 15 39 / 0.04);
  --shadow-card:  0 1px 3px rgb(0 15 39 / 0.04), 0 6px 20px rgb(0 15 39 / 0.05);
  --shadow-hover: 0 2px 6px rgb(0 15 39 / 0.06), 0 12px 32px rgb(0 15 39 / 0.08);
  --shadow-modal: 0 12px 48px rgb(0 15 39 / 0.16);
}

/* ── Tokens semânticos — TEMA CLARO (padrão) ─────────────────── */
:root {
  --color-bg:             #FAF6FE;   /* canvas — off-white da marca */
  --color-surface:        #FFFFFF;   /* card, painel */
  --color-surface-alt:    #F4F8FD;   /* zebra, header de grid */

  --color-sidebar:        #FFFFFF;
  --color-sidebar-text:   #3F639C;
  --color-sidebar-active-bg:   #E6EEFA;
  --color-sidebar-active-text: #1E4686;

  --color-text:           #000F27;
  --color-text-secondary: #3F639C;
  --color-text-muted:     #5F7FB1;   /* só rótulo/placeholder/ícone */
  --color-text-inverse:   #FFFFFF;

  --color-border:         #EAEEF6;   /* quase invisível — a elevação é a sombra */
  --color-border-strong:  #D5DDEB;
  --color-focus:          #1E4686;

  --color-primary:        #1E4686;
  --color-primary-hover:  #2E5491;
  --color-primary-active: #0F2A56;

  --color-success: #0E7C5A;  --color-success-bg: #E6F4EF;
  --color-warning: #B45309;  --color-warning-bg: #FDF3E6;
  --color-danger:  #B42318;  --color-danger-bg:  #FCECEA;
  --color-info:    #0B6E8F;  --color-info-bg:    #E6F2F7;
}

/* ── Tokens semânticos — TEMA ESCURO ─────────────────────────── */
/* Opção de conforto, não o padrão. Degraus REAIS de superfície: a primeira
   versão usava #071C3E sobre #000F27 (razão 1,13) e virava uma mancha
   chapada. Agora canvas → sidebar → surface → surface-alt sobem de verdade. */
[data-theme="dark"] {
  --bg:             #00091A;   /* canvas, mais fundo que o navy da marca */
  --surface:        #0B2145;   /* card e header — 1,25 sobre o canvas */
  --surface-alt:    #12305C;   /* zebra, header de grid, hover */

  --sidebar:        #061631;
  --sidebar-text:   #8DA8D2;
  --sidebar-active-bg:   #16345F;
  --sidebar-active-text: #BBD1F3;

  --txt:            #F2F6FC;
  --txt-secondary:  #9DB4DA;
  --txt-muted:      #6B87B8;
  --txt-inverse:    #00091A;   /* texto dos botões cheios — ver nota abaixo */

  --line:           #16305A;   /* desenha a aresta do card: sombra não aparece aqui */
  --line-strong:    #24487E;

  --primary:        #5F8FD8;
  --primary-hover:  #7BA5E4;
  --primary-active: #4A78BE;

  --success: #46D3A5;  --success-bg: #0A3A2E;
  --warning: #F5B44E;  --warning-bg: #402C0C;
  --danger:  #F58279;  --danger-bg:  #451915;
  --info:    #6FC1E3;  --info-bg:    #0A3242;

  --color-sage:  #6E9A80;  --color-sage-bg:  #10281C;  --color-sage-text:  #9CC4AA;
  --color-peach: #C98D76;  --color-peach-bg: #30201A;  --color-peach-text: #E8B79F;
}

body {
  background-color: var(--color-bg);
  color: var(--color-text);
  font-family: var(--font-sans);
  font-size: var(--text-body);
  line-height: var(--text-body--line-height);
  -webkit-font-smoothing: antialiased;
}

.numeric, td.numeric, .resultado-calculo {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}
```

Tema alternado por `data-theme` no `<html>`, persistido em `localStorage` pelo
`ThemeContext`, com `prefers-color-scheme` como valor inicial.

---

## 6. Aplicação nos componentes

| Componente | Especificação |
|---|---|
| **Sidebar** | fundo `--color-sidebar` (branco), largura 248px, borda direita `--color-border`. Item: texto `--color-sidebar-text`, ícone `--color-text-muted`, altura 40px, `--radius-md`, margem lateral de 8px. **Item ativo:** fundo `--color-sidebar-active-bg`, texto e ícone `--color-sidebar-active-text`, peso 500 — pílula arredondada, sem barra lateral dura. Grupos separados por rótulo em `caption` e `--color-text-muted`. |
Cada token aparece em duas formas equivalentes: `--surface` é o valor que troca
no tema, e `--color-surface` é como o `@theme` do Tailwind o expõe — no JSX você
escreve a classe, `bg-surface`.

| **Header** | 60px, fundo `--color-surface`, borda inferior `--color-border`, logo à esquerda, seletor de tenant (só `SUPERADMIN`) e menu de usuário à direita |
| **`TPage`** | padding 24px (16px no mobile), título em `h1`, ações à direita, breadcrumb em `caption` |
| **`TPanel`** | fundo `--color-surface`, `--radius-lg`, `--shadow-card`, **sem borda**, padding 20px; título em `h2` com 16px abaixo |
| **`TButton` primary** | fundo `--color-primary`, texto **`--color-txt-inverse`** (não branco — ver §Tokens fixos), `--radius-md`, altura 38px, padding 16px, peso 500, `transition: 150ms` |
| **`TButton` secondary** | fundo `--color-surface`, borda `--color-line-strong`, texto `--color-txt`, hover fundo **`--color-surface-alt`** |
| **`TButton` ghost** | sem fundo nem borda, texto `--color-primary`, hover fundo **`--color-surface-alt`** |
| **`TButton` danger** | fundo `--color-danger`, texto `--color-txt-inverse` — só em exclusão confirmada |
| **`TEntry` / `TCombo`** | altura 38px, `--radius-md`, fundo `--color-surface`, borda `--color-border-strong`; foco borda `--color-focus` + `box-shadow: 0 0 0 3px rgb(30 70 134 / 0.10)` (halo suave, não outline duro); erro borda `--color-danger` + mensagem em `caption` |
| **`TDataGrid`** | sem borda externa; header fundo `--color-surface-alt`, texto `--color-txt-secondary` em `h3`; linha 44px; separador `--color-line`; hover **`--color-surface-alt`**; primeira e última linha com canto arredondado. Abaixo de `md` vira cartão — regra 6 do `CLAUDE.md` |
| **`TWindow` (modal)** ⏳ | overlay `rgb(0 15 39 / 0.35)` com `backdrop-filter: blur(2px)`, painel `--radius-xl`, `--shadow-modal` |
| **Badge** | `--radius-pill`, altura 22px, `caption`, fundo `--color-*-bg`, texto `--color-*`, sempre com ícone de 14px |
| **Badge de contexto** | adulto: fundo `--color-sage-bg`, texto `--color-sage-text`, ícone de folha. Pediátrico: fundo `--color-peach-bg`, texto `--color-peach-text`, ícone de bebê |
| **`TResult`** ⏳ | fundo `--color-surface`, borda esquerda 3px na cor do contexto (sálvia ou pêssego), `--radius-md`; valor em `numeric` 20px `--color-text`; unidade em `caption` `--color-text-muted`; classificação como badge; referência em `caption` |
| **Toast** | `--radius-md`, `--shadow-hover`, barra de progresso na cor semântica |
| **Estado vazio** | ícone de 40px em `--color-txt-muted`, título em `h3`, texto em `--color-txt-secondary`, ação em ghost — nunca uma tabela vazia sem explicação |
| **Loading** | *skeleton* em `--color-surface-alt` com pulso de 1,5s. Nunca spinner em tela cheia |

### Tokens fixos × tokens de tema

Distinção que já causou um bug: o hover do alternador de tema usava
`bg-brand-50` e, no tema escuro, ficava **branco**.

| Grupo | Muda com o tema? | Quando usar |
|---|---|---|
| `brand-*`, `navy`, `blue`, `offwhite`, `silver-*`, `sage`, `peach` | **não** — são a paleta da marca, valores fixos | ilustração, badge de contexto, detalhe de marca — onde a cor é a mesma nos dois temas |
| `bg`, `surface`, `surface-alt`, `txt*`, `line*`, `primary*`, `sidebar*` | **sim** | tudo que é superfície, texto, borda, estado e ação |

Regra prática: **se o elemento faz parte da interface, use token de tema.**
`hover:bg-surface-alt`, nunca `hover:bg-brand-50`. `text-txt-inverse` em botão
cheio, nunca `text-white` — no escuro a primária é clara e o branco reprova o
contraste (3,28:1 contra os 4,5:1 exigidos).

### Foco visível

O sistema é usado à beira do leito, com teclado e às vezes com luva.

```css
:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
  border-radius: var(--radius-sm);
}
```

Campos usam halo (`box-shadow`) em vez de outline, para não brigar com o raio.
Nunca `outline: none` sem substituto.

### Movimento

Transições de 150–200ms, `ease-out`. Só em cor, sombra e opacidade — nunca em
layout. E sempre:

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
```

---

## 7. Espaçamento e densidade

Escala de 4px: `4 · 8 · 12 · 16 · 20 · 24 · 32 · 48 · 64`.

Densidade **confortável** — um pouco mais folgada que um ERP, porque o tom é
delicado; mas ainda compacta o bastante para a tela de atendimento não exigir
rolagem constante.

| Elemento | Altura |
|---|---|
| Campo de formulário | 38 px |
| Botão | 38 px (32 px em `size="sm"`) |
| Linha de grid | 44 px |
| Header | 60 px |
| Sidebar expandida / recolhida | 248 px / 68 px |
| Espaço entre painéis | 20 px |
| Espaço entre campos na mesma linha | 16 px |

Ícones: **SVG inline**, traço de 1.8, em `src/assets/icons/index.tsx`. 20px na
interface, 18px na sidebar, 16px em botão pequeno. Todos usam `currentColor` e
herdam a cor do texto — nenhum componente define ícone fora desse módulo.

---

## 8. Impressão

Ficha do paciente e relatórios saem no papel.

**A folha não é a tela — é um documento próprio.** Imprimir o formulário dava
formulário: campos de entrada, rótulos de digitação, abas. Quem lê no papel
precisa de um **demonstrativo** — identificação no topo, tira de indicadores,
seções com o número já calculado e a sua procedência ao lado.

Cada tela imprimível monta uma `Folha` (`components/impressao/Folha.tsx`) e a
entrega à `TPage` pela prop `documento`. A `TPage` então marca **todo o conteúdo
de tela como `.nao-imprime`**: a folha é o que sai, e não há como uma tela ganhar
documento e continuar imprimindo o formulário por baixo dele.

**O desenho vem do eroERP** (`ero/src/utils/geradorPdf.ts`), que monta relatório
com jsPDF. A ferramenta não vem: jsPDF seria dependência nova, layout imperativo
em milímetros e — o que pesa — uma **segunda montagem** do mesmo documento, com
os números reescritos à mão. Duas montagens divergem, e a do papel envelhece
calada. Aqui o documento lê os mesmos objetos que a tela leu, com os mesmos
formatadores e os mesmos tokens.

Tela sem documento próprio ainda imprime como está, limpa da interface. É o
fallback de quem aperta Ctrl+P fora das telas de registro.

### 8.0 As peças da folha

| Peça | O que é |
|---|---|
| `Folha` | a folha: cabeçalho com logo, título, paciente, linha de referência e rodapé |
| `TiraIndicadores` | os cinco números do topo, em caixa — o que se lê primeiro |
| `Secao` | bloco com barra de título na cor da marca e nota de procedência |
| `LinhasDeValor` | rótulo → **valor** → detalhe (classificação, origem do número) |
| `TabelaDoc` | tabela de verdade, para série no tempo e listagem |
| `DocumentoLista` | relatório de uma listagem, **com os filtros aplicados escritos no cabeçalho** |

**O cabeçalho não é faixa cheia.** Um bloco navy sangrando de margem a margem
pesa na página, come tinta e faz o documento parecer papel timbrado de banco — o
oposto do que o sistema é na tela. A cor aparece em três lugares e só: o logo, o
filete sob o cabeçalho e o número dentro da tira de indicadores.

- **Fundo branco, e a paleta clara da marca por cima.** Tinta navy `#000F27`,
  cartão de canto arredondado com borda hairline `--line`, faixa de gráfico no
  seu próprio tom
- Corpo em **IBM Plex Sans** 10,5pt, entrelinha 1,45 — a mesma família da tela
- **Logo colorido** no cabeçalho de cada folha, com o título do documento e o
  paciente à direita
- Rodapé com "Documento gerado por Nutri Hospitalar de Sucesso"
- Sidebar, header da aplicação, botões, abas e filtros com `display: none`
- Tabelas com **linha de base** `--line` e o cabeçalho no seu fundo; sem grade
  fechada e sem zebra — o sistema não usa zebra em lugar nenhum
- Sombra sai: no papel ela vira mancha cinza, não profundidade
- `@page { margin: 15mm; }`

### 8.1 As duas coisas que a cor exige

1. **`print-color-adjust: exact` na árvore inteira.** Sem isso o Chrome
   descarta todo fundo e toda faixa de gráfico, e a folha sai com o desenho
   pela metade.
2. **O tema escuro vale só em tela.** O bloco `[data-theme="dark"]` de
   `theme.css` está envolvido em `@media screen`, e por isso o papel herda
   sempre a paleta clara. Sem esse envelope, quem trabalha no escuro imprimiria
   navy chapado — um cartucho por folha. Envolver custa uma linha; redeclarar os
   trinta tokens na regra de impressão seria a próxima coisa a divergir.

### 8.1.1 Quebra de página

**Só promete não quebrar quem é menor que a página.** `break-inside: avoid` numa
caixa mais alta que o A4 — a tabela de 18 dias de acompanhamento é — faz o
navegador empurrá-la inteira para a folha seguinte e deixar a anterior em
branco. A regra fica onde a unidade é pequena:

| Elemento | Regra | Por quê |
|---|---|---|
| `.folha-tira` | `break-inside: avoid` | cinco números que só significam algo juntos |
| `.folha-tabela tr` | `break-inside: avoid` | a maior unidade que cabe sempre |
| `.folha-secao-titulo` | `break-after: avoid` | título no pé com o conteúdo na folha seguinte é pior que quebrar a tabela |
| `.folha-tabela thead` | `display: table-header-group` | da segunda folha em diante seriam números sem nome de coluna |
| `.folha-rodape` | `break-before: avoid` | senão ele sozinho vira uma última folha |
| `.folha-secao` | **nenhuma** | é o que pode ser mais alto que a página |

### 8.2 O que não conseguimos entregar

**A numeração de página não é nossa.** Regras de margem paginada
(`@bottom-right { content: counter(page) }`) não são suportadas por nenhum
navegador; quem numera é o cabeçalho/rodapé do próprio diálogo de impressão,
ligado por padrão. O rodapé traz a identificação; o número vem de lá.

### 8.3 Listas: relatório e planilha

Toda listagem clínica oferece os dois, **no recorte que está na tela**:

- **Relatório** — `DocumentoLista`, com os filtros aplicados escritos no
  cabeçalho. Uma folha com 40 linhas e sem o recorte que as produziu é
  indefensável: quem a recebe não sabe se são todos os pacientes ou os de um mês.
- **Planilha** — CSV com BOM UTF-8 e ponto-e-vírgula, que abre no Excel em
  português com um duplo clique. XLSX exigiria ~1 MB de biblioteca para escrever
  um formato que aqui ninguém precisa.

Dois cuidados que não são detalhe:

1. **Os dois cobrem o filtro inteiro, não a página visível.** Exportar "os 20 da
   página 1" seria a armadilha óbvia, e o usuário só descobriria ao conferir o
   total. Há um teto de 2.000 linhas, e quando ele corta o relatório **diz
   quantas ficaram de fora** — truncar em silêncio faz a exportação parecer
   completa sem ser.
2. **Injeção de fórmula no CSV.** Célula que começa com `=`, `+`, `-` ou `@` é
   executada ao abrir a planilha. Nome de paciente é texto que o usuário digitou;
   `utils/planilha.ts` prefixa com apóstrofo, como o OWASP recomenda.

As colunas do relatório e as da planilha são **as mesmas**: um só lugar define o
que é relevante naquela lista, e os dois não conseguem divergir.

> **Esta seção foi reescrita duas vezes.** A primeira versão mandava folha em
> preto e branco, logo monocromático, Lato 11pt e grade `#9D9D9D` — herança de
> relatório de sistema antigo. A segunda trocou por **cor, suavidade e o logo da
> marca**, mas ainda imprimia a tela. A terceira é esta: o papel passou a ser um
> **documento próprio**, porque imprimir o formulário devolvia formulário.

---

## 9. Checklist visual

- [ ] Nenhuma cor fora dos tokens — zero hex solto no JSX ou no CSS de componente
- [ ] Texto corrido em `--color-text` ou `--color-text-secondary`, nunca `--color-text-muted`
- [ ] Sálvia e pêssego só em contexto (faixa, badge, borda de resultado) — nunca em botão ou link
- [ ] Nenhum peso de fonte acima de 600
- [ ] Painéis com sombra difusa, sem borda dura
- [ ] Raio ≥ 12px em card, painel e modal
- [ ] Funciona em tema claro **e** escuro
- [ ] Foco de teclado visível em todo elemento interativo
- [ ] `prefers-reduced-motion` respeitado
- [ ] Números em `.numeric` (tabular)
- [ ] Ícones vindos de `assets/icons`
- [ ] Estado vazio e skeleton de carregamento previstos
- [ ] Status com ícone ou rótulo, não só cor
- [ ] Legível em 1280×720 sem rolagem horizontal

---

## 10. Paleta de gráficos

Cor de gráfico **não é escolhida a olho** — é validada. Cada cor faz exatamente um
trabalho, e o conjunto passa por checagens computáveis (banda de luminosidade,
piso de croma, separação sob daltonismo, contraste sobre a superfície).

**As duas superfícies deste sistema exigem validação própria.** A escura é
`#0B2145`, azul-marinho e não cinza — uma série azul que funciona sobre cinza
pode sumir sobre ela. Toda paleta abaixo foi validada contra `#FFFFFF` e
`#0B2145`, não contra superfícies genéricas.

### 10.1 Categórica — identidade de série

Para gráficos com séries distintas (cobertura calórica × proteica, oferta ×
necessidade). Ordem fixa, atribuída em sequência, **nunca ciclada**.

| Slot | Claro | Escuro |
|---|---|---|
| 1 | `#2a78d6` | `#3987e5` |
| 2 | `#eb6834` | `#d95926` |
| 3 | `#1baf7a` | `#199e70` |

Os três passam todos os pares em ambos os temas. O slot 3 fica em 2,82:1 no tema
claro — abaixo de 3:1 —, então **exige rótulo visível ou tabela equivalente**, que
as telas já trazem.

### 10.2 Classificação da OMS — BAIXA · ADEQUADA · ALTA

Usa os três slots categóricos: **azul → verde → laranja**. Frio de um lado,
quente do outro, adequado no meio — lê-se como escala ordenada sem que "adequado"
vire cinza de "nada", que é o que ele não é.

> ⚠️ **Os tokens `success`/`warning`/`info` do sistema NÃO servem para gráfico.**
> `info` (`#0B6E8F`) e `success` (`#0E7C5A`) ficam a ΔE 10,7 — abaixo do piso 15
> — e `info` tem croma 0,096, abaixo do piso: lê como cinza. Como **texto
> rotulado** eles funcionam e continuam em uso no `TResult`; como duas fatias
> vizinhas num gráfico, colapsam. Medido, não suposto.

### 10.3 Sequencial — faixas de percentil da OMS

Uma cor só, claro → escuro. As faixas são **contexto**, não protagonista: a linha
do paciente é que carrega a informação.

| Faixa | Claro | Escuro |
|---|---|---|
| P3–P97 (externa, situa o extremo) | `#cde2fb` | `#12305C` |
| P15–P85 (interna, é a que classifica) | `#9ec5f4` | `#24487E` |

No tema escuro a faixa usa os degraus de navy do próprio tema (`surface-alt` e
`line-strong`): sobre `#0B2145`, um azul claro brigaria com a linha do paciente.

**O ponto do paciente não é colorido por classificação.** A posição dele em
relação à faixa já diz se está adequado — colorir também seria gastar o canal de
cor com o que o gráfico já mostra. A classificação aparece por escrito, no
tooltip.

### 10.4 Ordinal — faixas etárias

Ordem que importa (0–6 m, 6–12 m, …) pede rampa de uma cor só, com degraus
visíveis. **Cinco degraus**, porque seis não mantêm o intervalo mínimo de
luminosidade entre vizinhos:

| Tema | Degraus |
|---|---|
| Claro | `#86b6ef` `#3987e5` `#256abf` `#184f95` `#0d366b` |
| Escuro | `#1c5cab` `#3987e5` `#6da7ec` `#9ec5f4` `#cde2fb` |

No escuro a âncora inverte — mais é **mais claro** — porque sobre navy o extremo
escuro encosta na superfície.

A faixa "acima de 60 meses" fica **fora da rampa**, em cinza de texto: ela está
fora do alcance das curvas da OMS, e a cor deve dizer isso.

### 10.5 Formas que este sistema não usa

- **Dois eixos Y no mesmo gráfico.** O alinhamento entre as duas escalas é
  arbitrário e inventa correlação. Duas medidas de escala diferente = dois
  gráficos.
- **Pizza para comparar valores próximos**, ou de duas fatias. Distribuição de
  sexo é cartão de indicador; classificação é barra empilhada.
- **Rampa de valor em categoria sem ordem.** Colorir fórmula láctea por
  quantidade duplica o que o comprimento da barra já diz. Categoria nominal =
  uma cor só para todas as barras.
- **Número em todo ponto.** Rótulo é seletivo — o extremo, a ponta da série.
- **Grade tracejada.** Tracejado significa limiar (a linha dos 100 % de
  adequação, a necessidade energética), não grade.

### 10.6 Obrigatório em todo gráfico

- Legenda sempre que houver 2 ou mais séries; uma série só dispensa, o título nomeia.
- **Tabela equivalente** para os mesmos dados — cor nunca é o único caminho.
- Ao recalcular, o desenho anterior fica esmaecido; não pisca nem some.
- Filtros numa linha só, **acima** de tudo o que eles filtram — nunca dentro do card.
- Altura do container inclui a faixa do eixo X, senão o card ganha rolagem interna.

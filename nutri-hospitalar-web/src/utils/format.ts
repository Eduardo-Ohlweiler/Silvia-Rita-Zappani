/**
 * Datas trafegam como string ISO e são formatadas só na exibição — nada de
 * `Date` no estado (docs/04-arquitetura-frontend).
 */

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
})

const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })

export function formatarDataHora(iso?: string | null): string {
  if (!iso) return '—'
  return DATA_HORA.format(new Date(iso))
}

/**
 * Data em pt-BR. Ausente vira travessão.
 *
 * <b>Um dia inteiro de diferença dependia desta função.</b> `new Date('2026-08-29')`
 * — data sem hora — é lida pelo JavaScript como **meia-noite UTC**, e o `Intl`
 * formata no fuso local. Em qualquer fuso a oeste de Greenwich, incluindo o
 * Brasil inteiro, isso volta o dia anterior: a avaliação de 07/08/2027 saía
 * impressa como 06/08/2027, em prontuário.
 *
 * O conserto é o mesmo idioma que `utils/idade.ts` já usava: montar a data com
 * `T00:00:00`, que o JavaScript lê como meia-noite **local**. Timestamp completo
 * (`createdAt`, com hora e fuso) não passa por aqui — ele é um instante real, e
 * formatá-lo no fuso do leitor é justamente o certo.
 */
export function formatarData(iso?: string | null): string {
  if (!iso) return '—'
  return DATA.format(paraDataLocal(iso))
}

/** `YYYY-MM-DD` é dia do calendário, não instante — vira meia-noite LOCAL. */
function paraDataLocal(iso: string): Date {
  return /^\d{4}-\d{2}-\d{2}$/.test(iso) ? new Date(`${iso}T00:00:00`) : new Date(iso)
}

/** Converte `<input type="datetime-local">` para ISO com fuso, ou undefined. */
export function paraIso(valor?: string): string | undefined {
  if (!valor) return undefined
  return new Date(valor).toISOString()
}

/** CPF ou CNPJ com pontuação, a partir do valor cru que o backend guarda. */
export function formatarDocumento(documento?: string | null): string {
  if (!documento) return '—'
  const digitos = documento.replace(/\D/g, '')
  if (digitos.length === 11)
    return digitos.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
  if (digitos.length === 14)
    return digitos.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5')
  return documento
}

/**
 * Máscara enquanto se digita. Diferente de `formatarDocumento`, que só exibe:
 * aqui o valor é parcial a cada tecla, e pontuar o que já veio evita o efeito
 * de ver o campo "pular" só no último dígito.
 */
export function mascararCpf(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 11)
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
}

export function mascararCnpj(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 14)
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2')
}

export function mascararCep(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 8)
  return d.replace(/^(\d{5})(\d)/, '$1-$2')
}

export function mascararTelefone(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 10) return d.replace(/^(\d{2})(\d{4})(\d)/, '($1) $2-$3')
  return d.replace(/^(\d{2})(\d{5})(\d)/, '($1) $2-$3')
}

/** Só dígitos, ou `undefined` — é o que a API espera receber. */
export function somenteDigitos(valor?: string | null): string | undefined {
  if (!valor) return undefined
  const d = valor.replace(/\D/g, '')
  return d === '' ? undefined : d
}

export function formatarTelefone(codigoPais?: string | null, telefone?: string | null): string {
  if (!telefone) return '—'
  const digitos = telefone.replace(/\D/g, '')
  const nacional =
    digitos.length === 11
      ? `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`
      : digitos.length === 10
        ? `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`
        : telefone
  return codigoPais && codigoPais !== '55' ? `+${codigoPais} ${nacional}` : nacional
}

/**
 * Número em pt-BR, para exibição. Ausente vira travessão.
 *
 * `maximo` corta as casas sem inventar zeros: 723 sai "723" e 649,44 sai
 * "649,44", com a mesma chamada. Resultado de cálculo costuma ter escala 4 no
 * banco e nenhuma tela quer ver "723,0000".
 */
export function formatarNumero(
  valor?: number | null,
  maximo = 2,
  minimo = 0,
): string {
  if (valor === null || valor === undefined || Number.isNaN(valor)) return '—'
  return valor.toLocaleString('pt-BR', {
    minimumFractionDigits: minimo,
    maximumFractionDigits: maximo,
  })
}

/**
 * `1.234.567` e `1.500` são milhar. `0.750` e `72.5` não são — ver `paraNumero`.
 */
const PONTO_DE_MILHAR = /^-?(?:\d{1,3}(?:\.\d{3}){2,}|[1-9]\d{0,2}\.\d{3})$/

/**
 * Converte o que o usuário digitou em número, aceitando **vírgula ou ponto**
 * como separador decimal.
 *
 * O ponto é ambíguo em pt-BR: em `1.500` separa milhar, em `72,5` o teclado
 * numérico do celular manda `72.5` e ele é decimal. Tratar todo ponto como
 * milhar — que é o que esta função fazia — faz **`72.5` virar 725** e
 * **`0.75` virar 75**, em silêncio, em campo de peso que prescreve dieta.
 *
 * A ambiguidade se desfaz pelo formato, e a preferência é do decimal:
 *
 * | Digitado | Vira | Por quê |
 * |---|---|---|
 * | `1.234,56` | 1234,56 | tem vírgula: ela é o decimal, o ponto é milhar |
 * | `1.234.567` | 1234567 | mais de um ponto: todos são milhar |
 * | `1.500` | 1500 | 1 a 3 dígitos, ponto, exatamente 3 — e não começa em zero |
 * | `72.5` · `0.750` · `1.25` | 72,5 · 0,750 · 1,25 | qualquer outro ponto é decimal |
 */
export function paraNumero(valor?: string | null): number | undefined {
  if (valor === null || valor === undefined) return undefined
  const cru = valor.trim()
  if (cru === '') return undefined

  const limpo = cru.includes(',')
    ? cru.replace(/\./g, '').replace(',', '.')
    : PONTO_DE_MILHAR.test(cru)
      ? cru.replace(/\./g, '')
      : cru

  const numero = Number(limpo)
  return Number.isNaN(numero) ? undefined : numero
}

/**
 * Máscara numérica de **centavos**: os dígitos entram pela direita e as casas
 * decimais são fixas. Digitar `7` `2` `5` `0` com duas casas dá `72,50`.
 *
 * É a mecânica do eroERP (`numerodecimal2`), que é o hábito da casa — digita-se
 * só dígito e a vírgula se posiciona sozinha, sem caçar tecla de pontuação.
 *
 * **Vazio entra, vazio sai — e essa é a diferença que importa.** No eroERP,
 * `parseInt(digitos || "0") / 100` transforma "apaguei tudo" em `0,00`: um campo
 * opcional que o usuário limpou é enviado como **zero, não como ausente**. Isso
 * quebraria a invariante deste sistema, onde ausência carrega o motivo e um peso
 * em branco faz o cálculo dizer "informe o peso" em vez de devolver número. Lá o
 * defeito é visível a olho nu: existe um campo rotulado "vazio = sugerir" que a
 * máscara torna impossível de deixar vazio.
 *
 * @param casas    casas decimais fixas. `0` dá inteiro puro (PA, idade)
 * @param comSinal aceita `-` à frente. Só o balanço hídrico precisa — e é
 *                 justamente o que o eroERP não sabia fazer, porque a máscara
 *                 decimal de lá come o sinal no `replace(/\D/g, '')`
 */
export function mascararDecimal(valor: string, casas = 2, comSinal = false): string {
  const negativo = comSinal && valor.trimStart().startsWith('-')
  const digitos = valor.replace(/\D/g, '')

  if (digitos === '') return negativo ? '-' : ''

  const sinal = negativo ? '-' : ''
  if (casas === 0) return sinal + String(Number(digitos))

  // `Number` e não `parseInt`: acima de 15 dígitos o parseInt do eroERP
  // ultrapassa MAX_SAFE_INTEGER e perde precisão em silêncio.
  const numero = Number(digitos) / 10 ** casas
  return (
    sinal +
    numero.toLocaleString('pt-BR', {
      minimumFractionDigits: casas,
      maximumFractionDigits: casas,
    })
  )
}

/**
 * Número do servidor no formato que a máscara produz — é o que repovoa o campo
 * ao abrir um registro salvo.
 *
 * Sem isto, `72.5` voltaria como `"72,5"` (uma casa) e a primeira tecla digitada
 * o releria como os dígitos `725`, virando **`7,25`**. O campo tem de reabrir
 * exatamente como a máscara o teria escrito.
 */
export function textoDaMascara(valor?: number | null, casas = 2): string {
  if (valor === null || valor === undefined || Number.isNaN(valor)) return ''
  return formatarNumero(valor, casas, casas)
}

/**
 * Texto colado, relido como número e reformatado pela máscara.
 *
 * Sem isto, colar `"70"` num campo de duas casas dá **`0,70`**, porque o texto
 * passa pelo mesmo acumulador de dígitos — o eroERP erra assim, sem avisar, e
 * copiar valor de planilha é exatamente o que a nutricionista faz. Aqui o texto
 * passa antes por {@link paraNumero}, que já desambigua vírgula e ponto.
 */
export function mascararColado(texto: string, casas = 2, comSinal = false): string {
  const numero = paraNumero(texto)
  if (numero === undefined) return mascararDecimal(texto, casas, comSinal)

  const arredondado = comSinal ? numero : Math.abs(numero)
  return mascararDecimal(
    (arredondado < 0 ? '-' : '') + Math.abs(arredondado).toFixed(casas).replace('.', ''),
    casas,
    comSinal,
  )
}

/**
 * O rótulo legível de um valor de enum, a partir da lista de opções que a tela
 * já usa no combo.
 *
 * Existe porque o documento impresso vazava o nome cru do enum: a janela de
 * perda de peso saía como **"janela de um_mes"** no prontuário, em vez de
 * "1 mês". `UM_MES.toLowerCase()` é `um_mes` — a conversão parecia bastar e não
 * bastava.
 *
 * Devolve `undefined` quando não encontra, e **não** o valor cru: cair no
 * `SCREAMING_SNAKE_CASE` seria repetir em silêncio o defeito que esta função
 * existe para impedir. Quem chama decide o que mostrar no lugar.
 */
export function rotuloDe<T extends string>(
  opcoes: readonly { valor: T; rotulo: string }[],
  valor?: T | null,
): string | undefined {
  if (!valor) return undefined
  return opcoes.find((o) => o.valor === valor)?.rotulo
}

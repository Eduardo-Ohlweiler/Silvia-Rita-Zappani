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

export function formatarData(iso?: string | null): string {
  if (!iso) return '—'
  return DATA.format(new Date(iso))
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

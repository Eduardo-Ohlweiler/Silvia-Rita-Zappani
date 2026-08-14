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

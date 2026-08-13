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

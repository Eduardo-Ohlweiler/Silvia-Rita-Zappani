/**
 * Idade em anos completos entre o nascimento e uma data de referência.
 *
 * A referência é a **data da avaliação**, não hoje: reabrir um registro de três
 * meses atrás tem de mostrar a idade que o paciente tinha no dia, e não a de
 * agora. É o mesmo cuidado que o formulário da pediatria tem com os meses.
 */
export function idadeEmAnos(nascimento: string, referencia: string): number | undefined {
  const nasc = new Date(`${nascimento}T00:00:00`)
  const ref = new Date(`${referencia}T00:00:00`)
  if (Number.isNaN(nasc.getTime()) || Number.isNaN(ref.getTime())) return undefined

  let anos = ref.getFullYear() - nasc.getFullYear()
  const mes = ref.getMonth() - nasc.getMonth()
  if (mes < 0 || (mes === 0 && ref.getDate() < nasc.getDate())) anos -= 1

  return anos < 0 ? undefined : anos
}

/**
 * Faixa da classificação da OMS usada na pediatria — `docs/09 §4.2`.
 *
 * Acima dela o mês deixa de ser unidade de leitura: nenhuma tabela de percentil
 * chega ali.
 */
const LIMITE_OMS_MESES = 60

/**
 * Idade em meses, escrita para gente ler.
 *
 * **O mês é a unidade da pediatria**, e continua sendo: toda tabela da OMS, o
 * VET das DRIs e a necessidade proteica são indexados por mês, e converter para
 * anos obrigaria a nutricionista a desfazer a conta para conferir contra a
 * tabela. Por isso 0 a 60 meses saem em meses, exatamente como sempre saíram.
 *
 * O que muda é só **acima de 60 meses**, onde a classificação da OMS já não
 * vale e o número em meses vira ruído — um adulto lançado por engano na tela
 * pediátrica aparecia como "833 meses". Ali os anos entram **ao lado**, sem
 * tirar o mês de quem o usa.
 */
export function idadeEmMesesTexto(meses?: number | null): string {
  if (meses == null) return '—'

  const texto = `${meses} ${meses === 1 ? 'mês' : 'meses'}`
  if (meses <= LIMITE_OMS_MESES) return texto

  const anos = Math.floor(meses / 12)
  return `${texto} (${anos} ${anos === 1 ? 'ano' : 'anos'})`
}

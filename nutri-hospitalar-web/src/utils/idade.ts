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

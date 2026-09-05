/**
 * A tela inicial — a lista de trabalho do dia.
 *
 * Espelha `PainelInicialDto`. Os campos anuláveis são **opcionais**: o backend
 * serializa omitindo nulos, e a chave simplesmente não vem.
 */

export interface PendenteDoDia {
  pessoaId: string
  pessoaNome: string
  ultimoDia: string
  /** Dias desde o último registro. Número puro — a tela não o pinta. */
  diasSemRegistro: number
  avaliacaoId?: string | null
}

/** `diasDeTerapia` viaja junto porque sem ele o alerta mentiria metade do tempo. */
export interface AlertaAdesao {
  pessoaId: string
  pessoaNome: string
  adesaoMedia: number
  diasConsiderados: number
  diasDeTerapia: number
  avaliacaoId?: string | null
}

/** De → para, sem escrever "piorou": quem lê é quem julga. */
export interface MudancaDeFaixa {
  pacienteId: string
  pacienteNome: string
  dataAnterior: string
  dataAtual: string
  idadeMeses: number
  indice: string
  de: string
  para: string
  avaliacaoId: string
}

export interface SemAvaliacao {
  pacienteId: string
  pacienteNome: string
  modulo: string
  ultimaAvaliacao: string
  diasSemAvaliacao: number
}

/** Trinta dias corridos. `pacientes*` é gente, não avaliação — a consulta devolve uma linha por paciente. */
export interface NumerosDoMes {
  pacientesUti: number
  pacientesPediatria: number
  diasRegistrados: number
  adesaoMediaUti?: number | null
}

export interface PainelInicial {
  hoje: string
  pendentesDeHoje: PendenteDoDia[]
  registradosHoje: number
  totalEmAcompanhamento: number
  adesaoBaixa: AlertaAdesao[]
  mudancasDeFaixa: MudancaDeFaixa[]
  haMaisTempoSemAvaliacao: SemAvaliacao[]
  numeros: NumerosDoMes
}

// ─── Encerramento do acompanhamento ───────────────────────────────────

/** Espelha o enum `MotivoEncerramento`. */
export type MotivoEncerramento =
  | 'ALTA_HOSPITALAR'
  | 'OBITO'
  | 'TRANSFERENCIA'
  | 'SUSPENSAO_TERAPIA'
  | 'OUTRO'

export const OPCOES_MOTIVO_ENCERRAMENTO: { valor: MotivoEncerramento; rotulo: string }[] = [
  { valor: 'ALTA_HOSPITALAR', rotulo: 'Alta hospitalar' },
  { valor: 'OBITO', rotulo: 'Óbito' },
  { valor: 'TRANSFERENCIA', rotulo: 'Transferência' },
  { valor: 'SUSPENSAO_TERAPIA', rotulo: 'Suspensão da terapia nutricional' },
  { valor: 'OUTRO', rotulo: 'Outro' },
]

export interface EncerramentoUti {
  encerradoEm: string
  motivo: MotivoEncerramento
  observacao?: string | null
}

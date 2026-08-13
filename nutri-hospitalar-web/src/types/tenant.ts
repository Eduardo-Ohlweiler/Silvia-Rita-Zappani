import type { Paginacao } from './comum'

/** Espelha o enum `PeriodoAcesso` do backend. A licença é do cliente. */
export type PeriodoAcesso =
  | 'INDETERMINADO'
  | 'UM_MES'
  | 'DOIS_MESES'
  | 'TRES_MESES'
  | 'SEIS_MESES'
  | 'UM_ANO'
  | 'DOIS_ANOS'

export const PERIODO_ACESSO_LABEL: Record<PeriodoAcesso, string> = {
  INDETERMINADO: 'Indeterminado',
  UM_MES: '1 mês',
  DOIS_MESES: '2 meses',
  TRES_MESES: '3 meses',
  SEIS_MESES: '6 meses',
  UM_ANO: '1 ano',
  DOIS_ANOS: '2 anos',
}

export const OPCOES_PERIODO_ACESSO = (
  Object.keys(PERIODO_ACESSO_LABEL) as PeriodoAcesso[]
).map((p) => ({ valor: p, rotulo: PERIODO_ACESSO_LABEL[p] }))

/** Dias que faltam para o fim do acesso a partir dos quais a tela avisa. */
export const DIAS_ALERTA = 7

export interface TenantResponse {
  id: string
  nome: string
  ativo: boolean
  periodoAcesso: PeriodoAcesso
  /** Nulo em acesso indeterminado. */
  acessoExpiraEm: string | null
  acessoExpirado: boolean
  /** Nulo em acesso indeterminado; negativo quando o prazo já venceu. */
  diasParaExpirar: number | null
  createdAt: string
}

export interface TenantUpdate {
  nome: string
}

export interface TenantAcesso {
  periodoAcesso: PeriodoAcesso
}

export interface TenantFiltros extends Paginacao {
  nome?: string
  ativo?: boolean
  /** Só quem vence dentro da janela. É o filtro "Expirando". */
  expirandoEmDias?: number
}

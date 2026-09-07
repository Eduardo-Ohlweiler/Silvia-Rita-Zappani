import type { Paginacao } from './comum'

/**
 * Módulo Clínica — fichas de anamnese e os modelos que as definem.
 *
 * <p>A ficha não tem colunas de pergunta: quem define o que se pergunta é o
 * **modelo**, e cada resposta guarda o **retrato** da pergunta que a produziu.
 * Por isso a tela desenha uma ficha salva a partir de `respostas`, e nunca do
 * modelo de hoje.
 */

// ── Tipos de campo ─────────────────────────────────────────────────────────

export type TipoCampoFicha =
  | 'TEXTO'
  | 'TEXTO_LONGO'
  | 'CHECKBOX'
  | 'DATA'
  | 'NUMERO'
  | 'OPCOES'
  | 'MULTIPLAS_OPCOES'

export const TIPOS_CAMPO: { valor: TipoCampoFicha; rotulo: string; ajuda: string }[] = [
  { valor: 'TEXTO', rotulo: 'Texto curto', ajuda: 'Uma linha — nome de alergia, de medicamento' },
  { valor: 'TEXTO_LONGO', rotulo: 'Texto longo', ajuda: 'Várias linhas — queixa, objetivo' },
  { valor: 'CHECKBOX', rotulo: 'Sim / Não', ajuda: 'Com a opção de deixar não informado' },
  { valor: 'DATA', rotulo: 'Data', ajuda: 'Dia do calendário' },
  { valor: 'NUMERO', rotulo: 'Número', ajuda: 'Refeições por dia, litros de água' },
  { valor: 'OPCOES', rotulo: 'Opções (uma)', ajuda: 'Escolhe uma da lista' },
  { valor: 'MULTIPLAS_OPCOES', rotulo: 'Opções (várias)', ajuda: 'Escolhe quantas quiser' },
]

/** Os dois tipos que exigem a lista de opções — espelha o enum do servidor. */
export function exigeOpcoes(tipo: TipoCampoFicha): boolean {
  return tipo === 'OPCOES' || tipo === 'MULTIPLAS_OPCOES'
}

// ── Modelo de ficha ────────────────────────────────────────────────────────

export interface CampoFicha {
  /** Ausente = pergunta nova. */
  id?: string
  secao?: string
  rotulo: string
  tipo: TipoCampoFicha
  opcoes?: string[]
  obrigatorio?: boolean
  ativo?: boolean
}

export interface CampoFichaResponse {
  id: string
  secao: string | null
  rotulo: string
  tipo: TipoCampoFicha
  opcoes: string[]
  ordem: number
  obrigatorio: boolean
  ativo: boolean
}

export interface ModeloFichaResponse {
  id: string
  nome: string
  descricao: string | null
  ativo: boolean
  /** Do sistema: visível a todo cliente, editável por nenhum. Clone para adaptar. */
  doSistema: boolean
  campos: CampoFichaResponse[]
  createdAt: string
  updatedAt: string | null
}

export interface ModeloFichaSelect {
  id: string
  nome: string
  descricao: string | null
  doSistema: boolean
  totalCampos: number
}

export interface ModeloFichaCreate {
  nome: string
  descricao?: string
  campos: CampoFicha[]
  ativo?: boolean
}

export type ModeloFichaUpdate = ModeloFichaCreate

export interface ModeloFichaFiltros extends Paginacao {
  nome?: string
  ativo?: boolean
  doSistema?: boolean
}

// ── Ficha de anamnese ──────────────────────────────────────────────────────

export interface RespostaFicha {
  campoId: string
  /**
   * Sempre texto. Sim/não manda `'true'`, `'false'` ou **ausente** — e ausente
   * é *não informado*, que não é a mesma coisa que *não*. Opções múltiplas
   * mandam um JSON array.
   */
  valor?: string
}

/** A resposta como ficou gravada, **com o retrato da pergunta**. */
export interface RespostaFichaResponse {
  id: string
  /** Rastro. Nulo quando a pergunta foi apagada do modelo. */
  campoId: string | null
  secao: string | null
  rotulo: string
  tipo: TipoCampoFicha
  opcoes: string[]
  ordem: number
  obrigatorio: boolean
  valor: string | null
}

export interface FichaAnamneseResponse {
  id: string
  pacienteId: string
  pacienteNome: string
  profissionalId: string | null
  profissionalNome: string | null
  dataPreenchimento: string
  /** Nulo quando o modelo foi apagado — a ficha continua inteira. */
  modeloId: string | null
  modeloNome: string
  /** O modelo não existe mais. */
  modeloRemovido: boolean
  /** O modelo existe, mas não é mais o que gerou esta ficha. */
  modeloAlterado: boolean
  respostas: RespostaFichaResponse[]
  observacao: string | null
  createdAt: string
  updatedAt: string | null
}

export interface FichaAnamneseLista {
  id: string
  pacienteId: string
  pacienteNome: string
  profissionalNome: string | null
  dataPreenchimento: string
  modeloNome: string
  respondidas: number
  totalPerguntas: number
}

export interface FichaAnamneseCreate {
  pacienteId: string
  profissionalId?: string
  dataPreenchimento: string
  modeloId: string
  respostas: RespostaFicha[]
  observacao?: string
}

export type FichaAnamneseUpdate = FichaAnamneseCreate

export interface FichaAnamneseFiltros extends Paginacao {
  pacienteId?: string
  modeloId?: string
  de?: string
  ate?: string
}

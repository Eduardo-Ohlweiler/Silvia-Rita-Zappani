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

// ── Escalas nutricionais pontuadas (docs/13) ───────────────────────────────

/**
 * As escalas que o servidor sabe interpretar. Espelha `EscalaRegistry`.
 *
 * O código é só o **nome** da régua: a porta, as faixas, o corte e o ajuste por
 * idade moram no servidor, com fonte citada. A tela não soma nada — a regra do
 * denominador da adesão já existiu em quatro linguagens neste projeto, e sete
 * telas erraram.
 */
export type EscoreCodigo = 'MNA' | 'NRS_2002'

/** Espelha `TomResultado` do backend. Igual ao que o `TResult` já consome. */
export type TomEscore = 'NEUTRO' | 'ADEQUADO' | 'ATENCAO' | 'CRITICO'

/**
 * Do tom do servidor para o tom do `TBadge`.
 *
 * <p>São **dois vocabulários diferentes**, e por isso existe este mapa. O
 * backend fala `TomResultado` — maiúsculas, quatro gravidades clínicas, o mesmo
 * que o `TResult` consome. O `TBadge` fala em cor de interface: `sucesso`,
 * `alerta`, `erro`. Um `toLowerCase()` casaria só `neutro` e devolveria selo sem
 * cor nos outros três, calado. Mapa explícito, num lugar só.
 */
export const TOM_DO_ESCORE = {
  NEUTRO: 'neutro',
  ADEQUADO: 'sucesso',
  ATENCAO: 'alerta',
  CRITICO: 'erro',
} as const satisfies Record<TomEscore, string>

export interface ClassificacaoEscore {
  rotulo: string
  tom: TomEscore
}

export interface GrupoEscore {
  grupo: string
  rotulo: string
  /** **Nulo enquanto o grupo estiver incompleto** — soma parcial de escala é escore errado. */
  subtotal: number | null
  /** Nulo no grupo que não pontua: a pré-triagem da NRS-2002 é porta, não soma. */
  maximo: number | null
  classificacao: ClassificacaoEscore | null
  motivoAusencia: string | null
  /**
   * A escala **dispensou** este bloco — a pré-triagem da NRS-2002 não encontrou
   * critério, e as etapas 2 e 3 não se aplicam. Não é o mesmo que incompleto:
   * não falta responder, não há o que responder. Nulo nas fichas gravadas antes
   * de o campo existir, e nulo é "aplica-se".
   */
  naoSeAplica: boolean | null
  /** Os rótulos inteiros, para a tela listar sem recortar a frase do motivo. */
  perguntasSemResposta: string[]
}

export interface Escore {
  escala: EscoreCodigo | string
  escalaNome: string
  referencia: string
  grupos: GrupoEscore[]
  /** Publicado **separado** do total, para a conta poder ser refeita: 2 + 2 + 1 = 5. */
  ajusteIdade: number | null
  ajusteIdadeDescricao: string | null
  total: number | null
  totalMaximo: number
  classificacao: ClassificacaoEscore | null
  /** A conduta que a publicação prescreve. A MNA não prescreve nenhuma. */
  conclusao: string | null
  motivoAusencia: string | null
}

export interface EscoreRequest {
  modeloId: string
  pacienteId?: string
  dataPreenchimento?: string
  respostas: RespostaFicha[]
}

/** Os blocos de escore, para o seletor do editor de um modelo clonado. */
export const GRUPOS_ESCORE: Record<EscoreCodigo, { valor: string; rotulo: string }[]> = {
  MNA: [
    { valor: 'TRIAGEM', rotulo: 'Triagem' },
    { valor: 'GLOBAL', rotulo: 'Avaliação global' },
  ],
  NRS_2002: [
    { valor: 'PRE_TRIAGEM', rotulo: 'Pré-triagem' },
    { valor: 'ESTADO_NUTRICIONAL', rotulo: 'Estado nutricional' },
    { valor: 'GRAVIDADE_DOENCA', rotulo: 'Gravidade da doença' },
  ],
}

// ── Modelo de ficha ────────────────────────────────────────────────────────

export interface CampoFicha {
  /** Ausente = pergunta nova. */
  id?: string
  secao?: string
  rotulo: string
  tipo: TipoCampoFicha
  opcoes?: string[]
  /** Quanto vale cada opção, **na mesma ordem**. Só em modelo com escala. */
  pontos?: number[]
  /** Em qual bloco da escala esta pergunta soma. */
  grupoEscore?: string
  obrigatorio?: boolean
  ativo?: boolean
}

export interface CampoFichaResponse {
  id: string
  secao: string | null
  rotulo: string
  tipo: TipoCampoFicha
  opcoes: string[]
  /** Paralelo a `opcoes`. Vazio na pergunta que não pontua. */
  pontos: number[]
  grupoEscore: string | null
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
  /** Qual escala pontuada o modelo aplica. Nulo = questionário descritivo. */
  escoreCodigo: EscoreCodigo | null
  /** O nome e a procedência vêm do servidor — a tela não monta citação à mão. */
  escalaNome: string | null
  escalaReferencia: string | null
  campos: CampoFichaResponse[]
  createdAt: string
  updatedAt: string | null
}

export interface ModeloFichaSelect {
  id: string
  nome: string
  descricao: string | null
  doSistema: boolean
  escoreCodigo: EscoreCodigo | null
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
  /** Quanto esta resposta valeu, do retrato. Nulo quando não pontua ou não foi respondida. */
  pontos: number | null
  grupoEscore: string | null
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
  /** O escore **congelado no dia**, e não recalculado ao abrir. Nulo sem escala. */
  escore: Escore | null
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
  escoreTotal: number | null
  escoreClassificacao: string | null
  escoreTom: TomEscore | null
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

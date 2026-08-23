import type { Paginacao } from './comum'

// ─── Fórmula enteral ──────────────────────────────────────────────────

/** Espelha o enum `CategoriaFormulaEnteral` do backend. */
export type CategoriaFormulaEnteral =
  | 'PADRAO'
  | 'HIPERPROTEICA'
  | 'DIABETES'
  | 'PEPTIDICA'
  | 'IMUNOMODULADORA'
  | 'RENAL'
  | 'HEPATICA'

/**
 * O rótulo vem do backend em `categoriaDescricao`; esta tabela existe só para
 * montar o combo de filtro, onde não há registro de onde tirar o texto.
 */
export const CATEGORIAS_ENTERAIS: { valor: CategoriaFormulaEnteral; rotulo: string }[] = [
  { valor: 'PADRAO', rotulo: 'Padrão' },
  { valor: 'HIPERPROTEICA', rotulo: 'Hiperproteica' },
  { valor: 'DIABETES', rotulo: 'Diabetes' },
  { valor: 'PEPTIDICA', rotulo: 'Peptídica' },
  { valor: 'IMUNOMODULADORA', rotulo: 'Imunomoduladora' },
  { valor: 'RENAL', rotulo: 'Renal' },
  { valor: 'HEPATICA', rotulo: 'Hepática' },
]

/**
 * **Toda composição é por litro** — inclusive a de produto que vem em frasco de
 * 500 ml. Foi a apresentação de 500 ml que produziu o pior defeito da planilha
 * de origem: proteína subestimada em 50 % numa das abas.
 *
 * Os campos anuláveis são opcionais de verdade: o backend serializa com
 * `NON_NULL`, então a chave **não vem** quando o valor é nulo.
 */
export interface FormulaEnteralResponse {
  id: string
  nome: string
  categoria?: CategoriaFormulaEnteral
  categoriaDescricao?: string
  densidadeKcalMl: number
  proteinaGL: number
  choGL?: number
  lipGL?: number
  fibrasGL?: number
  potassioMgL?: number
  osmolaridadeMosmL?: number
  /** Do rótulo do produto. Ausente = o cálculo estima pela densidade, e diz. */
  aguaLivrePerc?: number
  ativo: boolean
  /** Fórmula do sistema: visível para todo cliente, editável por nenhum. */
  global: boolean
  createdAt: string
  updatedAt: string | null
}

export interface FormulaEnteralSelect {
  id: string
  nome: string
  categoriaDescricao?: string
  densidadeKcalMl: number
  proteinaGL: number
  choGL?: number
  lipGL?: number
  fibrasGL?: number
  potassioMgL?: number
  aguaLivrePerc?: number
  global: boolean
}

export interface FormulaEnteralCreate {
  nome: string
  categoria?: CategoriaFormulaEnteral
  densidadeKcalMl: number
  proteinaGL: number
  choGL?: number
  lipGL?: number
  fibrasGL?: number
  potassioMgL?: number
  osmolaridadeMosmL?: number
  aguaLivrePerc?: number
  ativo?: boolean
}

export type FormulaEnteralUpdate = Omit<FormulaEnteralCreate, 'ativo'>

export interface FormulaEnteralFiltros extends Paginacao {
  nome?: string
  categoria?: CategoriaFormulaEnteral
  ativo?: boolean
  global?: boolean
}

/** "Peptamen Intense (1 kcal/ml · 92 g PTN/L)" — escolhe-se pela composição. */
export function rotuloFormulaEnteral(f: FormulaEnteralSelect): string {
  const dens = f.densidadeKcalMl.toLocaleString('pt-BR')
  const ptn = f.proteinaGL.toLocaleString('pt-BR')
  return `${f.nome} (${dens} kcal/ml · ${ptn} g PTN/L)`
}

// ─── Produto nutricional ──────────────────────────────────────────────

/** Espelha o enum `TipoProdutoNutricional` do backend. */
export type TipoProdutoNutricional = 'SUPLEMENTO_ORAL' | 'MODULO_PROTEICO' | 'INSUMO_ARTESANAL'

/** Espelha o enum `PapelArtesanal`. Os quatro papéis são fixos. */
export type PapelArtesanal = 'BASE' | 'CARBOIDRATO' | 'PROTEINA' | 'LIPIDIO'

export const TIPOS_PRODUTO: { valor: TipoProdutoNutricional; rotulo: string }[] = [
  { valor: 'SUPLEMENTO_ORAL', rotulo: 'Suplemento oral' },
  { valor: 'MODULO_PROTEICO', rotulo: 'Módulo proteico' },
  { valor: 'INSUMO_ARTESANAL', rotulo: 'Insumo de dieta artesanal' },
]

export const PAPEIS_ARTESANAIS: { valor: PapelArtesanal; rotulo: string }[] = [
  { valor: 'BASE', rotulo: 'Base' },
  { valor: 'CARBOIDRATO', rotulo: 'Carboidrato' },
  { valor: 'PROTEINA', rotulo: 'Proteína' },
  { valor: 'LIPIDIO', rotulo: 'Lipídio' },
]

/**
 * Composição **por medida**, e a medida se declara junto (`medidaNome` +
 * `medidaQtd`) porque cada rótulo usa a sua: medida de 7,8 g, sachê, frasco, ml.
 */
export interface ProdutoNutricionalResponse {
  id: string
  nome: string
  tipo: TipoProdutoNutricional
  tipoDescricao: string
  medidaNome: string
  medidaQtd: number
  /** Da embalagem fechada. Sem ela não há cálculo de latas por mês. */
  embalagemQtd?: number
  kcal?: number
  proteinaG?: number
  choG?: number
  acucarG?: number
  lipG?: number
  sodioMg?: number
  potassioMg?: number
  fosforoMg?: number
  ferroMg?: number
  fibrasG?: number
  osmolaridadeMosmL?: number
  /** Derivado do tipo pelo servidor — nunca enviado no request. */
  moduloProteico: boolean
  papelArtesanal?: PapelArtesanal
  papelDescricao?: string
  observacao?: string
  ativo: boolean
  global: boolean
  createdAt: string
  updatedAt: string | null
}

export interface ProdutoNutricionalSelect {
  id: string
  nome: string
  tipoDescricao: string
  medidaNome: string
  medidaQtd: number
  embalagemQtd?: number
  kcal?: number
  proteinaG?: number
  choG?: number
  lipG?: number
  papelArtesanal?: PapelArtesanal
  global: boolean
}

/**
 * `moduloProteico` **não entra**: o servidor o deriva do `tipo`. São o mesmo
 * fato dito duas vezes, e campo redundante que trafega pode chegar discordando.
 */
export interface ProdutoNutricionalCreate {
  nome: string
  tipo: TipoProdutoNutricional
  medidaNome: string
  medidaQtd: number
  embalagemQtd?: number
  kcal?: number
  proteinaG?: number
  choG?: number
  acucarG?: number
  lipG?: number
  sodioMg?: number
  potassioMg?: number
  fosforoMg?: number
  ferroMg?: number
  fibrasG?: number
  osmolaridadeMosmL?: number
  papelArtesanal?: PapelArtesanal
  observacao?: string
  ativo?: boolean
}

export type ProdutoNutricionalUpdate = Omit<ProdutoNutricionalCreate, 'ativo'>

export interface ProdutoNutricionalFiltros extends Paginacao {
  nome?: string
  tipo?: TipoProdutoNutricional
  ativo?: boolean
  global?: boolean
}

/** "Trophic Basic (medida 7,8 g · 30 kcal · 1,2 g PTN)". */
export function rotuloProduto(p: ProdutoNutricionalSelect): string {
  const partes = [`${p.medidaNome} ${p.medidaQtd.toLocaleString('pt-BR')} g`]
  if (p.kcal != null) partes.push(`${p.kcal.toLocaleString('pt-BR')} kcal`)
  if (p.proteinaG != null) partes.push(`${p.proteinaG.toLocaleString('pt-BR')} g PTN`)
  return `${p.nome} (${partes.join(' · ')})`
}

// ─── Cálculo ──────────────────────────────────────────────────────────

/** Espelham os enums do backend. */
export type Sexo = 'MASCULINO' | 'FEMININO'
export type EtniaChumlea = 'BRANCA' | 'NEGRA'
export type FaseTerapia = 'AGUDA' | 'REABILITACAO'
export type TerapiaRenal = 'NENHUMA' | 'HEMODIALISE_INTERMITENTE' | 'HEMODIALISE_CONTINUA'
export type ModoInfusao = 'CONTINUA' | 'INTERMITENTE'
export type PopulacaoReferencia = 'POPULACAO_CLINICA' | 'ADULTO_SAUDAVEL'
export type JanelaPerdaPeso = 'UMA_SEMANA' | 'UM_MES' | 'TRES_MESES' | 'SEIS_MESES'

export type SegmentoAmputado =
  | 'MAO' | 'ANTEBRACO' | 'ANTEBRACO_MAO' | 'BRACO' | 'MEMBRO_SUPERIOR'
  | 'PE' | 'PERNA' | 'MEMBRO_INFERIOR'

export type OrigemPeso =
  | 'INFORMADO' | 'ESTIMADO_CHUMLEA' | 'ESTIMADO_JUNG' | 'ESTIMADO_RABITO'

export const OPCOES_SEXO: { valor: Sexo; rotulo: string }[] = [
  { valor: 'MASCULINO', rotulo: 'Masculino' },
  { valor: 'FEMININO', rotulo: 'Feminino' },
]

export const OPCOES_ETNIA: { valor: EtniaChumlea; rotulo: string }[] = [
  { valor: 'BRANCA', rotulo: 'Branca' },
  { valor: 'NEGRA', rotulo: 'Negra' },
]

export const OPCOES_FASE: { valor: FaseTerapia; rotulo: string }[] = [
  { valor: 'AGUDA', rotulo: 'Fase aguda' },
  { valor: 'REABILITACAO', rotulo: 'Reabilitação' },
]

export const OPCOES_TERAPIA_RENAL: { valor: TerapiaRenal; rotulo: string }[] = [
  { valor: 'NENHUMA', rotulo: 'Nenhuma' },
  { valor: 'HEMODIALISE_INTERMITENTE', rotulo: 'Hemodiálise intermitente' },
  { valor: 'HEMODIALISE_CONTINUA', rotulo: 'Hemodiálise contínua' },
]

export const OPCOES_MODO_INFUSAO: { valor: ModoInfusao; rotulo: string }[] = [
  { valor: 'CONTINUA', rotulo: 'Contínua' },
  { valor: 'INTERMITENTE', rotulo: 'Intermitente' },
]

export const OPCOES_JANELA_PERDA: { valor: JanelaPerdaPeso; rotulo: string }[] = [
  { valor: 'UMA_SEMANA', rotulo: '1 semana' },
  { valor: 'UM_MES', rotulo: '1 mês' },
  { valor: 'TRES_MESES', rotulo: '3 meses' },
  { valor: 'SEIS_MESES', rotulo: '6 meses' },
]

/**
 * As duas colunas de ajuste de CB e CP pelo IMC. Divergem **só** em IMC < 18,5 —
 * ver `docs/10` §2.9. O padrão é a clínica, porque o módulo é de UTI.
 */
export const OPCOES_POPULACAO: { valor: PopulacaoReferencia; rotulo: string }[] = [
  { valor: 'POPULACAO_CLINICA', rotulo: 'População clínica' },
  { valor: 'ADULTO_SAUDAVEL', rotulo: 'Adulto saudável' },
]

export const OPCOES_SEGMENTO: { valor: SegmentoAmputado; rotulo: string }[] = [
  { valor: 'MAO', rotulo: 'Mão (0,7 %)' },
  { valor: 'ANTEBRACO', rotulo: 'Antebraço (1,6 %)' },
  { valor: 'ANTEBRACO_MAO', rotulo: 'Antebraço com mão (2,3 %)' },
  { valor: 'BRACO', rotulo: 'Braço (2,7 %)' },
  { valor: 'MEMBRO_SUPERIOR', rotulo: 'Membro superior (5,0 %)' },
  { valor: 'PE', rotulo: 'Pé (1,5 %)' },
  { valor: 'PERNA', rotulo: 'Perna (4,4 %)' },
  { valor: 'MEMBRO_INFERIOR', rotulo: 'Membro inferior (16,0 %)' },
]

export const OPCOES_ORIGEM_PESO: { valor: OrigemPeso; rotulo: string }[] = [
  { valor: 'INFORMADO', rotulo: 'Peso informado' },
  { valor: 'ESTIMADO_RABITO', rotulo: 'Estimado — Rabito 2008' },
  { valor: 'ESTIMADO_CHUMLEA', rotulo: 'Estimado — Chumlea 1988' },
  { valor: 'ESTIMADO_JUNG', rotulo: 'Estimado — Jung 2004' },
]

/**
 * O corpo do `POST /uti/calculo`.
 *
 * **Só entradas.** Nenhum resultado é enviado: o servidor recalcula tudo, e o
 * que a tela mostra é sempre o que o servidor devolveu.
 */
export interface CalculoUtiRequest {
  sexo?: Sexo | null
  etnia?: EtniaChumlea | null
  idadeAnos?: number | null
  alturaCm?: number | null
  alturaJoelhoCm?: number | null
  circBracoCm?: number | null
  circPanturrilhaCm?: number | null
  circAbdominalCm?: number | null
  pesoAtualKg?: number | null
  pesoUsualKg?: number | null
  janelaPerda?: JanelaPerdaPeso | null
  segmentosAmputados?: SegmentoAmputado[] | null
  populacaoReferencia?: PopulacaoReferencia | null
  origemPesoPreferida?: OrigemPeso | null
  fase?: FaseTerapia | null
  terapiaRenal?: TerapiaRenal | null
  kcalPorKgAlvo?: number | null
  proteinaPorKgAlvo?: number | null
  formulaEnteralId?: string | null
  modoInfusao?: ModoInfusao | null
  volumePorTempo?: number | null
  tempo?: number | null
  volumeDietaManualMl?: number | null
}

/** Rótulo e tom, atribuídos pelo servidor. A cor nunca sai de casar texto. */
export interface ClassificacaoUti {
  rotulo: string
  tom: 'NEUTRO' | 'ADEQUADO' | 'ATENCAO' | 'CRITICO'
}

/**
 * O backend serializa com `NON_NULL`: a chave **não vem** quando o valor é nulo.
 * Por isso quase tudo aqui é opcional — o tipo não promete o que a resposta pode
 * não trazer.
 */
export interface ResultadoAntropometria {
  alturaEstimadaCm?: number
  pesoChumleaKg?: number
  pesoJungKg?: number
  pesoRabitoKg?: number
  motivoEstimativas?: string

  pesoDeTrabalhoKg?: number
  pesoDeTrabalhoOrigem?: string
  alturaUsadaCm?: number
  alturaUsadaOrigem?: string
  motivoPesoDeTrabalho?: string

  imc?: number
  classificacaoImcOms?: ClassificacaoUti
  classificacaoImcOpas?: ClassificacaoUti
  motivoImc?: string

  pesoIdealKg?: number
  pesoIdealImc25Kg?: number
  pesoAjustadoKg?: number
  pesoCorrigidoAmputacaoKg?: number

  percentualPerdaPeso?: number
  classificacaoPerdaPeso?: ClassificacaoUti
  motivoPerdaPeso?: string

  p50CircBracoCm?: number
  adequacaoCircBracoPerc?: number
  classificacaoAdequacaoCircBraco?: ClassificacaoUti
  motivoAdequacaoCircBraco?: string

  circBracoAjustadaCm?: number
  classificacaoMassaMuscularBraco?: ClassificacaoUti
  circPanturrilhaAjustadaCm?: number
  classificacaoDeplecaoPanturrilha?: ClassificacaoUti
  populacaoReferenciaUsada?: string
  /** Verdadeiro quando IMC < 18,5 — é a única faixa em que as colunas divergem. */
  ajustePeloImcRelevante: boolean
  motivoDeplecao?: string
}

export interface ResultadoNecessidades {
  energiaMinima?: number
  energiaMaxima?: number
  proteinaMinima?: number
  proteinaMaxima?: number
  metaEnergetica?: number
  metaEnergeticaOrigem?: string
  metaProteica?: number
  metaProteicaOrigem?: string
  proteinaTerapiaRenal?: number
  /** Quando verdadeiro, a fase da terapia **não se aplica**. */
  obeso: boolean
  baseDoPeso?: string
  motivo?: string
}

export interface DegrauProgressao {
  dia: number
  percentual: number
  kcal?: number
  volume?: number
}

export interface ResultadoDieta {
  formulaNome?: string
  densidadeKcalMl?: number
  proteinaGL?: number
  volumeTotalMl?: number
  volumeTotalDescricao?: string
  caloriasOfertadas?: number
  proteinaOfertada?: number
  caloriasPorQuilo?: number
  proteinaPorQuilo?: number
  percentualDoVct?: number
  percentualDaProteina?: number
  choOfertado?: number
  lipOfertado?: number
  fibrasOfertadas?: number
  potassioOfertado?: number
  volumePleno?: number
  proteinaNoVolumePleno?: number
  proteinaSuplementar?: number
  unidadeDoVolume?: string
  progressao: DegrauProgressao[]
  motivo?: string
}

export interface FracaoAgua {
  vezesAoDia: number
  mlPorVez?: number
}

export interface ResultadoHidratacao {
  necessidadeMinima?: number
  necessidadeIdeal?: number
  percentualAgua?: number
  /** "água livre do rótulo" ou "estimada pela densidade" — a diferença importa. */
  percentualAguaOrigem?: string
  volumeDietaConsiderado?: number
  aguaNaDieta?: number
  aguaExtraMinima?: number
  aguaExtraIdeal?: number
  distribuicaoMinima: FracaoAgua[]
  distribuicaoIdeal: FracaoAgua[]
  motivo?: string
}

export interface ResultadoUti {
  antropometria: ResultadoAntropometria
  necessidades: ResultadoNecessidades
  dieta: ResultadoDieta
  hidratacao: ResultadoHidratacao
}

// ─── Ferramentas clínicas ─────────────────────────────────────────────

/**
 * Como a bolsa de noradrenalina foi preparada.
 *
 * Os "32" e "64" da planilha **não são fórmulas** — são 2 e 4 ampolas de 4 mg em
 * 250 ml da mesma conta. Expor assim é o que os torna auditáveis, e é o que
 * permite ao serviço que prepara diferente informar os seus próprios números sem
 * precisar de uma terceira fórmula.
 */
export type PreparoNoradrenalina = 'SIMPLES_32' | 'CONCENTRADA_64' | 'AMPOLAS_E_SORO'

export const OPCOES_PREPARO_NORA: { valor: PreparoNoradrenalina; rotulo: string }[] = [
  { valor: 'SIMPLES_32', rotulo: 'Simples — 2 ampolas em 250 ml' },
  { valor: 'CONCENTRADA_64', rotulo: 'Concentrada — 4 ampolas em 250 ml' },
  { valor: 'AMPOLAS_E_SORO', rotulo: 'Outro preparo' },
]

export interface FerramentasClinicasRequest {
  noraPesoKg?: number | null
  noraVazaoMlH?: number | null
  noraPreparo?: PreparoNoradrenalina | null
  noraAmpolas?: number | null
  noraVolumeSoroMl?: number | null

  balancoProteinaG?: number | null
  balancoUreiaG?: number | null

  propofolVazaoMlH?: number | null
  propofolHoras?: number | null

  artesanalVetKcal?: number | null
  artesanalPesoKg?: number | null
  insumoBaseId?: string | null
  dosesBase?: number | null
  insumoCarboidratoId?: string | null
  medidasCarboidrato?: number | null
  insumoProteinaId?: string | null
  medidasProteina?: number | null
  insumoLipidioId?: string | null
  medidasLipidio?: number | null
  administracoesPorDia?: number | null
}

export interface ResultadoNoradrenalina {
  /** A concentração da bolsa — é o número que se confere à beira do leito. */
  concentracaoMcgMl?: number
  doseMcgKgMin?: number
  preparoDescricao?: string
  motivo?: string
}

export interface ResultadoBalancoNitrogenado {
  nitrogenioIngerido?: number
  nitrogenioExcretado?: number
  balanco?: number
  classificacao?: ClassificacaoUti
  motivo?: string
}

export interface ResultadoPropofol {
  kcalDia?: number
  /** Explícito: na planilha as 24 h são constante escondida na fórmula. */
  horasConsideradas?: number
  motivo?: string
}

export interface EmbalagemMes {
  produto: string
  quantidade?: number
}

export interface ItemReceita {
  produto: string
  medidasPorDia?: number
  medidasPorAdministracao?: number
}

export interface ResultadoArtesanal {
  dosesBase?: number
  choTotal?: number
  ptnTotal?: number
  lipTotal?: number
  kcalBase?: number
  kcalTotal?: number
  /** Pela soma dos macros. Tem de bater com `kcalTotal` — os dois vão à tela. */
  kcalPorMacros?: number
  kcalPorQuilo?: number
  proteinaPorQuilo?: number
  percChoSobreVet?: number
  percPtnSobreVet?: number
  percLipSobreVet?: number
  percChoSobreOfertado?: number
  percPtnSobreOfertado?: number
  percLipSobreOfertado?: number
  aguaTotal?: number
  administracoesPorDia?: number
  aguaPorAdministracao?: number
  embalagensPorMes: EmbalagemMes[]
  receitaPorAdministracao: ItemReceita[]
  motivo?: string
}

export interface ResultadoFerramentas {
  noradrenalina: ResultadoNoradrenalina
  balancoNitrogenado: ResultadoBalancoNitrogenado
  propofol: ResultadoPropofol
  artesanal: ResultadoArtesanal
}

// ─── Avaliação ────────────────────────────────────────────────────────

export interface AvaliacaoUtiCreate {
  pacienteId: string
  profissionalId?: string | null
  dataAvaliacao: string
  /** As mesmas entradas que a calculadora manda — um DTO só, não dois. */
  calculo: CalculoUtiRequest
  observacao?: string | null
}

export type AvaliacaoUtiUpdate = AvaliacaoUtiCreate

/**
 * A avaliação gravada.
 *
 * `resultado` vem **do banco**, não recalculado: abrir um registro de três meses
 * atrás mostra o que se decidiu naquele dia. Tem a mesma forma do resultado de
 * `/uti/calculo`, então a tela usa um tipo só.
 */
export interface AvaliacaoUtiResponse {
  id: string
  pacienteId: string
  pacienteNome: string
  profissionalId?: string
  profissionalNome?: string
  dataAvaliacao: string
  calculo: CalculoUtiRequest
  resultado: ResultadoUti
  /** A fórmula saiu do catálogo depois desta avaliação. O retrato dela ficou. */
  formulaRemovida: boolean
  observacao?: string
  createdAt: string
  updatedAt: string | null
}

export interface AvaliacaoUtiLista {
  id: string
  pacienteNome: string
  profissionalNome?: string
  dataAvaliacao: string
  pesoTrabalhoKg?: number
  pesoTrabalhoOrigem?: string
  imc?: number
  classificacaoImc?: string
  tomClassificacao?: 'NEUTRO' | 'ADEQUADO' | 'ATENCAO' | 'CRITICO'
  metaEnergetica?: number
  formulaNome?: string
}

export interface AvaliacaoUtiFiltros extends Paginacao {
  pacienteId?: string
  pacienteNome?: string
  de?: string
  ate?: string
}

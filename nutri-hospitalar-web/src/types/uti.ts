import type { Paginacao } from './comum'
import type { MotivoEncerramento } from './inicio'

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
export type PosicaoNaFaixa = 'MINIMO' | 'MEDIO' | 'MAXIMO'
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

/**
 * Onde na faixa recomendada fixar a meta. O **máximo** é o padrão: é o que o
 * exemplo em cache da própria planilha mostra — VCT 1360 para 68 kg em fase
 * aguda é 20 × 68, o topo. O eroERP usa o ponto médio sem dizer a ninguém.
 *
 * Não confundir com a progressão dos primeiros dias (25/50/75/100 %), que é
 * outra coisa e já está na aba da dieta.
 */
export const OPCOES_POSICAO_FAIXA: { valor: PosicaoNaFaixa; rotulo: string }[] = [
  { valor: 'MINIMO', rotulo: 'Mínimo da faixa' },
  { valor: 'MEDIO', rotulo: 'Ponto médio' },
  { valor: 'MAXIMO', rotulo: 'Máximo da faixa' },
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
  posicaoNaFaixa?: PosicaoNaFaixa | null
  formulaEnteralId?: string | null
  /** Módulo proteico para cobrir a lacuna. Só `MODULO_PROTEICO` é aceito. */
  moduloProteicoId?: string | null
  modoInfusao?: ModoInfusao | null
  volumePorTempo?: number | null
  tempo?: number | null
  volumeDietaManualMl?: number | null
}

/**
 * O tom com que o servidor mandou colorir. É gravado junto com o rótulo, e é
 * por ele que os gráficos colorem — nunca por procurar palavra dentro do texto.
 */
export type TomResultado = 'NEUTRO' | 'ADEQUADO' | 'ATENCAO' | 'CRITICO'

/** Rótulo e tom, atribuídos pelo servidor. A cor nunca sai de casar texto. */
export interface ClassificacaoUti {
  rotulo: string
  tom: TomResultado
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
  /**
   * Por que esta coluna, quando algo além do padrão a justifica — a perda de
   * peso registrada, ou a escolha manual. Nulo quando é só o padrão.
   */
  motivoPopulacaoReferencia?: string
  /** Verdadeiro quando IMC < 18,5 — é a única faixa em que as colunas divergem. */
  ajustePeloImcRelevante: boolean
  /** Um motivo por medida: a panturrilha tem o seu, e nomeia o que falta. */
  motivoMassaMuscularBraco?: string
  motivoDeplecaoPanturrilha?: string
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

  /** Retrato do módulo escolhido — o nome como estava no dia da prescrição. */
  moduloNome?: string
  moduloGramas?: number
  moduloMedidas?: number
  moduloKcal?: number
  /** Por que não há sugestão: meta atingida, módulo não escolhido, ou cadastro incompleto. */
  motivoModulo?: string
  unidadeDoVolume?: string
  progressao: DegrauProgressao[]
  /**
   * Por que a escada está vazia. Campo próprio, e não o `motivo` do bloco: numa
   * avaliação salva a tabela derivada não é gravada mesmo com o bloco inteiro
   * calculado, e a frase acabava colada a números que existem.
   */
  motivoProgressao?: string
  /** Por que o BLOCO não saiu. */
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
  /** Mesma história de `ResultadoDieta.motivoProgressao`. */
  motivoDistribuicao?: string
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

  /**
   * Nulo enquanto o acompanhamento corre. Preenchido, o paciente saiu da lista
   * de trabalho da tela inicial — e a tela diz quando e por quê, para ninguém
   * achar que o registro parou sozinho.
   */
  encerradoEm?: string | null
  motivoEncerramento?: MotivoEncerramento | null
  motivoEncerramentoDescricao?: string | null
  observacaoEncerramento?: string | null
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
  tomClassificacao?: TomResultado
  metaEnergetica?: number
  formulaNome?: string
}

export interface AvaliacaoUtiFiltros extends Paginacao {
  pacienteId?: string
  pacienteNome?: string
  de?: string
  ate?: string
}

// ─── Acompanhamento diário ────────────────────────────────────────────

/**
 * Como o paciente está respirando.
 *
 * **Vocabulário nosso, declarado.** A planilha não tem o campo, e o eroERP
 * guarda um texto livre rotulado "VM / O₂ (%)" que mistura modo e percentual.
 */
export type SuporteVentilatorio =
  | 'AR_AMBIENTE' | 'CATETER_NASAL' | 'MASCARA' | 'ALTO_FLUXO'
  | 'VNI' | 'VENTILACAO_MECANICA' | 'TRAQUEOSTOMIA'

export const OPCOES_SUPORTE: { valor: SuporteVentilatorio; rotulo: string }[] = [
  { valor: 'AR_AMBIENTE', rotulo: 'Ar ambiente' },
  { valor: 'CATETER_NASAL', rotulo: 'Cateter nasal' },
  { valor: 'MASCARA', rotulo: 'Máscara de oxigênio' },
  { valor: 'ALTO_FLUXO', rotulo: 'Cateter nasal de alto fluxo' },
  { valor: 'VNI', rotulo: 'Ventilação não invasiva' },
  { valor: 'VENTILACAO_MECANICA', rotulo: 'Ventilação mecânica invasiva' },
  { valor: 'TRAQUEOSTOMIA', rotulo: 'Traqueostomia' },
]

/**
 * As medidas do dia.
 *
 * **Não existe percentual recebido aqui.** Ele é derivado do volume — no eroERP
 * é campo digitável ao lado de um calculado, e os dois vão para o banco.
 */
export interface RegistroDiarioUtiCreate {
  pessoaId: string
  avaliacaoId?: string | null
  data: string

  dieta?: string | null
  volPrescrito24h?: number | null
  volRecebido24h?: number | null

  mg?: number | null
  k?: number | null
  na?: number | null
  lactato?: number | null
  pcr?: number | null
  ph?: number | null
  pco2?: number | null
  hco3?: number | null
  hgt?: number | null

  suporteVentilatorio?: SuporteVentilatorio | null
  fio2Perc?: number | null
  paSistolica?: number | null
  paDiastolica?: number | null
  balancoHidricoMl?: number | null
  diureseMl?: number | null
  evacuacao?: string | null

  cafeManha?: number | null
  lancheManha?: number | null
  almoco?: number | null
  lancheTarde?: number | null
  jantar?: number | null
  ceia?: number | null

  observacao?: string | null
}

export type RegistroDiarioUtiUpdate = RegistroDiarioUtiCreate

export interface RegistroDiarioUtiResponse extends RegistroDiarioUtiCreate {
  id: string
  pessoaNome: string
  avaliacaoData?: string
  avaliacaoVolumePrescrito?: number
  avaliacaoMetaEnergetica?: number
  suporteVentilatorioDescricao?: string

  /** Derivados — não são colunas. */
  percentualRecebido?: number
  /** "prescrito na avaliação" ou "prescrito informado no dia". */
  referenciaDoPercentual?: string
  caloriasRecebidas?: number
  proteinaRecebida?: number
  caloriasPorQuilo?: number
  proteinaPorQuilo?: number
  diuresePorQuiloHora?: number
  mediaIngestaoOral?: number
  motivoDerivados?: string

  createdAt: string
  updatedAt: string | null
}

export interface RegistroDiarioUtiLista {
  id: string
  pessoaNome: string
  data: string
  volPrescrito24h?: number
  volRecebido24h?: number
  percentualRecebido?: number
  caloriasPorQuilo?: number
  balancoHidricoMl?: number
  diureseMl?: number
  temAvaliacao: boolean
}

export interface RegistroDiarioUtiFiltros extends Paginacao {
  pessoaId?: string
  pessoaNome?: string
  de?: string
  ate?: string
}

/** A avaliação que o servidor sugere vincular — sugestão, não vínculo. */
export interface AvaliacaoSugerida {
  id?: string
  dataAvaliacao?: string
  pesoTrabalhoKg?: number
  metaEnergetica?: number
  volumePrescrito?: number
  formulaNome?: string
  motivo?: string
}

// ─── Painéis ───────────────────────────────────────────────────────────

/**
 * Um ponto da trajetória de um paciente de UTI.
 *
 * Rótulo e tom das classificações viajam juntos porque foram gravados juntos: o
 * tom é o que o gráfico usa para colorir. Colorir por texto — como o eroERP faz
 * com `includes('adequado')` — quebra no primeiro rótulo que contém a palavra
 * por acaso.
 */
export interface PontoAvaliacaoUti {
  dataAvaliacao: string
  idadeAnos?: number

  pesoTrabalhoKg?: number
  imc?: number
  classifImcOms?: string
  classifImcOmsTom?: TomResultado

  percPerdaPeso?: number
  classifPerdaPeso?: string
  classifPerdaPesoTom?: TomResultado

  adequacaoCircBracoPerc?: number
  classifAdequacaoCb?: string
  classifAdequacaoCbTom?: TomResultado

  /**
   * As metas de peso da própria avaliação — é o que dá contexto ao gráfico.
   * Sem elas um paciente com uma avaliação só vira um ponto solto num eixo
   * automático: tecnicamente correto e clinicamente mudo.
   */
  pesoIdealKg?: number
  pesoIdealImc25Kg?: number
  pesoAjustadoKg?: number

  /** A faixa recomendada, que vira o fundo do gráfico de oferta. */
  energiaMinima?: number
  energiaMaxima?: number
  proteinaMinima?: number
  proteinaMaxima?: number

  metaEnergetica?: number
  metaProteica?: number

  formulaNome?: string
  fase?: string
  obeso: boolean

  volumeTotalMl?: number
  caloriasOfertadas?: number
  proteinaOfertada?: number
  caloriasPorQuilo?: number
  proteinaPorQuilo?: number
  percentualDoVct?: number
  percentualDaProteina?: number
}

export interface HistoricoFormulaUti {
  formulaNome: string
  avaliacoes: number
  primeiroUso?: string
  ultimoUso?: string
}

export interface PainelPacienteUti {
  pacienteId: string
  pacienteNome: string
  sexo?: 'MASCULINO' | 'FEMININO'
  dataNascimento?: string
  idadeAnosAtual?: number

  totalAvaliacoes: number
  primeiraAvaliacao?: string
  ultimaAvaliacao?: string
  ultima?: AvaliacaoUtiResponse

  evolucao: PontoAvaliacaoUti[]
  historicoFormulas: HistoricoFormulaUti[]

  /** A ponte para o painel de acompanhamento. */
  totalDiasRegistrados: number
  primeiroDia?: string
  ultimoDia?: string
}

export interface PainelAcompanhamentoUti {
  pessoaId: string
  pessoaNome: string

  de?: string
  ate?: string
  totalDias: number
  diasSemAvaliacao: number

  /** Média das adesões. **Não é nota** — ver `AdesaoNoTempo`. */
  adesaoMedia?: number
  caloriasPorQuiloMedia?: number
  proteinaPorQuiloMedia?: number
  balancoAcumuladoMl?: number
  diureseMediaMlKgHora?: number
  ingestaoOralMedia?: number

  ultimaAvaliacao?: string
  metaEnergetica?: number
  metaProteica?: number
  volumePrescritoNaAvaliacao?: number

  /** Ordem cronológica crescente: é o eixo X. */
  dias: RegistroDiarioUtiResponse[]
}

export interface PontoPeriodoUti {
  periodo: string
  avaliacoes: number
  dias: number
  /**
   * Média do percentual recebido nos dias do mês. **Ausente**, e não zero,
   * quando o mês não teve dia com prescrição — zero seria um mês em que
   * ninguém recebeu nada, e a linha desceria ao chão dizendo isso.
   */
  adesaoMedia?: number
}

/** `tom` nulo = a fatia "não classificado", que não é categoria clínica. */
export interface ContagemRotuladaUti {
  rotulo: string
  tom?: TomResultado
  quantidade: number
}

export interface ContagemUti {
  rotulo: string
  quantidade: number
}

export interface PacienteRankingUti {
  pacienteId: string
  pacienteNome: string
  avaliacoes: number
  dias: number
}

export interface DashboardUti {
  totalAvaliacoes: number
  totalPacientes: number
  avaliacoesMes: number
  totalDiasRegistrados: number

  idadeMediaAnos?: number
  pesoTrabalhoMedio?: number
  imcMedio?: number
  metaEnergeticaMedia?: number
  metaProteicaMedia?: number
  kcalPorQuiloMedio?: number
  proteinaPorQuiloMedio?: number
  adesaoMedia?: number
  percEutrofia?: number
  avaliacoesObesidade: number

  porPeriodo: PontoPeriodoUti[]

  classifImcOms: ContagemRotuladaUti[]
  classifAdequacaoCb: ContagemRotuladaUti[]
  classifPerdaPeso: ContagemRotuladaUti[]

  porFormula: ContagemUti[]
  porFase: ContagemUti[]
  porTerapiaRenal: ContagemUti[]
  porModoInfusao: ContagemUti[]
  /** Degraus com ordem — coluna com rampa ordinal, nunca cor por quantidade. */
  porFaixaEtaria: ContagemUti[]

  pacientesMaisAvaliados: PacienteRankingUti[]
}

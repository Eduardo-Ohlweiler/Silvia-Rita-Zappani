import type { Paginacao } from './comum'
import type { Sexo } from './pessoa'

// ─── Fórmula láctea ───────────────────────────────────────────────────

export interface FormulaLacteaResponse {
  id: string
  nome: string
  kcalPor100ml: number
  proteinaPor100ml: number
  ativo: boolean
  /** Fórmula do sistema: visível para todo cliente, editável por nenhum. */
  global: boolean
  createdAt: string
  updatedAt: string | null
}

export interface FormulaLacteaSelect {
  id: string
  nome: string
  kcalPor100ml: number
  proteinaPor100ml: number
  global: boolean
}

export interface FormulaLacteaCreate {
  nome: string
  kcalPor100ml: number
  proteinaPor100ml: number
  ativo?: boolean
}

export type FormulaLacteaUpdate = Omit<FormulaLacteaCreate, 'ativo'>

export interface FormulaLacteaFiltros extends Paginacao {
  nome?: string
  ativo?: boolean
  global?: boolean
}

/** "NAN 2 (73,8 kcal · 1,65 g / 100 ml)" — quem prescreve escolhe pela composição. */
export function rotuloFormula(f: FormulaLacteaSelect): string {
  const kcal = f.kcalPor100ml.toLocaleString('pt-BR')
  const prot = f.proteinaPor100ml.toLocaleString('pt-BR')
  return `${f.nome} (${kcal} kcal · ${prot} g / 100 ml)`
}

// ─── Resultado do cálculo ─────────────────────────────────────────────

/** Espelha o enum `FaixaOms` do backend. */
export type FaixaOms = 'BAIXA' | 'ADEQUADA' | 'ALTA'

export interface Classificacao {
  faixa: FaixaOms
  rotulo: string
}

export interface EstadoNutricional {
  imc?: number | null
  motivoImc?: string | null
  pesoIdade?: Classificacao | null
  estaturaIdade?: Classificacao | null
  imcIdade?: Classificacao | null
  motivo?: string | null
}

export interface Necessidades {
  vet?: number | null
  motivoVet?: string | null
  proteina?: number | null
  motivoProteina?: string | null
}

export interface DietaCalculada {
  vezesDia?: number | null
  volumeTotal?: number | null
  caloriasTotais?: number | null
  proteinaTotal?: number | null
  percCalorico?: number | null
  percProteico?: number | null
  motivo?: string | null
}

/**
 * Três blocos, um por aba da tela de cálculo.
 *
 * Numa avaliação salva os `motivo*` vêm nulos: eles explicam a ausência no
 * momento em que se digita, e voltam assim que o usuário mexe numa entrada.
 *
 * Os campos anuláveis são **opcionais**: o backend serializa omitindo nulos, e
 * a chave simplesmente não vem. Comparar com `!= null` cobre os dois casos.
 */
export interface ResultadoPediatrico {
  estadoNutricional: EstadoNutricional
  necessidades: Necessidades
  dieta: DietaCalculada
}

/**
 * As entradas do cálculo. Nada é obrigatório: faltando uma, o resultado que
 * dependia dela volta ausente com o motivo — nunca com erro.
 */
export interface CalculoPediatricoRequest {
  sexo?: Sexo | null
  idadeMeses?: number | null
  peso?: number | null
  estatura?: number | null
  formulaLacteaId?: string | null
  volumeMl?: number | null
  /** Intervalo entre tomadas, em horas — não a quantidade delas. */
  frequenciaHoras?: number | null
}

// ─── Avaliação ────────────────────────────────────────────────────────

export interface AvaliacaoPediatricaResponse {
  id: string
  pacienteId: string
  pacienteNome: string
  profissionalId?: string | null
  profissionalNome?: string | null

  dataAvaliacao: string
  sexo: Sexo
  idadeMeses: number
  peso: number
  estatura?: number | null

  /** Retrato tirado no cálculo, não leitura do catálogo de hoje. */
  formulaLacteaId?: string | null
  formulaNome?: string | null
  formulaKcalPor100ml?: number | null
  formulaProteinaPor100ml?: number | null
  volumeMl?: number | null
  frequenciaHoras?: number | null

  resultado: ResultadoPediatrico

  observacao?: string | null
  createdAt: string
  updatedAt?: string | null
}

export interface AvaliacaoPediatricaLista {
  id: string
  dataAvaliacao: string
  pacienteNome: string
  idadeMeses: number
  peso: number
  imc?: number | null
  classifImcIdade?: FaixaOms | null
  classifImcIdadeRotulo?: string | null
  formulaNome?: string | null
}

/** Só entradas. Nenhum resultado é aceito do cliente — o servidor recalcula. */
export interface AvaliacaoPediatricaCreate {
  pacienteId: string
  profissionalId?: string | null
  dataAvaliacao: string
  sexo: Sexo
  idadeMeses: number
  peso: number
  estatura?: number | null
  formulaLacteaId?: string | null
  volumeMl?: number | null
  frequenciaHoras?: number | null
  observacao?: string | null
}

export type AvaliacaoPediatricaUpdate = AvaliacaoPediatricaCreate

export interface AvaliacaoPediatricaFiltros extends Paginacao {
  pacienteId?: string
  formulaLacteaId?: string
  de?: string
  ate?: string
  mesesMin?: number
  mesesMax?: number
}

// ─── Painéis ──────────────────────────────────────────────────────────

/** Um mês da curva de crescimento da OMS, com os cinco percentis. */
export interface CurvaOmsPonto {
  idadeMeses: number

  pesoP3: number
  pesoP15: number
  pesoP50: number
  pesoP85: number
  pesoP97: number

  estaturaP3: number
  estaturaP15: number
  estaturaP50: number
  estaturaP85: number
  estaturaP97: number

  imcP3: number
  imcP15: number
  imcP50: number
  imcP85: number
  imcP97: number
}

export interface PontoEvolutivo {
  dataAvaliacao: string
  idadeMeses: number
  peso?: number | null
  estatura?: number | null
  imc?: number | null
  classifPesoIdade?: FaixaOms | null
  classifEstaturaIdade?: FaixaOms | null
  classifImcIdade?: FaixaOms | null
  vet?: number | null
  proteinaNecessidade?: number | null
  caloriasTotais?: number | null
  proteinaTotal?: number | null
  percCalorico?: number | null
  percProteico?: number | null
}

export interface HistoricoFormula {
  formulaNome: string
  avaliacoes: number
  primeiroUso?: string | null
  ultimoUso?: string | null
}

export interface PainelPaciente {
  pacienteId: string
  pacienteNome: string
  sexo?: Sexo | null
  dataNascimento?: string | null
  idadeMesesAtual?: number | null

  totalAvaliacoes: number
  primeiraAvaliacao?: string | null
  ultimaAvaliacao?: string | null

  ultima?: AvaliacaoPediatricaResponse | null
  evolucao: PontoEvolutivo[]
  historicoFormulas: HistoricoFormula[]
}

export interface PontoPeriodo {
  periodo: string
  avaliacoes: number
}

/** `faixa` nula = não classificado. */
export interface ContagemFaixa {
  faixa?: FaixaOms | null
  rotulo: string
  quantidade: number
}

export interface Contagem {
  rotulo: string
  quantidade: number
}

export interface PacienteRanking {
  pacienteId: string
  pacienteNome: string
  avaliacoes: number
}

export interface DashboardGeral {
  totalAvaliacoes: number
  totalPacientes: number
  avaliacoesMes: number

  idadeMediaMeses?: number | null
  pesoMedio?: number | null
  imcMedio?: number | null
  percImcAdequado?: number | null
  coberturaCaloricaMedia?: number | null

  porPeriodo: PontoPeriodo[]

  classifPesoIdade: ContagemFaixa[]
  classifEstaturaIdade: ContagemFaixa[]
  classifImcIdade: ContagemFaixa[]

  porFormula: Contagem[]
  porFaixaEtaria: Contagem[]
  porSexo: Contagem[]

  pacientesMaisAvaliados: PacienteRanking[]
}

// ─── Acompanhamento diário (docs/11) ────────────────────────────────────

/**
 * Os derivados de um dia de acompanhamento — **nenhum deles é gravado**.
 *
 * Cada um vem com o motivo de estar ausente, quando está. Ver docs/11 §5.
 */
export interface AcompanhamentoDerivado {
  /** A idade NO DIA do registro, da data de nascimento. Na pediatria ela anda. */
  idadeMeses?: number | null
  motivoIdade?: string

  imc?: number | null
  motivoImc?: string

  pesoIdade?: Classificacao | null
  estaturaIdade?: Classificacao | null
  imcIdade?: Classificacao | null
  motivoEstadoNutricional?: string

  /**
   * O volume CONTRA O QUAL a adesão foi medida.
   *
   * O `volPrescrito24h` do registro é o digitado no dia — registro, não
   * denominador. Quem mostra prescrito ao lado de percentual mostra ESTE:
   * o par errado fazia a tela dizer "650 / 700 ml · 90,28 %".
   */
  prescritoDeReferencia?: number | null
  percentualRecebido?: number | null
  /** Contra o quê comparou: o prescrito da avaliação ou o informado no dia. */
  referenciaDoRecebido?: string
  motivoPercentualRecebido?: string

  caloriasRecebidas?: number | null
  proteinaRecebida?: number | null
  motivoOferta?: string

  caloriasPorKg?: number | null
  proteinaPorKg?: number | null
  motivoPorQuilo?: string

  adequacaoCalorica?: number | null
  motivoAdequacaoCalorica?: string
  adequacaoProteica?: number | null
  motivoAdequacaoProteica?: string

  aceitacaoTomadas?: number | null
  motivoAceitacao?: string
}

export interface RegistroDiarioPediatricoLista {
  id: string
  pessoaNome: string
  data: string
  idadeMeses?: number | null
  pesoKg?: number | null
  volPrescrito24h?: number | null
  volRecebido24h?: number | null
  /** O denominador da adesão — ver `AcompanhamentoDerivado`. */
  prescritoDeReferencia?: number | null
  referenciaDoRecebido?: string
  percentualRecebido?: number | null
  adequacaoCalorica?: number | null
  aceitacaoTomadas?: number | null
  temAvaliacao: boolean
}

export interface RegistroDiarioPediatricoResponse {
  id: string
  pessoaId: string
  pessoaNome: string

  avaliacaoId?: string | null
  avaliacaoData?: string | null
  avaliacaoVolumeTotal?: number | null
  avaliacaoVet?: number | null
  avaliacaoProteinaNecessidade?: number | null
  avaliacaoFormulaNome?: string | null

  data: string

  pesoKg?: number | null
  estaturaCm?: number | null
  volPrescrito24h?: number | null
  volRecebido24h?: number | null
  tomadasPrevistas?: number | null
  tomadasAceitas?: number | null
  observacao?: string | null

  derivados: AcompanhamentoDerivado

  createdAt?: string
  updatedAt?: string
}

export interface RegistroDiarioPediatricoCreate {
  pessoaId: string
  avaliacaoId?: string | null
  data: string
  pesoKg?: number | null
  estaturaCm?: number | null
  volPrescrito24h?: number | null
  volRecebido24h?: number | null
  tomadasPrevistas?: number | null
  tomadasAceitas?: number | null
  observacao?: string | null
}

export type RegistroDiarioPediatricoUpdate = RegistroDiarioPediatricoCreate

export interface RegistroDiarioPediatricoFiltros {
  page?: number
  size?: number
  pessoaId?: string
  pessoaNome?: string
  de?: string
  ate?: string
}

/** A avaliação que o dia deveria referenciar — sugestão, não vínculo. */
export interface AvaliacaoPediatricaSugerida {
  id?: string | null
  dataAvaliacao?: string | null
  peso?: number | null
  volumeTotal?: number | null
  vet?: number | null
  proteinaNecessidade?: number | null
  formulaNome?: string | null
  /** Quando não há avaliação até a data — e o dia pode ser registrado assim mesmo. */
  aviso?: string | null
}

export interface PainelAcompanhamentoPediatrico {
  pessoaId: string
  pessoaNome: string

  de?: string | null
  ate?: string | null
  totalDias: number
  diasSemAvaliacao: number

  adesaoMedia?: number | null
  caloriasPorKgMedia?: number | null
  proteinaPorKgMedia?: number | null
  adequacaoCaloricaMedia?: number | null
  adequacaoProteicaMedia?: number | null
  aceitacaoTomadasMedia?: number | null

  /** O crescimento no período — o que só a pediatria tem. */
  pesoInicialKg?: number | null
  pesoFinalKg?: number | null
  variacaoPesoKg?: number | null
  idadeInicialMeses?: number | null
  idadeFinalMeses?: number | null

  ultimaAvaliacao?: string | null
  vet?: number | null
  proteinaNecessidade?: number | null
  volumePrescritoNaAvaliacao?: number | null

  /** Ordem cronológica crescente: é o eixo X. */
  dias: RegistroDiarioPediatricoResponse[]
}

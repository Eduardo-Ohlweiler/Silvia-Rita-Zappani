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

import { api } from './api'
import type { Page } from '@/types/comum'
import type {
  AvaliacaoUtiCreate,
  AvaliacaoUtiFiltros,
  AvaliacaoUtiLista,
  AvaliacaoUtiResponse,
  AvaliacaoUtiUpdate,
  CalculoUtiRequest,
  FerramentasClinicasRequest,
  ResultadoFerramentas,
  ResultadoUti,
  FormulaEnteralCreate,
  FormulaEnteralFiltros,
  FormulaEnteralResponse,
  FormulaEnteralSelect,
  FormulaEnteralUpdate,
  PapelArtesanal,
  ProdutoNutricionalCreate,
  ProdutoNutricionalFiltros,
  ProdutoNutricionalResponse,
  ProdutoNutricionalSelect,
  ProdutoNutricionalUpdate,
  TipoProdutoNutricional,
} from '@/types/uti'

/**
 * Catálogos da terapia nutricional de UTI adulto.
 *
 * Módulo de negócio: o escopo é sempre o tenant efetivo do token. Os dois
 * catálogos são **híbridos** — a leitura alcança as linhas globais do sistema, a
 * escrita não. Tentar alterar uma global devolve 400 com a explicação, e por
 * isso o `global` vem em toda resposta: a tela desabilita a ação antes do
 * clique, em vez de deixar o usuário descobrir no erro.
 */
/**
 * **Nenhuma fórmula nutricional é calculada aqui.** As entradas vão para o
 * `/uti/calculo` e o resultado volta pronto — é a mesma conta que a avaliação
 * vai gravar, então a tela nunca mostra um número diferente do que o banco
 * guarda. Ver `docs/04` §7.
 */
export const calculoUtiService = {
  /** Calcula sem gravar. É o que a tela chama a cada alteração de campo. */
  calcular: (dto: CalculoUtiRequest) =>
    api.post<ResultadoUti>('/uti/calculo', dto).then((r) => r.data),
}

/**
 * As quatro ferramentas clínicas, sem persistência.
 *
 * Vão num corpo só porque a tela recalcula tudo com um debounce só — as quatro
 * são independentes entre si, ao contrário das abas do cálculo.
 */
export const ferramentasClinicasService = {
  calcular: (dto: FerramentasClinicasRequest) =>
    api.post<ResultadoFerramentas>('/uti/ferramentas-clinicas', dto).then((r) => r.data),
}

/**
 * As avaliações gravadas.
 *
 * **Abrir não recalcula.** O `findById` devolve os resultados como foram
 * gravados; a tela os passa ao componente de cálculo como `resultadoInicial`, e
 * o recálculo só assume ao primeiro toque num campo.
 */
export const avaliacaoUtiService = {
  getAll: (params: AvaliacaoUtiFiltros) =>
    api.get<Page<AvaliacaoUtiLista>>('/uti/avaliacoes', { params }).then((r) => r.data),

  findById: (id: string) =>
    api.get<AvaliacaoUtiResponse>(`/uti/avaliacoes/${id}`).then((r) => r.data),

  create: (dto: AvaliacaoUtiCreate) =>
    api.post<AvaliacaoUtiResponse>('/uti/avaliacoes', dto).then((r) => r.data),

  update: (id: string, dto: AvaliacaoUtiUpdate) =>
    api.put<AvaliacaoUtiResponse>(`/uti/avaliacoes/${id}`, dto).then((r) => r.data),

  remover: (id: string) => api.delete(`/uti/avaliacoes/${id}`).then(() => undefined),
}

export const formulaEnteralService = {
  getAll: (params: FormulaEnteralFiltros) =>
    api.get<Page<FormulaEnteralResponse>>('/formulas-enterais', { params }).then((r) => r.data),

  select: (termo?: string) =>
    api
      .get<FormulaEnteralSelect[]>('/formulas-enterais/select', { params: { termo } })
      .then((r) => r.data),

  findById: (id: string) =>
    api.get<FormulaEnteralResponse>(`/formulas-enterais/${id}`).then((r) => r.data),

  create: (dto: FormulaEnteralCreate) =>
    api.post<FormulaEnteralResponse>('/formulas-enterais', dto).then((r) => r.data),

  update: (id: string, dto: FormulaEnteralUpdate) =>
    api.put<FormulaEnteralResponse>(`/formulas-enterais/${id}`, dto).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api
      .patch<FormulaEnteralResponse>(`/formulas-enterais/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),
}

export const produtoNutricionalService = {
  getAll: (params: ProdutoNutricionalFiltros) =>
    api
      .get<Page<ProdutoNutricionalResponse>>('/produtos-nutricionais', { params })
      .then((r) => r.data),

  select: (tipo?: TipoProdutoNutricional, termo?: string) =>
    api
      .get<ProdutoNutricionalSelect[]>('/produtos-nutricionais/select', {
        params: { tipo, termo },
      })
      .then((r) => r.data),

  /** Alimenta a sugestão de módulo da dieta enteral — catálogo, não lista fixa. */
  modulosProteicos: () =>
    api
      .get<ProdutoNutricionalSelect[]>('/produtos-nutricionais/modulos-proteicos')
      .then((r) => r.data),

  /** Os insumos de um dos quatro papéis da receita artesanal. */
  insumosPorPapel: (papel: PapelArtesanal) =>
    api
      .get<ProdutoNutricionalSelect[]>('/produtos-nutricionais/insumos-artesanais', {
        params: { papel },
      })
      .then((r) => r.data),

  findById: (id: string) =>
    api.get<ProdutoNutricionalResponse>(`/produtos-nutricionais/${id}`).then((r) => r.data),

  create: (dto: ProdutoNutricionalCreate) =>
    api.post<ProdutoNutricionalResponse>('/produtos-nutricionais', dto).then((r) => r.data),

  update: (id: string, dto: ProdutoNutricionalUpdate) =>
    api.put<ProdutoNutricionalResponse>(`/produtos-nutricionais/${id}`, dto).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api
      .patch<ProdutoNutricionalResponse>(`/produtos-nutricionais/${id}/ativo`, null, {
        params: { ativo },
      })
      .then((r) => r.data),
}

import { api } from './api'
import type { EncerramentoUti } from '@/types/inicio'
import type { Page } from '@/types/comum'
import type {
  AvaliacaoSugerida,
  AvaliacaoUtiCreate,
  AvaliacaoUtiFiltros,
  AvaliacaoUtiLista,
  AvaliacaoUtiResponse,
  AvaliacaoUtiUpdate,
  CalculoUtiRequest,
  DashboardUti,
  FerramentasClinicasRequest,
  ResultadoFerramentas,
  ResultadoUti,
  FormulaEnteralCreate,
  FormulaEnteralFiltros,
  FormulaEnteralResponse,
  FormulaEnteralSelect,
  FormulaEnteralUpdate,
  PainelAcompanhamentoUti,
  PainelPacienteUti,
  PapelArtesanal,
  ProdutoNutricionalCreate,
  ProdutoNutricionalFiltros,
  ProdutoNutricionalResponse,
  ProdutoNutricionalSelect,
  ProdutoNutricionalUpdate,
  RegistroDiarioUtiCreate,
  RegistroDiarioUtiFiltros,
  RegistroDiarioUtiLista,
  RegistroDiarioUtiResponse,
  RegistroDiarioUtiUpdate,
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

  /** Encerra o acompanhamento — tira o paciente da lista de trabalho. */
  encerrar: (id: string, dto: EncerramentoUti) =>
    api
      .patch<AvaliacaoUtiResponse>(`/uti/avaliacoes/${id}/encerramento`, dto)
      .then((r) => r.data),

  /** Desfaz um encerramento feito por engano. */
  reabrir: (id: string) =>
    api
      .delete<AvaliacaoUtiResponse>(`/uti/avaliacoes/${id}/encerramento`)
      .then((r) => r.data),
}

/**
 * O acompanhamento diário.
 *
 * **Um registro por paciente por dia** — repetir devolve 409. E o vínculo com a
 * avaliação nunca acontece em silêncio: a tela pede a sugestão e manda o que o
 * usuário confirmar.
 */
export const registroDiarioUtiService = {
  getAll: (params: RegistroDiarioUtiFiltros) =>
    api
      .get<Page<RegistroDiarioUtiLista>>('/uti/registros-diarios', { params })
      .then((r) => r.data),

  findById: (id: string) =>
    api.get<RegistroDiarioUtiResponse>(`/uti/registros-diarios/${id}`).then((r) => r.data),

  /** A mais recente daquele paciente até aquela data. Pode não haver. */
  avaliacaoSugerida: (pessoaId: string, data: string) =>
    api
      .get<AvaliacaoSugerida>('/uti/registros-diarios/avaliacao-sugerida', {
        params: { pessoaId, data },
      })
      .then((r) => r.data),

  create: (dto: RegistroDiarioUtiCreate) =>
    api.post<RegistroDiarioUtiResponse>('/uti/registros-diarios', dto).then((r) => r.data),

  update: (id: string, dto: RegistroDiarioUtiUpdate) =>
    api.put<RegistroDiarioUtiResponse>(`/uti/registros-diarios/${id}`, dto).then((r) => r.data),

  remover: (id: string) => api.delete(`/uti/registros-diarios/${id}`).then(() => undefined),
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

/**
 * Os três painéis. Só leitura, e nenhum deles recalcula: o servidor soma o que
 * já está gravado.
 */
export const utiPainelService = {
  /** `dias = 0` traz desde sempre — é o padrão de um painel de paciente. */
  painelPaciente: (pacienteId: string, dias = 0, formulaEnteralId?: string) =>
    api
      .get<PainelPacienteUti>('/uti/painel-paciente', {
        params: { pacienteId, dias, formulaEnteralId },
      })
      .then((r) => r.data),

  /** Sem `de`, o servidor devolve os últimos 30 dias. */
  painelAcompanhamento: (pessoaId: string, de?: string, ate?: string) =>
    api
      .get<PainelAcompanhamentoUti>('/uti/painel-acompanhamento', {
        params: { pessoaId, de, ate },
      })
      .then((r) => r.data),

  dashboard: (params: { dias?: number; formulaEnteralId?: string }) =>
    api.get<DashboardUti>('/uti/dashboard', { params }).then((r) => r.data),
}

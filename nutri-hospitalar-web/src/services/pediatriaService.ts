import { api } from './api'
import type { Page } from '@/types/comum'
import type {
  AvaliacaoPediatricaCreate,
  AvaliacaoPediatricaFiltros,
  AvaliacaoPediatricaLista,
  AvaliacaoPediatricaResponse,
  AvaliacaoPediatricaUpdate,
  CalculoPediatricoRequest,
  FormulaLacteaCreate,
  FormulaLacteaFiltros,
  FormulaLacteaResponse,
  FormulaLacteaSelect,
  FormulaLacteaUpdate,
  CurvaOmsPonto,
  DashboardGeral,
  PainelPaciente,
  ResultadoPediatrico,
} from '@/types/pediatria'

/**
 * Módulo de negócio: o escopo é sempre o tenant efetivo do token.
 *
 * **Nenhuma fórmula nutricional é calculada aqui.** As entradas vão para o
 * `/calcular` e o resultado volta pronto — é a mesma conta que a gravação usa,
 * então a tela nunca mostra um número diferente do que o banco guarda.
 */
export const pediatriaService = {
  /** Calcula sem gravar. É o que a tela chama a cada alteração de campo. */
  calcular: (dto: CalculoPediatricoRequest) =>
    api
      .post<ResultadoPediatrico>('/pediatria/avaliacoes/calcular', dto)
      .then((r) => r.data),

  getAll: (params: AvaliacaoPediatricaFiltros) =>
    api
      .get<Page<AvaliacaoPediatricaLista>>('/pediatria/avaliacoes', { params })
      .then((r) => r.data),

  findById: (id: string) =>
    api
      .get<AvaliacaoPediatricaResponse>(`/pediatria/avaliacoes/${id}`)
      .then((r) => r.data),

  create: (dto: AvaliacaoPediatricaCreate) =>
    api
      .post<AvaliacaoPediatricaResponse>('/pediatria/avaliacoes', dto)
      .then((r) => r.data),

  update: (id: string, dto: AvaliacaoPediatricaUpdate) =>
    api
      .put<AvaliacaoPediatricaResponse>(`/pediatria/avaliacoes/${id}`, dto)
      .then((r) => r.data),

  remover: (id: string) => api.delete(`/pediatria/avaliacoes/${id}`).then(() => undefined),

  /** A janela da curva da OMS que o gráfico vai desenhar. */
  curvaOms: (sexo: string, idadeMin: number, idadeMax: number) =>
    api
      .get<CurvaOmsPonto[]>('/pediatria/curvas-oms', { params: { sexo, idadeMin, idadeMax } })
      .then((r) => r.data),

  painelPaciente: (pacienteId: string, dias = 0) =>
    api
      .get<PainelPaciente>('/pediatria/painel-paciente', { params: { pacienteId, dias } })
      .then((r) => r.data),

  dashboard: (params: { dias?: number; formulaLacteaId?: string; sexo?: string }) =>
    api.get<DashboardGeral>('/pediatria/dashboard', { params }).then((r) => r.data),
}

export const formulaLacteaService = {
  getAll: (params: FormulaLacteaFiltros) =>
    api.get<Page<FormulaLacteaResponse>>('/formulas-lacteas', { params }).then((r) => r.data),

  select: (termo?: string) =>
    api
      .get<FormulaLacteaSelect[]>('/formulas-lacteas/select', { params: { termo } })
      .then((r) => r.data),

  findById: (id: string) =>
    api.get<FormulaLacteaResponse>(`/formulas-lacteas/${id}`).then((r) => r.data),

  create: (dto: FormulaLacteaCreate) =>
    api.post<FormulaLacteaResponse>('/formulas-lacteas', dto).then((r) => r.data),

  update: (id: string, dto: FormulaLacteaUpdate) =>
    api.put<FormulaLacteaResponse>(`/formulas-lacteas/${id}`, dto).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api
      .patch<FormulaLacteaResponse>(`/formulas-lacteas/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),
}

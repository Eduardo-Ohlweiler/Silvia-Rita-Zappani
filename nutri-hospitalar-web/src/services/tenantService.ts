import { api } from './api'
import type { Page, SelectOption } from '@/types/comum'
import type {
  PeriodoAcesso,
  TenantFiltros,
  TenantResponse,
  TenantUpdate,
} from '@/types/tenant'

/**
 * Não há criação de tenant: ele nasce junto do seu primeiro usuário, em
 * `POST /usuarios` sem `tenantId`. Ver docs/02-modulo-usuarios §3.1.
 */
export const tenantService = {
  getAll: (params: TenantFiltros) =>
    api.get<Page<TenantResponse>>('/tenants', { params }).then((r) => r.data),

  findById: (id: string) => api.get<TenantResponse>(`/tenants/${id}`).then((r) => r.data),

  select: (nome?: string) =>
    api.get<SelectOption[]>('/tenants/select', { params: { nome } }).then((r) => r.data),

  update: (id: string, dto: TenantUpdate) =>
    api.put<TenantResponse>(`/tenants/${id}`, dto).then((r) => r.data),

  /**
   * Reativar um cliente com o prazo vencido devolve 409 — a renovação é
   * `definirAcesso`, que reconta a data e reativa junto.
   */
  alterarAtivo: (id: string, ativo: boolean) =>
    api.patch<TenantResponse>(`/tenants/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),

  /** Define ou renova o período de acesso, sempre a partir de hoje. */
  definirAcesso: (id: string, periodoAcesso: PeriodoAcesso) =>
    api.patch<TenantResponse>(`/tenants/${id}/acesso`, { periodoAcesso }).then((r) => r.data),
}

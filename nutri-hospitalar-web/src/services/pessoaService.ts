import { api } from './api'
import type { Page } from '@/types/comum'
import type {
  PessoaCreate,
  PessoaFiltros,
  PessoaResponse,
  PessoaSelect,
  PessoaUpdate,
} from '@/types/pessoa'

/**
 * Módulo de negócio: o escopo é sempre o tenant efetivo do token. Não há
 * consulta global aqui — só usuários e log de acesso a têm, e por razões
 * específicas (docs/02 §6.1).
 */
export const pessoaService = {
  getAll: (params: PessoaFiltros) =>
    api.get<Page<PessoaResponse>>('/pessoas', { params }).then((r) => r.data),

  findById: (id: string) => api.get<PessoaResponse>(`/pessoas/${id}`).then((r) => r.data),

  select: (termo?: string, tipoCadastroId?: string) =>
    api
      .get<PessoaSelect[]>('/pessoas/select', { params: { termo, tipoCadastroId } })
      .then((r) => r.data),

  create: (dto: PessoaCreate) =>
    api.post<PessoaResponse>('/pessoas', dto).then((r) => r.data),

  update: (id: string, dto: PessoaUpdate) =>
    api.put<PessoaResponse>(`/pessoas/${id}`, dto).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api
      .patch<PessoaResponse>(`/pessoas/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),
}

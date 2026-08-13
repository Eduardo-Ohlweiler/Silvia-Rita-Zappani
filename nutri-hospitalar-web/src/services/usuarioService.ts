import { api } from './api'
import type { Page, SelectOption } from '@/types/comum'
import type {
  PerfilUpdate,
  SenhaUpdate,
  UsuarioCreate,
  UsuarioFiltros,
  UsuarioResponse,
  UsuarioUpdate,
} from '@/types/usuario'

export const usuarioService = {
  /** Escopo do tenant efetivo. */
  getAll: (params: UsuarioFiltros) =>
    api.get<Page<UsuarioResponse>>('/usuarios', { params }).then((r) => r.data),

  /**
   * Todos os tenants. É o padrão da tela de usuários para o superadmin: ao
   * cadastrar um cliente novo, o usuário nasce no tenant recém-criado e não
   * apareceria na listagem do tenant efetivo.
   */
  getAllGlobal: (params: UsuarioFiltros & { tenantId?: string }) =>
    api.get<Page<UsuarioResponse>>('/usuarios/global', { params }).then((r) => r.data),

  findById: (id: string) =>
    api.get<UsuarioResponse>(`/usuarios/${id}`).then((r) => r.data),

  select: (nome?: string) =>
    api.get<SelectOption[]>('/usuarios/select', { params: { nome } }).then((r) => r.data),

  create: (dto: UsuarioCreate) =>
    api.post<UsuarioResponse>('/usuarios', dto).then((r) => r.data),

  update: (id: string, dto: UsuarioUpdate) =>
    api.put<UsuarioResponse>(`/usuarios/${id}`, dto).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api.patch<UsuarioResponse>(`/usuarios/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),

  desbloquear: (id: string) =>
    api.patch<UsuarioResponse>(`/usuarios/${id}/desbloquear`).then((r) => r.data),

  // ─── Perfil próprio — qualquer usuário autenticado ──────────────────
  getPerfil: () => api.get<UsuarioResponse>('/usuarios/perfil').then((r) => r.data),

  updatePerfil: (dto: PerfilUpdate) =>
    api.put<UsuarioResponse>('/usuarios/perfil', dto).then((r) => r.data),

  alterarSenha: (dto: SenhaUpdate) =>
    api.patch<void>('/usuarios/perfil/senha', dto).then((r) => r.data),
}

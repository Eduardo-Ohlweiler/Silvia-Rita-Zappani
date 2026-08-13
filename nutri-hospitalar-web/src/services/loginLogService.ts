import { api } from './api'
import type { Page } from '@/types/comum'
import type { LoginLogFiltros, LoginLogResponse } from '@/types/loginlog'

export const loginLogService = {
  /** Escopo do tenant efetivo. */
  getAll: (params: LoginLogFiltros) =>
    api.get<Page<LoginLogResponse>>('/login-logs', { params }).then((r) => r.data),

  /** Auditoria consolidada — o único ponto que lê através dos tenants. */
  getAllGlobal: (params: LoginLogFiltros) =>
    api.get<Page<LoginLogResponse>>('/login-logs/global', { params }).then((r) => r.data),
}

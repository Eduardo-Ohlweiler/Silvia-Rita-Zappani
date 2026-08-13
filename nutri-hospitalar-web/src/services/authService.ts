import { api } from './api'
import type { AuthResponse, LoginRequest } from '@/types/auth'

export const authService = {
  login: (dados: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', dados).then((r) => r.data),

  logout: () => api.post<void>('/auth/logout').then((r) => r.data),

  refresh: (refreshToken: string) =>
    api.post<AuthResponse>('/auth/refresh', { refreshToken }).then((r) => r.data),

  /** Somente SUPERADMIN. Reemite o token com o tenant de destino. */
  switchTenant: (tenantId: string) =>
    api.post<AuthResponse>(`/auth/switch-tenant/${tenantId}`).then((r) => r.data),

  exitTenant: () => api.post<AuthResponse>('/auth/exit-tenant').then((r) => r.data),
}

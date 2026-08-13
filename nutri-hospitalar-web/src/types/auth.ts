/** Espelha o enum `Role` do backend. Nível único por usuário. */
export type Role = 'SUPERADMIN' | 'ADMIN' | 'USER'

export const ROLE_LABEL: Record<Role, string> = {
  SUPERADMIN: 'Superadministrador',
  ADMIN: 'Administrador',
  USER: 'Usuário',
}

/** Espelha o `AuthResponseDto`. */
export interface AuthResponse {
  accessToken: string
  /** Nulo na troca de tenant: o token existente continua valendo. */
  refreshToken: string | null
  usuarioId: string
  nome: string
  email: string
  role: Role
  tenantId: string
  tenantNome: string
  impersonating: boolean
}

export interface LoginRequest {
  email: string
  senha: string
}

/** Sessão em memória, derivada do `AuthResponse`. */
export interface Sessao {
  usuarioId: string
  nome: string
  email: string
  role: Role
  tenantId: string
  tenantNome: string
  /** true quando um SUPERADMIN está navegando dentro de outro tenant. */
  impersonating: boolean
}

import type { Role } from './auth'
import type { Paginacao } from './comum'
import type { PeriodoAcesso } from './tenant'

/** Espelha o `UsuarioResponseDto`. Nunca traz senha nem hash. */
export interface UsuarioResponse {
  id: string
  tenantId: string
  tenantNome: string
  nome: string
  email: string
  telefone: string | null
  codigoPais: string
  role: Role
  ativo: boolean
  bloqueado: boolean
  createdAt: string
}

/**
 * O `tenantId` decide o destino, e omiti-lo no tenant raiz **cria um cliente
 * novo** com o nome do usuário — que nasce ADMIN, ignorando a `role` enviada.
 * Ver docs/02-modulo-usuarios §3.1.
 */
export interface UsuarioCreate {
  nome: string
  email: string
  senha: string
  telefone?: string
  codigoPais?: string
  role?: Role
  tenantId?: string
  /** Licença do cliente novo. Ignorado ao entrar num tenant existente. */
  periodoAcesso?: PeriodoAcesso
}

export interface UsuarioUpdate {
  nome: string
  email: string
  telefone?: string
  codigoPais?: string
  role: Role
}

export interface UsuarioFiltros extends Paginacao {
  nome?: string
  email?: string
  role?: Role
  ativo?: boolean
}

export interface PerfilUpdate {
  nome: string
  telefone?: string
  codigoPais?: string
}

export interface SenhaUpdate {
  senhaAtual: string
  senhaNova: string
}

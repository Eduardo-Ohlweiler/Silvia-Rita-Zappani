import type { Paginacao } from './comum'

export type MotivoFalha =
  | 'SENHA_INVALIDA'
  | 'USUARIO_INEXISTENTE'
  | 'USUARIO_INATIVO'
  | 'TENANT_INATIVO'
  | 'BLOQUEADO'
  | 'ACESSO_EXPIRADO'

export const MOTIVO_FALHA_LABEL: Record<MotivoFalha, string> = {
  SENHA_INVALIDA: 'Senha inválida',
  USUARIO_INEXISTENTE: 'Usuário inexistente',
  USUARIO_INATIVO: 'Usuário inativo',
  TENANT_INATIVO: 'Tenant inativo',
  BLOQUEADO: 'Conta bloqueada',
  ACESSO_EXPIRADO: 'Período de acesso encerrado',
}

export type TipoLogout = 'MANUAL' | 'EXPIRACAO'

export const TIPO_LOGOUT_LABEL: Record<TipoLogout, string> = {
  MANUAL: 'Manual',
  EXPIRACAO: 'Expiração',
}

export interface LoginLogResponse {
  id: string
  tenantId: string | null
  tenantNome: string | null
  usuarioId: string | null
  usuarioNome: string | null
  /** Único vestígio quando a tentativa foi com e-mail inexistente. */
  emailTentativa: string | null
  sucesso: boolean
  motivoFalha: MotivoFalha | null
  dataLogin: string
  dataLogout: string | null
  tipoLogout: TipoLogout | null
  enderecoIp: string | null
  userAgent: string | null
  /** Preenchido quando um superadmin entrou neste tenant. */
  impersonadoPorNome: string | null
}

export interface LoginLogFiltros extends Paginacao {
  usuarioId?: string
  tenantId?: string
  sucesso?: boolean
  de?: string
  ate?: string
  ip?: string
}

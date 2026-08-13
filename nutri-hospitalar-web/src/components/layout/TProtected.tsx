import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import type { Role } from '@/types/auth'

interface TProtectedProps {
  /** Sem roles, basta estar autenticado. */
  roles?: Role[]
}

/**
 * Guarda de rota. É **conveniência de navegação**, não segurança: a autorização
 * real é o `@PreAuthorize` do backend. Esconder rota não protege dado.
 */
export function TProtected({ roles }: TProtectedProps) {
  const { autenticado, carregando, hasRole } = useAuth()
  const location = useLocation()

  // Enquanto a sessão é restaurada pelo refresh token, não decidir nada —
  // senão a tela de login pisca a cada F5 de quem já está logado.
  if (carregando) {
    return (
      <div className="grid min-h-dvh place-items-center bg-bg">
        <span
          role="status"
          aria-label="Carregando"
          className="size-8 animate-spin rounded-full border-2 border-primary border-t-transparent"
        />
      </div>
    )
  }

  if (!autenticado) {
    return <Navigate to="/login" replace state={{ de: location.pathname }} />
  }

  if (roles && !hasRole(...roles)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

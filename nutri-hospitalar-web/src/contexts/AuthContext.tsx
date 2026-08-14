import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { registrarPerdaDeSessao, renovarSessao } from '@/services/api'
import { authService } from '@/services/authService'
import { tokenStore } from '@/services/tokenStore'
import type { AuthResponse, LoginRequest, Role, Sessao } from '@/types/auth'

interface AuthContextValor {
  sessao: Sessao | null
  /** true enquanto a sessão é restaurada no boot — evita piscar a tela de login. */
  carregando: boolean
  autenticado: boolean
  login: (dados: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  /** Devolve a sessão nova — quem troca precisa do nome do destino. */
  switchTenant: (tenantId: string) => Promise<Sessao>
  exitTenant: () => Promise<Sessao>
  /** Conveniência visual. A autorização real é o `@PreAuthorize` do backend. */
  hasRole: (...roles: Role[]) => boolean
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValor | null>(null)

function paraSessao(resposta: AuthResponse): Sessao {
  return {
    usuarioId: resposta.usuarioId,
    nome: resposta.nome,
    email: resposta.email,
    role: resposta.role,
    tenantId: resposta.tenantId,
    tenantNome: resposta.tenantNome,
    impersonating: resposta.impersonating,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(null)
  const [carregando, setCarregando] = useState(true)

  const aplicar = useCallback((resposta: AuthResponse) => {
    tokenStore.setAccessToken(resposta.accessToken)
    // Na troca de tenant o refresh vem nulo: o token atual continua valendo.
    if (resposta.refreshToken) tokenStore.setRefreshToken(resposta.refreshToken)

    const nova = paraSessao(resposta)
    setSessao(nova)
    return nova
  }, [])

  const encerrar = useCallback(() => {
    tokenStore.clear()
    setSessao(null)
  }, [])

  /**
   * Restaura a sessão no boot. O access token vive só em memória, então o F5
   * o perde — o refresh token no `localStorage` é o que traz a sessão de volta.
   *
   * Usa `renovarSessao()`, que compartilha a trava com o interceptor. Sem ela,
   * o `StrictMode` monta este efeito duas vezes, as duas reapresentam o mesmo
   * refresh token, e o backend — corretamente — revoga a cadeia inteira
   * tratando como token roubado. O sintoma era cair no login sem motivo.
   */
  useEffect(() => {
    if (!tokenStore.getRefreshToken()) {
      setCarregando(false)
      return
    }

    renovarSessao()
      .then(aplicar)
      .catch(encerrar)
      .finally(() => setCarregando(false))
  }, [aplicar, encerrar])

  /** O interceptor avisa quando a renovação falhou de vez. */
  useEffect(() => registrarPerdaDeSessao(encerrar), [encerrar])

  const valor = useMemo<AuthContextValor>(
    () => ({
      sessao,
      carregando,
      autenticado: sessao !== null,

      login: async (dados) => {
        aplicar(await authService.login(dados))
      },

      logout: async () => {
        try {
          await authService.logout()
        } finally {
          // Mesmo que o logout falhe no servidor, a sessão local morre aqui.
          encerrar()
        }
      },

      switchTenant: async (tenantId) => aplicar(await authService.switchTenant(tenantId)),
      exitTenant: async () => aplicar(await authService.exitTenant()),

      hasRole: (...roles) => {
        if (!sessao) return false
        if (sessao.role === 'SUPERADMIN') return true
        return roles.includes(sessao.role)
      },
    }),
    [sessao, carregando, aplicar, encerrar],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

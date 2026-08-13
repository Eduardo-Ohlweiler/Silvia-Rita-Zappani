import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { toast } from 'react-toastify'
import type { AuthResponse } from '@/types/auth'
import type { ErroResponse } from '@/types/comum'
import { tokenStore } from './tokenStore'

/** Em dev cai no proxy do Vite (`/api` → :8080); em produção, a URL real. */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30_000,
})

interface RequisicaoRepetivel extends InternalAxiosRequestConfig {
  _jaTentouRenovar?: boolean
}

/** Rotas que não levam token — e que não devem disparar renovação. */
const ROTAS_PUBLICAS = ['/auth/login', '/auth/refresh']

const ehRotaPublica = (url?: string) =>
  !!url && ROTAS_PUBLICAS.some((rota) => url.startsWith(rota))

api.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken()
  if (token && !ehRotaPublica(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * **Uma renovação por vez, no aplicativo inteiro.**
 *
 * O refresh é de uso único com rotação: reapresentar um token já consumido faz
 * o backend revogar toda a cadeia, tratando como roubo. Duas chamadas
 * concorrentes com o mesmo token derrubam a sessão do próprio usuário.
 *
 * Isso acontece em dois cenários reais, e por isso a trava é compartilhada:
 *
 * 1. Várias requisições tomando 401 ao mesmo tempo.
 * 2. O `StrictMode` do React, que em desenvolvimento monta o efeito de boot
 *    duas vezes — as duas leriam o mesmo token do `localStorage`.
 */
let renovacaoEmCurso: Promise<AuthResponse> | null = null

export function renovarSessao(): Promise<AuthResponse> {
  renovacaoEmCurso ??= executarRenovacao().finally(() => {
    renovacaoEmCurso = null
  })
  return renovacaoEmCurso
}

async function executarRenovacao(): Promise<AuthResponse> {
  const refreshToken = tokenStore.getRefreshToken()
  if (!refreshToken) throw new Error('sem refresh token')

  // `axios.post` puro: não passa pelos interceptores desta instância
  const { data } = await axios.post<AuthResponse>(
    `${api.defaults.baseURL}/auth/refresh`,
    { refreshToken },
    { timeout: 30_000 },
  )

  tokenStore.setAccessToken(data.accessToken)
  if (data.refreshToken) tokenStore.setRefreshToken(data.refreshToken)
  return data
}

/** Chamado pelo AuthContext quando a sessão morre de vez. */
let aoPerderSessao: (() => void) | null = null
export function registrarPerdaDeSessao(callback: () => void) {
  aoPerderSessao = callback
}

api.interceptors.response.use(
  (resposta) => resposta,
  async (erro: AxiosError<ErroResponse>) => {
    const original = erro.config as RequisicaoRepetivel | undefined

    const deveRenovar =
      erro.response?.status === 401 &&
      original &&
      !original._jaTentouRenovar &&
      !ehRotaPublica(original.url) &&
      tokenStore.getRefreshToken()

    if (deveRenovar) {
      original._jaTentouRenovar = true
      try {
        const { accessToken } = await renovarSessao()
        original.headers.Authorization = `Bearer ${accessToken}`
        return api(original)
      } catch {
        tokenStore.clear()
        aoPerderSessao?.()
      }
    }

    return Promise.reject(erro)
  },
)

/**
 * Tratamento padrão de erro de API. Usar em todo `catch`.
 *
 * O 401 é omitido de propósito: o interceptor acima já cuidou dele, e mostrar
 * "não autenticado" enquanto a renovação acontece só confunde.
 */
export function handleApiError(erro: unknown): void {
  if (axios.isAxiosError<ErroResponse>(erro)) {
    const status = erro.response?.status

    if (status === 401) return
    if (status === 403) {
      toast.error('Você não tem permissão para esta ação')
      return
    }
    if (status === 404) {
      toast.error('Registro não encontrado')
      return
    }
    if (!erro.response) {
      toast.error('Não foi possível falar com o servidor')
      return
    }
    toast.error(erro.response.data?.erro ?? 'Falha na comunicação com o servidor')
    return
  }
  toast.error('Erro inesperado')
}

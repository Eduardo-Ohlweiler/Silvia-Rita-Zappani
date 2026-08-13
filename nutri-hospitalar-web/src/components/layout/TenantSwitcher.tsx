import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { IconTenants } from '@/assets/icons'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'
import type { SelectOption } from '@/types/comum'

/**
 * Troca de tenant do superadmin.
 *
 * O superadmin **não fura** o filtro de tenant — ele muda o valor do filtro. O
 * backend reemite o token com o tenant de destino, e todo o resto do sistema
 * continua filtrando normalmente.
 */
export function TenantSwitcher() {
  const { sessao, switchTenant, exitTenant } = useAuth()
  const [tenants, setTenants] = useState<SelectOption[]>([])
  const [trocando, setTrocando] = useState(false)

  const ehSuperadmin = sessao?.role === 'SUPERADMIN'

  useEffect(() => {
    // A guarda vai DENTRO do efeito: hooks rodam antes do early return, então
    // sem isto um ADMIN dispararia /tenants/select, tomaria 403 e veria um
    // toast de "sem permissão" logo ao entrar.
    if (!ehSuperadmin) return
    tenantService.select().then(setTenants).catch(handleApiError)
  }, [ehSuperadmin])

  if (!ehSuperadmin || !sessao) return null

  /**
   * **Nunca recarregar a página aqui.** O access token vive em memória: um
   * reload o descarta, o boot restaura a sessão pelo refresh token — que
   * pertence à sessão do tenant de origem — e o usuário volta de onde saiu,
   * como se a troca não tivesse acontecido.
   *
   * Não é preciso: o `AuthContext` já atualizou a sessão, e as telas refazem a
   * consulta porque dependem de `sessao.tenantId`.
   */
  async function trocar(tenantId: string) {
    if (!tenantId || tenantId === sessao?.tenantId) return
    setTrocando(true)
    try {
      const nome = tenants.find((t) => t.id === tenantId)?.nome
      await switchTenant(tenantId)
      toast.success(nome ? `Você entrou em "${nome}"` : 'Você entrou em outro tenant')
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setTrocando(false)
    }
  }

  async function sair() {
    setTrocando(true)
    try {
      await exitTenant()
      toast.success('De volta ao tenant raiz')
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setTrocando(false)
    }
  }

  return (
    <div className="flex items-center gap-2">
      <label className="sr-only" htmlFor="seletor-tenant">
        Tenant
      </label>
      <div className="relative">
        <IconTenants className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-txt-muted" />
        <select
          id="seletor-tenant"
          value={sessao.tenantId}
          disabled={trocando}
          onChange={(e) => void trocar(e.target.value)}
          className="h-9 max-w-47.5 appearance-none truncate rounded-md border border-line-strong
            bg-surface pl-8 pr-3 text-caption text-txt
            focus:border-primary focus:outline-none focus:ring-[3px] focus:ring-primary/10
            disabled:opacity-60"
        >
          {/* O tenant atual pode não estar na lista se estiver inativo */}
          {!tenants.some((t) => t.id === sessao.tenantId) && (
            <option value={sessao.tenantId}>{sessao.tenantNome}</option>
          )}
          {tenants.map((t) => (
            <option key={t.id} value={t.id}>
              {t.nome}
            </option>
          ))}
        </select>
      </div>

      {sessao.impersonating && (
        <button
          type="button"
          onClick={() => void sair()}
          disabled={trocando}
          className="whitespace-nowrap rounded-md px-2 py-1 text-caption font-medium
            text-warning hover:bg-warning-bg disabled:opacity-60"
        >
          Sair do tenant
        </button>
      )}
    </div>
  )
}

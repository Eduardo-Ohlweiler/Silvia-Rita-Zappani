import { useState } from 'react'
import { toast } from 'react-toastify'
import { TCombo } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'

/**
 * Troca de tenant do superadmin.
 *
 * O superadmin **não fura** o filtro de tenant — ele muda o valor do filtro. O
 * backend reemite o token com o tenant de destino, e todo o resto do sistema
 * continua filtrando normalmente.
 *
 * A busca é no servidor, e não um `<select>` com a lista inteira: o
 * `/tenants/select` devolve no máximo 100, então com mais clientes que isso os
 * seguintes em ordem alfabética ficariam **inalcançáveis** — não é questão de
 * conforto, é cliente que não dá para abrir.
 */
export function TenantSwitcher() {
  const { sessao, switchTenant, exitTenant } = useAuth()
  const [trocando, setTrocando] = useState(false)

  const ehSuperadmin = sessao?.role === 'SUPERADMIN'

  // Sem lista pré-carregada, some também o efeito que disparava
  // `/tenants/select` na montagem — o ADMIN nem chegava a ver o componente,
  // mas tomava 403 e um toast ao entrar no sistema.
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
      const { tenantNome } = await switchTenant(tenantId)
      toast.success(`Você entrou em "${tenantNome}"`)
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
      <TCombo
        label="Tenant"
        rotuloOculto
        tamanho="sm"
        className="w-full lg:w-56"
        value={sessao.tenantId}
        /* O tenant atual pode estar fora dos 100 primeiros — ou inativo, e
           portanto fora da busca. O nome já está na sessão: use-o. */
        rotuloInicial={sessao.tenantNome}
        buscar={tenantService.select}
        onChange={(id) => void trocar(id)}
        disabled={trocando}
        placeholder="Buscar cliente…"
      />

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

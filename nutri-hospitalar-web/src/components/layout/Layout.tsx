import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { IconAlerta, IconMenu, IconSair } from '@/assets/icons'
import { useAuth } from '@/hooks/useAuth'
import { ROLE_LABEL } from '@/types/auth'
import { Sidebar } from './Sidebar'
import { TenantSwitcher } from './TenantSwitcher'
import { TThemeToggle } from '../common/TThemeToggle'

export function Layout() {
  const { sessao, logout } = useAuth()
  const [menuAberto, setMenuAberto] = useState(false)

  return (
    <div className="min-h-dvh bg-bg">
      <Sidebar aberta={menuAberto} onFechar={() => setMenuAberto(false)} />

      {/* Deslocado pela sidebar só a partir de lg, onde ela é fixa */}
      <div className="conteudo-da-aplicacao lg:pl-sidebar">
        {sessao?.impersonating && (
          <div className="nao-imprime flex items-center justify-center gap-2 bg-warning-bg px-4 py-2 text-center text-caption text-warning">
            <IconAlerta className="size-4 shrink-0" />
            <span>
              Você está navegando como o tenant{' '}
              <strong>&ldquo;{sessao.tenantNome}&rdquo;</strong>.
            </span>
          </div>
        )}

        <header className="nao-imprime sticky top-0 z-20 flex h-header items-center gap-3 border-b border-line bg-surface px-4 sm:px-6">
          <button
            type="button"
            onClick={() => setMenuAberto(true)}
            aria-label="Abrir menu"
            className="grid size-9 shrink-0 place-items-center rounded-md text-txt-secondary hover:bg-surface-alt lg:hidden"
          >
            <IconMenu className="size-5" />
          </button>

          <div className="ml-auto flex items-center gap-2 sm:gap-3">
            <div className="hidden lg:block">
              <TenantSwitcher />
            </div>

            <div className="hidden text-right leading-tight md:block">
              <p className="text-body text-txt">{sessao?.nome}</p>
              <p className="text-caption text-txt-secondary">
                {sessao && ROLE_LABEL[sessao.role]}
              </p>
            </div>

            <TThemeToggle />

            <button
              type="button"
              onClick={() => void logout()}
              title="Sair"
              aria-label="Sair"
              className="grid size-9 place-items-center rounded-md text-txt-secondary
                transition-colors hover:bg-danger-bg hover:text-danger"
            >
              <IconSair className="size-5" />
            </button>
          </div>
        </header>

        {/* Seletor de tenant abaixo do header no mobile, onde não cabe ao lado */}
        {sessao?.role === 'SUPERADMIN' && (
          <div className="nao-imprime border-b border-line bg-surface px-4 py-2 lg:hidden">
            <TenantSwitcher />
          </div>
        )}

        <main className="mx-auto w-full max-w-350 p-4 sm:p-6">
          {/* Nada de cabeçalho de papel aqui: quem carrega marca, cliente e
              data é a própria `Folha` de cada documento. Um cabeçalho global
              apareceria por cima dela, duplicado. */}
          <Outlet />
        </main>
      </div>
    </div>
  )
}

import type { ReactNode } from 'react'

interface TPageProps {
  title: string
  subtitle?: string
  actions?: ReactNode
  /**
   * O que sai no papel — uma `Folha`, não a tela.
   *
   * Quando vem preenchido, **todo o conteúdo de tela para de imprimir**: a
   * folha é o documento, e ela não é um espelho do formulário. Sem ele a
   * página imprime como está, que é o comportamento de quem ainda não tem
   * documento próprio.
   */
  documento?: ReactNode
  children: ReactNode
}

export function TPage({ title, subtitle, actions, documento, children }: TPageProps) {
  // Marcar aqui, e não em cada tela, é o que impede uma delas de ganhar
  // documento e continuar imprimindo o formulário por baixo dele.
  const soTela = documento ? 'nao-imprime' : undefined

  return (
    <div className="flex flex-col gap-5">
      <header
        className={`flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between ${soTela ?? ''}`}
      >
        <div>
          <h1 className="text-h1 font-medium text-txt">{title}</h1>
          {subtitle && <p className="mt-0.5 text-caption text-txt-secondary">{subtitle}</p>}
        </div>
        {actions && <div className="flex shrink-0 gap-2">{actions}</div>}
      </header>

      <div className={soTela}>{children}</div>

      {documento}
    </div>
  )
}

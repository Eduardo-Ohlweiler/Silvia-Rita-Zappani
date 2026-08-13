import type { ReactNode } from 'react'

interface TPanelProps {
  title?: string
  actions?: ReactNode
  children: ReactNode
  className?: string
}

/**
 * No tema claro a elevação vem da sombra difusa, e a borda é quase invisível
 * (`#EAEEF6`). No escuro a sombra não aparece — fundo escuro engole sombra —
 * então é a borda (`#16305A`) que desenha a aresta do card. A mesma classe
 * serve os dois: quem muda é o token.
 */
export function TPanel({ title, actions, children, className = '' }: TPanelProps) {
  return (
    <section
      className={`rounded-lg border border-line bg-surface p-5 shadow-card ${className}`}
    >
      {(title || actions) && (
        <header className="mb-4 flex items-center justify-between gap-4">
          {title && <h2 className="text-h2 font-medium text-txt">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  )
}

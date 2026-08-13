import type { ReactNode } from 'react'

interface TPageProps {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
}

export function TPage({ title, subtitle, actions, children }: TPageProps) {
  return (
    <div className="flex flex-col gap-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-h1 font-medium text-txt">{title}</h1>
          {subtitle && <p className="mt-0.5 text-caption text-txt-secondary">{subtitle}</p>}
        </div>
        {actions && <div className="flex shrink-0 gap-2">{actions}</div>}
      </header>
      {children}
    </div>
  )
}

import type { ReactNode } from 'react'

type Tom = 'neutro' | 'sucesso' | 'alerta' | 'erro' | 'info' | 'sage' | 'peach'

const TONS: Record<Tom, string> = {
  neutro: 'bg-surface-alt text-txt-secondary',
  sucesso: 'bg-success-bg text-success',
  alerta: 'bg-warning-bg text-warning',
  erro: 'bg-danger-bg text-danger',
  info: 'bg-info-bg text-info',
  sage: 'bg-sage-bg text-sage-text',
  peach: 'bg-peach-bg text-peach-text',
}

interface TBadgeProps {
  tom?: Tom
  /** Cor nunca é o único portador de informação — o ícone acompanha. */
  icone?: ReactNode
  children: ReactNode
}

export function TBadge({ tom = 'neutro', icone, children }: TBadgeProps) {
  return (
    <span
      className={`inline-flex h-[22px] items-center gap-1 rounded-full px-2 text-caption
        font-medium whitespace-nowrap ${TONS[tom]}`}
    >
      {icone}
      {children}
    </span>
  )
}

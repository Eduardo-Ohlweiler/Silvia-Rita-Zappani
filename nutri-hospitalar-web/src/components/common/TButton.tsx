import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variante = 'primary' | 'secondary' | 'ghost' | 'danger'
type Tamanho = 'md' | 'sm'

interface TButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variante
  size?: Tamanho
  loading?: boolean
  children: ReactNode
}

/**
 * `text-txt-inverse` em vez de `text-white` nos botões cheios: no tema escuro
 * a primária é clara (`#5F8FD8`) e texto branco sobre ela dá 3,28:1 —
 * reprovado. Com o inverso (navy) sobe para 6,08:1, e no tema claro o inverso
 * já é branco sobre `#1E4686`, 9,23:1. Um token resolve os dois.
 */
const VARIANTES: Record<Variante, string> = {
  primary:
    'bg-primary text-txt-inverse hover:bg-primary-hover active:bg-primary-active',
  secondary:
    'bg-surface text-txt border border-line-strong hover:bg-surface-alt',
  ghost: 'text-primary hover:bg-surface-alt',
  danger: 'bg-danger text-txt-inverse hover:opacity-90',
}

const TAMANHOS: Record<Tamanho, string> = {
  md: 'h-[38px] px-4 text-body',
  sm: 'h-8 px-3 text-caption',
}

export function TButton({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  className = '',
  children,
  ...props
}: TButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || loading}
      aria-busy={loading}
      className={`inline-flex items-center justify-center gap-2 rounded-md font-medium
        transition-colors duration-150
        disabled:cursor-not-allowed disabled:opacity-60
        ${VARIANTES[variant]} ${TAMANHOS[size]} ${className}`}
    >
      {loading && (
        <span
          aria-hidden
          className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent"
        />
      )}
      {children}
    </button>
  )
}

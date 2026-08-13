import { forwardRef, useId, type SelectHTMLAttributes } from 'react'
import { IconSeta } from '@/assets/icons'

export interface OpcaoSelect {
  valor: string
  rotulo: string
}

interface TSelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  label: string
  opcoes: OpcaoSelect[]
  /** Primeira opção, de valor vazio. Ex.: "Todos". */
  vazio?: string
  error?: string
  ajuda?: string
}

/**
 * `<select>` nativo, estilizado. Nativo de propósito: no celular abre o seletor
 * do sistema, que é mais acessível e mais rápido que qualquer dropdown custom.
 */
export const TSelect = forwardRef<HTMLSelectElement, TSelectProps>(function TSelect(
  { label, opcoes, vazio, error, ajuda, className = '', id, ...props },
  ref,
) {
  const gerado = useId()
  const selectId = id ?? gerado
  const erroId = `${selectId}-erro`
  const ajudaId = `${selectId}-ajuda`

  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={selectId} className="text-caption text-txt-secondary">
        {label}
      </label>

      <div className="relative">
        <select
          {...props}
          id={selectId}
          ref={ref}
          aria-invalid={!!error}
          aria-describedby={error ? erroId : ajuda ? ajudaId : undefined}
          className={`h-[38px] w-full appearance-none rounded-md border bg-surface pl-3 pr-9
            text-body text-txt transition-shadow duration-150
            focus:outline-none focus:ring-[3px]
            disabled:cursor-not-allowed disabled:opacity-60
            ${
              error
                ? 'border-danger focus:border-danger focus:ring-danger/15'
                : 'border-line-strong focus:border-primary focus:ring-primary/10'
            }`}
        >
          {vazio !== undefined && <option value="">{vazio}</option>}
          {opcoes.map((o) => (
            <option key={o.valor} value={o.valor}>
              {o.rotulo}
            </option>
          ))}
        </select>

        <IconSeta className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-txt-muted" />
      </div>

      {error ? (
        <span id={erroId} role="alert" className="text-caption text-danger">
          {error}
        </span>
      ) : ajuda ? (
        <span id={ajudaId} className="text-caption text-txt-muted">
          {ajuda}
        </span>
      ) : null}
    </div>
  )
})

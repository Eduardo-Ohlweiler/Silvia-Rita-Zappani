import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react'

interface TEntryProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  /** Unidade ou ícone à direita — "cm", "kg", olho de senha. */
  suffix?: ReactNode
  ajuda?: string
}

/**
 * Campo de texto. `forwardRef` porque o react-hook-form registra a ref.
 *
 * O foco usa halo (`box-shadow`) em vez de `outline`, para não brigar com o
 * raio arredondado.
 */
export const TEntry = forwardRef<HTMLInputElement, TEntryProps>(function TEntry(
  { label, error, suffix, ajuda, className = '', id, ...props },
  ref,
) {
  const gerado = useId()
  const inputId = id ?? gerado
  const erroId = `${inputId}-erro`
  const ajudaId = `${inputId}-ajuda`

  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={inputId} className="text-caption text-txt-secondary">
        {label}
      </label>

      <div className="relative">
        <input
          {...props}
          id={inputId}
          ref={ref}
          aria-invalid={!!error}
          aria-describedby={error ? erroId : ajuda ? ajudaId : undefined}
          className={`h-[38px] w-full rounded-md border bg-surface px-3 text-body text-txt
            placeholder:text-txt-muted
            transition-shadow duration-150
            focus:outline-none focus:ring-[3px]
            disabled:cursor-not-allowed disabled:opacity-60
            ${suffix ? 'pr-11' : ''}
            ${
              error
                ? 'border-danger focus:border-danger focus:ring-danger/15'
                : 'border-line-strong focus:border-primary focus:ring-primary/10'
            }`}
        />
        {suffix && (
          <span className="absolute inset-y-0 right-0 flex items-center pr-3 text-caption text-txt-muted">
            {suffix}
          </span>
        )}
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

import { forwardRef, useId, type TextareaHTMLAttributes } from 'react'

interface TTextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
  ajuda?: string
}

/**
 * Campo de texto longo — observações, conduta, intercorrências.
 *
 * Espelha o `TEntry` em rótulo, foco, ajuda e erro. Existe porque o textarea
 * estava inline e cru no formulário da avaliação pediátrica, com as classes
 * copiadas à mão: com dois usos, a segunda cópia é onde as duas começam a
 * divergir.
 */
export const TTextArea = forwardRef<HTMLTextAreaElement, TTextAreaProps>(function TTextArea(
  { label, error, ajuda, className = '', id, rows = 5, ...props },
  ref,
) {
  const geradoId = useId()
  const campoId = id ?? geradoId
  const erroId = `${campoId}-erro`
  const ajudaId = `${campoId}-ajuda`

  return (
    <label htmlFor={campoId} className={`flex flex-col gap-1.5 ${className}`}>
      <span className="text-caption text-txt-secondary">{label}</span>

      <textarea
        ref={ref}
        id={campoId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? erroId : ajuda ? ajudaId : undefined}
        className={`w-full rounded-md border bg-surface px-3 py-2 text-body text-txt
          placeholder:text-txt-muted transition-shadow duration-150
          focus:outline-none focus:ring-[3px]
          ${
            error
              ? 'border-danger focus:border-danger focus:ring-danger/10'
              : 'border-line-strong focus:border-primary focus:ring-primary/10'
          }`}
        {...props}
      />

      {error ? (
        <span id={erroId} role="alert" className="text-caption text-danger">
          {error}
        </span>
      ) : ajuda ? (
        <span id={ajudaId} className="text-caption text-txt-muted">
          {ajuda}
        </span>
      ) : null}
    </label>
  )
})

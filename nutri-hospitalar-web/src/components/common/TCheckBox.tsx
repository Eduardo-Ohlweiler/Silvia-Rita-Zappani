import { forwardRef, useId, type InputHTMLAttributes } from 'react'

interface TCheckBoxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  error?: string
  ajuda?: string
}

/**
 * Caixa de seleção.
 *
 * <p>Estava na lista de "a criar quando a fatia pedir" do {@code docs/04} §4
 * desde a fatia 2, e é a ficha de anamnese que a pede: uma pergunta de opções
 * múltiplas é um grupo destas, e o formulário do modelo tem a caixa
 * "obrigatória" em cada linha.
 *
 * <p>Espelha o {@code TEntry} em foco, erro e ajuda. O alvo de toque é a linha
 * inteira, e não o quadrado de 16 px — o sistema é usado no celular, à beira do
 * leito.
 */
export const TCheckBox = forwardRef<HTMLInputElement, TCheckBoxProps>(function TCheckBox(
  { label, error, ajuda, className = '', id, disabled, ...props },
  ref,
) {
  const gerado = useId()
  const campoId = id ?? gerado
  const erroId = `${campoId}-erro`
  const ajudaId = `${campoId}-ajuda`

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <label
        htmlFor={campoId}
        className={`flex min-h-9 items-center gap-2.5 text-body text-txt
          ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
      >
        <input
          {...props}
          ref={ref}
          id={campoId}
          type="checkbox"
          disabled={disabled}
          aria-invalid={!!error}
          aria-describedby={error ? erroId : ajuda ? ajudaId : undefined}
          className="size-4 shrink-0 cursor-pointer rounded border-line-strong accent-primary
                     transition-shadow duration-150
                     focus:outline-none focus:ring-[3px] focus:ring-primary/10
                     disabled:cursor-not-allowed"
        />
        <span className="min-w-0">{label}</span>
      </label>

      {error ? (
        <p id={erroId} role="alert" className="text-caption text-danger">
          {error}
        </p>
      ) : ajuda ? (
        <p id={ajudaId} className="text-caption text-txt-muted">
          {ajuda}
        </p>
      ) : null}
    </div>
  )
})

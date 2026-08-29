import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react'
import { mascararColado, mascararDecimal } from '@/utils/format'

/**
 * `decimal` cobre quase tudo. `inteiro` é para o que o DTO tipa como `Integer`
 * (idade, administrações por dia) e para a PA, que é mmHg redondo.
 * `decimalComSinal` existe por um campo só — o balanço hídrico.
 */
export type MascaraNumerica = 'decimal' | 'inteiro' | 'decimalComSinal'

interface TEntryProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  /** Unidade ou ícone à direita — "cm", "kg", olho de senha. */
  suffix?: ReactNode
  ajuda?: string
  /** Máscara de centavos: dígitos entram pela direita, vírgula fixa. */
  mascara?: MascaraNumerica
  /** Casas decimais. Ignorado em `inteiro`; padrão 2 nas demais. */
  casas?: number
}

/**
 * Campo de texto. `forwardRef` porque o react-hook-form registra a ref.
 *
 * O foco usa halo (`box-shadow`) em vez de `outline`, para não brigar com o
 * raio arredondado.
 *
 * <b>Sobre a máscara.</b> Ela mora aqui, e não em cada `onChange`, porque são
 * ~70 campos numéricos: repetir a chamada setenta vezes é onde um deles fica
 * para trás. O componente reescreve `e.target.value` <i>antes</i> de repassar o
 * evento — o input é controlado, então corrigir o nó do DOM na hora é o que
 * impede o texto cru de ficar visível quando o valor mascarado não muda (digitar
 * uma letra em "0,68" não altera o estado, logo não há novo render).
 *
 * Colar é tratado à parte: o texto colado passa por `paraNumero` em vez de
 * virar acumulador de dígitos, senão colar "70" daria "0,70".
 */
export const TEntry = forwardRef<HTMLInputElement, TEntryProps>(function TEntry(
  { label, error, suffix, ajuda, mascara, casas, className = '', id, onChange, ...props },
  ref,
) {
  const gerado = useId()
  const inputId = id ?? gerado
  const erroId = `${inputId}-erro`
  const ajudaId = `${inputId}-ajuda`

  const casasEfetivas = mascara === 'inteiro' ? 0 : (casas ?? 2)
  const comSinal = mascara === 'decimalComSinal'

  const aoDigitar = !mascara
    ? onChange
    : (evento: React.ChangeEvent<HTMLInputElement>) => {
        const colou = (evento.nativeEvent as InputEvent).inputType === 'insertFromPaste'
        evento.target.value = colou
          ? mascararColado(evento.target.value, casasEfetivas, comSinal)
          : mascararDecimal(evento.target.value, casasEfetivas, comSinal)
        onChange?.(evento)
      }

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
          onChange={aoDigitar}
          inputMode={props.inputMode ?? (mascara === 'inteiro' ? 'numeric' : mascara ? 'decimal' : undefined)}
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

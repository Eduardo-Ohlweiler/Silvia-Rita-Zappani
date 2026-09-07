import { useId } from 'react'
import type { OpcaoSelect } from './TSelect'

interface TRadioProps {
  label: string
  opcoes: OpcaoSelect[]
  /** Valor escolhido, ou string vazia quando nada foi escolhido. */
  value: string
  onChange: (valor: string) => void
  /**
   * Rótulo da opção que representa **ausência de resposta**.
   *
   * <p>Sem ela, uma escolha feita não pode ser desfeita, e o formulário passa a
   * mentir: "Não" fica indistinguível de "ninguém perguntou". Num prontuário
   * essa é a diferença que importa, e é por isso que a ficha de anamnese usa
   * este componente no lugar de uma caixa de seleção para o sim/não.
   */
  vazio?: string
  error?: string
  ajuda?: string
  disabled?: boolean
  className?: string
  /** Empilha em vez de alinhar. Para listas de mais de três opções. */
  vertical?: boolean
}

/**
 * Grupo de rádios.
 *
 * <p>Estava na lista de "a criar quando a fatia pedir" do {@code docs/04} §4, e
 * é a ficha de anamnese que o pede: o sim/não de três estados e a pergunta de
 * opção única.
 *
 * <p>Não substitui o {@code TSelect}: rádio serve quando as opções são poucas e
 * ver todas de uma vez é a informação — "Sim / Não / Não informado" dentro de um
 * {@code <select>} esconderia justamente o terceiro estado.
 */
export function TRadio({
  label,
  opcoes,
  value,
  onChange,
  vazio,
  error,
  ajuda,
  disabled,
  className = '',
  vertical,
}: TRadioProps) {
  const grupo = useId()
  const erroId = `${grupo}-erro`
  const ajudaId = `${grupo}-ajuda`

  const todas = vazio ? [...opcoes, { valor: '', rotulo: vazio }] : opcoes

  return (
    <fieldset
      className={`flex flex-col gap-1.5 ${className}`}
      aria-invalid={!!error}
      aria-describedby={error ? erroId : ajuda ? ajudaId : undefined}
    >
      <legend className="text-caption text-txt-secondary">{label}</legend>

      <div className={`flex gap-x-5 gap-y-1 ${vertical ? 'flex-col' : 'flex-wrap items-center'}`}>
        {todas.map((opcao) => (
          <label
            key={opcao.valor || '__vazio'}
            className={`flex min-h-9 items-center gap-2 text-body
              ${opcao.valor === '' ? 'text-txt-secondary' : 'text-txt'}
              ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
          >
            <input
              type="radio"
              name={grupo}
              value={opcao.valor}
              checked={value === opcao.valor}
              disabled={disabled}
              onChange={() => onChange(opcao.valor)}
              className="size-4 shrink-0 cursor-pointer border-line-strong accent-primary
                         transition-shadow duration-150
                         focus:outline-none focus:ring-[3px] focus:ring-primary/10
                         disabled:cursor-not-allowed"
            />
            <span className="min-w-0">{opcao.rotulo}</span>
          </label>
        ))}
      </div>

      {error ? (
        <p id={erroId} role="alert" className="text-caption text-danger">
          {error}
        </p>
      ) : ajuda ? (
        <p id={ajudaId} className="text-caption text-txt-muted">
          {ajuda}
        </p>
      ) : null}
    </fieldset>
  )
}

import { useRef, type KeyboardEvent } from 'react'

export interface Aba {
  id: string
  rotulo: string
  /** Marca a aba com um ponto — ex.: campo obrigatório em falta lá dentro. */
  alerta?: boolean
}

interface TTabsProps {
  abas: Aba[]
  ativa: string
  onChange: (id: string) => void
  className?: string
}

/**
 * Abas de navegação dentro de uma tela.
 *
 * Existe para as telas de cálculo (doc 04 §7): empilhar medidas, dieta e
 * resultados num painel atrás do outro produz uma tela em que se digita em cima
 * e se procura o resultado lá embaixo. Cada aba é autocontida — poucas
 * entradas, e o resultado delas logo ali.
 *
 * No mobile a faixa rola na horizontal, e **só ela**: `overflow-x-auto` aqui
 * impede que a página inteira ganhe rolagem lateral, que é o que a regra 6
 * proíbe.
 *
 * Teclado conforme o padrão de `tablist`: setas andam entre as abas, Home e End
 * vão às pontas. Sem isso o componente é inacessível para quem não usa mouse.
 */
export function TTabs({ abas, ativa, onChange, className = '' }: TTabsProps) {
  const refs = useRef<Record<string, HTMLButtonElement | null>>({})

  function aoTeclar(evento: KeyboardEvent<HTMLDivElement>) {
    const atual = abas.findIndex((a) => a.id === ativa)
    if (atual < 0) return

    const destino =
      evento.key === 'ArrowRight'
        ? (atual + 1) % abas.length
        : evento.key === 'ArrowLeft'
          ? (atual - 1 + abas.length) % abas.length
          : evento.key === 'Home'
            ? 0
            : evento.key === 'End'
              ? abas.length - 1
              : -1

    if (destino < 0) return
    evento.preventDefault()
    const alvo = abas[destino]
    onChange(alvo.id)
    refs.current[alvo.id]?.focus()
  }

  return (
    <div
      role="tablist"
      onKeyDown={aoTeclar}
      className={`flex gap-1 overflow-x-auto border-b border-line ${className}`}
    >
      {abas.map((aba) => {
        const selecionada = aba.id === ativa
        return (
          <button
            key={aba.id}
            ref={(el) => {
              refs.current[aba.id] = el
            }}
            type="button"
            role="tab"
            id={`aba-${aba.id}`}
            aria-selected={selecionada}
            aria-controls={`painel-${aba.id}`}
            /* Só a aba ativa entra na ordem de tabulação: dentro de um
             * tablist, Tab sai para o conteúdo e as setas trocam de aba. */
            tabIndex={selecionada ? 0 : -1}
            onClick={() => onChange(aba.id)}
            className={`-mb-px flex shrink-0 items-center gap-2 whitespace-nowrap border-b-2 px-4 py-2.5
              text-body font-medium transition-colors
              focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/20
              ${
                selecionada
                  ? 'border-primary text-primary'
                  : 'border-transparent text-txt-secondary hover:border-line-strong hover:text-txt'
              }`}
          >
            {aba.rotulo}
            {aba.alerta && (
              <span
                aria-hidden="true"
                className="size-1.5 shrink-0 rounded-full bg-warning"
              />
            )}
          </button>
        )
      })}
    </div>
  )
}

interface TTabPanelProps {
  id: string
  ativa: string
  children: React.ReactNode
}

/** O painel de uma aba. Não renderiza nada quando a aba não é a ativa. */
export function TTabPanel({ id, ativa, children }: TTabPanelProps) {
  if (id !== ativa) return null
  return (
    <div role="tabpanel" id={`painel-${id}`} aria-labelledby={`aba-${id}`}>
      {children}
    </div>
  )
}

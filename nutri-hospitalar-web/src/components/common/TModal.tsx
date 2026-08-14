import { useEffect, useRef, type ReactNode } from 'react'
import { IconFechar } from '@/assets/icons'

interface TModalProps {
  aberto: boolean
  titulo: string
  onFechar: () => void
  children: ReactNode
  /** Rodapé com os botões. Sem ele, o modal é só leitura. */
  acoes?: ReactNode
  /** Largura máxima no desktop. No celular ocupa a tela. */
  largura?: 'sm' | 'md' | 'lg'
}

const LARGURAS = {
  sm: 'sm:max-w-md',
  md: 'sm:max-w-2xl',
  lg: 'sm:max-w-4xl',
}

/**
 * Janela sobreposta. Existe para o cadastro rápido dentro do formulário de
 * pessoa — interromper o preenchimento para cadastrar um responsável e voltar
 * perdendo o que já foi digitado seria pior que a janela.
 *
 * No celular ocupa a tela inteira: modal centralizado em 360px vira uma caixa
 * apertada com o teclado por cima.
 */
export function TModal({
  aberto,
  titulo,
  onFechar,
  children,
  acoes,
  largura = 'md',
}: TModalProps) {
  const caixaRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!aberto) return

    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') onFechar()
    }

    document.addEventListener('keydown', aoTeclar)
    // Sem isto a página de trás rola junto quando o modal chega ao fim
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    caixaRef.current?.focus()

    return () => {
      document.removeEventListener('keydown', aoTeclar)
      document.body.style.overflow = overflowAnterior
    }
  }, [aberto, onFechar])

  if (!aberto) return null

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      {/* Cor fixa de propósito: o scrim escurece o conteúdo nos dois temas. */}
      <div
        onClick={onFechar}
        aria-hidden
        className="absolute inset-0 bg-navy/50 backdrop-blur-[2px]"
      />

      <div
        ref={caixaRef}
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        tabIndex={-1}
        className={`relative flex max-h-dvh w-full flex-col rounded-t-lg border border-line
          bg-surface shadow-card outline-none
          sm:max-h-[90dvh] sm:rounded-lg ${LARGURAS[largura]}`}
      >
        <header className="flex shrink-0 items-center justify-between gap-4 border-b border-line px-5 py-4">
          <h2 className="text-h2 font-medium text-txt">{titulo}</h2>
          <button
            type="button"
            onClick={onFechar}
            aria-label="Fechar"
            className="grid size-9 shrink-0 place-items-center rounded-md text-txt-secondary
              hover:bg-surface-alt"
          >
            <IconFechar className="size-5" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>

        {acoes && (
          <footer className="flex shrink-0 flex-col-reverse gap-2 border-t border-line px-5 py-4 sm:flex-row sm:justify-end">
            {acoes}
          </footer>
        )}
      </div>
    </div>
  )
}

import { IconAnterior, IconProximo } from '@/assets/icons'
import type { Page } from '@/types/comum'

interface TDataGridFooterProps<T> {
  pagina?: Page<T>
  onPaginaChange: (pagina: number) => void
}

export function TDataGridFooter<T>({ pagina, onPaginaChange }: TDataGridFooterProps<T>) {
  if (!pagina || pagina.totalElements === 0) return null

  const primeiro = pagina.number * pagina.size + 1
  const ultimo = Math.min(primeiro + pagina.content.length - 1, pagina.totalElements)

  return (
    <div className="flex flex-col items-center justify-between gap-3 sm:flex-row">
      <p className="text-caption text-txt-secondary">
        {primeiro}–{ultimo} de {pagina.totalElements}
      </p>

      <div className="flex items-center gap-1">
        <BotaoPagina
          rotulo="Página anterior"
          desabilitado={pagina.first}
          onClick={() => onPaginaChange(pagina.number - 1)}
        >
          <IconAnterior className="size-4" />
        </BotaoPagina>

        <span className="px-2 text-caption text-txt-secondary">
          {pagina.number + 1} / {pagina.totalPages}
        </span>

        <BotaoPagina
          rotulo="Próxima página"
          desabilitado={pagina.last}
          onClick={() => onPaginaChange(pagina.number + 1)}
        >
          <IconProximo className="size-4" />
        </BotaoPagina>
      </div>
    </div>
  )
}

function BotaoPagina({
  rotulo,
  desabilitado,
  onClick,
  children,
}: {
  rotulo: string
  desabilitado: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={rotulo}
      disabled={desabilitado}
      onClick={onClick}
      className="grid size-8 place-items-center rounded-md border border-line-strong
        text-txt-secondary transition-colors hover:bg-surface-alt hover:text-txt
        disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
    >
      {children}
    </button>
  )
}

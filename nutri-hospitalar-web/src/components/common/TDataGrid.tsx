import type { ReactNode } from 'react'
import { IconVazio } from '@/assets/icons'

export interface Coluna<T> {
  chave: string
  cabecalho: string
  render: (linha: T) => ReactNode
  /** Some antes das demais em telas médias. */
  secundaria?: boolean
  /** Alinha à direita e aplica numerais tabulares. */
  numerica?: boolean
  className?: string
}

interface TDataGridProps<T> {
  colunas: Coluna<T>[]
  linhas: T[]
  chaveDe: (linha: T) => string
  carregando?: boolean
  onLinhaClick?: (linha: T) => void
  /** Título da linha no modo cartão (mobile). */
  tituloCartao?: (linha: T) => ReactNode
  vazioTitulo?: string
  vazioDescricao?: string
  acoes?: (linha: T) => ReactNode
}

/**
 * Tabela no desktop, **lista de cartões no mobile** — regra de responsividade
 * do projeto. Uma tabela de 6 colunas em 360px de largura é ilegível; abaixo de
 * `md` cada linha vira um cartão com rótulo e valor empilhados.
 */
export function TDataGrid<T>({
  colunas,
  linhas,
  chaveDe,
  carregando = false,
  onLinhaClick,
  tituloCartao,
  vazioTitulo = 'Nenhum registro encontrado',
  vazioDescricao = 'Ajuste os filtros ou cadastre o primeiro.',
  acoes,
}: TDataGridProps<T>) {
  if (carregando) return <Skeleton />

  if (linhas.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-lg border border-line bg-surface px-6 py-14 text-center shadow-card">
        <IconVazio className="size-10 text-txt-muted" />
        <p className="text-h3 font-medium text-txt">{vazioTitulo}</p>
        <p className="text-caption text-txt-secondary">{vazioDescricao}</p>
      </div>
    )
  }

  const clicavel = !!onLinhaClick

  return (
    <>
      {/* ── Desktop: tabela ─────────────────────────────────────────── */}
      <div className="hidden overflow-hidden rounded-lg border border-line bg-surface shadow-card md:block">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-body">
            <thead>
              <tr className="bg-surface-alt">
                {colunas.map((c) => (
                  <th
                    key={c.chave}
                    scope="col"
                    className={`px-4 py-2.5 text-left text-h3 font-medium text-txt-secondary
                      ${c.numerica ? 'text-right' : ''}
                      ${c.secundaria ? 'hidden lg:table-cell' : ''}`}
                  >
                    {c.cabecalho}
                  </th>
                ))}
                {acoes && <th scope="col" className="w-px px-4 py-2.5" />}
              </tr>
            </thead>
            <tbody>
              {linhas.map((linha) => (
                <tr
                  key={chaveDe(linha)}
                  onClick={clicavel ? () => onLinhaClick(linha) : undefined}
                  className={`border-t border-line transition-colors
                    ${clicavel ? 'cursor-pointer hover:bg-surface-alt' : ''}`}
                >
                  {colunas.map((c) => (
                    <td
                      key={c.chave}
                      className={`h-linha px-4 text-txt
                        ${c.numerica ? 'text-right numeric' : ''}
                        ${c.secundaria ? 'hidden lg:table-cell' : ''}
                        ${c.className ?? ''}`}
                    >
                      {c.render(linha)}
                    </td>
                  ))}
                  {acoes && (
                    <td
                      className="px-4 text-right"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex justify-end gap-1">{acoes(linha)}</div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Mobile: cartões ─────────────────────────────────────────── */}
      <ul className="flex flex-col gap-3 md:hidden">
        {linhas.map((linha) => (
          <li
            key={chaveDe(linha)}
            onClick={clicavel ? () => onLinhaClick(linha) : undefined}
            className={`rounded-lg border border-line bg-surface p-4 shadow-card
              ${clicavel ? 'cursor-pointer active:bg-surface-alt' : ''}`}
          >
            {tituloCartao && (
              <p className="mb-3 text-h3 font-medium text-txt">{tituloCartao(linha)}</p>
            )}
            <dl className="flex flex-col gap-2">
              {colunas.map((c) => (
                <div key={c.chave} className="flex items-start justify-between gap-3">
                  <dt className="text-caption text-txt-secondary">{c.cabecalho}</dt>
                  <dd className={`text-right text-body text-txt ${c.numerica ? 'numeric' : ''}`}>
                    {c.render(linha)}
                  </dd>
                </div>
              ))}
            </dl>
            {acoes && (
              <div
                className="mt-3 flex flex-wrap gap-2 border-t border-line pt-3"
                onClick={(e) => e.stopPropagation()}
              >
                {acoes(linha)}
              </div>
            )}
          </li>
        ))}
      </ul>
    </>
  )
}

function Skeleton() {
  return (
    <div
      role="status"
      aria-label="Carregando"
      className="overflow-hidden rounded-lg border border-line bg-surface shadow-card"
    >
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex h-linha items-center gap-4 border-t border-line px-4 first:border-t-0">
          <div className="h-3 w-1/3 animate-pulse rounded bg-surface-alt" />
          <div className="h-3 w-1/4 animate-pulse rounded bg-surface-alt" />
          <div className="ml-auto h-3 w-16 animate-pulse rounded bg-surface-alt" />
        </div>
      ))}
    </div>
  )
}

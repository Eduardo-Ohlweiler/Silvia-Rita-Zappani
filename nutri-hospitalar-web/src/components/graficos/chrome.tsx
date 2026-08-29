import type { ReactNode } from 'react'

/**
 * As peças comuns a todos os gráficos do sistema — pediatria e UTI.
 *
 * Grade e eixos são hairline sólidos, um tom fora da superfície — tracejado
 * significa limiar (a linha dos 100 % de adequação), não grade. Texto de eixo e
 * de tooltip usa token de tinta, nunca a cor da série: a cor mora na marca ao
 * lado, não na palavra.
 */
interface TooltipLinha {
  rotulo: string
  valor: ReactNode
  cor?: string
}

/**
 * Tooltip do sistema, em vez do padrão do recharts — que herda fundo branco
 * fixo e some no tema escuro.
 */
export function TooltipCartao({
  titulo,
  linhas,
}: {
  titulo: ReactNode
  linhas: TooltipLinha[]
}) {
  return (
    <div className="rounded-md border border-line bg-surface px-3 py-2 shadow-modal">
      <p className="mb-1 text-caption font-medium text-txt">{titulo}</p>
      <ul className="flex flex-col gap-0.5">
        {linhas.map((l) => (
          <li key={l.rotulo} className="flex items-center gap-2 text-caption text-txt-secondary">
            {l.cor && (
              <span
                aria-hidden="true"
                className="size-2 shrink-0 rounded-full"
                style={{ background: l.cor }}
              />
            )}
            <span>{l.rotulo}</span>
            <span className="numeric ml-auto font-medium text-txt">{l.valor}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * Título de um gráfico, com a nota de referência.
 *
 * A referência não é enfeite: quem lê precisa saber de onde vem a faixa
 * desenhada por trás dos dados.
 */
export function TituloGrafico({
  children,
  referencia,
  acoes,
}: {
  children: ReactNode
  referencia?: string
  acoes?: ReactNode
}) {
  return (
    <header className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
      <div className="flex flex-col">
        <h3 className="text-h3 font-medium text-txt">{children}</h3>
        {referencia && <span className="text-caption text-txt-muted">{referencia}</span>}
      </div>
      {acoes}
    </header>
  )
}

/**
 * Número solto com o seu rótulo — a linha de indicadores no topo dos painéis.
 *
 * Mora aqui, e não em cada painel, porque os três da UTI e os dois da pediatria
 * mostram a mesma coisa: um valor grande e uma etiqueta pequena.
 */
export function Indicador({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-caption text-txt-secondary">{rotulo}</span>
      <span className="text-h3 font-semibold text-txt">{valor}</span>
    </div>
  )
}

/** Estado vazio de um gráfico — sem dado não se desenha eixo nenhum. */
export function GraficoVazio({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-[220px] items-center justify-center rounded-md border border-dashed border-line px-4 text-center">
      <p className="text-caption text-txt-muted">{children}</p>
    </div>
  )
}

export { EIXO, GRADE } from '@/components/graficos/eixos'

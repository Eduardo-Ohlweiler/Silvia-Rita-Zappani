import type { ReactNode } from 'react'

interface TResultGroupProps {
  titulo: string
  /** Uma linha explicando o que o grupo responde. Opcional. */
  descricao?: string
  colunas?: 2 | 3 | 4
  children: ReactNode
}

/**
 * Um subgrupo de resultados dentro de uma aba.
 *
 * A aba de antropometria da UTI tem **15 resultados**. Empilhados numa grade só,
 * viram um paredão em que nada se destaca — e o profissional procura o
 * diagnóstico no meio das estimativas. Agrupados em quatro blocos nomeados
 * (estimativas · diagnóstico · metas de peso · perda e depleção), cada número
 * fica ao lado dos que respondem à mesma pergunta.
 *
 * Deliberadamente simples: um título, uma grade e nada mais. Não tem estado, não
 * colapsa, não guarda preferência — quem precisa de menos na tela muda de aba.
 */
export function TResultGroup({ titulo, descricao, colunas = 3, children }: TResultGroupProps) {
  const grade =
    colunas === 2
      ? 'sm:grid-cols-2'
      : colunas === 4
        ? 'sm:grid-cols-2 lg:grid-cols-4'
        : 'sm:grid-cols-2 lg:grid-cols-3'

  return (
    <section className="flex flex-col gap-3">
      <header className="flex flex-col gap-0.5">
        <h3 className="text-caption font-semibold uppercase tracking-wide text-txt-secondary">
          {titulo}
        </h3>
        {descricao && <p className="text-caption text-txt-muted">{descricao}</p>}
      </header>

      <div className={`grid gap-5 ${grade}`}>{children}</div>
    </section>
  )
}

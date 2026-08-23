import type { ReactNode } from 'react'

export interface ColunaResultado<T> {
  chave: string
  cabecalho: string
  /** Alinha à direita e aplica a fonte tabular. */
  numerica?: boolean
  render: (linha: T) => ReactNode
}

interface TResultTableProps<T> {
  titulo?: string
  descricao?: string
  colunas: ColunaResultado<T>[]
  linhas: T[]
  chaveDe: (linha: T) => string | number
  /** Enquanto recalcula, a tabela anterior fica visível e esmaecida. */
  recalculando?: boolean
  vazio?: string
}

/**
 * Uma tabela densa e somente leitura, resultado de cálculo.
 *
 * **Não é `TDataGrid`.** Aquele é lista paginada de entidade — tem clique de
 * linha, ações, rodapé de paginação e vira cartão no mobile porque cada linha é
 * um registro que se abre. Esta é o oposto: poucas linhas, todas juntas, nenhuma
 * clicável, e a leitura é por coluna (a progressão do dia 1 ao 4, o módulo
 * proteico por produto). Transformar em cartão quebraria a comparação, que é o
 * ponto.
 *
 * No mobile ela rola **dentro do próprio contêiner** — nunca a página, que é o
 * que a regra 6 proíbe.
 */
export function TResultTable<T>({
  titulo,
  descricao,
  colunas,
  linhas,
  chaveDe,
  recalculando = false,
  vazio = 'Sem dados para exibir',
}: TResultTableProps<T>) {
  return (
    <section className="flex flex-col gap-3">
      {(titulo || descricao) && (
        <header className="flex flex-col gap-0.5">
          {titulo && (
            <h3 className="text-caption font-semibold uppercase tracking-wide text-txt-secondary">
              {titulo}
            </h3>
          )}
          {descricao && <p className="text-caption text-txt-muted">{descricao}</p>}
        </header>
      )}

      {linhas.length === 0 ? (
        <p className="text-caption text-txt-muted">{vazio}</p>
      ) : (
        <div
          className={`overflow-x-auto rounded-md border border-line transition-opacity duration-150
            ${recalculando ? 'opacity-50' : 'opacity-100'}`}
        >
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-line bg-surface-alt">
                {colunas.map((c) => (
                  <th
                    key={c.chave}
                    scope="col"
                    className={`px-3 py-2 text-caption font-semibold text-txt-secondary
                      ${c.numerica ? 'text-right' : 'text-left'}`}
                  >
                    {c.cabecalho}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {linhas.map((linha) => (
                <tr key={chaveDe(linha)} className="border-b border-line last:border-b-0">
                  {colunas.map((c) => (
                    <td
                      key={c.chave}
                      className={`whitespace-nowrap px-3 py-2 text-body text-txt
                        ${c.numerica ? 'numeric text-right' : 'text-left'}`}
                    >
                      {c.render(linha)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

/**
 * Os depoimentos do site atual (WordPress) falam de "fluxo de caixa",
 * "pró-labore" e "contador" — vieram de outro nicho (financeiro/contábil) e
 * não fazem sentido para nutrição hospitalar. Os textos abaixo são
 * placeholders de exemplo, sinalizados para a Silvia substituir por
 * depoimentos reais de nutricionistas que passaram pela Sessão Estratégica,
 * antes de publicar a página.
 */
const DEPOIMENTOS_PLACEHOLDER = [
  {
    texto: '[Depoimento a confirmar com a Silvia — exemplo de estrutura de texto]',
    nome: '[Nome da nutricionista]',
  },
  {
    texto: '[Depoimento a confirmar com a Silvia — exemplo de estrutura de texto]',
    nome: '[Nome da nutricionista]',
  },
  {
    texto: '[Depoimento a confirmar com a Silvia — exemplo de estrutura de texto]',
    nome: '[Nome da nutricionista]',
  },
]

export function Depoimentos() {
  return (
    <section className="bg-[var(--landing-bg)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto max-w-5xl">
        <h2 className="text-center text-2xl sm:text-3xl">
          Veja o que nutricionistas que{' '}
          <span className="text-[var(--landing-primary)]">já passaram pela Sessão Estratégica</span> têm
          a dizer:
        </h2>

        <div className="mt-4 rounded-lg border border-dashed border-[var(--landing-primary)]/40 bg-[var(--landing-primary)]/5 p-3 text-center text-xs text-[var(--landing-muted)]">
          Depoimentos de exemplo — aguardando conteúdo real da Silvia antes de publicar.
        </div>

        <div className="mt-8 flex snap-x snap-mandatory gap-4 overflow-x-auto pb-4">
          {DEPOIMENTOS_PLACEHOLDER.map((depoimento, indice) => (
            <figure
              key={indice}
              className="w-[85%] shrink-0 snap-start rounded-2xl border border-[var(--landing-line)] bg-[var(--landing-bg-alt)] p-6 sm:w-[45%] md:w-[30%]"
            >
              <blockquote className="text-sm italic text-[var(--landing-ink)]/90">
                “{depoimento.texto}”
              </blockquote>
              <figcaption className="mt-4 text-sm font-semibold text-[var(--landing-primary)]">
                {depoimento.nome}
              </figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  )
}

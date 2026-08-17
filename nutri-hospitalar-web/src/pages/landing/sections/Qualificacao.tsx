const ITENS = [
  'É estudante de nutrição no último ano ou recém-formada e quer atuar no hospital',
  'Atua em outra área e quer migrar para o ambiente hospitalar',
  'Trabalha no hospital, mas sente insegurança nas condutas e nas decisões clínicas',
  'Está despreparada para o dia a dia hospitalar',
  'Sabe a teoria, mas se sente perdida na hora de executar na prática',
  'Quer estrutura real para organizar sua rotina clínica',
  'Tem medo de ser questionada pela equipe multidisciplinar e não saber responder',
  'Quer se posicionar com autoridade',
  'Quer crescer profissionalmente e assumir mais responsabilidade no hospital',
]

export function Qualificacao() {
  return (
    <section className="bg-[var(--landing-bg)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto max-w-3xl text-center">
        <h2 className="text-2xl uppercase sm:text-3xl md:text-4xl">
          Essa sessão <span className="text-[var(--landing-primary)]">é para você que</span>
        </h2>
        <span className="mt-4 inline-block rounded-full border border-[var(--landing-line)] px-6 py-2 text-xs uppercase tracking-wide text-[var(--landing-muted)] sm:text-sm">
          Clique nas opções que combinam com o seu momento:
        </span>
      </div>

      <div className="mx-auto mt-8 max-w-3xl overflow-hidden rounded-2xl bg-[var(--landing-bg-alt)]">
        <ul className="divide-y divide-white">
          {ITENS.map((item) => (
            <li key={item}>
              <label className="flex cursor-pointer items-center gap-4 px-5 py-4 sm:px-6">
                <input type="checkbox" className="peer sr-only" />
                <span className="relative flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 border-[var(--landing-primary)]/40 peer-checked:border-[var(--landing-primary)] peer-checked:bg-[var(--landing-primary)] peer-checked:[&>svg]:opacity-100">
                  <svg
                    viewBox="0 0 21 14"
                    className="h-3 w-4 fill-none stroke-white opacity-0"
                    aria-hidden="true"
                  >
                    <polyline
                      points="1,5 6,9 14,1"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </span>
                <span className="text-sm sm:text-base">{item}</span>
              </label>
            </li>
          ))}
        </ul>

        <p className="bg-[var(--landing-primary)]/10 px-6 py-5 text-center text-sm sm:text-base">
          Se você marcou pelo menos 2 opções,{' '}
          <strong className="text-[var(--landing-primary)]">
            a sessão estratégica foi feita para você.
          </strong>
        </p>
      </div>
    </section>
  )
}

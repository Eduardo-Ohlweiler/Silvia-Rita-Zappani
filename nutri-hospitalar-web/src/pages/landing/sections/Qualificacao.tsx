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
      <div className="mx-auto max-w-3xl">
        <h2 className="text-center text-2xl sm:text-3xl">
          Essa sessão <span className="text-[var(--landing-primary)]">é para você que</span>
        </h2>
        <p className="mt-2 text-center text-[var(--landing-muted)]">
          Clique nas opções que combinam com o seu momento:
        </p>

        <ul className="mt-8 space-y-3">
          {ITENS.map((item) => (
            <li key={item}>
              <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-[var(--landing-line)] bg-[var(--landing-bg-alt)] p-4 transition hover:border-[var(--landing-primary)]">
                <input
                  type="checkbox"
                  className="mt-1 h-5 w-5 shrink-0 accent-[var(--landing-primary)]"
                />
                <span className="text-sm sm:text-base">{item}</span>
              </label>
            </li>
          ))}
        </ul>

        <p className="mt-8 text-center text-lg font-medium">
          Se você marcou pelo menos 2 opções,{' '}
          <span className="text-[var(--landing-primary)]">a sessão estratégica foi feita para você.</span>
        </p>
      </div>
    </section>
  )
}

const BENEFICIOS = [
  'Se sentir preparada e segura para assumir e atuar no hospital',
  'Ter clareza e organização para estruturar sua rotina clínica com firmeza',
  'Saber se comunicar com médicos e equipe sem medo de ser questionada',
  'Reduzir a ansiedade e a autocrítica que consomem sua energia fora do trabalho',
  'Ser respeitada pela equipe multiprofissional e ter suas condutas levadas a sério',
  'Construir autoridade clínica e ser reconhecida como referência dentro do hospital',
  'Assumir responsabilidades maiores e crescer dentro do hospital com confiança',
]

function IconeCheck() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      className="h-6 w-6 shrink-0 text-[var(--landing-primary)]"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="11" stroke="currentColor" strokeWidth="1.5" />
      <path
        d="M7.5 12.5l2.8 2.8 6-6.2"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function Beneficios() {
  return (
    <section className="bg-[var(--landing-bg-alt)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto max-w-3xl">
        <h2 className="text-center text-2xl sm:text-3xl">
          Esse é o começo da <span className="text-[var(--landing-primary)]">sua virada profissional!</span>
        </h2>
        <p className="mt-2 text-center text-[var(--landing-ink)]/90">
          A Sessão Estratégica com Silvia Zappani <strong>é o primeiro passo para você:</strong>
        </p>

        <ul className="mt-8 space-y-4">
          {BENEFICIOS.map((item) => (
            <li key={item} className="flex items-start gap-3">
              <IconeCheck />
              <span className="text-sm sm:text-base">{item}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

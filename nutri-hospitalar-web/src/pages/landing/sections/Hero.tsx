import logoSilvia from '../assets/img/logo-silvia.svg'
import { PandaVideoPlayer } from '../components/PandaVideoPlayer'

export function Hero() {
  return (
    <section className="bg-[var(--landing-bg-alt)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-6 text-center">
        <img src={logoSilvia} alt="Silvia Zappani" className="h-auto w-40 sm:w-48" />

        <h1 className="text-3xl leading-tight sm:text-4xl md:text-5xl">
          Segurança clínica hospitalar{' '}
          <span className="text-[var(--landing-primary)]">na prática para nutricionistas</span>
        </h1>

        <p className="max-w-2xl text-base text-[var(--landing-muted)] sm:text-lg">
          Na Sessão Estratégica gratuita com Silvia Zappani, nutricionista com 13 anos de
          experiência hospitalar, você descobre o que realmente precisa para atuar com
          segurança, se posicionar com a equipe multidisciplinar e ser respeitada dentro do
          hospital.
        </p>

        <div className="w-full max-w-xl">
          <PandaVideoPlayer videoId="ef45bada-f86c-46c4-af71-211680c041a6" />
        </div>

        <a
          href="#sessao-estrategica"
          className="inline-flex items-center justify-center rounded-full border-2 border-[var(--landing-primary)] px-8 py-4 text-center font-semibold text-[var(--landing-primary)] transition hover:bg-[var(--landing-primary)] hover:text-white"
        >
          Quero garantir minha vaga gratuita
        </a>
      </div>
    </section>
  )
}

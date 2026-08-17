import fundoHero from '../assets/img/hero-fundo.jpeg'
import logoSilvia from '../assets/img/logo-silvia.svg'
import retratoSilvia from '../assets/img/silvia-retrato.webp'
import { PandaVideoPlayer } from '../components/PandaVideoPlayer'

export function Hero() {
  return (
    <section className="relative overflow-hidden bg-[var(--landing-bg-alt)]">
      {/* Fundo único cobrindo a seção inteira, com o retrato ancorado à direita
          — o conteúdo fica sobreposto, sem divisão em colunas. */}
      <div className="absolute inset-0" aria-hidden="true">
        <img src={fundoHero} alt="" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-r from-white via-white/85 to-transparent md:via-white/70" />
        <img
          src={retratoSilvia}
          alt=""
          className="absolute right-0 bottom-0 hidden h-full w-auto max-w-[55%] object-contain object-bottom md:block"
        />
      </div>

      <div className="relative mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-10 text-center sm:px-6 sm:py-14 md:max-w-6xl md:items-start md:py-20 md:text-left">
        <img src={logoSilvia} alt="Silvia Zappani" className="h-auto w-40 sm:w-48" />

        <h1 className="max-w-xl text-3xl leading-tight sm:text-4xl md:text-5xl">
          Segurança clínica hospitalar{' '}
          <span className="text-[var(--landing-primary)]">na prática para nutricionistas</span>
        </h1>

        <p className="max-w-xl text-base text-[var(--landing-muted)] sm:text-lg">
          Na Sessão Estratégica gratuita com Silvia Zappani, nutricionista com 13 anos de
          experiência hospitalar, você descobre o que realmente precisa para atuar com segurança,
          se posicionar com a equipe multidisciplinar e ser respeitada dentro do hospital.
        </p>

        <div className="w-full max-w-xl">
          <PandaVideoPlayer videoId="ef45bada-f86c-46c4-af71-211680c041a6" />
        </div>

        <a
          href="#sessao-estrategica"
          className="inline-flex items-center justify-center rounded-full border-2 border-[var(--landing-primary)] bg-white/80 px-8 py-4 text-center font-semibold text-[var(--landing-primary)] transition hover:bg-[var(--landing-primary)] hover:text-white"
        >
          Quero garantir minha vaga gratuita
        </a>
      </div>
    </section>
  )
}

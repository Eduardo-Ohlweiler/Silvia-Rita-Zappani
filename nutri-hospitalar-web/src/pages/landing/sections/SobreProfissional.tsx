import fotoSilvia from '../assets/img/sobre-profissional.webp'

export function SobreProfissional() {
  return (
    <section className="bg-[var(--landing-bg-alt)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto grid max-w-5xl gap-8 md:grid-cols-2 md:items-center md:gap-12">
        <div className="order-2 md:order-1">
          <p className="font-['Prata'] text-sm uppercase tracking-wide text-[var(--landing-muted)]">
            Quem vai conduzir a sessão?
          </p>
          <h2 className="mt-2 text-2xl sm:text-3xl">
            <span className="text-[var(--landing-primary)]">Silvia Zappani,</span> nutricionista
            hospitalar
          </h2>

          <p className="mt-4 text-base text-[var(--landing-ink)]/90">
            Coordenadora e Responsável Técnica de um hospital credenciado pela ONA. Foi ela
            quem desenvolveu os POPs, os protocolos e toda a estrutura técnica que elevou o
            serviço de nutrição ao nível de excelência que tem hoje. São 16 anos de formada,
            mais de 13 deles dentro do ambiente hospitalar.
          </p>
          <p className="mt-4 text-base text-[var(--landing-ink)]/90">
            Silvia começou como substituta eventual, sem manual, sem direcionamento e com
            muito medo de errar. Cada decisão clínica era um peso e cada conduta, uma dúvida.
            Ela construiu a profissional que é hoje passo a passo, dentro do hospital, na
            pressão real do dia a dia.
          </p>
          <p className="mt-4 text-base text-[var(--landing-ink)]/90">
            Hoje, quando recebe estagiárias e nutricionistas recém-formadas no hospital, ela
            se reconhece nelas. E é por isso que criou essa Sessão Estratégica para encurtar
            o caminho que ela percorreu sozinha.
          </p>
        </div>

        <div className="order-1 md:order-2">
          <img
            src={fotoSilvia}
            alt="Silvia Zappani"
            className="mx-auto w-full max-w-sm rounded-2xl object-cover shadow-lg"
          />
        </div>
      </div>
    </section>
  )
}

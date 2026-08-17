import pilar1 from '../assets/img/pilar-1.webp'
import pilar2 from '../assets/img/pilar-2.webp'
import pilar3 from '../assets/img/pilar-3.webp'
import pilar4 from '../assets/img/pilar-4.webp'

const PILARES = [
  {
    imagem: pilar1,
    titulo: 'O diagnóstico da sua situação atual',
    texto:
      'Um mapeamento do seu momento atual: o que já sabe, o que ainda trava e o que está impedindo sua evolução clínica na prática.',
  },
  {
    imagem: pilar2,
    titulo: 'Os pilares da segurança hospitalar real',
    texto:
      'Você entende o que realmente faz a diferença entre uma nutricionista insegura e uma que atua com autoridade, independente do tempo de formada ou da área em que atuou antes.',
  },
  {
    imagem: pilar3,
    titulo: 'O que você precisa desenvolver a partir de agora',
    texto:
      'Com base no seu perfil, são identificados os pontos que precisam ser trabalhados para você se sentir preparada e respeitada dentro do hospital.',
  },
  {
    imagem: pilar4,
    titulo: 'Seu próximo passo',
    texto:
      'Você sai com um direcionamento personalizado: o que priorizar, o que desenvolver e como construir segurança clínica real no seu dia a dia hospitalar.',
  },
]

export function EstruturaSessao() {
  return (
    <section className="bg-[var(--landing-bg)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto max-w-5xl">
        <h2 className="text-center text-2xl sm:text-3xl">
          30 minutos de conversa direta, personalizada{' '}
          <span className="text-[var(--landing-primary)]">para o seu momento</span>
        </h2>
        <p className="mt-2 text-center text-[var(--landing-muted)]">
          Ao final, você ganha clareza porque entende:
        </p>

        <div className="mt-10 grid gap-8 sm:grid-cols-2">
          {PILARES.map((pilar) => (
            <div key={pilar.titulo} className="flex flex-col gap-4">
              <img
                src={pilar.imagem}
                alt=""
                className="w-full rounded-xl object-cover"
                loading="lazy"
              />
              <h3 className="text-lg">{pilar.titulo}</h3>
              <p className="text-sm text-[var(--landing-ink)]/90">{pilar.texto}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

import imagemOferta from '../assets/img/oferta.webp'
import { WhatsAppButton } from '../components/WhatsAppButton'

const FAQ = [
  {
    pergunta: 'Quanto tempo dura a sessão?',
    resposta:
      'A sessão estratégica tem duração de 30 minutos. É um tempo totalmente focado na sua realidade. Sem enrolação, sem conteúdo genérico, cada minuto é direcionado para entender onde você está, o que está te travando e qual é o seu próximo passo.',
  },
  {
    pergunta: 'O que acontece durante a sessão?',
    resposta:
      'A sessão é conduzida pela nutricionista Silvia Zappani. Começa com um diagnóstico do seu momento atual: onde você está, o que já sabe e o que ainda trava. Em seguida, são identificados os pontos que precisam ser desenvolvidos para você ter segurança clínica hospitalar na prática. Por fim, você sai com um direcionamento concreto: o que priorizar, por onde começar e como avançar com mais confiança no dia a dia.',
  },
  {
    pergunta: 'A sessão é realmente gratuita?',
    resposta:
      'Sim, a sessão é 100% gratuita. Mas isso não significa que ela é sem compromisso. Para que a conversa seja realmente aproveitada, pedimos que você apareça no horário combinado e esteja genuinamente disposta a olhar para a sua realidade. O seu tempo vale, e o da Silvia também.',
  },
  {
    pergunta: 'Preciso ter experiência hospitalar para participar?',
    resposta:
      'Não. A sessão foi pensada tanto para nutricionistas que ainda não atuam no hospital quanto para aquelas que já estão no ambiente hospitalar, mas se sentem inseguras na prática. O que importa é que você tenha o diploma (ou esteja próxima de se formar) e queira desenvolver segurança clínica real.',
  },
  {
    pergunta: 'Como funciona o agendamento?',
    resposta:
      'Ao clicar no botão desta página, você abre uma conversa direto no WhatsApp da Silvia. É por lá que combinamos o melhor horário para a sua sessão.',
  },
  {
    pergunta: 'Essa sessão é para mim se eu já fiz vários cursos e ainda me sinto insegura?',
    resposta:
      'Especialmente para você. O problema raramente é falta de conteúdo, é falta de estrutura e direcionamento. A sessão foi criada para quem já estudou bastante, mas ainda sente que falta algo para transformar conhecimento em segurança clínica real.',
  },
]

export function OfertaFaq() {
  return (
    <section id="sessao-estrategica" className="bg-[var(--landing-bg-alt)] px-4 py-10 sm:px-6 md:py-16">
      <div className="mx-auto grid max-w-5xl gap-10 md:grid-cols-2 md:items-center">
        <div className="order-2 text-center md:order-1 md:text-left">
          <p className="text-sm uppercase tracking-wide text-[var(--landing-muted)]">
            A faculdade te ensinou a teoria. Mas o hospital exige muito mais do que isso
          </p>
          <p className="mt-2 font-['Prata'] text-lg text-[var(--landing-primary)]">
            Sessão Estratégica
          </p>
          <h2 className="mt-2 text-2xl sm:text-3xl">
            Entenda o que você precisa saber para ter{' '}
            <span className="text-[var(--landing-primary)]">
              segurança no atendimento clínico hospitalar
            </span>
          </h2>
          <p className="mt-4 text-[var(--landing-ink)]/90">
            A segurança clínica nasce de estrutura, direcionamento e prática orientada. O
            objetivo é ajudar nutricionistas iniciantes ou recém-formadas que desejam atuar na
            área hospitalar.
          </p>
          <p className="mt-4 font-semibold">
            30 minutos ao vivo, personalizados para a sua realidade.
          </p>

          <div className="mt-6">
            <WhatsAppButton rotulo="Quero agendar sessão gratuita" />
          </div>
        </div>

        <div className="order-1 md:order-2">
          <img
            src={imagemOferta}
            alt=""
            className="mx-auto w-full max-w-sm rounded-2xl object-cover"
          />
        </div>
      </div>

      <div className="mx-auto mt-16 max-w-3xl">
        <h2 className="text-center text-2xl sm:text-3xl">Perguntas Frequentes</h2>

        <div className="mt-8 divide-y divide-[var(--landing-line)] rounded-2xl border border-[var(--landing-line)] bg-[var(--landing-bg)]">
          {FAQ.map((item) => (
            <details key={item.pergunta} className="group p-5">
              <summary className="cursor-pointer list-none font-medium marker:content-none">
                <span className="flex items-center justify-between gap-4">
                  {item.pergunta}
                  <span className="text-[var(--landing-primary)] transition group-open:rotate-45">+</span>
                </span>
              </summary>
              <p className="mt-3 text-sm text-[var(--landing-ink)]/90">{item.resposta}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  )
}

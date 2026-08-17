const DEPOIMENTOS = [
  {
    texto:
      'Iniciei na área hospitalar com bastante receio, com medo de errar, mas a nutricionista Silvia sempre me ensinou com paciência e me passou segurança com a sua experiência de anos. Eu aprendi muito com ela! Recomendo a mentoria!',
    nome: 'Tainara Gabriele Neves',
  },
  {
    texto:
      'Oii Silvia! Participar dessa mentoria foi uma experiência extremamente valiosa. Foi um momento de muito aprendizado, troca e clareza, que me ajudou a enxergar pontos que antes passavam despercebidos. Ao longo da mentoria, consegui organizar melhor minhas ideias, alinhar estratégias e, principalmente, ter mais segurança nas decisões que preciso tomar no dia a dia. Não foi só conteúdo técnico, foi direcionamento, visão e crescimento profissional. Muito obrigada por todo o conhecimento passado! 🥰',
    nome: 'Tiago da Silva Martins',
  },
  {
    texto:
      'Oii Silvia! Participar dessa mentoria foi uma experiência extremamente valiosa. Foi um momento de muito aprendizado, troca e clareza, que me ajudou a enxergar pontos que antes passavam despercebidos. Ao longo da mentoria, consegui organizar melhor minhas ideias, alinhar estratégias e, principalmente, ter mais segurança nas decisões que preciso tomar no dia a dia. Não foi só conteúdo técnico, foi. Quando realizei minha primeira consulta, indicada e encaminhada por minha cardiologista, percebi que a nutricionista Silvia ia fazer a diferença na minha vida. Não é uma profissional com métodos prontos e dietas impossíveis de seguir, ela ensina a melhor maneira de se alimentar com as preferências e restrições alimentares de cada pessoa, explica, recalcula, tira dúvidas, orienta, às vezes até é psicóloga. Minha experiência com essa profissional está sendo incrível, e é como eu disse a ela no primeiro atendimento: devia ter lhe procurado a muitos anos atrás. Confio e indico o trabalho dela.',
    nome: 'Simone Geller',
  },
  {
    texto:
      'Profissional com muita experiência e excelente conhecimento na área, com atuação em nutrição hospitalar e obesidade. É dedicada, competente e transmite segurança no que faz. Possui diversas receitas e cardápios, utilizando uma abordagem prática e de fácil compreensão. Apresenta ótimos resultados e oferece um atendimento de excelente qualidade.',
    nome: 'Janaína Maria',
  },
  {
    texto:
      'Excelente profissional! Sua inteligência, determinação, dedicação, com certeza fazem a diferença no teu trabalho! Te admiro e te agradeço pelo que tu entrega a cada paciente, cada uma de nossas crianças, e também em nossa instituição, nosso querido HSSM. Obrigada, Silvia!',
    nome: 'Luciane Winkelmann',
  },
]

export function Depoimentos() {
  return (
    <section className="bg-[var(--landing-bg)] py-10 sm:py-16">
      <div className="mx-auto max-w-5xl px-4 sm:px-6">
        <h2 className="text-center text-2xl sm:text-3xl">
          Veja o que nutricionistas que{' '}
          <span className="text-[var(--landing-primary)]">já passaram pela Sessão Estratégica</span> têm
          a dizer:
        </h2>
      </div>

      {/* A lista aparece duas vezes: é o que faz o loop do trilho ser
          contínuo, sem salto visível ao reiniciar. */}
      <div className="mt-8 overflow-hidden">
        <div className="landing-marquee items-start">
          {[...DEPOIMENTOS, ...DEPOIMENTOS].map((depoimento, indice) => (
            <figure
              key={indice}
              className="mr-4 w-64 shrink-0 rounded-2xl bg-[var(--landing-primary)] p-6 text-white sm:w-72"
              aria-hidden={indice >= DEPOIMENTOS.length}
            >
              <blockquote className="text-sm">“{depoimento.texto}”</blockquote>
              <figcaption className="mt-4 text-sm font-semibold">{depoimento.nome}</figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  )
}

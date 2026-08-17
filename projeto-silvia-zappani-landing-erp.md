# Projeto: ERP + Landing Page — Silvia Zappani (Mentoria)

## Contexto

Uma cliente (mentora/nutricionista hospitalar, Silvia Zappani) contratou o desenvolvimento de uma ferramenta (espécie de ERP) para vender dentro da mentoria dela. Durante a análise do projeto, foi identificado um bug na landing page atual dela (feita em WordPress), e surgiu a oportunidade de entregar a landing page refeita junto com a ferramenta, como um pacote único.

## Objetivo

Construir um único projeto em React que sirva dois propósitos através de rotas diferentes:

1. **Landing page de vendas/captura** (recriação da página atual em WordPress, corrigindo o bug encontrado e possivelmente melhorando o que fizer sentido).
2. **ERP** — a ferramenta principal que está sendo desenvolvida para a mentoria.

## Stack técnica

- **Frontend:** React (a landing page e o ERP compartilham o mesmo projeto/build, com rotas diferentes apontando para cada parte).
- **Backend:** Java com Spring Boot (usado pelo ERP; a landing page é majoritariamente estática/client-side, mas pode consumir endpoints do backend se necessário, ex: formulário de contato).
- **Design de referência:** existe um arquivo Figma disponível (mais preciso que só inspecionar a página ao vivo — usar como fonte de verdade para cores, espaçamentos, tipografia e componentes quando disponível).

## Decisão de arquitetura de rotas

Uma única aplicação React, com rotas separadas, por exemplo:

- `/` (ou `/silvia-rita-zappani` ou domínio próprio) → landing page de captura.
- `/app` ou `/erp` → aplicação do ERP (provavelmente atrás de autenticação).

**Pontos ainda em aberto** (decidir durante a implementação, não bloqueiam o início):

- SEO/pré-renderização da landing page: como ela precisa ser indexada no Google e ter bom preview ao compartilhar link, avaliar se um SPA client-side puro é suficiente ou se vale investir em pré-renderização apenas dessa rota (meta tags dinâmicas, prerender, ou SSR leve tipo Vite SSR/Next.js só para essa página).
- Padrão visual: seguir 100% o Figma/identidade visual atual da cliente (landing com marca própria) vs. reaproveitar o design system que já está sendo construído para o ERP. Tendência inicial é manter a landing fiel à marca da cliente, já que é material de venda dela.

## Análise da landing page atual

**URL:** https://monetizeionlineequipe.online/silvia-rita-zappani/

**Plataforma:** WordPress + Elementor 4.2.2 (com Elementor Pro), tema Hello Elementor, cache via plugin LiteSpeed Cache, rastreamento via plugin PixelYourSite (Meta Pixel).

### Estrutura de seções (ordem de exibição)

1. **Hero** — título "Segurança clínica hospitalar na prática para nutricionistas", texto de apresentação da Sessão Estratégica gratuita com Silvia Zappani, vídeo (ver seção Vídeo abaixo), e botão CTA "Quero garantir minha vaga gratuita" (âncora interna para `#rolagem123`).
2. **Qualificação** — lista de 9 itens em formato checkbox ("Essa sessão é para você que..."), permitindo à visitante marcar quais se aplicam ao seu momento (recém-formada, insegura na prática, quer migrar de área, etc.). Fecha com "Se você marcou pelo menos 2 opções, a sessão foi feita para você."
3. **Sobre a palestrante** — biografia de Silvia Zappani: 16 anos de formada, 13+ anos de atuação hospitalar, coordenadora e Responsável Técnica em hospital credenciado pela ONA, criadora de POPs e protocolos. Inclui a história pessoal dela (começou insegura, sem manual, e construiu a carreira na prática).
4. **Estrutura da sessão / 4 pilares** — em grade, explica o que a pessoa ganha nos 30 minutos: diagnóstico da situação atual, pilares da segurança hospitalar real, o que precisa desenvolver, e qual o próximo passo.
5. **Transformação prometida** — bloco de texto: "Você entra como uma nutricionista insegura... e sai se sentindo uma profissional mais segura, estratégica e confiante."
6. **7 benefícios-chave** — lista com ícones (check) detalhando resultados esperados (segurança para atuar, clareza na rotina, comunicação com equipe médica sem medo, redução de ansiedade, ser respeitada pela equipe multiprofissional, construir autoridade clínica, crescer no hospital).
7. **Depoimentos** — carrossel (Swiper) com 6 depoimentos em texto (sem foto/vídeo). **Atenção:** os depoimentos atuais mencionam contexto financeiro/contábil ("fluxo de caixa", "pró-labore", "imposto", "contador") — parecem ter vindo de outro template/nicho e não fazem sentido para uma página de nutrição hospitalar. Vale realinhar esse conteúdo com a cliente antes de reutilizar.
8. **Comparação faculdade vs. prática + oferta principal** — seção com âncora `#rolagem123` (destino do primeiro CTA), reforça "a faculdade te ensinou a teoria, mas o hospital exige mais", apresenta a "Sessão Estratégica" (30 min, ao vivo, personalizada) e botão CTA "Quero agendar sessão gratuita".
9. **FAQ** — acordeão (nested-accordion do Elementor) com 6 perguntas: duração da sessão, o que acontece durante, se é realmente gratuita, se precisa de experiência hospitalar, como funciona o agendamento, e se serve para quem já fez outros cursos.
10. **Footer** — crédito "Produzida por Monetizei Online" com link e logo em SVG.

### CTAs e formulário

- Botão 1: **"Quero garantir minha vaga gratuita"** → âncora interna `#rolagem123` (rola a página até a seção de oferta).
- Botão 2: **"Quero agendar sessão gratuita"** → link externo para formulário: `https://lacerta12.yayforms.link/Wq56xZ2` (Yayforms — não é WhatsApp nem checkout direto; o FAQ menciona que depois do formulário a equipe entra em contato em até 24h, com opção de agilizar por WhatsApp).

### Rastreamento

- Meta Pixel via plugin PixelYourSite, ID `1857026921901673`, disparando evento `PageView` no carregamento da página.

### Vídeo (ponto que motivou essa análise)

Encontrado **apenas 1 vídeo** no HTML da página (o usuário mencionou 2 — o segundo não foi localizado; ver "pendências" abaixo).

- **Localização:** logo no hero, entre o parágrafo de apresentação e o botão "Quero garantir minha vaga gratuita" — primeira coisa que a visitante vê.
- **Plataforma:** [Panda Video](https://pandavideo.com.br/) (player brasileiro voltado para VSL/funis de venda, não é YouTube/Vimeo/self-hosted).
- **ID do vídeo:** `ef45bada-f86c-46c4-af71-211680c041a6`
- **Domínio do player:** `player-vz-0767af7a-2a2.tv.pandavideo.com.br`
- **Scripts oficiais usados na página:**
  - `https://player.pandavideo.com.br/api.v2.js`
  - `https://player.pandavideo.com.br/player.external.js`
- **Comportamento configurado:** autoplay mudo (`autoplay: true`, `smartAutoplay: false`), sem branding do Panda (`pandaBranding: false`), sem barra de progresso/controles nativos (só `play-large`, botão grande de play central de 150px), com indicador de "mudo" que mostra o texto "Clique aqui / para ativar o som" sobreposto ao vídeo — técnica clássica de VSL para forçar interação.
- **Recurso avançado usado:** o player do Panda Video permite configurar CTAs/botões que aparecem em tempos específicos de reprodução (`dashboardButtons`, dependência de `showTime` por botão). Isso é um recurso pago da plataforma, não é só "um vídeo tocando" — importante saber disso ao planejar a reimplementação.

### Decisão sobre o vídeo (definida em conversa)

Manter simples: **reaproveitar a mesma hospedagem/embed do Panda Video** dentro do React, em vez de migrar para hospedagem própria. Isso preserva as métricas que a cliente já tem no painel do Panda Video e evita reconstruir a lógica de CTAs-por-tempo do zero.

**Sugestão de implementação:** criar um componente React (ex: `<PandaVideoPlayer videoId="ef45bada-f86c-46c4-af71-211680c041a6" />`) que, em um efeito (`useEffect`), carrega os scripts oficiais do Panda Video e injeta o player em uma div com o mesmo padrão de proporção usado hoje (`padding-top: 56.25%` para manter 16:9 responsivo), reproduzindo o mesmo comportamento (autoplay mudo, indicador de som, CTAs por tempo se necessário).

## Escopo sugerido para a implementação (Claude Code)

1. Configurar o roteamento (React Router ou equivalente) separando claramente `landing` e `erp` dentro do mesmo projeto/build.
2. Recriar a estrutura de seções da landing (lista acima) fielmente ao Figma fornecido, mantendo os textos e CTAs atuais (ajustando o que for combinado com a cliente).
3. Implementar o componente de vídeo reaproveitando o embed do Panda Video com o mesmo ID (ou o ID atualizado, se a cliente decidir trocar o vídeo).
4. Recriar o formulário/CTA de agendamento (avaliar se mantém o link externo do Yayforms ou se substitui por formulário próprio integrado ao backend Spring Boot).
5. Reimplementar o rastreamento do Meta Pixel (evento `PageView` no mínimo, replicando o pixel ID atual: `1857026921901673`).
6. Corrigir o bug identificado (a definir).
7. Revisar/realinhar o conteúdo dos depoimentos com a cliente antes de publicar.

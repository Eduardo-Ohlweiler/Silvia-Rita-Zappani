# Nutri Hospitalar de Sucesso — Guia para Agentes

Calculadora nutricional hospitalar multi-tenant (UTI adulto + pediatria).
Sistema leve. Nada de complexidade que não se pague.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 · Maven |
| Persistência | PostgreSQL 16 · Spring Data JPA / Hibernate 6 · Liquibase XML |
| Segurança | Spring Security · JWT (JJWT 0.12.x) · BCrypt · Bucket4j |
| Docs | SpringDoc OpenAPI |
| Frontend | React 19 · TypeScript · Vite · Tailwind v4 |
| Front (libs) | react-router-dom · axios · react-hook-form · zod · react-toastify |
| Chave primária | **UUID** em todas as tabelas |

---

## Estrutura

```
sistema silvia/
├── CLAUDE.md · docs/
├── nutri-hospitalar-api/          # com.nutri.hospitalar  ← fatia 1 pronta
│   ├── .env.example · run-dev.sh · README.md
│   └── src/main/java/com/nutri/hospitalar/
│       ├── config/                # SecurityConfig, JwtUtil, JwtFilter, SecurityUtils,
│       │                          # AuthDetails, RateLimitFilter, PageableUtils,
│       │                          # TransacaoConfig, DataSeeder, SwaggerConfig
│       ├── exceptions/            # GlobalExceptionHandler + hierarquia
│       ├── baseentity/            # BaseEntity, TenantEntity
│       ├── jobs/                  # ManutencaoJob
│       └── {modulo}/              # tenant, usuario, auth, loginlog, refreshtoken,
│           ├── controller/        # pessoa, contato, catalogo, clinica,
│           ├── dtos/              # pediatria, uti, ...
│           ├── entity/
│           ├── enums/
│           ├── mapper/
│           ├── repository/
│           └── service/
│                                  # clinica/ = fichas de anamnese:
│                                  #   ModeloFicha + CampoFicha (catálogo híbrido)
│                                  #   FichaAnamnese + RespostaFicha (com o RETRATO)
│                                  # clinica/escore/ = escalas pontuadas (docs/13)
│                                  #   EscalaNutricional · MnaEscala · Nrs2002Escala
│                                  #   EscalaRegistry · EscoreCalculator · SomaPorGrupo
│                                  # uti/ = terapia nutricional adulto:
│                                  #   FormulaEnteral (composição SEMPRE por litro)
│                                  #   ProdutoNutricional (suplemento · módulo · insumo)
│                                  #   PercentilCb (referência, sem tenant)
│                                  # uti/calculo/ = 7 classes puras + UtiMatematica
│                                  #   cascata/ PesoDeTrabalho · Altura · MetaEnergetica
│                                  #            MetaProteica · VolumeDieta
└── nutri-hospitalar-web/          # fatia 2 pronta
    ├── public/                    # favicon, ícones, og-image
    └── src/
        ├── assets/brand/          # logo-full · logo-mark · logo-simbolo (currentColor)
        ├── assets/icons/          # SVG inline, módulo único
        ├── styles/theme.css       # tokens Tailwind v4 + temas claro/escuro
        ├── components/graficos/   # chrome · eixos · referencias (faixas com fonte)
        │                          # SerieNoTempo · MetaVersusOfertado · BarrasComZero
        ├── components/impressao/  # Folha · TiraIndicadores · Secao · LinhasDeValor
        │                          # TabelaDoc · DocumentoLista (doc 05 §8)
        ├── components/common/     # TPage TPanel TDataGrid TDataGridFooter
        │                          # TEntry TSelect TCombo TButton TBadge TModal TThemeToggle
        │                          # TTabs TResult TBotaoImprimir (doc 04 §7)
        │                          # TCheckBox TRadio (fatia 12)
        ├── components/layout/     # Layout · Sidebar · TenantSwitcher · TProtected
        ├── contexts/              # AuthContext · ThemeContext
        ├── hooks/                 # useAuth · useTheme · useDebounce
        ├── services/              # api.ts (interceptor + refresh) + um por módulo
        ├── pages/                 # auth/Login · Dashboard · usuario · tenant
        │                          # pessoa · loginlog · perfil · pediatria
        │                          #   (catálogo, avaliação, acompanhamento
        │                          #    diário e os 3 painéis)
        │                          # uti/ (catálogos, cálculo, avaliação,
        │                          #      acompanhamento e os 3 painéis)
        │                          # clinica/ (fichas de anamnese e modelos)
        ├── components/pessoa/     # PessoaRapidaModal (cadastro rápido)
        ├── types/ utils/
        └── routes/AppRoutes.tsx
```

**Sem Docker.** PostgreSQL local, variáveis em `.env`, `./run-dev.sh` para subir.
Testes contra o banco `nutridb_test` — nada de H2 nem Testcontainers.

---

## Onde estamos

| Fatia | Situação |
|---|---|
| **1 — Base, acesso e auditoria** | ✅ pronta |
| **2 — Casca do front e área administrativa** | ✅ pronta |
| **3 — Pessoas** | ✅ pronta · porte do eroERP, com endereços (IBGE) e vínculos |
| **5 — Pediatria** | ✅ pronta · cálculo no servidor, telas em abas, painéis com paleta validada |
| **6 — Terapia Nutricional (UTI adulto)** | ✅ pronta · especificação [docs/10](docs/10-calculos-uti-adulto.md) · catálogos · cálculo e calculadora · ferramentas clínicas · avaliação · acompanhamento diário · **3 painéis** · **impressão** |
| **7 — Simetria pediatria ↔ UTI** | ✅ pronta · guarda no cadastro de fórmula láctea · 3 documentos de impressão · exportação nas 10 listas · `CatalogoPediatriaTest` |
| **8 — Módulo proteico** | ✅ pronta · migration 027 · o cálculo existia desde a fatia 2 e nenhuma tela o alcançava |
| **9 — Varredura de conformidade** | ✅ pronta · `docs/09` e `docs/10 §13` item a item, calculador órfão e entrada órfã. Achou 2 defeitos de fala (corrigidos) e 7 lacunas de teste (fechadas) |
| **10 — Acompanhamento diário pediátrico** | ✅ pronta · especificação [docs/11](docs/11-acompanhamento-pediatrico.md) · migration 028 · **3º painel** · impressão · exportação. **Nenhuma constante clínica nova**: a régua já estava no sistema |
| **10.1 — Validação da fatia 10** | ✅ o painel de acompanhamento ganhou a impressão que só a UTI tinha, e o `DELETE` do dia ganhou teste (feliz e cross-tenant). A varredura no navegador achou uma legenda que não fechava com o número ao lado |
| **11 — Tela inicial** | ✅ pronta · migration 029 · a lista de trabalho do dia, e o **encerramento do acompanhamento**, que é a saída dela |
| **11.1 — Verificação de ponta a ponta** | ✅ pronta · o que foi **preenchido** na tela × o que a tela **enviou** × o que o servidor **persistiu** × o que **reabriu**, em todas as telas. Achou 3 defeitos reais (folha do dia com o prescrito errado ao lado da adesão, eixo de gráfico em notação inglesa, exportação com teto de 100 em vez de 2.000) — nenhum deles visível a `build`, `lint` ou aos 368 testes |
| **11.2 — Varredura de QA no navegador** | ✅ pronta · achou 3 defeitos que `build`, `lint` e os testes deixam passar: a classificação impressa duas vezes na tela do dia pediátrico, `IMCClassificação` colado em quatro tabelas sem padding, e o prescrito exibido ≠ o prescrito da conta em **sete** pontos — a cauda da fatia 11.1, que consertou só dois deles |
| **12 — Clínica: fichas de anamnese** | ✅ pronta · especificação [docs/12](docs/12-fichas-de-anamnese.md) · migration 030 · primeira tela do menu **Clínica** do eroERP · modelo de ficha como catálogo híbrido, com **clonar** · **o retrato da pergunta** em cada resposta · 3 modelos de nutrição semeados · impressão e exportação · dois grupos novos de menu |
| **12.1 — O 500 da calculadora** | ✅ pronta · incidente em produção, 06/09/2026. Origem de peso escolhida no seletor não checava o sinal, e medida deslocada pela máscara faz Chumlea 1988 devolver peso **negativo** → `IllegalArgumentException` → 500. Corrigido em três camadas: a guarda que faltava, pisos de plausibilidade com frase que ensina a vírgula, e a recusa **inline** em vez de toast numa tela que recalcula sozinha |
| **13 — Escalas nutricionais pontuadas** | ✅ pronta · especificação [docs/13](docs/13-escalas-nutricionais.md) · migration 031 · **MNA®** e **NRS-2002** como modelo do sistema · o ponto é **dado** (`campo_ficha.pontos`), a régua é **código com fonte citada** (`clinica/escore/`) · escore ao vivo, congelado na gravação, impresso e na listagem |
| 4 — Atendimento | **a redefinir**, não a construir — ver abaixo |
| 11 — `audit_log` | pendente · adiada para quando o sistema estiver em produção |

**475 testes** no total, contra o banco `nutridb_test`.

### O que falta, e por quê

**Pediatria e UTI estão completos contra as suas especificações** — e a
varredura de 31/08/2026 provou que "completo" não é "sem defeito": ela achou
**dois defeitos reais de fala**, um em cada módulo, ambos corrigidos e travados
em teste (ver as armadilhas novas abaixo). Vale repetir as quatro antes de dar
qualquer módulo por fechado: (1) `docs/09` e (2) `docs/10 §13` item a item; (3)
**calculador órfão** — método público de `calculo/` que ninguém chama, que foi
como o módulo proteico apareceu depois de duas auditorias; (4) **entrada órfã** —
campo que a API aceita e nenhuma tela oferece.

O que resta é isto, e **nada disso é buraco nesses dois módulos**:

| Pendência | Natureza | Por que não agora |
|---|---|---|
| **`audit_log`** | fatia própria | adiada **para quando o sistema estiver em produção** (decisão de 31/08/2026). A especificação já existe, em [docs/06 §A09](docs/06-seguranca-owasp.md) — inclusive o record `AuditEvent` e a retenção de 12 meses —, e o molde estrutural é o módulo `loginlog`, com o expurgo no `ManutencaoJob`. É a tabela que responde *"quem apagou?"*: os três `delete` do sistema são **físicos**, e o `created_by` some com a linha. Invisível no dia a dia; importa no dia em que alguém pergunta |
| **Recuperação de senha** | fatia própria | adiada junto do `audit_log`, e depende de escolher o serviço de e-mail — não há `spring-boot-starter-mail` no `pom.xml` |
| **`agua_livre_perc` nulo nas 53 fórmulas** · **potássio do `Peptimax pó`** | digitação de cadastro | o mecanismo está pronto e a tela tem o campo. O dado vem do rótulo, e inventá-lo por aproximação é a inferência que `docs/10` recusa |
| **Fonte primária do Chumlea 1988 de 8 ramos** | bibliografia | os números conferem contra a planilha; falta identificar a publicação |
| **Ferramentas clínicas pediátricas** (Holliday-Segar, superfície corporal, TIG) · **percentil de CB pediátrico** (Frisancho) | fonte a levantar | **nenhum está na `Pediatria.xlsx`**, que tem 3 abas contra as 15 da UTI. Decisão de 31/08/2026: **o que a planilha entrega é mantido; o que ela não entrega vem de literatura, com procedência citada** — o que a UTI já faz nas nove faixas de `graficos/referencias.ts`. Vira `docs/11` antes de virar código |
| **Fatia 4 — Atendimento** | redefinir | as duas avaliações já são o contêiner que ela descrevia — detalhe abaixo |

**Bloqueio resolvido.** As fatias de cálculo dependiam de uma especificação
numérica das fórmulas — com célula de origem, referência bibliográfica, unidade e
caso de teste. Nenhuma fórmula nutricional deve ser implementada por inferência: o
sistema prescreve dieta para paciente de UTI.

- ✅ **Pediatria**: `Pediatria.xlsx` → [docs/09](docs/09-calculos-pediatria.md)
- ✅ **UTI adulto**: `Facilita Nutri na UTI - com SA_atualiza (1).xlsx` (a de
  1,1 MB é a canônica) → [docs/10](docs/10-calculos-uti-adulto.md)

**As duas especificações listam os defeitos da planilha de origem, e cada defeito
tem um teste que garante que NÃO o replicamos.** São 22 na planilha da UTI, seis
deles mudando dose ao paciente. Ao implementar, o documento vence a planilha, e a
planilha vence o eroERP.

Duas coisas clinicamente relevantes só existem nas **imagens** da planilha da UTI,
fora de qualquer célula, e por isso nunca foram implementadas no eroERP: o
**ajuste da circunferência do braço e da panturrilha pelo IMC** antes de comparar
com o ponto de corte. Rastreadas até a literatura primária em
[docs/10 §2.9](docs/10-calculos-uti-adulto.md).

**A fatia 4 (Atendimento) foi ultrapassada pelos fatos.** O `docs/00 §4` a
descreve como *"a consulta numa data, contêiner do que vem a seguir"* — mas
`AvaliacaoUti` e `AvaliacaoPediatrica` já têm `paciente`, `profissional` e data,
e **já são esse contêiner**. Criar a tabela hoje significa migration nas duas,
refatorar dois módulos que funcionam e reabrir prontuário, sem nada novo na
tela. Ela só volta a fazer sentido diante de requisito que hoje não existe: mais
de um tipo de avaliação no mesmo encontro, ou faturamento por atendimento.

## Modelo de acesso

Nível único por usuário, enum em `usuario.role`: **`SUPERADMIN`** (administra o
sistema, único que atravessa tenants) · **`ADMIN`** (administra o próprio tenant) ·
**`USER`** (opera dentro do tenant). Sem tabela de role, sem grupo de acesso.

A área administrativa — `/tenants`, `/usuarios`, `/login-logs` — é exclusiva do
`SUPERADMIN`. `ADMIN` e `USER` acessam os módulos de negócio e `/usuarios/perfil`.

**Sem autocadastro.** A superfície pública são duas rotas: `/auth/login` e
`/auth/refresh`. Usuário só nasce por `POST /usuarios`, do superadmin — e é lá que
um cliente novo ganha o seu tenant, quando nenhum é indicado.

**A licença é do tenant, não do usuário.** `tenant.periodo_acesso`
(Indeterminado · 1 · 2 · 3 · 6 meses · 1 · 2 anos) e `tenant.acesso_expira_em`
são escolhidos ao abrir o cliente, no mesmo formulário. Uma rotina diária
desliga quem venceu, e o login barra o vencido sem esperar por ela. Renovar é
`PATCH /tenants/{id}/acesso`, sempre recontando a partir de hoje — e é a única
porta de volta: reativar um vencido devolve 409. O tenant do superadmin nunca
expira. Detalhe em [docs/02 §1.1](docs/02-modulo-usuarios.md).

---

## As 6 regras que não podem ser esquecidas

**1. Isolamento por tenant é a regra máxima.**
Toda entidade de negócio tem `tenant_id NOT NULL`. Toda consulta filtra por
`securityUtils.getTenantIdLogado()`. Proibido `findById`, `findAll`, `deleteById`
em entidade multi-tenant — use `findByIdAndTenantId`, `findAllByTenantId`.
Nunca aceitar `tenantId`, `usuarioId` ou `role` vindos do frontend — a única
exceção são as consultas `/global` do superadmin, onde `tenantId` é *filtro
opcional* e a role substitui o `WHERE`.
Detalhes: [docs/01-multitenant.md](docs/01-multitenant.md)

**2. Listagem com filtro opcional = native query com `CAST` explícito.**
O JPQL do Hibernate 6 não consegue tipar um parâmetro nulo solto em
`:param IS NULL`, e o Postgres responde *"não foi possível determinar o tipo de
dados do parâmetro $N"*. Native query resolve e ainda libera `unaccent`.

```sql
WHERE u.tenant_id = CAST(:tenantId AS uuid)
  AND (CAST(:nome  AS text)    IS NULL OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
  AND (CAST(:ativo AS boolean) IS NULL OR u.ativo = CAST(:ativo AS boolean))
  AND (CAST(:de    AS timestamptz) IS NULL OR l.data_login >= CAST(:de AS timestamptz))
ORDER BY u.nome
```

Toda native paginada precisa de `countQuery`, e o `Sort` do cliente é descartado
com `PageableUtils.semOrdenacao(pageable)` — a ordenação é fixa no SQL.

**3. DTO sempre.** Controller nunca recebe nem retorna Entity. Request e Response
são `record` com Bean Validation. Conversão só via Mapper.

**4. Liquibase sempre.** Nenhuma alteração estrutural fora de
`db/changelog/changes/NNN-descricao.xml`. Nunca editar changeSet já executado.

**5. Nada de dado clínico, senha ou token em log.** Peso, diagnóstico, prescrição,
evolução — nada disso vai para log. São dados de saúde sob a LGPD.

**6. Toda tela é responsiva. Sem exceção.**
Desktop, tablet e celular. Não é ajuste no fim — é como a tela nasce. Nenhuma
tela é dada por pronta sem funcionar em 360px de largura, sem rolagem
horizontal e sem conteúdo cortado. Grid vira cartão no mobile, sidebar vira
*drawer*, coluna secundária some antes da principal. O sistema é usado à beira
do leito, muitas vezes no celular.

---

## Armadilhas já pagas

Cada uma destas custou depuração. Não repita.

**Ler pode ser global; escrever é dentro do tenant.**
`GET /usuarios/global` e `GET /login-logs/global` são os únicos pontos que
atravessam tenants, exclusivos de `SUPERADMIN` e com nome explícito. Toda
escrita (`POST`, `PUT`, `PATCH`) filtra pelo tenant efetivo e devolve 404 fora
dele. No front, abrir um registro de outro cliente **entra naquele tenant
antes** — é o que o `switch-tenant` faz.

**Nunca recarregar a página ao trocar de tenant.**
O access token vive em memória. Um `navigate(0)` ou `location.assign` o
descarta, o boot restaura a sessão pelo refresh token — que pertence ao tenant
de **origem** — e o usuário volta de onde saiu. O `AuthContext` já atualizou a
sessão; as listas refazem a consulta porque dependem de `sessao.tenantId`.

**Uma trava só de renovação de token, no aplicativo inteiro.**
O refresh é de uso único com rotação: reapresentar um token consumido faz o
backend revogar a cadeia, tratando como roubo. O `StrictMode` monta efeitos
duas vezes em desenvolvimento — sem a trava compartilhada de
`renovarSessao()`, o boot derruba a própria sessão. O sintoma era cair no login
sem motivo aparente.

**Escrita que precisa sobreviver a rollback vai em transação independente.**
Contador de tentativas de login, revogação de refresh reutilizado e log de
falha acontecem em caminhos que terminam em exceção. Use
`@Transactional(REQUIRES_NEW)` entre beans, ou o `TransactionTemplate`
`transacaoIndependente` quando a chamada é interna à classe — self-invocation
não passa pelo proxy e a anotação é ignorada em silêncio.

**`brand-*` é fixo; `surface`/`txt`/`line`/`primary` viram com o tema.**
`hover:bg-brand-50` fica branco no tema escuro. Se o elemento é interface, use
token de tema. Botão cheio usa `text-txt-inverse`, nunca `text-white` — no
escuro a primária é clara e o branco reprova o contraste (3,28:1).

**Ponto digitado é decimal, não milhar.** `paraNumero` (`utils/format.ts`) é a
única porta por onde texto vira número no front. Ela removia *todo* ponto para
tratar `1.234,56`, e com isso `72.5` virava **725** e `0.75` virava **75** — em
silêncio, no campo de peso que prescreve dieta, com o teclado do celular
mandando ponto. Hoje a regra é pelo formato, com preferência do decimal: só é
milhar quando há vírgula no número, quando há mais de um ponto, ou no padrão
`1.500` (1 a 3 dígitos, três depois, sem começar em zero).

**Campo numérico usa máscara de centavos, e vazio tem de continuar vazio.**
`TEntry` recebe `mascara="decimal" | "inteiro" | "decimalComSinal"`: os dígitos
entram pela direita e a vírgula se posiciona sozinha (`7250` → `72,50`), como no
eroERP. Duas diferenças deliberadas em relação a lá, e as duas são defeito de
lá: **apagar tudo devolve string vazia**, não `0,00` — senão um campo opcional
que o usuário limpou viraria zero e o cálculo devolveria número em vez de dizer
o que falta; e **colar passa por `paraNumero`** antes de formatar, senão colar
`70` daria `0,70`. Ao repovoar registro salvo, use `textoDaMascara(valor, casas)`
com as mesmas casas do campo: `String(72.5)` daria `"72,5"`, e a primeira tecla
releria isso como os dígitos `725`.

**A compilação incremental do Maven mente quando você muda uma assinatura.**
`./run-dev.sh compile` respondeu *"Nothing to compile — all classes are up to
date"* e **BUILD SUCCESS** com o projeto quebrado: uma assinatura de mapper
mudou, o chamador em outro pacote não foi recompilado, e os testes rodaram
contra o `.class` velho. O sintoma foi 500 em três testes de painel, com
`NoSuchMethodError` escondido atrás de *"Erro inesperado"* — e nenhuma linha
apontando para a mudança real. Mudou assinatura pública, rode
`./run-dev.sh clean compile`. `touch` no arquivo não basta.

**Motivo de ausência é função das entradas, e a entrada está gravada.**
A avaliação salva devolvia todo motivo nulo — "eles explicam o instante da
digitação", dizia o javadoc — e uma criança de 37 meses reabria com VET,
proteína e as duas adequações em branco, **sem uma palavra**. Era o traço mudo
que o sistema inteiro existe para não repetir; a UTI tinha o mesmo defeito, em
nove campos. Hoje `findById` refaz o cálculo sobre as **entradas gravadas** e
aproveita dele **só texto**: todo número continua vindo das colunas. Não é
recalcular prontuário — é lembrar por que uma coluna está vazia. A UTI usa o
**retrato** da fórmula, não o catálogo, senão o motivo explicaria uma avaliação
que não é aquela.

**Motivo de tabela derivada não cabe no motivo do bloco.**
`TABELA_NAO_GRAVADA` ocupava o `motivo` de `Dieta` e `Hidratacao` na avaliação
salva — mas esse campo também é o "por que este bloco não saiu", e a tela o
pendura em **Volume total** e **Necessidade hídrica**. Resultado: a frase
"tabela derivada, não faz parte do registro" aparecia debaixo de números que
existiam. Hoje `motivoProgressao` e `motivoDistribuicao` são campos próprios, e
`TResult` só mostra motivo quando o valor está mesmo vazio.

**Imprimir o formulário devolve formulário.**
A primeira impressão era o DOM da tela com a interface escondida — e saía uma
folha de campos de entrada, rótulos de digitação e uma aba só, porque as outras
três estavam desmontadas. Quem lê no papel quer **demonstrativo**: identificação,
tira de indicadores, seções com o número já calculado e a procedência ao lado.
Hoje cada tela imprimível monta uma `Folha` e a passa à `TPage` pela prop
`documento`; a `TPage` marca o conteúdo de tela como `.nao-imprime`. O desenho
veio do `geradorPdf.ts` do eroERP; a ferramenta, não — jsPDF seria uma segunda
montagem dos mesmos números, e a do papel envelheceria calada.

**`new Date('2026-08-29')` é meia-noite UTC, e no Brasil isso é ontem.**
Data sem hora é **dia do calendário**, não instante — mas o JavaScript a lê como
UTC, e o `Intl` formata no fuso local. Em qualquer fuso a oeste de Greenwich o
resultado volta um dia: a avaliação de 07/08/2027 saía impressa como 06/08/2027,
em prontuário, e o formulário mostrava a data certa porque `<input type="date">`
usa a string ISO crua. Foram 60 chamadas em 16 arquivos — toda lista, todo
painel e todo documento impresso. O conserto é montar com `T00:00:00`, que o
JavaScript lê como meia-noite **local**, e `utils/idade.ts` já fazia assim: o
idioma certo existia no projeto e o `formatarData` não o usava. Timestamp
completo (`createdAt`) **não** entra nessa regra — ele é instante real, e
formatá-lo no fuso do leitor é o correto.

**Gráfico com faixa de referência precisa do domínio, não só da faixa.**
Os nove exames do acompanhamento desenhavam `ReferenceArea` com a faixa certa e
`domain={['auto','auto']}` no eixo — e **nenhuma faixa aparecia**. O recharts
fecha a escala nos dados: um potássio que oscilou entre 4,0 e 4,4 rende um eixo
de 4,0 a 4,4, e a banda de 3,5 a 5,0 fica inteira fora do quadro. O gráfico sai
bonito e sem a única coisa que o justifica — a régua contra a qual o número é
lido. O domínio tem de conter os limites da referência, com folga, e ser
arredondado na precisão do valor: sem isso o eixo herda o lixo de ponto
flutuante (a PCR saiu com piso `0,999998`).

**Linha tracejada ligando um ponto só não desenha linha.**
As metas de peso do painel do paciente eram duas séries tracejadas. Com uma
avaliação — que é o caso comum de quem acabou de internar — saíam **dois
tracinhos soltos** no meio do quadro. Valor que não varia com o x é
`ReferenceLine` horizontal, não série: aí ele atravessa o gráfico e ganha
rótulo, e o peso passa a ser lido entre o ideal e o teto de IMC 25.

**Série temporal cobre a janela pedida, não os meses que têm dado.**
O gráfico mensal começava no mês da primeira avaliação. Com três avaliações no
mesmo mês, "último ano" saía com um ponto no meio do branco, sugerindo que não
havia mais nada a mostrar. Havia: onze meses de zero, que é informação.

**`break-inside: avoid` numa caixa mais alta que a página esvazia a página.**
As seções da folha impressa tinham `break-inside: avoid`, e a tabela de 18 dias
de acompanhamento é mais alta que um A4. Uma caixa que não cabe em página
nenhuma é empurrada inteira para a seguinte: saiu uma capa em branco e o
relatório começando na página 2, com três páginas onde cabiam duas. A promessa
de não quebrar só pode ser feita por quem é menor que a página — a linha da
tabela, a tira de indicadores. Para o título existe `break-after: avoid`, que
impede o órfão sem prometer nada sobre a altura do que vem depois. E o rodapé
precisa de `break-before: avoid`: 8 mm de respiro bastavam para ele sozinho
virar uma última folha com nada além dele.

**Exportar a página visível é a armadilha da listagem.**
Relatório e planilha de uma lista precisam cobrir **o filtro inteiro**, não os 20
da página aberta — e o usuário só descobriria a diferença ao conferir o total.
`TAcoesDeExportacao` refaz a consulta sem paginação, com teto de 2.000 linhas, e
**anuncia quantas ficaram de fora** quando corta. E o CSV escapa `=`, `+`, `-` e
`@` no início da célula: nome de paciente é texto digitado, e uma planilha que
executa fórmula na máquina de quem abre é injeção, não formatação.

**Faixa clínica calibrada pela norma pode reprovar o próprio catálogo.**
O cadastro de fórmula láctea não valida coerência nenhuma — a enteral fecha por
Atwater, mas a láctea só tem kcal e proteína, e não há o que fechar. A guarda
óbvia seria a faixa de fórmula infantil do Codex, **60 a 70 kcal/100 ml** — e ela
recusaria **três das dez fórmulas que a migration 016 semeia**: FORTINI (150),
INFATRINE (100,8) e NEOCATE ADV (100). O catálogo não é de fórmula de partida, é
de tudo que a criança recebe, e 1,5 kcal/ml é conduta. O que sobrevive é a
**razão proteína/energia**, que é invariante de escala e vale igual para os dois:
as dez caem entre 1,86 e 2,85 g/100 kcal, dentro do Codex. Regra geral: antes de
pinar um limite clínico, **rodar o limite contra os dados que o sistema já
distribui** — e travar isso em teste, para quem apertar a faixa depois ser
reprovado pelo próprio seed. Detalhe em [docs/09 §7.1](docs/09-calculos-pediatria.md).

**Exportação com escopo duplo repete o `if` da tela, ou mente calada.**
`UsuarioList` e `LoginLogList` alternam entre a consulta do tenant e a `/global`
do superadmin. O `carregarTudo` da exportação **tem de repetir esse mesmo
`if`** — sem ele, o superadmin exporta da lista global e recebe só o próprio
tenant, com contagem plausível, sem erro e sem aviso. O relatório sai parecendo
completo, e é o único caso em que a exportação erra sem que nada na tela mude.

**Retrato gravado explica o número; catálogo de hoje explica o cálculo de hoje.**
A avaliação pediátrica salva imprimia a composição **atual** da fórmula ao lado
de uma oferta calculada com a **antiga**: uma fórmula editada de 70 para
100 kcal fazia a folha dizer *"100 kcal por 100 ml"* junto de *"880 ml ·
616 kcal"* — conta que não fecha, num prontuário. Os números estavam certos; a
legenda é que era de outro dia. A causa: `CalculoPediatrico` espelha para fora a
fórmula que ele acha em `formulaLacteaService.select()`, que é o catálogo vivo —
correto para o cálculo em andamento, errado para a avaliação salva. Hoje o
formulário guarda o **retrato** (`formulaNome`, `formulaKcalPor100ml`,
`formulaProteinaPor100ml` vêm na resposta) e o prefere enquanto a fórmula
escolhida for a mesma; trocar a fórmula devolve a vez ao catálogo, porque aí o
cálculo é novo. A UTI nunca teve o defeito: o documento dela lê
`dieta.formulaNome`, que já vem do retrato do servidor.

**Fórmula *alterada* no catálogo engana mais que fórmula *removida*.**
A UTI avisava quando a fórmula sai do catálogo (`formulaRemovida`), e ninguém
tratava o caso de ela ser **editada** — que é pior: o combo continua mostrando o
produto certo, com a composição de hoje, ao lado de um resultado calculado com a
de ontem, e nada na tela denuncia. A avaliação pediátrica hoje compara retrato
com catálogo e mostra a faixa dizendo qual das duas explica os números.

**Calculador escrito e testado não é funcionalidade entregue.**
`DietaEnteralCalculator.moduloProteico` existia desde a fatia 2 — com javadoc, a
prova aritmética do defeito 10 da planilha e gabarito em teste — e **nenhuma
linha do sistema o chamava**. O endpoint que o alimentaria existia, o wrapper no
front existia com o comentário *"alimenta a sugestão de módulo"*, os três
produtos estavam no catálogo, e a tela dizia *"Proteína ainda em falta: 45 g"*
sem oferecer nada. Passou por duas auditorias sem aparecer, porque não havia
`TODO` nem teste vermelho: o método estava **verde e órfão**. Achar isto exigiu
perguntar *quem chama* cada calculador público, não *o que está marcado como
pendente*. Vale como varredura periódica: `grep` do nome de cada método público
de `calculo/`, e ver se algum só aparece na própria declaração e no teste.

**"—" é uma string, e string não é ausência.**
`formatarNumero(null)` devolve `'—'`. A calculadora de UTI passava
`formatarNumero(...)` direto em 53 pontos, e o `TResult` — que só testava vazio
contra `null`, `undefined` e `''` — tratava aquele traço como **valor
presente**. Resultado: **26 traços mudos na tela e zero motivos escritos**,
enquanto o servidor calculava cada frase fielmente. Três telas de UTI, 23
motivos, calados desde sempre. A pediatria escapou por acaso — lá as chamadas
são guardadas com `x != null ? formatarNumero(x) : undefined`.
O conserto foi no `TResult`, não nos 53 pontos: o traço virou a constante
`AUSENTE` em `utils/format.ts`, exportada porque **dois lugares precisam
concordar sobre ela** — quem a escreve e quem a reconhece. Ponto novo não regride.
Lição geral: **sentinela de ausência que também é valor de exibição precisa ser
constante compartilhada**, nunca literal repetido; e nenhuma tela está verificada
enquanto ninguém a abriu vazia para ver se ela fala.

**Simetria entre módulos é de propósito, não de colunas.**
A fatia 10 quase virou outra coisa. "Espelhar o `RegistroDiarioUti` na
pediatria" parece decisão de simetria, mas as 29 colunas de lá são gasometria,
FiO₂, suporte ventilatório, lactato e PCR — elas existem porque a `AvaliacaoUti`
tem fase da terapia crítica, terapia renal e noradrenalina. A
`AvaliacaoPediatrica` **não tem um único campo de terapia intensiva**. Copiar as
colunas teria transformado a pediatria em módulo de UTI pediátrica de lado, e
exigido levantar toda faixa de referência pediátrica por idade (AAP 2017 para
pressão, PALS para hipotensão, laboratório por faixa etária) — fatia própria,
com fonte própria. O que se espelha é **o que o módulo vizinho resolve**
(evolução no tempo, o 3º painel), não a lista de campos com que ele resolve.
Antes de copiar uma tabela de um módulo para outro, ler as colunas da *avaliação*
de cada um: elas dizem de que módulo se está falando.

**Na pediatria a idade anda, e isso muda o desenho.**
Na UTI a idade é entrada da avaliação e não muda entre os dias. Na pediatria ela
é o **eixo X das curvas**, e 30 dias de internação de um lactente atravessam uma
linha inteira da tabela da OMS. Por isso o dia de acompanhamento **calcula a
própria idade** da data de nascimento (`docs/11 §3`) em vez de copiá-la da
avaliação — idade copiada envelheceria calada — e **classifica com o peso do
dia** em vez de repetir a classificação gravada. O teste que trava isso é o de
uma criança de 9,0 kg (P85 exato, adequado) que passa a 9,2 kg: se a
classificação sair "adequado", o registro está copiando, e a série mostraria uma
criança parada onde ela mudou de faixa. Consequência de infraestrutura: como
cada registro precisa da linha da OMS da *sua* idade, o painel faria uma consulta
por dia — o mapa por sexo (`linhasPorIdade`) troca N consultas por, no máximo,
duas.

**Composição por litro e por 100 ml são a mesma conta com fator 10 de diferença.**
`AcompanhamentoCalculator` da UTI divide por **1000**, porque o catálogo enteral
é declarado por litro (defeito 1 de `docs/10`). A fórmula láctea é declarada por
**100 ml** (`docs/09 §7`). As quatro contas do acompanhamento são idênticas nos
dois módulos — mas **nenhuma é literalmente reusada**: não há um só `import` de
`uti` dentro de `pediatria`, e a precedência do prescrito está escrita duas
vezes, de propósito. E
`caloriasRecebidas` e `proteinaRecebida` **não podem ser**: reusar a função da
UTI daria dez vezes o valor, num número que vira adequação calórica na tela de
quem prescreve. Reuso entre módulos exige conferir a **unidade declarada do
catálogo**, não só a forma da fórmula.

**Teste que depende do dia do mês quebra sozinho, e ensina a ignorar vermelho.**
`UtiDashboardTest.adesaoMensalAusenteNaoEZero` afirmava algo sobre o **último mês
da série** e criava o dia em `hoje.minusDays(2)`. Passava 28 dias por mês e
quebrava nos dois primeiros, quando a subtração atravessa a virada — apareceu em
01/09, sem nada no código ter mudado. Ao afirmar sobre um período (mês corrente,
semana corrente), ancore a data **dentro** dele; subtrair dias não garante isso.

**Ramo `else if` sem `else` é traço mudo esperando acontecer.**
A varredura de `docs/09` achou o mesmo defeito das duas armadilhas acima num
terceiro lugar, e a causa é sintática: `if (a && b) {...} else if (!b) {motivo}`
**não tem ramo para "b veio, a não"**. No `CalculoPediatricoCalculator` isso
acontecia três vezes — VET sem peso, IMC sem peso, estado nutricional sem
medida nenhuma — e o peso é opcional *de propósito*, para quem preenche aos
poucos. Regra: em bloco que produz valor-e-motivo, **todo caminho que não
produz valor tem de produzir motivo**, e a forma de garantir isso é o `else`
final, nunca uma cadeia de `else if`. O teste que trava é o mais simples que
existe — chamar o cálculo sem a entrada e exigir `motivo != null` —, e a prova
de que ele trava algo é reintroduzir o defeito e ver dois testes ficarem
vermelhos. **Um teste que passa antes e depois da correção não trava nada.**

**Legenda de fonte escrita à mão mente no dia em que a fonte passa a variar.**
A tela da UTI declarava a coluna de referência usada no braço —
`${antro?.populacaoReferenciaUsada}` — e, uma linha abaixo, cravava
`"CP ajustada pelo IMC · Gonzalez 2021"` na panturrilha. Em IMC < 18,5 com
população clínica a CP **não** recebe o `+4`: o número é o da orientação GLIM, e
o rótulo jurava Gonzalez — na única faixa em que as duas colunas divergem, que é
a do falso-negativo. O backend mandava o dado certo; a tela o descartava. Duas
lições: quando o servidor manda a procedência, **usá-la** (literal ao lado de
interpolação, no mesmo bloco, é o cheiro); e **a citação bibliográfica não mora
na tela** — ela vive no enum e no `docs/`, e a tela mostra qual coluna valeu.

**Motivo compartilhado por duas medidas cala a mais fraca e culpa a inocente.**
Havia um `motivoDeplecao` para braço e panturrilha, e ele só existia quando as
**duas** faltavam — panturrilha sozinha ficava muda. Pior: a frase era "informe
peso e altura", exibida a quem tinha peso e altura registrados e apenas não
mediu a panturrilha. **Culpar o dado que está lá é pior que não dizer nada.**
Hoje é um motivo por medida, e ele nomeia o que falta: a medida, o IMC ou o
sexo. Mesma família de `motivoProgressao`/`motivoDistribuicao`: **motivo é do
tamanho da coisa que faltou**, e agrupar dois é perder os dois.

**`toISOString()` devolve a data em UTC, e à noite isso é amanhã.**
Os quatro formulários abriam com `new Date().toISOString().slice(0, 10)` como
data padrão. No Brasil, das 21h à meia-noite, isso responde o **dia seguinte** —
e como toda data de avaliação e de acompanhamento é `@PastOrPresent`, o servidor
**recusava com 400**: o plantão noturno inteiro, num sistema hospitalar, sem que
nada na tela dissesse por quê. É a irmã da armadilha de `formatarData`: dia do
calendário não é instante, e tratá-lo como instante erra por um dia em todo fuso
a oeste de Greenwich — só que aqui o erro é para o **futuro**, e por isso ele não
some, ele bloqueia. Hoje existe `hojeIso()` em `utils/format.ts`, com
`toLocaleDateString('sv-SE')`. Só apareceu porque alguém abriu a tela às 22h.

**Lista de trabalho sem saída vira cemitério.**
A tela inicial lista quem ainda não tem registro do dia — e o paciente que
recebeu alta **nunca sairia**, porque o modelo não tem internação, leito nem
alta: nenhuma coluna, em nenhuma tabela. Em uma semana a lista estaria cheia de
gente que não está mais no leito, e quem usa aprenderia a ignorá-la, o que é
pior do que não ter lista. Daí o `encerrado_em` na avaliação (migration 029) —
na avaliação, e não numa tabela de internação, porque ela **já é** o contêiner do
atendimento, que foi a razão de a fatia 4 ter sido abandonada. Regra geral: toda
lista que cobra ação precisa de uma saída explícita, e a janela de inatividade é
rede para o que ninguém encerrou, nunca o critério principal.

**Derivado que a regra escolhe tem de ser publicado, não só a procedência.**
O servidor escolhia o denominador da adesão corretamente — o da avaliação vence
o digitado — e nunca dizia **qual número** escolheu: mandava só a procedência em
texto. Quem exibisse prescrito ao lado de percentual tinha de adivinhar a regra,
e **sete telas adivinharam errado**: a lista da UTI dizia `855 / 900 ml ·
45,97 %` (855 de 900 é 95 %), a da pediatria `650 / 700 ml · 90,28 %`, e o papel
do **período** da UTI imprimia a tabela inteira assim. Pior: a correção anterior
— a de logo abaixo — mandava *"procurar os irmãos"* e consertou só os dois
documentos do dia; o irmão do período passou batido, e a cópia do painel
pediátrico, elogiada por *"já imprimir na ordem certa"*, escolhia por `??`
enquanto o servidor escolhe por `positivo()` — certa por sorte, errada no dia em
que uma avaliação tivesse volume zero. A regra chegou a existir em **quatro
linguagens**: o calculator, o ternário do mapper, o `??` do front e um `COALESCE`
em SQL, este último divergindo de verdade (escolhe por nulo, some com o dia da
média). Hoje o número e a procedência saem de **uma decisão só**, num record, e o
percentual é literalmente `parte / valor dele` — não há caminho que os faça
divergir. Lição: **procurar os irmãos não escala; publicar o número escolhido,
sim.** Quando o servidor decide entre dois valores, ele expõe o que decidiu — do
contrário cada tela reimplementa a decisão, e a enésima erra. O teste que trava
é a invariante: `recebido / denominador_exposto` tem de reproduzir o percentual
exposto.

**A folha do dia imprimia o prescrito que ninguém usou na conta.**
`Volume prescrito em 24 h` saía do valor **digitado no dia**, e a `Adesão` ao
lado dele vinha medida contra o **volume da avaliação** — que é o que o
servidor usa (`percentualRecebido(recebido, volumeDaAvaliacao, volDigitado)`).
No papel: *"Prescrito 1.500 · Recebido 1.200 · Adesão 88,0 %"*, e 1.200 de
1.500 é 80 %. Um prontuário que se contradiz faz quem confere concluir que o
sistema errou. O `DocumentoPainelAcompanhamentoPediatrico` **já** imprimia na
ordem certa desde a fatia 7; os dois documentos **do dia** — UTI e pediatria —
ficaram com a ordem velha, e ninguém comparou um com o outro. Regra: quando o
servidor manda a procedência (`referenciaDoPercentual`, `referenciaDoRecebido`),
**o número exibido tem de ser o que essa procedência nomeia** — e a procedência
vai ao lado, para a conta poder ser refeita no papel. Corolário: ao consertar
um documento de impressão, procurar os irmãos dele.

**Rótulo de eixo sem formatador fala outra língua.**
O `<YAxis>` dos três gráficos compartilhados não tinha `tickFormatter`, então o
recharts imprimia o número com o `toString()` do JavaScript. No gráfico de
crescimento pediátrico o peso de uma criança de 14 meses saía **`9.263`** — que
em português se lê *nove mil duzentos e sessenta e três*. Os eixos da UTI
escaparam por acaso: as escalas de lá caem em inteiros, e inteiro não tem
separador decimal para errar, então o defeito ficou invisível até a pediatria
plotar quilos com três casas. É a irmã de `paraNumero`: **o ponto não é
separador de milhar aqui**. O `casas` já existia em todo gráfico — para o
tooltip; era só o eixo que não o usava. Hoje é `tickNumerico(casas)` em
`graficos/eixos.ts`, num lugar só, para o próximo gráfico não precisar lembrar.

**O teto de exportação era vinte vezes menor que o anunciado.**
`TAcoesDeExportacao` pedia `size=2000` numa requisição só, e o
`spring.data.web.pageable.max-page-size: 100` devolvia **100**, calado. A
planilha do log de acesso saía com 100 de 175. Ninguém perdia dado sem saber —
o aviso de corte dispara e diz quantos ficaram de fora, que é a rede que a
fatia 7 montou —, mas o número prometido no código e neste arquivo era outro, e
quem exportasse um mês de acompanhamento levaria *"estreite o período"* todo
dia sem entender por quê. Subir o limite do servidor consertaria pelo lado
errado: ele existe para nenhuma listagem devolver página gigante. Hoje a
exportação **pagina** — 100 por vez até o total ou até o teto —, e o
`carregar` das onze listas recebe o índice da página. Lição: **limite pedido
pelo cliente não é limite obtido**; quando os dois são declarados em camadas
diferentes, o menor vence em silêncio, e só contar as linhas do arquivo mostra.

**Componente de folha impressa carrega a forma do dado que o pariu.**
`LinhasDeValor` nasceu para prontuário de cálculo: rótulo à esquerda, valor numa
coluna de **22 % alinhada à direita**, classificação em cinza. É o desenho certo
para "72,5 kg". A ficha de anamnese reusou o componente — e ali **toda resposta é
prosa**: "Amendoim e frutos do mar" saiu quebrado em duas linhas, espremido à
direita, com um terço da folha vazio ao lado. Os números estavam certos e a folha
estava errada, e nada apontava para isso: `build`, `lint` e os 424 testes
passaram, porque a largura de uma coluna não tem assertiva. **Só apareceu no
PDF.** O conserto foi uma variante (`prosa`) no componente, e não uma tabela nova
na tela — mas a lição é anterior: ao reusar um componente de impressão, perguntar
**de que forma era o dado para o qual ele foi desenhado**. Reuso entre documentos
exige conferir a forma do conteúdo, como reuso entre módulos exige conferir a
unidade declarada do catálogo.

**Pergunta editável precisa gravar o retrato, e mais ainda que número.**
No eroERP a resposta de anamnese guarda só o `campo_id`, e a ficha é desenhada
lendo os campos vivos do template: trocar *"Consome álcool?"* por *"Consome
álcool diariamente?"* faz um "Sim" de um ano atrás passar a responder **outra
pergunta**, e desativar um campo some com a resposta na tela sem sumir do banco.
É a mesma armadilha do retrato da fórmula, com um agravante: uma resposta de
anamnese é **só texto**, e não há número ao lado para não fechar — se o rótulo
muda, **nada denuncia**. Por isso `resposta_ficha` guarda `secao`, `rotulo`,
`tipo`, `opcoes`, `ordem` e `obrigatorio` como colunas próprias, e a ficha salva
é desenhada, impressa e exportada a partir das suas próprias linhas. Duas
consequências boas: o `tipo` gravado é o que mantém o `valor` legível (é ele que
diz se `"true"` é sim/não e se `["Leite","Ovo"]` é opção múltipla), e **apagar um
modelo deixa de esvaziar prontuário** — o `modelo_id` é anulável com
`ON DELETE SET NULL`, e o `modelo_nome` sobrevive na ficha.

**Guarda com teto e sem piso deixa passar exatamente o erro mais provável.**
Todo campo antropométrico de `CalculoUtiRequestDto` tinha
`@DecimalMax` com uma frase boa — *"Altura acima de 260 cm não é plausível"* — e
`@DecimalMin("0.0")`, isto é, **só barrava negativo**. Só que a máscara de
centavos torna o erro *pequeno* o mais provável dos dois: quem digita `156` num
campo de duas casas obtém **1,56 cm**, e ninguém digita 26.000. Em produção,
06/09/2026, isso virou três *"Erro interno"* seguidos na calculadora — uma
profissional com braço de **0,32 cm** e o seletor em "Estimado — Chumlea 1988".
As equações de estimativa de peso são **lineares com uma constante grande
subtraída**, então medida deslocada não devolve número pequeno: devolve
**negativo**. Com 0,32 cm de braço, Chumlea 1988 dá peso negativo para toda
combinação de sexo e etnia e **qualquer** altura de joelho, inclusive a correta
de 53 cm. Lição: **piso e teto são a mesma guarda pela metade cada um**, e o
lado que a interface torna provável é o que precisa da frase melhor — a nossa
ensina a vírgula (*"para 156 cm, digite 15600"*).

**O ramo que o usuário escolhe não herda a guarda do ramo automático.**
`resolverPeso` tinha dois caminhos para o mesmo record. O automático testava
`positivo(estimado)`; o da origem **escolhida no seletor** testava só
`escolhido == null`. Todos os outros pontos que constroem `PesoDeTrabalho`
testam o sinal — só aquele não. O record recusa não-positivo com
`IllegalArgumentException`, que não tem handler e cai no catch-all: **HTTP 500**,
*"Erro interno. Referência: …"*, na tela de quem prescreve. E não havia teste:
`grep -rn origemPesoPreferida src/test` **não retornava nada** — o caminho
inteiro, quatro origens, estava descoberto. É primo do calculador órfão: lá o
método público não era chamado por ninguém; aqui o ramo era chamado só por
usuário, nunca por teste. Vale a mesma varredura: **para cada `if` que separa
"o sistema escolheu" de "o usuário escolheu", conferir se as duas pernas têm as
mesmas guardas.**

**Nesta tela um 400 não é evento, é estado do formulário.**
Corrigido o 500, os pisos novos passaram a devolver 400 — e a calculadora
recalcula a cada 500 ms, com o formulário pela metade quase o tempo todo. Como
`handleApiError` manda todo 400 para o `toast`, digitar um único número
empilhava **três avisos vermelhos**: a máscara passa por `0,05 · 0,53 · 5,30`
antes de chegar a `53,00`, e cada pausa acima do debounce dispara um. Medido no
navegador, não deduzido. O `CalculoUti` hoje separa os dois: **400 é recusa de
entrada e aparece inline**, numa faixa que some sozinha quando o número fica
inteiro; 500, sessão e rede continuam indo para o toast. Regra geral: **numa
superfície que recalcula sozinha, erro de validação é estado — quem decide se
vira toast é a tela, não o interceptor.** E o `GlobalExceptionHandler` prefixa
cada erro com o nome do campo em Java (`circPanturrilhaCm: …`), que desambigua
mensagem genérica mas é ruído quando a frase já nomeia o campo: a faixa tira o
prefixo na exibição.

**Regra clínica que uma entrada manual apaga precisa dizer que apagou.**
A precedência da meta proteica é *alvo digitado → terapia renal → obesidade →
faixa da fase*, e o alvo vencer é **certo**: é conduta explícita de quem
prescreve. O que não podia continuar é ele vencer **calado**. Com hemodiálise
contínua e alvo de 1,3 g/kg num paciente de 64,91 kg, o sistema adotava
**84,38 g** em vez de 129,82, media a adequação contra os 84,38, declarava *"a
dieta já cobre a meta proteica"* e **suprimia a sugestão do módulo proteico** —
até 45 g de déficit sem uma palavra na tela. Enquanto isso a interface prometia
o oposto em **cinco literais**: dois na tela (`ajuda` do seletor e a
`referencia` cravada de *"substitui a faixa da fase"*), dois na folha impressa e
um no javadoc do enum. E o servidor publicava a meta renal como
`META_POR_FAIXA`, então a tela escreveria *"136,0 · da faixa da fase"* debaixo
de *"Proteína — máximo 102,0"* — a única pista era a **ausência do `· máximo`**,
que ninguém procura. Três lições. (1) É a irmã da adesão, com o sinal
invertido: **quando a regra descarta um valor, publicar o descartado** — a
procedência do escolhido não diz o que foi calado. (2) **Frase montada no
servidor só pode levar número que venha de entrada.** A tentação era publicar
*"a hemodiálise recomenda 129,82 g/dia"*, e o número sairia de
`getProteinaGKg()`, que é **régua**: corrigir 2,0 para 1,9 faria toda avaliação
salva reabrir dizendo outro valor ao lado do gravado. `volumeTotalDescricao`
("62 ml/h × 22 h") escapa porque deriva **só de entradas**. Hoje o servidor
publica o *status* e a tela põe os números, que vêm de coluna. (3) **Frase de
estado bloqueado tem de nomear os vencedores, não uma ação a tentar.** A
primeira versão do texto do seletor travado dizia *"limpe o alvo calórico ou o
proteico"* — e no navegador, com o alvo proteico já vazio e diálise escolhida,
ela continuava travada mandando limpar um campo em branco: ali quem tomou a
proteína era a diálise. Só apareceu porque alguém abriu a tela.

**"Não calcula" pode ser "não há o que calcular", e a forma é que mente.**
A queixa era que o módulo proteico não calculava na calculadora. A aritmética
estava certa — lacuna zero, porque a dieta entregava 136,4 g contra uma meta de
84,38 — e o servidor explicava a frase certa. O que enganava era o **desenho**:
um grupo intitulado com o produto escolhido, **três traços**, a frase pendurada
só embaixo do primeiro campo e uma legenda órfã (*"do rótulo do produto"*) ao
lado de um valor vazio, porque o `TResult` só esconde a referência de quem tem
motivo para esconder. Sem sugestão não há três resultados a mostrar — há uma
frase. E *"no acompanhamento está correto"* era ilusão de segunda ordem: a
avaliação salva **não recalcula ao abrir**, ela exibe os números gravados de um
dia em que havia lacuna. Antes de caçar defeito de cálculo em duas telas que
compartilham componente, DTO e calculador, conferir se uma delas está
mostrando **retrato** em vez de recálculo.

**Bloco que a régua declara e o modelo não tem conta como completo.**
`SomaPorGrupo` marcava um grupo como fechado quando a lista de pendências estava
vazia — e a lista de um grupo **inexistente** também está vazia. Resultado: uma
MNA sem as perguntas G a R somava só a triagem e publicava aquilo como **escore
total**, lido pelas faixas de 30: nove pontos viram *"Desnutrido"* para quem
respondeu tudo o que havia para responder. A guarda do service impede que um
modelo assim seja **gravado**; a de `completo()` impede que ele seja **somado**, e
é a que não depende da outra existir. Regra geral: *nada a fazer* e *nada a
cobrar* não são a mesma coisa — `vazio.completo()` tem de ser falso, e o motivo
de um bloco ausente é próprio (*"este modelo não tem as perguntas deste bloco"*),
nunca um `"faltam responder: "` seguido de nada, que é traço mudo com prefixo.

**Verificação ao contrário escolhe o dado errado e não verifica nada.**
O teste do escore congelado inverte a pontuação de uma pergunta no modelo e exige
que a ficha salva não se mova. A primeira versão invertia a pergunta **A**, de
três opções — e o gabarito responde a **opção do meio** dela: `[0,1,2]` invertido
para `[2,1,0]` continua valendo 1 ali. O teste passava com o defeito e sem ele,
que é a definição de teste que não trava nada. Trocado para a pergunta **B**, de
quatro opções com a resposta na terceira, o defeito reintroduzido derruba o teste
na hora: 20,5 vira 19,5. *Reintroduzir o defeito* só prova alguma coisa quando o
dado do teste é sensível a ele — e num vetor simétrico, o ponto do meio é
invariante à inversão. Antes de confiar numa verificação ao contrário, perguntar
**qual número muda** quando o defeito volta.

**O método órfão era o conserto do defeito que a tela mostrou.**
`SomaPorGrupo.intocado()` foi escrito com javadoc e **nunca chamado** — o mesmo
padrão de `DietaEnteralCalculator.moduloProteico`, que passou por duas auditorias
verde e órfão. Aqui o preço apareceu no navegador: a MNA em branco imprimia as
**dezoito perguntas** dentro do painel de escore, logo acima do formulário que faz
exatamente essas dezoito — o questionário duas vezes na mesma tela, com cara de
diagnóstico. A regra que faltava era a que o método já dizia: *bloco intocado
conta, bloco começado enumera*. Listar o que falta só informa depois que alguém
começou; antes disso é ruído, porque o formulário inteiro é a lista. Duas lições:
`grep` do nome de cada método público novo **antes de dar a fatia por pronta**, e
— a mais barata — **abrir a tela vazia**, que foi o que revelou este e a
concordância de *"Nenhuma das 1 perguntas"* nos blocos de uma pergunta só da
NRS-2002. Nenhuma assertiva de número pega uma frase malfeita.

**`Map.of` numa mensagem que o usuário lê muda de assunto a cada execução.**
A guarda que confere os máximos de cada bloco iterava `escala.maximoPorGrupo()`,
que era um `Map.of` — sem ordem definida. A mesma edição errada num modelo
clonado ora reclamava da `TRIAGEM`, ora da `GLOBAL`, e o teste ficava vermelho de
forma intermitente. Pior que o teste: quem tenta salvar duas vezes e recebe duas
queixas diferentes conclui que o sistema está confuso, e não a edição. Coleção que
alimenta **texto lido por gente** é ordenada — `LinkedHashMap`, na ordem em que a
publicação apresenta os blocos.

**Rota literal antes de `/{id}`.** `/usuarios/global`, `/select` e `/perfil`
convivem com `/usuarios/{id}` porque o Spring prefere o literal. Se der
*"Valor inválido para o parâmetro: id"*, a aplicação em execução está
desatualizada — reinicie.

---

## Índice de documentação

| Doc | Assunto |
|---|---|
| [docs/00-PROMPT-MESTRE.md](docs/00-PROMPT-MESTRE.md) | Prompt de criação do projeto · ordem de construção · glossário |
| [docs/01-multitenant.md](docs/01-multitenant.md) | Isolamento por tenant — regra máxima |
| [docs/02-modulo-usuarios.md](docs/02-modulo-usuarios.md) | Usuários, níveis de acesso, log de acesso, troca de tenant |
| [docs/03-arquitetura-backend.md](docs/03-arquitetura-backend.md) | Padrão Java 21 + Spring Boot |
| [docs/04-arquitetura-frontend.md](docs/04-arquitetura-frontend.md) | Padrão React 19 + TS + Tailwind |
| [docs/05-identidade-visual.md](docs/05-identidade-visual.md) | Paleta, tipografia, tokens, marca |
| [docs/06-seguranca-owasp.md](docs/06-seguranca-owasp.md) | OWASP Top 10 aplicado a Java/Spring |
| [docs/07-banco-liquibase.md](docs/07-banco-liquibase.md) | PostgreSQL + Liquibase |
| [docs/08-modulo-pessoas.md](docs/08-modulo-pessoas.md) | Cadastro de pessoas — PF/PJ, contatos, endereços e vínculos |
| [docs/09-calculos-pediatria.md](docs/09-calculos-pediatria.md) | **Especificação numérica** das fórmulas da pediatria — OMS, DRIs, caso de teste |
| [docs/10-calculos-uti-adulto.md](docs/10-calculos-uti-adulto.md) | **Especificação numérica** da UTI adulto — Chumlea, Jung, Rabito, dieta enteral, hidratação |
| [docs/11-acompanhamento-pediatrico.md](docs/11-acompanhamento-pediatrico.md) | **Especificação** do acompanhamento diário pediátrico — os derivados, a idade que anda, e por que nenhuma constante nova entrou |
| [docs/12-fichas-de-anamnese.md](docs/12-fichas-de-anamnese.md) | **Fichas de anamnese** — a ficha dirigida por modelo, o retrato da pergunta, e o que falta do menu Clínica |
| [docs/13-escalas-nutricionais.md](docs/13-escalas-nutricionais.md) | **Especificação numérica** da MNA® e da NRS-2002 — pontos item a item, faixas, porta, o ponto por idade, e o que faltava na fonte recebida |

---

## Antes de escrever código novo

1. Procurar módulo semelhante já implementado no projeto
2. Seguir a mesma estrutura, nomenclatura e componentes
3. Só então gerar código novo

A implementação nova deve parecer ter sido escrita junto com o resto do sistema.
O módulo `usuario` é a referência de CRUD multi-tenant completo: migration →
entidade → DTOs `record` → mapper estático → repository com `AndTenantId` →
service com `@Transactional` → controller com `@PreAuthorize` → testes.

Para **módulo de negócio** (não administrativo), a referência é `pessoa`:
`isAuthenticated()` em vez de role, listas filhas sincronizadas num único PUT e
catálogo de referência sem tenant. Ver [docs/08](docs/08-modulo-pessoas.md).

Boa parte do que vem pela frente já existe no **eroERP**
(`~/Documentos/EroErp`), de onde o cadastro de pessoas foi portado. Ao começar
um módulo novo, procure lá primeiro — e lembre que `cliente_id` de lá é
`tenant_id` aqui, `BIGSERIAL` vira UUID e JPQL com filtro opcional vira native
query com `CAST`.

---

## Olhar as telas — não pule isto

**Não há teste de front neste projeto**, e `build` + `lint` + a suíte inteira
verde **já deixaram passar cinco defeitos reais** que só apareceram no
navegador: um prontuário imprimindo "100 kcal por 100 ml" ao lado de "880 ml ·
616 kcal"; 26 traços mudos numa tela cujos motivos o servidor calculava
fielmente; uma nota prometendo "arredondado" ao lado de 14,33.

A skill **[`olhar-telas`](.claude/skills/olhar-telas/SKILL.md)** tem o driver
pronto e as sete armadilhas já pagas (esperar por `aside` e não por `**/app**`,
máscara de centavos, combo de paciente × seletor de tenant, campo escondido em
outra aba). Uso direto:

```bash
node .claude/skills/olhar-telas/olhar.mjs /app/uti/calculadora tela
node .claude/skills/olhar-telas/olhar.mjs /app/uti/avaliacoes lista --pdf
node .claude/skills/olhar-telas/olhar.mjs /app/pediatria/dashboard ped --360
```

Sai em `/tmp/olhar/`, e reporta rolagem horizontal, elemento culpado e erro de
console. **Depois abra o arquivo** — gerar e não olhar não vale.

`playwright-core` já vem no `node_modules` do front (dependência do Vite) e
dirige o `/usr/bin/google-chrome` do sistema: **não instale playwright**.

## Rodar

```bash
cd nutri-hospitalar-api
cp .env.example .env      # ajuste DB_PASSWORD e JWT_SECRET
./run-dev.sh              # sobe em :8080
./run-dev.sh test         # 475 testes contra nutridb_test
```

Exige **JDK 21**. O `run-dev.sh` localiza o JDK certo mesmo que o `JAVA_HOME` da
máquina aponte para outra versão. Detalhes no
[README da API](nutri-hospitalar-api/README.md).

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
│           ├── controller/        # pessoa, contato, catalogo, atendimento,
│           ├── dtos/              # pediatria, uti, ...
│           ├── entity/
│           ├── enums/
│           ├── mapper/
│           ├── repository/
│           └── service/
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
        ├── components/layout/     # Layout · Sidebar · TenantSwitcher · TProtected
        ├── contexts/              # AuthContext · ThemeContext
        ├── hooks/                 # useAuth · useTheme · useDebounce
        ├── services/              # api.ts (interceptor + refresh) + um por módulo
        ├── pages/                 # auth/Login · Dashboard · usuario · tenant
        │                          # pessoa · loginlog · perfil · pediatria
        │                          # uti/ (catálogos, cálculo, avaliação,
        │                          #      acompanhamento e os 3 painéis)
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
| 4 — Atendimento | pendente |
| 7 a 9 — Catálogos · Acompanhamento · audit_log | pendentes |

**272 testes** no total, contra o banco `nutridb_test`.

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

Também não implementado, de propósito: **recuperação de senha** (depende de
definir o serviço de e-mail) e **`audit_log`** de operações de negócio — este
entra como fatia própria agora que o cadastro de pessoas fechou, e não junto
dele: auditar CRUD antes de o cadastro estar estável significaria refazer o log
a cada mudança de campo. Detalhe em [README da API](nutri-hospitalar-api/README.md).

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

## Rodar

```bash
cd nutri-hospitalar-api
cp .env.example .env      # ajuste DB_PASSWORD e JWT_SECRET
./run-dev.sh              # sobe em :8080
./run-dev.sh test         # 272 testes contra nutridb_test
```

Exige **JDK 21**. O `run-dev.sh` localiza o JDK certo mesmo que o `JAVA_HOME` da
máquina aponte para outra versão. Detalhes no
[README da API](nutri-hospitalar-api/README.md).

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
│           ├── dtos/              # antropometria, necessidade, prescricao,
│           ├── entity/            # pediatria, formula, suplemento, ...
│           ├── enums/
│           ├── mapper/
│           ├── repository/
│           └── service/
└── nutri-hospitalar-web/          # fatia 2 pronta
    ├── public/                    # favicon, ícones, og-image
    └── src/
        ├── assets/brand/          # logo-full · logo-mark · logo-simbolo (currentColor)
        ├── assets/icons/          # SVG inline, módulo único
        ├── styles/theme.css       # tokens Tailwind v4 + temas claro/escuro
        ├── components/common/     # TPage TPanel TDataGrid TDataGridFooter
        │                          # TEntry TSelect TCombo TButton TBadge TModal TThemeToggle
        ├── components/layout/     # Layout · Sidebar · TenantSwitcher · TProtected
        ├── contexts/              # AuthContext · ThemeContext
        ├── hooks/                 # useAuth · useTheme · useDebounce
        ├── services/              # api.ts (interceptor + refresh) + um por módulo
        ├── pages/                 # auth/Login · Dashboard · usuario · tenant
        │                          # pessoa · loginlog · perfil
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
| **4 — Atendimento** | ⬅️ **próxima** |
| 5 a 9 — Antropometria · Necessidades · Pediatria · Catálogos · Acompanhamento | pendentes |

**90 testes** no total, contra o banco `nutridb_test`.

**Bloqueio conhecido:** as fatias 5 a 7 (cálculo) dependem de um documento que
**ainda não existe** — a especificação numérica das fórmulas, a extrair de
`Facilita Nutri na UTI - com SA_atualiza (1).xlsx` e `Pediatria.xlsx` (na raiz),
com referência bibliográfica, unidade e caso de teste por fórmula. Nenhuma
fórmula nutricional deve ser implementada por inferência: o sistema prescreve
dieta para paciente de UTI. As fatias 3 e 4 não dependem disso.

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
./run-dev.sh test         # 41 testes contra nutridb_test
```

Exige **JDK 21**. O `run-dev.sh` localiza o JDK certo mesmo que o `JAVA_HOME` da
máquina aponte para outra versão. Detalhes no
[README da API](nutri-hospitalar-api/README.md).

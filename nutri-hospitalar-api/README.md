# Nutri Hospitalar de Sucesso — API

Backend da calculadora nutricional hospitalar multi-tenant.
Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Liquibase.

Padrões do projeto: [`../docs/`](../docs/) · Regras para agentes: [`../CLAUDE.md`](../CLAUDE.md)

---

## Pré-requisitos

- **JDK 21** — obrigatório. Se o `JAVA_HOME` da máquina apontar para outra versão,
  o build falha com `release version 21 not supported`. O `run-dev.sh` localiza o
  JDK 21 sozinho; para rodar o Maven na mão, exporte:
  `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`
- **PostgreSQL** rodando localmente
- Maven não precisa estar instalado se houver `./mvnw`

Sem Docker.

## Banco

```bash
createdb -U postgres -h localhost nutridb
createdb -U postgres -h localhost nutridb_test    # usado pelos testes
```

As tabelas são criadas pelo **Liquibase** na subida
(`src/main/resources/db/changelog/`). Nunca alterar o banco na mão.

## Variáveis de ambiente

```bash
cp .env.example .env      # ajuste os valores
```

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | `jdbc:postgresql://localhost:5432/nutridb` |
| `DB_USER` | sim | usuário do banco |
| `DB_PASSWORD` | sim | senha do banco |
| `JWT_SECRET` | sim | chave HMAC, **mínimo 32 caracteres** |
| `SUPERADMIN_NAME` | sim | nome do superadmin criado na primeira subida |
| `SUPERADMIN_EMAIL` | sim | e-mail do superadmin |
| `SUPERADMIN_PASSWORD` | sim | senha do superadmin, **mínimo 10 caracteres** |
| `SUPERADMIN_TENANT_NOME` | não | nome do tenant raiz (padrão: `Nutri Hospitalar Admin`) |
| `CORS_ALLOWED_ORIGINS` | não em dev, **sim em prod** | origens do frontend, separadas por vírgula |
| `SPRING_PROFILES_ACTIVE` | não | `dev` (padrão) ou `prod` |
| `ACESSO_FUSO_HORARIO` | não (só em prod) | fuso em que o período de acesso vence (padrão: `America/Sao_Paulo`) |
| `TEST_DB_URL` | não | banco de teste; cai em `nutridb_test` |

Valores com espaço precisam de aspas — o `run-dev.sh` faz `source .env`:
`SUPERADMIN_NAME="Eduardo Ohlweiler"`

O `.env` está no `.gitignore`. Nenhum segredo vai para o repositório.

## Executar

```bash
./run-dev.sh            # sobe a API em http://localhost:8080
./run-dev.sh test       # roda os 74 testes
./run-dev.sh package    # gera o jar
```

O script valida as variáveis obrigatórias e o tamanho do `JWT_SECRET` antes de subir.

Na primeira subida, o `DataSeeder` cria o tenant raiz e o superadmin. É idempotente.

- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Testes

```bash
./run-dev.sh test
```

Rodam contra o PostgreSQL local (`nutridb_test`), com o Liquibase recriando o
schema a cada execução. **Não usamos H2**: sem `unaccent`, com tratamento
diferente de `NULL` em constraint única e aceitando JPQL que o Postgres rejeita,
ele daria falsa confiança exatamente onde dói.

Cobertura das fatias 1 e 3 — 74 testes:

| Classe | O que prova |
|---|---|
| `TenantIsolationTest` | tenant A não lê, altera nem desativa nada do tenant B; listagem e log de acesso não vazam |
| `PeriodoAcessoTest` | o prazo do cliente nasce com ele e é ignorado em tenant existente; a rotina desliga só os vencidos e nunca o tenant do superadmin; login e refresh barram o vencido antes da rotina; reativar sem renovar é 409; renovar reconta a data e devolve o acesso |
| `CriacaoDeClienteTest` | criar usuário sem tenant abre um cliente novo com ele como ADMIN; com `tenantId` ou dentro de um tenant, entra no existente; e-mail duplicado em outro tenant é recusado; a listagem global acha quem a do tenant não acha, filtra por tenant quando pedido e recusa quem não é superadmin |
| `AuthTest` | a rota de registro não existe (e nenhum tenant nasce por ela); rota inexistente devolve 404 e método errado 405, nunca 500; mensagem de falha uniforme; bloqueio após 5 tentativas; rotação e detecção de reuso de refresh token; troca de tenant |
| `AutorizacaoTest` | área administrativa restrita ao superadmin; perfil próprio; token adulterado; desativação derruba token em uso |
| `PessoaTest` | PF e PJ com validação de dígito verificador; documento duplicado no tenant é 409 e **o mesmo CPF em outro tenant passa**; campo do tipo errado é 400 e a troca de tipo limpa o anterior; a lista de contatos substitui o estado, com um só principal; pessoa de outro tenant é 404; `USER` opera o cadastro |

## Modelo de acesso

Nível único por usuário, enum na coluna `usuario.role`:

| Nível | Alcance |
|---|---|
| `SUPERADMIN` | administra o sistema; **único que atravessa tenants** |
| `ADMIN` | administra o próprio tenant |
| `USER` | opera dentro do próprio tenant |

A **área administrativa** — `/tenants`, `/usuarios`, `/login-logs` — é exclusiva do
`SUPERADMIN`. `ADMIN` e `USER` acessam os módulos de negócio e o próprio perfil
(`/usuarios/perfil`).

### Criar um cliente novo

**Não existe rota pública de registro.** `POST /usuarios` é o único caminho por
onde um usuário nasce, e é exclusivo do SUPERADMIN. Também não há tela de cadastro
de tenant: o cliente nasce junto do seu primeiro usuário. O destino é decidido
assim:

| Entrada | Destino |
|---|---|
| `tenantId` informado | aquele tenant |
| superadmin **dentro** de um tenant (troca de tenant) | o tenant efetivo |
| superadmin no tenant raiz, **sem** `tenantId` | **cria um tenant novo** com o nome do usuário |

Ao abrir um cliente novo o usuário nasce `ADMIN` — a role informada é ignorada,
porque um tenant sem administrador seria um tenant que ninguém consegue gerir.
Para tenant existente a role é obrigatória.

O **e-mail é único no sistema inteiro**, não por tenant: é a credencial de login,
e a autenticação acontece antes de o tenant ser conhecido.

### Período de acesso — a licença do cliente

O prazo mora no **tenant** e é escolhido no mesmo `POST /usuarios` que abre o
cliente (`periodoAcesso`: `INDETERMINADO` · `UM_MES` · `DOIS_MESES` ·
`TRES_MESES` · `SEIS_MESES` · `UM_ANO` · `DOIS_ANOS`). Informado ao entrar num
tenant existente, é ignorado — a licença é do cliente, não do assento.

- A data expira **no fim do dia**, no fuso de `app.acesso.fuso-horario`
  (padrão `America/Sao_Paulo`)
- `ManutencaoJob.inativarAcessosExpirados` roda **diariamente às 03h15** e
  desliga quem venceu. Nunca desliga o tenant que abriga um SUPERADMIN
- O login e o refresh barram o vencido **sem esperar a rotina**, registrando
  `ACESSO_EXPIRADO` no log de acesso
- `PATCH /tenants/{id}/acesso` define ou renova, sempre recontando a partir de
  hoje, e reativa o cliente desligado por prazo vencido. Reativar por
  `PATCH /tenants/{id}/ativo` com prazo vencido devolve **409**: sem renovar, a
  rotina desfaria a reativação na madrugada seguinte

### Isolamento por tenant

Manual e explícito: toda consulta filtra por `tenant_id` obtido do claim do JWT,
via `securityUtils.getTenantIdLogado()`. Recurso de outro tenant devolve **404**,
nunca 403 — um 403 confirmaria que o id existe.

O superadmin **não fura** o filtro: `POST /auth/switch-tenant/{id}` reemite o token
com o tenant de destino e `impersonating: true`. Ele muda o *valor* do filtro, não
o desliga. Não há caminho de código sem `WHERE tenant_id = ?`.

### Ler pode ser global; escrever é dentro do tenant

`GET /usuarios` devolve só o tenant efetivo — e por isso um cliente recém-criado
parecia ter sumido: o usuário existia, mas em outro tenant. `GET /usuarios/global`
resolve isso: atravessa clientes, aceita `tenantId` como **filtro opcional** e
devolve `tenantId`/`tenantNome` em cada linha.

Ela é exclusiva do superadmin, e é a **única** consulta do sistema sem
`WHERE tenant_id = ?` — troca o filtro por `@PreAuthorize("hasRole('SUPERADMIN')")`.
As escritas continuam todas dentro do tenant: `GET`/`PUT /usuarios/{id}` devolvem
404 para quem é de outro cliente. Por isso o front entra no tenant do usuário
antes de abrir a edição, em vez de a API relaxar o filtro.

O cadastro de pessoas, que veio depois, **não** ganhou visão global: não havia a
mesma necessidade. Visão global é exceção justificada, não conveniência.

## Cadastro de pessoas

Porte do módulo `pessoa` do eroERP, com `cliente_id` virando `tenant_id`.
Detalhe em [`../docs/08-modulo-pessoas.md`](../docs/08-modulo-pessoas.md).

- Uma tabela para **pessoa física e jurídica**, classificada por **tipos de
  cadastro** (Paciente · Responsável · Profissional de saúde · Fornecedor ·
  Outros). Paciente não é tabela: é um tipo de cadastro de uma pessoa
- **CPF e CNPJ com dígito verificador**; documento chega com ou sem máscara e é
  gravado só com dígitos. Unicidade **por tenant** — duas clínicas podem
  atender a mesma pessoa
- Campo do tipo errado (CNPJ numa pessoa física) é **recusado com 400**, não
  ignorado; trocar de tipo no `PUT` limpa os campos do tipo anterior
- Telefones, e-mails e redes sociais viajam **dentro** do `POST`/`PUT` da
  pessoa, e a lista é o **estado completo**: item com `id` atualiza, sem `id`
  nasce, o que não vier é removido. Só um principal por lista; nenhum marcado
  promove o primeiro
- Os catálogos (`/tipos-cadastro/select` e os outros três) são **globais, sem
  `tenant_id`** e somente leitura — ver `../docs/01-multitenant.md` §6.1
- É **módulo de negócio**: `isAuthenticated()`, ADMIN e USER operam

## Endpoints

### Público (com rate limit de 5/min por IP)

Duas rotas. Não existe registro público.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/login` | autentica |
| `POST` | `/auth/refresh` | rotaciona o par de tokens |

### Autenticado

| Método | Rota |
|---|---|
| `POST` | `/auth/logout` |
| `GET` `PUT` | `/usuarios/perfil` |
| `PATCH` | `/usuarios/perfil/senha` |
| `GET` `POST` `PUT` `PATCH` | `/pessoas`, `/pessoas/{id}`, `/pessoas/{id}/ativo`, `/pessoas/select` |
| `GET` | `/tipos-cadastro/select`, `/tipos-telefone/select`, `/tipos-email/select`, `/tipos-rede-social/select` |

### Somente SUPERADMIN

| Método | Rota |
|---|---|
| `GET` `PUT` `PATCH` | `/tenants`, `/tenants/{id}`, `/tenants/{id}/ativo`, `/tenants/{id}/acesso`, `/tenants/select` |
| `GET` `POST` `PUT` `PATCH` | `/usuarios`, `/usuarios/global`, `/usuarios/{id}`, `/usuarios/{id}/ativo`, `/usuarios/{id}/desbloquear`, `/usuarios/select` |
| `GET` | `/login-logs`, `/login-logs/global` |
| `POST` | `/auth/switch-tenant/{tenantId}`, `/auth/exit-tenant` |

## Tokens

| Token | Validade | Onde guardar |
|---|---|---|
| Access | 8 horas | memória do front |
| Refresh | 7 dias | `localStorage`; no banco só o hash SHA-256 |

O refresh é de **uso único com rotação**. Reapresentar um token já consumido
revoga toda a cadeia do usuário — é o sinal clássico de token roubado.

O `JwtFilter` recarrega usuário e tenant a cada requisição e rejeita se qualquer
um estiver inativo. Custa um SELECT por chamada e é o que impede um token válido
de continuar funcionando depois de uma desativação.

## Segurança

`docs/06-seguranca-owasp.md` tem o detalhe. Em resumo: BCrypt força 12; headers
HSTS/CSP/frame-options; CORS por variável de ambiente; Actuator só `health`+`info`;
Swagger desligado em produção; nenhum stack trace na resposta; bloqueio após 5
tentativas de login; log de acesso registrando sucesso **e** falha.

**Nunca** vão para log: senha, JWT, refresh token ou dado clínico de paciente.

Análise de dependências (a primeira execução baixa a base do NVD):

```bash
./mvnw -Psecurity dependency-check:check
```

## Pendências conhecidas

Coisas que o sistema **não** faz hoje, registradas para não virarem surpresa:

| Item | Por quê ainda não | Quando |
|---|---|---|
| Recuperação de senha | depende de definir o serviço de e-mail (SMTP, Resend, Brevo…) | fatia própria, quando o envio de e-mail for decidido |
| `audit_log` de operações de negócio | só o `login_log` existe; auditar CRUD só faz sentido quando houver paciente e prescrição | junto da fatia de Paciente |
| Confirmação de e-mail | mesma dependência de envio de e-mail. Menos urgente sem autocadastro: quem cria a conta é o superadmin, então o e-mail já foi conferido por uma pessoa | com a recuperação de senha |

Enquanto não há recuperação de senha, o superadmin resolve pelo painel:
desativa/reativa a conta, desbloqueia após tentativas, e o próprio usuário troca a
senha em `PATCH /usuarios/perfil/senha` informando a atual.

## Notas de implementação

**Listagens com filtro usam native query** com `CAST(:param AS tipo)` explícito.
O JPQL do Hibernate 6 não consegue tipar um parâmetro nulo solto em
`:param IS NULL`, e o PostgreSQL responde *"não foi possível determinar o tipo de
dados do parâmetro $N"*. O cast nativo resolve e ainda libera `unaccent` na busca
por nome.

**A ordenação é fixa no SQL.** `PageableUtils.semOrdenacao(...)` descarta o `Sort`
do cliente: em native query o Spring Data usaria o nome da propriedade como nome
de coluna (`dataLogin` não existe, a coluna é `data_login`), e `?sort=` é entrada
de usuário que vira SQL. Quando alguma tela precisar de ordenação por coluna, ela
entra com allowlist de campos.

**Escrita que precisa sobreviver a rollback** usa o `TransactionTemplate`
`transacaoIndependente`: contador de tentativas de login, revogação de refresh
token reutilizado e log de falha acontecem em caminhos que terminam em exceção —
na transação corrente, o rollback apagaria justamente o rastro que se queria
guardar.

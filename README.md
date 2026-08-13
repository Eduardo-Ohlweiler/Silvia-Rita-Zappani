# Nutri Hospitalar de Sucesso

Calculadora nutricional hospitalar **multi-tenant** — UTI adulto e pediatria.
Substitui as planilhas de cálculo usadas à beira do leito.

```
sistema silvia/
├── nutri-hospitalar-api/     Java 21 · Spring Boot 3.5 · PostgreSQL 16
├── nutri-hospitalar-web/     React 19 · TypeScript · Vite · Tailwind v4
├── docs/                     padrões do projeto (ler antes de codar)
└── CLAUDE.md                 regras para agentes de IA
```

**Sem Docker.** PostgreSQL instalado localmente, variáveis em `.env`.

---

## 1. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| **JDK** | **21** | obrigatório — o build falha no 17 |
| **PostgreSQL** | 16+ | rodando em `localhost:5432` |
| **Node.js** | 20+ | testado no 22 |
| Maven | — | não precisa instalar se houver `./mvnw` |

Confira:

```bash
java -version     # precisa dizer 21
node -v
pg_isready
```

> **Atenção ao `JAVA_HOME`.** Se a máquina tiver mais de um JDK, é comum o
> `JAVA_HOME` apontar para o 17 mesmo com o `java` do PATH sendo 21 — e o Maven
> usa o `JAVA_HOME`. O `run-dev.sh` localiza o JDK 21 sozinho. Para rodar o Maven
> na mão, exporte antes:
>
> ```bash
> export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
> ```

---

## 2. Banco de dados

Crie os dois bancos vazios. As tabelas são criadas pelo **Liquibase** na primeira
subida — nunca altere o banco na mão.

```bash
createdb -U postgres -h localhost nutridb
createdb -U postgres -h localhost nutridb_test    # usado pelos testes
```

Se o `createdb` pedir senha e você não tiver o `.pgpass` configurado:

```bash
PGPASSWORD=suasenha createdb -U postgres -h localhost nutridb
PGPASSWORD=suasenha createdb -U postgres -h localhost nutridb_test
```

---

## 3. Variáveis de ambiente

### Backend — `nutri-hospitalar-api/.env`

```bash
cd nutri-hospitalar-api
cp .env.example .env      # e ajuste os valores
```

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | **sim** | `jdbc:postgresql://localhost:5432/nutridb` |
| `DB_USER` | **sim** | usuário do PostgreSQL (ex.: `postgres`) |
| `DB_PASSWORD` | **sim** | senha do PostgreSQL |
| `JWT_SECRET` | **sim** | chave HMAC do JWT — **mínimo 32 caracteres**; a aplicação se recusa a subir com menos |
| `SUPERADMIN_NAME` | **sim** | nome do superadmin criado na primeira subida |
| `SUPERADMIN_EMAIL` | **sim** | e-mail de login do superadmin |
| `SUPERADMIN_PASSWORD` | **sim** | senha do superadmin — **mínimo 10 caracteres** |
| `SUPERADMIN_TENANT_NOME` | não | nome do tenant raiz (padrão: `Nutri Hospitalar Admin`) |
| `CORS_ALLOWED_ORIGINS` | não em dev<br>**sim em prod** | origens do front, separadas por vírgula (padrão em dev: `http://localhost:5173,http://localhost:4173`) |
| `SPRING_PROFILES_ACTIVE` | não | `dev` (padrão) ou `prod` |
| `TEST_DB_URL` | não | banco de teste; cai em `nutridb_test` |

**Duas armadilhas que já custaram tempo:**

1. **Valor com espaço precisa de aspas.** O `run-dev.sh` faz `source .env`, então
   `SUPERADMIN_NAME=Eduardo Ohlweiler` quebra o shell. Escreva
   `SUPERADMIN_NAME="Eduardo Ohlweiler"`.
2. **A senha do superadmin precisa de 10 caracteres.** Com menos, a aplicação
   sobe e morre no `DataSeeder`, de propósito — é dado de saúde, a política é
   rígida.

### Frontend — `nutri-hospitalar-web/.env.local`

```bash
cd nutri-hospitalar-web
cp .env.example .env.local
```

| Variável | Obrigatória | Descrição |
|---|---|---|
| `VITE_API_URL` | não em dev | base das chamadas à API. Em dev use `/api`, que passa pelo proxy do Vite. Em produção, a URL real (`https://api.seudominio.com`). |

Em desenvolvimento **não precisa mexer em nada**: o Vite faz proxy de `/api` para
`http://localhost:8080`, então o front chama a própria origem e não há CORS no
caminho.

> Só variáveis com prefixo `VITE_` chegam ao navegador — e tudo que chega vai no
> bundle, à vista de qualquer um. **Nunca** coloque segredo no `.env.local` do
> front.

Nenhum dos dois `.env` vai para o repositório (estão no `.gitignore`).

---

## 4. Rodar

Precisa de **dois terminais**.

### Terminal 1 — backend

```bash
cd nutri-hospitalar-api
./run-dev.sh
```

O script valida as variáveis obrigatórias e o tamanho do `JWT_SECRET` antes de
subir, e localiza o JDK 21 sozinho.

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Na primeira subida o `DataSeeder` cria o tenant raiz e o superadmin com as
credenciais do `.env`. É idempotente — nas próximas não faz nada.

### Terminal 2 — frontend

```bash
cd nutri-hospitalar-web
npm install       # só na primeira vez
npm run dev
```

- Aplicação: `http://localhost:5173`

### Entrar

Use o `SUPERADMIN_EMAIL` e o `SUPERADMIN_PASSWORD` do `.env` do backend.

---

## 5. Outros comandos

### Backend

```bash
./run-dev.sh test                      # 41 testes contra nutridb_test
./run-dev.sh package                   # gera o jar em target/
./mvnw -Psecurity dependency-check:check   # OWASP A06 (a 1ª vez baixa a base do NVD)
```

Os testes rodam contra o PostgreSQL local, com o Liquibase recriando o schema a
cada execução. Não usamos H2 nem Testcontainers — o porquê está no
[README da API](nutri-hospitalar-api/README.md).

### Frontend

```bash
npm run build     # tsc + build de produção em dist/
npm run preview   # serve o dist/ em :4173
npm run lint      # oxlint
```

---

## 6. Modelo de acesso

Nível único por usuário, enum na coluna `usuario.role`:

| Nível | Alcance |
|---|---|
| `SUPERADMIN` | administra o sistema; **único que atravessa tenants** |
| `ADMIN` | administra o próprio tenant |
| `USER` | opera dentro do próprio tenant |

A área administrativa — `/tenants`, `/usuarios`, `/login-logs` — é exclusiva do
`SUPERADMIN`. `ADMIN` e `USER` acessam os módulos de negócio e o próprio perfil.

**Não há autocadastro.** Não existe rota pública de registro: toda conta é
provisionada pelo superadmin em `POST /usuarios`. E não há tela de cadastro de
tenant — um cliente nasce junto do seu primeiro usuário, quando o superadmin
cadastra alguém sem indicar tenant. O primeiro superadmin vem do `DataSeeder`,
por variável de ambiente.

**Isolamento por tenant é a regra máxima do projeto.** Toda consulta filtra por
`tenant_id`, e recurso de outro tenant devolve 404 — nunca 403. O superadmin não
fura o filtro: ele troca o *valor* do filtro via `switch-tenant`.
Detalhes em [docs/01-multitenant.md](docs/01-multitenant.md).

---

## 7. Estado do projeto

| Fatia | Situação |
|---|---|
| **1 — Base, acesso e auditoria** | ✅ pronta · 41 testes |
| **2 — Casca do front e área administrativa** | ✅ pronta |
| **3 — Paciente** | ⬅️ **próxima** |
| 4 — Atendimento | pendente |
| 5 — Antropometria | pendente |
| 6 — Necessidades e prescrição enteral | pendente |
| 7 — Pediatria | pendente |
| 8 — Catálogos (fórmulas, suplementos) | pendente |
| 9 — Acompanhamento e relatórios | pendente |

### O que já dá para usar

Entre com o `SUPERADMIN_EMAIL` do `.env` e você tem:

| Tela | Rota |
|---|---|
| Login, com alternador de tema | `/login` |
| Dashboard (provisório) | `/` |
| Meu perfil e troca de senha | `/perfil` |
| Usuários — lista global com filtro de cliente, criação e edição | `/usuarios` |
| Tenants — listar, entrar, ativar/desativar | `/tenants` |
| Log de acesso — por tenant ou global | `/log-acesso` |

Sidebar clara, tema claro e escuro alternáveis, seletor de tenant do superadmin
no header, faixa de aviso quando está dentro de outro tenant, e tudo responsivo
até 360px — grid vira cartão no mobile, sidebar vira *drawer*.

**Não implementado ainda**, registrado para não virar surpresa:

- **Recuperação de senha** — depende de definir o serviço de e-mail. Enquanto
  isso o superadmin resolve pelo painel, e o usuário troca a própria senha
  informando a atual.
- **Auditoria de operações de negócio** (`audit_log`) — hoje só existe o
  `login_log`. Entra junto da fatia de Paciente.
- **Especificação numérica das fórmulas** — as fatias 5 a 7 dependem de um
  documento que extraia as fórmulas das planilhas com referência bibliográfica e
  caso de teste. Nenhuma fórmula deve ser implementada "de memória".

---

## 8. Documentação

Leia antes de escrever código. O [CLAUDE.md](CLAUDE.md) é o resumo carregado em
toda sessão de IA.

| Doc | Assunto |
|---|---|
| [docs/00-PROMPT-MESTRE.md](docs/00-PROMPT-MESTRE.md) | visão do sistema, ordem de construção, glossário do domínio |
| [docs/01-multitenant.md](docs/01-multitenant.md) | isolamento por tenant — a regra máxima |
| [docs/02-modulo-usuarios.md](docs/02-modulo-usuarios.md) | usuários, níveis de acesso, log de acesso, troca de tenant |
| [docs/03-arquitetura-backend.md](docs/03-arquitetura-backend.md) | padrão Java 21 + Spring Boot |
| [docs/04-arquitetura-frontend.md](docs/04-arquitetura-frontend.md) | padrão React 19 + TS + Tailwind |
| [docs/05-identidade-visual.md](docs/05-identidade-visual.md) | paleta, tipografia, tokens, arquivos da marca |
| [docs/06-seguranca-owasp.md](docs/06-seguranca-owasp.md) | OWASP Top 10 aplicado a Java/Spring + LGPD |
| [docs/07-banco-liquibase.md](docs/07-banco-liquibase.md) | PostgreSQL + Liquibase |

---

## 9. Problemas comuns

| Sintoma | Causa e solução |
|---|---|
| `release version 21 not supported` | O Maven está usando o JDK 17. Use `./run-dev.sh`, ou exporte `JAVA_HOME` para o JDK 21. No VS Code, o [.vscode/settings.json](.vscode/settings.json) já aponta o 21 — reinicie o language server: `Ctrl+Shift+P` → *Java: Clean Java Language Server Workspace*. |
| Erros vermelhos no VS Code que o `mvn` não reproduz | Cache velho do language server. Mesma limpeza acima. |
| `.env: linha N: comando não encontrado` | Valor com espaço sem aspas no `.env`. Ver §3. |
| `SUPERADMIN_PASSWORD precisa de no mínimo 10 caracteres` | É proposital. Aumente a senha no `.env`. |
| `JWT_SECRET precisa de no mínimo 32 caracteres` | Idem. |
| `FATAL: database "nutridb" does not exist` | Falta criar os bancos. Ver §2. |
| `fe_sendauth: no password supplied` | O `DB_PASSWORD` do `.env` está vazio ou errado. |
| Front carrega mas as chamadas à API falham | O backend não está no ar. Confira `curl http://localhost:8080/actuator/health`. |
| Porta 8080 ou 5173 ocupada | `fuser -k 8080/tcp` (ou `5173`). |
| `Conta temporariamente bloqueada` no login | Cinco senhas erradas bloqueiam por 15 minutos. Espere, ou libere em `PATCH /usuarios/{id}/desbloquear` com o superadmin. |

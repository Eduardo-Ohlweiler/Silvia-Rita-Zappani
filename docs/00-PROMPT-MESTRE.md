# Prompt-Mestre — Nutri Hospitalar de Sucesso

> Este é o documento de partida. Ele descreve **o que** construir, **com o quê** e **em que ordem**.
> Os documentos 01–07 descrevem **como**. Leia este primeiro e o específico da tarefa depois.

---

## 1. O sistema

**Nome:** Nutri Hospitalar de Sucesso
**Marca:** Silvia Rita Zappani — nutrição clínica hospitalar
**Repositório:** `nutri-hospitalar`

Sistema web **multi-tenant** que substitui as planilhas de cálculo nutricional usadas
à beira do leito. O nutricionista cadastra o paciente, registra as medidas
antropométricas, e o sistema calcula: estimativas de peso e altura, estado
nutricional, necessidades calóricas e proteicas, prescrição de nutrição enteral
(contínua, intermitente e sistema aberto), hidratação, e o acompanhamento
seriado do paciente. Há um módulo pediátrico próprio, com percentis da OMS.

**Público:** nutricionistas clínicos e hospitalares, cada um (ou cada clínica) num
tenant próprio.

**Porte:** leve. Poucas dezenas de usuários por tenant, baixo volume de escrita,
cálculo puro em memória. Nada de fila, cache distribuído, microserviço ou
event-sourcing. Um backend Spring Boot, um banco PostgreSQL, um front React.

---

## 2. Stack obrigatória

### Backend — `nutri-hospitalar-api`

| Item | Versão / escolha |
|---|---|
| Java | **21** |
| Spring Boot | **3.5.x** |
| Build | Maven (com wrapper `mvnw`) |
| Pacote base | `com.nutri.hospitalar` |
| Banco | PostgreSQL 16 |
| Migrations | Liquibase **XML** |
| ORM | Spring Data JPA + Hibernate 6 |
| Segurança | Spring Security + JJWT **0.12.x** (API nova: `Jwts.builder().subject(...)`, `Jwts.parser().verifyWith(...)`) |
| Hash de senha | BCrypt, força 12 |
| Rate limiting | Bucket4j |
| Validação | Bean Validation (Jakarta) |
| Docs | SpringDoc OpenAPI 2.8.x |
| Observabilidade | Spring Boot Actuator |
| Boilerplate | Lombok |
| Testes | JUnit 5 + Spring Boot Test, contra PostgreSQL local (`nutridb_test`) |

Não incluir JasperReports, Apache POI ou qualquer dependência de relatório/planilha
até existir um requisito concreto que as exija.

### Frontend — `nutri-hospitalar-web`

| Item | Versão / escolha |
|---|---|
| React | **19** |
| Linguagem | **TypeScript** (zero `any`) |
| Bundler | Vite |
| Estilo | **Tailwind v4** (`@theme` com os tokens da marca) |
| Rotas | react-router-dom v7 |
| HTTP | axios (via `services/api.ts`) |
| Formulários | react-hook-form + **zod** |
| Notificação | react-toastify |
| Ícones | SVG inline em `assets/icons` |
| Fontes | `@fontsource/ibm-plex-sans` + `@fontsource/lato` (self-hosted) |

### Infraestrutura

**Sem Docker.** PostgreSQL instalado localmente, variáveis de ambiente em `.env`
(fora do repositório) e `./run-dev.sh` para subir. Exige **JDK 21**.

---

## 3. Estrutura de repositório

```
sistema silvia/
├── CLAUDE.md
├── docs/                               # estes documentos
├── nutri-hospitalar-api/
│   ├── pom.xml
│   ├── mvnw
│   └── src/
│       ├── main/java/com/nutri/hospitalar/
│       │   ├── NutriHospitalarApplication.java
│       │   ├── baseentity/             # BaseEntity, TenantEntity
│       │   ├── config/                 # SecurityConfig, JwtUtil, JwtFilter,
│       │   │                           # SecurityUtils, CorsConfig, SwaggerConfig,
│       │   │                           # RateLimitFilter, DataSeeder
│       │   ├── exceptions/             # GlobalExceptionHandler + hierarquia
│       │   └── {modulo}/
│       │       ├── controller/
│       │       ├── dtos/
│       │       ├── entity/
│       │       ├── enums/
│       │       ├── mapper/
│       │       ├── repository/
│       │       └── service/
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-prod.yml
│       │   └── db/changelog/
│       │       ├── changelog-master.xml
│       │       └── changes/NNN-descricao.xml
│       └── test/java/com/nutri/hospitalar/
└── nutri-hospitalar-web/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── components/
        │   ├── common/                 # biblioteca interna T*
        │   └── layout/                 # Layout, Sidebar, Header, TProtected
        ├── contexts/                   # AuthContext, ThemeContext
        ├── hooks/                      # useAuth, useDebounce, ...
        ├── pages/{modulo}/
        ├── routes/
        ├── services/                   # api.ts + um service por módulo
        ├── styles/                     # theme.css (tokens Tailwind v4)
        ├── types/
        └── utils/
```

Módulos no backend: `tenant`, `usuario`, `auth`, `loginlog`, `refreshtoken`
(prontos) · `paciente`, `atendimento`, `antropometria`, `necessidade`,
`prescricao`, `hidratacao`, `pediatria`, `formula`, `suplemento`,
`acompanhamento` (a construir).

---

## 4. Ordem de construção

Fatias verticais. Cada uma vai do banco à tela e é entregável sozinha.
Não começar a próxima com a anterior pela metade.

| # | Fatia | Conteúdo |
|---|---|---|
| **1** ✅ | **Base + acesso** | `pom.xml`, `BaseEntity`/`TenantEntity`, exceptions, `SecurityConfig`, `JwtUtil`, `JwtFilter`, `SecurityUtils`, `RateLimitFilter`, `DataSeeder`, jobs de manutenção, e o módulo completo de **usuários** (Tenant, Usuario com role enum, LoginLog, RefreshToken, Auth com login/logout/refresh/switch-tenant — sem registro público). **Pronto** — ver [02-modulo-usuarios.md](02-modulo-usuarios.md). |
| **2** ✅ | **Casca do front** | Vite + Tailwind com os tokens da marca, tema claro/escuro, `Layout`, `Sidebar`, `TProtected`, `AuthContext`, `api.ts`, biblioteca `T*`, telas de login, início, perfil, usuários, tenants e log de acesso, com o seletor de tenant do superadmin. **Pronta** — ver [04-arquitetura-frontend.md](04-arquitetura-frontend.md). |
| **3** ⬅️ | **Paciente** | Cadastro do paciente (dados pessoais, data de nascimento, sexo, etnia, leito/UTI, data de internação). Padrão Form/List. |
| **4** | **Atendimento** | Um atendimento é a consulta/avaliação numa data. Pertence a um paciente. É o contêiner de tudo que vem a seguir. |
| **5** | **Antropometria** | Estimativas de altura (Chumlea) e peso (Chumlea, Jung, Rabito), peso ideal e ajustado, correção de amputados (Osterkamp), % de perda de peso (Blackburn), adequação de CB por percentil, IMC com classificação OMS e OPAS (idosos), depleção muscular por CB/CP. |
| **6** | **Necessidades + prescrição enteral** | kcal/kg e g/kg por fase da doença crítica; ajuste para obesidade e hemodiálise; cálculo de dieta contínua e intermitente sobre o catálogo de fórmulas; progressão de dieta; módulo proteico; sistema aberto (dieta artesanal); hidratação. |
| **7** | **Pediatria** | Percentis OMS (peso/estatura/IMC por idade e sexo, 0–60 meses), EER por faixa etária, DRI de proteína, prescrição de fórmula láctea. |
| **8** | **Catálogos** | Fórmulas enterais e suplementos — semeados globalmente (`tenant_id NULL`), com possibilidade de o tenant cadastrar os seus. Padrão de catálogo híbrido em [01-multitenant.md](01-multitenant.md). |
| **9** | **Acompanhamento** | Evolução seriada (antropometria por data), controle de ingestão, prescrito × infundido, ficha do paciente, e exportação em PDF. |

As fatias 1 e 2 estão entregues e verificadas contra a API rodando — o resumo do
estado atual fica no [CLAUDE.md](../CLAUDE.md), que é lido em toda sessão. A
próxima é a **3**.

### Bloqueio conhecido das fatias 5–7

**A especificação numérica das fórmulas é um documento à parte, ainda a ser
escrito** a partir das planilhas `Facilita Nutri na UTI - com SA_atualiza (1).xlsx`
e `Pediatria.xlsx`. Ele precisa de, para cada fórmula: origem citada, faixa de
validade das entradas, unidade de cada termo e ao menos um caso de teste numérico
conferido contra a planilha.

Nenhuma fórmula nutricional deve ser implementada de memória ou por inferência.
O sistema prescreve dieta para paciente de UTI: um coeficiente errado não é bug de
software, é erro clínico. As fatias 3 e 4 (paciente e atendimento) não dependem
disso e podem seguir antes.

---

## 5. Regras invioláveis

Estas valem para todo código do projeto, sem exceção e sem discussão:

1. **Isolamento por tenant.** Nenhum usuário vê, altera, cria ou exclui dado de
   outro tenant. Toda consulta filtra por `tenant_id`. Esta regra tem prioridade
   sobre qualquer outra.
2. **O tenant vem do JWT, nunca do cliente.** Idem `usuarioId` e `role`. Nada que
   determine autorização pode vir do body, da query ou de header customizado.
3. **DTO sempre.** Controller não recebe nem devolve Entity. Request e Response são
   `record` com Bean Validation. Conversão só via Mapper.
4. **Paginação sempre.** Nenhum endpoint devolve coleção ilimitada. `size` tem teto.
5. **Liquibase sempre.** Zero alteração manual de banco. Zero `ddl-auto` diferente
   de `none`. Zero changeSet editado depois de executado.
6. **BCrypt sempre.** Senha nunca em texto plano, nunca em MD5/SHA1, nunca em log.
7. **Nenhum dado clínico em log.** Peso, diagnóstico, evolução, prescrição e
   qualquer identificador de paciente ficam fora de log, de mensagem de erro e de
   telemetria. São dados sensíveis de saúde sob a LGPD.
8. **O cálculo mora no backend.** Nenhuma fórmula nutricional é reimplementada em
   TypeScript. O front envia entradas e exibe resultados.
9. **Stack trace nunca chega ao cliente.** `GlobalExceptionHandler` devolve mensagem
   controlada; o detalhe fica no log do servidor.
10. **Secret nenhum no repositório.** Tudo por variável de ambiente.

---

## 6. Definição de pronto

Um módulo do backend só está pronto quando tem:

- [ ] Migration Liquibase com FK, índice de `tenant_id` e uniques compostas nomeadas
- [ ] Entity estendendo `TenantEntity` (ou `BaseEntity` se for global)
- [ ] DTOs `record`: `CreateDto`, `UpdateDto`, `ResponseDto`, `SelectDto` — com Bean Validation
- [ ] Mapper (classe com métodos estáticos)
- [ ] Repository com `findByIdAndTenantId`, `findAllWithFilters(...)`, `findForSelect(...)` — nenhum `findById`/`findAll` exposto
- [ ] Service com `@Transactional`, obtendo o tenant por `securityUtils.getTenantIdLogado()`
- [ ] Controller com `@PreAuthorize` em **todos** os métodos, paginação nas listagens
- [ ] Teste de isolamento: usuário do tenant A não alcança recurso do tenant B
- [ ] Teste de cálculo (se o módulo calcula) com valores conferidos contra a planilha

Uma tela do frontend só está pronta quando tem:

- [ ] Padrão Form/List (entidade principal) ou FormList (auxiliar)
- [ ] Componentes da biblioteca interna `T*` — nenhum input/select/grid novo
- [ ] react-hook-form + schema zod
- [ ] Chamadas via `services/api.ts`, nunca `fetch`
- [ ] Erros tratados com `toast`, nunca `alert`
- [ ] Tipagem completa, zero `any`
- [ ] Funciona em desktop, tablet e mobile
- [ ] Respeita os tokens de cor e tipografia da marca

---

## 7. Documentação de referência

| Doc | Quando ler |
|---|---|
| [01-multitenant.md](01-multitenant.md) | Antes de qualquer entidade, repository ou service |
| [02-modulo-usuarios.md](02-modulo-usuarios.md) | Fatia 1 e sempre que mexer em acesso/permissão |
| [03-arquitetura-backend.md](03-arquitetura-backend.md) | Todo código Java |
| [04-arquitetura-frontend.md](04-arquitetura-frontend.md) | Todo código React |
| [05-identidade-visual.md](05-identidade-visual.md) | Qualquer decisão de cor, fonte ou espaçamento |
| [06-seguranca-owasp.md](06-seguranca-owasp.md) | Antes de abrir endpoint, tratar input ou logar evento |
| [07-banco-liquibase.md](07-banco-liquibase.md) | Toda migration |

---

## 8. Glossário do domínio

Extraído da aba `Siglário` da planilha. Use este vocabulário no código, nos DTOs e
na interface — o domínio já tem nome, não invente outro.

### Antropometria

| Sigla | Significado |
|---|---|
| **CB** | Circunferência do braço |
| **CP** | Circunferência da panturrilha |
| **CA** | Circunferência abdominal |
| **CC** | Circunferência da coxa |
| **AJ** | Altura do joelho |
| **DCT** | Dobra cutânea tricipital |
| **IMC** | Índice de massa corporal |
| **P50** | Percentil 50 |
| **MUAC** | *Mid-upper arm circumference* (equivalente à CB) |
| **Desn** | Desnutrição |

### Nutrição

| Sigla | Significado |
|---|---|
| **VCT / VET** | Valor calórico total / valor energético total |
| **NC** | Necessidades calóricas |
| **NP** | Necessidades proteicas |
| **PTN** | Proteínas |
| **CHO** | Carboidratos |
| **LIP** | Lipídeos |
| **AÇ** | Açúcar |
| **Kcal** | Calorias |
| **NE** | Nutrição enteral |
| **TNE** | Terapia nutricional enteral |
| **Osmol.** | Osmolaridade |
| **Cont / Inter** | Dieta contínua / intermitente |
| **EER** | *Estimated Energy Requirement* (pediatria) |
| **DRI** | *Dietary Reference Intake* (pediatria) |

### Clínica

| Sigla | Significado |
|---|---|
| **BH** | Balanço hídrico |
| **HGT** | Glicemia capilar |
| **VM** | Ventilação mecânica |
| **AA** | Ar ambiente |
| **O2** | Oxigênio |
| **HD** | Hemodiálise |
| **PCR** | Proteína C-reativa |
| **Lact** | Lactato |
| **Int** | Internação |
| **UTI** | Unidade de terapia intensiva |

### Eletrólitos

`Na` sódio · `K` potássio · `P` fósforo · `Mg` magnésio

### Convenção de nomenclatura no código

Siglas viram nomes por extenso em identificadores Java e TypeScript, e a sigla
aparece no rótulo da interface:

```java
private BigDecimal circunferenciaBraco;      // rótulo na tela: "CB (cm)"
private BigDecimal alturaJoelho;             // rótulo na tela: "AJ (cm)"
private BigDecimal valorCaloricoTotal;       // rótulo na tela: "VCT (kcal)"
```

Exceção: siglas universalmente conhecidas e sem ambiguidade — `imc`, `pesoIdeal`,
`kcalPorKg`, `ptnPorKg` — podem ser usadas como estão.

Toda medida antropométrica e todo resultado de cálculo é `BigDecimal`, nunca
`double` ou `float`.

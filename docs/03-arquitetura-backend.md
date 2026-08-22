# Arquitetura Backend — Java 21 + Spring Boot 3.5

> Leia antes de criar qualquer entidade, controller, service, repository, mapper
> ou migration.

---

## 1. Stack

Java 21 · Spring Boot 3.5.16 · Maven · PostgreSQL 16 · Liquibase XML ·
Spring Data JPA / Hibernate 6 · Spring Security · JJWT 0.12.6 ·
Bucket4j 8.19 (`bucket4j_jdk17-core`) · Bean Validation · Lombok ·
SpringDoc OpenAPI 2.8 · Spring Boot Actuator · JUnit 5

**Sem Docker.** PostgreSQL local, variáveis em `.env`, `./run-dev.sh` para subir.
Exige **JDK 21** — o `run-dev.sh` localiza o JDK certo mesmo que o `JAVA_HOME` da
máquina aponte para outra versão.

Não adicionar dependência que não esteja nesta lista sem justificativa escrita no
PR. Em particular: nada de MapStruct (mapper é código explícito), nada de
JasperReports ou Apache POI até existir requisito de relatório/planilha, nada de
Querydsl (JPQL basta), nada de biblioteca de cache distribuído.

---

## 2. Arquitetura modular por domínio

Cada módulo tem suas próprias camadas. Nada de pacotes globais `controllers/`,
`services/`, `entities/`.

```
com.nutri.hospitalar
├── NutriHospitalarApplication.java
├── baseentity/           BaseEntity, TenantEntity
├── config/               SecurityConfig, JwtUtil, JwtProperties, JwtFilter,
│                         SecurityUtils, AuthDetails, RateLimitFilter,
│                         PageableUtils, TransacaoConfig, SwaggerConfig, DataSeeder
├── jobs/                 ManutencaoJob
├── exceptions/           ApplicationException, BadRequestException,
│                         ConflictException, ForbiddenException, NotFoundException,
│                         UnauthorizedException, ErroResponseDto,
│                         GlobalExceptionHandler
└── paciente/
    ├── controller/       PacienteController
    ├── dtos/             PacienteCreateDto, PacienteUpdateDto,
    │                     PacienteResponseDto, PacienteSelectDto
    ├── entity/           Paciente
    ├── enums/            Sexo, Etnia
    ├── mapper/           PacienteMapper
    ├── repository/       PacienteRepository
    └── service/          PacienteService
```

Se uma funcionalidade pertence a um módulo, ela mora nele. `config/` e
`exceptions/` são as únicas exceções — infraestrutura transversal de verdade.

---

## 3. Fluxo obrigatório

```
Controller → Service → Repository → Banco
```

| Camada | Faz | Nunca faz |
|---|---|---|
| **Controller** | recebe HTTP, valida DTO, chama service, devolve response | acessa repository, contém regra de negócio, chama API externa, faz try/catch de negócio |
| **Service** | regra de negócio, validação, controle de tenant, autorização fina, auditoria, cálculo | monta JSON, conhece `HttpServletRequest` (salvo IP/User-Agent para auditoria) |
| **Repository** | consulta e persistência | regra de negócio, validação, chamada externa |

---

## 4. Entidades

Toda entidade de negócio estende `TenantEntity`; entidades globais estendem
`BaseEntity`. Ver [01-multitenant.md](01-multitenant.md) para o código das duas.

```java
@Entity
@Table(name = "paciente")
@Getter @Setter @NoArgsConstructor
public class Paciente extends TenantEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 20)
    private Sexo sexo;

    @Column(name = "peso_atual", precision = 8, scale = 3)
    private BigDecimal pesoAtual;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
```

Regras:

- **Lombok** — `@Getter @Setter @NoArgsConstructor`. Nunca `@Data` (traz `equals`,
  `hashCode` e `toString` que quebram com proxies lazy e vazam dados em log).
- **Enums** — sempre `@Enumerated(EnumType.STRING)`. Nunca `ORDINAL`.
- **`length`** obrigatório em toda `String`.
- **`precision`/`scale`** obrigatórios em todo `BigDecimal`.
- **Relacionamentos** — sempre `FetchType.LAZY`. `EAGER` só com justificativa.
- **Coleções** — nada de `CascadeType.REMOVE` sem intenção explícita.
- **Sem `@PrePersist` em subclasse.** Os callbacks ficam só em `BaseEntity`: a
  JPA invoca os de toda a hierarquia e redeclarar causa execução dupla. Valor
  padrão vai em inicializador de campo.
- **Sem lógica de negócio na entidade**, com uma exceção: métodos de leitura
  derivados e triviais, como `Usuario.estaBloqueado()`.

### Tipos numéricos

| Grandeza | Tipo Java | Coluna |
|---|---|---|
| Medida antropométrica (cm, kg) | `BigDecimal` | `NUMERIC(8,3)` |
| Resultado de cálculo (kcal, g) | `BigDecimal` | `NUMERIC(12,4)` |
| Percentual | `BigDecimal` | `NUMERIC(6,2)` |
| Contagem | `Integer` | `INTEGER` |

**Nunca `double` ou `float`** em nada que seja nutrição ou dosagem. O sistema
prescreve dieta; erro de arredondamento aqui tem consequência clínica.

### Datas

| Uso | Tipo |
|---|---|
| Auditoria, log, timestamps técnicos | **`Instant`** (UTC) |
| Data de nascimento, data do atendimento | `LocalDate` |
| Horário de infusão dentro do dia | `LocalTime` |

Nunca `LocalDateTime` para auditoria, agendamento ou integração — não carrega
fuso e vira ambiguidade na primeira mudança de horário de verão ou servidor.
`jackson.time-zone: UTC` no `application.yml`.

---

## 5. DTOs

Sempre `record`. Sempre com Bean Validation. Controller nunca vê `Entity`.

```
dtos/
├── PacienteCreateDto.java
├── PacienteUpdateDto.java
├── PacienteResponseDto.java
└── PacienteSelectDto.java
```

```java
public record PacienteCreateDto(

        @NotBlank @Size(max = 255)
        String nome,

        @Past
        LocalDate dataNascimento,

        @NotNull
        Sexo sexo,

        @CPF
        String cpf,

        @DecimalMin("0.5") @DecimalMax("500.0") @Digits(integer = 3, fraction = 3)
        BigDecimal pesoAtual
) {}
```

- `CreateDto` e `UpdateDto` são separados, mesmo quando idênticos hoje — divergem
  cedo (o create exige senha, o update não; o create aceita CPF, o update não).
- `ResponseDto` nunca expõe senha, hash, token ou id interno de outro tenant.
- `SelectDto` é o par mínimo `{ id, descricao }` para combos.
- Toda entrada validada. **Nunca confiar no frontend.**

---

## 6. Mappers

Classe final com métodos estáticos e construtor privado. Sem MapStruct — a
conversão explícita é curta, legível e não gera código na build.

```java
public final class PacienteMapper {

    private PacienteMapper() {}

    public static Paciente toEntity(PacienteCreateDto dto) {
        Paciente p = new Paciente();
        p.setNome(dto.nome());
        p.setDataNascimento(dto.dataNascimento());
        p.setSexo(dto.sexo());
        p.setCpf(dto.cpf());
        p.setPesoAtual(dto.pesoAtual());
        return p;                             // tenant é setado no service
    }

    public static void applyUpdate(Paciente p, PacienteUpdateDto dto) {
        p.setNome(dto.nome());
        p.setDataNascimento(dto.dataNascimento());
        p.setSexo(dto.sexo());
        p.setPesoAtual(dto.pesoAtual());
    }

    public static PacienteResponseDto toResponse(Paciente p) {
        return new PacienteResponseDto(p.getId(), p.getNome(), p.getDataNascimento(),
                                       p.getSexo(), p.getCpf(), p.getPesoAtual(),
                                       p.getAtivo(), p.getCreatedAt());
    }

    public static PacienteSelectDto toSelect(Paciente p) {
        return new PacienteSelectDto(p.getId(), p.getNome());
    }
}
```

O mapper **nunca** seta o tenant. Isso é responsabilidade do service, que o obtém
de `SecurityUtils`.

---

## 7. Repositories

Padrão completo em [01-multitenant.md §3](01-multitenant.md). Resumo:

- Todo método recebe `tenantId`
- `findByIdAndTenantId`, `existsByXAndTenantId`, `existsByXAndTenantIdAndIdNot`
- `findAllWithFilters(Pageable, tenantId, ...)` e `findForSelect(tenantId, nome)`

Consulta simples: derivação de nome do Spring Data.
**Listagem com filtro opcional: native query** com `CAST` explícito do PostgreSQL.

O JPQL do Hibernate 6 não tipa um parâmetro nulo solto em `:param IS NULL` e o
Postgres responde *"não foi possível determinar o tipo de dados do parâmetro $N"*.
Com String o sintoma é `função lower(bytea) não existe` — mesma causa. O cast
nativo resolve os dois e libera `unaccent`.

| Java | Cast |
|---|---|
| `String` | `CAST(:p AS text)` |
| `UUID` | `CAST(:p AS uuid)` |
| `Boolean` | `CAST(:p AS boolean)` |
| `Instant` | `CAST(:p AS timestamptz)` |
| enum | passe `.name()` e use `CAST(:p AS text)` |

Toda native paginada precisa de `countQuery`. A ordenação é fixa no SQL e o
service passa `PageableUtils.semOrdenacao(pageable)` — em native o Spring Data
usaria o nome da propriedade como coluna (`dataLogin` não existe, é `data_login`),
e `?sort=` é entrada de usuário que vira SQL (OWASP A03).

### Consultas em coleção — evitar N+1

Listagem de usuário com roles e grupos carrega tudo lazy e dispara N+1.
Usar `@EntityGraph` na listagem:

```java
@EntityGraph(attributePaths = {"roles", "grupos", "grupos.roles"})
Optional<Usuario> findByEmailIgnoreCase(String email);
```

Para páginas, `@EntityGraph` com `@ManyToMany` gera produto cartesiano — nesse
caso, buscar a página de ids primeiro e carregar as coleções numa segunda query.

---

## 8. Services

```java
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final SecurityUtils      securityUtils;
```

- Injeção por construtor (`@RequiredArgsConstructor` + campos `final`).
  **Nunca `@Autowired` em campo.**
- `@Transactional(readOnly = true)` em leitura, `@Transactional` em escrita.
- O tenant vem sempre de `securityUtils.getTenantIdLogado()`.
- Regra de negócio mora aqui, inclusive as fórmulas de cálculo.

### Serviços de cálculo

As fórmulas nutricionais ficam em classes puras, sem estado e sem dependência de
Spring, dentro do módulo:

```java
public final class EstimativaPesoCalculator {

    private EstimativaPesoCalculator() {}

    /**
     * Peso estimado — Rabito et al. (2008).
     * Peso = 0.5759×CB + 0.5263×CA + 1.2452×CP − 4.8689×sexo − 32.9241
     * onde sexo: masculino = 1, feminino = 2.
     */
    public static BigDecimal rabito(BigDecimal cb, BigDecimal ca, BigDecimal cp, Sexo sexo) { ... }
}
```

Toda fórmula carrega: a referência bibliográfica no Javadoc, unidades explícitas
nos parâmetros, `BigDecimal` do início ao fim, `RoundingMode.HALF_UP` com escala
declarada, e um teste com valores conferidos contra a planilha de origem.

Classe pura é trivial de testar e não pode acessar banco por acidente.

---

## 9. Controllers

```java
@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
    public ResponseEntity<Page<PacienteResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) Boolean ativo) {
        return ResponseEntity.ok(pacienteService.getAll(pageable, nome, cpf, ativo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
    public ResponseEntity<PacienteResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
    public ResponseEntity<PacienteResponseDto> create(@Valid @RequestBody PacienteCreateDto dto) {
        PacienteResponseDto criado = pacienteService.create(dto);
        return ResponseEntity.created(URI.create("/pacientes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
    public ResponseEntity<PacienteResponseDto> update(@PathVariable UUID id,
                                                      @Valid @RequestBody PacienteUpdateDto dto) {
        return ResponseEntity.ok(pacienteService.update(id, dto));
    }
}
```

Regras:

- **`@PreAuthorize` em todos os métodos.** Não existe endpoint apenas autenticado
  fora de `/auth/**` e `/usuarios/perfil`.
- Rotas no plural e em kebab-case: `/pacientes`, `/login-logs`, `/formulas-enterais`.
- `@Valid` em todo `@RequestBody`.
- Status: `200` leitura e update, `201` create (com `Location`), `204` delete,
  `404` não encontrado ou de outro tenant, `409` conflito, `422` regra de negócio.
- Sem `try/catch` — o `GlobalExceptionHandler` cuida.

### Paginação obrigatória

Toda listagem devolve `Page<T>` e recebe `Pageable`. Nenhum endpoint devolve
coleção ilimitada. `spring.data.web.pageable.max-page-size: 100` põe teto no
`size` — sem isso, `?size=1000000` é um DoS trivial.

`findForSelect` é a exceção controlada: devolve `List<SelectDto>` porque alimenta
combo, mas filtra por `ativo = true` e tem limite fixo no JPQL.

---

## 10. Tratamento de exceções

```java
public class ApplicationException extends RuntimeException { ... }   // 400
public class BadRequestException  extends ApplicationException { }   // 400
public class UnauthorizedException extends ApplicationException { }  // 401
public class ForbiddenException   extends ApplicationException { }   // 403
public class NotFoundException    extends ApplicationException { }   // 404
public class ConflictException    extends ApplicationException { }   // 409
public class BusinessException    extends ApplicationException { }   // 422
```

```java
public record ErroResponseDto(String erro, int codigo, Instant timestamp, String path) {}
```

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroResponseDto> notFound(NotFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDto> validacao(MethodArgumentNotValidException e,
                                                     HttpServletRequest req) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    /** Rede de segurança — nunca devolve a mensagem original. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDto> inesperado(Exception e, HttpServletRequest req) {
        String ref = UUID.randomUUID().toString();
        log.error("Erro inesperado [{}] em {}", ref, req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                     "Erro interno. Referência: " + ref, req);
    }
}
```

O handler genérico **nunca** devolve `e.getMessage()` nem stack trace. Devolve uma
referência opaca; o detalhe fica no log do servidor. Uma mensagem de exceção crua
vaza nome de tabela, de coluna, caminho de arquivo e versão de biblioteca.

---

## 11. Configuração

`application.yml` — só seleciona o profile.
`application-dev.yml` / `application-prod.yml` — tudo por variável de ambiente.

```yaml
spring:
  datasource:
    url:      ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate.ddl-auto: none          # SEMPRE none — o schema é do Liquibase
    open-in-view: false               # evita lazy loading fora da transação
    properties.hibernate.jdbc.batch_size: 25
  liquibase:
    change-log: classpath:db/changelog/changelog-master.xml
  jackson:
    time-zone: UTC
    default-property-inclusion: non_null
  data.web.pageable:
    default-page-size: 20
    max-page-size: 100

jwt:
  secret:      ${JWT_SECRET}          # ≥ 32 caracteres
  access-ttl:  PT8H
  refresh-ttl: P7D

app:
  cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}    # nunca hard-coded
  superadmin:
    tenant-nome: ${SUPERADMIN_TENANT_NOME:Nutri Hospitalar Admin}
    nome:        ${SUPERADMIN_NOME}
    email:       ${SUPERADMIN_EMAIL}
    senha:       ${SUPERADMIN_SENHA}
  rate-limit:
    login-por-minuto: 5
  auditoria:
    retencao-meses: 12
```

Diferenças em `prod`:

```yaml
spring.jpa.show-sql: false
springdoc.api-docs.enabled: false          # ou protegido por autenticação
management.endpoints.web.exposure.include: health,info
management.endpoint.health.show-details: never
logging.level.root: INFO
```

`open-in-view: false` é importante: com o padrão `true`, uma coleção lazy acessada
no mapper dispara query fora da transação e mascara N+1.

---

## 12. Testes

Rodam contra o **PostgreSQL local** `nutridb_test`, com o Liquibase recriando o
schema a cada execução (`drop-first`). Configuração em
`src/test/resources/application-test.yml`.

Nem H2 nem Testcontainers:

- **H2**, mesmo em modo PostgreSQL, não tem `unaccent`, trata `NULL` em constraint
  única de outro jeito e aceita SQL que o Postgres rejeita — daria falsa confiança
  exatamente onde dói.
- **Testcontainers** exige Docker, que este projeto não usa.

| Tipo | Anotação | O que cobre |
|---|---|---|
| Unitário de cálculo | nenhuma (JUnit puro) | fórmulas nutricionais |
| Integração | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` | rota, status, validação, `@PreAuthorize`, native query |
| Isolamento | idem, estendendo `AbstractIntegrationTest` | tenant A × tenant B |

`AbstractIntegrationTest` monta o cenário canônico — dois tenants completos com
seus administradores, mais o superadmin — e autentica pelo próprio endpoint, de
modo que o token do teste é igual ao de produção. A limpeza entre testes respeita
a ordem de dependência: `refresh_token → login_log → usuario → tenant`.

```bash
./run-dev.sh test
```

Obrigatório por módulo:

- **teste de isolamento** com os quatro casos de [01-multitenant.md §10](01-multitenant.md)
- **teste de cálculo** com valores conferidos contra a planilha de origem, quando o módulo calcula

```java
@Test
void rabito_masculino_conferidoComPlanilha() {
    // Facilita Nutri na UTI, aba "Estimativas Antropométricas", célula B28.
    // Peso = 0,5759·CB + 0,5263·CA + 1,2452·CP − 4,8689·sexo − 32,9241
    BigDecimal peso = EstimativaPesoCalculator.rabito(
            new BigDecimal("25.0"),   // CB cm
            new BigDecimal("90.0"),   // CA cm
            new BigDecimal("34.0"),   // CP cm
            Sexo.MASCULINO);          // sexo = 1
    assertThat(peso).isEqualByComparingTo("66.3083");
}
```

---

## 13. Logs

```java
log.info("Paciente criado id={} tenant={}", paciente.getId(), tenantId);   // CERTO
log.info("Paciente criado: {}", paciente);                                 // ERRADO
```

**Nunca logar:** senha, hash de senha, JWT, refresh token, chave de API, nome de
paciente, CPF, peso, diagnóstico, prescrição, evolução clínica — nada que
identifique ou descreva um paciente.

Log de negócio referencia por **id e tenant**, nunca por conteúdo. Ver
[06-seguranca-owasp.md](06-seguranca-owasp.md) A09.

---

## 14. Dependências

Após adicionar qualquer dependência:

```bash
./mvnw dependency-check:check
```

Vulnerabilidade crítica bloqueia o merge. O plugin roda no CI.

---

## 15. O que nunca fazer

- Repository dentro de Controller
- Regra de negócio em Controller
- `tenantId`, `usuarioId` ou `role` vindos do frontend
- Entity como Request ou Response
- `findAll()` sem paginação
- `findById()` em entidade multi-tenant
- SQL concatenado
- Stack trace ou `e.getMessage()` cru na resposta
- Senha sem BCrypt
- Secret no `application.yml` ou no código
- `@Autowired` em campo
- `@Data` do Lombok em entidade
- `double`/`float` em medida ou dosagem
- `LocalDateTime` em auditoria ou agendamento
- `EnumType.ORDINAL`
- `ddl-auto` diferente de `none`
- `open-in-view: true`
- Alterar banco fora do Liquibase
- Editar changeSet já executado
- Endpoint sem `@PreAuthorize`
- Dado clínico em log

---

## 16. Checklist de PR

- [ ] Módulo com as sete pastas do padrão
- [ ] Entity estende `TenantEntity` (ou `BaseEntity` se global)
- [ ] DTOs `record` com Bean Validation, Create e Update separados
- [ ] Mapper estático, sem setar tenant
- [ ] Repository sem método sem `tenantId`; listagem filtrada em native query com `CAST` explícito e `countQuery`
- [ ] Service com `@Transactional` e tenant vindo de `SecurityUtils`
- [ ] Controller com `@PreAuthorize` em todos os métodos e paginação nas listagens
- [ ] Migration Liquibase com FK, índice e uniques nomeadas
- [ ] Teste de isolamento
- [ ] Teste de cálculo conferido contra a planilha, se aplicável
- [ ] Nenhum dado clínico em log
- [ ] `dependency-check` limpo

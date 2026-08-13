# Multi-Tenant — A Regra Máxima

> Leia antes de criar qualquer entidade, repository, service ou controller.
>
> **Nenhum usuário pode ver, criar, alterar ou excluir dado de outro tenant.**
> Esta regra tem prioridade sobre qualquer outra regra deste projeto. Quando houver
> conflito entre isolamento e qualquer outra coisa — desempenho, elegância, prazo —
> o isolamento ganha.

O sistema guarda dados de saúde. Vazamento entre tenants aqui não é um bug de
software, é um incidente de dados pessoais sensíveis.

---

## 1. Modelo

O tenant é a entidade **`Tenant`**. Um tenant é criado automaticamente no cadastro
de um novo usuário (ver [02-modulo-usuarios.md](02-modulo-usuarios.md)).

```java
@Entity
@Table(name = "tenant")
@Getter @Setter @NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
```

### Classes base

`BaseEntity` — para entidades **globais** ou de escopo próprio (`Tenant`,
`LoginLog`, `RefreshToken`, catálogos do sistema):

```java
@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist void aoInserir()   { this.createdAt = Instant.now(); }
    @PreUpdate  void aoAtualizar() { this.updatedAt = Instant.now(); }
}
```

> Os callbacks de ciclo de vida ficam **apenas** em `BaseEntity`. Subclasse não
> redeclara `@PrePersist`: a JPA invoca os callbacks de toda a hierarquia, e
> redeclarar causa execução dupla. Valor padrão de subclasse vai em inicializador
> de campo (`private Boolean ativo = true;`).

`TenantEntity` — para **toda entidade de negócio**:

```java
@MappedSuperclass
@Getter @Setter
public abstract class TenantEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
```

Toda entidade de negócio estende `TenantEntity`. Sem exceção:
`Usuario`, `Paciente`, `Atendimento`, `AvaliacaoAntropometrica`, `Necessidade`,
`PrescricaoEnteral`, `Acompanhamento`.

Estendem apenas `BaseEntity`: `Tenant` (é a raiz), `LoginLog` (o tenant é
opcional — falha de login com e-mail inexistente não tem tenant) e `RefreshToken`
(escopo vem do usuário).

---

## 2. De onde vem o tenant

**Do JWT. Nunca do cliente.**

O `tenantId` é um claim do token, escrito no login pelo servidor. O `JwtFilter`
o coloca no `Authentication`, e `SecurityUtils` o lê de lá — sem ida ao banco.

```java
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository  tenantRepository;

    /** Tenant efetivo da requisição — lido do claim do JWT. */
    public UUID getTenantIdLogado() {
        return authDetails().tenantId();
    }

    public UUID getUsuarioIdLogado() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Id da sessão em login_log, para fechar no logout. */
    public UUID getSessionIdLogado() {
        return authDetails().sessionId();
    }

    /** true quando um SUPERADMIN está navegando dentro de outro tenant. */
    public boolean isImpersonating() {
        return authDetails().impersonating();
    }

    public Usuario getUsuarioLogado() {
        return usuarioRepository.findById(getUsuarioIdLogado())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
    }

    /** Referência lazy ao tenant, para setar em entidades sem SELECT extra. */
    public Tenant getTenantReference() {
        return tenantRepository.getReferenceById(getTenantIdLogado());
    }

    /** Nível de acesso — para regra de negócio, não para autorização de rota. */
    public Role getRoleLogada() {
        return detalhes().role();
    }

    public boolean isSuperadmin() {
        return Role.SUPERADMIN.equals(getRoleLogada());
    }

    private AuthDetails authDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof AuthDetails d))
            throw new UnauthorizedException("Sessão inválida");
        return d;
    }
}

public record AuthDetails(UUID tenantId, UUID sessionId, Role role, boolean impersonating) {}
```

> **Por que o claim e não uma consulta ao banco.** No EroErp,
> `getClienteIdLogado()` faz `usuarioRepository.findById()` e é chamado 338 vezes
> no código — um SELECT por chamada fora de transação. Com o claim o custo é zero,
> e é ele que torna possível a troca de tenant do superadmin: basta reemitir o
> token com outro `tenantId`.
>
> Contrapartida: mudança de tenant ou desativação de usuário só surte efeito no
> próximo token. Por isso o `JwtFilter` revalida `usuario.ativo` e `tenant.ativo`
> a cada requisição — ver [02-modulo-usuarios.md](02-modulo-usuarios.md).

---

## 3. Repository — o padrão obrigatório

Todo método de repository de entidade multi-tenant recebe `tenantId`.
**Não existe método sem tenant.**

Consultas simples usam derivação de nome do Spring Data. **Listagens com filtro
opcional usam native query** com `CAST` explícito do PostgreSQL — ver o porquê
logo abaixo.

```java
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

    // ─── Leitura por id ────────────────────────────────────────────────
    Optional<Paciente> findByIdAndTenantId(UUID id, UUID tenantId);

    // ─── Unicidade (create e update) ───────────────────────────────────
    boolean existsByCpfAndTenantId(String cpf, UUID tenantId);
    boolean existsByCpfAndTenantIdAndIdNot(String cpf, UUID tenantId, UUID id);

    // ─── Listagem paginada com filtros ─────────────────────────────────
    @Query(value = """
            SELECT p.* FROM paciente p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:cpf   AS text)    IS NULL OR p.cpf LIKE '%' || CAST(:cpf AS text) || '%')
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
            ORDER BY p.nome
            """,
            countQuery = """
            SELECT count(*) FROM paciente p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:cpf   AS text)    IS NULL OR p.cpf LIKE '%' || CAST(:cpf AS text) || '%')
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
            """,
            nativeQuery = true)
    Page<Paciente> findAllWithFilters(Pageable pageable,
                                      @Param("tenantId") UUID tenantId,
                                      @Param("nome") String nome,
                                      @Param("cpf") String cpf,
                                      @Param("ativo") Boolean ativo);

    // ─── Select para combos ────────────────────────────────────────────
    @Query(value = """
            SELECT p.* FROM paciente p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND p.ativo = true
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
            ORDER BY p.nome
            LIMIT 100
            """, nativeQuery = true)
    List<Paciente> findForSelect(@Param("tenantId") UUID tenantId, @Param("nome") String nome);
}
```

### Por que native, e por que o CAST

O JPQL do Hibernate 6 não consegue tipar um parâmetro nulo que aparece solto em
`:param IS NULL`. O PostgreSQL responde:

```
ERRO: não foi possível determinar o tipo de dados do parâmetro $7
```

Com String o sintoma é outro — o parâmetro chega como `bytea` e sai
`função lower(bytea) não existe` — mas a causa é a mesma. O cast nativo resolve os
dois casos de uma vez, e ainda libera `unaccent`, que faz diferença real em
português.

Tabela de casts por tipo:

| Java | Cast |
|---|---|
| `String` | `CAST(:p AS text)` |
| `UUID` | `CAST(:p AS uuid)` |
| `Boolean` | `CAST(:p AS boolean)` |
| `Instant` | `CAST(:p AS timestamptz)` |
| `LocalDate` | `CAST(:p AS date)` |
| `BigDecimal` | `CAST(:p AS numeric)` |
| enum | passe `.name()` e use `CAST(:p AS text)` |

Duas regras que vêm junto:

1. **Toda native paginada precisa de `countQuery`.**
2. **A ordenação é fixa no SQL.** O service passa
   `PageableUtils.semOrdenacao(pageable)`: em native query o Spring Data usaria o
   nome da propriedade como nome de coluna (`dataLogin` não existe, a coluna é
   `data_login`), e `?sort=` é entrada de usuário que vira SQL (OWASP A03).
   Ordenação por coluna, quando alguma tela precisar, entra com allowlist.

---

## 4. Service — o padrão obrigatório

```java
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final SecurityUtils      securityUtils;

    @Transactional(readOnly = true)
    public PacienteResponseDto findById(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return PacienteMapper.toResponse(
                pacienteRepository.findByIdAndTenantId(id, tenantId)
                        .orElseThrow(() -> new NotFoundException("Paciente não encontrado")));
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponseDto> getAll(Pageable pageable, String nome, String cpf, Boolean ativo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pacienteRepository
                .findAllWithFilters(pageable, tenantId, nome, cpf, ativo)
                .map(PacienteMapper::toResponse);
    }

    @Transactional
    public PacienteResponseDto create(PacienteCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        if (dto.cpf() != null && pacienteRepository.existsByCpfAndTenantId(dto.cpf(), tenantId))
            throw new ConflictException("Já existe paciente com esse CPF");

        Paciente paciente = PacienteMapper.toEntity(dto);
        paciente.setTenant(securityUtils.getTenantReference());   // <- do JWT, nunca do DTO

        return PacienteMapper.toResponse(pacienteRepository.save(paciente));
    }

    @Transactional
    public PacienteResponseDto update(UUID id, PacienteUpdateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        Paciente paciente = pacienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));

        PacienteMapper.applyUpdate(paciente, dto);
        return PacienteMapper.toResponse(pacienteRepository.save(paciente));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        Paciente paciente = pacienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));
        pacienteRepository.delete(paciente);
    }
}
```

### 404, nunca 403

Recurso de outro tenant responde **404 Not Found**, com a mesma mensagem de um id
inexistente. Um 403 confirmaria que o id existe em algum lugar — é vazamento de
informação por canal lateral. Por isso o padrão é sempre
`findByIdAndTenantId(...).orElseThrow(NotFoundException::new)`, e nunca
`findById(...)` seguido de comparação de tenant.

---

## 5. Entidades filhas — validar pelo pai

Quando a entidade filha pertence a um agregado (`AvaliacaoAntropometrica` →
`Atendimento` → `Paciente`), **não basta** filtrar a filha por tenant: é preciso
garantir que o pai informado também é do tenant.

```java
@Transactional
public AvaliacaoResponseDto create(UUID atendimentoId, AvaliacaoCreateDto dto) {
    UUID tenantId = securityUtils.getTenantIdLogado();

    // valida o pai dentro do tenant — sem isso, um id de atendimento de outro
    // tenant seria aceito e gravaria um filho órfão de contexto
    Atendimento atendimento = atendimentoRepository.findByIdAndTenantId(atendimentoId, tenantId)
            .orElseThrow(() -> new NotFoundException("Atendimento não encontrado"));

    AvaliacaoAntropometrica avaliacao = AvaliacaoMapper.toEntity(dto);
    avaliacao.setAtendimento(atendimento);
    avaliacao.setTenant(securityUtils.getTenantReference());

    return AvaliacaoMapper.toResponse(avaliacaoRepository.save(avaliacao));
}
```

A filha também carrega `tenant_id` próprio, ainda que redundante com o pai — é o
que permite filtrar direto e o que faz o índice funcionar.

---

## 6. Catálogos híbridos: global + tenant

Fórmulas enterais e suplementos vêm semeados com o sistema (`tenant_id NULL` =
global, visível a todos) e cada tenant pode cadastrar os seus.

Regra: **global é legível por todos, editável por ninguém.**

```java
@Repository
public interface FormulaEnteralRepository extends JpaRepository<FormulaEnteral, UUID> {

    /** LEITURA — enxerga o catálogo global e o do tenant. */
    @Query("""
        SELECT f FROM FormulaEnteral f
        WHERE f.id = :id AND (f.tenant.id = :tenantId OR f.tenant IS NULL)
        """)
    Optional<FormulaEnteral> findByIdVisivel(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /** ESCRITA / EXCLUSÃO — só o que é do próprio tenant. */
    Optional<FormulaEnteral> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        SELECT f FROM FormulaEnteral f
        WHERE (f.tenant.id = :tenantId OR f.tenant IS NULL)
          AND (:nome IS NULL OR LOWER(f.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
          AND (:ativo IS NULL OR f.ativo = :ativo)
        ORDER BY f.nome
        """)
    Page<FormulaEnteral> findAllVisiveis(Pageable pageable,
                                         @Param("tenantId") UUID tenantId,
                                         @Param("nome") String nome,
                                         @Param("ativo") Boolean ativo);
}
```

Nesse caso `tenant_id` é `NULL`-able na tabela, e a unique é
`uk_formula_tenant_nome (tenant_id, nome)` — o PostgreSQL trata `NULL` como
distinto, então o catálogo global e o de cada tenant podem ter nomes iguais.

---

## 7. Superadmin

`SUPERADMIN` é a única role que atravessa tenants. O usuário superadmin pertence a
um tenant raiz semeado pelo `DataSeeder`.

O que ele pode:

- **Listar, ativar e desativar tenants** — `/tenants`, com `@PreAuthorize("hasRole('SUPERADMIN')")`
- **Trocar de tenant** — `POST /auth/switch-tenant/{tenantId}` reemite o JWT com o
  `tenantId` alvo e `impersonating: true`. A partir daí ele usa o sistema
  exatamente como um usuário daquele tenant, passando pelos mesmos filtros de
  `tenant_id`. Não há caminho de código que ignore o filtro.
- **Voltar** — `POST /auth/exit-tenant` reemite o token no tenant raiz.

Isso significa que **não existe query sem filtro de tenant em lugar nenhum**,
nem para o superadmin. O superadmin não fura o filtro; ele muda o valor do filtro.

### As exceções, e por que são fechadas

São três, todas explícitas e anotadas com `@PreAuthorize("hasRole('SUPERADMIN')")`:

| Onde | Por quê |
|---|---|
| `TenantController` | opera sobre a tabela `tenant`, que é global por natureza |
| `LoginLogController#getAllGlobal` | auditoria consolidada — *"quem tentou entrar hoje?"* não se responde de dentro de um tenant |
| `UsuarioController#getAllGlobal` | *"onde está o usuário que acabei de criar?"* — o cliente novo nasce em outro tenant, e a lista do tenant efetivo não o mostra |

Nessas três a **role substitui o filtro**: sem `SUPERADMIN`, não há rota. E há duas
regras que as mantêm exceção, em vez de porta:

1. **`tenantId` ali é filtro, nunca escopo.** Chega como parâmetro *opcional* para
   restringir a um cliente. Em qualquer outro endpoint um `tenantId` do cliente é
   bug (§8); aqui ele só estreita um resultado que a role já autorizou.
2. **Ler global não abre escrever global.** Não existe `PUT`, `POST`, `PATCH` nem
   `DELETE` global. `GET`/`PUT /usuarios/{id}` continuam em `findByIdAndTenantId` e
   devolvem 404 para recurso de outro tenant. Para *editar* fora do seu tenant, o
   superadmin troca de tenant — e a troca fica registrada.

Módulo novo **não** ganha visão global por simetria. Só se existir uma pergunta que
de fato precise ser respondida de fora do tenant — e a resposta precisa carregar
`tenantId`/`tenantNome` em cada linha, senão a lista convida ao erro.

Toda troca de tenant é registrada em `login_log`.

---

## 8. Proibições

Em entidade multi-tenant, **nunca**:

```java
repository.findById(id)              // sem tenant
repository.findAll()                 // sem tenant e sem paginação
repository.findAll(pageable)         // sem tenant
repository.deleteById(id)            // sem tenant
repository.getReferenceById(id)      // sem tenant
repository.existsById(id)            // sem tenant
```

E **nunca** confiar no cliente:

```java
// ERRADO — o tenant veio do DTO
paciente.setTenant(tenantRepository.findById(dto.tenantId()).orElseThrow());

// ERRADO — o usuário veio do DTO
atendimento.setUsuario(usuarioRepository.findById(dto.usuarioId()).orElseThrow());

// ERRADO — a role veio do DTO sem verificação de quem está pedindo
usuario.setRole(dto.role());

// CERTO
paciente.setTenant(securityUtils.getTenantReference());
atendimento.setUsuario(securityUtils.getUsuarioLogado());
```

Um `tenantId` em query param só é aceito em endpoint anotado com
`@PreAuthorize("hasRole('SUPERADMIN')")`. Em qualquer outro lugar é bug.

### Endpoints `select/{id}`

Endpoints de apoio a combo (`/usuarios/select/{id}`, `/pacientes/select/{id}`)
filtram por tenant como qualquer outro. É um caminho fácil de esquecer: o método
parece inofensivo, recebe `@PreAuthorize("isAuthenticated()")` e chama um
`findById` sem tenant — e vira enumeração de registros de todos os tenants.

---

## 9. Banco

Toda tabela de negócio (ver [07-banco-liquibase.md](07-banco-liquibase.md)):

```xml
<column name="tenant_id" type="UUID">
    <constraints nullable="false"
                 foreignKeyName="fk_paciente_tenant"
                 references="tenant(id)"/>
</column>
...
<createIndex tableName="paciente" indexName="idx_paciente_tenant_id">
    <column name="tenant_id"/>
</createIndex>

<addUniqueConstraint tableName="paciente"
                     columnNames="tenant_id, cpf"
                     constraintName="uk_paciente_tenant_cpf"/>
```

Nenhuma unique de campo de negócio é global — sempre composta com `tenant_id`.
Dois tenants podem ter o mesmo paciente e o mesmo e-mail de usuário.

Índices de busca também começam por `tenant_id`:
`idx_paciente_tenant_nome (tenant_id, nome)`.

---

## 10. Testes de isolamento

O isolamento aqui é manual — garantido por disciplina, não por Hibernate `@Filter`
nem por Row-Level Security. **Portanto o teste é a única prova de que funciona**,
e não é opcional.

Todo módulo multi-tenant tem um teste que cobre estes quatro casos:

```java
@SpringBootTest
@AutoConfigureMockMvc
class PacienteTenantIsolationTest {

    // Cenário montado no @BeforeEach:
    //   tenantA + usuarioA + pacienteA
    //   tenantB + usuarioB + pacienteB

    @Test
    void usuarioNaoLeRecursoDeOutroTenant() throws Exception {
        mockMvc.perform(get("/pacientes/{id}", pacienteB.getId()).header(AUTHORIZATION, tokenA))
               .andExpect(status().isNotFound());          // 404, não 403
    }

    @Test
    void listagemNaoTrazRegistroDeOutroTenant() throws Exception {
        mockMvc.perform(get("/pacientes").header(AUTHORIZATION, tokenA))
               .andExpect(jsonPath("$.content[*].id").value(not(hasItem(pacienteB.getId().toString()))));
    }

    @Test
    void updateCrossTenantFalha() throws Exception {
        mockMvc.perform(put("/pacientes/{id}", pacienteB.getId())
                        .header(AUTHORIZATION, tokenA)
                        .contentType(APPLICATION_JSON).content(payloadValido))
               .andExpect(status().isNotFound());
    }

    @Test
    void deleteCrossTenantFalha() throws Exception {
        mockMvc.perform(delete("/pacientes/{id}", pacienteB.getId()).header(AUTHORIZATION, tokenA))
               .andExpect(status().isNotFound());
        assertThat(pacienteRepository.findById(pacienteB.getId())).isPresent();
    }
}
```

Uma classe base `AbstractTenantIsolationTest` monta os dois tenants, os dois
usuários e emite os dois tokens — cada módulo só declara suas rotas.

### Revisão de PR

Todo PR que toca backend responde:

- [ ] Toda entidade nova estende `TenantEntity`?
- [ ] Todo método de repository recebe `tenantId`?
- [ ] Nenhum `findById`/`findAll`/`deleteById` em entidade multi-tenant?
- [ ] O tenant e o usuário vêm de `SecurityUtils`, nunca do DTO?
- [ ] Recurso de outro tenant devolve 404?
- [ ] Entidade filha valida o pai dentro do tenant?
- [ ] `tenantId` em parâmetro só em endpoint `SUPERADMIN`?
- [ ] Migration tem FK, índice e uniques compostas com `tenant_id`?
- [ ] Existe teste de isolamento cobrindo os quatro casos?

# Segurança — OWASP Top 10 (2021) aplicado a Java / Spring Boot

> Aplique em **todo código gerado**. Cada item traz a regra e o exemplo na stack
> real do projeto: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL.

**Contexto que muda o peso das coisas:** este sistema armazena **dados de saúde** —
peso, diagnóstico, prescrição, evolução clínica. Sob a LGPD (art. 5º, II) são
dados pessoais **sensíveis**, com regime mais rígido que dado comum. Um vazamento
aqui é incidente notificável à ANPD, não um bug qualquer.

---

## A01 — Broken Access Control

A categoria nº 1 do OWASP, e a de maior risco neste projeto. Em sistema
multi-tenant, **o controle de acesso é o isolamento por tenant**.

**Regra:** todo recurso verifica se o usuário autenticado pode acessar *aquele
dado específico* — não apenas se está autenticado.

```java
// ❌ ERRADO — só autenticação; qualquer usuário lê o paciente de qualquer tenant
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public PacienteResponseDto findById(@PathVariable UUID id) {
    return PacienteMapper.toResponse(pacienteRepository.findById(id).orElseThrow());
}

// ✅ CERTO — nível de acesso + tenant no mesmo movimento
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
public PacienteResponseDto findById(@PathVariable UUID id) {
    return pacienteService.findById(id);      // findByIdAndTenantId internamente
}
```

**Regras adicionais:**

- Todo endpoint tem `@PreAuthorize`. Exceções: `/auth/login`, `/auth/refresh` e `/usuarios/perfil`.
- Toda consulta filtra por `tenantId` vindo do JWT — ver [01-multitenant.md](01-multitenant.md).
- Recurso de outro tenant devolve **404, nunca 403**. Um 403 confirma que o id
  existe em algum lugar; é vazamento por canal lateral.
- **IDOR:** nunca aceitar `tenantId`, `usuarioId` ou `role` do body/query para
  determinar propriedade. UUID como PK ajuda contra enumeração, mas não substitui
  o filtro.
- **Escalonamento de privilégio:** um `ADMIN` não pode conceder `SUPERADMIN`.
  A verificação vive no service, não só na tela.
- Endpoints `select/{id}` filtram por tenant como qualquer outro — é o caminho
  mais fácil de esquecer.
- **`@PreAuthorize` no controller, filtro de tenant no service.** As duas camadas,
  sempre. Nenhuma cobre a outra.

---

## A02 — Cryptographic Failures

**Regra:** dado sensível protegido em trânsito e em repouso.

```java
// ❌ ERRADO
usuario.setSenha(dto.senha());
usuario.setSenha(DigestUtils.md5Hex(dto.senha()));

// ✅ CERTO
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);       // custo 12
}
usuario.setSenha(passwordEncoder.encode(dto.senha()));
...
if (!passwordEncoder.matches(dto.senha(), usuario.getSenha()))
    throw new UnauthorizedException("E-mail ou senha inválidos");
```

**Regras adicionais:**

- Nunca MD5, SHA1 ou SHA256 puro para senha. BCrypt custo 12.
- Access token com validade **máxima de 8 horas**. Refresh de 7 dias, de uso único
  e com rotação.
- `JWT_SECRET` com **≥ 32 caracteres** aleatórios, sempre por variável de ambiente.
  Nunca no `application.yml`, nunca no código, nunca no repositório.
- HTTPS obrigatório em produção. `server.forward-headers-strategy: framework`
  atrás de proxy, e HSTS ligado.
- Refresh token guardado **hasheado** (SHA-256) na tabela — token em claro no banco
  é senha em claro.
- Erro de senha e usuário inexistente devolvem a **mesma mensagem**. O motivo real
  vai só para o `login_log`.

---

## A03 — Injection

**Regra:** nunca concatenar entrada do usuário em query, comando ou expressão.

```java
// ❌ ERRADO — SQL Injection
@Query(value = "SELECT * FROM paciente WHERE nome = '" + nome + "'", nativeQuery = true)

// ❌ ERRADO — concatenação em JPQL dinâmico
String jpql = "SELECT p FROM Paciente p WHERE p.nome LIKE '%" + nome + "%'";
em.createQuery(jpql, Paciente.class).getResultList();

// ✅ CERTO — native query com parâmetro nomeado e CAST explícito
@Query(value = """
        SELECT p.* FROM paciente p
        WHERE p.tenant_id = CAST(:tenantId AS uuid)
          AND (CAST(:nome AS text) IS NULL
               OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
        ORDER BY p.nome
        """, countQuery = "...", nativeQuery = true)
Page<Paciente> findAllWithFilters(Pageable pageable,
                                  @Param("tenantId") UUID tenantId,
                                  @Param("nome") String nome);
```

**Regras adicionais:**

- Bean Validation em **todo** DTO de entrada. Nada de campo sem restrição.
- `sort` de `Pageable` é entrada do usuário e vira SQL. Hoje o projeto **descarta**
  o `Sort` do cliente com `PageableUtils.semOrdenacao(pageable)` e usa `ORDER BY`
  fixo dentro da native query — sem ordenação dinâmica não há o que sanitizar, e
  `?sort=senha,desc` simplesmente não existe.

  ```java
  public static Pageable semOrdenacao(Pageable pageable) {
      int tamanho = Math.min(Math.max(pageable.getPageSize(), 1), 100);
      return PageRequest.of(pageable.getPageNumber(), tamanho);
  }
  ```

  Quando alguma tela precisar de ordenação por coluna, ela entra com allowlist
  explícita de campos ordenáveis.
- Nada de SpEL montada com entrada do usuário em `@PreAuthorize` ou `@Query`.
- Nada de `Runtime.exec`, `ProcessBuilder` ou expressão dinâmica com dado externo.
- **XSS:** o front é React, que escapa por padrão. Nunca usar
  `dangerouslySetInnerHTML` com conteúdo vindo da API.

---

## A04 — Insecure Design

**Regra:** limites e defesas fazem parte do desenho, não são acréscimo posterior.

```java
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTEGIDAS =
            Set.of("/auth/login", "/auth/refresh");

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (!PROTEGIDAS.contains(req.getRequestURI())) { chain.doFilter(req, res); return; }

        Bucket bucket = buckets.computeIfAbsent(chaveDe(req), k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(1))
                        .build())
                .build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setContentType(APPLICATION_JSON_VALUE);
            res.getWriter().write(erroJson("Muitas tentativas. Aguarde um minuto."));
        }
    }
}
```

**Regras adicionais:**

- Rate limit por IP em login e refresh — as **duas únicas rotas públicas**.
- **Bloqueio por usuário** após 5 falhas consecutivas, por 15 minutos — complementar
  ao rate limit por IP (ver [02-modulo-usuarios.md §6.3](02-modulo-usuarios.md)).
- Paginação obrigatória em toda listagem, com `max-page-size: 100`.
- **Não há cadastro público.** Usuário só nasce por `POST /usuarios`, exclusivo do
  SUPERADMIN. Elimina de saída a superfície de abuso de autosserviço: criação de
  conta em massa, enumeração de e-mail no cadastro e tenant-órfão de bot.
- Nenhum segredo ou token OTP volta na resposta da API.
- Limite de tamanho de upload e de corpo de requisição
  (`spring.servlet.multipart.max-file-size`).

---

## A05 — Security Misconfiguration

**Regra:** cabeçalhos de segurança ligados, superfície reduzida, nada de detalhe
técnico vazando.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)                  // API stateless com JWT
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true).maxAgeInSeconds(31_536_000))
                .frameOptions(FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; frame-ancestors 'none'; object-src 'none'")))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**", "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(customEntryPoint)          // 401 JSON
                .accessDeniedHandler(customAccessDeniedHandler))     // 403 JSON
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**CORS por variável de ambiente, nunca hard-coded:**

```java
// ❌ ERRADO
config.setAllowedOrigins(List.of("http://localhost:5173"));
config.setAllowedOrigins(List.of("*"));                       // com credentials, nem funciona

// ✅ CERTO
@Value("${app.cors.allowed-origins}")
private List<String> allowedOrigins;

CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(allowedOrigins);
config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
config.setAllowedHeaders(List.of("Authorization","Content-Type"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);
```

**Actuator e Swagger em produção:**

```yaml
# application-prod.yml
management:
  endpoints.web.exposure.include: health,info      # NUNCA "*"
  endpoint.health.show-details: never
springdoc:
  api-docs.enabled: false                          # ou protegido por autenticação
  swagger-ui.enabled: false
spring:
  jpa.show-sql: false
server:
  error.include-stacktrace: never
  error.include-message: never
```

`management.endpoints.web.exposure.include: "*"` expõe `/actuator/env`, que
devolve as variáveis de ambiente — inclusive `JWT_SECRET` e `DB_PASSWORD`.

**Stack trace nunca chega ao cliente:**

```java
// ❌ ERRADO — vaza nome de tabela, coluna, caminho e versão de biblioteca
@ExceptionHandler(Exception.class)
public ResponseEntity<ErroResponseDto> erro(Exception e) {
    return ResponseEntity.status(500).body(new ErroResponseDto(e.getMessage(), 500, now(), path));
}

// ✅ CERTO — referência opaca; o detalhe fica no log do servidor
@ExceptionHandler(Exception.class)
public ResponseEntity<ErroResponseDto> erro(Exception e, HttpServletRequest req) {
    String ref = UUID.randomUUID().toString();
    log.error("Erro inesperado [{}] em {}", ref, req.getRequestURI(), e);
    return ResponseEntity.status(500)
            .body(new ErroResponseDto("Erro interno. Referência: " + ref, 500, Instant.now(), req.getRequestURI()));
}
```

---

## A06 — Vulnerable and Outdated Components

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>10.0.4</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>      <!-- falha em High e Critical -->
    <suppressionFiles>
      <suppressionFile>owasp-suppressions.xml</suppressionFile>
    </suppressionFiles>
  </configuration>
</plugin>
```

```bash
./mvnw dependency-check:check        # backend
npm audit --audit-level=high         # frontend
```

**Regras adicionais:**

- Rodar no CI; vulnerabilidade crítica bloqueia o merge.
- Toda supressão em `owasp-suppressions.xml` precisa de comentário justificando e
  de data de revisão.
- Revisar `pom.xml` e `package.json` antes de aceitar código gerado por IA — é
  onde entra dependência inventada ou abandonada.
- Manter Spring Boot na linha suportada. Boot 3.5 traz correções de segurança que
  não são retroportadas indefinidamente.

---

## A07 — Identification and Authentication Failures

```java
// ✅ Bloqueio progressivo por usuário
private void registrarTentativaInvalida(Usuario usuario) {
    int tentativas = usuario.getTentativasFalhas() + 1;
    usuario.setTentativasFalhas(tentativas);
    if (tentativas >= 5)
        usuario.setBloqueadoAte(Instant.now().plus(Duration.ofMinutes(15)));
    usuarioRepository.save(usuario);
}

// ✅ Mesma mensagem para usuário inexistente e senha errada
throw new UnauthorizedException("E-mail ou senha inválidos");
```

**Regras adicionais:**

- Senha com mínimo de **10 caracteres** (`@Size(min = 10, max = 72)` — 72 é o teto
  do BCrypt) e validada contra uma lista de senhas comuns.
- Troca de senha exige a senha atual.
- `JwtFilter` revalida `usuario.ativo` e `tenant.ativo` **a cada requisição** — sem
  isso, um token válido continua funcionando depois de o usuário ser desativado.
- Logout fecha a sessão em `login_log` e revoga os refresh tokens.
- Refresh token de uso único, com rotação. Reapresentar um token já revogado
  revoga toda a cadeia do usuário e gera evento de auditoria — é sinal de roubo.
- Nada de `HttpSession` ou sessão em memória. A API é stateless.
- Recuperação de senha — **ainda não implementada**, depende de definir o serviço
  de e-mail. Quando entrar: token aleatório de ≥ 32 bytes, validade de 30 minutos,
  uso único, e resposta sempre igual exista ou não o e-mail.

---

## A08 — Software and Data Integrity Failures

**Regra:** verificar a integridade de tudo que vem de fora do sistema.

```java
// ❌ ERRADO — deserialização polimórfica insegura
mapper.enableDefaultTyping();
mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance);

// ✅ CERTO — Jackson padrão, tipos explícitos, campo desconhecido rejeitado
spring.jackson.deserialization.fail-on-unknown-properties: true
```

**Regras adicionais:**

- Nunca `ObjectInputStream` com dado externo.
- Payload de qualquer integração futura validado por schema antes de processar.
- Webhook, se houver, com assinatura HMAC validada em tempo constante:

  ```java
  boolean valido = MessageDigest.isEqual(
          assinaturaRecebida.getBytes(UTF_8), hmacEsperado.getBytes(UTF_8));
  ```
  `MessageDigest.isEqual`, não `String.equals` — comparação em tempo constante.
- Upload de arquivo (foto, laudo): validar extensão, MIME real (magic bytes, não
  o header enviado), tamanho máximo, e gravar com nome gerado pelo servidor, fora
  do diretório servido estaticamente.
- Build reprodutível: versões fixas no `pom.xml`, sem intervalo de versão; lockfile
  do npm commitado.

---

## A09 — Security Logging and Monitoring Failures

**Sempre registrar:**

| Evento | Onde |
|---|---|
| Login com sucesso | `login_log` |
| Login com falha, com motivo | `login_log` |
| Bloqueio por tentativas | `login_log` + log da aplicação |
| Logout (manual e por expiração) | `login_log` |
| Troca de tenant do superadmin | `login_log`, com `impersonado_por` |
| Criação, alteração e exclusão de registro | `audit_log` |
| Alteração do nível de acesso de um usuário | `audit_log` (crítico) |
| Acesso negado (403) | log da aplicação, com usuário, tenant e rota |
| Refresh token revogado reapresentado | `audit_log` + alerta |

```java
public record AuditEvent(UUID tenantId, UUID usuarioId, String entidade,
                         UUID registroId, AcaoAuditoria acao, Instant momento) {}
```

**Nunca registrar:**

```java
// ❌ ERRADO
log.info("Login: {} / {}", email, senha);
log.debug("Token gerado: {}", token);
log.info("Paciente salvo: {}", paciente);            // toString com dado clínico

// ✅ CERTO
log.info("Login bem-sucedido usuarioId={} tenantId={}", usuario.getId(), tenantId);
log.info("Paciente criado id={} tenantId={}", paciente.getId(), tenantId);
```

Fora da lista, sem exceção: senha, hash de senha, JWT, refresh token, chave de API,
nome de paciente, CPF, peso, diagnóstico, prescrição, evolução clínica.
**Log de negócio referencia por id e tenant, nunca por conteúdo.**

Por isso as entidades usam `@Getter @Setter`, nunca `@Data` — o `toString` gerado
pelo `@Data` derrama a entidade inteira no primeiro log descuidado.

**Retenção:** `login_log` e `audit_log` por 12 meses, com job mensal de expurgo
(`app.auditoria.retencao-meses`). Guardar dado de saúde indefinidamente sem
finalidade é problema de LGPD, não zelo.

---

## A10 — Server-Side Request Forgery (SSRF)

Hoje o sistema não faz chamada HTTP de saída. Quando fizer (envio de e-mail via
API, integração com prontuário), a regra é allowlist:

```java
private static final Set<String> HOSTS_PERMITIDOS = Set.of("api.provedor-email.com");

public static boolean urlSegura(String url) {
    try {
        URI uri = URI.create(url);
        return List.of("https").contains(uri.getScheme())
            && HOSTS_PERMITIDOS.contains(uri.getHost());
    } catch (IllegalArgumentException e) {
        return false;
    }
}
```

**Regras adicionais:**

- Nunca requisitar URL fornecida pelo usuário sem allowlist.
- Bloquear IP privado e loopback (`127.0.0.0/8`, `10/8`, `172.16/12`, `192.168/16`,
  `169.254.169.254`) — o metadata endpoint da nuvem é o alvo clássico.
- Não seguir redirecionamento automaticamente em chamada de saída.
- Timeout de conexão e de leitura em todo cliente HTTP.

---

## LGPD — o que este projeto assume

Dados de saúde são dados pessoais sensíveis (LGPD, art. 5º, II).

- **Finalidade e minimização:** só coletar o que a avaliação nutricional exige.
- **Isolamento:** cada tenant é um controlador distinto; o isolamento não é só
  técnico, é a fronteira jurídica entre controladores.
- **Titularidade:** prever exportação dos dados do paciente e exclusão a pedido.
- **Rastreabilidade:** `audit_log` responde quem acessou o quê e quando.
- **Retenção:** log de auditoria por 12 meses; dado de paciente pelo prazo que a
  regulamentação do prontuário exigir, definido com a cliente.
- **Incidente:** vazamento entre tenants é notificável à ANPD. É por isso que o
  teste de isolamento não é opcional.

---

## Checklist de PR

Antes de aprovar qualquer PR, especialmente com código gerado por IA:

- [ ] **A01** — `@PreAuthorize` em todos os endpoints; toda query filtra por tenant; 404 (não 403) para recurso de outro tenant; nenhum `tenantId`/`usuarioId`/`role` vindo do cliente
- [ ] **A02** — senha em BCrypt(12); token ≤ 8h; secret por env var; refresh hasheado no banco
- [ ] **A03** — zero concatenação em query; Bean Validation em todo DTO; `sort` com allowlist; `CAST(:param AS string)` onde couber
- [ ] **A04** — rate limit nas rotas de auth; bloqueio por tentativas; paginação com teto
- [ ] **A05** — headers de segurança; CORS por env var; Actuator só health/info; Swagger fechado em prod; nenhum stack trace na resposta
- [ ] **A06** — `dependency-check` e `npm audit` limpos
- [ ] **A07** — mensagem de erro de login uniforme; `usuario.ativo` e `tenant.ativo` revalidados por request; logout revoga sessão
- [ ] **A08** — payload externo validado; upload com MIME real verificado
- [ ] **A09** — evento de segurança registrado; **nenhum dado clínico, senha ou token em log**
- [ ] **A10** — URL externa em allowlist; timeout configurado
- [ ] **Isolamento** — teste automatizado provando que o tenant A não alcança o tenant B

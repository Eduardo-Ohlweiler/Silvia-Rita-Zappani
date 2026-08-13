package com.nutri.hospitalar.auth;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.usuario.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticação")
class AuthTest extends AbstractIntegrationTest {

    // ─────────────────────────────────────────────────────────────────
    //  Superfície pública
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("não existe rota pública de registro e nenhum tenant nasce por ela")
    void naoExisteRotaPublicaDeRegistro() throws Exception {
        long tenantsAntes = tenantRepository.count();
        long usuariosAntes = usuarioRepository.count();

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"nomeTenant":"Consultorio Pirata","nome":"Maria",
                                 "email":"maria@pirata.local","senha":"SenhaForte123"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(tenantRepository.count()).isEqualTo(tenantsAntes);
        assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
        assertThat(usuarioRepository.findByEmailIgnoreCase("maria@pirata.local")).isEmpty();
    }

    @Test
    @DisplayName("rota inexistente devolve 404, não 500")
    void rotaInexistenteDevolve404() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("método errado devolve 405, não 500")
    void metodoErradoDevolve405() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Login
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("usuário inexistente e senha errada devolvem a MESMA mensagem")
    void mensagemDeFalhaNaoRevelaSeOEmailExiste() throws Exception {
        String comEmailReal = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"senhaErrada123"}
                                """.formatted(adminA.getEmail())))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String comEmailFalso = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"nao.existe@teste.local","senha":"senhaErrada123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(comEmailReal).get("erro").asText())
                .isEqualTo(objectMapper.readTree(comEmailFalso).get("erro").asText());
    }

    @Test
    @DisplayName("usuário inativo não entra")
    void usuarioInativoNaoEntra() throws Exception {
        adminA.setAtivo(false);
        usuarioRepository.save(adminA);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tenant inativo bloqueia todos os seus usuários")
    void tenantInativoBloqueiaOsUsuarios() throws Exception {
        tenantA.setAtivo(false);
        tenantRepository.save(tenantA);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Bloqueio por tentativas — OWASP A07
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cinco senhas erradas bloqueiam a conta, e a senha certa deixa de valer")
    void bloqueiaAposCincoTentativas() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType("application/json")
                            .content("""
                                    {"email":"%s","senha":"senhaErrada123"}
                                    """.formatted(adminA.getEmail())))
                    .andExpect(status().isUnauthorized());
        }

        Usuario apos = usuarioRepository.findById(adminA.getId()).orElseThrow();
        assertThat(apos.getTentativasFalhas()).isEqualTo(5);
        assertThat(apos.estaBloqueado())
                .as("o contador precisa sobreviver ao rollback do login que falhou")
                .isTrue();

        // Mesmo com a senha correta, a conta está bloqueada
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login válido zera o contador de tentativas")
    void loginValidoZeraOContador() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"senhaErrada123"}
                                """.formatted(adminA.getEmail())))
                .andExpect(status().isUnauthorized());

        assertThat(usuarioRepository.findById(adminA.getId()).orElseThrow()
                .getTentativasFalhas()).isEqualTo(1);

        autenticar(adminA.getEmail());

        assertThat(usuarioRepository.findById(adminA.getId()).orElseThrow()
                .getTentativasFalhas()).isZero();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Refresh token
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh rotaciona o par e invalida o token usado")
    void refreshRotacionaEInvalidaOAnterior() throws Exception {
        String primeiro = refreshTokenDeLogin(adminA.getEmail());
        String segundo = objectMapper.readTree(chamarRefresh(primeiro, 200))
                .get("refreshToken").asText();

        assertThat(segundo).isNotEqualTo(primeiro);
        chamarRefresh(primeiro, 401);
    }

    @Test
    @DisplayName("reapresentar token já usado derruba a cadeia inteira")
    void reusoDerrubaACadeia() throws Exception {
        String primeiro = refreshTokenDeLogin(adminA.getEmail());
        String segundo = objectMapper.readTree(chamarRefresh(primeiro, 200))
                .get("refreshToken").asText();

        chamarRefresh(primeiro, 401);       // reuso detectado

        // O token novo também precisa cair — a revogação não pode ser desfeita
        // pelo rollback da exceção lançada acima.
        chamarRefresh(segundo, 401);
    }

    @Test
    @DisplayName("logout revoga os refresh tokens do usuário")
    void logoutRevogaOsRefreshTokens() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(adminA.getEmail(), SENHA_PADRAO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String access = "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();
        String refresh = objectMapper.readTree(corpo).get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout").header(AUTHORIZATION, access))
                .andExpect(status().isNoContent());

        chamarRefresh(refresh, 401);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Troca de tenant
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN não consegue trocar de tenant")
    void adminNaoTrocaDeTenant() throws Exception {
        mockMvc.perform(post("/auth/switch-tenant/{id}", tenantB.getId())
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER não consegue trocar de tenant")
    void userNaoTrocaDeTenant() throws Exception {
        mockMvc.perform(post("/auth/switch-tenant/{id}", tenantB.getId())
                        .header(AUTHORIZATION, autenticar(userA.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("superadmin troca de tenant e volta")
    void superadminTrocaEVolta() throws Exception {
        String raiz = autenticar(superadmin.getEmail());

        String noTenantB = mockMvc.perform(post("/auth/switch-tenant/{id}", tenantB.getId())
                        .header(AUTHORIZATION, raiz))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantB.getId().toString()))
                .andExpect(jsonPath("$.impersonating").value(true))
                .andReturn().getResponse().getContentAsString();

        String tokenB = "Bearer " + objectMapper.readTree(noTenantB).get("accessToken").asText();

        mockMvc.perform(post("/auth/exit-tenant").header(AUTHORIZATION, tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(superadmin.getTenant().getId().toString()))
                .andExpect(jsonPath("$.impersonating").value(false));
    }

    @Test
    @DisplayName("não é possível entrar em tenant inativo")
    void naoEntraEmTenantInativo() throws Exception {
        tenantB.setAtivo(false);
        tenantRepository.save(tenantB);

        mockMvc.perform(post("/auth/switch-tenant/{id}", tenantB.getId())
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail())))
                .andExpect(status().isConflict());
    }

    // ─────────────────────────────────────────────────────────────────

    private String refreshTokenDeLogin(String email) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA_PADRAO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("refreshToken").asText();
    }

    private String chamarRefresh(String refreshToken, int statusEsperado) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
    }
}

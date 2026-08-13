package com.nutri.hospitalar.usuario;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.usuario.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regra de acesso do sistema: a <b>área administrativa</b> — tenants, usuários
 * e log de acesso — é exclusiva do SUPERADMIN. ADMIN e USER acessam o resto e
 * o próprio perfil.
 */
@DisplayName("Autorização por nível de acesso")
class AutorizacaoTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("ADMIN e USER não alcançam a área administrativa")
    void areaAdministrativaEExclusivaDoSuperadmin() throws Exception {
        for (String token : new String[]{autenticar(adminA.getEmail()), autenticar(userA.getEmail())}) {
            mockMvc.perform(get("/usuarios").header(AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/tenants").header(AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/login-logs").header(AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("qualquer autenticado lê e edita o próprio perfil")
    void perfilProprioEAcessivelATodos() throws Exception {
        String token = autenticar(userA.getEmail());

        mockMvc.perform(get("/usuarios/perfil").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userA.getEmail()))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(put("/usuarios/perfil")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Nome Novo","telefone":"51999998888"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    @DisplayName("o perfil nunca devolve a senha")
    void perfilNaoExpoeSenha() throws Exception {
        String corpo = mockMvc.perform(get("/usuarios/perfil")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("senha").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("sem token, a resposta é 401")
    void semTokenE401() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/usuarios/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token adulterado é rejeitado")
    void tokenAdulteradoERejeitado() throws Exception {
        String token = autenticar(adminA.getEmail());
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/usuarios/perfil").header(AUTHORIZATION, adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("usuário desativado depois do login perde o acesso na hora")
    void desativacaoDerrubaTokenEmUso() throws Exception {
        String token = autenticar(adminA.getEmail());

        mockMvc.perform(get("/usuarios/perfil").header(AUTHORIZATION, token))
                .andExpect(status().isOk());

        adminA.setAtivo(false);
        usuarioRepository.save(adminA);

        mockMvc.perform(get("/usuarios/perfil").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tenant desativado depois do login derruba o token em uso")
    void tenantInativoDerrubaTokenEmUso() throws Exception {
        String token = autenticar(adminA.getEmail());

        tenantA.setAtivo(false);
        tenantRepository.save(tenantA);

        mockMvc.perform(get("/usuarios/perfil").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("superadmin provisiona usuário no tenant em que está")
    void superadminProvisionaUsuarioNoTenantEfetivo() throws Exception {
        String raiz = autenticar(superadmin.getEmail());
        String corpo = mockMvc.perform(post("/auth/switch-tenant/{id}", tenantA.getId())
                        .header(AUTHORIZATION, raiz))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String noTenantA = "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, noTenantA)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Ana","email":"ana@clinicaA.local",
                                 "senha":"SenhaDaAna123","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(usuarioRepository.findByEmailIgnoreCase("ana@clinicaa.local"))
                .isPresent()
                .get()
                .satisfies(u -> {
                    assertThat(u.getTenant().getId()).isEqualTo(tenantA.getId());
                    assertThat(u.getRole()).isEqualTo(Role.USER);
                    assertThat(u.getSenha()).startsWith("$2a$");     // BCrypt, nunca em claro
                });
    }

    @Test
    @DisplayName("e-mail duplicado no mesmo tenant é rejeitado")
    void emailDuplicadoNoTenantERejeitado() throws Exception {
        String raiz = autenticar(superadmin.getEmail());
        String corpo = mockMvc.perform(post("/auth/switch-tenant/{id}", tenantA.getId())
                        .header(AUTHORIZATION, raiz))
                .andReturn().getResponse().getContentAsString();
        String noTenantA = "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();

        mockMvc.perform(post("/usuarios")
                        .header(AUTHORIZATION, noTenantA)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Clone","email":"%s","senha":"SenhaClone123","role":"USER"}
                                """.formatted(adminA.getEmail())))
                .andExpect(status().isConflict());
    }
}

package com.nutri.hospitalar.usuario;

import com.nutri.hospitalar.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A prova de que o isolamento por tenant funciona.
 *
 * <p>O isolamento aqui é manual — garantido por {@code findByIdAndTenantId} e
 * disciplina, não por Hibernate {@code @Filter} nem Row-Level Security. Este
 * teste é, portanto, a <b>única</b> garantia automatizada de que ele vale.
 * Todo módulo multi-tenant novo precisa do seu equivalente.
 */
@DisplayName("Isolamento por tenant")
class TenantIsolationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("superadmin dentro do tenant A não lê usuário do tenant B — 404, não 403")
    void naoLeRecursoDeOutroTenant() throws Exception {
        String token = tokenNoTenantA();

        mockMvc.perform(get("/usuarios/{id}", adminB.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listagem do tenant A não traz nenhum registro do tenant B")
    void listagemNaoVazaEntreTenants() throws Exception {
        String token = tokenNoTenantA();

        mockMvc.perform(get("/usuarios").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))          // adminA e userA
                .andExpect(jsonPath("$.content[*].tenantId",
                        everyItem(is(tenantA.getId().toString()))));
    }

    @Test
    @DisplayName("update cross-tenant falha e não altera o registro")
    void updateCrossTenantFalha() throws Exception {
        String token = tokenNoTenantA();

        mockMvc.perform(put("/usuarios/{id}", adminB.getId())
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Invadido","email":"invadido@teste.local","role":"USER"}
                                """))
                .andExpect(status().isNotFound());

        assertThat(usuarioRepository.findById(adminB.getId()).orElseThrow().getNome())
                .isEqualTo(adminB.getNome());
    }

    @Test
    @DisplayName("desativação cross-tenant falha e o usuário segue ativo")
    void desativacaoCrossTenantFalha() throws Exception {
        String token = tokenNoTenantA();

        mockMvc.perform(patch("/usuarios/{id}/ativo", adminB.getId())
                        .header(AUTHORIZATION, token)
                        .param("ativo", "false"))
                .andExpect(status().isNotFound());

        assertThat(usuarioRepository.findById(adminB.getId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("o log de acesso do tenant A não mostra login do tenant B")
    void logDeAcessoNaoVazaEntreTenants() throws Exception {
        autenticar(adminB.getEmail());          // gera registro no tenant B
        String token = tokenNoTenantA();

        mockMvc.perform(get("/login-logs").header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].tenantId",
                        everyItem(is(tenantA.getId().toString()))));
    }

    /** Superadmin entra no tenant A — é assim que ele opera dentro de um tenant. */
    private String tokenNoTenantA() throws Exception {
        String tokenRaiz = autenticar(superadmin.getEmail());

        String corpo = mockMvc.perform(post("/auth/switch-tenant/{id}", tenantA.getId())
                        .header(AUTHORIZATION, tokenRaiz))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impersonating").value(true))
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + objectMapper.readTree(corpo).get("accessToken").asText();
    }
}

package com.nutri.hospitalar.configuracao;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.configuracao.enums.PaletaSistema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paleta do sistema: lida por qualquer um — inclusive sem login, porque a tela
 * de entrada já abre no tema — e escrita só pelo SUPERADMIN. Vale para todos
 * os tenants de uma vez.
 */
@DisplayName("Configurações gerais — paleta do sistema")
class ConfiguracaoSistemaTest extends AbstractIntegrationTest {

    private static final String ROTA = "/configuracoes/aparencia";

    private String corpo(String paleta) {
        return """
                {"paleta":"%s"}
                """.formatted(paleta);
    }

    @Test
    @DisplayName("sem login, a leitura devolve a paleta padrão")
    void leituraPublicaDevolvePadrao() throws Exception {
        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paleta").value("PADRAO"));
    }

    @Test
    @DisplayName("a leitura pública expõe só o nome da paleta")
    void leituraPublicaNaoExpoeMaisNada() throws Exception {
        String resposta = mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(resposta).size()).isEqualTo(1);
    }

    @Test
    @DisplayName("o superadmin troca a paleta, e ela vale para todos os tenants")
    void superadminTrocaParaTodos() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header(AUTHORIZATION, autenticar(superadmin.getEmail()))
                        .contentType("application/json")
                        .content(corpo("JADE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paleta").value("JADE"));

        // Sem login, e de dentro de um tenant qualquer: a mesma resposta.
        mockMvc.perform(get(ROTA))
                .andExpect(jsonPath("$.paleta").value("JADE"));
        mockMvc.perform(get(ROTA).header(AUTHORIZATION, autenticar(adminB.getEmail())))
                .andExpect(jsonPath("$.paleta").value("JADE"));

        assertThat(configuracaoSistemaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("ADMIN e USER não trocam a paleta")
    void adminEUserNaoEscrevem() throws Exception {
        for (String token : new String[]{autenticar(adminA.getEmail()), autenticar(userA.getEmail())}) {
            mockMvc.perform(put(ROTA)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpo("MALVA")))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get(ROTA)).andExpect(jsonPath("$.paleta").value("PADRAO"));
    }

    @Test
    @DisplayName("sem token, a escrita é 401 — a liberação pública é só do GET")
    void escritaSemTokenE401() throws Exception {
        mockMvc.perform(put(ROTA)
                        .contentType("application/json")
                        .content(corpo("MALVA")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("paleta ausente ou inexistente é 400")
    void paletaInvalidaE400() throws Exception {
        String token = autenticar(superadmin.getEmail());

        mockMvc.perform(put(ROTA)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(ROTA)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(corpo("TERRACOTA")))
                .andExpect(status().isBadRequest());
    }

    /**
     * O enum e o {@code CHECK} das migrations 033/034 são duas listas da mesma coisa.
     * Uma paleta nova no enum e esquecida no banco passaria em todo o resto e
     * explodiria em 409 só na tela do superadmin, no dia em que ele a escolhesse.
     */
    @Test
    @DisplayName("o banco aceita toda paleta que o enum declara")
    void bancoAceitaTodoValorDoEnum() throws Exception {
        String token = autenticar(superadmin.getEmail());

        for (PaletaSistema paleta : PaletaSistema.values()) {
            mockMvc.perform(put(ROTA)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpo(paleta.name())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paleta").value(paleta.name()));
        }
    }
}

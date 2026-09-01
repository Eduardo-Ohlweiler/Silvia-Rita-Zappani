package com.nutri.hospitalar.pediatria;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo de fórmulas lácteas — carga da migration, o que o cadastro recusa e
 * a lista paginada.
 *
 * <p>É o equivalente pediátrico de {@code CatalogoUtiTest}. A assimetria que ele
 * fecha era real: a fórmula enteral recusava composição implausível em sete
 * situações, e a láctea aceitava qualquer número — num campo que prescreve
 * mamadeira.
 *
 * <p>O isolamento por tenant já é coberto por {@code PediatriaTenantIsolationTest};
 * aqui só entra o que é novo.
 */
@DisplayName("Catálogo de fórmulas lácteas")
class CatalogoPediatriaTest extends AbstractIntegrationTest {

    @Nested
    @DisplayName("Carga da migration 016")
    class Carga {

        @Test
        @DisplayName("as 10 fórmulas da planilha entram, e todas são globais")
        void dezGlobais() {
            var todas = formulaLacteaRepository.findAll();
            assertThat(todas).hasSize(10);
            assertThat(todas).allMatch(FormulaLactea::ehGlobal);
            assertThat(todas).extracting(FormulaLactea::getNome)
                    .contains("NAN 1", "NAN 2", "NEOCATE ADV", "FORTINI", "NESTOGENO 1");
        }

        /**
         * O teste que impede a guarda de nascer errada.
         *
         * <p>Se alguém apertar a faixa de {@code FormulaLacteaService}, o próprio
         * catálogo do sistema reprova aqui — antes de reprovar o cadastro da
         * nutricionista. Foi uma faixa apertada demais que a pesquisa derrubou:
         * a de fórmula infantil do Codex (60 a 70 kcal/100 ml) recusaria o
         * FORTINI, o INFATRINE e o NEOCATE ADV, que são do próprio seed.
         */
        @Test
        @DisplayName("as 10 passam pelas guardas de plausibilidade do cadastro")
        void seedPassaPelasGuardas() {
            for (FormulaLactea f : formulaLacteaRepository.findAll()) {
                BigDecimal kcal = f.getKcalPor100ml();
                BigDecimal ptn = f.getProteinaPor100ml();

                assertThat(kcal).as("densidade de %s", f.getNome())
                        .isBetween(new BigDecimal("20"), new BigDecimal("250"));

                BigDecimal razao = ptn.multiply(new BigDecimal("100"), MathContext.DECIMAL64)
                        .divide(kcal, MathContext.DECIMAL64);

                assertThat(razao).as("proteína por 100 kcal de %s", f.getNome())
                        .isBetween(new BigDecimal("1.0"), new BigDecimal("6.0"));
            }
        }

        /**
         * A faixa do Codex CXS 72-1981 é 1,8 a 3,0 g/100 kcal para fórmula
         * infantil, e as dez do seed cabem nela — inclusive os hipercalóricos.
         * É o que justifica a razão ser a régua, e não a densidade.
         */
        @Test
        @DisplayName("as 10 cabem na faixa do Codex, inclusive os hipercalóricos")
        void seedCabeNoCodex() {
            for (FormulaLactea f : formulaLacteaRepository.findAll()) {
                BigDecimal razao = f.getProteinaPor100ml()
                        .multiply(new BigDecimal("100"), MathContext.DECIMAL64)
                        .divide(f.getKcalPor100ml(), MathContext.DECIMAL64);

                assertThat(razao).as("%s (Codex CXS 72-1981)", f.getNome())
                        .isBetween(new BigDecimal("1.8"), new BigDecimal("3.0"));
            }
        }

        @Test
        @DisplayName("o FORTINI é hipercalórico de propósito — 150 kcal/100 ml")
        void fortiniEhHipercalorico() {
            // Trava explícita: se um dia alguém "corrigir" este número achando
            // que é erro de digitação, o teste explica que não é.
            var fortini = formulaLacteaRepository.findAll().stream()
                    .filter(f -> f.getNome().equals("FORTINI")).findFirst().orElseThrow();

            assertThat(fortini.getKcalPor100ml()).isEqualByComparingTo("150.0");
            assertThat(fortini.getProteinaPor100ml()).isEqualByComparingTo("3.34");
        }
    }

    @Nested
    @DisplayName("O que o cadastro recusa")
    class Recusa {

        private String criar(String json) throws Exception {
            return mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content(json))
                    .andReturn().getResponse().getContentAsString();
        }

        @Test
        @DisplayName("a composição da lata de pó é recusada, nomeando o erro")
        void composicaoDoPo() throws Exception {
            // 500 kcal e 11 g é o rótulo por 100 g de pó. A razão proteica está
            // certa (2,2 g/100 kcal) — só a faixa de densidade pega este erro.
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Do rótulo da lata","kcalPor100ml":500,"proteinaPor100ml":11}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("densidade de pó")))
                    .andExpect(jsonPath("$.erro").value(containsString("100 ml já preparados")));
        }

        @Test
        @DisplayName("o fator 10 na proteína é recusado, mesmo com a densidade certa")
        void fatorDezNaProteina() throws Exception {
            // 67 kcal está perfeito; 14 g em vez de 1,4 dá 20,9 g/100 kcal.
            // Só a razão pega este erro.
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Proteína deslocada","kcalPor100ml":67,"proteinaPor100ml":14}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("ponto decimal da proteína")));
        }

        @Test
        @DisplayName("energia e proteína trocadas de campo são recusadas")
        void camposTrocados() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Invertida","kcalPor100ml":1.4,"proteinaPor100ml":67}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("trocaram de campo")));
        }

        @Test
        @DisplayName("o suplemento hipercalórico legítimo passa")
        void hipercaloricoPassa() throws Exception {
            // O FORTINI do seed, cadastrado à mão. Se a guarda o recusasse,
            // estaria reprovando produto que o próprio sistema distribui.
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Hipercalórico da casa","kcalPor100ml":150,"proteinaPor100ml":3.34}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("os limites exatos: 250 kcal passa, 250,1 não")
        void limiteDaDensidade() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"No teto","kcalPor100ml":250,"proteinaPor100ml":5}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Um passo acima","kcalPor100ml":250.1,"proteinaPor100ml":5}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("os limites exatos da razão: 6,0 passa, 6,1 não")
        void limiteDaRazao() throws Exception {
            // 100 kcal com 6 g dá exatamente 6,0 g/100 kcal.
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"No teto da razão","kcalPor100ml":100,"proteinaPor100ml":6}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Um passo acima da razão","kcalPor100ml":100,"proteinaPor100ml":6.1}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        /**
         * Os PISOS das duas guardas (docs/09 §7.1). Só os tetos estavam
         * travados: remover a comparação com o piso não quebrava nada, e a
         * guarda passava a aceitar composição implausível para baixo — que é
         * o lado onde mora o erro de digitação por fator de 10.
         */
        @Test
        @DisplayName("os limites exatos da densidade por baixo: 20 kcal passa, 19,9 não")
        void pisoDaDensidade() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"No piso","kcalPor100ml":20,"proteinaPor100ml":0.5}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Um passo abaixo","kcalPor100ml":19.9,"proteinaPor100ml":0.5}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("os limites exatos da razão por baixo: 1,0 passa, 0,9 não")
        void pisoDaRazao() throws Exception {
            // 100 kcal com 1 g dá exatamente 1,0 g/100 kcal.
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"No piso da razão","kcalPor100ml":100,"proteinaPor100ml":1}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Um passo abaixo da razão","kcalPor100ml":100,"proteinaPor100ml":0.9}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        /**
         * Módulo puro de carboidrato existe em dieta metabólica pediátrica, e
         * recusá-lo transformaria cadastro legítimo em cadastro impossível —
         * mesmo critério que a fórmula enteral usa com macro ausente.
         */
        @Test
        @DisplayName("proteína zero passa: é módulo de carboidrato, não erro")
        void proteinaZeroPassa() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Módulo de carboidrato","kcalPor100ml":80,"proteinaPor100ml":0}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("a alteração passa pela mesma guarda que o cadastro")
        void alteracaoTambemValida() throws Exception {
            String criada = criar("""
                    {"nome":"Vai ser estragada","kcalPor100ml":70,"proteinaPor100ml":1.5}
                    """);
            String id = objectMapper.readTree(criada).get("id").asText();

            mockMvc.perform(put("/formulas-lacteas/" + id)
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Vai ser estragada","kcalPor100ml":500,"proteinaPor100ml":11}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("densidade de pó")));
        }
    }

    @Nested
    @DisplayName("A lista paginada")
    class Lista {

        @Test
        @DisplayName("traz as globais e as próprias, e o filtro global separa")
        void filtroGlobal() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                    .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                    .contentType("application/json")
                    .content("""
                            {"nome":"Só do A","kcalPor100ml":70,"proteinaPor100ml":1.5}
                            """));

            mockMvc.perform(get("/formulas-lacteas").param("size", "50")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(11));

            mockMvc.perform(get("/formulas-lacteas").param("global", "true").param("size", "50")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.totalElements").value(10));

            mockMvc.perform(get("/formulas-lacteas").param("global", "false")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].nome").value("Só do A"));
        }

        @Test
        @DisplayName("a lista não vaza a fórmula de outro cliente")
        void listaNaoVaza() throws Exception {
            mockMvc.perform(post("/formulas-lacteas")
                    .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                    .contentType("application/json")
                    .content("""
                            {"nome":"Exclusiva do A","kcalPor100ml":70,"proteinaPor100ml":1.5}
                            """));

            mockMvc.perform(get("/formulas-lacteas").param("global", "false")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("o filtro de nome ignora acento e caixa")
        void filtroDeNome() throws Exception {
            mockMvc.perform(get("/formulas-lacteas").param("nome", "neocate")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }
}

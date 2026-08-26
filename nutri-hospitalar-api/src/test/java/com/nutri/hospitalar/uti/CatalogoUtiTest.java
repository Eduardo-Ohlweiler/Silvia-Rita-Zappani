package com.nutri.hospitalar.uti;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.calculo.UtiMatematica;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
import com.nutri.hospitalar.uti.repository.PercentilCbRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogos da terapia nutricional de UTI adulto — carga, isolamento por tenant
 * e as regras que o cadastro impõe.
 *
 * <p>Os números conferidos aqui não são arbitrários: cada um trava um defeito
 * concreto da planilha de origem ou do eroERP, listado em {@code docs/10}.
 */
@DisplayName("Catálogos da UTI adulto")
class CatalogoUtiTest extends AbstractIntegrationTest {

    @Autowired PercentilCbRepository percentilCbRepository;

    @Nested
    @DisplayName("Carga das migrations")
    class Carga {

        @Test
        @DisplayName("as 53 fórmulas enterais entram, e todas são globais")
        void formulasEnterais() {
            var todas = formulaEnteralRepository.findAll();
            assertThat(todas).hasSize(53);
            assertThat(todas).allMatch(FormulaEnteral::ehGlobal);
        }

        @Test
        @DisplayName("os 30 produtos entram, separados nos três tipos")
        void produtos() {
            var todos = produtoNutricionalRepository.findAll();
            // 30 da migration 019 + 5 insumos artesanais da 026
            assertThat(todos).hasSize(35);
            assertThat(todos).allMatch(ProdutoNutricional::ehGlobal);

            assertThat(todos).filteredOn(p -> p.getTipo() == TipoProdutoNutricional.SUPLEMENTO_ORAL)
                    .hasSize(23);
            assertThat(todos).filteredOn(p -> p.getTipo() == TipoProdutoNutricional.MODULO_PROTEICO)
                    .hasSize(3);
            assertThat(todos).filteredOn(p -> p.getTipo() == TipoProdutoNutricional.INSUMO_ARTESANAL)
                    .hasSize(9);
        }

        @Test
        @DisplayName("os quatro papéis artesanais estão todos ocupados")
        void papeisArtesanais() {
            // Se um papel ficasse vago, a receita do sistema aberto sairia
            // incompleta e sem avisar.
            for (PapelArtesanal papel : PapelArtesanal.values()) {
                assertThat(produtoNutricionalRepository.findAll())
                        .as("papel %s", papel)
                        .anyMatch(p -> p.getPapelArtesanal() == papel);
            }
        }

        /** Os que vieram de `TNE SA!A2:A5`, e cujos números o gabarito usa. */
        private static final Set<String> QUATRO_DA_PLANILHA = Set.of(
                "Trophic basic pó", "Carbodex", "Albumix power", "Óleo de soja");

        @Test
        @DisplayName("os insumos da 026 fecham por Atwater e não mexem nos quatro da planilha")
        void insumosDaMigration026() {
            var insumos = produtoNutricionalRepository.findAll().stream()
                    .filter(p -> p.getTipo() == TipoProdutoNutricional.INSUMO_ARTESANAL)
                    .toList();

            // Todo insumo entra em cálculo, e o calculador deriva a energia por
            // Atwater (4/4/9). Se o rótulo transcrito não fecha, o número que a
            // tela mostra não é o do produto — foi assim que os defeitos 1 e 2
            // do catálogo de fórmulas apareceram.
            //
            // A tolerância existe porque rótulo REAL não fecha exato: o Trophic
            // basic declara 33,93 kcal/medida e os macros somam 33,49 — 1,3 %,
            // que é fibra, poliol e arredondamento do fabricante, não erro de
            // transcrição. É a mesma razão do CHECK de ±12 % da migration 018.
            // Os insumos de macronutriente puro (óleo, maltodextrina) fecham em
            // zero, e é isso que a folga apertada garante.
            BigDecimal folgaMaxima = new BigDecimal("5");

            for (var p : insumos) {
                BigDecimal porAtwater = UtiMatematica.kcalDeMacros(
                        p.getChoG(), p.getProteinaG(), p.getLipG());
                BigDecimal desvio = porAtwater.subtract(p.getKcal())
                        .abs()
                        .multiply(new BigDecimal("100"))
                        .divide(p.getKcal(), 4, RoundingMode.HALF_UP);

                assertThat(desvio)
                        .as("%s declara %s kcal e os macros somam %s (%s %% de desvio)",
                                p.getNome(), p.getKcal(), porAtwater, desvio)
                        .isLessThanOrEqualTo(folgaMaxima);
            }

            // Os cinco da 026 são macronutriente puro: fecham exato, sem folga.
            for (var p : insumos.stream().filter(i -> !QUATRO_DA_PLANILHA.contains(i.getNome())).toList()) {
                assertThat(UtiMatematica.kcalDeMacros(p.getChoG(), p.getProteinaG(), p.getLipG()))
                        .as("%s é macronutriente puro e tem de fechar exato", p.getNome())
                        .isEqualByComparingTo(p.getKcal());
            }

            // Os quatro da planilha continuam com os números da planilha: o
            // gabarito artesanal de docs/10 §10 é calculado sobre eles.
            var trophic = insumos.stream()
                    .filter(p -> p.getNome().equals("Trophic basic pó")).findFirst().orElseThrow();
            assertThat(trophic.getMedidaQtd()).isEqualByComparingTo("7.8");
            assertThat(trophic.getKcal()).isEqualByComparingTo("33.93");
            assertThat(trophic.getProteinaG()).isEqualByComparingTo("1.24");
            assertThat(trophic.getChoG()).isEqualByComparingTo("4.68");
            assertThat(trophic.getLipG()).isEqualByComparingTo("1.09");

            // E nenhum papel ficou com uma opção só, exceto a base — que é o
            // que a 026 registra como pendência de rótulo, não de código.
            assertThat(insumos).filteredOn(p -> p.getPapelArtesanal() == PapelArtesanal.LIPIDIO)
                    .hasSize(5);
            assertThat(insumos).filteredOn(p -> p.getPapelArtesanal() == PapelArtesanal.CARBOIDRATO)
                    .hasSize(2);
        }

        @Test
        @DisplayName("o papel proteico enxerga também os módulos proteicos do catálogo")
        void papelProteicoIncluiModulos() throws Exception {
            // Sem isto o combo de proteína mostrava uma opção enquanto três
            // módulos já conferidos estavam no catálogo, invisíveis.
            mockMvc.perform(get("/produtos-nutricionais/insumos-artesanais")
                            .param("papel", "PROTEINA")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[?(@.nome == 'Albumix power')]").exists())
                    .andExpect(jsonPath("$[?(@.nome == 'Nutren Just Protein')]").exists());
        }

        @Test
        @DisplayName("a tabela de percentil de CB tem as 9 faixas nos dois sexos")
        void percentilCb() {
            assertThat(percentilCbRepository.findAll()).hasSize(18);
        }
    }

    @Nested
    @DisplayName("As correções que o documento manda fazer")
    class Correcoes {

        @Test
        @DisplayName("Fresubin 2kcal HP está normalizado por litro, não por frasco de 500 ml")
        void fresubinNormalizado() {
            // Defeitos 1 e 2 de docs/10 §11: a planilha traz 50 g por embalagem
            // de 500 ml, e uma das abas divide por 1000 — proteína pela metade.
            // Por litro são 100 g, e é assim que fica gravado.
            FormulaEnteral f = buscarGlobal("Fresubin 2kcal HP");
            assertThat(f.getProteinaGL()).isEqualByComparingTo("100");
            assertThat(f.getDensidadeKcalMl()).isEqualByComparingTo("2.0");
        }

        @Test
        @DisplayName("nenhum nome carrega a apresentação — ela contradiria o por litro")
        void nomeSemApresentacao() {
            // Migration 022. "Fresubin 2kcal HP (500ml) · 100 g PTN/L" convida a
            // ler 100 g no frasco, que é o defeito 1 voltando pelo rótulo.
            assertThat(formulaEnteralRepository.findAll())
                    .extracting(FormulaEnteral::getNome)
                    .noneMatch(n -> n.contains("500ml"));
        }

        @Test
        @DisplayName("nenhum nome do catálogo tem espaço sobrando nas pontas")
        void nomesComTrim() {
            // A planilha traz 'Impact ', 'Isosource 1.5 ' e outros com espaço à
            // direita; qualquer junção por nome quebraria.
            assertThat(formulaEnteralRepository.findAll())
                    .extracting(FormulaEnteral::getNome)
                    .allMatch(n -> n.equals(n.strip()));
        }

        @Test
        @DisplayName("Diben HP tem a densidade corrigida para 1,5")
        void dibenCorrigido() {
            // A planilha declara 1,0 e a composição fecha em +45,4 %.
            assertThat(buscarGlobal("Diben HP").getDensidadeKcalMl()).isEqualByComparingTo("1.5");
        }

        @Test
        @DisplayName("Nutrison Advanced Peptisorb tem o lipídio sem o fator 10")
        void peptisorbCorrigido() {
            assertThat(buscarGlobal("Nutrison Advanced Peptisorb").getLipGL())
                    .isEqualByComparingTo("17");
        }

        @Test
        @DisplayName("aos 59 anos o P50 de CB é o da faixa 50–59,9, não o da anterior")
        void percentilNaFronteiraDaFaixa() {
            // Defeito 19: o PROCV da planilha devolve a linha anterior no
            // limite, e aos 59 anos lê 32,3 (faixa 30–39,9) em vez de 32,6.
            var faixa = percentilCbRepository
                    .findFaixaDe(Sexo.MASCULINO, new BigDecimal("59"))
                    .orElseThrow();

            assertThat(faixa.getP50Cm()).isEqualByComparingTo("32.6");
            assertThat(faixa.getP50Cm()).isNotEqualByComparingTo("32.3");
        }

        @Test
        @DisplayName("toda fórmula com os três macros fecha dentro dos 12 %")
        void catalogoInteiroFecha() {
            // É esta conta que encontrou os quatro produtos errados da planilha.
            // Rodá-la sobre o catálogo inteiro impede que um seed novo os traga
            // de volta.
            for (FormulaEnteral f : formulaEnteralRepository.findAll()) {
                if (f.getChoGL() == null || f.getLipGL() == null) continue;

                double declarada = f.getDensidadeKcalMl().doubleValue() * 1000;
                double macros = f.getProteinaGL().doubleValue() * 4
                        + f.getChoGL().doubleValue() * 4
                        + f.getLipGL().doubleValue() * 9;

                assertThat(Math.abs(macros - declarada) / declarada)
                        .as("fechamento de %s", f.getNome())
                        .isLessThanOrEqualTo(0.12);
            }
        }
    }

    @Nested
    @DisplayName("Global: todo cliente lê, nenhum escreve")
    class Global {

        @Test
        @DisplayName("a fórmula global é visível para os dois clientes")
        void formulaVisivelParaTodos() throws Exception {
            UUID id = buscarGlobal("Diben HP").getId();

            for (String email : new String[]{adminA.getEmail(), adminB.getEmail()}) {
                mockMvc.perform(get("/formulas-enterais/" + id)
                                .header(AUTHORIZATION, autenticar(email)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.nome").value("Diben HP"))
                        .andExpect(jsonPath("$.global").value(true));
            }
        }

        @Test
        @DisplayName("a fórmula global devolve 400 explicado na alteração, não 404")
        void formulaNaoEditavel() throws Exception {
            UUID id = buscarGlobal("Diben HP").getId();

            mockMvc.perform(put("/formulas-enterais/" + id)
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Adulterada","densidadeKcalMl":1.5,"proteinaGL":75}
                                    """))
                    .andExpect(status().isBadRequest())
                    // 400 e não 404: o registro está à frente do usuário na
                    // lista, e mandá-lo procurar seria mentira.
                    .andExpect(jsonPath("$.erro").value(containsString("fórmula do sistema")));

            mockMvc.perform(patch("/formulas-enterais/" + id + "/ativo")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("ativo", "false"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("o produto global devolve 400 explicado na alteração")
        void produtoNaoEditavel() throws Exception {
            UUID id = produtoNutricionalRepository.findAll().getFirst().getId();

            mockMvc.perform(patch("/produtos-nutricionais/" + id + "/ativo")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("ativo", "false"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("produto do sistema")));
        }
    }

    @Nested
    @DisplayName("Isolamento por tenant")
    class Isolamento {

        @Test
        @DisplayName("fórmula própria de um cliente não aparece para o outro")
        void formulaPropriaNaoVaza() throws Exception {
            String id = criarFormulaNoA("Só do A");

            mockMvc.perform(get("/formulas-enterais/" + id)
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.global").value(false));

            mockMvc.perform(get("/formulas-enterais/" + id)
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("o combo do B traz as 53 globais e nenhuma fórmula do A")
        void comboNaoVaza() throws Exception {
            criarFormulaNoA("Exclusiva do A");

            mockMvc.perform(get("/formulas-enterais/select")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(53))
                    .andExpect(jsonPath("$[?(@.nome == 'Exclusiva do A')]").isEmpty());
        }

        @Test
        @DisplayName("produto próprio de um cliente não aparece no catálogo do outro")
        void produtoProprioNaoVaza() throws Exception {
            mockMvc.perform(post("/produtos-nutricionais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Módulo do A","tipo":"MODULO_PROTEICO",
                                     "medidaNome":"medida","medidaQtd":5,
                                     "kcal":20,"proteinaG":4.5}
                                    """))
                    .andExpect(status().isCreated());

            // O A vê o seu módulo somado aos 3 globais; o B só os 3.
            mockMvc.perform(get("/produtos-nutricionais/modulos-proteicos")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.length()").value(4));

            mockMvc.perform(get("/produtos-nutricionais/modulos-proteicos")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(jsonPath("$.length()").value(3));
        }
    }

    @Nested
    @DisplayName("O que o cadastro recusa")
    class Recusas {

        @Test
        @DisplayName("composição de frasco de 500 ml lançada como litro é recusada")
        void naoAceitaComposicaoPorEmbalagem() throws Exception {
            // 50 g de PTN e 2,0 kcal/ml: se fosse por litro, os macros fechariam
            // em torno de −50 %. É o defeito 1 da planilha, e ele não pode
            // sequer ser gravado.
            mockMvc.perform(post("/formulas-enterais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Fresubin sem normalizar","densidadeKcalMl":2.0,
                                     "proteinaGL":50,"choGL":90,"lipGL":39}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("não fecha")))
                    // A mensagem nomeia a causa provável em vez de mandar
                    // conferir sete campos.
                    .andExpect(jsonPath("$.erro").value(containsString("500 ml")));
        }

        @Test
        @DisplayName("macro com o ponto decimal deslocado é recusado, com a dica certa")
        void naoAceitaFatorDez() throws Exception {
            // Defeito do Peptisorb: lipídio 170 g/L onde são 17.
            mockMvc.perform(post("/formulas-enterais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Peptisorb com fator 10","densidadeKcalMl":1.0,
                                     "proteinaGL":40,"choGL":176,"lipGL":170}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("ponto decimal")));
        }

        @Test
        @DisplayName("fórmula sem carboidrato e lipídio é aceita — não há o que fechar")
        void aceitaComposicaoParcial() throws Exception {
            mockMvc.perform(post("/formulas-enterais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Só densidade e proteína","densidadeKcalMl":1.2,
                                     "proteinaGL":60}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("insumo artesanal sem papel é recusado")
        void insumoSemPapel() throws Exception {
            mockMvc.perform(post("/produtos-nutricionais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Insumo órfão","tipo":"INSUMO_ARTESANAL",
                                     "medidaNome":"g","medidaQtd":100,"kcal":400,"proteinaG":10}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("papel na receita")));
        }

        @Test
        @DisplayName("suplemento oral com papel artesanal é recusado")
        void suplementoComPapel() throws Exception {
            mockMvc.perform(post("/produtos-nutricionais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Suplemento com papel","tipo":"SUPLEMENTO_ORAL",
                                     "medidaNome":"frasco","medidaQtd":200,
                                     "papelArtesanal":"PROTEINA"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("não teria efeito")));
        }

        @Test
        @DisplayName("módulo proteico sem calorias é recusado — sairia dose nula em silêncio")
        void moduloSemComposicao() throws Exception {
            mockMvc.perform(post("/produtos-nutricionais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Módulo vazio","tipo":"MODULO_PROTEICO",
                                     "medidaNome":"medida","medidaQtd":5}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("entra em cálculo")));
        }

        @Test
        @DisplayName("o servidor deriva moduloProteico do tipo e ignora o que vier no corpo")
        void moduloProteicoNaoVemDoCliente() throws Exception {
            // Mesmo mandando `moduloProteico: true` num suplemento, o servidor
            // grava false: são o mesmo fato dito duas vezes, e quem manda é o
            // tipo.
            mockMvc.perform(post("/produtos-nutricionais")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Suplemento adulterado","tipo":"SUPLEMENTO_ORAL",
                                     "medidaNome":"frasco","medidaQtd":200,
                                     "moduloProteico":true}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.moduloProteico").value(false));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    private FormulaEnteral buscarGlobal(String nome) {
        return formulaEnteralRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && nome.equals(f.getNome()))
                .findFirst().orElseThrow(() ->
                        new AssertionError("fórmula global não encontrada no seed: " + nome));
    }

    private String criarFormulaNoA(String nome) throws Exception {
        String corpo = mockMvc.perform(post("/formulas-enterais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"%s","densidadeKcalMl":1.0,"proteinaGL":40}
                                """.formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }
}

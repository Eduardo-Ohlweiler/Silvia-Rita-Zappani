package com.nutri.hospitalar.uti;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O endpoint {@code POST /uti/calculo} ponta a ponta.
 *
 * <p>Os números em si são conferidos em {@code CalculoUtiTest}, contra a
 * planilha. Aqui o que se verifica é o contrato: o que o servidor aceita, o que
 * recusa e o que devolve.
 */
@DisplayName("Endpoint de cálculo da UTI")
class CalculoUtiEndpointTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("o caso canônico volta com os números da planilha")
    void casoCanonico() throws Exception {
        UUID peptamen = formulaGlobal("Peptamen Intense");

        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                 "pesoAtualKg":68,"circBracoCm":25,"circPanturrilhaCm":34,
                                 "circAbdominalCm":90,"alturaJoelhoCm":53,
                                 "fase":"AGUDA",
                                 "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                                 "volumePorTempo":62,"tempo":22}
                                """.formatted(peptamen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dieta.volumeTotalMl").value(1364.0))
                .andExpect(jsonPath("$.necessidades.metaEnergetica").value(1360.0))
                .andExpect(jsonPath("$.antropometria.pesoDeTrabalhoKg").value(68.0));
    }

    @Test
    @DisplayName("o peso e a meta voltam com a origem por escrito")
    void origemDaCascata() throws Exception {
        // Sem peso informado, o servidor estima — e diz qual equação usou.
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                 "circBracoCm":25,"circPanturrilhaCm":34,"circAbdominalCm":90,
                                 "fase":"AGUDA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antropometria.pesoDeTrabalhoOrigem")
                        .value("peso estimado · Rabito 2008"))
                // A posição na faixa viaja junto: o padrão é o topo, e ele
                // aparece escrito em vez de ficar implícito no código.
                .andExpect(jsonPath("$.necessidades.metaEnergeticaOrigem")
                        .value("da faixa da fase · máximo"));
    }

    @Test
    @DisplayName("a origem da meta sobrevive ao protocolo de obesidade")
    void origemNaObesidade() throws Exception {
        // IMC 35 com 1,60 m e 90 kg. A meta vem do protocolo, não da fase — e é
        // isso que a aba da dieta precisa exibir.
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"FEMININO","idadeAnos":50,"alturaCm":160,
                                 "pesoAtualKg":90,"fase":"AGUDA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.necessidades.obeso").value(true))
                .andExpect(jsonPath("$.necessidades.metaEnergeticaOrigem")
                        .value("protocolo de obesidade · máximo"))
                // E diz sobre qual peso cada meta foi calculada
                .andExpect(jsonPath("$.necessidades.baseDoPeso").value(containsString("peso atual")));
    }

    @Test
    @DisplayName("resultado mandado no corpo é ignorado — o servidor recalcula")
    void naoAceitaResultadoAdulterado() throws Exception {
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":62,
                                 "imc":999,"metaEnergetica":99999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antropometria.imc").value(20.2449));
    }

    @Test
    @DisplayName("amputação sobreposta devolve 422 nomeando os dois segmentos")
    void amputacaoSobreposta() throws Exception {
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":70,
                                 "segmentosAmputados":["MEMBRO_SUPERIOR","MAO"]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("Membro superior")))
                .andExpect(jsonPath("$.erro").value(containsString("Mão")));
    }

    @Test
    @DisplayName("ausência vem com o motivo, nunca muda")
    void ausenciaExplicada() throws Exception {
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antropometria.motivoPesoDeTrabalho")
                        .value(containsString("Informe o peso")))
                .andExpect(jsonPath("$.dieta.motivo")
                        .value(containsString("Escolha a fórmula enteral")));
    }

    @Test
    @DisplayName("sem água livre no cadastro, o cálculo estima pela densidade — e diz que estimou")
    void aguaEstimadaPelaDensidade() throws Exception {
        // Peptamen Intense: 1,0 kcal/ml, sem água livre cadastrada. Nenhuma das
        // 53 fórmulas do seed tem o dado do rótulo ainda — ver docs/10 §13.
        UUID peptamen = formulaGlobal("Peptamen Intense");

        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":72,
                                 "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                                 "volumePorTempo":80,"tempo":20}
                                """.formatted(peptamen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidratacao.necessidadeIdeal").value(2160.0))
                .andExpect(jsonPath("$.hidratacao.percentualAgua").value(85.0))
                .andExpect(jsonPath("$.hidratacao.percentualAguaOrigem")
                        .value("estimada pela densidade"));
    }

    @Test
    @DisplayName("com água livre no cadastro, o cálculo usa o rótulo — e diz que usou")
    void aguaDoRotulo() throws Exception {
        // A mesma densidade de 1,0 daria 85 % pela escada; o rótulo diz 84 %, e
        // é o rótulo que vale.
        String corpo = mockMvc.perform(post("/formulas-enterais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Com água no rótulo","densidadeKcalMl":1.0,
                                 "proteinaGL":40,"aguaLivrePerc":84}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":72,
                                 "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                                 "volumePorTempo":80,"tempo":20}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidratacao.percentualAgua").value(84.0))
                .andExpect(jsonPath("$.hidratacao.percentualAguaOrigem")
                        .value("água livre do rótulo"));
    }

    @Test
    @DisplayName("densidade intermediária sem rótulo devolve o motivo, não um número inventado")
    void densidadeSemLinhaNaTabela() throws Exception {
        // 1,24 kcal/ml não tem linha na tabela de água da planilha. Aplicar uma
        // função escada seria inferência nossa sobre prescrição de UTI.
        UUID novasource = formulaGlobal("Novasource Senior");

        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":72,
                                 "formulaEnteralId":"%s","modoInfusao":"INTERMITENTE",
                                 "volumePorTempo":133,"tempo":6}
                                """.formatted(novasource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidratacao.percentualAgua").doesNotExist())
                .andExpect(jsonPath("$.hidratacao.motivo")
                        .value(containsString("água livre no rótulo")));
    }

    @Test
    @DisplayName("fórmula de outro cliente devolve 404")
    void formulaDeOutroTenant() throws Exception {
        String corpo = mockMvc.perform(post("/formulas-enterais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Só do A","densidadeKcalMl":1.0,"proteinaGL":40}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pesoAtualKg":70,"formulaEnteralId":"%s"}
                                """.formatted(id)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("entrada implausível é recusada na validação")
    void validacaoDeFaixa() throws Exception {
        mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("{\"pesoAtualKg\":900}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────

    private UUID formulaGlobal(String nome) {
        return formulaEnteralRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && nome.equals(f.getNome()))
                .findFirst()
                .map(FormulaEnteral::getId)
                .orElseThrow(() -> new AssertionError("fórmula não semeada: " + nome));
    }
}

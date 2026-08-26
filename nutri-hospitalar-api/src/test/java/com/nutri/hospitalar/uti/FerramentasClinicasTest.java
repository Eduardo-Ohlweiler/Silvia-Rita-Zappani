package com.nutri.hospitalar.uti;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As quatro ferramentas clínicas ponta a ponta.
 *
 * <p>Os números estão conferidos contra a planilha em {@code CalculoUtiTest}.
 * Aqui o que se verifica é o contrato e os dois consertos de desenho: os presets
 * de noradrenalina expostos como ampolas e soro, e os insumos artesanais vindos
 * do catálogo em vez de constante no código.
 */
@DisplayName("Ferramentas clínicas")
class FerramentasClinicasTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("noradrenalina — os presets devolvem a concentração que os explica")
    void presetsDeNoradrenalina() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"noraPesoKg":70,"noraVazaoMlH":25,"noraPreparo":"SIMPLES_32"}
                                """))
                .andExpect(status().isOk())
                // O "32" da planilha não é constante: são 2 ampolas em 250 ml
                .andExpect(jsonPath("$.noradrenalina.concentracaoMcgMl").value(32.0))
                .andExpect(jsonPath("$.noradrenalina.doseMcgKgMin").value(0.1905));

        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"noraPesoKg":70,"noraVazaoMlH":25,"noraPreparo":"CONCENTRADA_64"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noradrenalina.concentracaoMcgMl").value(64.0))
                .andExpect(jsonPath("$.noradrenalina.doseMcgKgMin").value(0.381));
    }

    @Test
    @DisplayName("noradrenalina — o preparo do serviço usa a mesma fórmula")
    void preparoProprio() throws Exception {
        // 4 ampolas em 234 ml, o exemplo de Cálculos!J5. É a mesma conta dos
        // presets — e é por isso que existe uma fórmula só.
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"noraPesoKg":70,"noraVazaoMlH":20,
                                 "noraPreparo":"AMPOLAS_E_SORO","noraAmpolas":4,
                                 "noraVolumeSoroMl":234}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noradrenalina.doseMcgKgMin").value(0.3256));
    }

    @Test
    @DisplayName("balanço nitrogenado vem classificado, não só numérico")
    void balancoClassificado() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"balancoProteinaG":95,"balancoUreiaG":40}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balancoNitrogenado.balanco").value(-7.4916))
                // O eroERP mostra só o número, colorido por um hex cravado
                .andExpect(jsonPath("$.balancoNitrogenado.classificacao.rotulo")
                        .value(containsString("catabolismo")))
                .andExpect(jsonPath("$.balancoNitrogenado.classificacao.tom").value("ATENCAO"));
    }

    @Test
    @DisplayName("propofol — as horas são campo, e a resposta diz qual usou")
    void propofolComHoras() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("{\"propofolVazaoMlH\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propofol.kcalDia").value(528.0))
                .andExpect(jsonPath("$.propofol.horasConsideradas").value(24));

        // Desligado ao meio-dia não entregou 24 horas de caloria
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("{\"propofolVazaoMlH\":20,\"propofolHoras\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propofol.kcalDia").value(264.0));
    }

    @Test
    @DisplayName("dieta artesanal — os insumos vêm do catálogo, e o caso canônico fecha")
    void artesanalDoCatalogo() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"artesanalVetKcal":2000,"artesanalPesoKg":75,
                                 "insumoBaseId":"%s",
                                 "insumoCarboidratoId":"%s","medidasCarboidrato":3,
                                 "insumoProteinaId":"%s","medidasProteina":2,
                                 "insumoLipidioId":"%s","medidasLipidio":0.5,
                                 "administracoesPorDia":4}
                                """.formatted(insumo(PapelArtesanal.BASE),
                                              insumo(PapelArtesanal.CARBOIDRATO),
                                              insumo(PapelArtesanal.PROTEINA),
                                              insumo(PapelArtesanal.LIPIDIO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artesanal.dosesBase").value(48.7504))
                .andExpect(jsonPath("$.artesanal.kcalTotal").value(1946.6498))
                // Os dois caminhos para a kcal concordam — a conferência que a
                // própria planilha oferece
                .andExpect(jsonPath("$.artesanal.kcalPorMacros").value(1946.6498))
                .andExpect(jsonPath("$.artesanal.aguaTotal").value(1789.3855));
    }

    @Test
    @DisplayName("dieta artesanal — os dois denominadores aparecem nomeados")
    void doisDenominadores() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"artesanalVetKcal":2000,"artesanalPesoKg":75,
                                 "insumoBaseId":"%s",
                                 "insumoCarboidratoId":"%s","medidasCarboidrato":3,
                                 "insumoProteinaId":"%s","medidasProteina":2,
                                 "insumoLipidioId":"%s","medidasLipidio":0.5}
                                """.formatted(insumo(PapelArtesanal.BASE),
                                              insumo(PapelArtesanal.CARBOIDRATO),
                                              insumo(PapelArtesanal.PROTEINA),
                                              insumo(PapelArtesanal.LIPIDIO))))
                .andExpect(status().isOk())
                // Os números da planilha, cujo denominador é o VET desejado
                .andExpect(jsonPath("$.artesanal.percChoSobreVet").value(52.23))
                // E os que somam 100, porque dividem pelo que foi ofertado
                .andExpect(jsonPath("$.artesanal.percChoSobreOfertado").value(53.66));
    }

    @Test
    @DisplayName("cadastrar um insumo novo faz efeito na lista de papéis")
    void catalogoAlimentaAReceita() throws Exception {
        // No eroERP os quatro insumos estão cravados em código; cadastrar não
        // produz efeito nenhum. Aqui produz.
        //
        // Cinco lipídios globais: o óleo de soja da planilha mais os quatro
        // óleos da migration 026.
        mockMvc.perform(get("/produtos-nutricionais/insumos-artesanais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("papel", "LIPIDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mockMvc.perform(post("/produtos-nutricionais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Óleo de coco do hospital","tipo":"INSUMO_ARTESANAL",
                                 "medidaNome":"colher","medidaQtd":13,"embalagemQtd":900,
                                 "kcal":108,"proteinaG":0,"choG":0,"lipG":12,
                                 "papelArtesanal":"LIPIDIO"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/produtos-nutricionais/insumos-artesanais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("papel", "LIPIDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                // O do cliente vem antes dos globais
                .andExpect(jsonPath("$[0].nome").value("Óleo de coco do hospital"));
    }

    @Test
    @DisplayName("corpo vazio devolve os quatro motivos, não quatro traços")
    void ausenciasExplicadas() throws Exception {
        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noradrenalina.motivo").value(containsString("peso e a vazão")))
                .andExpect(jsonPath("$.balancoNitrogenado.motivo").value(containsString("ureia")))
                .andExpect(jsonPath("$.propofol.motivo").value(containsString("vazão do propofol")))
                .andExpect(jsonPath("$.artesanal.motivo").value(containsString("insumo base")));
    }

    @Test
    @DisplayName("insumo de outro cliente não é usado, e as outras ferramentas seguem")
    void insumoDeOutroTenant() throws Exception {
        String corpo = mockMvc.perform(post("/produtos-nutricionais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Base do A","tipo":"INSUMO_ARTESANAL",
                                 "medidaNome":"medida","medidaQtd":8,"embalagemQtd":800,
                                 "kcal":34,"proteinaG":1.2,"choG":4.7,"lipG":1.1,
                                 "papelArtesanal":"BASE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        mockMvc.perform(post("/uti/ferramentas-clinicas")
                        .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"artesanalVetKcal":2000,"insumoBaseId":"%s",
                                 "propofolVazaoMlH":20}
                                """.formatted(id)))
                .andExpect(status().isOk())
                // O insumo alheio some, com motivo...
                .andExpect(jsonPath("$.artesanal.motivo").value(containsString("insumo base")))
                // ...e as outras três ferramentas continuam respondendo
                .andExpect(jsonPath("$.propofol.kcalDia").value(528.0));
    }

    // ─────────────────────────────────────────────────────────────────────

    private UUID insumo(PapelArtesanal papel) {
        return produtoNutricionalRepository.findAll().stream()
                .filter(p -> p.getPapelArtesanal() == papel)
                .findFirst()
                .map(ProdutoNutricional::getId)
                .orElseThrow(() -> new AssertionError("insumo não semeado para o papel " + papel));
    }
}

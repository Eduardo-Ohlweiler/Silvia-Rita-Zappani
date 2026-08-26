package com.nutri.hospitalar.uti;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O acompanhamento diário — o que o vínculo com a avaliação paga, e o que a
 * restrição de um-por-dia impede.
 */
@DisplayName("Acompanhamento diário de UTI")
class RegistroDiarioUtiTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa paciente;
    private UUID avaliacaoId;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void cenario() throws Exception {
        paciente = criarPaciente(tenantA, "Paciente do A");
        avaliacaoId = criarAvaliacao(hoje.minusDays(1));
    }

    @Test
    @DisplayName("o vínculo com a avaliação paga kcal/kg, proteína e diurese por quilo")
    void derivadosComAvaliacao() throws Exception {
        // Avaliação: 68 kg, Peptamen Intense 1,0 kcal/ml e 92 g/L, VT de 1364 ml.
        // Recebeu 1200 ml no dia, e urinou 1800 ml.
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                 "volRecebido24h":1200,"diureseMl":1800}
                                """.formatted(paciente.getId(), avaliacaoId, hoje)))
                .andExpect(status().isCreated())
                // 1200 / 1364 × 100
                .andExpect(jsonPath("$.percentualRecebido").value(87.98))
                .andExpect(jsonPath("$.referenciaDoPercentual").value("prescrito na avaliação"))
                // 1200 ml × 1,0 kcal/ml
                .andExpect(jsonPath("$.caloriasRecebidas").value(1200.0))
                // 1200 × 92 / 1000
                .andExpect(jsonPath("$.proteinaRecebida").value(110.4))
                .andExpect(jsonPath("$.caloriasPorQuilo").value(17.6471))
                .andExpect(jsonPath("$.proteinaPorQuilo").value(1.6235))
                // 1800 / 68 / 24
                .andExpect(jsonPath("$.diuresePorQuiloHora").value(1.1029));
    }

    @Test
    @DisplayName("sem avaliação vinculada, o dia grava e diz o que ficou de fora")
    void semAvaliacaoAindaGrava() throws Exception {
        // Paciente que internou de madrugada tem dia antes de avaliação.
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s",
                                 "volPrescrito24h":1500,"volRecebido24h":1200,"diureseMl":1800}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated())
                // Cai no prescrito digitado, e diz que caiu
                .andExpect(jsonPath("$.percentualRecebido").value(80.0))
                .andExpect(jsonPath("$.referenciaDoPercentual")
                        .value("prescrito informado no dia"))
                // Sem peso e sem fórmula, esses não existem — com o motivo
                .andExpect(jsonPath("$.caloriasPorQuilo").doesNotExist())
                .andExpect(jsonPath("$.diuresePorQuiloHora").doesNotExist())
                .andExpect(jsonPath("$.motivoDerivados").value(containsString("Sem avaliação")));
    }

    @Test
    @DisplayName("dois registros do mesmo paciente no mesmo dia dão 409")
    void umRegistroPorDia() throws Exception {
        String corpo = """
                {"pessoaId":"%s","data":"%s","volRecebido24h":1200}
                """.formatted(paciente.getId(), hoje);

        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated());

        // No eroERP os dois coexistem, e o painel plota os dois.
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json").content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("Já existe um acompanhamento")));

        assertThat(registroDiarioUtiRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a avaliação sugerida é a mais recente ATÉ a data, não a última de todas")
    void avaliacaoSugerida() throws Exception {
        // Uma avaliação de amanhã não deve ser sugerida para o dia de hoje.
        criarAvaliacao(hoje);

        mockMvc.perform(get("/uti/registros-diarios/avaliacao-sugerida")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("pessoaId", paciente.getId().toString())
                        .param("data", hoje.minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(avaliacaoId.toString()))
                .andExpect(jsonPath("$.pesoTrabalhoKg").value(68.0));

        // Antes de qualquer avaliação, vem vazia com o motivo
        mockMvc.perform(get("/uti/registros-diarios/avaliacao-sugerida")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("pessoaId", paciente.getId().toString())
                        .param("data", hoje.minusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.motivo").value(containsString("ainda não tem avaliação")));
    }

    @Test
    @DisplayName("apagar avaliação com dias vinculados dá 409 dizendo quantos")
    void naoApagaAvaliacaoComDias() throws Exception {
        for (int i = 0; i < 3; i++)
            mockMvc.perform(post("/uti/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "volRecebido24h":1200}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje.minusDays(i))))
                    .andExpect(status().isCreated());

        mockMvc.perform(delete("/uti/avaliacoes/" + avaliacaoId)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("3 dias")));

        assertThat(avaliacaoUtiRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a média de ingestão oral ignora as refeições em branco")
    void mediaIngestaoOral() throws Exception {
        // Não ter registrado o lanche da tarde não é o mesmo que o paciente
        // ter recusado o lanche da tarde.
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s",
                                 "cafeManha":100,"almoco":50,"jantar":75}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated())
                // (100 + 50 + 75) / 3, e não / 6
                .andExpect(jsonPath("$.mediaIngestaoOral").value(75.0));
    }

    @Test
    @DisplayName("os tipos corrigidos: glicemia numérica, ventilação em enum, pressão em duas colunas")
    void tiposCorrigidos() throws Exception {
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","hgt":180.5,
                                 "suporteVentilatorio":"VENTILACAO_MECANICA","fio2Perc":40,
                                 "paSistolica":120,"paDiastolica":80}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated())
                // hgt é VARCHAR(255) no eroERP, e por isso o painel não o plota
                .andExpect(jsonPath("$.hgt").value(180.5))
                .andExpect(jsonPath("$.suporteVentilatorioDescricao")
                        .value("Ventilação mecânica invasiva"))
                .andExpect(jsonPath("$.fio2Perc").value(40.0))
                // "120/80" em texto livre não entra em gráfico
                .andExpect(jsonPath("$.paSistolica").value(120.0))
                .andExpect(jsonPath("$.paDiastolica").value(80.0));
    }

    @Test
    @DisplayName("o balanço hídrico aceita negativo; a diurese, não")
    void balancoNegativoEDiuresePositiva() throws Exception {
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","balancoHidricoMl":-750}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balancoHidricoMl").value(-750.0));

        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","diureseMl":-100}
                                """.formatted(paciente.getId(), hoje.minusDays(5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FiO₂ abaixo de 21 % é recusada — o ar ambiente é o piso")
    void fio2AbaixoDoArAmbiente() throws Exception {
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","fio2Perc":15}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registro de outro cliente não é lido nem removido")
    void isolamentoPorTenant() throws Exception {
        String corpo = mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","volRecebido24h":1200}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asText();

        String comoB = autenticar(adminB.getEmail());

        mockMvc.perform(get("/uti/registros-diarios/" + id).header(AUTHORIZATION, comoB))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/uti/registros-diarios/" + id).header(AUTHORIZATION, comoB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/uti/registros-diarios").header(AUTHORIZATION, comoB))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─────────────────────────────────────────────────────────────────────

    private UUID criarAvaliacao(LocalDate data) throws Exception {
        UUID peptamen = formulaEnteralRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && "Peptamen Intense".equals(f.getNome()))
                .findFirst().map(FormulaEnteral::getId).orElseThrow();

        String corpo = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                            "pesoAtualKg":68,"fase":"AGUDA",
                                            "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                                            "volumePorTempo":62,"tempo":22}}
                                """.formatted(paciente.getId(), data, peptamen)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    private Pessoa criarPaciente(Tenant tenant, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        pessoa.setSexo(Sexo.MASCULINO);
        pessoa.getTiposCadastro().add(tipoCadastroRepository.findAll().stream()
                .filter(t -> "Paciente".equals(t.getNome()))
                .findFirst().orElseThrow());
        return pessoaRepository.save(pessoa);
    }
}

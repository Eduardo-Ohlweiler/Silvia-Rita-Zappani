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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A avaliação de UTI: gravar, reabrir e não recalcular.
 *
 * <p>Os números estão conferidos contra a planilha em {@code CalculoUtiTest}.
 * Aqui o que se verifica é o que só a persistência pode quebrar — o retrato, a
 * imutabilidade do registro e o isolamento por tenant.
 */
@DisplayName("Avaliação de UTI adulto")
class AvaliacaoUtiTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa pacienteA;
    private Pessoa pacienteB;
    private UUID peptamen;

    @BeforeEach
    void cenario() {
        pacienteA = criarPaciente(tenantA, "Paciente do A");
        pacienteB = criarPaciente(tenantB, "Paciente do B");
        peptamen = formulaGlobal("Peptamen Intense");
    }

    @Test
    @DisplayName("grava e devolve os números do caso canônico")
    void gravaOCasoCanonico() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), peptamen)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado.dieta.volumeTotalMl").value(1364.0))
                .andExpect(jsonPath("$.resultado.necessidades.metaEnergetica").value(1360.0))
                .andExpect(jsonPath("$.resultado.antropometria.imc").value(24.093))
                .andExpect(jsonPath("$.pacienteNome").value("Paciente do A"));
    }

    @Test
    @DisplayName("o cálculo e a gravação produzem números idênticos")
    void testeEspelho() throws Exception {
        // As entradas são o MESMO DTO nos dois caminhos — é por isso que este
        // teste pode existir, e é o que impede a tela de mostrar um número
        // diferente do que o banco guarda.
        String entradas = """
                {"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,"pesoAtualKg":68,
                 "circBracoCm":25,"circPanturrilhaCm":34,"circAbdominalCm":90,
                 "alturaJoelhoCm":53,"fase":"AGUDA","formulaEnteralId":"%s",
                 "modoInfusao":"CONTINUA","volumePorTempo":62,"tempo":22}
                """.formatted(peptamen);

        String doCalculo = mockMvc.perform(post("/uti/calculo")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(entradas))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String daGravacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s","calculo":%s}
                                """.formatted(pacienteA.getId(), LocalDate.now(), entradas)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var esperado = objectMapper.readTree(doCalculo);
        var obtido = objectMapper.readTree(daGravacao).get("resultado");

        /*
         * Compara VALORES, não os textos de ausência.
         *
         * Os campos `motivo*` são explicações de interface — "Informe o peso
         * habitual para avaliar a perda" —, não resultado clínico, e por isso
         * não são gravados. Congelá-los no banco significaria que melhorar a
         * redação de uma mensagem de ajuda deixaria os registros antigos com o
         * texto velho para sempre.
         *
         * Numa avaliação salva a ausência continua legível de outro jeito: as
         * entradas estão no mesmo formulário, repovoadas, e o campo vazio ao
         * lado do traço diz o que faltou.
         */
        for (String secao : new String[]{"antropometria", "necessidades"})
            for (var campo : iteravel(esperado.get(secao).fieldNames())) {
                if (campo.startsWith("motivo")) continue;
                org.assertj.core.api.Assertions
                        .assertThat(obtido.get(secao).get(campo))
                        .as("%s.%s", secao, campo)
                        .isEqualTo(esperado.get(secao).get(campo));
            }
    }

    @Test
    @DisplayName("resultado mandado no corpo é ignorado")
    void naoAceitaResultadoAdulterado() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":62,
                                            "imc":999}}
                                """.formatted(pacienteA.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado.antropometria.imc").value(20.2449));
    }

    @Test
    @DisplayName("abrir avaliação salva NÃO recalcula, mesmo se a fórmula mudar")
    void abrirNaoRecalcula() throws Exception {
        // Uma fórmula do próprio cliente, para poder alterá-la depois
        String corpoFormula = mockMvc.perform(post("/formulas-enterais")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fórmula que vai mudar","densidadeKcalMl":1.0,
                                 "proteinaGL":40,"choGL":120,"lipGL":40}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String formulaId = objectMapper.readTree(corpoFormula).get("id").asText();

        String avaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), UUID.fromString(formulaId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado.dieta.caloriasOfertadas").value(1364.0))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(avaliacao).get("id").asText();

        // Dobra a densidade da fórmula no catálogo
        mockMvc.perform(put("/formulas-enterais/" + formulaId)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fórmula que vai mudar","densidadeKcalMl":2.0,
                                 "proteinaGL":80,"choGL":240,"lipGL":80}
                                """))
                .andExpect(status().isOk());

        // A avaliação continua mostrando o que se decidiu no dia
        mockMvc.perform(get("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado.dieta.caloriasOfertadas").value(1364.0))
                .andExpect(jsonPath("$.resultado.dieta.densidadeKcalMl").value(1.0));
    }

    @Test
    @DisplayName("avaliação salva explica o campo vazio, em vez do traço mudo")
    void avaliacaoSalvaExplicaAAusencia() throws Exception {
        // O caso canônico sem peso habitual e sem a fórmula: dois blocos ficam
        // sem número, e é exatamente aí que a tela precisa de uma frase.
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                            "pesoAtualKg":68,"fase":"AGUDA"}}
                                """.formatted(pacienteA.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        mockMvc.perform(get("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                // Os números continuam vindo do banco
                .andExpect(jsonPath("$.resultado.antropometria.imc").value(24.0930))
                .andExpect(jsonPath("$.resultado.necessidades.metaEnergetica").value(1360.0))
                // E o que não saiu diz por quê — antes tudo isto vinha nulo
                .andExpect(jsonPath("$.resultado.antropometria.percentualPerdaPeso").doesNotExist())
                .andExpect(jsonPath("$.resultado.antropometria.motivoPerdaPeso").value(
                        "Informe o peso habitual e a janela de tempo para avaliar a perda"))
                .andExpect(jsonPath("$.resultado.antropometria.motivoEstimativas").value(
                        "Informe altura do joelho e circunferência do braço para as estimativas de peso"))
                .andExpect(jsonPath("$.resultado.dieta.volumeTotalMl").doesNotExist())
                .andExpect(jsonPath("$.resultado.dieta.motivo").value(
                        "Escolha a fórmula enteral para calcular o que a dieta entrega"));
    }

    @Test
    @DisplayName("a tabela derivada explica a si mesma, sem sujar o motivo do bloco")
    void tabelaDerivadaExplicaSeMesma() throws Exception {
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), formulaGlobal("Peptamen Intense"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        mockMvc.perform(get("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                // A dieta saiu inteira: o bloco não tem motivo nenhum...
                .andExpect(jsonPath("$.resultado.dieta.volumeTotalMl").value(1364.0))
                .andExpect(jsonPath("$.resultado.dieta.motivo").doesNotExist())
                // ...e a origem do volume, que também vinha muda, voltou
                .andExpect(jsonPath("$.resultado.dieta.volumeTotalDescricao").exists())
                // ...mas a tabela derivada, que não é gravada, diz o que é
                .andExpect(jsonPath("$.resultado.dieta.progressao.length()").value(0))
                .andExpect(jsonPath("$.resultado.dieta.motivoProgressao").value(
                        containsString("Tabela derivada")))
                .andExpect(jsonPath("$.resultado.hidratacao.motivoDistribuicao").value(
                        containsString("Tabela derivada")));
    }

    @Test
    @DisplayName("o retrato da fórmula guarda os macros, não só nome e densidade")
    void retratoCompleto() throws Exception {
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), peptamen)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        // O eroERP guarda só nome, densidade e proteína — reabrir uma avaliação
        // cuja fórmula saiu do catálogo mostra a composição pela metade.
        var gravada = avaliacaoUtiRepository.findById(UUID.fromString(id)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(gravada.getFormulaChoGL()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(gravada.getFormulaLipGL()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(gravada.getFormulaNome())
                .isEqualTo("Peptamen Intense");
    }

    @Test
    @DisplayName("a origem do peso e da meta ficam gravadas, por extenso")
    void origemGravada() throws Exception {
        // Sem peso informado: o servidor estima, e a procedência entra no
        // registro clínico. É o conserto do defeito 15 levado até o banco.
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                            "circBracoCm":25,"circPanturrilhaCm":34,
                                            "circAbdominalCm":90,"fase":"AGUDA"}}
                                """.formatted(pacienteA.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado.antropometria.pesoDeTrabalhoOrigem")
                        .value("peso estimado · Rabito 2008"))
                .andExpect(jsonPath("$.resultado.necessidades.metaEnergeticaOrigem")
                        .value("da faixa da fase · máximo"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        mockMvc.perform(get("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(jsonPath("$.resultado.antropometria.pesoDeTrabalhoOrigem")
                        .value("peso estimado · Rabito 2008"));
    }

    @Test
    @DisplayName("os segmentos amputados voltam da tabela filha")
    void segmentosAmputados() throws Exception {
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":70,
                                            "segmentosAmputados":["MAO","PE"]}}
                                """.formatted(pacienteA.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        mockMvc.perform(get("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculo.segmentosAmputados.length()").value(2));
    }

    @Test
    @DisplayName("amputação sobreposta é recusada com 422, sem gravar")
    void amputacaoSobrepostaNaoGrava() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","alturaCm":175,"pesoAtualKg":70,
                                            "segmentosAmputados":["MEMBRO_SUPERIOR","MAO"]}}
                                """.formatted(pacienteA.getId(), LocalDate.now())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("Membro superior")));

        org.assertj.core.api.Assertions.assertThat(avaliacaoUtiRepository.count()).isZero();
    }

    @Test
    @DisplayName("data no futuro é recusada")
    void dataNoFuturo() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"pesoAtualKg":70}}
                                """.formatted(pacienteA.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("paciente de outro cliente devolve 404")
    void pacienteDeOutroTenant() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteB.getId(), peptamen)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Paciente não encontrado"));
    }

    @Test
    @DisplayName("avaliação de outro cliente não é lida, alterada nem removida")
    void isolamentoPorTenant() throws Exception {
        String corpoAvaliacao = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), peptamen)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpoAvaliacao).get("id").asText();

        String comoB = autenticar(adminB.getEmail());

        mockMvc.perform(get("/uti/avaliacoes/" + id).header(AUTHORIZATION, comoB))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/uti/avaliacoes/" + id)
                        .header(AUTHORIZATION, comoB)
                        .contentType("application/json")
                        .content(corpo(pacienteB.getId(), peptamen)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/uti/avaliacoes/" + id).header(AUTHORIZATION, comoB))
                .andExpect(status().isNotFound());

        // E a listagem de cada um enxerga só o seu
        mockMvc.perform(get("/uti/avaliacoes").header(AUTHORIZATION, autenticar(adminA.getEmail())))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/uti/avaliacoes").header(AUTHORIZATION, comoB))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("a lista traz a classificação gravada e filtra por nome")
    void listagem() throws Exception {
        mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content(corpo(pacienteA.getId(), peptamen)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("pacienteNome", "paciente do a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].classificacaoImc").value("Eutrofia"))
                .andExpect(jsonPath("$.content[0].tomClassificacao").value("ADEQUADO"))
                .andExpect(jsonPath("$.content[0].pesoTrabalhoOrigem").value("informado"));

        // Filtro que não casa devolve vazio, não tudo
        mockMvc.perform(get("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .param("pacienteNome", "inexistente"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─────────────────────────────────────────────────────────────────────

    /** O caso canônico: 68 kg, 1,68 m, Peptamen Intense a 62 ml/h por 22 h. */
    private String corpo(UUID pacienteId, UUID formulaId) {
        return """
                {"pacienteId":"%s","dataAvaliacao":"%s",
                 "calculo":{"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                            "pesoAtualKg":68,"circBracoCm":25,"circPanturrilhaCm":34,
                            "circAbdominalCm":90,"alturaJoelhoCm":53,"fase":"AGUDA",
                            "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                            "volumePorTempo":62,"tempo":22}}
                """.formatted(pacienteId, LocalDate.now(), formulaId);
    }

    private static <T> Iterable<T> iteravel(java.util.Iterator<T> it) {
        return () -> it;
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

    private UUID formulaGlobal(String nome) {
        return formulaEnteralRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && nome.equals(f.getNome()))
                .findFirst().map(FormulaEnteral::getId).orElseThrow();
    }
}

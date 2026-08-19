package com.nutri.hospitalar.pediatria;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Painéis da pediatria")
class PediatriaDashboardTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa paciente;
    private UUID nan2;

    @BeforeEach
    void cenario() {
        paciente = criarPaciente(tenantA, "Criança do Painel", Sexo.FEMININO,
                LocalDate.now().minusMonths(14));
        nan2 = formulaGlobal();
    }

    // ─── Curva de referência ─────────────────────────────────────────────

    @Nested
    @DisplayName("Curva da OMS")
    class Curva {

        @Test
        @DisplayName("devolve a janela pedida com os cinco percentis")
        void janela() throws Exception {
            mockMvc.perform(get("/pediatria/curvas-oms")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("sexo", "FEMININO")
                            .param("idadeMin", "6")
                            .param("idadeMax", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5))
                    .andExpect(jsonPath("$[0].idadeMeses").value(6))
                    // Âncora do docs/09 §4.3 — F, 8 meses
                    .andExpect(jsonPath("$[2].pesoP15").value(7.000))
                    .andExpect(jsonPath("$[2].pesoP85").value(9.000))
                    // A faixa externa, que só o gráfico usa
                    .andExpect(jsonPath("$[2].pesoP3").value(6.300))
                    .andExpect(jsonPath("$[2].pesoP97").value(10.000))
                    .andExpect(jsonPath("$[2].pesoP50").value(7.900));
        }

        @Test
        @DisplayName("não passa de 60 meses, nem que peçam")
        void limite() throws Exception {
            mockMvc.perform(get("/pediatria/curvas-oms")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("sexo", "MASCULINO")
                            .param("idadeMin", "55")
                            .param("idadeMax", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(6));
        }
    }

    // ─── Painel do paciente ──────────────────────────────────────────────

    @Nested
    @DisplayName("Painel do paciente")
    class Painel {

        @Test
        @DisplayName("traz a última avaliação, a trajetória e o histórico de fórmulas")
        void completo() throws Exception {
            criarAvaliacao(paciente.getId(), 8, "9", "2026-01-10");
            criarAvaliacao(paciente.getId(), 12, "10", "2026-05-10");
            criarAvaliacao(paciente.getId(), 14, "11", "2026-08-10");

            mockMvc.perform(get("/pediatria/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pacienteNome").value("Criança do Painel"))
                    .andExpect(jsonPath("$.totalAvaliacoes").value(3))
                    .andExpect(jsonPath("$.primeiraAvaliacao").value("2026-01-10"))
                    .andExpect(jsonPath("$.ultimaAvaliacao").value("2026-08-10"))
                    .andExpect(jsonPath("$.ultima.peso").value(11.000))
                    // Ordenada por idade — é o eixo X das curvas
                    .andExpect(jsonPath("$.evolucao.length()").value(3))
                    .andExpect(jsonPath("$.evolucao[0].idadeMeses").value(8))
                    .andExpect(jsonPath("$.evolucao[2].idadeMeses").value(14))
                    .andExpect(jsonPath("$.historicoFormulas[0].formulaNome").value("NAN 2"))
                    .andExpect(jsonPath("$.historicoFormulas[0].avaliacoes").value(3));
        }

        @Test
        @DisplayName("o sexo vem da pessoa, não da última avaliação")
        void sexoVemDaPessoa() throws Exception {
            // No eroERP vinha da avaliação, porque Pessoa não tinha o campo —
            // criança sem avaliação nenhuma ficava sem curva para comparar.
            mockMvc.perform(get("/pediatria/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sexo").value("FEMININO"))
                    .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                    .andExpect(jsonPath("$.idadeMesesAtual").value(14));
        }

        @Test
        @DisplayName("paciente de outro cliente devolve 404")
        void isolamento() throws Exception {
            mockMvc.perform(get("/pediatria/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Painel gerencial ────────────────────────────────────────────────

    @Nested
    @DisplayName("Painel gerencial")
    class Geral {

        @Test
        @DisplayName("conta avaliações e pacientes distintos")
        void indicadores() throws Exception {
            Pessoa outro = criarPaciente(tenantA, "Outra criança", Sexo.MASCULINO,
                    LocalDate.now().minusMonths(9));
            criarAvaliacao(paciente.getId(), 8, "9", "2026-08-10");
            criarAvaliacao(paciente.getId(), 9, "9.4", "2026-08-12");
            criarAvaliacao(outro.getId(), 9, "9", "2026-08-11");

            mockMvc.perform(get("/pediatria/dashboard")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("dias", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(3))
                    .andExpect(jsonPath("$.totalPacientes").value(2))
                    .andExpect(jsonPath("$.pacientesMaisAvaliados[0].pacienteNome")
                            .value("Criança do Painel"))
                    .andExpect(jsonPath("$.pacientesMaisAvaliados[0].avaliacoes").value(2));
        }

        @Test
        @DisplayName("média ignora quem não tem o valor, em vez de contar zero")
        void mediaIgnoraAusente() throws Exception {
            // Uma com estatura (tem IMC), outra sem. A média de IMC é da primeira
            // sozinha — contar a segunda como zero afundaria o indicador.
            criarAvaliacaoComEstatura(paciente.getId(), 8, "9", "70", "2026-08-10");
            criarAvaliacao(paciente.getId(), 9, "9", "2026-08-11");

            mockMvc.perform(get("/pediatria/dashboard")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("dias", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(2))
                    // 9 / 0,70² = 18,3673 → 18,37
                    .andExpect(jsonPath("$.imcMedio").value(18.37));
        }

        @Test
        @DisplayName("% de IMC adequado é exato, não busca de texto")
        void percentualAdequado() throws Exception {
            // 70 cm → IMC 18,37, dentro de 15,4–18,5 aos 8 meses: adequado
            criarAvaliacaoComEstatura(paciente.getId(), 8, "9", "70", "2026-08-10");
            // 60 cm → IMC 25,0, acima do P85: sobrepeso
            criarAvaliacaoComEstatura(paciente.getId(), 8, "9", "60", "2026-08-11");

            mockMvc.perform(get("/pediatria/dashboard")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("dias", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.percImcAdequado").value(50.0))
                    .andExpect(jsonPath("$.classifImcIdade[?(@.faixa == 'ADEQUADA')].quantidade")
                            .value(1))
                    .andExpect(jsonPath("$.classifImcIdade[?(@.faixa == 'ALTA')].quantidade")
                            .value(1));
        }

        @Test
        @DisplayName("as três faixas aparecem mesmo zeradas")
        void faixasSempreVisiveis() throws Exception {
            criarAvaliacaoComEstatura(paciente.getId(), 8, "9", "70", "2026-08-10");

            mockMvc.perform(get("/pediatria/dashboard")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("dias", "0"))
                    .andExpect(status().isOk())
                    // Esconder a fatia vazia faria parecer que ninguém está
                    // abaixo do peso.
                    .andExpect(jsonPath("$.classifPesoIdade.length()").value(3))
                    .andExpect(jsonPath("$.classifPesoIdade[?(@.faixa == 'BAIXA')].quantidade")
                            .value(0));
        }

        @Test
        @DisplayName("não enxerga avaliação de outro cliente")
        void isolamento() throws Exception {
            criarAvaliacao(paciente.getId(), 8, "9", "2026-08-10");

            mockMvc.perform(get("/pediatria/dashboard")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("dias", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                    .andExpect(jsonPath("$.totalPacientes").value(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    private void criarAvaliacao(UUID pacienteId, int meses, String peso, String data) throws Exception {
        gravar(pacienteId, meses, peso, null, data);
    }

    private void criarAvaliacaoComEstatura(UUID pacienteId, int meses, String peso,
                                           String estatura, String data) throws Exception {
        gravar(pacienteId, meses, peso, estatura, data);
    }

    private void gravar(UUID pacienteId, int meses, String peso,
                        String estatura, String data) throws Exception {
        String campoEstatura = estatura == null ? "" : "\"estatura\":%s,".formatted(estatura);
        mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "sexo":"FEMININO","idadeMeses":%d,"peso":%s,%s
                                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3}
                                """.formatted(pacienteId, data, meses, peso, campoEstatura, nan2)))
                .andExpect(status().isCreated());
    }

    private Pessoa criarPaciente(Tenant tenant, String nome, Sexo sexo, LocalDate nascimento) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        pessoa.setSexo(sexo);
        pessoa.setDataNascimento(nascimento);
        pessoa.getTiposCadastro().add(tipoCadastroRepository.findAll().stream()
                .filter(t -> "Paciente".equals(t.getNome()))
                .findFirst().orElseThrow());
        return pessoaRepository.save(pessoa);
    }

    private UUID formulaGlobal() {
        return formulaLacteaRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && "NAN 2".equals(f.getNome()))
                .findFirst().orElseThrow().getId();
    }
}

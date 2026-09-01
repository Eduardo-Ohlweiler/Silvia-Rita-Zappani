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

        /**
         * As seis linhas de âncora do docs/09 §4.3, nas TRÊS medidas.
         *
         * <p>Antes disto só uma âncora era conferida, e só no peso: um
         * deslocamento de uma linha na carga de um dos sexos passava pela
         * suíte inteira. Tabela de referência de crescimento infantil
         * deslocada é classificação errada, silenciosa.
         */
        @Test
        @DisplayName("as seis âncoras do docs/09 §4.3 conferem nas três medidas")
        void ancoras() throws Exception {
            conferirAncora("MASCULINO",  0, 2.900, 3.900,  47.900,  51.800, 12.200, 14.800);
            conferirAncora("MASCULINO", 12, 8.600, 10.800, 73.300,  78.200, 15.500, 18.300);
            conferirAncora("MASCULINO", 60, 16.000, 21.100, 105.200, 114.800, 13.900, 16.700);
            conferirAncora("FEMININO",   0, 2.800, 3.700,  47.200,  51.100, 12.100, 14.700);
            conferirAncora("FEMININO",   8, 7.000, 9.000,  66.300,  71.200, 15.400, 18.500);
            conferirAncora("FEMININO",  60, 15.700, 21.300, 104.500, 114.400, 13.800, 17.000);
        }

        /**
         * O degrau dos 25 meses (docs/09 §4.4): a OMS emenda comprimento
         * deitado com estatura em pé, e o salto é da fonte.
         *
         * <p><b>Não "corrigir".</b> Este teste existe para reprovar quem
         * suavizar a curva achando que é erro de digitação.
         */
        @Test
        @DisplayName("a descontinuidade dos 25 meses é preservada, não suavizada")
        void descontinuidadeDos25Meses() throws Exception {
            mockMvc.perform(get("/pediatria/curvas-oms")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("sexo", "MASCULINO")
                            .param("idadeMin", "24")
                            .param("idadeMax", "25"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].estaturaP15").value(84.600))
                    .andExpect(jsonPath("$[1].estaturaP15").value(84.700))
                    .andExpect(jsonPath("$[0].imcP15").value(14.500))
                    .andExpect(jsonPath("$[1].imcP15").value(14.800));
        }

        private void conferirAncora(String sexo, int idade,
                                    double pesoP15, double pesoP85,
                                    double estP15, double estP85,
                                    double imcP15, double imcP85) throws Exception {
            mockMvc.perform(get("/pediatria/curvas-oms")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("sexo", sexo)
                            .param("idadeMin", String.valueOf(idade))
                            .param("idadeMax", String.valueOf(idade)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idadeMeses").value(idade))
                    .andExpect(jsonPath("$[0].pesoP15").value(pesoP15))
                    .andExpect(jsonPath("$[0].pesoP85").value(pesoP85))
                    .andExpect(jsonPath("$[0].estaturaP15").value(estP15))
                    .andExpect(jsonPath("$[0].estaturaP85").value(estP85))
                    .andExpect(jsonPath("$[0].imcP15").value(imcP15))
                    .andExpect(jsonPath("$[0].imcP85").value(imcP85));
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

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

/**
 * Os três painéis da UTI adulto.
 *
 * <p>O que se testa aqui não é o número da nutrição — esse já tem gabarito em
 * {@code CalculoUtiTest}. É a promessa dos painéis: <b>somam o que foi gravado,
 * não recalculam</b>, <b>ausência não vira zero</b> e <b>nada atravessa o
 * tenant</b>.
 */
@DisplayName("Painéis da UTI adulto")
class UtiDashboardTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa paciente;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void cenario() {
        paciente = criarPaciente(tenantA, "Paciente do Painel", Sexo.MASCULINO);
    }

    // ─── Painel do paciente ──────────────────────────────────────────────

    @Nested
    @DisplayName("Painel do paciente")
    class Paciente {

        @Test
        @DisplayName("traz a última avaliação, a trajetória e o histórico de fórmulas")
        void completo() throws Exception {
            criarAvaliacao(hoje.minusDays(20), "68");
            criarAvaliacao(hoje.minusDays(10), "66");
            criarAvaliacao(hoje.minusDays(2), "64");

            mockMvc.perform(get("/uti/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pacienteNome").value("Paciente do Painel"))
                    .andExpect(jsonPath("$.sexo").value("MASCULINO"))
                    .andExpect(jsonPath("$.totalAvaliacoes").value(3))
                    .andExpect(jsonPath("$.primeiraAvaliacao").value(hoje.minusDays(20).toString()))
                    .andExpect(jsonPath("$.ultimaAvaliacao").value(hoje.minusDays(2).toString()))
                    // A última inteira, na mesma forma que a tela já lê
                    .andExpect(jsonPath("$.ultima.resultado.antropometria.pesoDeTrabalhoKg").value(64.0000))
                    // Ordenada por data: é o eixo X
                    .andExpect(jsonPath("$.evolucao.length()").value(3))
                    .andExpect(jsonPath("$.evolucao[0].pesoTrabalhoKg").value(68.0000))
                    .andExpect(jsonPath("$.evolucao[2].pesoTrabalhoKg").value(64.0000))
                    // Sai do retrato gravado, não do catálogo
                    .andExpect(jsonPath("$.historicoFormulas[0].formulaNome").value("Peptamen Intense"))
                    .andExpect(jsonPath("$.historicoFormulas[0].avaliacoes").value(3));
        }

        @Test
        @DisplayName("paciente sem avaliação nenhuma ainda se identifica, e não devolve null")
        void semAvaliacao() throws Exception {
            mockMvc.perform(get("/uti/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    // O sexo vem da pessoa: sem isso não há a que comparar
                    .andExpect(jsonPath("$.sexo").value("MASCULINO"))
                    .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                    .andExpect(jsonPath("$.totalDiasRegistrados").value(0))
                    // Estrutura vazia, não ausente — a tela não pode quebrar aqui
                    .andExpect(jsonPath("$.evolucao").isArray())
                    .andExpect(jsonPath("$.evolucao.length()").value(0))
                    .andExpect(jsonPath("$.historicoFormulas").isArray());
        }

        @Test
        @DisplayName("aponta quantos dias de acompanhamento existem do lado")
        void contaOsDias() throws Exception {
            UUID avaliacao = criarAvaliacao(hoje.minusDays(3), "68");
            criarDia(avaliacao, hoje.minusDays(2), "1200", "1800");
            criarDia(avaliacao, hoje.minusDays(1), "1300", "1500");

            mockMvc.perform(get("/uti/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDiasRegistrados").value(2))
                    .andExpect(jsonPath("$.primeiroDia").value(hoje.minusDays(2).toString()))
                    .andExpect(jsonPath("$.ultimoDia").value(hoje.minusDays(1).toString()));
        }

        @Test
        @DisplayName("paciente de outro cliente devolve 404")
        void isolamento() throws Exception {
            mockMvc.perform(get("/uti/painel-paciente")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("pacienteId", paciente.getId().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Painel de acompanhamento ────────────────────────────────────────

    @Nested
    @DisplayName("Painel de acompanhamento")
    class Acompanhamento {

        @Test
        @DisplayName("os dias vêm em ordem crescente, com os derivados do mapper")
        void dias() throws Exception {
            UUID avaliacao = criarAvaliacao(hoje.minusDays(5), "68");
            criarDia(avaliacao, hoje.minusDays(3), "1200", "1800");
            criarDia(avaliacao, hoje.minusDays(2), "1364", "2000");

            mockMvc.perform(get("/uti/painel-acompanhamento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDias").value(2))
                    .andExpect(jsonPath("$.diasSemAvaliacao").value(0))
                    .andExpect(jsonPath("$.dias[0].data").value(hoje.minusDays(3).toString()))
                    // Os mesmos derivados da tela do acompanhamento: 1200/1364
                    .andExpect(jsonPath("$.dias[0].percentualRecebido").value(87.98))
                    .andExpect(jsonPath("$.dias[1].percentualRecebido").value(100.00))
                    // Média das duas adesões, uma casa
                    .andExpect(jsonPath("$.adesaoMedia").value(94.0))
                    // A régua vem da avaliação vigente no último dia
                    .andExpect(jsonPath("$.ultimaAvaliacao").value(hoje.minusDays(5).toString()))
                    .andExpect(jsonPath("$.volumePrescritoNaAvaliacao").value(1364.0000));
        }

        @Test
        @DisplayName("dia sem o valor não entra na média — ausência não é zero")
        void ausenciaNaoEZero() throws Exception {
            UUID avaliacao = criarAvaliacao(hoje.minusDays(5), "68");
            // Um dia com diurese, um sem. A média tem de ser a do primeiro.
            criarDia(avaliacao, hoje.minusDays(3), "1200", "1800");
            criarDia(avaliacao, hoje.minusDays(2), "1200", null);

            mockMvc.perform(get("/uti/painel-acompanhamento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDias").value(2))
                    // 1800 / 68 / 24 = 1,1029 — e não a metade disso
                    .andExpect(jsonPath("$.diureseMediaMlKgHora").value(1.10));
        }

        @Test
        @DisplayName("sem dia nenhum, devolve estrutura vazia e médias ausentes")
        void vazio() throws Exception {
            mockMvc.perform(get("/uti/painel-acompanhamento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDias").value(0))
                    .andExpect(jsonPath("$.dias").isArray())
                    // Ausente, não zero: não há adesão a relatar
                    .andExpect(jsonPath("$.adesaoMedia").doesNotExist())
                    .andExpect(jsonPath("$.balancoAcumuladoMl").doesNotExist());
        }

        @Test
        @DisplayName("paciente de outro cliente devolve 404")
        void isolamento() throws Exception {
            mockMvc.perform(get("/uti/painel-acompanhamento")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Painel gerencial ────────────────────────────────────────────────

    @Nested
    @DisplayName("Painel gerencial")
    class Gerencial {

        @Test
        @DisplayName("conta, distribui e ordena o que foi gravado")
        void completo() throws Exception {
            UUID avaliacao = criarAvaliacao(hoje.minusDays(3), "68");
            criarDia(avaliacao, hoje.minusDays(2), "1200", "1800");

            mockMvc.perform(get("/uti/dashboard")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(1))
                    .andExpect(jsonPath("$.totalPacientes").value(1))
                    .andExpect(jsonPath("$.totalDiasRegistrados").value(1))
                    .andExpect(jsonPath("$.pesoTrabalhoMedio").value(68.00))
                    // IMC de 68 kg e 1,68 m: 24,0930 → eutrofia
                    .andExpect(jsonPath("$.imcMedio").value(24.09))
                    .andExpect(jsonPath("$.percEutrofia").value(100.0))
                    .andExpect(jsonPath("$.avaliacoesObesidade").value(0))
                    .andExpect(jsonPath("$.classifImcOms[0].rotulo").value("Eutrofia"))
                    .andExpect(jsonPath("$.classifImcOms[0].quantidade").value(1))
                    .andExpect(jsonPath("$.porFormula[0].rotulo").value("Peptamen Intense"))
                    .andExpect(jsonPath("$.pacientesMaisAvaliados[0].pacienteNome")
                            .value("Paciente do Painel"))
                    .andExpect(jsonPath("$.pacientesMaisAvaliados[0].dias").value(1))
                    // Série mensal contínua, terminando no mês corrente
                    .andExpect(jsonPath("$.porPeriodo").isArray());
        }

        @Test
        @DisplayName("tenant sem nada devolve estrutura vazia, nunca null")
        void tenantVazio() throws Exception {
            mockMvc.perform(get("/uti/dashboard")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                    .andExpect(jsonPath("$.classifImcOms").isArray())
                    .andExpect(jsonPath("$.porFormula").isArray())
                    .andExpect(jsonPath("$.pacientesMaisAvaliados").isArray())
                    // Média sem amostra é ausente, não zero
                    .andExpect(jsonPath("$.imcMedio").doesNotExist())
                    .andExpect(jsonPath("$.percEutrofia").doesNotExist());
        }

        @Test
        @DisplayName("a avaliação de um cliente não aparece no painel do outro")
        void isolamento() throws Exception {
            criarAvaliacao(hoje.minusDays(3), "68");

            mockMvc.perform(get("/uti/dashboard")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                    .andExpect(jsonPath("$.totalPacientes").value(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    private UUID criarAvaliacao(LocalDate data, String pesoKg) throws Exception {
        UUID peptamen = formulaEnteralRepository.findAll().stream()
                .filter(f -> f.ehGlobal() && "Peptamen Intense".equals(f.getNome()))
                .findFirst().map(FormulaEnteral::getId).orElseThrow();

        String corpo = mockMvc.perform(post("/uti/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "calculo":{"sexo":"MASCULINO","idadeAnos":59,"alturaCm":168,
                                            "pesoAtualKg":%s,"fase":"AGUDA",
                                            "formulaEnteralId":"%s","modoInfusao":"CONTINUA",
                                            "volumePorTempo":62,"tempo":22}}
                                """.formatted(paciente.getId(), data, pesoKg, peptamen)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    /** @param diureseMl {@code null} deixa o campo fora do corpo — é o caso da ausência */
    private void criarDia(UUID avaliacaoId, LocalDate data,
                          String volRecebido, String diureseMl) throws Exception {
        String diurese = diureseMl == null ? "" : ",\"diureseMl\":" + diureseMl;

        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                 "volRecebido24h":%s%s}
                                """.formatted(paciente.getId(), avaliacaoId, data,
                                volRecebido, diurese)))
                .andExpect(status().isCreated());
    }

    private Pessoa criarPaciente(Tenant tenant, String nome, Sexo sexo) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        pessoa.setSexo(sexo);
        pessoa.getTiposCadastro().add(tipoCadastroRepository.findAll().stream()
                .filter(t -> "Paciente".equals(t.getNome()))
                .findFirst().orElseThrow());
        return pessoaRepository.save(pessoa);
    }
}

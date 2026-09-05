package com.nutri.hospitalar.inicio;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A tela inicial — a lista de trabalho do dia.
 *
 * <p>O que se prova aqui é a promessa dela: <b>a lista tem saída</b> (quem
 * encerrou não aparece), <b>o alerta de adesão respeita a primeira semana</b>
 * (senão acenderia para quem está em progressão, que é o certo), e <b>nada
 * atravessa o tenant</b>.
 */
@DisplayName("Tela inicial")
class PainelInicialTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    private Pessoa paciente;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void cenario() {
        paciente = criarPaciente(tenantA, "Paciente da Ronda", Sexo.MASCULINO);
    }

    @Nested
    @DisplayName("A lista de trabalho")
    class ListaDeTrabalho {

        @Test
        @DisplayName("tenant vazio devolve estrutura vazia, nunca nulo")
        void tenantVazio() throws Exception {
            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ronda").isArray())
                    .andExpect(jsonPath("$.ronda").isEmpty())
                    .andExpect(jsonPath("$.adesaoBaixa").isArray())
                    .andExpect(jsonPath("$.mudancasDeFaixa").isArray())
                    .andExpect(jsonPath("$.haMaisTempoSemAvaliacao").isArray())
                    .andExpect(jsonPath("$.totalEmAcompanhamento").value(0));
        }

        /**
         * A lista é a RONDA, não a pendência.
         *
         * <p>Antes ela filtrava quem já tinha o registro do dia, e a tela
         * terminava o trabalho dizendo "1 de 1 registrados" com nada embaixo —
         * o número contradizendo a lista. Quem aparece continua aparecendo;
         * o que muda é a marca.
         */
        @Test
        @DisplayName("quem já registrou hoje CONTINUA na ronda, marcado como registrado")
        void comRegistroDeHojeApareceMarcado() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(2));
            criarDia(aval, hoje.minusDays(1), 1200);
            criarDia(aval, hoje, 1200);

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEmAcompanhamento").value(1))
                    .andExpect(jsonPath("$.registradosHoje").value(1))
                    // A lista NÃO esvazia: ela mostra a ronda inteira.
                    .andExpect(jsonPath("$.ronda.length()").value(1))
                    .andExpect(jsonPath("$.ronda[0].registradoHoje").value(true))
                    // Com o id do dia: é ele que o clique abre para editar.
                    // Sem isto a tela mandava todo mundo para "novo", e quem já
                    // tinha registro caía num formulário em branco.
                    .andExpect(jsonPath("$.ronda[0].registroDeHojeId").exists());
        }

        @Test
        @DisplayName("quem NÃO tem registro de hoje aparece, com os dias em aberto")
        void semRegistroDeHojePende() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(3));
            criarDia(aval, hoje.minusDays(2), 1200);

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registradosHoje").value(0))
                    .andExpect(jsonPath("$.ronda.length()").value(1))
                    .andExpect(jsonPath("$.ronda[0].pessoaNome").value("Paciente da Ronda"))
                    .andExpect(jsonPath("$.ronda[0].registradoHoje").value(false))
                    // Sem registro hoje não há id — e aí o clique abre um novo.
                    .andExpect(jsonPath("$.ronda[0].registroDeHojeId").doesNotExist())
                    .andExpect(jsonPath("$.ronda[0].diasSemRegistro").value(2));
        }

        /**
         * A razão de o encerramento existir. Sem ele o paciente que recebeu alta
         * seria cobrado todo dia, e a lista viraria um cemitério — pior que
         * lista nenhuma, porque quem usa aprende a ignorá-la.
         */
        @Test
        @DisplayName("acompanhamento encerrado SAI da lista — é a saída dela")
        void encerradoSaiDaLista() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(3));
            criarDia(aval, hoje.minusDays(2), 1200);

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.ronda.length()").value(1));

            mockMvc.perform(patch("/uti/avaliacoes/" + aval + "/encerramento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"encerradoEm":"%s","motivo":"ALTA_HOSPITALAR"}
                                    """.formatted(hoje.minusDays(1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.motivoEncerramentoDescricao").value("Alta hospitalar"));

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.ronda").isEmpty())
                    .andExpect(jsonPath("$.totalEmAcompanhamento").value(0))
                    // E some do "há mais tempo sem avaliação" também: senão ele
                    // acumularia dias para sempre, que é o mesmo fantasma.
                    .andExpect(jsonPath("$.haMaisTempoSemAvaliacao").isEmpty());
        }

        @Test
        @DisplayName("reabrir traz o paciente de volta")
        void reabrirVolta() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(3));
            criarDia(aval, hoje.minusDays(2), 1200);

            mockMvc.perform(patch("/uti/avaliacoes/" + aval + "/encerramento")
                    .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                    .contentType("application/json")
                    .content("""
                            {"encerradoEm":"%s","motivo":"OBITO"}
                            """.formatted(hoje)));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/uti/avaliacoes/" + aval + "/encerramento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encerradoEm").doesNotExist());

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.ronda.length()").value(1));
        }

        @Test
        @DisplayName("encerrar antes da avaliação é recusado, dizendo as duas datas")
        void encerramentoAntesDaAvaliacao() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(2));

            mockMvc.perform(patch("/uti/avaliacoes/" + aval + "/encerramento")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"encerradoEm":"%s","motivo":"ALTA_HOSPITALAR"}
                                    """.formatted(hoje.minusDays(5))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.erro").value(containsString("terminar antes de começar")));
        }
    }

    @Nested
    @DisplayName("O alerta de adesão")
    class Adesao {

        /**
         * 60 % no dia 3 é <b>conduta</b>: a ESPEN recomenda oferta hipocalórica
         * nos primeiros dias, e a própria planilha progride 25 · 50 · 75 · 100 %.
         * Acender aqui ensinaria a ignorar o alerta em três dias.
         */
        @Test
        @DisplayName("adesão baixa DENTRO da primeira semana não alerta")
        void baixaNaPrimeiraSemanaNaoAlerta() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(3));
            criarDia(aval, hoje.minusDays(1), 800);   // 800/1364 = 58,7 %

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adesaoBaixa").isEmpty());
        }

        @Test
        @DisplayName("a mesma adesão DEPOIS da primeira semana alerta, e diz o dia")
        void baixaDepoisDaPrimeiraSemanaAlerta() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(12));
            criarDia(aval, hoje.minusDays(1), 800);

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adesaoBaixa.length()").value(1))
                    .andExpect(jsonPath("$.adesaoBaixa[0].pessoaNome").value("Paciente da Ronda"))
                    .andExpect(jsonPath("$.adesaoBaixa[0].diasDeTerapia").value(12));
        }

        @Test
        @DisplayName("adesão boa não alerta, mesmo bem depois da primeira semana")
        void adesaoBoaNaoAlerta() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(12));
            criarDia(aval, hoje.minusDays(1), 1364);   // 100 %

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(jsonPath("$.adesaoBaixa").isEmpty());
        }
    }

    @Nested
    @DisplayName("Isolamento por tenant")
    class Isolamento {

        @Test
        @DisplayName("o painel do outro cliente não enxerga paciente nenhum daqui")
        void naoAtravessaTenant() throws Exception {
            UUID aval = criarAvaliacao(hoje.minusDays(3));
            criarDia(aval, hoje.minusDays(2), 800);

            mockMvc.perform(get("/painel-inicial")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ronda").isEmpty())
                    .andExpect(jsonPath("$.adesaoBaixa").isEmpty())
                    .andExpect(jsonPath("$.haMaisTempoSemAvaliacao").isEmpty())
                    .andExpect(jsonPath("$.totalEmAcompanhamento").value(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Peptamen Intense, 62 ml/h × 22 h = volume total de 1364 ml. */
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

    private void criarDia(UUID avaliacaoId, LocalDate data, int volRecebido) throws Exception {
        mockMvc.perform(post("/uti/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","avaliacaoId":"%s","data":"%s","volRecebido24h":%d}
                                """.formatted(paciente.getId(), avaliacaoId, data, volRecebido)))
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

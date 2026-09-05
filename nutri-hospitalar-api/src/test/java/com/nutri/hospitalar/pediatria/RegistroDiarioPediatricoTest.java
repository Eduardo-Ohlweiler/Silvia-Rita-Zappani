package com.nutri.hospitalar.pediatria;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O acompanhamento diário pediátrico ponta a ponta — docs/11.
 *
 * <p>O que este teste guarda, acima de tudo: <b>a idade anda</b>. Cada registro
 * classifica com a idade do seu próprio dia, e é isso que separa esta tabela da
 * de UTI.
 */
@DisplayName("Acompanhamento diário pediátrico")
class RegistroDiarioPediatricoTest extends AbstractIntegrationTest {

    @Autowired TipoCadastroRepository tipoCadastroRepository;

    /** F, 8 meses hoje — a criança do caso canônico de docs/09 e docs/11. */
    private Pessoa paciente;
    private UUID avaliacaoId;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void cenario() throws Exception {
        paciente = criarPaciente(tenantA, "Criança do A", Sexo.FEMININO,
                hoje.minusMonths(8));
        avaliacaoId = criarAvaliacao(hoje.minusDays(1));
    }

    // ─── O caso canônico de docs/11 §7 ───────────────────────────────────

    @Nested
    @DisplayName("Derivados")
    class Derivados {

        @Test
        @DisplayName("o vínculo com a avaliação paga as adequações, e a oferta é por 100 ml")
        void casoCanonico() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "pesoKg":9.2,"volRecebido24h":800,
                                     "tomadasPrevistas":8,"tomadasAceitas":7}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated())
                    // A idade sai da data de nascimento, não de campo digitado
                    .andExpect(jsonPath("$.derivados.idadeMeses").value(8))
                    // 800 / 880 × 100 — o prescrito da AVALIAÇÃO
                    .andExpect(jsonPath("$.derivados.percentualRecebido").value(90.91))
                    .andExpect(jsonPath("$.derivados.prescritoDeReferencia").value(880.0))
                    .andExpect(jsonPath("$.derivados.referenciaDoRecebido")
                            .value(containsString("avaliação")))
                    // 73,8 × 800 / 100. Por litro daria 59,04 — dez vezes menos
                    .andExpect(jsonPath("$.derivados.caloriasRecebidas").value(590.4))
                    .andExpect(jsonPath("$.derivados.proteinaRecebida").value(13.2))
                    // 590,4 / 723 e 13,2 / 11
                    .andExpect(jsonPath("$.derivados.adequacaoCalorica").value(81.66))
                    .andExpect(jsonPath("$.derivados.adequacaoProteica").value(120.00))
                    // 7 / 8
                    .andExpect(jsonPath("$.derivados.aceitacaoTomadas").value(87.50));
        }

        /**
         * <b>O caso do Theo Barbosa.</b> Os dois prescritos preenchidos e
         * diferentes — a combinação que nenhum teste cobria, e que a tela
         * exibia como "650 / 700 ml · 90,28 %".
         */
        @Test
        @DisplayName("com avaliação E prescrito digitado, o exibido é o da avaliação")
        void oPrescritoExibidoEOQueAAdesaoUsou() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "pesoKg":9.2,"volPrescrito24h":700,"volRecebido24h":800}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated())
                    // O digitado fica gravado: é registro do dia.
                    .andExpect(jsonPath("$.volPrescrito24h").value(700.0))
                    // Mas o denominador é o da avaliação — 800 / 880, não 800 / 700.
                    .andExpect(jsonPath("$.derivados.prescritoDeReferencia").value(880.0))
                    .andExpect(jsonPath("$.derivados.percentualRecebido").value(90.91));
        }

        @Test
        @DisplayName("a lista carrega o denominador e a procedência")
        void aListaTrazOPrescritoDeReferencia() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "pesoKg":9.2,"volPrescrito24h":700,"volRecebido24h":800}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].volPrescrito24h").value(700.0))
                    .andExpect(jsonPath("$.content[0].prescritoDeReferencia").value(880.0))
                    .andExpect(jsonPath("$.content[0].referenciaDoRecebido")
                            .value(containsString("avaliação")))
                    .andExpect(jsonPath("$.content[0].percentualRecebido").value(90.91));
        }

        /**
         * <b>O teste que prova que o registro classifica, e não copia.</b>
         *
         * <p>A avaliação é de 9,0 kg — exatamente o P85, que docs/09 §4.1
         * classifica como adequado pela assimetria do {@code >}. O dia é de
         * 9,2 kg, que passa do P85. Se sair "Peso adequado", o registro está
         * repetindo a classificação gravada na avaliação, e a série mostraria
         * uma criança parada onde ela mudou de faixa.
         */
        @Test
        @DisplayName("classifica com o peso DO DIA: 9,2 kg passa do P85 de 9,0")
        void classificaComOPesoDoDia() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s","pesoKg":9.2}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.derivados.pesoIdade.faixa").value("ALTA"))
                    .andExpect(jsonPath("$.derivados.pesoIdade.rotulo").value("Acima do peso"));
        }

        @Test
        @DisplayName("nada de derivado é gravado: nem percentual, nem adequação, nem idade")
        void nenhumDerivadoEColuna() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "pesoKg":9.2,"volRecebido24h":800}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated());

            // A entidade tem só as entradas — o resto sai na leitura.
            var registro = registroDiarioPediatricoRepository.findAll().getFirst();
            assertThat(registro.getPesoKg()).isEqualByComparingTo("9.2");
            assertThat(registro.getVolRecebido24h()).isEqualByComparingTo("800");
        }
    }

    // ─── Ausências explicadas ────────────────────────────────────────────

    @Nested
    @DisplayName("Ausências explicadas")
    class Ausencias {

        @Test
        @DisplayName("sem avaliação o dia é registrável, e as adequações faltam COM motivo")
        void semAvaliacao() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s",
                                     "pesoKg":9,"volPrescrito24h":1000,"volRecebido24h":800}
                                    """.formatted(paciente.getId(), hoje)))
                    .andExpect(status().isCreated())
                    // Cai no prescrito do dia, e DIZ que caiu
                    .andExpect(jsonPath("$.derivados.percentualRecebido").value(80.00))
                    .andExpect(jsonPath("$.derivados.referenciaDoRecebido")
                            .value(containsString("no dia")))
                    // Sem fórmula não há oferta, e o motivo manda vincular
                    .andExpect(jsonPath("$.derivados.caloriasRecebidas").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoOferta").exists())
                    .andExpect(jsonPath("$.derivados.adequacaoCalorica").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoAdequacaoCalorica").exists())
                    // Mas a classificação sai: ela só precisa de peso, idade e sexo
                    .andExpect(jsonPath("$.derivados.pesoIdade.faixa").exists());
        }

        @Test
        @DisplayName("sem data de nascimento não há idade nem classificação, e o motivo aponta o cadastro")
        void semDataDeNascimento() throws Exception {
            Pessoa semNascimento = criarPaciente(tenantA, "Sem nascimento",
                    Sexo.FEMININO, null);

            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":9,"estaturaCm":70}
                                    """.formatted(semNascimento.getId(), hoje)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.derivados.idadeMeses").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoIdade")
                            .value(containsString("nascimento")))
                    .andExpect(jsonPath("$.derivados.pesoIdade").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoEstadoNutricional").exists())
                    // O IMC não depende da idade: continua saindo
                    .andExpect(jsonPath("$.derivados.imc").exists());
        }

        @Test
        @DisplayName("acima de 60 meses o dia é registrável, mas não classifica — e diz por quê")
        void acimaDe60Meses() throws Exception {
            Pessoa crianca = criarPaciente(tenantA, "Seis anos",
                    Sexo.FEMININO, hoje.minusMonths(72));

            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":22,"estaturaCm":118}
                                    """.formatted(crianca.getId(), hoje)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.derivados.idadeMeses").value(72))
                    .andExpect(jsonPath("$.derivados.pesoIdade").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoEstadoNutricional")
                            .value(containsString("60 meses")));
        }

        @Test
        @DisplayName("sem peso do dia não há kcal/kg — mas as adequações continuam saindo")
        void semPesoDoDia() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                     "volRecebido24h":800}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.derivados.caloriasPorKg").doesNotExist())
                    .andExpect(jsonPath("$.derivados.motivoPorQuilo")
                            .value(containsString("peso")))
                    // As adequações vêm das metas, não do peso do dia
                    .andExpect(jsonPath("$.derivados.adequacaoCalorica").value(81.66));
        }
    }

    // ─── O que o cadastro recusa ─────────────────────────────────────────

    @Nested
    @DisplayName("O que o cadastro recusa")
    class Recusas {

        @Test
        @DisplayName("dois registros do mesmo paciente no mesmo dia: 409 dizendo qual dia")
        void diaRepetido() throws Exception {
            criarDia(hoje);

            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":9.3}
                                    """.formatted(paciente.getId(), hoje)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(containsString("Já existe")));
        }

        @Test
        @DisplayName("aceitar mais tomadas do que se previu: 409, não adequação acima de 100 %")
        void aceitasAcimaDePrevistas() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s",
                                     "tomadasPrevistas":6,"tomadasAceitas":8}
                                    """.formatted(paciente.getId(), hoje)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(containsString("aceitas")));
        }

        @Test
        @DisplayName("peso zero é erro de digitação, não medida")
        void pesoZero() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":0}
                                    """.formatted(paciente.getId(), hoje)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("data no futuro é recusada")
        void dataNoFuturo() throws Exception {
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":9}
                                    """.formatted(paciente.getId(), hoje.plusDays(1))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("apagar avaliação com dias vinculados: 409 dizendo quantos")
        void avaliacaoComDiasVinculados() throws Exception {
            criarDia(hoje);

            mockMvc.perform(delete("/pediatria/avaliacoes/" + avaliacaoId)
                            .header(AUTHORIZATION, autenticar(adminA.getEmail())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(containsString("1 dia")));
        }
    }

    // ─── Isolamento por tenant ───────────────────────────────────────────

    @Test
    @DisplayName("apagar remove o dia, e apagar de novo já não acha")
    void apagarODia() throws Exception {
        UUID id = criarDia(hoje);
        String comoA = autenticar(adminA.getEmail());

        mockMvc.perform(delete("/pediatria/registros-diarios/" + id)
                        .header(AUTHORIZATION, comoA))
                .andExpect(status().isNoContent());

        assertThat(registroDiarioPediatricoRepository.findById(id)).isEmpty();

        // O dia liberou a data: o um-por-dia não pode ficar preso a um registro
        // que não existe mais.
        mockMvc.perform(post("/pediatria/registros-diarios")
                        .header(AUTHORIZATION, comoA)
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","data":"%s","pesoKg":9.2}
                                """.formatted(paciente.getId(), hoje)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/pediatria/registros-diarios/" + id)
                        .header(AUTHORIZATION, comoA))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("Isolamento por tenant")
    class Isolamento {

        @Test
        @DisplayName("o registro de um tenant não existe para o outro")
        void naoAtravessaTenant() throws Exception {
            UUID id = criarDia(hoje);

            mockMvc.perform(get("/pediatria/registros-diarios/" + id)
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isNotFound());

            mockMvc.perform(put("/pediatria/registros-diarios/" + id)
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","data":"%s","pesoKg":9}
                                    """.formatted(paciente.getId(), hoje)))
                    .andExpect(status().isNotFound());
        }

        /**
         * O {@code DELETE} não tinha teste nenhum — nem feliz, nem cruzado.
         *
         * <p>É apagar <b>físico</b> de dado clínico: a linha some e nada
         * sobrevive dela. O código está correto ({@code findByIdAndTenantId}),
         * e é justamente por isso que o teste importa — sem ele, quem mexer
         * neste service amanhã não tem o que o avise se quebrar o isolamento
         * do verbo que destrói.
         */
        @Test
        @DisplayName("apagar de outro tenant é 404, e não apaga pela metade")
        void naoApagaDeOutroTenant() throws Exception {
            UUID id = criarDia(hoje);

            mockMvc.perform(delete("/pediatria/registros-diarios/" + id)
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isNotFound());

            // Continua lá: o 404 recusou, não removeu.
            assertThat(registroDiarioPediatricoRepository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("a lista do outro tenant não traz o registro")
        void listaNaoVaza() throws Exception {
            criarDia(hoje);

            mockMvc.perform(get("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ─── O painel de acompanhamento ──────────────────────────────────────

    @Nested
    @DisplayName("Painel de acompanhamento")
    class Painel {

        @Test
        @DisplayName("traz os dias em ordem, as médias e o crescimento do período")
        void painelDoPaciente() throws Exception {
            // Três dias, com peso subindo: 9,0 → 9,2 → 9,4.
            criarDiaCom(hoje.minusDays(2), "9.0", "800");
            criarDiaCom(hoje.minusDays(1), "9.2", "820");
            criarDiaCom(hoje, "9.4", "880");

            mockMvc.perform(get("/pediatria/registros-diarios/painel")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDias").value(3))
                    .andExpect(jsonPath("$.diasSemAvaliacao").value(0))
                    // Ordem CRESCENTE: é o eixo X do gráfico
                    .andExpect(jsonPath("$.dias[0].data").value(hoje.minusDays(2).toString()))
                    .andExpect(jsonPath("$.dias[2].data").value(hoje.toString()))
                    // O crescimento do período — o que só a pediatria tem
                    .andExpect(jsonPath("$.pesoInicialKg").value(9.000))
                    .andExpect(jsonPath("$.pesoFinalKg").value(9.400))
                    .andExpect(jsonPath("$.variacaoPesoKg").value(0.400))
                    // A régua contra a qual se compara
                    .andExpect(jsonPath("$.vet").value(723.0))
                    .andExpect(jsonPath("$.adequacaoCaloricaMedia").exists());
        }

        /**
         * Dia sem o valor não entra na média como zero — contar ausência como
         * zero faria a criança parecer pior do que está.
         */
        @Test
        @DisplayName("a média ignora o dia sem o valor, e não o conta como zero")
        void mediaIgnoraAusencia() throws Exception {
            criarDiaCom(hoje.minusDays(1), "9.0", "880");   // 100 % do prescrito
            // Segundo dia SEM volume recebido: não há percentual nenhum.
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s","pesoKg":9.1}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/pediatria/registros-diarios/painel")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDias").value(2))
                    // 100 e não 50: a média é do único dia que tem o valor.
                    .andExpect(jsonPath("$.adesaoMedia").value(100.00));
        }

        /**
         * A variação usa o primeiro e o último dia QUE TÊM peso — não os do
         * período. Um dia sem balança no fim zeraria o ganho de duas semanas.
         */
        @Test
        @DisplayName("a variação de peso usa os dias que têm peso, não as bordas do período")
        void variacaoUsaDiasComPeso() throws Exception {
            criarDiaCom(hoje.minusDays(2), "9.0", "800");
            criarDiaCom(hoje.minusDays(1), "9.5", "800");
            // Último dia sem peso medido — não pode apagar o ganho anterior.
            mockMvc.perform(post("/pediatria/registros-diarios")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .contentType("application/json")
                            .content("""
                                    {"pessoaId":"%s","avaliacaoId":"%s","data":"%s","volRecebido24h":800}
                                    """.formatted(paciente.getId(), avaliacaoId, hoje)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/pediatria/registros-diarios/painel")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pesoFinalKg").value(9.500))
                    .andExpect(jsonPath("$.variacaoPesoKg").value(0.500));
        }

        @Test
        @DisplayName("o painel do outro tenant não encontra o paciente")
        void naoAtravessaTenant() throws Exception {
            criarDia(hoje);

            mockMvc.perform(get("/pediatria/registros-diarios/painel")
                            .header(AUTHORIZATION, autenticar(adminB.getEmail()))
                            .param("pessoaId", paciente.getId().toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── A sugestão de avaliação ─────────────────────────────────────────

    @Nested
    @DisplayName("Sugestão de avaliação")
    class Sugestao {

        @Test
        @DisplayName("sugere a mais recente ATÉ a data, com o que basta para reconhecê-la")
        void sugereAMaisRecenteAteADat() throws Exception {
            mockMvc.perform(get("/pediatria/registros-diarios/avaliacao-sugerida")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString())
                            .param("data", hoje.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(avaliacaoId.toString()))
                    .andExpect(jsonPath("$.volumeTotal").value(880.0))
                    .andExpect(jsonPath("$.vet").value(723.0))
                    .andExpect(jsonPath("$.formulaNome").exists())
                    .andExpect(jsonPath("$.aviso").doesNotExist());
        }

        @Test
        @DisplayName("um dia ANTES da avaliação não a sugere, e avisa que o dia é registrável")
        void diaAnteriorNaoSugere() throws Exception {
            mockMvc.perform(get("/pediatria/registros-diarios/avaliacao-sugerida")
                            .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                            .param("pessoaId", paciente.getId().toString())
                            .param("data", hoje.minusDays(10).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.aviso").value(containsString("registrado")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Um dia com peso e volume à escolha. */
    private void criarDiaCom(LocalDate data, String peso, String volume) throws Exception {
        mockMvc.perform(post("/pediatria/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                 "pesoKg":%s,"volRecebido24h":%s}
                                """.formatted(paciente.getId(), avaliacaoId, data, peso, volume)))
                .andExpect(status().isCreated());
    }

    /** O dia canônico: 9,2 kg, 800 ml recebidos. */
    private UUID criarDia(LocalDate data) throws Exception {
        String corpo = mockMvc.perform(post("/pediatria/registros-diarios")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pessoaId":"%s","avaliacaoId":"%s","data":"%s",
                                 "pesoKg":9.2,"volRecebido24h":800}
                                """.formatted(paciente.getId(), avaliacaoId, data)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    /**
     * A avaliação do caso canônico de docs/09 §9: F, 8 meses, 9 kg, NAN 2,
     * 110 ml a cada 3 h → 880 ml/dia, VET 723, proteína 11 g/dia.
     */
    private UUID criarAvaliacao(LocalDate data) throws Exception {
        FormulaLactea nan2 = formulaLacteaRepository.findAll().stream()
                .filter(f -> f.getNome().toUpperCase().contains("NAN 2"))
                .findFirst()
                .orElseGet(() -> formulaLacteaRepository.findAll().getFirst());

        String corpo = mockMvc.perform(post("/pediatria/avaliacoes")
                        .header(AUTHORIZATION, autenticar(adminA.getEmail()))
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataAvaliacao":"%s",
                                 "sexo":"FEMININO","idadeMeses":8,"peso":9,
                                 "formulaLacteaId":"%s","volumeMl":110,"frequenciaHoras":3}
                                """.formatted(paciente.getId(), data, nan2.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
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
}

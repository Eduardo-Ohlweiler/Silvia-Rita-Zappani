package com.nutri.hospitalar.clinica;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.tenant.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
 * Fichas de anamnese — e, sobretudo, <b>o retrato da pergunta</b>.
 *
 * <p>Os testes de {@link Retrato} são os que sustentam a fatia: sem eles, o
 * módulo volta a ser o do eroERP, onde editar o rótulo de uma pergunta reescreve
 * o que fichas antigas parecem ter perguntado.
 */
@DisplayName("Fichas de anamnese")
class FichaAnamneseTest extends AbstractIntegrationTest {

    @Nested
    @DisplayName("Gravar e reabrir")
    class GravarEReabrir {

        @Test
        @DisplayName("USER grava e lê — é módulo de negócio")
        void userOpera() throws Exception {
            String token = autenticar("user.a@teste.local");
            Cenario c = cenario(token);

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDaFicha(c, "true", "Amendoim", "Normal")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.modeloNome").value("Modelo da casa"))
                    .andExpect(jsonPath("$.respostas.length()").value(4));
        }

        /**
         * O retrato completo vai na resposta, e é dele que a tela desenha a
         * ficha: seção, rótulo, tipo, opções, ordem e obrigatoriedade.
         */
        @Test
        @DisplayName("a resposta salva carrega o retrato inteiro da pergunta")
        void retratoCompleto() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String id = gravar(token, c, "true", "Amendoim", "Normal");

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.respostas[0].secao").value("Saúde"))
                    .andExpect(jsonPath("$.respostas[0].rotulo").value("Tem alergia alimentar?"))
                    .andExpect(jsonPath("$.respostas[0].tipo").value("CHECKBOX"))
                    .andExpect(jsonPath("$.respostas[0].ordem").value(1))
                    .andExpect(jsonPath("$.respostas[3].tipo").value("OPCOES"))
                    .andExpect(jsonPath("$.respostas[3].opcoes.length()").value(3))
                    .andExpect(jsonPath("$.modeloAlterado").value(false))
                    .andExpect(jsonPath("$.modeloRemovido").value(false));
        }

        /**
         * Uma caixa em branco não distingue "o paciente disse que não" de
         * "ninguém perguntou". Nulo tem de continuar nulo ao reabrir.
         */
        @Test
        @DisplayName("sim/não deixado em branco reabre em branco — não vira 'não'")
        void naoInformadoContinuaNaoInformado() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String id = gravar(token, c, null, "Amendoim", "Normal");

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.respostas[0].valor").doesNotExist());
        }

        @Test
        @DisplayName("a lista conta respondidas de total, e branco não conta")
        void contagemDaLista() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            gravar(token, c, null, "Amendoim", "Normal");

            mockMvc.perform(get("/fichas-anamnese").header(AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].totalPerguntas").value(4))
                    /* Três: o sim/não ficou em branco, e branco não é resposta. */
                    .andExpect(jsonPath("$.content[0].respondidas").value(3))
                    .andExpect(jsonPath("$.content[0].modeloNome").value("Modelo da casa"));
        }
    }

    @Nested
    @DisplayName("O retrato — o que sustenta a fatia")
    class Retrato {

        /**
         * <b>O teste central.</b> Se a ficha for desenhada a partir do modelo
         * vivo — como no eroERP —, o rótulo antigo some e este teste fica
         * vermelho.
         */
        @Test
        @DisplayName("editar o rótulo no modelo NÃO muda o que a ficha antiga perguntou")
        void rotuloAntigoPermanece() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            reescreverPrimeiraPergunta(token, c, "Tem alergia alimentar GRAVE?");

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    /* O que foi perguntado naquele dia, e não o que se pergunta hoje. */
                    .andExpect(jsonPath("$.respostas[0].rotulo").value("Tem alergia alimentar?"))
                    .andExpect(jsonPath("$.respostas[0].valor").value("true"))
                    /* E a tela é avisada de que o modelo não é mais aquele. */
                    .andExpect(jsonPath("$.modeloAlterado").value(true));
        }

        /**
         * Desativar uma pergunta para de fazê-la em fichas novas; não apaga o que
         * já foi respondido. No eroERP a resposta sumia da tela sem sumir do
         * banco — que é o pior dos dois mundos.
         */
        @Test
        @DisplayName("desativar a pergunta no modelo não some com a resposta já dada")
        void respostaDeCampoDesativadoSobrevive() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            desativarSegundaPergunta(token, c);

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.respostas.length()").value(4))
                    .andExpect(jsonPath("$.respostas[1].rotulo").value("Quais alergias?"))
                    .andExpect(jsonPath("$.respostas[1].valor").value("Amendoim"))
                    .andExpect(jsonPath("$.modeloAlterado").value(true));
        }

        @Test
        @DisplayName("acrescentar pergunta ao modelo também conta como alterado")
        void perguntaNovaTambemAvisa() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            acrescentarPergunta(token, c, "Pergunta que entrou depois");

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.respostas.length()").value(4))
                    .andExpect(jsonPath("$.modeloAlterado").value(true));
        }

        /**
         * A consequência boa do retrato: sem ele, este método esvaziaria
         * prontuários.
         */
        @Test
        @DisplayName("apagar o modelo NÃO esvazia a ficha — ela reabre inteira")
        void apagarModeloNaoEsvaziaFicha() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            mockMvc.perform(delete("/modelos-ficha/" + c.modeloId).header(AUTHORIZATION, token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modeloId").doesNotExist())
                    .andExpect(jsonPath("$.modeloNome").value("Modelo da casa"))
                    .andExpect(jsonPath("$.modeloRemovido").value(true))
                    .andExpect(jsonPath("$.respostas.length()").value(4))
                    .andExpect(jsonPath("$.respostas[0].rotulo").value("Tem alergia alimentar?"))
                    .andExpect(jsonPath("$.respostas[0].valor").value("true"));

            /* E a listagem continua dizendo de qual modelo a ficha veio. */
            mockMvc.perform(get("/fichas-anamnese").header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.content[0].modeloNome").value("Modelo da casa"));
        }

        @Test
        @DisplayName("regravar atualiza o retrato — o modelo volta a fechar com a ficha")
        void regravarAtualizaORetrato() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            reescreverPrimeiraPergunta(token, c, "Tem alergia alimentar GRAVE?");

            /* Os ids das perguntas não mudam ao reescrever o rótulo. */
            mockMvc.perform(put("/fichas-anamnese/" + fichaId)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDaFicha(c, "false", "Nenhuma", "Constipado")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.respostas[0].rotulo").value("Tem alergia alimentar GRAVE?"))
                    .andExpect(jsonPath("$.respostas[0].valor").value("false"))
                    .andExpect(jsonPath("$.modeloAlterado").value(false));
        }

        @Test
        @DisplayName("trocar o modelo da ficha descarta as respostas antigas")
        void trocarModeloRecomeca() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            String fichaId = gravar(token, c, "true", "Amendoim", "Normal");

            String outroModelo = modeloDeUmaPerguntaSo(token);

            mockMvc.perform(put("/fichas-anamnese/" + fichaId)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s",
                                     "modeloId":"%s","respostas":[]}
                                    """.formatted(c.pacienteId, LocalDate.now(), outroModelo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.respostas.length()").value(1))
                    .andExpect(jsonPath("$.modeloNome").value("Modelo de uma pergunta"));
        }
    }

    @Nested
    @DisplayName("O que o servidor recusa")
    class Recusa {

        /** "Campo obrigatório" numa ficha de vinte perguntas manda procurar qual. */
        @Test
        @DisplayName("obrigatória sem resposta é 400, e a mensagem nomeia a pergunta")
        void obrigatoriaSemResposta() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s",
                                     "modeloId":"%s","respostas":[]}
                                    """.formatted(c.pacienteId, LocalDate.now(), c.modeloId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("Objetivo da consulta")));
        }

        @Test
        @DisplayName("opção fora da lista declarada é 400")
        void opcaoForaDaLista() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDaFicha(c, "true", "Amendoim", "Fantasia")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("Hábito intestinal")));
        }

        @Test
        @DisplayName("sim/não com texto no lugar de true/false é 400")
        void checkboxComTexto() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDaFicha(c, "talvez", "Amendoim", "Normal")))
                    .andExpect(status().isBadRequest());
        }

        /**
         * A irmã da armadilha do fuso: data de calendário tratada como instante
         * erra por um dia, e aqui o erro é para o futuro — então ele bloqueia.
         */
        @Test
        @DisplayName("data futura é 400")
        void dataFutura() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s",
                                     "modeloId":"%s","respostas":[]}
                                    """.formatted(c.pacienteId, LocalDate.now().plusDays(1), c.modeloId)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("responder a pergunta de outro modelo é 404")
        void respostaDeOutroModelo() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            UUID alheio = primeiraPerguntaDe(token, modeloDeUmaPerguntaSo(token));

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s","modeloId":"%s",
                                     "respostas":[{"campoId":"%s","valor":"x"}]}
                                    """.formatted(c.pacienteId, LocalDate.now(), c.modeloId, alheio)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Isolamento entre clientes")
    class Isolamento {

        @Test
        @DisplayName("paciente de outro tenant é 404")
        void pacienteDeOutroTenant() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Cenario c = cenario(token);
            Pessoa daB = criarPaciente(tenantB, "Paciente da B");

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s",
                                     "modeloId":"%s","respostas":[]}
                                    """.formatted(daB.getId(), LocalDate.now(), c.modeloId)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a ficha de outro tenant é 404 e não aparece na listagem")
        void fichaDeOutroTenant() throws Exception {
            String tokenA = autenticar("admin.a@teste.local");
            Cenario c = cenario(tokenA);
            String fichaId = gravar(tokenA, c, "true", "Amendoim", "Normal");

            String tokenB = autenticar("admin.b@teste.local");

            mockMvc.perform(get("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, tokenB))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/fichas-anamnese").header(AUTHORIZATION, tokenB))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("apagar a ficha de outro tenant é 404; a própria some")
        void apagar() throws Exception {
            String tokenA = autenticar("admin.a@teste.local");
            Cenario c = cenario(tokenA);
            String fichaId = gravar(tokenA, c, "true", "Amendoim", "Normal");

            mockMvc.perform(delete("/fichas-anamnese/" + fichaId)
                            .header(AUTHORIZATION, autenticar("admin.b@teste.local")))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/fichas-anamnese/" + fichaId).header(AUTHORIZATION, tokenA))
                    .andExpect(status().isNoContent());

            assertThat(fichaAnamneseRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("gravar ficha com modelo de outro tenant é 404")
        void modeloDeOutroTenant() throws Exception {
            String tokenA = autenticar("admin.a@teste.local");
            Cenario c = cenario(tokenA);

            Pessoa pacienteB = criarPaciente(tenantB, "Paciente da B");

            mockMvc.perform(post("/fichas-anamnese")
                            .header(AUTHORIZATION, autenticar("admin.b@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"pacienteId":"%s","dataPreenchimento":"%s",
                                     "modeloId":"%s","respostas":[]}
                                    """.formatted(pacienteB.getId(), LocalDate.now(), c.modeloId)))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Cenário ──────────────────────────────────────────────────────────────

    /** O que uma ficha precisa: um paciente e um modelo com as quatro perguntas. */
    private record Cenario(UUID pacienteId, String modeloId,
                           UUID alergia, UUID quais, UUID objetivo, UUID habito) {}

    private Cenario cenario(String token) throws Exception {
        Pessoa paciente = criarPaciente(tenantA, "Maria de Teste");

        String corpo = mockMvc.perform(post("/modelos-ficha")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo da casa","campos":[
                                  {"secao":"Saúde","rotulo":"Tem alergia alimentar?","tipo":"CHECKBOX"},
                                  {"secao":"Saúde","rotulo":"Quais alergias?","tipo":"TEXTO"},
                                  {"secao":"Objetivo","rotulo":"Objetivo da consulta","tipo":"TEXTO_LONGO","obrigatorio":true},
                                  {"secao":"Hábitos","rotulo":"Hábito intestinal","tipo":"OPCOES",
                                   "opcoes":["Normal","Constipado","Diarreico"]}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(corpo);
        JsonNode campos = json.get("campos");

        return new Cenario(
                paciente.getId(),
                json.get("id").asText(),
                UUID.fromString(campos.get(0).get("id").asText()),
                UUID.fromString(campos.get(1).get("id").asText()),
                UUID.fromString(campos.get(2).get("id").asText()),
                UUID.fromString(campos.get(3).get("id").asText()));
    }

    private String corpoDaFicha(Cenario c, String alergia, String quais, String habito) {
        String respostaAlergia = alergia == null
                ? "{\"campoId\":\"%s\"}".formatted(c.alergia)
                : "{\"campoId\":\"%s\",\"valor\":\"%s\"}".formatted(c.alergia, alergia);

        return """
                {"pacienteId":"%s","dataPreenchimento":"%s","modeloId":"%s","respostas":[
                  %s,
                  {"campoId":"%s","valor":"%s"},
                  {"campoId":"%s","valor":"Emagrecimento"},
                  {"campoId":"%s","valor":"%s"}]}
                """.formatted(c.pacienteId, LocalDate.now(), c.modeloId,
                respostaAlergia, c.quais, quais, c.objetivo, c.habito, habito);
    }

    private String gravar(String token, Cenario c, String alergia,
                          String quais, String habito) throws Exception {
        String corpo = mockMvc.perform(post("/fichas-anamnese")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(corpoDaFicha(c, alergia, quais, habito)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("id").asText();
    }

    /** Mantém os ids das quatro perguntas e troca só o texto da primeira. */
    private void reescreverPrimeiraPergunta(String token, Cenario c, String novoRotulo)
            throws Exception {
        mockMvc.perform(put("/modelos-ficha/" + c.modeloId)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo da casa","campos":[
                                  {"id":"%s","secao":"Saúde","rotulo":"%s","tipo":"CHECKBOX"},
                                  {"id":"%s","secao":"Saúde","rotulo":"Quais alergias?","tipo":"TEXTO"},
                                  {"id":"%s","secao":"Objetivo","rotulo":"Objetivo da consulta","tipo":"TEXTO_LONGO","obrigatorio":true},
                                  {"id":"%s","secao":"Hábitos","rotulo":"Hábito intestinal","tipo":"OPCOES",
                                   "opcoes":["Normal","Constipado","Diarreico"]}]}
                                """.formatted(c.alergia, novoRotulo, c.quais, c.objetivo, c.habito)))
                .andExpect(status().isOk());
    }

    private void desativarSegundaPergunta(String token, Cenario c) throws Exception {
        mockMvc.perform(put("/modelos-ficha/" + c.modeloId)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo da casa","campos":[
                                  {"id":"%s","secao":"Saúde","rotulo":"Tem alergia alimentar?","tipo":"CHECKBOX"},
                                  {"id":"%s","secao":"Saúde","rotulo":"Quais alergias?","tipo":"TEXTO","ativo":false},
                                  {"id":"%s","secao":"Objetivo","rotulo":"Objetivo da consulta","tipo":"TEXTO_LONGO","obrigatorio":true},
                                  {"id":"%s","secao":"Hábitos","rotulo":"Hábito intestinal","tipo":"OPCOES",
                                   "opcoes":["Normal","Constipado","Diarreico"]}]}
                                """.formatted(c.alergia, c.quais, c.objetivo, c.habito)))
                .andExpect(status().isOk());
    }

    private void acrescentarPergunta(String token, Cenario c, String rotulo) throws Exception {
        mockMvc.perform(put("/modelos-ficha/" + c.modeloId)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo da casa","campos":[
                                  {"id":"%s","secao":"Saúde","rotulo":"Tem alergia alimentar?","tipo":"CHECKBOX"},
                                  {"id":"%s","secao":"Saúde","rotulo":"Quais alergias?","tipo":"TEXTO"},
                                  {"id":"%s","secao":"Objetivo","rotulo":"Objetivo da consulta","tipo":"TEXTO_LONGO","obrigatorio":true},
                                  {"id":"%s","secao":"Hábitos","rotulo":"Hábito intestinal","tipo":"OPCOES",
                                   "opcoes":["Normal","Constipado","Diarreico"]},
                                  {"secao":"Extra","rotulo":"%s","tipo":"TEXTO"}]}
                                """.formatted(c.alergia, c.quais, c.objetivo, c.habito, rotulo)))
                .andExpect(status().isOk());
    }

    private String modeloDeUmaPerguntaSo(String token) throws Exception {
        String corpo = mockMvc.perform(post("/modelos-ficha")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo de uma pergunta","campos":[
                                  {"rotulo":"Única pergunta","tipo":"TEXTO"}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("id").asText();
    }

    private UUID primeiraPerguntaDe(String token, String modeloId) throws Exception {
        String corpo = mockMvc.perform(get("/modelos-ficha/" + modeloId)
                        .header(AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(corpo).get("campos").get(0).get("id").asText());
    }

    private Pessoa criarPaciente(Tenant tenant, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        return pessoaRepository.save(pessoa);
    }
}

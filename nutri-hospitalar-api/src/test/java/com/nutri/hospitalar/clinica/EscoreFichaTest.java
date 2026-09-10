package com.nutri.hospitalar.clinica;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.entity.ModeloFicha;
import com.nutri.hospitalar.clinica.mapper.OpcoesJson;
import com.nutri.hospitalar.clinica.repository.CampoFichaRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.tenant.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As escalas pontuadas de ponta a ponta — do seed à ficha gravada (docs/13).
 *
 * <p>O que os testes unitários de {@code EscalaNutricionalTest} não alcançam é
 * justamente o que quebra na prática: o casamento de {@code pontos} com
 * {@code opcoes} por índice, o <b>retrato do ponto</b> e o congelamento, que é o
 * que impede uma pontuação corrigida no catálogo — ou uma data de nascimento
 * acertada no cadastro — de reescrever, em silêncio, o resultado de um
 * prontuário.
 */
@DisplayName("Escore nas fichas de anamnese")
class EscoreFichaTest extends AbstractIntegrationTest {

    /** A coleção do modelo é preguiçosa, e o teste roda fora de transação. */
    @Autowired private CampoFichaRepository campoFichaRepository;

    private static final String MNA = "MNA — Mini Nutritional Assessment";
    private static final String NRS = "NRS-2002 — Triagem de risco nutricional";

    /**
     * O gabarito de docs/13 §2.6, expresso como o <b>índice da opção</b> escolhida
     * em cada uma das dezoito perguntas. Fecha em 9,0 · 11,5 · 20,5.
     */
    private static final int[] GABARITO_MNA =
            {1, 2, 2, 1, 1, 1, 0, 0, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1};

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MNA gravada")
    class Mna {

        @Test
        @DisplayName("o gabarito de docs/13 fecha 9,0 · 11,5 · 20,5 na ficha salva")
        void gabaritoNaFichaSalva() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String id = gravarMna(token, GABARITO_MNA);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escore.escala").value("MNA"))
                    .andExpect(jsonPath("$.escore.grupos[0].subtotal").value(9.0))
                    .andExpect(jsonPath("$.escore.grupos[0].classificacao.rotulo")
                            .value("Sob risco de desnutrição"))
                    .andExpect(jsonPath("$.escore.grupos[1].subtotal").value(11.5))
                    .andExpect(jsonPath("$.escore.total").value(20.5))
                    .andExpect(jsonPath("$.escore.classificacao.tom").value("ATENCAO"))
                    /* A MNA classifica e não prescreve conduta — ver MnaEscala. */
                    .andExpect(jsonPath("$.escore.conclusao").doesNotExist());
        }

        /**
         * O ponto entra no <b>retrato</b>, ao lado do rótulo e do tipo. É esta
         * coluna que faz o escore de uma ficha antiga continuar sendo o dela.
         */
        @Test
        @DisplayName("cada resposta guarda o ponto e o bloco que valeram")
        void pontoNoRetratoDaResposta() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String id = gravarMna(token, GABARITO_MNA);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.respostas[0].pontos").value(1.0))
                    .andExpect(jsonPath("$.respostas[0].grupoEscore").value("TRIAGEM"))
                    .andExpect(jsonPath("$.respostas[10].pontos").value(0.5))
                    .andExpect(jsonPath("$.respostas[10].grupoEscore").value("GLOBAL"));
        }

        @Test
        @DisplayName("a listagem traz o total, a classificação e o tom")
        void colunasPlanasNaListagem() throws Exception {
            String token = autenticar("admin.a@teste.local");
            gravarMna(token, GABARITO_MNA);

            mockMvc.perform(get("/fichas-anamnese").header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.content[0].escoreTotal").value(20.5))
                    .andExpect(jsonPath("$.content[0].escoreClassificacao")
                            .value("Sob risco de desnutrição"))
                    .andExpect(jsonPath("$.content[0].escoreTom").value("ATENCAO"));
        }

        /**
         * <b>Soma parcial de escala é escore errado.</b> Metade da MNA respondida
         * não pode virar um total menor: ela vira ausência com o motivo escrito, e
         * o motivo nomeia perguntas.
         */
        @Test
        @DisplayName("ficha pela metade grava escore nulo com motivo, e não um total menor")
        void metadeNaoViraTotalMenor() throws Exception {
            String token = autenticar("admin.a@teste.local");
            List<CampoFicha> campos = camposDe(MNA);

            /* Só as seis da triagem. */
            String respostas = respostas(campos.subList(0, 6), GABARITO_MNA);
            String id = gravarFicha(token, idDe(MNA), pacienteDe(1945), respostas);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.grupos[0].subtotal").value(9.0))
                    .andExpect(jsonPath("$.escore.total").doesNotExist())
                    .andExpect(jsonPath("$.escore.motivoAusencia")
                            .value(containsString("avaliação global")))
                    /*
                     * A avaliação global não teve NENHUMA resposta, e por isso a
                     * lista dela vem vazia: numa tela, listar as doze perguntas
                     * logo acima do formulário que faz essas doze é o
                     * questionário duas vezes. O motivo conta, sem enumerar.
                     */
                    .andExpect(jsonPath("$.escore.grupos[1].perguntasSemResposta.length()")
                            .value(0))
                    .andExpect(jsonPath("$.escore.grupos[1].motivoAusencia")
                            .value(containsString("Nenhuma das 12 perguntas")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("NRS-2002 gravada")
    class Nrs {

        @Test
        @DisplayName("o gabarito de docs/13 §3.8 fecha 5 de 7, em risco")
        void gabaritoNaFichaSalva() throws Exception {
            String token = autenticar("admin.a@teste.local");
            /* 81 anos na data de preenchimento: o ponto por idade entra. */
            String id = gravarNrs(token, pacienteDe(1945), "true", 2, 2);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.escala").value("NRS_2002"))
                    .andExpect(jsonPath("$.escore.ajusteIdade").value(1.0))
                    .andExpect(jsonPath("$.escore.ajusteIdadeDescricao")
                            .value(containsString("anos na data de preenchimento")))
                    .andExpect(jsonPath("$.escore.total").value(5.0))
                    .andExpect(jsonPath("$.escore.classificacao.rotulo").value("Em risco nutricional"))
                    .andExpect(jsonPath("$.escore.conclusao")
                            .value("Iniciar plano de terapia nutricional."));
        }

        /**
         * A porta fechada é <b>resultado</b>, não falta de dado: sai conclusão, e
         * não um traço com queixa de campo em branco.
         */
        @Test
        @DisplayName("quatro 'não' na pré-triagem concluem, sem escore e sem queixa")
        void portaFechadaConclui() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String id = gravarNrs(token, pacienteDe(1945), "false", null, null);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").doesNotExist())
                    .andExpect(jsonPath("$.escore.motivoAusencia").doesNotExist())
                    .andExpect(jsonPath("$.escore.classificacao.rotulo")
                            .value("Sem risco na triagem inicial"))
                    .andExpect(jsonPath("$.escore.conclusao")
                            .value(containsString("repetir a triagem semanalmente")));
        }

        /**
         * Sem data de nascimento o ponto por idade não pode ser decidido — e um
         * total um ponto menor cairia exatamente na faixa que decide iniciar
         * terapia nutricional.
         */
        @Test
        @DisplayName("paciente sem data de nascimento: escore não conclui, e diz por quê")
        void semDataDeNascimentoNaoConclui() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String id = gravarNrs(token, pacienteDe(null), "true", 2, 2);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").doesNotExist())
                    .andExpect(jsonPath("$.escore.motivoAusencia")
                            .value(containsString("data de nascimento")));
        }

        /**
         * <b>Um dia de diferença muda a conduta.</b>
         *
         * <p>Dois pacientes na mesma ficha de hoje: um faz 70 amanhã, o outro fez
         * 70 hoje. Com etapa 2 = 2 e etapa 3 = 0, o primeiro soma 2 (reavaliar
         * semanalmente) e o segundo soma 3 — o corte. Uma idade calculada
         * subtraindo anos, sem olhar mês e dia, daria 70 para os dois.
         *
         * <p>As datas são <b>relativas a hoje</b> de propósito: teste que crava um
         * dia do calendário quebra sozinho quando o calendário anda, e ensina a
         * ignorar vermelho — foi o que aconteceu com {@code adesaoMensalAusente}
         * em 01/09. E a data da ficha não pode ser amanhã: ela é
         * {@code @PastOrPresent}.
         */
        @Test
        @DisplayName("a idade conta mês e dia: um dia separa 'reavaliar' de 'em risco'")
        void idadeEDaDataDaFicha() throws Exception {
            String token = autenticar("admin.a@teste.local");
            LocalDate hoje = LocalDate.now();

            Pessoa fazAmanha = criarPaciente(tenantA, "Faz setenta amanhã",
                    hoje.plusDays(1).minusYears(70));
            Pessoa fezHoje = criarPaciente(tenantA, "Fez setenta hoje", hoje.minusYears(70));

            String comSessentaENove = gravarNrs(token, fazAmanha, "true", 2, 0, hoje);
            mockMvc.perform(get("/fichas-anamnese/" + comSessentaENove).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.ajusteIdade").value(0.0))
                    .andExpect(jsonPath("$.escore.total").value(2.0))
                    .andExpect(jsonPath("$.escore.classificacao.rotulo")
                            .value("Sem risco nutricional no momento"));

            String comSetenta = gravarNrs(token, fezHoje, "true", 2, 0, hoje);
            mockMvc.perform(get("/fichas-anamnese/" + comSetenta).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.ajusteIdade").value(1.0))
                    .andExpect(jsonPath("$.escore.total").value(3.0))
                    .andExpect(jsonPath("$.escore.classificacao.rotulo").value("Em risco nutricional"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("O escore congelado")
    class Congelado {

        /**
         * <b>O teste que sustenta a fatia.</b>
         *
         * <p>Clona a MNA, grava uma ficha, e depois <b>inverte a pontuação</b> de
         * uma pergunta no modelo — uma edição que a guarda de máximos aceita,
         * porque o maior ponto do bloco não muda. Reabrir a ficha tem de devolver
         * o escore <b>de antes</b>.
         *
         * <p>Verificado ao contrário: fazer o mapper recalcular em vez de ler
         * {@code escore_json} deixa este teste vermelho.
         */
        @Test
        @DisplayName("editar a pontuação do modelo não mexe no escore já gravado")
        void escoreCongeladoNaFichaSalva() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String clone = clonar(token, idDe(MNA));

            String id = gravarFicha(token, clone, pacienteDe(1945),
                    respostas(camposDoModelo(clone), GABARITO_MNA));

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").value(20.5));

            inverterPontosDaPerguntaB(token, clone);

            /*
             * A escolha é deliberada. Inverter a pergunta A não provaria nada: o
             * gabarito responde a opção do MEIO dela, e [0,1,2] invertido
             * continua valendo 1 ali — o teste passaria com defeito e sem defeito,
             * que é a definição de teste que não trava nada.
             *
             * A pergunta B tem quatro opções, o gabarito responde a terceira, e
             * [0,1,2,3] invertido faz essa resposta valer 1 em vez de 2. Se a
             * ficha lesse o catálogo, o total cairia para 19,5 e o retrato do
             * ponto viria 1,0.
             */
            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").value(20.5))
                    .andExpect(jsonPath("$.escore.grupos[0].subtotal").value(9.0))
                    .andExpect(jsonPath("$.respostas[1].pontos").value(2.0));
        }

        /**
         * {@code pessoa.data_nascimento} é editável fora da ficha. Corrigi-la não
         * pode fazer o ponto por idade da NRS entrar ou sair, calado, em toda
         * ficha antiga daquele paciente. É a segunda razão de {@code escore_json}
         * existir.
         */
        @Test
        @DisplayName("corrigir a data de nascimento não mexe no escore já gravado")
        void escoreNaoMudaComDataNascimentoCorrigida() throws Exception {
            String token = autenticar("admin.a@teste.local");
            Pessoa paciente = criarPaciente(tenantA, "Idoso de teste", LocalDate.of(1945, 3, 2));
            String id = gravarNrs(token, paciente, "true", 2, 2);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").value(5.0));

            paciente.setDataNascimento(LocalDate.of(1990, 3, 2));
            pessoaRepository.save(paciente);

            mockMvc.perform(get("/fichas-anamnese/" + id).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.escore.total").value(5.0))
                    .andExpect(jsonPath("$.escore.ajusteIdade").value(1.0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("O endpoint do escore ao vivo")
    class AoVivo {

        /**
         * A tela recalcula a cada pausa de digitação, e o formulário passa quase
         * toda a vida pela metade: <b>incompleto é estado, não erro</b>. Um 400
         * aqui viraria toast vermelho a cada tecla.
         */
        @Test
        @DisplayName("formulário pela metade devolve 200 com motivo, nunca 400")
        void naoRecusaIncompleto() throws Exception {
            String token = autenticar("admin.a@teste.local");
            List<CampoFicha> campos = camposDe(MNA);

            mockMvc.perform(post("/fichas-anamnese/escore")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"modeloId":"%s","pacienteId":"%s","dataPreenchimento":"%s",
                                     "respostas":[%s]}
                                    """.formatted(idDe(MNA), pacienteDe(1945).getId(),
                                    LocalDate.now(),
                                    itensDe(campos.subList(0, 3), GABARITO_MNA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").doesNotExist())
                    .andExpect(jsonPath("$.motivoAusencia").isNotEmpty())
                    .andExpect(jsonPath("$.grupos[0].motivoAusencia")
                            .value(containsString("Faltam responder")));
        }

        @Test
        @DisplayName("formulário completo devolve o mesmo número que a gravação")
        void mesmoNumeroDaGravacao() throws Exception {
            String token = autenticar("admin.a@teste.local");

            mockMvc.perform(post("/fichas-anamnese/escore")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"modeloId":"%s","pacienteId":"%s","dataPreenchimento":"%s",
                                     "respostas":[%s]}
                                    """.formatted(idDe(MNA), pacienteDe(1945).getId(),
                                    LocalDate.now(), itensDe(camposDe(MNA), GABARITO_MNA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(20.5));
        }

        /** Sem escala não há painel a desenhar — e isso não é erro. */
        @Test
        @DisplayName("modelo descritivo devolve 204")
        void modeloSemEscalaDevolve204() throws Exception {
            String token = autenticar("admin.a@teste.local");

            mockMvc.perform(post("/fichas-anamnese/escore")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"modeloId":"%s","respostas":[]}
                                    """.formatted(idDe("Anamnese nutricional adulto"))))
                    .andExpect(status().isNoContent());
        }

        /**
         * A tela troca de modelo e o debounce ainda dispara com as respostas do
         * anterior por alguns milissegundos. Um 404 nesse instante viraria toast
         * vermelho por causa de uma corrida que ninguém percebeu — na gravação o
         * rigor continua inteiro, e é ele que tem teste próprio.
         */
        @Test
        @DisplayName("id de pergunta de outro modelo é ignorado aqui, e não vira 404")
        void ignoraPerguntaDeOutroModelo() throws Exception {
            String token = autenticar("admin.a@teste.local");

            mockMvc.perform(post("/fichas-anamnese/escore")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"modeloId":"%s","respostas":[{"campoId":"%s","valor":"Sim"}]}
                                    """.formatted(idDe(MNA), camposDe(NRS).get(0).getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").doesNotExist());
        }

        @Test
        @DisplayName("o modelo de outro tenant devolve 404, como em todo o resto")
        void modeloDeOutroTenantNaoVaza() throws Exception {
            String tokenA = autenticar("admin.a@teste.local");
            String proprio = criarModeloSimples(tokenA);

            mockMvc.perform(post("/fichas-anamnese/escore")
                            .header(AUTHORIZATION, autenticar("admin.b@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"modeloId":"%s","respostas":[]}""".formatted(proprio)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Guardas do catálogo pontuado")
    class Guardas {

        @Test
        @DisplayName("o clone preserva a escala, os pontos e os blocos")
        void clonarPreservaEscore() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String clone = clonar(token, idDe(MNA));

            mockMvc.perform(get("/modelos-ficha/" + clone).header(AUTHORIZATION, token))
                    .andExpect(jsonPath("$.doSistema").value(false))
                    .andExpect(jsonPath("$.escoreCodigo").value("MNA"))
                    .andExpect(jsonPath("$.escalaNome").value("MNA® — Mini Nutritional Assessment"))
                    .andExpect(jsonPath("$.campos[0].pontos.length()").value(3))
                    .andExpect(jsonPath("$.campos[0].grupoEscore").value("TRIAGEM"));
        }

        /**
         * <b>Rodar o limite contra os dados.</b> Apagar perguntas de uma MNA
         * clonada produziria um total máximo de 8 lido pelas faixas de 30 —
         * "desnutrido" para quem respondeu tudo, com cara de resultado.
         */
        @Test
        @DisplayName("remover perguntas de um modelo com escala é recusado, com frase")
        void blocoQuebradoERecusado() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String clone = clonar(token, idDe(MNA));
            List<CampoFicha> campos = camposDoModelo(clone);

            /* Sobram as cinco primeiras da triagem: o bloco passa a somar 11. */
            mockMvc.perform(put("/modelos-ficha/" + clone)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDoModelo("Meu MNA", campos.subList(0, 5), null)))
                    .andExpect(status().isBadRequest())
                    /* Cita a TRIAGEM, e não a avaliação global, porque a ordem
                       dos blocos é a da escala — mensagem que muda de bloco a
                       cada execução faz quem lê achar que o sistema está confuso,
                       e não a edição. */
                    .andExpect(jsonPath("$.erro").value(containsString("TRIAGEM")))
                    .andExpect(jsonPath("$.erro").value(containsString("14")));
        }

        @Test
        @DisplayName("pontos em número diferente das opções é recusado, nomeando a pergunta")
        void pontosDeTamanhoDiferenteRecusado() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String clone = clonar(token, idDe(MNA));
            List<CampoFicha> campos = camposDoModelo(clone);

            mockMvc.perform(put("/modelos-ficha/" + clone)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDoModelo("Meu MNA", campos, campos.get(0).getId() + "|[0,1]")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("3 opções e 2 pontos")));
        }

        /**
         * Um modelo de três perguntas que se declarasse MNA receberia as faixas
         * da MNA sobre um total de cinco pontos. O campo simplesmente não é
         * aceito de fora.
         */
        @Test
        @DisplayName("escoreCodigo enviado na criação é ignorado")
        void escoreCodigoNaoVemDoCliente() throws Exception {
            String token = autenticar("admin.a@teste.local");

            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Minha MNA falsa","escoreCodigo":"MNA","campos":[
                                      {"rotulo":"Uma pergunta","tipo":"TEXTO"}]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.escoreCodigo").doesNotExist());
        }

        @Test
        @DisplayName("pontuar sem escala é recusado")
        void pontuarSemEscalaERecusado() throws Exception {
            String token = autenticar("admin.a@teste.local");

            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Questionário com pontos","campos":[
                                      {"rotulo":"Quanto?","tipo":"OPCOES","opcoes":["A","B"],
                                       "pontos":[0,1],"grupoEscore":"TRIAGEM"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro")
                            .value(containsString("não aplica uma escala pontuada")));
        }

        /**
         * Somar N opções marcadas abre uma aritmética que nenhuma publicação
         * define — proibido no banco e no service, com frase.
         */
        @Test
        @DisplayName("opção múltipla pontuada é recusada")
        void multiplasOpcoesPontuadoRecusado() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String clone = clonar(token, idDe(MNA));
            List<CampoFicha> campos = camposDoModelo(clone);

            mockMvc.perform(put("/modelos-ficha/" + clone)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content(corpoDoModelo("Meu MNA", campos,
                                    campos.get(0).getId() + "|MULTIPLAS")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("opção única")));
        }

        @Test
        @DisplayName("os dois modelos do sistema recusam edição, como os outros três")
        void osDoisNovosSaoImutaveis() throws Exception {
            String token = autenticar("admin.a@teste.local");
            for (String nome : List.of(MNA, NRS)) {
                mockMvc.perform(put("/modelos-ficha/" + idDe(nome))
                                .header(AUTHORIZATION, token)
                                .contentType("application/json")
                                .content("""
                                        {"nome":"Sequestrado","campos":[
                                          {"rotulo":"Uma","tipo":"TEXTO"}]}"""))
                        .andExpect(status().isNotFound());
            }
        }
    }

    // ── Preparo ──────────────────────────────────────────────────────────────

    private ModeloFicha modelo(String nome) {
        return modeloFichaRepository.findAll().stream()
                .filter(m -> m.getNome().equals(nome))
                .findFirst().orElseThrow();
    }

    private String idDe(String nome) {
        return modelo(nome).getId().toString();
    }

    private List<CampoFicha> camposDe(String nome) {
        return campoFichaRepository.findByModeloIdOrderByOrdemAsc(modelo(nome).getId());
    }

    private List<CampoFicha> camposDoModelo(String modeloId) {
        return campoFichaRepository.findByModeloIdOrderByOrdemAsc(java.util.UUID.fromString(modeloId));
    }

    /** Um item {@code {"campoId":…,"valor":…}} por pergunta, pelo índice da opção. */
    private String itensDe(List<CampoFicha> campos, int[] indices) {
        return IntStream.range(0, campos.size())
                .mapToObj(i -> {
                    CampoFicha campo = campos.get(i);
                    String opcao = OpcoesJson.paraLista(campo.getOpcoes()).get(indices[i]);
                    return "{\"campoId\":\"%s\",\"valor\":\"%s\"}".formatted(campo.getId(), opcao);
                })
                .collect(Collectors.joining(","));
    }

    private String respostas(List<CampoFicha> campos, int[] indices) {
        return itensDe(campos, indices);
    }

    private Pessoa criarPaciente(Tenant tenant, String nome, LocalDate nascimento) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(tenant);
        pessoa.setNome(nome);
        pessoa.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        pessoa.setDataNascimento(nascimento);
        return pessoaRepository.save(pessoa);
    }

    private Pessoa pacienteDe(Integer anoDeNascimento) {
        return criarPaciente(tenantA, "Paciente " + anoDeNascimento,
                anoDeNascimento == null ? null : LocalDate.of(anoDeNascimento, 3, 2));
    }

    private String gravarFicha(String token, String modeloId, Pessoa paciente,
                               String respostas) throws Exception {
        return gravarFicha(token, modeloId, paciente, respostas, LocalDate.now());
    }

    private String gravarFicha(String token, String modeloId, Pessoa paciente,
                               String respostas, LocalDate data) throws Exception {
        String corpo = mockMvc.perform(post("/fichas-anamnese")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"pacienteId":"%s","dataPreenchimento":"%s","modeloId":"%s",
                                 "respostas":[%s]}
                                """.formatted(paciente.getId(), data, modeloId, respostas)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("id").asText();
    }

    private String gravarMna(String token, int[] indices) throws Exception {
        return gravarFicha(token, idDe(MNA), pacienteDe(1945), respostas(camposDe(MNA), indices));
    }

    private String gravarNrs(String token, Pessoa paciente, String preTriagem,
                             Integer estado, Integer gravidade) throws Exception {
        return gravarNrs(token, paciente, preTriagem, estado, gravidade, LocalDate.now());
    }

    /**
     * @param preTriagem valor do PRIMEIRO sim/não; os outros três vão "false". Um
     *                   "true" basta para abrir a porta, que é o que o
     *                   instrumento manda
     */
    private String gravarNrs(String token, Pessoa paciente, String preTriagem,
                             Integer estado, Integer gravidade, LocalDate data) throws Exception {
        List<CampoFicha> campos = camposDe(NRS);
        StringBuilder itens = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            itens.append("{\"campoId\":\"%s\",\"valor\":\"%s\"},"
                    .formatted(campos.get(i).getId(), i == 0 ? preTriagem : "false"));
        }
        if (estado != null) itens.append(item(campos.get(4), estado)).append(',');
        if (gravidade != null) itens.append(item(campos.get(5), gravidade));

        String corpo = itens.toString().replaceAll(",$", "");
        return gravarFicha(token, idDe(NRS), paciente, corpo, data);
    }

    private String item(CampoFicha campo, int indice) {
        return "{\"campoId\":\"%s\",\"valor\":\"%s\"}"
                .formatted(campo.getId(), OpcoesJson.paraLista(campo.getOpcoes()).get(indice));
    }

    private String clonar(String token, String modeloId) throws Exception {
        String corpo = mockMvc.perform(post("/modelos-ficha/" + modeloId + "/clonar")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    private String criarModeloSimples(String token) throws Exception {
        String corpo = mockMvc.perform(post("/modelos-ficha")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Modelo só da A","campos":[
                                  {"rotulo":"Uma pergunta","tipo":"TEXTO"}]}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    /**
     * Devolve o modelo inteiro em JSON, com uma alteração pontual opcional:
     * {@code "<id>|[0,1]"} troca os pontos daquela pergunta, e
     * {@code "<id>|MULTIPLAS"} troca o tipo dela.
     */
    private String corpoDoModelo(String nome, List<CampoFicha> campos, String alteracao) {
        String alvo = alteracao == null ? null : alteracao.split("\\|")[0];
        String mudanca = alteracao == null ? null : alteracao.split("\\|")[1];

        String lista = campos.stream().map(c -> {
            boolean eu = c.getId().toString().equals(alvo);
            String tipo = eu && "MULTIPLAS".equals(mudanca) ? "MULTIPLAS_OPCOES" : c.getTipo().name();
            String pontos = eu && mudanca != null && mudanca.startsWith("[")
                    ? mudanca : c.getPontos();

            return """
                    {"id":"%s","secao":"%s","rotulo":"%s","tipo":"%s","opcoes":%s,
                     "pontos":%s,"grupoEscore":"%s"}"""
                    .formatted(c.getId(), c.getSecao(), c.getRotulo().replace("\"", "'"),
                            tipo, c.getOpcoes() == null ? "null" : c.getOpcoes(),
                            pontos == null ? "null" : pontos, c.getGrupoEscore());
        }).collect(Collectors.joining(","));

        return "{\"nome\":\"%s\",\"campos\":[%s]}".formatted(nome, lista);
    }

    /**
     * Inverte a pontuação da pergunta <b>B</b>: {@code [0,1,2,3]} vira
     * {@code [3,2,1,0]}.
     *
     * <p>A guarda de máximos aceita, porque o maior ponto do bloco continua 3 e a
     * triagem continua somando 14 — é uma edição legítima de catálogo. Mas a
     * resposta já gravada passaria a valer 1 em vez de 2 se a ficha lesse o
     * catálogo em vez do retrato. É exatamente a edição que o retrato existe para
     * neutralizar.
     */
    private void inverterPontosDaPerguntaB(String token, String modeloId) throws Exception {
        List<CampoFicha> campos = camposDoModelo(modeloId);
        mockMvc.perform(put("/modelos-ficha/" + modeloId)
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content(corpoDoModelo("Meu MNA", campos,
                                campos.get(1).getId() + "|[3,2,1,0]")))
                .andExpect(status().isOk());
    }

    private JsonNode json(String corpo) throws Exception {
        return objectMapper.readTree(corpo);
    }
}

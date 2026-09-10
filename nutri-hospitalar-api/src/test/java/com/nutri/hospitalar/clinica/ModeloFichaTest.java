package com.nutri.hospitalar.clinica;

import com.nutri.hospitalar.AbstractIntegrationTest;
import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.repository.CampoFichaRepository;
import com.nutri.hospitalar.clinica.entity.ModeloFicha;
import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
import com.nutri.hospitalar.clinica.escore.EscalaNutricional;
import com.nutri.hospitalar.clinica.escore.EscalaRegistry;
import com.nutri.hospitalar.clinica.mapper.OpcoesJson;
import com.nutri.hospitalar.clinica.mapper.PontosJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modelos de ficha — o catálogo híbrido, a imutabilidade do que é do sistema, e
 * o clonar que torna essa imutabilidade suportável.
 */
@DisplayName("Modelos de ficha de anamnese")
class ModeloFichaTest extends AbstractIntegrationTest {

    /**
     * As perguntas são lidas por aqui, e não por {@code modelo.getCampos()}: a
     * coleção é preguiçosa e o teste roda fora de transação. O projeto não usa
     * {@code @Transactional} em teste — abrir a exceção aqui esconderia, atrás de
     * um rollback, o que o endpoint realmente gravou.
     */
    @Autowired private CampoFichaRepository campoFichaRepository;

    private java.util.List<CampoFicha> camposDe(ModeloFicha modelo) {
        return campoFichaRepository.findByModeloIdOrderByOrdemAsc(modelo.getId());
    }


    @Nested
    @DisplayName("Carga das migrations 030 e 031")
    class Carga {

        @Test
        @DisplayName("os cinco modelos entram, e todos são do sistema")
        void cincoDoSistema() {
            List<ModeloFicha> todos = modeloFichaRepository.findAll();

            assertThat(todos).hasSize(5);
            assertThat(todos).allMatch(ModeloFicha::ehGlobal);
            assertThat(todos).extracting(ModeloFicha::getNome).containsExactlyInAnyOrder(
                    "Anamnese nutricional adulto",
                    "Anamnese nutricional pediátrica",
                    "Anamnese nutricional hospitalar",
                    "MNA — Mini Nutritional Assessment",
                    "NRS-2002 — Triagem de risco nutricional");
        }

        /**
         * Os três da fatia 12 são <b>descritivos</b>, e continuam sendo.
         *
         * <p>A migration 030 recusou semear instrumento pontuado por escrito, e a
         * fatia 13 não voltou atrás disso — ela abriu uma porta ao lado. Se
         * alguém pendurar pontuação num questionário descritivo, é aqui que
         * aparece.
         */
        @Test
        @DisplayName("os três da fatia 12 continuam sem escala, e os dois novos têm")
        void escalaSoNosDoisNovos() {
            assertThat(modeloFichaRepository.findAll())
                    .filteredOn(m -> m.getNome().startsWith("Anamnese nutricional"))
                    .allMatch(m -> m.getEscoreCodigo() == null);

            assertThat(modeloFichaRepository.findAll())
                    .filteredOn(ModeloFicha::temEscore)
                    .extracting(ModeloFicha::getEscoreCodigo)
                    .containsExactlyInAnyOrder("MNA", "NRS_2002");
        }

        /**
         * <b>Roda o limite contra os dados que o sistema distribui.</b>
         *
         * <p>Cada bloco do seed tem de somar exatamente o máximo que a publicação
         * declara — MNA 14 + 16, NRS 0 + 3 + 3. É o teste que reprova quem
         * apertar a régua depois: mexer numa faixa da {@code MnaEscala} sem mexer
         * no seed, ou vice-versa, fica vermelho aqui.
         */
        @Test
        @DisplayName("cada bloco do seed soma o máximo que a escala declara")
        void seedCoerenteEscore() {
            for (ModeloFicha modelo : modeloFichaRepository.findAll()) {
                if (!modelo.temEscore()) continue;

                EscalaNutricional escala = EscalaRegistry.de(modelo.getEscoreCodigo()).orElseThrow();
                Map<String, BigDecimal> somado = new HashMap<>();

                for (CampoFicha campo : camposDe(modelo)) {
                    List<BigDecimal> pontos = PontosJson.paraLista(campo.getPontos());

                    if (!pontos.isEmpty()) {
                        assertThat(campo.getTipo()).as("tipo de %s", campo.getRotulo())
                                .isEqualTo(TipoCampoFicha.OPCOES);
                        assertThat(pontos).as("cardinalidade de %s", campo.getRotulo())
                                .hasSameSizeAs(OpcoesJson.paraLista(campo.getOpcoes()));
                        assertThat(campo.getGrupoEscore()).as("bloco de %s", campo.getRotulo())
                                .isNotBlank();
                    }
                    if (campo.getGrupoEscore() != null) {
                        somado.merge(campo.getGrupoEscore(),
                                pontos.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                                BigDecimal::add);
                    }
                }

                assertThat(somado.keySet()).as("blocos de %s", modelo.getNome())
                        .isEqualTo(escala.maximoPorGrupo().keySet());

                escala.maximoPorGrupo().forEach((grupo, maximo) ->
                        assertThat(somado.get(grupo)).as("máximo do bloco %s de %s",
                                        grupo, modelo.getNome())
                                .isEqualByComparingTo(maximo));
            }
        }

        @Test
        @DisplayName("todo modelo semeado tem perguntas, e toda pergunta de opção tem opções")
        void seedCoerente() {
            for (ModeloFicha modelo : modeloFichaRepository.findAll()) {
                var campos = camposDe(modelo);
                assertThat(campos).as("perguntas de %s", modelo.getNome()).isNotEmpty();

                campos.forEach(campo -> {
                    if (campo.getTipo().exigeOpcoes())
                        assertThat(campo.getOpcoes()).as("opções de %s", campo.getRotulo()).isNotBlank();
                    else
                        assertThat(campo.getOpcoes()).as("opções de %s", campo.getRotulo()).isNull();
                });
            }
        }

        /**
         * O adulto não pede peso nem altura de propósito: o sistema já os coleta
         * na avaliação, e a mesma medida em dois lugares produz dois valores
         * divergentes para o mesmo paciente, sem dizer qual vale.
         */
        @Test
        @DisplayName("o modelo adulto não pede peso nem altura — a avaliação já os coleta")
        void adultoNaoDuplicaMedida() {
            ModeloFicha adulto = modeloFichaRepository.findAll().stream()
                    .filter(m -> m.getNome().equals("Anamnese nutricional adulto"))
                    .findFirst().orElseThrow();

            assertThat(camposDe(adulto)).extracting(c -> c.getRotulo().toLowerCase())
                    .noneMatch(r -> r.contains("peso atual") || r.startsWith("altura"));
        }

        @Test
        @DisplayName("os cinco aparecem para os dois tenants — são do sistema")
        void visiveisParaTodoTenant() throws Exception {
            for (String email : List.of("admin.a@teste.local", "admin.b@teste.local")) {
                mockMvc.perform(get("/modelos-ficha").header(AUTHORIZATION, autenticar(email)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalElements").value(5));
            }
        }
    }

    @Nested
    @DisplayName("O modelo do sistema é imutável")
    class Imutavel {

        private String idDoSistema() {
            return modeloFichaRepository.findAll().stream()
                    .filter(ModeloFicha::ehGlobal)
                    .findFirst().orElseThrow().getId().toString();
        }

        /**
         * 404 e não 403: 403 confirmaria a existência do registro a quem não
         * pode tocá-lo, e o resto do sistema responde 404 nessa situação.
         */
        @Test
        @DisplayName("editar devolve 404 — ele nem é encontrado para alteração")
        void editarNaoEncontra() throws Exception {
            mockMvc.perform(put("/modelos-ficha/" + idDoSistema())
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Sequestrado","campos":[
                                      {"rotulo":"Pergunta","tipo":"TEXTO"}]}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("inativar devolve 404")
        void inativarNaoEncontra() throws Exception {
            mockMvc.perform(patch("/modelos-ficha/" + idDoSistema() + "/ativo")
                            .param("ativo", "false")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("apagar devolve 404 — e os três continuam lá")
        void apagarNaoEncontra() throws Exception {
            mockMvc.perform(delete("/modelos-ficha/" + idDoSistema())
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local")))
                    .andExpect(status().isNotFound());

            /* Os cinco do sistema continuam lá — 030 semeou três, 031 mais dois. */
            assertThat(modeloFichaRepository.findAll()).hasSize(5);
        }

        @Test
        @DisplayName("mas ler funciona, e a resposta diz que é do sistema")
        void lerFunciona() throws Exception {
            mockMvc.perform(get("/modelos-ficha/" + idDoSistema())
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.doSistema").value(true));
        }
    }

    @Nested
    @DisplayName("Clonar")
    class Clonar {

        @Test
        @DisplayName("a cópia é do tenant, tem as mesmas perguntas e é editável")
        void copiaEditavel() throws Exception {
            String token = autenticar("admin.a@teste.local");

            ModeloFicha origem = modeloFichaRepository.findAll().stream()
                    .filter(m -> m.getNome().equals("Anamnese nutricional hospitalar"))
                    .findFirst().orElseThrow();
            int perguntasDaOrigem = camposDe(origem).size();

            String corpo = mockMvc.perform(post("/modelos-ficha/" + origem.getId() + "/clonar")
                            .header(AUTHORIZATION, token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.doSistema").value(false))
                    .andExpect(jsonPath("$.nome").value("Anamnese nutricional hospitalar (cópia)"))
                    .andReturn().getResponse().getContentAsString();

            String copiaId = objectMapper.readTree(corpo).get("id").asText();
            assertThat(objectMapper.readTree(corpo).get("campos")).hasSize(perguntasDaOrigem);

            /* E, ao contrário da origem, esta aceita ser alterada. */
            mockMvc.perform(put("/modelos-ficha/" + copiaId)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Beira-leito da Clinica A","campos":[
                                      {"secao":"Dieta","rotulo":"Aceita a dieta?","tipo":"CHECKBOX"}]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.campos.length()").value(1));
        }

        @Test
        @DisplayName("clonar duas vezes não esbarra na unique — o nome ganha sufixo")
        void doisClonesConvivem() throws Exception {
            String token = autenticar("admin.a@teste.local");
            String origemId = modeloFichaRepository.findAll().getFirst().getId().toString();

            mockMvc.perform(post("/modelos-ficha/" + origemId + "/clonar").header(AUTHORIZATION, token))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/modelos-ficha/" + origemId + "/clonar").header(AUTHORIZATION, token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value(containsString("(cópia 2)")));
        }

        @Test
        @DisplayName("clonar o modelo de outro tenant é 404")
        void clonarDeOutroTenantNaoAcha() throws Exception {
            String doTenantA = criarModeloSimples(autenticar("admin.a@teste.local"), "Só da A");

            mockMvc.perform(post("/modelos-ficha/" + doTenantA + "/clonar")
                            .header(AUTHORIZATION, autenticar("admin.b@teste.local")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Isolamento entre clientes")
    class Isolamento {

        @Test
        @DisplayName("o modelo criado por um tenant não aparece para o outro")
        void naoVazaNaListagem() throws Exception {
            criarModeloSimples(autenticar("admin.a@teste.local"), "Modelo particular da A");

            mockMvc.perform(get("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local")))
                    .andExpect(jsonPath("$.totalElements").value(6));

            /* Para a B, só os cinco do sistema. */
            mockMvc.perform(get("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.b@teste.local")))
                    .andExpect(jsonPath("$.totalElements").value(5));
        }

        @Test
        @DisplayName("nem no combo, nem por id")
        void naoVazaNoSelectNemPorId() throws Exception {
            String id = criarModeloSimples(autenticar("admin.a@teste.local"), "Modelo particular da A");
            String tokenB = autenticar("admin.b@teste.local");

            mockMvc.perform(get("/modelos-ficha/select").header(AUTHORIZATION, tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5));

            mockMvc.perform(get("/modelos-ficha/" + id).header(AUTHORIZATION, tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("o mesmo nome pode existir em dois clientes")
        void nomeIgualEmDoisClientes() throws Exception {
            criarModeloSimples(autenticar("admin.a@teste.local"), "Anamnese da casa");
            criarModeloSimples(autenticar("admin.b@teste.local"), "Anamnese da casa");
        }

        @Test
        @DisplayName("repetir o nome dentro do mesmo cliente é 409")
        void nomeRepetidoNoMesmoCliente() throws Exception {
            String token = autenticar("admin.a@teste.local");
            criarModeloSimples(token, "Anamnese da casa");

            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Anamnese da casa","campos":[
                                      {"rotulo":"Queixa","tipo":"TEXTO"}]}
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("As perguntas do modelo")
    class Perguntas {

        @Test
        @DisplayName("USER cria e lê — é módulo de negócio, não área administrativa")
        void userOpera() throws Exception {
            criarModeloSimples(autenticar("user.a@teste.local"), "Feito pelo USER");
        }

        /**
         * A ordem é a posição na lista, e não um número digitado: ordem à mão
         * produz duas perguntas com o número 3 e uma tela que desempata sozinha.
         */
        @Test
        @DisplayName("a ordem é a posição na lista, reatribuída pelo servidor")
        void ordemReatribuida() throws Exception {
            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Com ordem","campos":[
                                      {"rotulo":"Primeira","tipo":"TEXTO"},
                                      {"rotulo":"Segunda","tipo":"TEXTO"},
                                      {"rotulo":"Terceira","tipo":"TEXTO"}]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.campos[0].ordem").value(1))
                    .andExpect(jsonPath("$.campos[1].ordem").value(2))
                    .andExpect(jsonPath("$.campos[2].ordem").value(3));
        }

        @Test
        @DisplayName("a lista é o estado completo: a pergunta que não veio é removida")
        void listaEhEstadoCompleto() throws Exception {
            String token = autenticar("admin.a@teste.local");

            String corpo = mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Duas perguntas","campos":[
                                      {"rotulo":"Fica","tipo":"TEXTO"},
                                      {"rotulo":"Sai","tipo":"TEXTO"}]}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            var json = objectMapper.readTree(corpo);
            String id = json.get("id").asText();
            String idQueFica = json.get("campos").get(0).get("id").asText();

            mockMvc.perform(put("/modelos-ficha/" + id)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Duas perguntas","campos":[
                                      {"id":"%s","rotulo":"Fica","tipo":"TEXTO"}]}
                                    """.formatted(idQueFica)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.campos.length()").value(1))
                    .andExpect(jsonPath("$.campos[0].rotulo").value("Fica"));
        }

        @Test
        @DisplayName("pergunta de opção com menos de duas opções é 400, nomeando a pergunta")
        void opcaoSemOpcoes() throws Exception {
            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Sem opções","campos":[
                                      {"rotulo":"Hábito intestinal","tipo":"OPCOES","opcoes":["Normal"]}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("Hábito intestinal")));
        }

        @Test
        @DisplayName("opção pendurada em campo de texto é 400, e não silêncio")
        void opcoesEmCampoDeTexto() throws Exception {
            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Lixo de opção","campos":[
                                      {"rotulo":"Queixa","tipo":"TEXTO","opcoes":["A","B"]}]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("a mesma pergunta duas vezes na mesma seção é 400")
        void perguntaRepetida() throws Exception {
            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Repetida","campos":[
                                      {"secao":"Saúde","rotulo":"Fuma?","tipo":"CHECKBOX"},
                                      {"secao":"Saúde","rotulo":"fuma?","tipo":"CHECKBOX"}]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("modelo sem pergunta nenhuma é 400 — não há o que responder")
        void modeloSemPergunta() throws Exception {
            mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, autenticar("admin.a@teste.local"))
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Vazio","campos":[]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        /**
         * A busca do campo é no mapa dos já carregados, e não no banco,
         * justamente para que mandar o id de outro modelo não altere nada.
         */
        @Test
        @DisplayName("mandar o id de uma pergunta de outro modelo é 404")
        void perguntaDeOutroModelo() throws Exception {
            String token = autenticar("admin.a@teste.local");

            String outro = mockMvc.perform(post("/modelos-ficha")
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Outro modelo","campos":[
                                      {"rotulo":"Alheia","tipo":"TEXTO"}]}
                                    """))
                    .andReturn().getResponse().getContentAsString();
            String idAlheio = objectMapper.readTree(outro).get("campos").get(0).get("id").asText();

            String alvo = criarModeloSimples(token, "Modelo alvo");

            mockMvc.perform(put("/modelos-ficha/" + alvo)
                            .header(AUTHORIZATION, token)
                            .contentType("application/json")
                            .content("""
                                    {"nome":"Modelo alvo","campos":[
                                      {"id":"%s","rotulo":"Sequestrada","tipo":"TEXTO"}]}
                                    """.formatted(idAlheio)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String criarModeloSimples(String token, String nome) throws Exception {
        String corpo = mockMvc.perform(post("/modelos-ficha")
                        .header(AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"%s","campos":[
                                  {"secao":"Geral","rotulo":"Queixa principal","tipo":"TEXTO_LONGO"}]}
                                """.formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("id").asText();
    }
}

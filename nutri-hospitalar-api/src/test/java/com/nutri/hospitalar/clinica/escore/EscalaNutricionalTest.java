package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.dtos.GrupoEscoreDto;
import com.nutri.hospitalar.uti.enums.TomResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As duas escalas contra os gabaritos de {@code docs/13}.
 *
 * <p>Sem Spring e sem banco, como as classes de {@code uti/calculo/}: o que se
 * verifica aqui é aritmética e régua, e nenhuma delas depende de contexto.
 */
@DisplayName("Escalas nutricionais pontuadas")
class EscalaNutricionalTest {

    private final MnaEscala mna = new MnaEscala();
    private final Nrs2002Escala nrs = new Nrs2002Escala();

    // ── Montagem dos itens ──────────────────────────────────────────────────

    /** Pergunta que pontua, respondida, valendo {@code pontos}. */
    private ItemRespondido vale(String grupo, String rotulo, String pontos) {
        return new ItemRespondido(grupo, rotulo, true, new BigDecimal(pontos), true, "respondida");
    }

    /** Pergunta que pontua e ainda não foi respondida. */
    private ItemRespondido pendente(String grupo, String rotulo) {
        return new ItemRespondido(grupo, rotulo, true, null, false, null);
    }

    /** Sim/não da pré-triagem da NRS: responde e não pontua. */
    private ItemRespondido porta(String rotulo, String valor) {
        return new ItemRespondido(Nrs2002Escala.PRE_TRIAGEM, rotulo, false, null,
                valor != null, valor);
    }

    private GrupoEscoreDto grupo(EscoreDto escore, String nome) {
        return escore.grupos().stream().filter(g -> g.grupo().equals(nome))
                .findFirst().orElseThrow();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MNA®")
    class Mna {

        /** docs/13 §2.6, item a item. */
        private List<ItemRespondido> triagemDoGabarito() {
            return List.of(
                    vale(MnaEscala.TRIAGEM, "A. Diminuição da ingesta", "1"),
                    vale(MnaEscala.TRIAGEM, "B. Perda de peso", "2"),
                    vale(MnaEscala.TRIAGEM, "C. Mobilidade", "2"),
                    vale(MnaEscala.TRIAGEM, "D. Estresse ou doença aguda", "2"),
                    vale(MnaEscala.TRIAGEM, "E. Problemas neuropsicológicos", "1"),
                    vale(MnaEscala.TRIAGEM, "F. Índice de massa corporal", "1"));
        }

        private List<ItemRespondido> globalDoGabarito() {
            return List.of(
                    vale(MnaEscala.GLOBAL, "G. Vive em casa", "1"),
                    vale(MnaEscala.GLOBAL, "H. Mais de três medicamentos", "0"),
                    vale(MnaEscala.GLOBAL, "I. Lesões de pele", "1"),
                    vale(MnaEscala.GLOBAL, "J. Refeições por dia", "2"),
                    vale(MnaEscala.GLOBAL, "K. Leite, leguminosas e carne", "0.5"),
                    vale(MnaEscala.GLOBAL, "L. Fruta e hortícolas", "1"),
                    vale(MnaEscala.GLOBAL, "M. Copos de líquido", "0.5"),
                    vale(MnaEscala.GLOBAL, "N. Modo de se alimentar", "2"),
                    vale(MnaEscala.GLOBAL, "O. Acredita ter problema", "1"),
                    vale(MnaEscala.GLOBAL, "P. Saúde comparada", "1"),
                    vale(MnaEscala.GLOBAL, "Q. Perímetro braquial", "0.5"),
                    vale(MnaEscala.GLOBAL, "R. Perímetro da perna", "1"));
        }

        private List<ItemRespondido> gabarito() {
            List<ItemRespondido> todos = new ArrayList<>(triagemDoGabarito());
            todos.addAll(globalDoGabarito());
            return todos;
        }

        @Test
        @DisplayName("o gabarito de docs/13 §2.6 fecha em 9,0 · 11,5 · 20,5")
        void mnaGabaritoCompleto() {
            EscoreDto escore = mna.avaliar(gabarito(), null);

            assertThat(grupo(escore, MnaEscala.TRIAGEM).subtotal())
                    .isEqualByComparingTo("9.0");
            assertThat(grupo(escore, MnaEscala.TRIAGEM).classificacao().rotulo())
                    .isEqualTo("Sob risco de desnutrição");

            assertThat(grupo(escore, MnaEscala.GLOBAL).subtotal())
                    .isEqualByComparingTo("11.5");
            /* A avaliação global não tem faixa publicada isolada — e não se
               inventa uma. */
            assertThat(grupo(escore, MnaEscala.GLOBAL).classificacao()).isNull();

            assertThat(escore.total()).isEqualByComparingTo("20.5");
            assertThat(escore.totalMaximo()).isEqualByComparingTo("30");
            assertThat(escore.classificacao().rotulo()).isEqualTo("Sob risco de desnutrição");
            assertThat(escore.classificacao().tom()).isEqualTo(TomResultado.ATENCAO);
            assertThat(escore.motivoAusencia()).isNull();

            /* A MNA classifica e NÃO prescreve conduta. Escrever uma aqui seria
               acrescentar ao instrumento. */
            assertThat(escore.conclusao()).isNull();
            /* E não tem ajuste por idade — esse é da NRS. */
            assertThat(escore.ajusteIdade()).isNull();
        }

        /**
         * Os quatro meios-pontos somam 2,0 <b>exatos</b>, e o total sai com uma
         * casa: {@code 9} e {@code 9,0} têm de ser o mesmo número no JSON e na
         * folha.
         */
        @Test
        @DisplayName("os quatro meios-pontos somam 2,0 exatos, com uma casa")
        void mnaMeioPontoSoma() {
            List<ItemRespondido> quatroMeios = List.of(
                    vale(MnaEscala.GLOBAL, "K", "0.5"),
                    vale(MnaEscala.GLOBAL, "M", "0.5"),
                    vale(MnaEscala.GLOBAL, "P", "0.5"),
                    vale(MnaEscala.GLOBAL, "Q", "0.5"));

            BigDecimal soma = grupo(mna.avaliar(quatroMeios, null), MnaEscala.GLOBAL).subtotal();

            assertThat(soma).isEqualByComparingTo("2.0");
            assertThat(soma.toPlainString()).isEqualTo("2.0");
        }

        /**
         * A triagem é o instrumento inteiro na versão abreviada, e as faixas dela
         * são publicadas isoladamente: ela classifica mesmo sem a avaliação
         * global. O que <b>não</b> pode é o total sair de uma soma parcial.
         */
        @Test
        @DisplayName("triagem completa classifica sozinha; o total fica nulo com motivo")
        void mnaTriagemClassificaSozinha() {
            EscoreDto escore = mna.avaliar(triagemDoGabarito(), null);

            assertThat(grupo(escore, MnaEscala.TRIAGEM).subtotal()).isEqualByComparingTo("9.0");
            assertThat(grupo(escore, MnaEscala.TRIAGEM).classificacao().rotulo())
                    .isEqualTo("Sob risco de desnutrição");

            assertThat(escore.total()).isNull();
            assertThat(escore.classificacao()).isNull();
            assertThat(escore.motivoAusencia()).contains("avaliação global");
        }

        @Test
        @DisplayName("as três faixas da triagem e as três do total")
        void mnaFaixas() {
            assertThat(faixaDaTriagem("12")).isEqualTo("Estado nutricional normal");
            assertThat(faixaDaTriagem("11")).isEqualTo("Sob risco de desnutrição");
            assertThat(faixaDaTriagem("8")).isEqualTo("Sob risco de desnutrição");
            assertThat(faixaDaTriagem("7")).isEqualTo("Desnutrido");

            /* Os limites do total. docs/13 §2.4: 24 a 30 normal, 17 a 23,5 sob
               risco, MENOS de 17 desnutrido — 17 e 23,5 são os dois extremos
               fechados da faixa do meio, e 23,5 é o que obriga a comparação a
               ser decimal. */
            assertThat(faixaDoTotal("24", "0")).isEqualTo("Estado nutricional normal");
            assertThat(faixaDoTotal("14", "9.5")).isEqualTo("Sob risco de desnutrição"); // 23,5
            assertThat(faixaDoTotal("14", "3")).isEqualTo("Sob risco de desnutrição");   // 17
            assertThat(faixaDoTotal("13", "3.5")).isEqualTo("Desnutrido");               // 16,5
        }

        /** Uma pergunta só, valendo o total da triagem, para testar a faixa. */
        private String faixaDaTriagem(String pontos) {
            EscoreDto e = mna.avaliar(List.of(vale(MnaEscala.TRIAGEM, "T", pontos)), null);
            return grupo(e, MnaEscala.TRIAGEM).classificacao().rotulo();
        }

        private String faixaDoTotal(String triagem, String global) {
            EscoreDto e = mna.avaliar(List.of(
                    vale(MnaEscala.TRIAGEM, "T", triagem),
                    vale(MnaEscala.GLOBAL, "G", global)), null);
            return e.classificacao().rotulo();
        }

        /**
         * O motivo <b>nomeia</b> as perguntas. "Incompleto" numa ficha de dezoito
         * perguntas manda o profissional procurar qual.
         */
        @Test
        @DisplayName("o motivo nomeia as perguntas que faltam")
        void grupoIncompletoNomeiaAsPerguntas() {
            List<ItemRespondido> itens = new ArrayList<>(triagemDoGabarito().subList(0, 4));
            itens.add(pendente(MnaEscala.TRIAGEM, "E. Problemas neuropsicológicos"));
            itens.add(pendente(MnaEscala.TRIAGEM, "F. Índice de massa corporal"));

            GrupoEscoreDto triagem = grupo(mna.avaliar(itens, null), MnaEscala.TRIAGEM);

            assertThat(triagem.subtotal()).isNull();
            assertThat(triagem.motivoAusencia())
                    .contains("E. Problemas neuropsicológicos")
                    .contains("F. Índice de massa corporal");
            assertThat(triagem.perguntasSemResposta())
                    .containsExactly("E. Problemas neuropsicológicos", "F. Índice de massa corporal");
        }

        /**
         * Zero é ponto legítimo em quase todo item da MNA — então uma resposta que
         * não casa com opção nenhuma não pode valer zero. Ela conta como pendente
         * e aparece na frase, que é visível.
         */
        @Test
        @DisplayName("resposta que não vale ponto conta como pendente, e não como zero")
        void respostaOrfaNaoValeZero() {
            List<ItemRespondido> itens = new ArrayList<>(triagemDoGabarito().subList(0, 5));
            itens.add(new ItemRespondido(MnaEscala.TRIAGEM, "F. IMC", true, null, true, "seis"));

            GrupoEscoreDto triagem = grupo(mna.avaliar(itens, null), MnaEscala.TRIAGEM);

            assertThat(triagem.subtotal()).isNull();
            assertThat(triagem.perguntasSemResposta()).containsExactly("F. IMC");
        }

        /**
         * Um bloco que o modelo <b>não tem</b> não pode contar como respondido.
         *
         * <p>Este é o teste do defeito que a primeira versão desta classe tinha:
         * só a triagem no modelo, e o escore total saía 9,0 — classificado pelas
         * faixas de 30, isto é, "Desnutrido" para quem respondeu tudo o que havia
         * para responder.
         */
        @Test
        @DisplayName("bloco ausente do modelo não conta como completo, e diz isso")
        void blocoAusenteNaoSomaSozinho() {
            EscoreDto escore = mna.avaliar(triagemDoGabarito(), null);

            assertThat(escore.total()).isNull();
            assertThat(grupo(escore, MnaEscala.GLOBAL).subtotal()).isNull();
            assertThat(grupo(escore, MnaEscala.GLOBAL).motivoAusencia())
                    .contains("não tem as perguntas");
        }

        @Test
        @DisplayName("formulário em branco não vira escore zero")
        void mnaVazioNaoEZero() {
            EscoreDto escore = mna.avaliar(List.of(
                    pendente(MnaEscala.TRIAGEM, "A"),
                    pendente(MnaEscala.GLOBAL, "G")), null);

            assertThat(escore.total()).isNull();
            assertThat(escore.motivoAusencia()).isNotBlank();
            assertThat(grupo(escore, MnaEscala.TRIAGEM).subtotal()).isNull();
        }

        /**
         * Bloco <b>intocado</b> conta; bloco <b>começado</b> enumera.
         *
         * <p>Só apareceu no navegador: a ficha em branco imprimia as dezoito
         * perguntas dentro do painel de escore, logo acima do formulário que faz
         * exatamente essas dezoito. E a causa era um método órfão — {@code
         * intocado()} existia, testado por ninguém e chamado por ninguém, que é
         * como o módulo proteico ficou dois anos invisível.
         */
        @Test
        @DisplayName("bloco intocado conta as perguntas; bloco começado as enumera")
        void intocadoContaEComecadoEnumera() {
            List<ItemRespondido> nadaRespondido = List.of(
                    pendente(MnaEscala.TRIAGEM, "A. Ingesta"),
                    pendente(MnaEscala.TRIAGEM, "B. Perda de peso"),
                    pendente(MnaEscala.TRIAGEM, "C. Mobilidade"));

            GrupoEscoreDto intocado = grupo(mna.avaliar(nadaRespondido, null), MnaEscala.TRIAGEM);
            assertThat(intocado.perguntasSemResposta()).isEmpty();
            assertThat(intocado.motivoAusencia()).isEqualTo(
                    "Nenhuma das 3 perguntas deste bloco foi respondida.");

            List<ItemRespondido> umRespondido = List.of(
                    vale(MnaEscala.TRIAGEM, "A. Ingesta", "1"),
                    pendente(MnaEscala.TRIAGEM, "B. Perda de peso"),
                    pendente(MnaEscala.TRIAGEM, "C. Mobilidade"));

            GrupoEscoreDto comecado = grupo(mna.avaliar(umRespondido, null), MnaEscala.TRIAGEM);
            assertThat(comecado.perguntasSemResposta())
                    .containsExactly("B. Perda de peso", "C. Mobilidade");
            assertThat(comecado.motivoAusencia()).contains("Faltam responder");
        }

        /**
         * "Nenhuma das 1 perguntas" — a concordância que a NRS-2002 expõe, porque
         * as etapas 2 e 3 dela têm <b>uma</b> pergunta cada. Só apareceu no
         * navegador: nenhuma assertiva de número pega uma frase malfeita.
         */
        @Test
        @DisplayName("bloco de uma pergunta só fala no singular")
        void blocoDeUmaPerguntaFalaNoSingular() {
            GrupoEscoreDto um = grupo(
                    nrs.avaliar(List.of(pendente(Nrs2002Escala.ESTADO, "Estado nutricional")), null),
                    Nrs2002Escala.ESTADO);

            assertThat(um.motivoAusencia())
                    .isEqualTo("A pergunta deste bloco ainda não foi respondida.")
                    .doesNotContain("das 1");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("NRS-2002")
    class Nrs {

        private List<ItemRespondido> preTriagem(String... respostas) {
            List<ItemRespondido> itens = new ArrayList<>();
            String[] rotulos = {"IMC menor que 20,5", "Perda de peso", "Redução da ingestão",
                    "Doença grave"};
            for (int i = 0; i < rotulos.length; i++) {
                itens.add(porta(rotulos[i], i < respostas.length ? respostas[i] : null));
            }
            return itens;
        }

        private List<ItemRespondido> etapas(String estado, String gravidade) {
            return List.of(
                    vale(Nrs2002Escala.ESTADO, "Estado nutricional", estado),
                    vale(Nrs2002Escala.GRAVIDADE, "Gravidade da doença", gravidade));
        }

        private List<ItemRespondido> completo(String estado, String gravidade) {
            List<ItemRespondido> itens = new ArrayList<>(
                    preTriagem("true", "false", "false", "false"));
            itens.addAll(etapas(estado, gravidade));
            return itens;
        }

        /** docs/13 §3.8: estado 2 + gravidade 2 + idade 1 = 5, em risco. */
        @Test
        @DisplayName("o gabarito de docs/13 §3.8 fecha em 5 de 7, em risco")
        void nrsGabaritoCompleto() {
            EscoreDto escore = nrs.avaliar(completo("2", "2"), 81);

            assertThat(grupo(escore, Nrs2002Escala.ESTADO).subtotal()).isEqualByComparingTo("2.0");
            assertThat(grupo(escore, Nrs2002Escala.GRAVIDADE).subtotal()).isEqualByComparingTo("2.0");

            assertThat(escore.ajusteIdade()).isEqualByComparingTo("1.0");
            assertThat(escore.ajusteIdadeDescricao()).contains("81 anos");

            assertThat(escore.total()).isEqualByComparingTo("5.0");
            assertThat(escore.totalMaximo()).isEqualByComparingTo("7");
            assertThat(escore.classificacao().rotulo()).isEqualTo("Em risco nutricional");
            assertThat(escore.classificacao().tom()).isEqualTo(TomResultado.ATENCAO);
            assertThat(escore.conclusao()).isEqualTo("Iniciar plano de terapia nutricional.");
        }

        /**
         * A soma é publicada em parcelas para poder ser refeita à mão:
         * {@code 2 + 2 + 1 = 5}. Se o ajuste entrasse calado no total, quem
         * conferisse acharia 4 debaixo de um 5 impresso.
         */
        @Test
        @DisplayName("o total é exatamente a soma das parcelas publicadas")
        void nrsTotalEASomaDasParcelas() {
            EscoreDto escore = nrs.avaliar(completo("2", "2"), 81);

            BigDecimal recomposto = grupo(escore, Nrs2002Escala.ESTADO).subtotal()
                    .add(grupo(escore, Nrs2002Escala.GRAVIDADE).subtotal())
                    .add(escore.ajusteIdade());

            assertThat(recomposto).isEqualByComparingTo(escore.total());
        }

        /**
         * Quatro "não" encerram o instrumento — e isso é <b>resultado</b>, não
         * falta de dado. Por isso sai conclusão e classificação, e não um traço
         * com queixa de campo em branco.
         */
        @Test
        @DisplayName("pré-triagem toda 'não' conclui, e não reclama de dado faltando")
        void nrsPortaFechada() {
            EscoreDto escore = nrs.avaliar(preTriagem("false", "false", "false", "false"), 81);

            assertThat(escore.total()).isNull();
            assertThat(escore.motivoAusencia()).isNull();
            assertThat(escore.classificacao().rotulo()).isEqualTo("Sem risco na triagem inicial");
            assertThat(escore.conclusao()).contains("repetir a triagem semanalmente");

            assertThat(grupo(escore, Nrs2002Escala.PRE_TRIAGEM).classificacao().rotulo())
                    .isEqualTo("Nenhum critério presente");
        }

        /**
         * O defeito que a varredura no navegador achou: com a porta fechada, as
         * etapas 2 e 3 continuavam vivas.
         *
         * <p>Quem preenche as etapas <b>antes</b> de marcar os quatro "não" via
         * "Estado nutricional 2 de 3" ao lado de "nenhum critério" e de um total
         * vazio — dois números verdadeiros que não entram em conta nenhuma, à
         * espera de que quem confere os some. É a armadilha do denominador da
         * adesão: número exibido sem a regra que o governa.
         *
         * <p>As respostas continuam gravadas; o que some é o <b>ponto</b>.
         */
        @Test
        @DisplayName("porta fechada dispensa as etapas 2 e 3, mesmo respondidas")
        void portaFechadaDispensaAsEtapas() {
            List<ItemRespondido> itens = new ArrayList<>(
                    preTriagem("false", "false", "false", "false"));
            itens.addAll(etapas("2", "2"));

            EscoreDto escore = nrs.avaliar(itens, 81);

            for (String bloco : List.of(Nrs2002Escala.ESTADO, Nrs2002Escala.GRAVIDADE)) {
                GrupoEscoreDto etapa = grupo(escore, bloco);
                assertThat(etapa.dispensado()).isTrue();
                assertThat(etapa.subtotal()).isNull();
                assertThat(etapa.motivoAusencia()).contains("Não se aplica");
                assertThat(etapa.perguntasSemResposta()).isEmpty();
            }

            /* E o instrumento segue concluindo, não reclamando. */
            assertThat(escore.total()).isNull();
            assertThat(escore.motivoAusencia()).isNull();
            assertThat(escore.conclusao()).contains("repetir a triagem semanalmente");
        }

        /**
         * A guarda não pode ligar sozinha: com um critério presente as etapas
         * valem, e a soma tem de continuar exatamente a mesma de antes.
         */
        @Test
        @DisplayName("porta aberta não dispensa nada, e a soma não se move")
        void portaAbertaNaoDispensaNada() {
            EscoreDto escore = nrs.avaliar(completo("2", "2"), 81);

            assertThat(grupo(escore, Nrs2002Escala.ESTADO).dispensado()).isFalse();
            assertThat(grupo(escore, Nrs2002Escala.GRAVIDADE).dispensado()).isFalse();
            assertThat(escore.total()).isEqualByComparingTo("5.0");
        }

        /**
         * Pré-triagem pela metade não é porta fechada — e, portanto, não
         * dispensa etapa nenhuma. Sem esta distinção a tela desligaria as
         * seções no primeiro "não" e as religaria no clique seguinte.
         */
        @Test
        @DisplayName("pré-triagem incompleta não dispensa as etapas")
        void preTriagemIncompletaNaoDispensa() {
            EscoreDto escore = nrs.avaliar(preTriagem("false", "false"), 81);

            assertThat(grupo(escore, Nrs2002Escala.ESTADO).dispensado()).isFalse();
            assertThat(grupo(escore, Nrs2002Escala.GRAVIDADE).dispensado()).isFalse();
        }

        /**
         * Uma pré-triagem em branco <b>não</b> é uma pré-triagem toda "não": o
         * sim/não deste sistema tem três estados, e ausência não é negativa. Sem
         * essa distinção, o formulário recém-aberto se declararia sem risco.
         */
        @Test
        @DisplayName("pré-triagem em branco não fecha a porta sozinha")
        void nrsPreTriagemEmBrancoNaoConclui() {
            EscoreDto escore = nrs.avaliar(preTriagem(), 81);

            assertThat(escore.total()).isNull();
            assertThat(escore.classificacao()).isNull();
            assertThat(escore.conclusao()).isNull();
            assertThat(escore.motivoAusencia()).contains("quatro perguntas da pré-triagem");
        }

        @Test
        @DisplayName("com critério presente, o total exige as etapas 2 e 3")
        void nrsEtapasIncompletasNomeiamAEtapa() {
            List<ItemRespondido> itens = new ArrayList<>(
                    preTriagem("true", "false", "false", "false"));
            itens.add(vale(Nrs2002Escala.ESTADO, "Estado nutricional", "2"));
            itens.add(pendente(Nrs2002Escala.GRAVIDADE, "Gravidade da doença"));

            EscoreDto escore = nrs.avaliar(itens, 81);

            assertThat(escore.total()).isNull();
            assertThat(escore.motivoAusencia()).contains("gravidade da doença")
                    .doesNotContain("estado nutricional");
        }

        @Test
        @DisplayName("69 anos não pontua; 70 pontua")
        void nrsIdade69NaoPontuaE70Pontua() {
            EscoreDto aos69 = nrs.avaliar(completo("2", "0"), 69);
            assertThat(aos69.ajusteIdade()).isEqualByComparingTo("0.0");
            assertThat(aos69.total()).isEqualByComparingTo("2.0");
            assertThat(aos69.classificacao().rotulo()).isEqualTo("Sem risco nutricional no momento");
            assertThat(aos69.conclusao()).isEqualTo("Reavaliar semanalmente.");

            EscoreDto aos70 = nrs.avaliar(completo("2", "0"), 70);
            assertThat(aos70.ajusteIdade()).isEqualByComparingTo("1.0");
            assertThat(aos70.total()).isEqualByComparingTo("3.0");
            /* O corte é 3: o ponto por idade é o que muda a conduta. */
            assertThat(aos70.classificacao().rotulo()).isEqualTo("Em risco nutricional");
            assertThat(aos70.conclusao()).isEqualTo("Iniciar plano de terapia nutricional.");
        }

        /**
         * Sem data de nascimento o ponto por idade não pode ser decidido, e um
         * total um ponto menor cairia exatamente na faixa que decide iniciar
         * terapia nutricional. Recusa-se a concluir, e diz por quê.
         */
        @Test
        @DisplayName("sem idade, o escore não conclui — e escreve o motivo")
        void nrsSemDataNascimentoNaoConclui() {
            EscoreDto escore = nrs.avaliar(completo("2", "2"), null);

            assertThat(escore.total()).isNull();
            assertThat(escore.classificacao()).isNull();
            assertThat(escore.ajusteIdade()).isNull();
            assertThat(escore.motivoAusencia()).contains("data de nascimento");

            /* As etapas continuam somadas: o que falta é o ajuste, não elas. */
            assertThat(grupo(escore, Nrs2002Escala.ESTADO).subtotal()).isEqualByComparingTo("2.0");
        }

        @Test
        @DisplayName("a idade é contada na data de preenchimento, com mês e dia")
        void idadeContaMesEDia() {
            LocalDate ficha = LocalDate.of(2026, 9, 7);

            /* Faz 70 amanhã: hoje ainda tem 69. Subtrair anos erraria aqui. */
            assertThat(EscoreCalculator.idadeEm(LocalDate.of(1956, 9, 8), ficha)).isEqualTo(69);
            assertThat(EscoreCalculator.idadeEm(LocalDate.of(1956, 9, 7), ficha)).isEqualTo(70);
            assertThat(EscoreCalculator.idadeEm(null, ficha)).isNull();
        }
    }

    @Nested
    @DisplayName("Registro de escalas")
    class Registro {

        @Test
        @DisplayName("código desconhecido não explode: devolve vazio")
        void codigoOrfaoNaoExplode() {
            assertThat(EscalaRegistry.de("MUST")).isEmpty();
            assertThat(EscalaRegistry.de(null)).isEmpty();
            assertThat(EscalaRegistry.de("  ")).isEmpty();
            assertThat(EscalaRegistry.de("MNA")).isPresent();
        }

        /** O máximo declarado é a soma dos blocos — 14 + 16 = 30, 0 + 3 + 3 = 6 (+1 de idade). */
        @Test
        @DisplayName("o máximo de cada bloco fecha com o total da escala")
        void maximosCoerentes() {
            BigDecimal mnaSoma = mna.maximoPorGrupo().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(mnaSoma).isEqualByComparingTo("30");

            BigDecimal nrsSoma = nrs.maximoPorGrupo().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            /* 6 dos blocos + 1 do ajuste por idade = os 7 do instrumento. */
            assertThat(nrsSoma).isEqualByComparingTo("6");
        }
    }
}

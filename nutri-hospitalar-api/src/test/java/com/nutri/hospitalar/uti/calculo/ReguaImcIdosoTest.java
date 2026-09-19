package com.nutri.hospitalar.uti.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.enums.ReguaImcIdoso;
import com.nutri.hospitalar.uti.enums.TomResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A régua de IMC do idoso — Lipschitz 1994 e OPAS 2002. Ver {@code docs/10} §2.6.
 *
 * <p>Sem Spring e sem banco, como as outras classes de {@code uti/calculo/}: o
 * que se verifica aqui é régua e aritmética, e nenhuma delas depende de contexto.
 *
 * <p><b>Os dados foram escolhidos onde as duas réguas discordam.</b> Em IMC 24 ou
 * 29 elas dão o mesmo rótulo, e um teste ali passaria com o defeito e sem ele —
 * que é a definição de teste que não trava nada.
 */
@DisplayName("Régua de IMC do idoso")
class ReguaImcIdosoTest {

    /** 1,68 m: 2,8224 m². 22,5 × 2,8224 = 63,504 e 27,5 × 2,8224 = 77,616. */
    private static final BigDecimal ALTURA_168 = new BigDecimal("168");
    private static final BigDecimal PESO_IMC_22_5 = new BigDecimal("63.504");
    private static final BigDecimal PESO_IMC_27_5 = new BigDecimal("77.616");
    /** 24,0 exatos: as duas réguas concordam, e é o que o teste de idade quer. */
    private static final BigDecimal PESO_IMC_24 = new BigDecimal("67.7376");

    @Nested
    @DisplayName("Faixas de Lipschitz 1994")
    class Faixas {

        @Test
        @DisplayName("três faixas, cada uma fechada à esquerda")
        void fronteiras() {
            // Baixo peso < 22 · eutrofia 22 a 27 · excesso > 27 (PMID 8197257).
            // Os valores EXATOS do corte pertencem à faixa de cima — é a mesma
            // regra das outras duas réguas, e o defeito 17 de docs/10 §11 é
            // justamente o oposto disso na planilha.
            assertThat(rotuloLipschitz("21.99")).isEqualTo("Baixo peso");
            assertThat(rotuloLipschitz("22")).isEqualTo("Eutrofia");
            assertThat(rotuloLipschitz("26.99")).isEqualTo("Eutrofia");
            assertThat(rotuloLipschitz("27")).isEqualTo("Excesso de peso");
        }

        @Test
        @DisplayName("o tom acompanha a faixa, e não o texto")
        void tons() {
            assertThat(lipschitz("18").tom()).isEqualTo(TomResultado.CRITICO);
            assertThat(lipschitz("24").tom()).isEqualTo(TomResultado.ADEQUADO);
            assertThat(lipschitz("31").tom()).isEqualTo(TomResultado.ATENCAO);
        }

        @Test
        @DisplayName("sem IMC não há classificação")
        void semImc() {
            assertThat(AntropometriaCalculator.classificarImcLipschitz1994(null)).isNull();
        }

        private Classificacao lipschitz(String imc) {
            return AntropometriaCalculator.classificarImcLipschitz1994(new BigDecimal(imc));
        }

        private String rotuloLipschitz(String imc) {
            return lipschitz(imc).rotulo();
        }
    }

    @Nested
    @DisplayName("Corte de idade")
    class Idade {

        @Test
        @DisplayName("59 não classifica, 60 e 61 classificam")
        void corteEmSessenta() {
            // O Estatuto do Idoso é a convenção do sistema (docs/10 §2.2), e a
            // fronteira é o próprio 60: escrito `> 60`, o sexagenário perderia
            // a classificação sem que nada na tela denunciasse.
            assertThat(classificacaoIdoso(59, null)).isNull();
            assertThat(classificacaoIdoso(60, null)).isNotNull();
            assertThat(classificacaoIdoso(61, null)).isNotNull();
        }

        @Test
        @DisplayName("idade em branco produz motivo, nunca traço mudo")
        void semIdade() {
            var antro = antropometria(null, ALTURA_168, PESO_IMC_24, null);

            assertThat(antro.classificacaoImcIdoso()).isNull();
            assertThat(antro.motivoClassificacaoImcIdoso())
                    .isEqualTo("Informe a idade: a régua do idoso vale a partir de 60 anos");
            assertThat(antro.reguaIdosoRelevante()).isFalse();
        }

        @Test
        @DisplayName("abaixo de 60 o motivo diz a régua, não culpa a medida")
        void abaixoDoCorte() {
            var antro = antropometria(45, ALTURA_168, PESO_IMC_24, null);

            assertThat(antro.classificacaoImcIdoso()).isNull();
            assertThat(antro.motivoClassificacaoImcIdoso())
                    .isEqualTo("A régua do idoso aplica-se a partir de 60 anos");
        }

        @Test
        @DisplayName("idoso sem peso herda o motivo do IMC — o else final")
        void idosoSemImc() {
            // Idade em ordem, IMC ausente: sem o `else` final da cadeia este é o
            // caminho que ficaria mudo, e justamente para quem a régua vale.
            var antro = antropometria(70, ALTURA_168, null, null);

            assertThat(antro.imc()).isNull();
            assertThat(antro.classificacaoImcIdoso()).isNull();
            assertThat(antro.motivoClassificacaoImcIdoso())
                    .isNotNull()
                    .isEqualTo(antro.motivoImc());
        }
    }

    @Nested
    @DisplayName("A escolha da régua")
    class Escolha {

        @Test
        @DisplayName("o padrão é Lipschitz, e ele vem publicado")
        void padraoLipschitz() {
            var antro = antropometria(70, ALTURA_168, PESO_IMC_22_5, null);

            assertThat(antro.imc()).isEqualByComparingTo("22.5");
            assertThat(antro.classificacaoImcIdoso().rotulo()).isEqualTo("Eutrofia");
            assertThat(antro.reguaImcIdosoUsada()).isEqualTo("Lipschitz 1994");
            assertThat(antro.motivoClassificacaoImcIdoso()).isNull();
        }

        @Test
        @DisplayName("escolher a OPAS muda o diagnóstico, nos dois sentidos")
        void opasDiscordaDeLipschitz() {
            // 22,5: eutrofia por Lipschitz, baixo peso pela OPAS.
            var magro = antropometria(70, ALTURA_168, PESO_IMC_22_5, ReguaImcIdoso.OPAS_2002);
            assertThat(magro.classificacaoImcIdoso().rotulo()).isEqualTo("Baixo peso");
            assertThat(magro.classificacaoImcIdoso().tom()).isEqualTo(TomResultado.CRITICO);
            assertThat(magro.reguaImcIdosoUsada()).isEqualTo("OPAS 2002");

            // 27,5: excesso de peso por Lipschitz, eutrofia pela OPAS.
            var gordo = antropometria(70, ALTURA_168, PESO_IMC_27_5, ReguaImcIdoso.OPAS_2002);
            assertThat(gordo.classificacaoImcIdoso().rotulo()).isEqualTo("Eutrofia");
            assertThat(antropometria(70, ALTURA_168, PESO_IMC_27_5, null)
                    .classificacaoImcIdoso().rotulo()).isEqualTo("Excesso de peso");
        }

        @Test
        @DisplayName("a OPAS separa excesso de peso de obesidade; Lipschitz não")
        void opasTemQuatroFaixas() {
            // 31 × 2,8224 = 87,4944.
            var peso = new BigDecimal("87.4944");
            assertThat(antropometria(70, ALTURA_168, peso, ReguaImcIdoso.OPAS_2002)
                    .classificacaoImcIdoso().rotulo()).isEqualTo("Obesidade");
            assertThat(antropometria(70, ALTURA_168, peso, null)
                    .classificacaoImcIdoso().rotulo()).isEqualTo("Excesso de peso");
        }
    }

    @Nested
    @DisplayName("Faixa de peso do idoso")
    class FaixaDePeso {

        /** 1,75 m: 3,0625 m². */
        private final BigDecimal altura175 = new BigDecimal("175");

        @Test
        @DisplayName("os limites saem da régua escolhida, não de literal")
        void segueARegua() {
            var lipschitz = antropometria(70, altura175, PESO_IMC_24, null);
            assertThat(lipschitz.pesoIdealIdosoMinKg()).isEqualByComparingTo("67.375");
            assertThat(lipschitz.pesoIdealIdosoMaxKg()).isEqualByComparingTo("82.6875");

            var opas = antropometria(70, altura175, PESO_IMC_24, ReguaImcIdoso.OPAS_2002);
            assertThat(opas.pesoIdealIdosoMinKg()).isEqualByComparingTo("70.4375");
            assertThat(opas.pesoIdealIdosoMaxKg()).isEqualByComparingTo("85.75");

            assertThat(lipschitz.motivoPesoIdealIdoso()).isNull();
        }

        @Test
        @DisplayName("abaixo de 60 não há faixa, e a ausência é explicada")
        void abaixoDoCorte() {
            var antro = antropometria(59, altura175, PESO_IMC_24, null);

            assertThat(antro.pesoIdealIdosoMinKg()).isNull();
            assertThat(antro.pesoIdealIdosoMaxKg()).isNull();
            assertThat(antro.motivoPesoIdealIdoso())
                    .isEqualTo("A régua do idoso aplica-se a partir de 60 anos");
        }

        @Test
        @DisplayName("idoso sem altura: o motivo nomeia a altura, não a idade")
        void semAltura() {
            var antro = antropometria(70, null, PESO_IMC_24, null);

            assertThat(antro.pesoIdealIdosoMinKg()).isNull();
            assertThat(antro.motivoPesoIdealIdoso())
                    .isEqualTo("Informe a altura, ou a altura do joelho para estimá-la");
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    private static Classificacao classificacaoIdoso(Integer idade, ReguaImcIdoso regua) {
        return antropometria(idade, ALTURA_168, PESO_IMC_24, regua).classificacaoImcIdoso();
    }

    private static ResultadoUti.Antropometria antropometria(
            Integer idadeAnos, BigDecimal alturaCm, BigDecimal pesoKg, ReguaImcIdoso regua) {

        EntradaUti e = new EntradaUti(
                Sexo.MASCULINO, null, idadeAnos,
                alturaCm, null, null, null, null,
                pesoKg, null,
                null, null,
                null, regua, null,
                null, null, null, null, null,
                null, null, null,
                null);

        return AvaliacaoUtiCalculator.calcular(e, null, null, null).antropometria();
    }
}

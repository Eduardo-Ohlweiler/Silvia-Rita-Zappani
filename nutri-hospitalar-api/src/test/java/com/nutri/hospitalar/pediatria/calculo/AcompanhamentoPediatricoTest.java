package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere os derivados de um dia de acompanhamento contra {@code docs/11}.
 *
 * <p>Teste puro, sem Spring e sem banco.
 */
@DisplayName("Acompanhamento diário pediátrico")
class AcompanhamentoPediatricoTest {

    /** Linha do Percentil_F em 8 meses — a mesma do caso canônico de docs/09. */
    private static final LinhaPercentil F_8_MESES = new LinhaPercentil(
            bd("7.0"), bd("9.0"),      // peso     P15 / P85
            bd("66.3"), bd("71.2"),    // estatura P15 / P85
            bd("15.4"), bd("18.5"));   // IMC      P15 / P85

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Caso canônico de docs/11 §7")
    class CasoCanonico {

        /**
         * F, 8 meses no dia, peso do dia 9,2 kg, 800 ml recebidos, 7 de 8
         * tomadas aceitas. A avaliação vinculada prescreveu 880 ml/dia de
         * NAN 2 (73,8 kcal e 1,65 g/100 ml), com VET 723 e proteína 11 g/dia.
         */
        private final ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                new EntradaAcompanhamento(Sexo.FEMININO, 8,
                        bd("9.2"), null,
                        null, bd("800"), 8, 7,
                        bd("880"), bd("723"), bd("11"),
                        bd("73.8"), bd("1.65")),
                F_8_MESES);

        @Test
        @DisplayName("% recebido = 90,91 %, contra o prescrito da AVALIAÇÃO")
        void percentualRecebido() {
            // 800 / 880 × 100 = 90,9090...
            assertThat(r.percentualRecebido()).isEqualByComparingTo("90.91");
            // E diz contra o quê comparou: sem isso o número não significa nada.
            assertThat(r.referenciaDoRecebido()).contains("avaliação");
            assertThat(r.motivoPercentualRecebido()).isNull();
        }

        @Test
        @DisplayName("o volume recebido entrega 590,4 kcal e 13,2 g — por 100 ml, não por litro")
        void oferta() {
            // 73,8 × 800 / 100. Dividir por 1000, como a UTI faz, daria 59,04.
            assertThat(r.caloriasRecebidas()).isEqualByComparingTo("590.4");
            assertThat(r.proteinaRecebida()).isEqualByComparingTo("13.2");
            assertThat(r.motivoOferta()).isNull();
        }

        @Test
        @DisplayName("por quilo do peso DO DIA: 64,1739 kcal/kg e 1,4348 g/kg")
        void porQuilo() {
            // 590,4 / 9,2 e 13,2 / 9,2 — o peso é o do dia, não o da avaliação.
            assertThat(r.caloriasPorKg()).isEqualByComparingTo("64.1739");
            assertThat(r.proteinaPorKg()).isEqualByComparingTo("1.4348");
        }

        @Test
        @DisplayName("adequação calórica 81,66 % e proteica 120 %")
        void adequacoes() {
            assertThat(r.adequacaoCalorica()).isEqualByComparingTo("81.66");   // 590,4 / 723
            assertThat(r.adequacaoProteica()).isEqualByComparingTo("120.00");  // 13,2 / 11
        }

        @Test
        @DisplayName("aceitação das tomadas = 87,5 %")
        void aceitacao() {
            assertThat(r.aceitacaoTomadas()).isEqualByComparingTo("87.50");
        }

        /**
         * <b>O teste que prova que o registro calcula, e não copia.</b>
         *
         * <p>A avaliação era de 9,0 kg — exatamente o P85, que docs/09 §4.1
         * classifica como ADEQUADA pela assimetria do {@code >}. O dia é de
         * 9,2 kg, que <b>passa</b> do P85. Se isto devolver ADEQUADA, o
         * registro está repetindo a classificação gravada na avaliação em vez
         * de classificar o peso do dia — e a série inteira mentiria, mostrando
         * uma criança parada onde ela mudou de faixa.
         */
        @Test
        @DisplayName("9,2 kg passa do P85 de 9,0: classifica ACIMA, não repete a avaliação")
        void classificaComOPesoDoDia() {
            assertThat(r.pesoIdade()).isEqualTo(FaixaOms.ALTA);
            assertThat(r.motivoEstadoNutricional()).isNull();
        }

        @Test
        @DisplayName("sem estatura no dia não há IMC, e o motivo é dito")
        void semEstatura() {
            assertThat(r.imc()).isNull();
            assertThat(r.imcIdade()).isNull();
            assertThat(r.estaturaIdade()).isNull();
            assertThat(r.motivoImc()).contains("estatura");
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ausências explicadas")
    class Ausencias {

        @Test
        @DisplayName("sem avaliação, o % recebido cai no prescrito do dia — e diz isso")
        void semAvaliacaoUsaOPrescritoDoDia() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            bd("9"), null,
                            bd("1000"), bd("800"), null, null,
                            null, null, null, null, null),
                    F_8_MESES);

            assertThat(r.percentualRecebido()).isEqualByComparingTo("80.00");
            assertThat(r.referenciaDoRecebido()).contains("no dia");
        }

        @Test
        @DisplayName("sem prescrito nenhum, o % recebido falta COM motivo")
        void semPrescritoNenhum() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            bd("9"), null,
                            null, bd("800"), null, null,
                            null, null, null, null, null),
                    F_8_MESES);

            assertThat(r.percentualRecebido()).isNull();
            assertThat(r.motivoPercentualRecebido()).isNotNull().contains("prescrito");
        }

        @Test
        @DisplayName("sem data de nascimento não há idade nem classificação, e o motivo aponta o cadastro")
        void semNascimento() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, null,
                            bd("9"), bd("70"),
                            null, null, null, null,
                            null, null, null, null, null),
                    null);   // sem idade o service não acha linha

            assertThat(r.idadeMeses()).isNull();
            assertThat(r.motivoIdade()).isNotNull().contains("nascimento");
            assertThat(r.pesoIdade()).isNull();
            assertThat(r.motivoEstadoNutricional()).isNotNull().contains("nascimento");
            // O IMC não depende da idade: continua saindo
            assertThat(r.imc()).isEqualByComparingTo("18.3673");
        }

        @Test
        @DisplayName("acima de 60 meses classifica nada, e diz que a curva acabou")
        void foraDaCurva() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 61,
                            bd("20"), bd("110"),
                            null, null, null, null,
                            null, null, null, null, null),
                    null);

            assertThat(r.pesoIdade()).isNull();
            assertThat(r.motivoEstadoNutricional()).contains("60 meses");
        }

        @Test
        @DisplayName("volume recebido sem fórmula na avaliação: sem oferta, com motivo")
        void semFormula() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            bd("9"), null,
                            bd("880"), bd("800"), null, null,
                            null, null, null, null, null),
                    F_8_MESES);

            assertThat(r.caloriasRecebidas()).isNull();
            assertThat(r.proteinaRecebida()).isNull();
            assertThat(r.motivoOferta()).isNotNull().contains("fórmula");
            // O percentual recebido não depende da fórmula: continua saindo
            assertThat(r.percentualRecebido()).isEqualByComparingTo("90.91");
        }

        @Test
        @DisplayName("sem peso do dia não há kcal/kg, e o motivo cobra o peso — não a fórmula")
        void semPesoDoDia() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            null, null,
                            null, bd("800"), null, null,
                            bd("880"), bd("723"), bd("11"),
                            bd("73.8"), bd("1.65")),
                    F_8_MESES);

            assertThat(r.caloriasPorKg()).isNull();
            assertThat(r.motivoPorQuilo()).isNotNull().contains("peso");
            // As adequações NÃO dependem do peso — vêm das metas da avaliação
            assertThat(r.adequacaoCalorica()).isEqualByComparingTo("81.66");
        }

        @Test
        @DisplayName("entre 36 e 60 meses classifica, mas sem meta não há adequação")
        void semMetasEntre36E60Meses() {
            // A avaliação de uma criança de 40 meses não tem VET nem DRI
            // (docs/09 §8): 35 e 36 meses são os tetos, e o descompasso é da fonte.
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 40,
                            bd("15"), bd("98"),
                            null, bd("500"), null, null,
                            bd("500"), null, null,
                            bd("100"), bd("2")),
                    new LinhaPercentil(bd("13.5"), bd("17.4"),
                            bd("95.4"), bd("103.3"),
                            bd("14.2"), bd("17.0")));

            // A oferta sai — ela só precisa da fórmula
            assertThat(r.caloriasRecebidas()).isEqualByComparingTo("500");
            // As adequações não, e o motivo manda vincular avaliação com meta
            assertThat(r.adequacaoCalorica()).isNull();
            assertThat(r.motivoAdequacaoCalorica()).isNotNull().contains("VET");
            assertThat(r.adequacaoProteica()).isNull();
            assertThat(r.motivoAdequacaoProteica()).isNotNull();
            // Mas a classificação sai: a OMS vai até 60 meses
            assertThat(r.pesoIdade()).isEqualTo(FaixaOms.ADEQUADA);
        }

        @Test
        @DisplayName("sem tomadas informadas, a aceitação falta com motivo — e não vira zero")
        void semTomadas() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            bd("9"), null,
                            null, bd("800"), null, null,
                            bd("880"), null, null, null, null),
                    F_8_MESES);

            assertThat(r.aceitacaoTomadas()).isNull();
            assertThat(r.motivoAceitacao()).isNotNull();
        }

        @Test
        @DisplayName("zero tomadas aceitas é ZERO, não ausência — a criança recusou tudo")
        void zeroAceitasNaoEAusencia() {
            ResultadoAcompanhamento r = AcompanhamentoPediatricoCalculator.calcular(
                    new EntradaAcompanhamento(Sexo.FEMININO, 8,
                            bd("9"), null,
                            null, bd("0"), 8, 0,
                            bd("880"), null, null, null, null),
                    F_8_MESES);

            // Recusa total é informação clínica forte: não pode sair como traço.
            assertThat(r.aceitacaoTomadas()).isEqualByComparingTo("0.00");
            assertThat(r.motivoAceitacao()).isNull();
            assertThat(r.percentualRecebido()).isEqualByComparingTo("0.00");
        }
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}

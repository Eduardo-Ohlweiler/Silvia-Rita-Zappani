package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere o calculador contra a planilha `Pediatria.xlsx`, especificada em
 * docs/09-calculos-pediatria.md.
 *
 * <p>Teste puro, sem Spring e sem banco — o calculador não depende de nenhum
 * dos dois. Roda em milissegundos.
 */
@DisplayName("Cálculos da pediatria")
class CalculoPediatricoTest {

    /** Linha do Percentil_F em 8 meses — a do caso canônico (docs/09 §4.3). */
    private static final LinhaPercentil F_8_MESES = new LinhaPercentil(
            bd("7.0"), bd("9.0"),      // peso     P15 / P85
            bd("66.3"), bd("71.2"),    // estatura P15 / P85
            bd("15.4"), bd("18.5"));   // IMC      P15 / P85

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Caso canônico da planilha")
    class CasoCanonico {

        /**
         * F, 8 meses, 9 kg, sem estatura, NAN 2 (73,8 kcal · 1,65 g/100 ml),
         * 110 ml a cada 3 h. Os esperados são os valores em cache da própria
         * planilha — docs/09 §9.
         */
        private final ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                        bd("73.8"), bd("1.65"), bd("110"), bd("3")),
                F_8_MESES);

        @Test
        @DisplayName("peso para a idade fica adequado — 9 kg é exatamente o P85")
        void pesoParaIdade() {
            // O corte é assimétrico: >= P15 entra, e só > P85 sobe de faixa.
            // Trocar o > por >= aqui classificaria como "Acima do peso".
            assertThat(r.pesoIdade()).isEqualTo(FaixaOms.ADEQUADA);
        }

        @Test
        @DisplayName("sem estatura não há IMC, e o motivo é dito")
        void semEstatura() {
            assertThat(r.imc()).isNull();
            assertThat(r.imcIdade()).isNull();
            assertThat(r.estaturaIdade()).isNull();
            assertThat(r.motivoImc()).contains("estatura");
            // O peso classificou, então o bloco não está mudo por completo
            assertThat(r.motivoEstadoNutricional()).isNull();
        }

        @Test
        @DisplayName("VET = 723 kcal/dia e proteína = 11 g/dia")
        void necessidades() {
            assertThat(r.vet()).isEqualByComparingTo("723");          // (89×9−100)+22
            assertThat(r.proteinaNecessidade()).isEqualByComparingTo("11");
            assertThat(r.motivoVet()).isNull();
            assertThat(r.motivoProteina()).isNull();
        }

        @Test
        @DisplayName("dieta: 8 tomadas, 880 ml, 649,44 kcal e 14,52 g")
        void dieta() {
            assertThat(r.vezesDia()).isEqualByComparingTo("8");        // 24/3
            assertThat(r.volumeTotal()).isEqualByComparingTo("880");   // 8×110
            assertThat(r.caloriasTotais()).isEqualByComparingTo("649.44");
            assertThat(r.proteinaTotal()).isEqualByComparingTo("14.52");
            assertThat(r.motivoDieta()).isNull();
        }

        @Test
        @DisplayName("adequação: 89,83 % do VET e 132 % da proteína")
        void adequacao() {
            // Na planilha, 89,8257261410788 — arredondado a duas casas na coluna
            assertThat(r.percCalorico()).isEqualByComparingTo("89.83");
            assertThat(r.percProteico()).isEqualByComparingTo("132");
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cortes de percentil")
    class Cortes {

        @Test
        @DisplayName("exatamente no P15 é adequado")
        void noP15() {
            assertThat(CalculoPediatricoCalculator.faixa(bd("7.0"), bd("7.0"), bd("9.0")))
                    .isEqualTo(FaixaOms.ADEQUADA);
        }

        @Test
        @DisplayName("exatamente no P85 é adequado")
        void noP85() {
            assertThat(CalculoPediatricoCalculator.faixa(bd("9.0"), bd("7.0"), bd("9.0")))
                    .isEqualTo(FaixaOms.ADEQUADA);
        }

        @Test
        @DisplayName("logo abaixo do P15 é faixa baixa")
        void abaixoDoP15() {
            assertThat(CalculoPediatricoCalculator.faixa(bd("6.999"), bd("7.0"), bd("9.0")))
                    .isEqualTo(FaixaOms.BAIXA);
        }

        @Test
        @DisplayName("logo acima do P85 é faixa alta")
        void acimaDoP85() {
            assertThat(CalculoPediatricoCalculator.faixa(bd("9.001"), bd("7.0"), bd("9.0")))
                    .isEqualTo(FaixaOms.ALTA);
        }

        @Test
        @DisplayName("escala diferente não muda a comparação — 9 e 9.000 são o mesmo peso")
        void escalaNaoImporta() {
            // compareTo, não equals: BigDecimal("9").equals(new BigDecimal("9.0")) é false
            assertThat(CalculoPediatricoCalculator.faixa(bd("9"), bd("7.0"), bd("9.000")))
                    .isEqualTo(FaixaOms.ADEQUADA);
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Faixas de validade das DRIs")
    class FaixasDeValidade {

        @Test
        @DisplayName("a deposição energética muda nos limites 3, 6, 12 e 35 meses")
        void deposicao() {
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(0)).isEqualByComparingTo("175");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(3)).isEqualByComparingTo("175");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(4)).isEqualByComparingTo("56");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(6)).isEqualByComparingTo("56");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(7)).isEqualByComparingTo("22");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(12)).isEqualByComparingTo("22");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(13)).isEqualByComparingTo("20");
            assertThat(CalculoPediatricoCalculator.deposicaoEnergetica(35)).isEqualByComparingTo("20");
        }

        @Test
        @DisplayName("aos 36 meses há proteína mas não há VET — o descompasso é da fonte")
        void descompassoTrintaECincoTrintaESeis() {
            ResultadoPediatrico r = calcularIdade(36);

            assertThat(r.vet()).isNull();
            assertThat(r.motivoVet()).contains("35 meses");

            assertThat(r.proteinaNecessidade()).isEqualByComparingTo("13");
            assertThat(r.motivoProteina()).isNull();
        }

        @Test
        @DisplayName("aos 35 meses ainda há VET")
        void trintaECinco() {
            assertThat(calcularIdade(35).vet()).isNotNull();
        }

        @Test
        @DisplayName("aos 37 meses não há nem VET nem proteína, e ambos dizem por quê")
        void trintaESete() {
            ResultadoPediatrico r = calcularIdade(37);

            assertThat(r.vet()).isNull();
            assertThat(r.motivoVet()).contains("35 meses");
            assertThat(r.proteinaNecessidade()).isNull();
            assertThat(r.motivoProteina()).contains("36 meses");
        }

        @Test
        @DisplayName("as faixas de 4 a 18 anos da planilha não são usadas")
        void faixasNaoImplementadas() {
            // Existem em M20:N27 e nenhuma fórmula da planilha as referencia.
            // Ver docs/09 §6: habilitá-las é decisão clínica.
            assertThat(CalculoPediatricoCalculator.necessidadeProteica(48)).isNull();
            assertThat(CalculoPediatricoCalculator.necessidadeProteica(120)).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ausências explicadas")
    class Ausencias {

        @Test
        @DisplayName("fora das curvas da OMS, o motivo é dito em vez de traço mudo")
        void foraDaCurva() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 61, bd("20"), bd("110"),
                            null, null, null, null),
                    null);   // o service não acha linha acima de 60 meses

            assertThat(r.pesoIdade()).isNull();
            assertThat(r.motivoEstadoNutricional()).contains("60 meses");
            // O IMC não depende da curva: continua saindo
            assertThat(r.imc()).isNotNull();
        }

        @Test
        @DisplayName("sem sexo não há curva, e o motivo aponta o campo que falta")
        void semSexo() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(null, 8, bd("9"), null, null, null, null, null),
                    null);

            assertThat(r.motivoEstadoNutricional()).contains("sexo");
            // VET não depende do sexo — sai mesmo assim
            assertThat(r.vet()).isEqualByComparingTo("723");
        }

        @Test
        @DisplayName("sem frequência a dieta inteira fica ausente, sem divisão por zero")
        void semFrequencia() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                            bd("73.8"), bd("1.65"), bd("110"), null),
                    F_8_MESES);

            assertThat(r.vezesDia()).isNull();
            assertThat(r.volumeTotal()).isNull();
            assertThat(r.caloriasTotais()).isNull();
            assertThat(r.percCalorico()).isNull();
            assertThat(r.motivoDieta()).contains("frequência");
        }

        @Test
        @DisplayName("frequência zero não estoura: é tratada como ausente")
        void frequenciaZero() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                            bd("73.8"), bd("1.65"), bd("110"), BigDecimal.ZERO),
                    F_8_MESES);

            assertThat(r.vezesDia()).isNull();
            assertThat(r.motivoDieta()).contains("frequência");
        }

        @Test
        @DisplayName("sem fórmula há volume mas não há calorias, e o motivo é dito")
        void semFormula() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                            null, null, bd("110"), bd("3")),
                    F_8_MESES);

            assertThat(r.vezesDia()).isEqualByComparingTo("8");
            assertThat(r.volumeTotal()).isEqualByComparingTo("880");
            assertThat(r.caloriasTotais()).isNull();
            assertThat(r.motivoDieta()).contains("fórmula");
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Aritmética")
    class Aritmetica {

        @Test
        @DisplayName("frequência que não divide 24 não é arredondada")
        void frequenciaFracionaria() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                            bd("73.8"), bd("1.65"), bd("100"), bd("5")),
                    F_8_MESES);

            // 24/5 = 4,8 tomadas ao dia. Arredondar para 5 mudaria o volume
            // total em 20 ml/dia e, com ele, toda a adequação.
            assertThat(r.vezesDia()).isEqualByComparingTo("4.8");
            assertThat(r.volumeTotal()).isEqualByComparingTo("480");
        }

        @Test
        @DisplayName("frequência de 7 h: o arredondamento acontece só na saída")
        void frequenciaNaoExata() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), null,
                            bd("73.8"), bd("1.65"), bd("100"), bd("7")),
                    F_8_MESES);

            // 24/7 = 3,428571... → 3,4286 na escala da coluna
            assertThat(r.vezesDia()).isEqualByComparingTo("3.4286");
            // e o volume vem da divisão CHEIA (342,857...), não de 3,4286×100
            assertThat(r.volumeTotal()).isEqualByComparingTo("342.8571");
        }

        @Test
        @DisplayName("IMC = peso / (estatura em metros)²")
        void imc() {
            ResultadoPediatrico r = CalculoPediatricoCalculator.calcular(
                    new EntradaPediatrica(Sexo.FEMININO, 8, bd("9"), bd("70"),
                            null, null, null, null),
                    F_8_MESES);

            // 9 / 0,70² = 18,3673...
            assertThat(r.imc()).isEqualByComparingTo("18.3673");
            // 18,37 está entre 15,4 e 18,5 — adequado, por pouco
            assertThat(r.imcIdade()).isEqualTo(FaixaOms.ADEQUADA);
        }
    }

    private static ResultadoPediatrico calcularIdade(int idadeMeses) {
        return CalculoPediatricoCalculator.calcular(
                new EntradaPediatrica(Sexo.FEMININO, idadeMeses, bd("14"), null,
                        null, null, null, null),
                null);
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}

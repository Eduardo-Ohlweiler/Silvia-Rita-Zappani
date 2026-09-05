package com.nutri.hospitalar.uti.calculo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.nutri.hospitalar.uti.calculo.AcompanhamentoCalculator.percentualRecebido;
import static com.nutri.hospitalar.uti.calculo.AcompanhamentoCalculator.prescritoDeReferencia;
import static com.nutri.hospitalar.uti.calculo.UtiMatematica.percentual;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * O denominador da adesão, e a promessa de que ele é o que a tela mostra.
 *
 * <p>Teste puro, sem Spring e sem banco. Existe porque o número escolhido aqui
 * dentro era invisível de fora: o front exibia o volume <b>digitado</b> ao lado
 * de um percentual medido contra o da <b>avaliação</b>, e a folha do prontuário
 * afirmava "855 de 900 · 45,97 %".
 */
@DisplayName("Prescrito de referência da adesão")
class AcompanhamentoCalculatorTest {

    @Test
    @DisplayName("o da avaliação vence o digitado no dia")
    void oDaAvaliacaoVence() {
        var p = prescritoDeReferencia(bd("1364"), bd("700"));

        assertThat(p.valor()).isEqualByComparingTo("1364");
        assertThat(p.daAvaliacao()).isTrue();
    }

    @Test
    @DisplayName("sem avaliação, cai no digitado")
    void semAvaliacaoCaiNoDigitado() {
        var p = prescritoDeReferencia(null, bd("700"));

        assertThat(p.valor()).isEqualByComparingTo("700");
        assertThat(p.daAvaliacao()).isFalse();
    }

    /**
     * A escolha é por <b>positivo</b>, não por nulo — e é exatamente aqui que o
     * {@code COALESCE} do dashboard divergia: ele escolhia por nulo, ficava com
     * o zero, e o dia sumia da média enquanto esta conta o mantinha.
     */
    @Test
    @DisplayName("volume ZERO na avaliação é ausência de prescrição, não prescrição de zero")
    void zeroNaAvaliacaoNaoVence() {
        var p = prescritoDeReferencia(BigDecimal.ZERO, bd("700"));

        assertThat(p.valor()).isEqualByComparingTo("700");
        assertThat(p.daAvaliacao()).isFalse();
    }

    @Test
    @DisplayName("sem prescrito nenhum, não há denominador")
    void semNenhumDosDois() {
        assertThat(prescritoDeReferencia(null, null).valor()).isNull();
    }

    /**
     * <b>A invariante do módulo.</b>
     *
     * <p>O percentual tem de ser medido contra o número que o sistema
     * <b>publica</b> como denominador — e não contra outro. Enquanto a escolha
     * morava dentro de {@code percentualRecebido} e não saía de lá, nada
     * garantia isso: quem exibisse o prescrito tinha de adivinhar a regra, e
     * sete lugares do front adivinharam errado.
     *
     * <p>Reintroduzir a escolha em qualquer um dos dois lados — trocar
     * {@code positivo()} por {@code != null} num só, por exemplo — deixa este
     * teste vermelho.
     */
    @ParameterizedTest(name = "avaliação={0} · digitado={1} · recebido={2}")
    @CsvSource({
            "1364, 700,  1200",   // a avaliação vence
            "1860, 900,  855",    // o caso da Marlene: 45,97 %, não 95 %
            ",     700,  650",    // sem avaliação
            "0,    700,  650",    // avaliação com zero
            "720,  ,     650",    // sem digitado
    })
    @DisplayName("o percentual usa o MESMO denominador que o sistema expõe")
    void oPercentualUsaOMesmoDenominadorQueExpoe(BigDecimal avaliacao,
                                                 BigDecimal digitado,
                                                 BigDecimal recebido) {
        assertThat(percentualRecebido(recebido, avaliacao, digitado))
                .isEqualByComparingTo(
                        percentual(recebido, prescritoDeReferencia(avaliacao, digitado).valor()));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}

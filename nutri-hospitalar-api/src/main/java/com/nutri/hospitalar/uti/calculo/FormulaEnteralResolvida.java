package com.nutri.hospitalar.uti.calculo;

import java.math.BigDecimal;

/**
 * A fórmula enteral escolhida, já buscada no catálogo pelo service.
 *
 * <p>O calculador não conhece banco — recebe a composição pronta, como
 * {@code CalculoPediatricoCalculator} recebe a linha de percentil. Ver
 * {@code docs/03} §8.
 *
 * <p><b>Composição sempre por litro.</b> Não existe aqui campo de apresentação
 * nem ramo de frasco de 500 ml: a normalização acontece no catálogo, e é o que
 * torna o defeito 1 da planilha irrepresentável.
 *
 * @param aguaLivrePerc do rótulo. Nulo faz o cálculo cair na escada por
 *                      densidade — e dizer que caiu.
 */
public record FormulaEnteralResolvida(
        String nome,
        BigDecimal densidadeKcalMl,
        BigDecimal proteinaGL,
        BigDecimal choGL,
        BigDecimal lipGL,
        BigDecimal fibrasGL,
        BigDecimal potassioMgL,
        BigDecimal aguaLivrePerc
) {}

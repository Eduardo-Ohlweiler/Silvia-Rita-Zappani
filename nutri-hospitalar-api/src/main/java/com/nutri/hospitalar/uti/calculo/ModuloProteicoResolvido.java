package com.nutri.hospitalar.uti.calculo;

import java.math.BigDecimal;

/**
 * O módulo proteico escolhido, já buscado no catálogo pelo service.
 *
 * <p>Espelho de {@link FormulaEnteralResolvida}, e pela mesma razão: o
 * calculador não conhece banco — recebe a composição pronta. Ver {@code docs/03}
 * §8.
 *
 * <p><b>A caloria vem do rótulo, e é só isso que este record carrega dela.</b>
 * Não há campo de carboidrato aqui de propósito: a planilha recompõe a kcal do
 * módulo macro a macro em {@code Contínuo!U24} e erra — soma <i>gramas</i> de
 * carboidrato a um total de <i>kcal</i>. Sem o macro neste record, o defeito 10
 * é irrepresentável, do mesmo jeito que a ausência de campo de apresentação em
 * {@code FormulaEnteralResolvida} torna o defeito 1 irrepresentável.
 *
 * @param nome               como ficará gravado na avaliação
 * @param medidaG            gramas de uma medida do produto
 * @param proteinaPorMedidaG proteína em uma medida
 * @param kcalPorMedida      calorias em uma medida, <b>como o rótulo declara</b>
 */
public record ModuloProteicoResolvido(
        String nome,
        BigDecimal medidaG,
        BigDecimal proteinaPorMedidaG,
        BigDecimal kcalPorMedida
) {}

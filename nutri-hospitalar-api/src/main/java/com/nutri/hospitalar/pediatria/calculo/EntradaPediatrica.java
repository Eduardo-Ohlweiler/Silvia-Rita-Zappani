package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.math.BigDecimal;

/**
 * As entradas de um cálculo pediátrico, já normalizadas.
 *
 * <p>Só {@code sexo}, {@code idadeMeses} e {@code peso} são exigidos; o resto é
 * opcional e a sua ausência apenas deixa de produzir os resultados que dele
 * dependem — com o motivo explicado, nunca em silêncio.
 *
 * @param idadeMeses       idade em meses inteiros
 * @param peso             kg
 * @param estatura         cm — sem ela não há IMC
 * @param kcalPor100ml     composição da fórmula láctea escolhida
 * @param proteinaPor100ml composição da fórmula láctea escolhida, em g
 * @param volumeMl         volume por tomada, em ml
 * @param frequenciaHoras  INTERVALO entre tomadas, em horas — não a quantidade
 *                         delas. Ver docs/09 §2.
 */
public record EntradaPediatrica(
        Sexo sexo,
        Integer idadeMeses,
        BigDecimal peso,
        BigDecimal estatura,
        BigDecimal kcalPor100ml,
        BigDecimal proteinaPor100ml,
        BigDecimal volumeMl,
        BigDecimal frequenciaHoras
) {}

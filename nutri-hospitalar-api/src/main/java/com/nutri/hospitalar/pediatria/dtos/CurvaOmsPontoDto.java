package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * Um mês da curva de crescimento da OMS, com os cinco percentis de cada medida.
 *
 * <p>É o que a curva na tela desenha: a faixa larga P3–P97 por baixo e a
 * estreita P15–P85 por cima, como na curva impressa da OMS. Só a P15–P85
 * classifica — a externa situa o extremo, e é por isso que ela aparece mais
 * clara.
 */
public record CurvaOmsPontoDto(
        Integer idadeMeses,

        BigDecimal pesoP3,
        BigDecimal pesoP15,
        BigDecimal pesoP50,
        BigDecimal pesoP85,
        BigDecimal pesoP97,

        BigDecimal estaturaP3,
        BigDecimal estaturaP15,
        BigDecimal estaturaP50,
        BigDecimal estaturaP85,
        BigDecimal estaturaP97,

        BigDecimal imcP3,
        BigDecimal imcP15,
        BigDecimal imcP50,
        BigDecimal imcP85,
        BigDecimal imcP97
) {}

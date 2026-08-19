package com.nutri.hospitalar.pediatria.calculo;

import java.math.BigDecimal;

/**
 * Os cortes de uma linha da tabela da OMS — um sexo, uma idade em meses.
 *
 * <p>Só P15 e P85, que é o que a classificação usa. A tabela guarda também P3,
 * P50 e P97 para as curvas de crescimento, mas o calculador não os vê: ele
 * classifica, não desenha.
 */
public record LinhaPercentil(
        BigDecimal pesoP15,
        BigDecimal pesoP85,
        BigDecimal estaturaP15,
        BigDecimal estaturaP85,
        BigDecimal imcP15,
        BigDecimal imcP85
) {}

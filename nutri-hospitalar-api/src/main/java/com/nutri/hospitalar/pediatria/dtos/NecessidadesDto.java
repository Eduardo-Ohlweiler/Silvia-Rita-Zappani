package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * Bloco das necessidades pelas DRIs 2002 — ainda na primeira aba, porque usa
 * exatamente as mesmas entradas do estado nutricional: idade e peso.
 *
 * <p>Os dois motivos são separados porque as faixas de validade não coincidem:
 * aos 36 meses há proteína e não há VET. O descompasso é da planilha de origem
 * e está preservado — ver docs/09 §8.
 *
 * @param vet     kcal/dia
 * @param proteina g/dia — valor fixo da faixa etária, não por quilo
 */
public record NecessidadesDto(
        BigDecimal vet,
        String motivoVet,
        BigDecimal proteina,
        String motivoProteina
) {}

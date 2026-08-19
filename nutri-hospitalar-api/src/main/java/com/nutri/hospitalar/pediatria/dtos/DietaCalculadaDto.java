package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * Bloco da dieta láctea prescrita — a segunda aba da tela de cálculo.
 *
 * <p>Os dois percentuais dependem do VET e da necessidade proteica, que são
 * calculados na primeira aba. Por isso a segunda aba exibe esses dois valores
 * como referência somente-leitura: um "132 % da necessidade" sem dizer de que
 * necessidade é um número sem significado.
 *
 * @param vezesDia    24 ÷ intervalo em horas. Pode ser fracionário, e não é
 *                    arredondado — a cada 5 h são 4,8 tomadas ao dia.
 */
public record DietaCalculadaDto(
        BigDecimal vezesDia,
        BigDecimal volumeTotal,
        BigDecimal caloriasTotais,
        BigDecimal proteinaTotal,
        BigDecimal percCalorico,
        BigDecimal percProteico,
        String motivo
) {}

package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do acompanhamento. O suficiente para ver a evolução de relance:
 * quanto foi ofertado, quanto chegou, e como o paciente estava.
 *
 * @param volPrescrito24h o que foi <b>digitado</b> no dia — registro, não
 *                        denominador
 * @param prescritoDeReferencia o volume contra o qual o percentual foi medido.
 *                              A lista mostrava o digitado ao lado do percentual
 *                              e a divisão não fechava: "855 / 900 ml · 45,97 %"
 * @param referenciaDoPercentual de onde veio o de referência. Sem ela a planilha
 *                               exportaria um denominador sem nome
 */
public record RegistroDiarioUtiListaDto(
        UUID id,
        String pessoaNome,
        LocalDate data,
        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,
        BigDecimal prescritoDeReferencia,
        String referenciaDoPercentual,
        BigDecimal percentualRecebido,
        BigDecimal caloriasPorQuilo,
        BigDecimal balancoHidricoMl,
        BigDecimal diureseMl,
        /** Sem avaliação vinculada não há kcal/kg nem diurese por quilo. */
        boolean temAvaliacao
) {}

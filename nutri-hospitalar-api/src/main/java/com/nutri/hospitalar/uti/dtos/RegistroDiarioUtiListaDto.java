package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do acompanhamento. O suficiente para ver a evolução de relance:
 * quanto foi ofertado, quanto chegou, e como o paciente estava.
 */
public record RegistroDiarioUtiListaDto(
        UUID id,
        String pessoaNome,
        LocalDate data,
        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,
        BigDecimal percentualRecebido,
        BigDecimal caloriasPorQuilo,
        BigDecimal balancoHidricoMl,
        BigDecimal diureseMl,
        /** Sem avaliação vinculada não há kcal/kg nem diurese por quilo. */
        boolean temAvaliacao
) {}

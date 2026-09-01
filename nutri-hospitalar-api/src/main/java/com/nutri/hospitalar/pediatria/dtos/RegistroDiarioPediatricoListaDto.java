package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do acompanhamento pediátrico. O suficiente para ver a evolução de
 * relance: como a criança estava, quanto foi ofertado e quanto chegou.
 *
 * <p>A <b>idade</b> vem aqui porque na pediatria ela anda — dois registros do
 * mesmo paciente com 30 dias de intervalo são idades diferentes, e sem a coluna
 * a lista pareceria repetir a mesma criança.
 */
public record RegistroDiarioPediatricoListaDto(
        UUID id,
        String pessoaNome,
        LocalDate data,
        Integer idadeMeses,
        BigDecimal pesoKg,
        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,
        BigDecimal percentualRecebido,
        BigDecimal adequacaoCalorica,
        BigDecimal aceitacaoTomadas,
        /** Sem avaliação vinculada não há adequação: falta a meta. */
        boolean temAvaliacao
) {}

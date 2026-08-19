package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma avaliação salva: as entradas, o retrato da fórmula usada e o resultado
 * <b>como foi gravado</b> — sem recálculo na leitura.
 *
 * <p>Os campos {@code formula*} são o retrato tirado no momento do cálculo, não
 * uma leitura do catálogo. Se a fórmula foi editada ou desativada depois, aqui
 * continuam os números com que a dieta foi de fato prescrita — e
 * {@code formulaLacteaId} pode vir nulo, se ela foi excluída, sem que isso
 * apague nada.
 */
public record AvaliacaoPediatricaResponseDto(
        UUID id,

        UUID pacienteId,
        String pacienteNome,
        UUID profissionalId,
        String profissionalNome,

        LocalDate dataAvaliacao,
        Sexo sexo,
        Integer idadeMeses,
        BigDecimal peso,
        BigDecimal estatura,

        UUID formulaLacteaId,
        String formulaNome,
        BigDecimal formulaKcalPor100ml,
        BigDecimal formulaProteinaPor100ml,
        BigDecimal volumeMl,
        BigDecimal frequenciaHoras,

        ResultadoPediatricoDto resultado,

        String observacao,
        Instant createdAt,
        Instant updatedAt
) {}

package com.nutri.hospitalar.uti.dtos;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros da listagem. Todos opcionais — a native query trata o nulo com
 * {@code CAST} explícito, como manda a regra 2 do {@code CLAUDE.md}.
 */
public record AvaliacaoUtiFiltrosDto(
        UUID pacienteId,
        String pacienteNome,
        LocalDate de,
        LocalDate ate
) {}

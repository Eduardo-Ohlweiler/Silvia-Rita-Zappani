package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param global fórmula do sistema: visível para todo tenant, editável por
 *               nenhum. A tela usa isso para marcar a linha e desabilitar as
 *               ações, em vez de deixar o usuário descobrir no 400.
 */
public record FormulaLacteaResponseDto(
        UUID id,
        String nome,
        BigDecimal kcalPor100ml,
        BigDecimal proteinaPor100ml,
        Boolean ativo,
        boolean global,
        Instant createdAt,
        Instant updatedAt
) {}

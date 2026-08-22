package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Toda composição por litro. Ver {@link FormulaEnteralCreateDto}.
 *
 * @param categoriaDescricao rótulo acentuado para exibir. Vai junto com a
 *                           constante para o front não manter a sua própria
 *                           tabela de tradução — e não errar um acento num
 *                           filtro que depois não acha nada.
 * @param global             fórmula do sistema: visível para todo tenant,
 *                           editável por nenhum. A tela usa isso para marcar a
 *                           linha e desabilitar as ações, em vez de deixar o
 *                           usuário descobrir no 400.
 */
public record FormulaEnteralResponseDto(
        UUID id,
        String nome,
        CategoriaFormulaEnteral categoria,
        String categoriaDescricao,
        BigDecimal densidadeKcalMl,
        BigDecimal proteinaGL,
        BigDecimal choGL,
        BigDecimal lipGL,
        BigDecimal fibrasGL,
        BigDecimal potassioMgL,
        BigDecimal osmolaridadeMosmL,
        BigDecimal aguaLivrePerc,
        Boolean ativo,
        boolean global,
        Instant createdAt,
        Instant updatedAt
) {}

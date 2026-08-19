package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de combo. Leva a composição junto porque a tela de cálculo mostra
 * "NAN 2 (73,8 kcal · 1,65 g / 100 ml)" no próprio rótulo da opção — quem
 * prescreve escolhe pela composição, não só pelo nome.
 */
public record FormulaLacteaSelectDto(
        UUID id,
        String nome,
        BigDecimal kcalPor100ml,
        BigDecimal proteinaPor100ml,
        boolean global
) {}

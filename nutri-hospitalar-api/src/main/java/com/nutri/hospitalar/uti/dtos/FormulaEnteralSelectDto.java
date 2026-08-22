package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de combo da tela de dieta enteral.
 *
 * <p>Leva a composição junto porque quem prescreve escolhe pela composição, e
 * não pelo nome: o rótulo da opção mostra
 * "Peptamen Intense (1,0 kcal/ml · 92 g PTN/L)".
 *
 * <p>{@code aguaLivrePerc} vem também, e anulável de propósito: a aba de
 * hidratação precisa saber se o número da água veio do rótulo ou da estimativa
 * por densidade — e é a ausência aqui que decide isso. Ver {@code docs/10} §5.1.
 */
public record FormulaEnteralSelectDto(
        UUID id,
        String nome,
        String categoriaDescricao,
        BigDecimal densidadeKcalMl,
        BigDecimal proteinaGL,
        BigDecimal choGL,
        BigDecimal lipGL,
        BigDecimal fibrasGL,
        BigDecimal potassioMgL,
        BigDecimal aguaLivrePerc,
        boolean global
) {}

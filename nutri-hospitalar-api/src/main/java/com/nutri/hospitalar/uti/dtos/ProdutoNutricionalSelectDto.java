package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.PapelArtesanal;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de combo.
 *
 * <p>Leva a medida e a composição junto: o rótulo da opção mostra
 * "Trophic Basic (medida 7,8 g · 30 kcal · 1,2 g PTN)", e é assim que se
 * escolhe um módulo proteico — pela proteína por medida, não pelo nome.
 *
 * <p>{@code embalagemQtd} vem porque o cálculo de latas por mês depende dela, e
 * {@code papelArtesanal} porque a receita do sistema aberto monta os quatro
 * papéis a partir desta mesma lista.
 */
public record ProdutoNutricionalSelectDto(
        UUID id,
        String nome,
        String tipoDescricao,
        String medidaNome,
        BigDecimal medidaQtd,
        BigDecimal embalagemQtd,
        BigDecimal kcal,
        BigDecimal proteinaG,
        BigDecimal choG,
        BigDecimal lipG,
        PapelArtesanal papelArtesanal,
        boolean global
) {}

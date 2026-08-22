package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Composição por medida. Ver {@link ProdutoNutricionalCreateDto}.
 *
 * @param tipoDescricao  rótulo acentuado do tipo, para o front não manter a sua
 *                       própria tabela de tradução.
 * @param papelDescricao rótulo do papel na receita artesanal; ausente nos
 *                       outros dois tipos.
 * @param moduloProteico derivado do tipo pelo servidor. Vem no response porque
 *                       a tela marca a linha, mas nunca é aceito no request.
 * @param global         produto do sistema: visível para todo tenant, editável
 *                       por nenhum.
 */
public record ProdutoNutricionalResponseDto(
        UUID id,
        String nome,
        TipoProdutoNutricional tipo,
        String tipoDescricao,
        String medidaNome,
        BigDecimal medidaQtd,
        BigDecimal embalagemQtd,
        BigDecimal kcal,
        BigDecimal proteinaG,
        BigDecimal choG,
        BigDecimal acucarG,
        BigDecimal lipG,
        BigDecimal sodioMg,
        BigDecimal potassioMg,
        BigDecimal fosforoMg,
        BigDecimal ferroMg,
        BigDecimal fibrasG,
        BigDecimal osmolaridadeMosmL,
        Boolean moduloProteico,
        PapelArtesanal papelArtesanal,
        String papelDescricao,
        String observacao,
        Boolean ativo,
        boolean global,
        Instant createdAt,
        Instant updatedAt
) {}

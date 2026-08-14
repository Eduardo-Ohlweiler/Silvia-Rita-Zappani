package com.nutri.hospitalar.vinculo.dtos;

import com.nutri.hospitalar.vinculo.enums.TipoVinculo;

import java.util.UUID;

/**
 * O vínculo visto de dentro do cadastro de uma pessoa: os dados são sempre da
 * <b>outra</b>, e o tipo é o papel dela.
 */
public record VinculoResponseDto(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        String documento,
        TipoVinculo tipo,
        String tipoDescricao,
        String observacao
) {}

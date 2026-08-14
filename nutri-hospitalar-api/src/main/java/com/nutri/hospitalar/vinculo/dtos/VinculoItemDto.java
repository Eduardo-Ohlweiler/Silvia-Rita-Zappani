package com.nutri.hospitalar.vinculo.dtos;

import com.nutri.hospitalar.vinculo.enums.TipoVinculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Item da lista de vínculos. {@code id} nulo é linha nova.
 *
 * <p>{@code tipo} é o papel da OUTRA pessoa em relação a quem está sendo
 * editado: marcar "Responsável" numa linha significa que a pessoa escolhida é
 * responsável por esta.
 */
public record VinculoItemDto(

        UUID id,

        @NotNull(message = "Informe a pessoa do vínculo")
        UUID pessoaId,

        @NotNull(message = "Informe o tipo do vínculo")
        TipoVinculo tipo,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String observacao
) {}

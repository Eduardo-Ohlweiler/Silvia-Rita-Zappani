package com.nutri.hospitalar.contato.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Item da lista de redes sociais. {@code id} nulo é linha nova. */
public record RedeSocialItemDto(

        UUID id,

        @NotNull(message = "Informe a rede social")
        UUID tipoRedeSocialId,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String usuario,

        @Size(max = 500, message = "No máximo 500 caracteres")
        String url,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String observacao
) {}

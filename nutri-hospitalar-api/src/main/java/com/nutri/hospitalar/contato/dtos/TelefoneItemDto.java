package com.nutri.hospitalar.contato.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Item da lista de telefones. {@code id} nulo é linha nova. */
public record TelefoneItemDto(

        UUID id,

        @NotNull(message = "Informe o tipo do telefone")
        UUID tipoTelefoneId,

        @NotBlank(message = "Informe o telefone")
        @Pattern(regexp = "\\d{10,11}",
                message = "Telefone deve ter DDD e 10 ou 11 dígitos, só números")
        String numero,

        @Pattern(regexp = "\\d{1,4}", message = "Código do país inválido")
        String codigoPais,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String observacao,

        Boolean principal
) {}

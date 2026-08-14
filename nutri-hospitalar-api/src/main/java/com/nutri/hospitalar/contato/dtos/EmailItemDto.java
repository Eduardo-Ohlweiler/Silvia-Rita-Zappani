package com.nutri.hospitalar.contato.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Item da lista de e-mails. {@code id} nulo é linha nova. */
public record EmailItemDto(

        UUID id,

        @NotNull(message = "Informe o tipo do e-mail")
        UUID tipoEmailId,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String email,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String observacao,

        Boolean principal
) {}

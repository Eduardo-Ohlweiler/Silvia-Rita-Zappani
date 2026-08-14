package com.nutri.hospitalar.contato.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Item da lista de endereços. {@code id} nulo é linha nova. */
public record EnderecoItemDto(

        UUID id,

        @NotNull(message = "Informe o tipo do endereço")
        UUID tipoEnderecoId,

        @NotNull(message = "Informe a cidade")
        UUID cidadeId,

        @Size(max = 9, message = "CEP inválido")
        String cep,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String rua,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String numero,

        @Size(max = 100, message = "No máximo 100 caracteres")
        String bairro,

        @Size(max = 100, message = "No máximo 100 caracteres")
        String complemento,

        Boolean principal
) {}

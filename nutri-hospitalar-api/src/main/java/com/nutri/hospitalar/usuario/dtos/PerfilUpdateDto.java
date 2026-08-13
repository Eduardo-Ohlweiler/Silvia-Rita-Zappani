package com.nutri.hospitalar.usuario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilUpdateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String telefone,

        @Pattern(regexp = "\\d{1,4}", message = "Código do país inválido")
        String codigoPais
) {}

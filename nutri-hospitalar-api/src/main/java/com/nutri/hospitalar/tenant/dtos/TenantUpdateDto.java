package com.nutri.hospitalar.tenant.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantUpdateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome
) {}

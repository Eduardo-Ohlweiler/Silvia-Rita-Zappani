package com.nutri.hospitalar.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(

        @NotBlank(message = "Informe o e-mail")
        String email,

        @NotBlank(message = "Informe a senha")
        String senha
) {}

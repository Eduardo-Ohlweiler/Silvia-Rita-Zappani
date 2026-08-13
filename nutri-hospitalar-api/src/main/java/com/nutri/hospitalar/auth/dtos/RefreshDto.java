package com.nutri.hospitalar.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshDto(

        @NotBlank(message = "Informe o refresh token")
        String refreshToken
) {}

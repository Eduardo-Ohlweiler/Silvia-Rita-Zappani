package com.nutri.hospitalar.auth.dtos;

import com.nutri.hospitalar.usuario.enums.Role;

import java.util.UUID;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UUID usuarioId,
        String nome,
        String email,
        Role role,
        UUID tenantId,
        String tenantNome,
        boolean impersonating
) {}

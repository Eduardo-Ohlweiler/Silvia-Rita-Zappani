package com.nutri.hospitalar.usuario.dtos;

import com.nutri.hospitalar.usuario.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponseDto(
        UUID id,
        UUID tenantId,
        String tenantNome,
        String nome,
        String email,
        String telefone,
        String codigoPais,
        Role role,
        Boolean ativo,
        Boolean bloqueado,
        Instant createdAt
) {}

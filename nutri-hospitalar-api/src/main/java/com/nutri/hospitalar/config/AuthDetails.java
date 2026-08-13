package com.nutri.hospitalar.config;

import com.nutri.hospitalar.usuario.enums.Role;

import java.util.UUID;

public record AuthDetails(
        UUID tenantId,
        UUID sessionId,
        Role role,
        boolean impersonating
) {}

package com.nutri.hospitalar.loginlog.dtos;

import com.nutri.hospitalar.loginlog.enums.MotivoFalha;
import com.nutri.hospitalar.loginlog.enums.TipoLogout;

import java.time.Instant;
import java.util.UUID;

public record LoginLogResponseDto(
        UUID id,
        UUID tenantId,
        String tenantNome,
        UUID usuarioId,
        String usuarioNome,
        String emailTentativa,
        Boolean sucesso,
        MotivoFalha motivoFalha,
        Instant dataLogin,
        Instant dataLogout,
        TipoLogout tipoLogout,
        String enderecoIp,
        String userAgent,
        String impersonadoPorNome
) {}

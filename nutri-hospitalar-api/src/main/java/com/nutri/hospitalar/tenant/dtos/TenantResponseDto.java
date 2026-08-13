package com.nutri.hospitalar.tenant.dtos;

import com.nutri.hospitalar.tenant.enums.PeriodoAcesso;

import java.time.Instant;
import java.util.UUID;

public record TenantResponseDto(
        UUID id,
        String nome,
        Boolean ativo,
        PeriodoAcesso periodoAcesso,
        /** Nulo em acesso indeterminado. */
        Instant acessoExpiraEm,
        Boolean acessoExpirado,
        /** Nulo em acesso indeterminado; negativo quando o prazo já venceu. */
        Long diasParaExpirar,
        Instant createdAt
) {}

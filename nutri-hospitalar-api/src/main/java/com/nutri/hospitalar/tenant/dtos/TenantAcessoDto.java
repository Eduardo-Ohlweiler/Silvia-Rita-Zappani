package com.nutri.hospitalar.tenant.dtos;

import com.nutri.hospitalar.tenant.enums.PeriodoAcesso;
import jakarta.validation.constraints.NotNull;

/** Define ou renova o período de acesso do cliente, sempre a partir de hoje. */
public record TenantAcessoDto(

        @NotNull(message = "Informe o período de acesso")
        PeriodoAcesso periodoAcesso
) {}

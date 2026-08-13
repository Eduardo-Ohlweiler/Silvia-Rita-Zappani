package com.nutri.hospitalar.loginlog.mapper;

import com.nutri.hospitalar.loginlog.dtos.LoginLogResponseDto;
import com.nutri.hospitalar.loginlog.entity.LoginLog;

public final class LoginLogMapper {

    private LoginLogMapper() {}

    public static LoginLogResponseDto toResponse(LoginLog log) {
        return new LoginLogResponseDto(
                log.getId(),
                log.getTenant() != null ? log.getTenant().getId() : null,
                log.getTenant() != null ? log.getTenant().getNome() : null,
                log.getUsuario() != null ? log.getUsuario().getId() : null,
                log.getUsuario() != null ? log.getUsuario().getNome() : null,
                log.getEmailTentativa(),
                log.getSucesso(),
                log.getMotivoFalha(),
                log.getDataLogin(),
                log.getDataLogout(),
                log.getTipoLogout(),
                log.getEnderecoIp(),
                log.getUserAgent(),
                log.getImpersonadoPor() != null ? log.getImpersonadoPor().getNome() : null);
    }
}

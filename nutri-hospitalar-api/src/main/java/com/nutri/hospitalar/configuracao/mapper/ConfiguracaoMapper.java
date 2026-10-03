package com.nutri.hospitalar.configuracao.mapper;

import com.nutri.hospitalar.configuracao.dtos.AparenciaResponseDto;
import com.nutri.hospitalar.configuracao.entity.ConfiguracaoSistema;

public final class ConfiguracaoMapper {

    private ConfiguracaoMapper() {}

    public static AparenciaResponseDto toAparencia(ConfiguracaoSistema configuracao) {
        return new AparenciaResponseDto(configuracao.getPaleta());
    }
}

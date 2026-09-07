package com.nutri.hospitalar.clinica.dtos;

import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;

import java.util.List;
import java.util.UUID;

public record CampoFichaResponseDto(
        UUID id,
        String secao,
        String rotulo,
        TipoCampoFicha tipo,
        List<String> opcoes,
        Integer ordem,
        Boolean obrigatorio,
        Boolean ativo
) {}

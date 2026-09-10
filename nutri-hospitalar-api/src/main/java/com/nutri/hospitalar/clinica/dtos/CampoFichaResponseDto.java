package com.nutri.hospitalar.clinica.dtos;

import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param pontos      paralelo a {@code opcoes}. Vazio na pergunta que não pontua
 * @param grupoEscore o bloco da escala. Nulo fica fora do escore
 */
public record CampoFichaResponseDto(
        UUID id,
        String secao,
        String rotulo,
        TipoCampoFicha tipo,
        List<String> opcoes,
        List<BigDecimal> pontos,
        String grupoEscore,
        Integer ordem,
        Boolean obrigatorio,
        Boolean ativo
) {}

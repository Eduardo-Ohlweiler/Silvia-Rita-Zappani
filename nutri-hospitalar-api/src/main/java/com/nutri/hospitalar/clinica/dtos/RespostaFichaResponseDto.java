package com.nutri.hospitalar.clinica.dtos;

import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A resposta como ficou gravada — <b>com o retrato</b>.
 *
 * <p>Tudo o que a tela e o papel precisam para desenhar a pergunta vem daqui, e
 * não do modelo: é o que faz uma ficha de um ano atrás continuar mostrando a
 * pergunta que foi feita, e não a que o modelo faz hoje.
 *
 * @param campoId rastro. Nulo quando a pergunta foi apagada do modelo — a
 *                resposta continua inteira.
 * @param pontos  quanto esta resposta valeu, do retrato. Nulo quando a pergunta
 *                não pontua ou não foi respondida — nunca zero por omissão, que
 *                é ponto legítimo em quase todo item das duas escalas.
 */
public record RespostaFichaResponseDto(
        UUID id,
        UUID campoId,
        String secao,
        String rotulo,
        TipoCampoFicha tipo,
        List<String> opcoes,
        Integer ordem,
        Boolean obrigatorio,
        BigDecimal pontos,
        String grupoEscore,
        String valor
) {}

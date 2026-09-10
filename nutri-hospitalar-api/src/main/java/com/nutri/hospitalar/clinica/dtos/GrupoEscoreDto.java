package com.nutri.hospitalar.clinica.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Um bloco de pontuação de uma escala — a triagem da MNA, a etapa 2 da NRS-2002.
 *
 * @param subtotal              <b>nulo enquanto o grupo estiver incompleto</b>.
 *                              Numa escala, soma parcial não é escore menor: é
 *                              escore errado. Cinco perguntas em branco fazem a
 *                              MNA somar 12 e parecer "sob risco" num paciente
 *                              que pode ser normal
 * @param maximo                nulo no grupo que não pontua — a pré-triagem da
 *                              NRS-2002 é uma porta, não uma soma
 * @param classificacao         só nos grupos que têm faixa publicada. A avaliação
 *                              global da MNA não tem, e por isso não inventa uma
 * @param motivoAusencia        por que o {@code subtotal} está nulo
 * @param perguntasSemResposta  os rótulos inteiros, para a tela poder listá-los
 *                              sem reconstruir nada a partir da frase
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrupoEscoreDto(
        String grupo,
        String rotulo,
        BigDecimal subtotal,
        BigDecimal maximo,
        ClassificacaoEscoreDto classificacao,
        String motivoAusencia,
        List<String> perguntasSemResposta
) {}

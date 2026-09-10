package com.nutri.hospitalar.clinica.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * O escore de uma escala nutricional — o que o servidor <b>publica</b>, e não o
 * que ele guardou para si.
 *
 * <p>{@code ajusteIdade} sai <b>separado</b> do total de propósito. É a armadilha
 * do denominador da adesão com o sinal invertido: quando a regra soma ou descarta
 * um valor, o servidor publica o valor — senão cada tela reimplementa a decisão, e
 * a enésima erra. Aqui a conta precisa poder ser refeita à mão no papel:
 * {@code 2 + 2 + 1 = 5}.
 *
 * <p><b>A leitura é tolerante</b> ({@code ignoreUnknown}) porque este record é
 * congelado em {@code ficha_anamnese.escore_json}: uma ficha de um ano atrás não
 * pode ficar impossível de abrir porque o DTO ganhou um campo depois. É a mesma
 * promessa de {@code OpcoesJson.paraLista}.
 *
 * @param conclusao       a conduta que a <b>publicação</b> prescreve, quando ela
 *                        prescreve alguma. A NRS-2002 tem ("iniciar plano de
 *                        terapia nutricional", "reavaliar semanalmente"); a MNA
 *                        <b>não</b> tem, e por isso vem nula ali — inventar
 *                        conduta seria acrescentar ao instrumento
 * @param total           nulo quando a escala não pode ser concluída
 * @param motivoAusencia  por que o {@code total} está nulo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscoreDto(
        String escala,
        String escalaNome,
        String referencia,
        List<GrupoEscoreDto> grupos,
        BigDecimal ajusteIdade,
        String ajusteIdadeDescricao,
        BigDecimal total,
        BigDecimal totalMaximo,
        ClassificacaoEscoreDto classificacao,
        String conclusao,
        String motivoAusencia
) {}

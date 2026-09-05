package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;

/**
 * Os derivados de um dia de acompanhamento — <b>nenhum deles é coluna</b>.
 *
 * <p>Cada um vem acompanhado do <b>motivo</b> de estar ausente, quando está: a
 * tela explica em vez de mostrar traço mudo. Ver {@code docs/11} §5.
 *
 * @param prescritoDeReferencia o volume <b>contra o qual</b> o percentual foi
 *                              medido. O {@code volPrescrito24h} do registro é o
 *                              digitado no dia; havendo avaliação vinculada, não
 *                              é ele que entra na conta
 * @param referenciaDoRecebido contra o quê o percentual comparou — o prescrito da
 *                             avaliação ou o informado no dia. A diferença
 *                             importa, e some se não for dita
 */
public record AcompanhamentoDerivadoDto(
        Integer idadeMeses,
        String motivoIdade,

        BigDecimal imc,
        String motivoImc,

        ClassificacaoDto pesoIdade,
        ClassificacaoDto estaturaIdade,
        ClassificacaoDto imcIdade,
        String motivoEstadoNutricional,

        BigDecimal prescritoDeReferencia,
        BigDecimal percentualRecebido,
        String referenciaDoRecebido,
        String motivoPercentualRecebido,

        BigDecimal caloriasRecebidas,
        BigDecimal proteinaRecebida,
        String motivoOferta,

        BigDecimal caloriasPorKg,
        BigDecimal proteinaPorKg,
        String motivoPorQuilo,

        BigDecimal adequacaoCalorica,
        String motivoAdequacaoCalorica,
        BigDecimal adequacaoProteica,
        String motivoAdequacaoProteica,

        BigDecimal aceitacaoTomadas,
        String motivoAceitacao
) {}

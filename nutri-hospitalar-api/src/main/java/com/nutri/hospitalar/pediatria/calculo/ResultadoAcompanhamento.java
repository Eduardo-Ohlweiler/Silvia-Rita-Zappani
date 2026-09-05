package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pediatria.enums.FaixaOms;

import java.math.BigDecimal;

/**
 * O que se deriva de um dia de acompanhamento pediátrico.
 *
 * <p><b>Nada disto é coluna.</b> Todos saem do que foi medido mais a avaliação
 * vinculada e a tabela da OMS — guardá-los seria criar duas versões do mesmo
 * número, que é o que o eroERP faz com o {@code % recebido}, campo digitável ao
 * lado de um calculado, os dois indo para o banco.
 *
 * <p>Cada bloco traz o <b>motivo</b> de o número faltar quando ele falta. Todo
 * caminho que não produz valor produz motivo: é a regra de {@code docs/09} §10 e
 * a lição do {@code else if} sem {@code else} registrada no {@code CLAUDE.md}.
 *
 * @param prescritoDeReferencia o volume <b>contra o qual</b> o percentual foi
 *                              medido. O digitado no dia fica registrado, mas
 *                              quando há avaliação vinculada não é ele que entra
 *                              na conta — e exibir um ao lado do outro sem
 *                              dizer qual é qual fazia a tela afirmar
 *                              "650 de 700 · 90,28 %"
 * @param referenciaDoRecebido contra o quê o percentual comparou — "prescrito na
 *                             avaliação" ou "prescrito informado no dia". A
 *                             diferença importa, e some se não for dita
 */
public record ResultadoAcompanhamento(

        // ─── Antropometria do dia ───────────────────────────────────────
        Integer idadeMeses,
        String motivoIdade,

        BigDecimal imc,
        String motivoImc,

        FaixaOms pesoIdade,
        FaixaOms estaturaIdade,
        FaixaOms imcIdade,
        String motivoEstadoNutricional,

        // ─── Dieta recebida ─────────────────────────────────────────────
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

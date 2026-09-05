package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.SuporteVentilatorio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia de acompanhamento, com os derivados.
 *
 * <p>Os campos {@code derivado*} <b>não são colunas</b>: saem do que foi medido
 * mais a avaliação vinculada, calculados na leitura. Guardá-los criaria duas
 * versões do mesmo número.
 *
 * @param prescritoDeReferencia o volume <b>contra o qual</b> o percentual foi
 *                              medido. O {@code volPrescrito24h} é o que foi
 *                              digitado no dia; quando há avaliação vinculada,
 *                              não é ele que entra na conta. Exibir um ao lado
 *                              do outro sem dizer qual é qual fazia a folha
 *                              afirmar "855 de 900 · 45,97 %"
 * @param percentualRecebido medido contra o {@code prescritoDeReferencia}, e
 *                           contra nenhum outro número
 * @param referenciaDoPercentual "prescrito na avaliação" ou "prescrito informado
 *                               no dia" — a diferença importa, e some se não for
 *                               dita
 * @param caloriasRecebidas  só existe com avaliação vinculada: precisa da
 *                           composição da fórmula
 * @param diuresePorQuiloHora só existe com avaliação vinculada: precisa do peso
 */
public record RegistroDiarioUtiResponseDto(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        UUID avaliacaoId,
        LocalDate avaliacaoData,
        BigDecimal avaliacaoVolumePrescrito,
        BigDecimal avaliacaoMetaEnergetica,
        LocalDate data,

        String dieta,
        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,

        BigDecimal mg,
        BigDecimal k,
        BigDecimal na,
        BigDecimal lactato,
        BigDecimal pcr,
        BigDecimal ph,
        BigDecimal pco2,
        BigDecimal hco3,
        BigDecimal hgt,

        SuporteVentilatorio suporteVentilatorio,
        String suporteVentilatorioDescricao,
        BigDecimal fio2Perc,
        BigDecimal paSistolica,
        BigDecimal paDiastolica,
        BigDecimal balancoHidricoMl,
        BigDecimal diureseMl,
        String evacuacao,

        BigDecimal cafeManha,
        BigDecimal lancheManha,
        BigDecimal almoco,
        BigDecimal lancheTarde,
        BigDecimal jantar,
        BigDecimal ceia,

        // ─── Derivados ──────────────────────────────────────────────────
        BigDecimal prescritoDeReferencia,
        BigDecimal percentualRecebido,
        String referenciaDoPercentual,
        BigDecimal caloriasRecebidas,
        BigDecimal proteinaRecebida,
        BigDecimal caloriasPorQuilo,
        BigDecimal proteinaPorQuilo,
        BigDecimal diuresePorQuiloHora,
        BigDecimal mediaIngestaoOral,
        String motivoDerivados,

        String observacao,
        Instant createdAt,
        Instant updatedAt
) {}

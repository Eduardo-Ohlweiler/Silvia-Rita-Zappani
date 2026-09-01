package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia de acompanhamento pediátrico, como a tela o lê.
 *
 * <p>As <b>entradas</b> vêm das colunas; os <b>derivados</b> vêm do cálculo,
 * refeito na leitura sobre a avaliação vinculada e a tabela da OMS. É o oposto da
 * avaliação, que grava os resultados: lá o número é prescrição registrada e não
 * pode mudar quando a régua mudar; aqui é leitura de acompanhamento, e recalcular
 * sobre a régua de hoje é o correto ({@code docs/11} §8).
 *
 * @param avaliacaoVolumeTotal o volume que a avaliação prescreveu — vem para a
 *                             tela poder mostrar contra o quê o percentual
 *                             comparou
 * @param derivados            tudo o que não é coluna, com o motivo de cada
 *                             ausência
 */
public record RegistroDiarioPediatricoResponseDto(
        UUID id,
        UUID pessoaId,
        String pessoaNome,

        UUID avaliacaoId,
        LocalDate avaliacaoData,
        BigDecimal avaliacaoVolumeTotal,
        BigDecimal avaliacaoVet,
        BigDecimal avaliacaoProteinaNecessidade,
        String avaliacaoFormulaNome,

        LocalDate data,

        // ─── Entradas ───────────────────────────────────────────────────
        BigDecimal pesoKg,
        BigDecimal estaturaCm,
        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,
        Integer tomadasPrevistas,
        Integer tomadasAceitas,
        String observacao,

        // ─── Derivados ──────────────────────────────────────────────────
        AcompanhamentoDerivadoDto derivados,

        Instant createdAt,
        Instant updatedAt
) {}

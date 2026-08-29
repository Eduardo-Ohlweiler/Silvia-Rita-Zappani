package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Os dias de um paciente: adesão à dieta, laboratório, balanço e hemodinâmica.
 *
 * <p>Os dias vêm inteiros, no mesmo {@link RegistroDiarioUtiResponseDto} que a
 * tela do acompanhamento já consome. É reuso deliberado: uma forma própria de
 * ponto duplicaria o cálculo dos derivados — {@code percentualRecebido},
 * {@code caloriasPorQuilo}, {@code diuresePorQuiloHora} — e no dia em que
 * divergisse, o gráfico e a lista mostrariam números diferentes do mesmo dia.
 *
 * <p><b>Sobre as médias.</b> Elas ignoram o dia em que o valor não existe:
 * um dia sem lactato não entra na média de lactato. Contar como zero faria o
 * paciente parecer melhor do que está, e é o erro que a planilha comete ao usar
 * {@code MÉDIA} sobre coluna com célula vazia formatada como zero.
 *
 * @param adesaoMedia          média de {@code percentualRecebido} nos dias que
 *                             têm o quanto foi prescrito. <b>Não é para ser
 *                             lida como nota</b>: a ESPEN recomenda oferta
 *                             abaixo de 70 % nos primeiros dias, então 60 % no
 *                             dia 2 é conduta e no dia 10 é alerta
 * @param diasSemAvaliacao     dias sem avaliação vinculada — neles kcal/kg,
 *                             proteína e diurese por quilo não existem, porque
 *                             dependem do peso e da fórmula prescritos
 * @param balancoAcumuladoMl   soma dos balanços do período. Faz sentido somar
 *                             porque cada um já é o saldo de 24 h
 */
public record PainelAcompanhamentoUtiDto(
        UUID pessoaId,
        String pessoaNome,

        LocalDate de,
        LocalDate ate,
        long totalDias,
        long diasSemAvaliacao,

        BigDecimal adesaoMedia,
        BigDecimal caloriasPorQuiloMedia,
        BigDecimal proteinaPorQuiloMedia,
        BigDecimal balancoAcumuladoMl,
        BigDecimal diureseMediaMlKgHora,
        BigDecimal ingestaoOralMedia,

        /** A avaliação vigente no último dia — a régua contra a qual se compara. */
        LocalDate ultimaAvaliacao,
        BigDecimal metaEnergetica,
        BigDecimal metaProteica,
        BigDecimal volumePrescritoNaAvaliacao,

        /** Ordem cronológica crescente: é o eixo X. */
        List<RegistroDiarioUtiResponseDto> dias
) {}

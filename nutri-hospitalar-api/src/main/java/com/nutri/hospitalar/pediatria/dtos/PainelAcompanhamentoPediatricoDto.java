package com.nutri.hospitalar.pediatria.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Os dias de uma criança: como ela cresceu, o que recebeu e quanto disso cobriu
 * a necessidade. Especificação em {@code docs/11}.
 *
 * <p>Os dias vêm inteiros, no mesmo {@link RegistroDiarioPediatricoResponseDto}
 * que a tela do acompanhamento já consome. É reuso deliberado: uma forma própria
 * de ponto duplicaria o cálculo dos derivados, e no dia em que divergisse o
 * gráfico e a lista mostrariam números diferentes do mesmo dia.
 *
 * <p><b>Sobre as médias.</b> Elas ignoram o dia em que o valor não existe — um
 * dia sem peso não entra na média de peso. Contar como zero faria a criança
 * parecer pior do que está, e é o erro que a planilha comete ao usar
 * {@code MÉDIA} sobre coluna com célula vazia formatada como zero.
 *
 * <p><b>Sobre a variação de peso.</b> É a diferença entre o primeiro e o último
 * dia <b>que têm peso medido</b>, não entre o primeiro e o último dia do
 * período: um dia sem balança no fim da janela zeraria o ganho de duas semanas.
 * O sistema <b>não</b> classifica se o ganho é adequado — a velocidade esperada
 * de ganho por idade não está na {@code Pediatria.xlsx} e não entrou nesta fatia
 * ({@code docs/11} §2).
 *
 * @param diasSemAvaliacao dias sem avaliação vinculada — neles as adequações
 *                         calórica e proteica não existem, porque falta a meta
 * @param variacaoPesoKg   ganho (positivo) ou perda (negativo) no período
 * @param dias             ordem cronológica crescente: é o eixo X
 */
public record PainelAcompanhamentoPediatricoDto(
        UUID pessoaId,
        String pessoaNome,

        LocalDate de,
        LocalDate ate,
        long totalDias,
        long diasSemAvaliacao,

        BigDecimal adesaoMedia,
        BigDecimal caloriasPorKgMedia,
        BigDecimal proteinaPorKgMedia,
        BigDecimal adequacaoCaloricaMedia,
        BigDecimal adequacaoProteicaMedia,
        BigDecimal aceitacaoTomadasMedia,

        /** O crescimento no período — o que só a pediatria tem. */
        BigDecimal pesoInicialKg,
        BigDecimal pesoFinalKg,
        BigDecimal variacaoPesoKg,
        Integer idadeInicialMeses,
        Integer idadeFinalMeses,

        /** A avaliação vigente no último dia — a régua contra a qual se compara. */
        LocalDate ultimaAvaliacao,
        BigDecimal vet,
        BigDecimal proteinaNecessidade,
        BigDecimal volumePrescritoNaAvaliacao,

        List<RegistroDiarioPediatricoResponseDto> dias
) {}

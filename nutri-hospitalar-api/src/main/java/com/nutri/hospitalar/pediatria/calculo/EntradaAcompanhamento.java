package com.nutri.hospitalar.pediatria.calculo;

import com.nutri.hospitalar.pessoa.enums.Sexo;

import java.math.BigDecimal;

/**
 * As entradas de um dia de acompanhamento pediátrico, já normalizadas.
 *
 * <p>Os quatro últimos campos vêm da <b>avaliação vinculada</b> e podem estar
 * todos nulos — o vínculo é opcional ({@code docs/11} §4). Quando estão, é deles
 * que saem as metas e a composição da fórmula.
 *
 * @param idadeMeses  a idade <b>no dia do registro</b>, calculada da data de
 *                    nascimento. Nula quando o paciente não tem nascimento
 *                    cadastrado — e aí não há classificação, com o motivo escrito
 * @param volumePrescritoNaAvaliacao o volume total/dia que a avaliação
 *                    prescreveu. <b>Vence o prescrito digitado no dia</b>: é o
 *                    que estava de fato prescrito, e não depende de alguém
 *                    repetir o número certo
 * @param formulaKcalPor100ml   composição do retrato gravado na avaliação —
 *                    <b>por 100 ml</b>, não por litro como no catálogo enteral
 */
public record EntradaAcompanhamento(
        Sexo sexo,
        Integer idadeMeses,

        BigDecimal pesoKg,
        BigDecimal estaturaCm,

        BigDecimal volPrescrito24h,
        BigDecimal volRecebido24h,
        Integer tomadasPrevistas,
        Integer tomadasAceitas,

        BigDecimal volumePrescritoNaAvaliacao,
        BigDecimal vetDaAvaliacao,
        BigDecimal proteinaNecessidadeDaAvaliacao,
        BigDecimal formulaKcalPor100ml,
        BigDecimal formulaProteinaPor100ml
) {}

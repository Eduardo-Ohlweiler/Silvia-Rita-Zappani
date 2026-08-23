package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.PreparoNoradrenalina;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * As entradas das quatro ferramentas clínicas.
 *
 * <p>As quatro são independentes — não há cascata entre elas, como há nas abas
 * do cálculo. Vêm num corpo só porque a tela recalcula tudo com um debounce só,
 * e quatro requisições por tecla seriam desperdício.
 *
 * <p><b>O peso aparece duas vezes, e é proposital:</b> {@code noraPesoKg} e
 * {@code artesanalPesoKg} são de pacientes potencialmente diferentes. Esta é
 * uma tela de conferência avulsa, não uma avaliação — quem confere a dose de
 * noradrenalina de um leito e a receita artesanal de outro não deveria ter os
 * dois amarrados. Dentro de <b>cada</b> ferramenta, porém, o peso é um só: o
 * eroERP pede o peso duas vezes na mesma aba de noradrenalina.
 *
 * @param noraPreparo os presets de 32 e 64 mcg/ml, ou o preparo do serviço.
 *                    Ampolas e soro só são lidos em {@code AMPOLAS_E_SORO}.
 * @param propofolHoras horas de infusão. Na planilha isto é a constante 24,
 *                      escondida na fórmula — propofol desligado ao meio-dia
 *                      não entregou 24 horas de caloria.
 * @param dosesBase medidas da base da dieta artesanal. Em branco, o servidor
 *                  sugere a partir do VET.
 */
public record FerramentasClinicasRequestDto(

        // ─── Noradrenalina ──────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O peso não pode ser negativo")
        @DecimalMax(value = "500.0", message = "Peso acima de 500 kg não é plausível")
        @Digits(integer = 4, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal noraPesoKg,

        @DecimalMin(value = "0.0", message = "A vazão não pode ser negativa")
        @DecimalMax(value = "1000.0", message = "Vazão acima de 1000 ml/h não é plausível")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal noraVazaoMlH,

        PreparoNoradrenalina noraPreparo,

        @DecimalMin(value = "0.0", message = "O número de ampolas não pode ser negativo")
        @DecimalMax(value = "50.0", message = "Mais de 50 ampolas não é plausível")
        @Digits(integer = 3, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal noraAmpolas,

        @DecimalMin(value = "0.0", message = "O volume do soro não pode ser negativo")
        @DecimalMax(value = "2000.0", message = "Volume acima de 2000 ml não é plausível")
        @Digits(integer = 5, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal noraVolumeSoroMl,

        // ─── Balanço nitrogenado ────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "A proteína não pode ser negativa")
        @DecimalMax(value = "1000.0", message = "Valor acima de 1000 g não é plausível")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal balancoProteinaG,

        @DecimalMin(value = "0.0", message = "A ureia não pode ser negativa")
        @DecimalMax(value = "200.0", message = "Valor acima de 200 g não é plausível")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal balancoUreiaG,

        // ─── Propofol ───────────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "A vazão não pode ser negativa")
        @DecimalMax(value = "200.0", message = "Vazão acima de 200 ml/h não é plausível")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal propofolVazaoMlH,

        @DecimalMin(value = "0.0", message = "As horas não podem ser negativas")
        @DecimalMax(value = "24.0", message = "O dia tem 24 horas")
        @Digits(integer = 2, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal propofolHoras,

        // ─── Dieta artesanal ────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O VET não pode ser negativo")
        @DecimalMax(value = "10000.0", message = "VET acima de 10000 kcal não é plausível")
        @Digits(integer = 6, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal artesanalVetKcal,

        @DecimalMin(value = "0.0", message = "O peso não pode ser negativo")
        @DecimalMax(value = "500.0", message = "Peso acima de 500 kg não é plausível")
        @Digits(integer = 4, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal artesanalPesoKg,

        UUID insumoBaseId,
        @DecimalMin(value = "0.0", message = "As doses não podem ser negativas")
        @Digits(integer = 6, fraction = 4, message = "No máximo 4 casas decimais")
        BigDecimal dosesBase,

        UUID insumoCarboidratoId,
        @DecimalMin(value = "0.0", message = "As medidas não podem ser negativas")
        @Digits(integer = 6, fraction = 4, message = "No máximo 4 casas decimais")
        BigDecimal medidasCarboidrato,

        UUID insumoProteinaId,
        @DecimalMin(value = "0.0", message = "As medidas não podem ser negativas")
        @Digits(integer = 6, fraction = 4, message = "No máximo 4 casas decimais")
        BigDecimal medidasProteina,

        UUID insumoLipidioId,
        @DecimalMin(value = "0.0", message = "As medidas não podem ser negativas")
        @Digits(integer = 6, fraction = 4, message = "No máximo 4 casas decimais")
        BigDecimal medidasLipidio,

        @Min(value = 1, message = "A receita é dividida em pelo menos uma administração")
        @Max(value = 24, message = "No máximo 24 administrações por dia")
        Integer administracoesPorDia
) {}

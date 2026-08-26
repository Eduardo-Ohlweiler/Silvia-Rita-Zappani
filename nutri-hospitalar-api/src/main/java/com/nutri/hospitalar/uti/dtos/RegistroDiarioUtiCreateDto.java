package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.SuporteVentilatorio;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia de acompanhamento a gravar.
 *
 * <p><b>Não existe campo de percentual recebido.</b> No eroERP ele é digitável,
 * ao lado de um "% recebido (calculado)" que a tela mostra — o mesmo valor duas
 * vezes, e os dois vão para o banco podendo divergir. Aqui o percentual é
 * derivado do volume, e não há como discordar dele.
 *
 * @param avaliacaoId a avaliação que estava valendo. Opcional: paciente que
 *                    internou de madrugada tem dia antes de avaliação. A tela
 *                    pede a sugestão ao servidor e manda o que o usuário
 *                    confirmar — o vínculo nunca acontece em silêncio.
 * @param fio2Perc    separada do modo ventilatório, que é enum. No eroERP os
 *                    dois moram num campo de texto só.
 */
public record RegistroDiarioUtiCreateDto(

        @NotNull(message = "Informe o paciente")
        UUID pessoaId,

        UUID avaliacaoId,

        @NotNull(message = "Informe a data")
        @PastOrPresent(message = "A data do acompanhamento não pode ser no futuro")
        LocalDate data,

        // ─── Dieta e TNE ────────────────────────────────────────────────
        @Size(max = 255, message = "No máximo 255 caracteres")
        String dieta,

        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @DecimalMax(value = "10000.0", message = "Volume acima de 10000 ml não é plausível")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volPrescrito24h,

        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @DecimalMax(value = "10000.0", message = "Volume acima de 10000 ml não é plausível")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volRecebido24h,

        // ─── Laboratório ────────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O magnésio não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal mg,

        @DecimalMin(value = "0.0", message = "O potássio não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal k,

        @DecimalMin(value = "0.0", message = "O sódio não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal na,

        @DecimalMin(value = "0.0", message = "O lactato não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal lactato,

        @DecimalMin(value = "0.0", message = "A PCR não pode ser negativa")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal pcr,

        @DecimalMin(value = "6.0", message = "pH abaixo de 6,0 não é compatível com a vida")
        @DecimalMax(value = "8.0", message = "pH acima de 8,0 não é compatível com a vida")
        @Digits(integer = 2, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal ph,

        @DecimalMin(value = "0.0", message = "A pCO₂ não pode ser negativa")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal pco2,

        @DecimalMin(value = "0.0", message = "O bicarbonato não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal hco3,

        @DecimalMin(value = "0.0", message = "A glicemia não pode ser negativa")
        @DecimalMax(value = "1500.0", message = "Glicemia acima de 1500 mg/dL não é plausível")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal hgt,

        // ─── Clínica e balanço ──────────────────────────────────────────
        SuporteVentilatorio suporteVentilatorio,

        @DecimalMin(value = "21.0", message = "A FiO₂ mínima é 21 % — o ar ambiente")
        @DecimalMax(value = "100.0", message = "A FiO₂ máxima é 100 %")
        @Digits(integer = 3, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal fio2Perc,

        @DecimalMin(value = "0.1", message = "A pressão sistólica deve ser maior que zero")
        @DecimalMax(value = "300.0", message = "Pressão acima de 300 mmHg não é plausível")
        @Digits(integer = 4, fraction = 1, message = "No máximo 1 casa decimal")
        BigDecimal paSistolica,

        @DecimalMin(value = "0.1", message = "A pressão diastólica deve ser maior que zero")
        @DecimalMax(value = "200.0", message = "Pressão acima de 200 mmHg não é plausível")
        @Digits(integer = 4, fraction = 1, message = "No máximo 1 casa decimal")
        BigDecimal paDiastolica,

        /* Sem @DecimalMin: é a única grandeza da tabela que pode ser negativa. */
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal balancoHidricoMl,

        @DecimalMin(value = "0.0", message = "A diurese não pode ser negativa")
        @DecimalMax(value = "20000.0", message = "Diurese acima de 20000 ml não é plausível")
        @Digits(integer = 8, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal diureseMl,

        @Size(max = 100, message = "No máximo 100 caracteres")
        String evacuacao,

        // ─── Ingestão oral ──────────────────────────────────────────────
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal cafeManha,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal lancheManha,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal almoco,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal lancheTarde,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal jantar,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
        @Digits(integer = 3, fraction = 2) BigDecimal ceia,

        @Size(max = 5000, message = "No máximo 5000 caracteres")
        String observacao
) implements MedidasDoDia {}

package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * As entradas de um cálculo, sem paciente e sem persistência.
 *
 * <p>Nada aqui é obrigatório de propósito: a tela recalcula a cada alteração e
 * o usuário vai preenchendo aos poucos. Faltando uma entrada, o resultado que
 * dependia dela vem ausente <b>com o motivo</b> — não com um 400 que apagaria a
 * tela inteira a cada tecla.
 *
 * <p>Os limites são de sanidade, para barrar digitação absurda antes de ela
 * virar prescrição: 50 anos de idade, 500 kg, 300 cm.
 */
public record CalculoPediatricoRequestDto(

        Sexo sexo,

        @Min(value = 0, message = "A idade não pode ser negativa")
        @Max(value = 600, message = "Idade em meses fora do razoável")
        Integer idadeMeses,

        @DecimalMin(value = "0.0", message = "O peso não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal peso,

        @DecimalMin(value = "0.0", message = "A estatura não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal estatura,

        UUID formulaLacteaId,

        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volumeMl,

        /** Intervalo entre tomadas, em horas — não a quantidade delas. */
        @DecimalMin(value = "0.0", message = "A frequência não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal frequenciaHoras
) {}

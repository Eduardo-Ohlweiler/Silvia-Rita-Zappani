package com.nutri.hospitalar.pediatria.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * O que se grava numa avaliação: <b>só as entradas</b>.
 *
 * <p>Repare no que não está aqui: IMC, classificação, VET, adequação. Nenhum
 * resultado é aceito do cliente — o servidor recalcula e grava o resultado do
 * próprio cálculo. No eroERP o DTO trazia os doze resultados e o backend os
 * gravava como vieram; um cliente adulterado escrevia {@code imc: 999} no
 * prontuário.
 *
 * <p>O sexo é exigido mesmo já existindo em {@code pessoa}: é a entrada que
 * escolhe a curva da OMS, e o cadastro pode não tê-lo.
 */
public record AvaliacaoPediatricaCreateDto(

        @NotNull(message = "Selecione o paciente")
        UUID pacienteId,

        UUID profissionalId,

        @NotNull(message = "Informe a data da avaliação")
        LocalDate dataAvaliacao,

        @NotNull(message = "Informe o sexo")
        Sexo sexo,

        @NotNull(message = "Informe a idade em meses")
        @Min(value = 0, message = "A idade não pode ser negativa")
        @Max(value = 600, message = "Idade em meses fora do razoável")
        Integer idadeMeses,

        @NotNull(message = "Informe o peso")
        @DecimalMin(value = "0.001", message = "O peso deve ser maior que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal peso,

        @DecimalMin(value = "0.001", message = "A estatura deve ser maior que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal estatura,

        UUID formulaLacteaId,

        @DecimalMin(value = "0.001", message = "O volume deve ser maior que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volumeMl,

        /** Intervalo entre tomadas, em horas — não a quantidade delas. */
        @DecimalMin(value = "0.001", message = "A frequência deve ser maior que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal frequenciaHoras,

        @Size(max = 2000, message = "No máximo 2000 caracteres")
        String observacao
) {}

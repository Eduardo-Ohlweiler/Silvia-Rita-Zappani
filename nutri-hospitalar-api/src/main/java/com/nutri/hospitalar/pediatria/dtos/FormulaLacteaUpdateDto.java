package com.nutri.hospitalar.pediatria.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Mesmos campos do cadastro. `ativo` não entra: tem o seu próprio
 * `PATCH /{id}/ativo`, como em pessoa.
 */
public record FormulaLacteaUpdateDto(

        @NotBlank(message = "Informe o nome da fórmula")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotNull(message = "Informe as calorias por 100 ml")
        @DecimalMin(value = "0.001", message = "As calorias devem ser maiores que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal kcalPor100ml,

        @NotNull(message = "Informe a proteína por 100 ml")
        @DecimalMin(value = "0.0", message = "A proteína não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal proteinaPor100ml

) {}

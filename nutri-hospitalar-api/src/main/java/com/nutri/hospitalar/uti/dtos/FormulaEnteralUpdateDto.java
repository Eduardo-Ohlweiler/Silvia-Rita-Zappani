package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Mesmos campos do cadastro. {@code ativo} não entra: tem o seu próprio
 * {@code PATCH /{id}/ativo}, como em fórmula láctea e em pessoa.
 */
public record FormulaEnteralUpdateDto(

        @NotBlank(message = "Informe o nome da fórmula")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        CategoriaFormulaEnteral categoria,

        @NotNull(message = "Informe a densidade calórica")
        @DecimalMin(value = "0.001", message = "A densidade deve ser maior que zero")
        @DecimalMax(value = "5.0", message = "Densidade acima de 5 kcal/ml não existe em fórmula enteral")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal densidadeKcalMl,

        @NotNull(message = "Informe a proteína por litro")
        @DecimalMin(value = "0.0", message = "A proteína não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal proteinaGL,

        @DecimalMin(value = "0.0", message = "O carboidrato não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal choGL,

        @DecimalMin(value = "0.0", message = "O lipídio não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal lipGL,

        @DecimalMin(value = "0.0", message = "As fibras não podem ser negativas")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal fibrasGL,

        @DecimalMin(value = "0.0", message = "O potássio não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal potassioMgL,

        @DecimalMin(value = "0.0", message = "A osmolaridade não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal osmolaridadeMosmL,

        @DecimalMin(value = "0.0", message = "A água livre não pode ser negativa")
        @DecimalMax(value = "100.0", message = "A água livre não passa de 100 %")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal aguaLivrePerc

) {}

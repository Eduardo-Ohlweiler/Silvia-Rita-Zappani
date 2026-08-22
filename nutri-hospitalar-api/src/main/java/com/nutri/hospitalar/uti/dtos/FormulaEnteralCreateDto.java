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
 * Composição declarada <b>por litro</b>, sempre — mesmo para o produto que vem
 * em frasco de 500 ml.
 *
 * <p>Não existe campo de apresentação de propósito. Foi a apresentação de 500 ml
 * que produziu o pior defeito da planilha de origem: o
 * {@code Fresubin 2kcal HP} cadastrado por embalagem, dividido por 1000 numa aba
 * e por 500 na outra, com <b>proteína subestimada em 50 %</b> numa delas. Ver
 * {@code docs/10} §8.1.
 *
 * <p>Quem cadastra converte uma vez: 50 g em 500 ml são 100 g/L. E o
 * {@code CHECK} de fechamento energético do banco recusa quem esquecer — a soma
 * dos macros por Atwater tem de ficar a menos de 12 % da densidade declarada.
 *
 * @param aguaLivrePerc teor de água livre em % do volume, como vem no rótulo.
 *                      Deixar em branco é legítimo: o cálculo cai na tabela por
 *                      densidade e <b>diz que caiu</b>. Preencher pelo rótulo é
 *                      melhor — a escada por densidade é aproximação por faixa.
 *                      Ver {@code docs/10} §5.1.
 */
public record FormulaEnteralCreateDto(

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
        BigDecimal aguaLivrePerc,

        Boolean ativo
) {}

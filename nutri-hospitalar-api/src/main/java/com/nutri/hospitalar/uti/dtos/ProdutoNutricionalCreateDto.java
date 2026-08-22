package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Composição declarada <b>por medida</b> — a medida que o rótulo usa.
 *
 * <p><b>Não existe campo {@code moduloProteico}.</b> Ele é derivado de
 * {@code tipo} pelo servidor, e não aceito do cliente: são o mesmo fato dito
 * duas vezes, e campo redundante que trafega é campo que pode chegar
 * discordando. O banco impõe que os dois concordem; aqui eles não têm como
 * discordar.
 *
 * <p>As regras de coerência que dependem do tipo — papel artesanal obrigatório
 * em insumo e proibido nos outros, composição obrigatória em quem entra em
 * cálculo — são conferidas no service, com mensagem que diz o que fazer.
 * Bean Validation não cruza campos, e violação de {@code CHECK} chegaria ao
 * usuário como erro de banco.
 *
 * @param medidaNome como a medida se chama no rótulo: "medida", "sachê",
 *                   "frasco", "ml". É o que a tela escreve ao lado do número.
 * @param medidaQtd  gramas ou ml de UMA medida — o denominador da composição.
 * @param embalagemQtd gramas ou ml da embalagem fechada. Sem ela não há cálculo
 *                     de latas por mês, que é o que vai para a compra.
 */
public record ProdutoNutricionalCreateDto(

        @NotBlank(message = "Informe o nome do produto")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotNull(message = "Informe o tipo do produto")
        TipoProdutoNutricional tipo,

        @NotBlank(message = "Informe o nome da medida (medida, sachê, frasco, ml)")
        @Size(max = 30, message = "No máximo 30 caracteres")
        String medidaNome,

        @NotNull(message = "Informe a quantidade da medida")
        @DecimalMin(value = "0.001", message = "A medida deve ser maior que zero")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal medidaQtd,

        @DecimalMin(value = "0.001", message = "A embalagem deve ser maior que zero")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal embalagemQtd,

        @DecimalMin(value = "0.0", message = "As calorias não podem ser negativas")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal kcal,

        @DecimalMin(value = "0.0", message = "A proteína não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal proteinaG,

        @DecimalMin(value = "0.0", message = "O carboidrato não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal choG,

        @DecimalMin(value = "0.0", message = "O açúcar não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal acucarG,

        @DecimalMin(value = "0.0", message = "O lipídio não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal lipG,

        @DecimalMin(value = "0.0", message = "O sódio não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal sodioMg,

        @DecimalMin(value = "0.0", message = "O potássio não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal potassioMg,

        @DecimalMin(value = "0.0", message = "O fósforo não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal fosforoMg,

        @DecimalMin(value = "0.0", message = "O ferro não pode ser negativo")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal ferroMg,

        @DecimalMin(value = "0.0", message = "As fibras não podem ser negativas")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal fibrasG,

        @DecimalMin(value = "0.0", message = "A osmolaridade não pode ser negativa")
        @Digits(integer = 5, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal osmolaridadeMosmL,

        PapelArtesanal papelArtesanal,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String observacao,

        Boolean ativo
) {}

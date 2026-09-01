package com.nutri.hospitalar.pediatria.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia de acompanhamento pediátrico a alterar. Ver {@code docs/11} §4.
 *
 * <p><b>Nenhum derivado é campo.</b> Não existe percentual recebido, adequação
 * nem classificação da OMS aqui: todos saem na leitura, sobre a avaliação
 * vinculada e a tabela de percentis. No eroERP o {@code % recebido} é digitável
 * ao lado de um calculado, e os dois vão para o banco podendo divergir.
 *
 * <p><b>A idade também não é campo</b>, e é o ponto que separa esta tabela da de
 * UTI: ela sai da data de nascimento e da data do registro ({@code docs/11} §3).
 * Idade digitada por dia divergiria do calendário na primeira distração.
 *
 * @param avaliacaoId a avaliação que estava valendo. Opcional: criança que
 *                    internou de madrugada tem dia antes de avaliação. A tela
 *                    pede a sugestão ao servidor e manda o que o usuário
 *                    confirmar — o vínculo nunca acontece em silêncio.
 */
public record RegistroDiarioPediatricoUpdateDto(

        @NotNull(message = "Informe o paciente")
        UUID pessoaId,

        UUID avaliacaoId,

        @NotNull(message = "Informe a data")
        @PastOrPresent(message = "A data do acompanhamento não pode ser no futuro")
        LocalDate data,

        // ─── Antropometria do dia ───────────────────────────────────────
        @DecimalMin(value = "0.1", message = "O peso deve ser maior que zero")
        @DecimalMax(value = "150.0", message = "Peso acima de 150 kg não é de paciente pediátrico")
        @Digits(integer = 3, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal pesoKg,

        @DecimalMin(value = "20.0", message = "Estatura abaixo de 20 cm não é medida plausível")
        @DecimalMax(value = "200.0", message = "Estatura acima de 200 cm não é de paciente pediátrico")
        @Digits(integer = 3, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal estaturaCm,

        // ─── Dieta láctea ───────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volPrescrito24h,

        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @Digits(integer = 7, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volRecebido24h,

        @Min(value = 1, message = "As tomadas previstas devem ser pelo menos 1")
        @Max(value = 24, message = "Mais de 24 tomadas num dia não é plausível")
        Integer tomadasPrevistas,

        @Min(value = 0, message = "As tomadas aceitas não podem ser negativas")
        @Max(value = 24, message = "Mais de 24 tomadas num dia não é plausível")
        Integer tomadasAceitas,

        @Size(max = 2000, message = "No máximo 2000 caracteres")
        String observacao

) implements MedidasDoDiaPediatrico {}

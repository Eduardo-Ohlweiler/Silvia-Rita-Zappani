package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.uti.enums.MotivoEncerramento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Encerra o acompanhamento nutricional de uma avaliação.
 *
 * <p>Data e motivo são <b>obrigatórios juntos</b>, como o {@code CHECK} da
 * migration 029 exige: encerrar sem dizer por quê deixa a estatística do serviço
 * com um buraco que ninguém preenche depois.
 *
 * <p>{@code @PastOrPresent} pela mesma razão de {@code dataAvaliacao}: isto é
 * registro do que aconteceu, não agendamento de alta.
 */
public record EncerramentoUtiDto(

        @NotNull(message = "Informe a data em que o acompanhamento terminou")
        @PastOrPresent(message = "A data do encerramento não pode ser no futuro")
        LocalDate encerradoEm,

        @NotNull(message = "Informe o motivo do encerramento")
        MotivoEncerramento motivo,

        @Size(max = 500, message = "No máximo 500 caracteres")
        String observacao
) {}

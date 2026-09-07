package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param dataPreenchimento dia do calendário, nunca instante. É
 *                          {@code @PastOrPresent} — e o front tem de mandar a
 *                          data <b>local</b>, com {@code hojeIso()}: um
 *                          {@code toISOString()} às 22h manda amanhã, e o
 *                          plantão noturno inteiro levaria 400 sem entender.
 */
public record FichaAnamneseCreateDto(

        @NotNull(message = "Informe o paciente")
        UUID pacienteId,

        UUID profissionalId,

        @NotNull(message = "Informe a data de preenchimento")
        @PastOrPresent(message = "A data não pode ser futura")
        LocalDate dataPreenchimento,

        @NotNull(message = "Informe o modelo da ficha")
        UUID modeloId,

        @Valid
        List<RespostaFichaDto> respostas,

        String observacao
) {}

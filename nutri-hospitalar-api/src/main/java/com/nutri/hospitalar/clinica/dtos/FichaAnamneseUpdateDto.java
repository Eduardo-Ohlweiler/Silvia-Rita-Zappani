package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Regravar a ficha <b>atualiza o retrato</b> a partir dos campos vivos — é o
 * mesmo encontro sendo corrigido, com quem corrige vendo o que o modelo pergunta
 * hoje. O que nunca acontece é o retrato mudar <b>sem</b> alguém regravar.
 *
 * <p>Trocar o {@code modeloId} descarta as respostas antigas e recomeça: meio
 * caminho seria uma ficha com metade das perguntas de um modelo e metade de
 * outro.
 */
public record FichaAnamneseUpdateDto(

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

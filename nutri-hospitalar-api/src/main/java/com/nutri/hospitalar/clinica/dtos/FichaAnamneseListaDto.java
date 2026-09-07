package com.nutri.hospitalar.clinica.dtos;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A linha da listagem.
 *
 * <p>{@code modeloNome} vem do <b>retrato</b> da ficha, não do modelo: a lista
 * tem de continuar dizendo de qual modelo cada ficha veio mesmo depois de o
 * modelo ser apagado ou renomeado.
 *
 * @param respondidas quantas perguntas têm resposta, de {@code totalPerguntas}.
 *                    É o que distingue a ficha completa da que ficou pela
 *                    metade, sem precisar abrir.
 */
public record FichaAnamneseListaDto(
        UUID id,
        UUID pacienteId,
        String pacienteNome,
        String profissionalNome,
        LocalDate dataPreenchimento,
        String modeloNome,
        int respondidas,
        int totalPerguntas
) {}

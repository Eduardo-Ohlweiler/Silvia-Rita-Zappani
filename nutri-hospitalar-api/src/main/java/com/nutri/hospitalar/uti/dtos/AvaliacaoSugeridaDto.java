package com.nutri.hospitalar.uti.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A avaliação que o servidor sugere vincular a um dia.
 *
 * <p>É <b>sugestão</b>, não vínculo automático: a tela mostra qual seria e o
 * usuário confirma ou troca. Ligar em silêncio faria o número de kcal/kg mudar
 * sem que ninguém tivesse escolhido a referência.
 *
 * <p>Ausência é resposta válida — paciente que internou de madrugada tem dia
 * antes de avaliação, e o motivo acompanha.
 */
public record AvaliacaoSugeridaDto(
        UUID id,
        LocalDate dataAvaliacao,
        BigDecimal pesoTrabalhoKg,
        BigDecimal metaEnergetica,
        BigDecimal volumePrescrito,
        String formulaNome,
        String motivo
) {}

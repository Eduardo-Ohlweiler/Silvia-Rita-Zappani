package com.nutri.hospitalar.clinica.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param doSistema modelo global, visível a todo cliente e editável por nenhum.
 *                  A tela usa isto para abrir em somente leitura e oferecer
 *                  Clonar em vez de Salvar — um botão que existe e recusa é pior
 *                  que um botão que não existe.
 */
public record ModeloFichaResponseDto(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        boolean doSistema,
        List<CampoFichaResponseDto> campos,
        Instant createdAt,
        Instant updatedAt
) {}

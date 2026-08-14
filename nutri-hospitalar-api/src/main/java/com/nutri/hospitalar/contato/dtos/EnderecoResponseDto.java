package com.nutri.hospitalar.contato.dtos;

import java.util.UUID;

/**
 * Leva o nome da cidade e a UF junto: a tela mostra "Porto Alegre/RS" sem uma
 * segunda chamada ao servidor.
 */
public record EnderecoResponseDto(
        UUID id,
        UUID tipoEnderecoId,
        String tipoEnderecoNome,
        UUID cidadeId,
        String cidadeNome,
        String estadoSigla,
        String estadoNome,
        String cep,
        String rua,
        String numero,
        String bairro,
        String complemento,
        Boolean principal
) {}

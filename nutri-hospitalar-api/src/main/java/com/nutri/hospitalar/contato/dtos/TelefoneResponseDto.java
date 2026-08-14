package com.nutri.hospitalar.contato.dtos;

import java.util.UUID;

public record TelefoneResponseDto(
        UUID id,
        UUID tipoTelefoneId,
        String tipoTelefoneNome,
        String codigoPais,
        String numero,
        String observacao,
        Boolean principal
) {}

package com.nutri.hospitalar.contato.dtos;

import java.util.UUID;

public record RedeSocialResponseDto(
        UUID id,
        UUID tipoRedeSocialId,
        String tipoRedeSocialNome,
        String usuario,
        String url,
        String observacao
) {}

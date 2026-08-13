package com.nutri.hospitalar.exceptions;

import java.time.Instant;

public record ErroResponseDto(
        String erro,
        int codigo,
        Instant timestamp,
        String path
) {
    public static ErroResponseDto de(String erro, int codigo, String path) {
        return new ErroResponseDto(erro, codigo, Instant.now(), path);
    }
}

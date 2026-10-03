package com.nutri.hospitalar.configuracao.dtos;

import com.nutri.hospitalar.configuracao.enums.PaletaSistema;
import jakarta.validation.constraints.NotNull;

public record AparenciaUpdateDto(
        @NotNull(message = "Escolha a paleta")
        PaletaSistema paleta
) {
}

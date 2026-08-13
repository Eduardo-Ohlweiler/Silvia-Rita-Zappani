package com.nutri.hospitalar.usuario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SenhaUpdateDto(

        @NotBlank(message = "Informe a senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 10, max = 72, message = "A senha deve ter entre 10 e 72 caracteres")
        String senhaNova
) {}

package com.nutri.hospitalar.usuario.dtos;

import com.nutri.hospitalar.usuario.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String email,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String telefone,

        @Pattern(regexp = "\\d{1,4}", message = "Código do país inválido")
        String codigoPais,

        @NotNull(message = "Informe o nível de acesso")
        Role role
) {}

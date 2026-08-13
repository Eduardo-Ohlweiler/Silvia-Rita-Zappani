package com.nutri.hospitalar.usuario.dtos;

import com.nutri.hospitalar.tenant.enums.PeriodoAcesso;
import com.nutri.hospitalar.usuario.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UsuarioCreateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String email,

        @NotBlank(message = "Informe a senha")
        @Size(min = 10, max = 72, message = "A senha deve ter entre 10 e 72 caracteres")
        String senha,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String telefone,

        @Pattern(regexp = "\\d{1,4}", message = "Código do país inválido")
        String codigoPais,

        Role role,

        UUID tenantId,

        /**
         * Licença do cliente. Só vale quando o tenant nasce aqui — entrando num
         * tenant existente é ignorado, como acontece com a {@code role} no
         * caminho de cliente novo. Nulo é {@code INDETERMINADO}.
         */
        PeriodoAcesso periodoAcesso
) {}

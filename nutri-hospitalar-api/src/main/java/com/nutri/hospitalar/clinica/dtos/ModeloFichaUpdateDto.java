package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** A lista de campos é o estado completo: o que não veio, foi removido. */
public record ModeloFichaUpdateDto(

        @NotBlank(message = "Informe o nome do modelo")
        @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
        String nome,

        String descricao,

        @NotEmpty(message = "O modelo precisa de ao menos uma pergunta")
        @Valid
        List<CampoFichaDto> campos,

        Boolean ativo
) {}

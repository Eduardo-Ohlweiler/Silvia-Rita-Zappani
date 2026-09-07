package com.nutri.hospitalar.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Modelo novo, com as suas perguntas na mesma carga.
 *
 * <p>O modelo criado aqui é <b>sempre do tenant logado</b>. Não há como criar
 * modelo do sistema pela API: os três que existem vêm do seed da migration 030,
 * e é isso que os torna iguais para todo cliente.
 */
public record ModeloFichaCreateDto(

        @NotBlank(message = "Informe o nome do modelo")
        @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
        String nome,

        String descricao,

        /* Modelo sem pergunta é ficha em branco — não há o que responder. */
        @NotEmpty(message = "O modelo precisa de ao menos uma pergunta")
        @Valid
        List<CampoFichaDto> campos,

        Boolean ativo
) {}

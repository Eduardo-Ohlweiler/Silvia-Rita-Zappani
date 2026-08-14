package com.nutri.hospitalar.pessoa.dtos;

import com.nutri.hospitalar.contato.dtos.EmailItemDto;
import com.nutri.hospitalar.contato.dtos.RedeSocialItemDto;
import com.nutri.hospitalar.contato.dtos.TelefoneItemDto;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * As listas de contato são o estado completo, não um acréscimo: item com
 * {@code id} é atualizado, sem {@code id} é criado, e o que não vier é
 * removido. Enviar {@code null} numa lista apaga todos os itens dela.
 *
 * <p>{@code ativo} não entra aqui — tem endpoint próprio
 * ({@code PATCH /{id}/ativo}), como no módulo de usuários.
 */
public record PessoaUpdateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotNull(message = "Informe o tipo de pessoa")
        TipoPessoa tipoPessoa,

        LocalDate dataNascimento,

        @Size(max = 14, message = "CPF inválido")
        String cpf,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String rg,

        @Size(max = 18, message = "CNPJ inválido")
        String cnpj,

        @Size(max = 50, message = "No máximo 50 caracteres")
        String inscricaoEstadual,

        @Size(max = 50, message = "No máximo 50 caracteres")
        String inscricaoMunicipal,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String nomeFantasia,

        @Size(max = 255, message = "No máximo 255 caracteres")
        String razaoSocial,

        @Size(max = 500, message = "No máximo 500 caracteres")
        String observacao,

        @NotEmpty(message = "Selecione ao menos um tipo de cadastro")
        Set<UUID> tiposCadastroIds,

        List<@Valid TelefoneItemDto> telefones,

        List<@Valid EmailItemDto> emails,

        List<@Valid RedeSocialItemDto> redesSociais
) {}

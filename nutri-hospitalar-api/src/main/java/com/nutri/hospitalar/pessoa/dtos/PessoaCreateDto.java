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
 * Documentos podem vir com máscara: o service reduz a dígitos antes de validar
 * e gravar. Os campos do tipo errado (CNPJ numa pessoa física, por exemplo)
 * são recusados com 400, não ignorados em silêncio.
 */
public record PessoaCreateDto(

        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "No máximo 255 caracteres")
        String nome,

        @NotNull(message = "Informe o tipo de pessoa")
        TipoPessoa tipoPessoa,

        // ─── Pessoa física ──────────────────────────────────────────────
        LocalDate dataNascimento,

        @Size(max = 14, message = "CPF inválido")
        String cpf,

        @Size(max = 20, message = "No máximo 20 caracteres")
        String rg,

        // ─── Pessoa jurídica ────────────────────────────────────────────
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

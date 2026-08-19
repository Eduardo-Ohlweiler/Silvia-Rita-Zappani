package com.nutri.hospitalar.pessoa.dtos;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.contato.dtos.EmailResponseDto;
import com.nutri.hospitalar.contato.dtos.EnderecoResponseDto;
import com.nutri.hospitalar.contato.dtos.RedeSocialResponseDto;
import com.nutri.hospitalar.contato.dtos.TelefoneResponseDto;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.vinculo.dtos.VinculoResponseDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PessoaResponseDto(
        UUID id,
        String nome,
        TipoPessoa tipoPessoa,
        LocalDate dataNascimento,
        String cpf,
        String rg,
        Sexo sexo,
        String cnpj,
        String inscricaoEstadual,
        String inscricaoMunicipal,
        String nomeFantasia,
        String razaoSocial,
        String observacao,
        Boolean ativo,
        Set<CatalogoSelectDto> tiposCadastro,
        List<TelefoneResponseDto> telefones,
        List<EmailResponseDto> emails,
        List<RedeSocialResponseDto> redesSociais,
        List<EnderecoResponseDto> enderecos,
        List<VinculoResponseDto> vinculos,
        Instant createdAt,
        Instant updatedAt
) {}

package com.nutri.hospitalar.pessoa.mapper;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.contato.mapper.ContatoMapper;
import com.nutri.hospitalar.pessoa.dtos.PessoaResponseDto;
import com.nutri.hospitalar.pessoa.dtos.PessoaSelectDto;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.vinculo.entity.PessoaVinculo;
import com.nutri.hospitalar.vinculo.mapper.PessoaVinculoMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PessoaMapper {

    private PessoaMapper() {}

    /**
     * Para a listagem: sem vínculos.
     *
     * <p>Vínculo não é coleção da pessoa — mora nos dois sentidos e exige
     * consulta própria. Carregá-lo por linha na listagem seria um N+1 para
     * mostrar o que a tela nem exibe.
     */
    public static PessoaResponseDto toResponse(Pessoa pessoa) {
        return toResponse(pessoa, List.of());
    }

    public static PessoaResponseDto toResponse(Pessoa pessoa, List<PessoaVinculo> vinculos) {
        return new PessoaResponseDto(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getTipoPessoa(),
                pessoa.getDataNascimento(),
                pessoa.getCpf(),
                pessoa.getRg(),
                pessoa.getCnpj(),
                pessoa.getInscricaoEstadual(),
                pessoa.getInscricaoMunicipal(),
                pessoa.getNomeFantasia(),
                pessoa.getRazaoSocial(),
                pessoa.getObservacao(),
                pessoa.getAtivo(),
                tiposCadastro(pessoa),
                ContatoMapper.toTelefoneList(pessoa.getTelefones()),
                ContatoMapper.toEmailList(pessoa.getEmails()),
                ContatoMapper.toRedeSocialList(pessoa.getRedesSociais()),
                ContatoMapper.toEnderecoList(pessoa.getEnderecos()),
                PessoaVinculoMapper.toResponseList(pessoa, vinculos),
                pessoa.getCreatedAt(),
                pessoa.getUpdatedAt());
    }

    public static PessoaSelectDto toSelect(Pessoa pessoa) {
        return new PessoaSelectDto(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.ehPessoaFisica() ? pessoa.getCpf() : pessoa.getCnpj());
    }

    private static Set<CatalogoSelectDto> tiposCadastro(Pessoa pessoa) {
        if (pessoa.getTiposCadastro() == null) return Set.of();
        return pessoa.getTiposCadastro().stream()
                .map(t -> new CatalogoSelectDto(t.getId(), t.getNome()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

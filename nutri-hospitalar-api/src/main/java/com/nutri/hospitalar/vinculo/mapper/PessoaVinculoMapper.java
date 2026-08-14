package com.nutri.hospitalar.vinculo.mapper;

import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.vinculo.dtos.VinculoResponseDto;
import com.nutri.hospitalar.vinculo.entity.PessoaVinculo;
import com.nutri.hospitalar.vinculo.enums.TipoVinculo;

import java.util.List;
import java.util.UUID;

public final class PessoaVinculoMapper {

    private PessoaVinculoMapper() {}

    /**
     * Monta a lista na perspectiva da pessoa informada.
     *
     * <p>Quando ela é a origem da aresta, o rótulo é o próprio tipo; quando é o
     * destino, é o inverso. É aqui que uma linha só vira "responsável" num
     * cadastro e "dependente" no outro.
     */
    public static List<VinculoResponseDto> toResponseList(Pessoa pessoa,
                                                          List<PessoaVinculo> vinculos) {
        if (pessoa == null || vinculos == null) return List.of();

        return vinculos.stream()
                .map(v -> paraPerspectivaDe(pessoa.getId(), v))
                .toList();
    }

    private static VinculoResponseDto paraPerspectivaDe(UUID pessoaId, PessoaVinculo vinculo) {
        boolean souAOrigem = pessoaId.equals(vinculo.getPessoaOrigem().getId());

        Pessoa outra = souAOrigem ? vinculo.getPessoaDestino() : vinculo.getPessoaOrigem();
        TipoVinculo tipo = souAOrigem ? vinculo.getTipo() : vinculo.getTipo().inverso();

        return new VinculoResponseDto(
                vinculo.getId(),
                outra.getId(),
                outra.getNome(),
                outra.ehPessoaFisica() ? outra.getCpf() : outra.getCnpj(),
                tipo,
                tipo.getDescricao(),
                vinculo.getObservacao());
    }
}

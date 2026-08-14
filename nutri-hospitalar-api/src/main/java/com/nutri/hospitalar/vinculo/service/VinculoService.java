package com.nutri.hospitalar.vinculo.service;

import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.vinculo.dtos.VinculoItemDto;
import com.nutri.hospitalar.vinculo.entity.PessoaVinculo;
import com.nutri.hospitalar.vinculo.enums.TipoVinculo;
import com.nutri.hospitalar.vinculo.repository.PessoaVinculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sincroniza os vínculos de uma pessoa, no mesmo contrato das listas de
 * contato: o que vem é o estado completo.
 *
 * <p>A diferença é que o vínculo tem dois donos. Remover pelo cadastro de um
 * remove do outro — é uma relação, não uma anotação particular.
 */
@Service
@RequiredArgsConstructor
public class VinculoService {

    private final PessoaVinculoRepository vinculoRepository;
    private final PessoaRepository pessoaRepository;

    @Transactional(readOnly = true)
    public List<PessoaVinculo> daPessoa(Pessoa pessoa) {
        return vinculoRepository.findAllByPessoaIdAndTenantId(
                pessoa.getId(), pessoa.getTenant().getId());
    }

    @Transactional
    public List<PessoaVinculo> sincronizar(Pessoa pessoa, List<VinculoItemDto> itens) {
        UUID tenantId = pessoa.getTenant().getId();

        Map<UUID, PessoaVinculo> existentes = new LinkedHashMap<>();
        vinculoRepository.findAllByPessoaIdAndTenantId(pessoa.getId(), tenantId)
                .forEach(v -> existentes.put(v.getId(), v));

        if (itens == null || itens.isEmpty()) {
            vinculoRepository.deleteAll(existentes.values());
            return List.of();
        }

        Set<UUID> recebidos = itens.stream()
                .map(VinculoItemDto::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        existentes.values().stream()
                .filter(v -> !recebidos.contains(v.getId()))
                .forEach(vinculoRepository::delete);

        // As remoções precisam chegar ao banco ANTES das inserções: trocar um
        // vínculo por outro no mesmo PUT, sem isto, pode ter a ordem invertida
        // pelo Hibernate e esbarrar em uk_pessoa_vinculo_par.
        vinculoRepository.flush();

        List<PessoaVinculo> resultado = new ArrayList<>();

        for (VinculoItemDto item : itens) {
            if (pessoa.getId().equals(item.pessoaId()))
                throw new BadRequestException("Não é possível vincular a pessoa a ela mesma");

            resultado.add(item.id() == null
                    ? criar(pessoa, item, tenantId)
                    : atualizar(pessoa, item, existentes));
        }

        return resultado;
    }

    private PessoaVinculo criar(Pessoa pessoa, VinculoItemDto item, UUID tenantId) {
        Pessoa outra = pessoaRepository.findByIdAndTenantId(item.pessoaId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Pessoa do vínculo não encontrada"));

        // Orientação canônica: origem é a de menor id. É o que permite uma
        // unique simples pegar o par duplicado, venha de que lado vier.
        boolean pessoaEhOrigem = pessoa.getId().compareTo(outra.getId()) < 0;
        Pessoa origem = pessoaEhOrigem ? pessoa : outra;
        Pessoa destino = pessoaEhOrigem ? outra : pessoa;
        TipoVinculo tipo = pessoaEhOrigem ? item.tipo() : item.tipo().inverso();

        if (vinculoRepository.existsByTenantIdAndPessoaOrigemIdAndPessoaDestinoId(
                tenantId, origem.getId(), destino.getId()))
            throw new BadRequestException("Já existe vínculo com essa pessoa");

        PessoaVinculo vinculo = new PessoaVinculo();
        vinculo.setTenant(pessoa.getTenant());
        vinculo.setPessoaOrigem(origem);
        vinculo.setPessoaDestino(destino);
        vinculo.setTipo(tipo);
        vinculo.setObservacao(item.observacao());

        return vinculoRepository.save(vinculo);
    }

    /**
     * O par não muda na edição — trocar a pessoa é remover e criar. Aqui só o
     * tipo e a observação mudam, e o tipo entra na orientação gravada.
     */
    private PessoaVinculo atualizar(Pessoa pessoa, VinculoItemDto item,
                                    Map<UUID, PessoaVinculo> existentes) {
        PessoaVinculo vinculo = existentes.get(item.id());
        if (vinculo == null)
            throw new NotFoundException("Vínculo não encontrado");

        boolean pessoaEhOrigem = vinculo.getPessoaOrigem().getId().equals(pessoa.getId());
        vinculo.setTipo(pessoaEhOrigem ? item.tipo() : item.tipo().inverso());
        vinculo.setObservacao(item.observacao());

        return vinculoRepository.save(vinculo);
    }
}

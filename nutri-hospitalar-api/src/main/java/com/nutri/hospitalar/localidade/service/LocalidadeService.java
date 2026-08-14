package com.nutri.hospitalar.localidade.service;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.localidade.dtos.CidadeSelectDto;
import com.nutri.hospitalar.localidade.entity.Cidade;
import com.nutri.hospitalar.localidade.repository.CidadeRepository;
import com.nutri.hospitalar.localidade.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Leitura de estados e municípios do IBGE. Somente leitura: os dados vêm do
 * seed da migration {@code 011-estado-cidade-ibge.xml} e não há tela de
 * manutenção — município não é cadastro do cliente.
 */
@Service
@RequiredArgsConstructor
public class LocalidadeService {

    private final EstadoRepository estadoRepository;
    private final CidadeRepository cidadeRepository;

    @Transactional(readOnly = true)
    public List<CatalogoSelectDto> estados() {
        return estadoRepository.findAllByAtivoTrueOrderByNome().stream()
                .map(e -> new CatalogoSelectDto(e.getId(), e.getSigla() + " — " + e.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CidadeSelectDto> cidades(String nome, UUID estadoId) {
        return cidadeRepository.findForSelect(nomeOuNulo(nome), estadoId).stream()
                .map(c -> new CidadeSelectDto(c.getId(), c.getNome(), c.getEstado().getSigla()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Cidade exigirCidade(UUID id) {
        return cidadeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cidade não encontrada"));
    }

    private String nomeOuNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

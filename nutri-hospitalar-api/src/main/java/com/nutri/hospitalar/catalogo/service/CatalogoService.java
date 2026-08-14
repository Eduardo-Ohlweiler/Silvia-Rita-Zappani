package com.nutri.hospitalar.catalogo.service;

import com.nutri.hospitalar.catalogo.dtos.CatalogoSelectDto;
import com.nutri.hospitalar.catalogo.entity.CatalogoEntity;
import com.nutri.hospitalar.catalogo.entity.TipoCadastro;
import com.nutri.hospitalar.catalogo.entity.TipoEmail;
import com.nutri.hospitalar.catalogo.entity.TipoRedeSocial;
import com.nutri.hospitalar.catalogo.entity.TipoTelefone;
import com.nutri.hospitalar.catalogo.repository.TipoCadastroRepository;
import com.nutri.hospitalar.catalogo.repository.TipoEmailRepository;
import com.nutri.hospitalar.catalogo.repository.TipoRedeSocialRepository;
import com.nutri.hospitalar.catalogo.repository.TipoTelefoneRepository;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Leitura dos catálogos do cadastro de pessoas.
 *
 * <p>Só leitura: os valores vêm do seed da migration
 * {@code 009-catalogos-pessoa.xml}. Não há tela de manutenção de catálogo, e
 * não haverá enquanto ninguém pedir — cinco tipos de cadastro não justificam
 * um CRUD.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final TipoCadastroRepository tipoCadastroRepository;
    private final TipoTelefoneRepository tipoTelefoneRepository;
    private final TipoEmailRepository tipoEmailRepository;
    private final TipoRedeSocialRepository tipoRedeSocialRepository;

    @Transactional(readOnly = true)
    public List<CatalogoSelectDto> tiposCadastro() {
        return paraSelect(tipoCadastroRepository.findAllByAtivoTrueOrderByNome());
    }

    @Transactional(readOnly = true)
    public List<CatalogoSelectDto> tiposTelefone() {
        return paraSelect(tipoTelefoneRepository.findAllByAtivoTrueOrderByNome());
    }

    @Transactional(readOnly = true)
    public List<CatalogoSelectDto> tiposEmail() {
        return paraSelect(tipoEmailRepository.findAllByAtivoTrueOrderByNome());
    }

    @Transactional(readOnly = true)
    public List<CatalogoSelectDto> tiposRedeSocial() {
        return paraSelect(tipoRedeSocialRepository.findAllByAtivoTrueOrderByNome());
    }

    /**
     * Resolve os tipos de cadastro de uma pessoa.
     *
     * <p>Recusa a lista vazia e o id inexistente: sem isso, um id errado vindo
     * da tela sumiria em silêncio e a pessoa seria salva sem classificação
     * nenhuma.
     */
    @Transactional(readOnly = true)
    public Set<TipoCadastro> exigirTiposCadastro(Set<UUID> ids) {
        if (ids == null || ids.isEmpty())
            throw new BadRequestException("Selecione ao menos um tipo de cadastro");

        List<TipoCadastro> encontrados = tipoCadastroRepository.findAllById(ids);

        if (encontrados.size() != ids.size())
            throw new BadRequestException("Tipo de cadastro inválido");

        return new LinkedHashSet<>(encontrados);
    }

    @Transactional(readOnly = true)
    public TipoTelefone exigirTipoTelefone(UUID id) {
        return tipoTelefoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de telefone não encontrado"));
    }

    @Transactional(readOnly = true)
    public TipoEmail exigirTipoEmail(UUID id) {
        return tipoEmailRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de e-mail não encontrado"));
    }

    @Transactional(readOnly = true)
    public TipoRedeSocial exigirTipoRedeSocial(UUID id) {
        return tipoRedeSocialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de rede social não encontrado"));
    }

    private List<CatalogoSelectDto> paraSelect(List<? extends CatalogoEntity> itens) {
        return itens.stream()
                .map(i -> new CatalogoSelectDto(i.getId(), i.getNome()))
                .toList();
    }
}

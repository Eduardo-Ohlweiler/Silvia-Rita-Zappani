package com.nutri.hospitalar.contato.service;

import com.nutri.hospitalar.catalogo.service.CatalogoService;
import com.nutri.hospitalar.contato.dtos.EmailItemDto;
import com.nutri.hospitalar.contato.dtos.RedeSocialItemDto;
import com.nutri.hospitalar.contato.dtos.TelefoneItemDto;
import com.nutri.hospitalar.contato.entity.ContatoEntity;
import com.nutri.hospitalar.contato.entity.Email;
import com.nutri.hospitalar.contato.entity.RedeSocial;
import com.nutri.hospitalar.contato.entity.Telefone;
import com.nutri.hospitalar.contato.repository.EmailRepository;
import com.nutri.hospitalar.contato.repository.RedeSocialRepository;
import com.nutri.hospitalar.contato.repository.TelefoneRepository;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Sincroniza as listas de contato de uma pessoa.
 *
 * <p>A lista recebida é o <b>estado completo</b>, não um acréscimo: item com
 * {@code id} atualiza, item sem {@code id} nasce, e o que não veio é removido.
 * É o contrato do eroERP, e é o que faz o formulário inteiro caber num PUT.
 *
 * <p>Cada método devolve a lista resultante para o chamador plantar na pessoa
 * em memória. Sem isso a resposta sairia com a coleção antiga: o objeto já está
 * no contexto de persistência, e um {@code findById} devolveria ele mesmo, com
 * os filhos de antes.
 */
@Service
@RequiredArgsConstructor
public class ContatoService {

    private final TelefoneRepository telefoneRepository;
    private final EmailRepository emailRepository;
    private final RedeSocialRepository redeSocialRepository;
    private final CatalogoService catalogoService;

    @Transactional
    public List<Telefone> sincronizarTelefones(Pessoa pessoa, List<TelefoneItemDto> itens) {
        Map<UUID, Telefone> existentes = indexar(
                telefoneRepository.findAllByPessoaIdAndTenantId(pessoa.getId(), tenantDe(pessoa)));

        if (vazia(itens)) {
            telefoneRepository.deleteAll(existentes.values());
            return List.of();
        }

        boolean algumPrincipal = exigirNoMaximoUmPrincipal(itens, TelefoneItemDto::principal, "telefone");
        removerAusentes(telefoneRepository, existentes, idsRecebidos(itens, TelefoneItemDto::id));

        List<Telefone> resultado = new ArrayList<>();

        for (int i = 0; i < itens.size(); i++) {
            TelefoneItemDto item = itens.get(i);
            Telefone telefone = resolver(existentes, item.id(), Telefone::new, pessoa, "Telefone");

            telefone.setTipoTelefone(catalogoService.exigirTipoTelefone(item.tipoTelefoneId()));
            telefone.setNumero(item.numero());
            telefone.setCodigoPais(codigoPaisOuPadrao(item.codigoPais()));
            telefone.setObservacao(item.observacao());
            telefone.setPrincipal(ehOPrincipal(item.principal(), algumPrincipal, i));

            resultado.add(telefoneRepository.save(telefone));
        }

        return resultado;
    }

    @Transactional
    public List<Email> sincronizarEmails(Pessoa pessoa, List<EmailItemDto> itens) {
        Map<UUID, Email> existentes = indexar(
                emailRepository.findAllByPessoaIdAndTenantId(pessoa.getId(), tenantDe(pessoa)));

        if (vazia(itens)) {
            emailRepository.deleteAll(existentes.values());
            return List.of();
        }

        boolean algumPrincipal = exigirNoMaximoUmPrincipal(itens, EmailItemDto::principal, "e-mail");
        removerAusentes(emailRepository, existentes, idsRecebidos(itens, EmailItemDto::id));

        List<Email> resultado = new ArrayList<>();

        for (int i = 0; i < itens.size(); i++) {
            EmailItemDto item = itens.get(i);
            Email email = resolver(existentes, item.id(), Email::new, pessoa, "E-mail");

            email.setTipoEmail(catalogoService.exigirTipoEmail(item.tipoEmailId()));
            email.setEmail(item.email().trim().toLowerCase());
            email.setObservacao(item.observacao());
            email.setPrincipal(ehOPrincipal(item.principal(), algumPrincipal, i));

            resultado.add(emailRepository.save(email));
        }

        return resultado;
    }

    /** Rede social não tem principal: o que identifica cada linha é o tipo. */
    @Transactional
    public List<RedeSocial> sincronizarRedesSociais(Pessoa pessoa, List<RedeSocialItemDto> itens) {
        Map<UUID, RedeSocial> existentes = indexar(
                redeSocialRepository.findAllByPessoaIdAndTenantId(pessoa.getId(), tenantDe(pessoa)));

        if (vazia(itens)) {
            redeSocialRepository.deleteAll(existentes.values());
            return List.of();
        }

        removerAusentes(redeSocialRepository, existentes, idsRecebidos(itens, RedeSocialItemDto::id));

        List<RedeSocial> resultado = new ArrayList<>();

        for (RedeSocialItemDto item : itens) {
            RedeSocial rede = resolver(existentes, item.id(), RedeSocial::new, pessoa, "Rede social");

            rede.setTipoRedeSocial(catalogoService.exigirTipoRedeSocial(item.tipoRedeSocialId()));
            rede.setUsuario(item.usuario());
            rede.setUrl(item.url());
            rede.setObservacao(item.observacao());

            resultado.add(redeSocialRepository.save(rede));
        }

        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────

    private boolean vazia(List<?> itens) {
        return itens == null || itens.isEmpty();
    }

    private <E extends ContatoEntity> Map<UUID, E> indexar(List<E> existentes) {
        Map<UUID, E> mapa = new LinkedHashMap<>();
        existentes.forEach(e -> mapa.put(e.getId(), e));
        return mapa;
    }

    /** @return true se algum item veio marcado como principal. */
    private <D> boolean exigirNoMaximoUmPrincipal(List<D> itens, Function<D, Boolean> principalDe,
                                                  String rotulo) {
        long marcados = itens.stream()
                .filter(i -> Boolean.TRUE.equals(principalDe.apply(i)))
                .count();

        if (marcados > 1)
            throw new BadRequestException("Apenas um " + rotulo + " pode ser o principal");

        return marcados == 1;
    }

    private <D> Set<UUID> idsRecebidos(List<D> itens, Function<D, UUID> idDe) {
        return itens.stream().map(idDe).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private <E extends ContatoEntity> void removerAusentes(JpaRepository<E, UUID> repository,
                                                           Map<UUID, E> existentes,
                                                           Set<UUID> recebidos) {
        existentes.values().stream()
                .filter(e -> !recebidos.contains(e.getId()))
                .forEach(repository::delete);
    }

    /**
     * Item sem id nasce; com id, tem de estar entre os desta pessoa neste
     * tenant. Procurar no mapa em vez de no banco não é só economia de query:
     * é o que impede alterar o contato de outra pessoa mandando o id dela.
     */
    private <E extends ContatoEntity> E resolver(Map<UUID, E> existentes, UUID id,
                                                 Supplier<E> novo, Pessoa pessoa, String rotulo) {
        if (id == null) {
            E entidade = novo.get();
            entidade.setPessoa(pessoa);
            entidade.setTenant(pessoa.getTenant());
            return entidade;
        }

        E entidade = existentes.get(id);
        if (entidade == null)
            throw new NotFoundException(rotulo + " não encontrado");

        return entidade;
    }

    /**
     * Ninguém marcou: o primeiro da lista vira o principal. Pessoa sem telefone
     * principal é pessoa que ninguém sabe como chamar.
     */
    private boolean ehOPrincipal(Boolean marcado, boolean algumPrincipal, int indice) {
        return Boolean.TRUE.equals(marcado) || (!algumPrincipal && indice == 0);
    }

    private String codigoPaisOuPadrao(String codigoPais) {
        return (codigoPais == null || codigoPais.isBlank()) ? "55" : codigoPais;
    }

    private UUID tenantDe(Pessoa pessoa) {
        return pessoa.getTenant().getId();
    }
}

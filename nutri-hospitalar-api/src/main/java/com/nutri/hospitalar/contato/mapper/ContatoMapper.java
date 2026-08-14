package com.nutri.hospitalar.contato.mapper;

import com.nutri.hospitalar.contato.dtos.EmailResponseDto;
import com.nutri.hospitalar.contato.dtos.RedeSocialResponseDto;
import com.nutri.hospitalar.contato.dtos.TelefoneResponseDto;
import com.nutri.hospitalar.contato.entity.Email;
import com.nutri.hospitalar.contato.entity.RedeSocial;
import com.nutri.hospitalar.contato.entity.Telefone;

import java.util.Comparator;
import java.util.List;

public final class ContatoMapper {

    private ContatoMapper() {}

    /** Principal primeiro: é o número que a equipe procura na tela. */
    public static List<TelefoneResponseDto> toTelefoneList(List<Telefone> telefones) {
        if (telefones == null) return List.of();
        return telefones.stream()
                .sorted(Comparator.comparing(Telefone::getPrincipal).reversed())
                .map(t -> new TelefoneResponseDto(
                        t.getId(),
                        t.getTipoTelefone().getId(),
                        t.getTipoTelefone().getNome(),
                        t.getCodigoPais(),
                        t.getNumero(),
                        t.getObservacao(),
                        t.getPrincipal()))
                .toList();
    }

    public static List<EmailResponseDto> toEmailList(List<Email> emails) {
        if (emails == null) return List.of();
        return emails.stream()
                .sorted(Comparator.comparing(Email::getPrincipal).reversed())
                .map(e -> new EmailResponseDto(
                        e.getId(),
                        e.getTipoEmail().getId(),
                        e.getTipoEmail().getNome(),
                        e.getEmail(),
                        e.getObservacao(),
                        e.getPrincipal()))
                .toList();
    }

    public static List<RedeSocialResponseDto> toRedeSocialList(List<RedeSocial> redes) {
        if (redes == null) return List.of();
        return redes.stream()
                .map(r -> new RedeSocialResponseDto(
                        r.getId(),
                        r.getTipoRedeSocial().getId(),
                        r.getTipoRedeSocial().getNome(),
                        r.getUsuario(),
                        r.getUrl(),
                        r.getObservacao()))
                .toList();
    }
}

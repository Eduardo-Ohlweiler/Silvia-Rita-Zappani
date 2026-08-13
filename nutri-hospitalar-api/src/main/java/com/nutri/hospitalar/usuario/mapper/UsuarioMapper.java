package com.nutri.hospitalar.usuario.mapper;

import com.nutri.hospitalar.usuario.dtos.UsuarioResponseDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioSelectDto;
import com.nutri.hospitalar.usuario.entity.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {}

    public static UsuarioResponseDto toResponse(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getTenant().getId(),
                usuario.getTenant().getNome(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getCodigoPais(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.estaBloqueado(),
                usuario.getCreatedAt());
    }

    public static UsuarioSelectDto toSelect(Usuario usuario) {
        return new UsuarioSelectDto(usuario.getId(), usuario.getNome());
    }
}

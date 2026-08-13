package com.nutri.hospitalar.config;

import com.nutri.hospitalar.exceptions.UnauthorizedException;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;

    public UUID getTenantIdLogado() {
        return detalhes().tenantId();
    }

    public UUID getUsuarioIdLogado() {
        Authentication auth = autenticacao();
        if (!(auth.getPrincipal() instanceof UUID id))
            throw new UnauthorizedException("Sessão inválida");
        return id;
    }

    public UUID getSessionIdLogado() {
        return detalhes().sessionId();
    }

    public Role getRoleLogada() {
        return detalhes().role();
    }

    public boolean isSuperadmin() {
        return Role.SUPERADMIN.equals(getRoleLogada());
    }

    public boolean isImpersonating() {
        return detalhes().impersonating();
    }

    public Usuario getUsuarioLogado() {
        return usuarioRepository.findById(getUsuarioIdLogado())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
    }

    public Tenant getTenantReference() {
        return tenantRepository.getReferenceById(getTenantIdLogado());
    }

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new UnauthorizedException("Não autenticado");
        return auth;
    }

    private AuthDetails detalhes() {
        if (!(autenticacao().getDetails() instanceof AuthDetails detalhes))
            throw new UnauthorizedException("Sessão inválida");
        return detalhes;
    }
}

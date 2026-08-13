package com.nutri.hospitalar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutri.hospitalar.exceptions.ErroResponseDto;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.tenant.repository.TenantRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extrairToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.validar(token);

            UUID usuarioId = jwtUtil.usuarioId(claims);
            UUID tenantId  = jwtUtil.tenantId(claims);

            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new JwtException("Usuário não encontrado"));

            if (!Boolean.TRUE.equals(usuario.getAtivo()))
                throw new JwtException("Usuário inativo");

            Tenant tenantEfetivo = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new JwtException("Tenant não encontrado"));

            if (!Boolean.TRUE.equals(tenantEfetivo.getAtivo()))
                throw new JwtException("Tenant inativo");

            boolean tenantDeOutrem = !tenantId.equals(usuario.getTenant().getId());
            if (tenantDeOutrem && !usuario.isSuperadmin())
                throw new JwtException("Token fora do tenant do usuário");

            var authentication = new UsernamePasswordAuthenticationToken(
                    usuarioId,
                    null,
                    List.of(new SimpleGrantedAuthority(usuario.getRole().authority())));

            authentication.setDetails(new AuthDetails(
                    tenantId,
                    jwtUtil.sessionId(claims),
                    usuario.getRole(),
                    jwtUtil.impersonating(claims)));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            responderNaoAutorizado(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private String extrairToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(h -> h.startsWith(PREFIXO))
                .map(h -> h.substring(PREFIXO.length()).trim())
                .filter(t -> !t.isEmpty())
                .orElse(null);
    }

    private void responderNaoAutorizado(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErroResponseDto.de(
                "Sessão expirada ou inválida",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI()));
    }
}

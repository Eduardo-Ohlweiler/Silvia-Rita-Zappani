package com.nutri.hospitalar.config;

import com.nutri.hospitalar.usuario.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_TENANT_ID     = "tenantId";
    public static final String CLAIM_SESSION_ID    = "sessionId";
    public static final String CLAIM_ROLE          = "role";
    public static final String CLAIM_IMPERSONATING = "impersonating";

    private final JwtProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarAccessToken(UUID usuarioId, UUID tenantId, Role role,
                                   UUID sessionId, boolean impersonating) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_SESSION_ID, sessionId != null ? sessionId.toString() : null)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_IMPERSONATING, impersonating)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(properties.getAccessTtl())))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID usuarioId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID tenantId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
    }

    public UUID sessionId(Claims claims) {
        String valor = claims.get(CLAIM_SESSION_ID, String.class);
        return valor == null ? null : UUID.fromString(valor);
    }

    public Role role(Claims claims) {
        return Role.valueOf(claims.get(CLAIM_ROLE, String.class));
    }

    public boolean impersonating(Claims claims) {
        return Boolean.TRUE.equals(claims.get(CLAIM_IMPERSONATING, Boolean.class));
    }
}

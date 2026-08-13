package com.nutri.hospitalar.refreshtoken.service;

import com.nutri.hospitalar.config.JwtProperties;
import com.nutri.hospitalar.exceptions.UnauthorizedException;
import com.nutri.hospitalar.refreshtoken.entity.RefreshToken;
import com.nutri.hospitalar.refreshtoken.repository.RefreshTokenRepository;
import com.nutri.hospitalar.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final int BYTES_DO_TOKEN = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TransactionTemplate transacaoIndependente;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String emitir(Usuario usuario, UUID sessionId) {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        secureRandom.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setSessionId(sessionId);
        token.setTokenHash(hash(valor));
        token.setExpiraEm(Instant.now().plus(jwtProperties.getRefreshTtl()));
        refreshTokenRepository.save(token);

        return valor;
    }

    @Transactional
    public RefreshToken consumir(String valor) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(valor))
                .orElseThrow(() -> new UnauthorizedException("Sessão expirada. Faça login novamente."));

        if (token.getRevogadoEm() != null) {

            UUID usuarioId = token.getUsuario().getId();
            transacaoIndependente.executeWithoutResult(status ->
                    refreshTokenRepository.revogarTodosDoUsuario(usuarioId, Instant.now()));

            log.warn("Refresh token reutilizado — cadeia revogada para o usuário {}", usuarioId);
            throw new UnauthorizedException("Sessão expirada. Faça login novamente.");
        }

        if (token.getExpiraEm().isBefore(Instant.now()))
            throw new UnauthorizedException("Sessão expirada. Faça login novamente.");

        token.setRevogadoEm(Instant.now());
        refreshTokenRepository.save(token);

        return token;
    }

    @Transactional
    public void revogarTodosDoUsuario(UUID usuarioId) {
        refreshTokenRepository.revogarTodosDoUsuario(usuarioId, Instant.now());
    }

    @Transactional
    public int expurgarExpirados(Instant limite) {
        return refreshTokenRepository.expurgarExpirados(limite);
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}

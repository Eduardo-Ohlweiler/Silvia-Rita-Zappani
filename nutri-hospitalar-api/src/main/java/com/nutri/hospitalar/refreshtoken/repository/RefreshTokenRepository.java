package com.nutri.hospitalar.refreshtoken.repository;

import com.nutri.hospitalar.refreshtoken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query(value = """
            UPDATE refresh_token
            SET revogado_em = CAST(:agora AS timestamptz),
                updated_at  = CAST(:agora AS timestamptz)
            WHERE usuario_id = CAST(:usuarioId AS uuid)
              AND revogado_em IS NULL
            """, nativeQuery = true)
    int revogarTodosDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query(value = "DELETE FROM refresh_token WHERE expira_em < CAST(:limite AS timestamptz)",
            nativeQuery = true)
    int expurgarExpirados(@Param("limite") Instant limite);
}

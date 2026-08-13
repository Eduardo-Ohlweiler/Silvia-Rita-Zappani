package com.nutri.hospitalar.loginlog.repository;

import com.nutri.hospitalar.loginlog.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, UUID> {

    @Query(value = """
            SELECT l.* FROM login_log l
            WHERE l.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:usuarioId AS uuid) IS NULL OR l.usuario_id = CAST(:usuarioId AS uuid))
              AND (CAST(:sucesso AS boolean) IS NULL OR l.sucesso = CAST(:sucesso AS boolean))
              AND (CAST(:de  AS timestamptz) IS NULL OR l.data_login >= CAST(:de  AS timestamptz))
              AND (CAST(:ate AS timestamptz) IS NULL OR l.data_login <= CAST(:ate AS timestamptz))
              AND (CAST(:ip  AS text) IS NULL OR l.endereco_ip LIKE '%' || CAST(:ip AS text) || '%')
            ORDER BY l.data_login DESC
            """,
            countQuery = """
            SELECT count(*) FROM login_log l
            WHERE l.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:usuarioId AS uuid) IS NULL OR l.usuario_id = CAST(:usuarioId AS uuid))
              AND (CAST(:sucesso AS boolean) IS NULL OR l.sucesso = CAST(:sucesso AS boolean))
              AND (CAST(:de  AS timestamptz) IS NULL OR l.data_login >= CAST(:de  AS timestamptz))
              AND (CAST(:ate AS timestamptz) IS NULL OR l.data_login <= CAST(:ate AS timestamptz))
              AND (CAST(:ip  AS text) IS NULL OR l.endereco_ip LIKE '%' || CAST(:ip AS text) || '%')
            """,
            nativeQuery = true)
    Page<LoginLog> findAllWithFilters(Pageable pageable,
                                      @Param("tenantId") UUID tenantId,
                                      @Param("usuarioId") UUID usuarioId,
                                      @Param("sucesso") Boolean sucesso,
                                      @Param("de") Instant de,
                                      @Param("ate") Instant ate,
                                      @Param("ip") String ip);

    @Query(value = """
            SELECT l.* FROM login_log l
            WHERE (CAST(:tenantId AS uuid) IS NULL OR l.tenant_id = CAST(:tenantId AS uuid))
              AND (CAST(:usuarioId AS uuid) IS NULL OR l.usuario_id = CAST(:usuarioId AS uuid))
              AND (CAST(:sucesso AS boolean) IS NULL OR l.sucesso = CAST(:sucesso AS boolean))
              AND (CAST(:de  AS timestamptz) IS NULL OR l.data_login >= CAST(:de  AS timestamptz))
              AND (CAST(:ate AS timestamptz) IS NULL OR l.data_login <= CAST(:ate AS timestamptz))
              AND (CAST(:ip  AS text) IS NULL OR l.endereco_ip LIKE '%' || CAST(:ip AS text) || '%')
            ORDER BY l.data_login DESC
            """,
            countQuery = """
            SELECT count(*) FROM login_log l
            WHERE (CAST(:tenantId AS uuid) IS NULL OR l.tenant_id = CAST(:tenantId AS uuid))
              AND (CAST(:usuarioId AS uuid) IS NULL OR l.usuario_id = CAST(:usuarioId AS uuid))
              AND (CAST(:sucesso AS boolean) IS NULL OR l.sucesso = CAST(:sucesso AS boolean))
              AND (CAST(:de  AS timestamptz) IS NULL OR l.data_login >= CAST(:de  AS timestamptz))
              AND (CAST(:ate AS timestamptz) IS NULL OR l.data_login <= CAST(:ate AS timestamptz))
              AND (CAST(:ip  AS text) IS NULL OR l.endereco_ip LIKE '%' || CAST(:ip AS text) || '%')
            """,
            nativeQuery = true)
    Page<LoginLog> findAllGlobal(Pageable pageable,
                                 @Param("tenantId") UUID tenantId,
                                 @Param("usuarioId") UUID usuarioId,
                                 @Param("sucesso") Boolean sucesso,
                                 @Param("de") Instant de,
                                 @Param("ate") Instant ate,
                                 @Param("ip") String ip);

    @Modifying
    @Query(value = """
            UPDATE login_log
            SET data_logout = CAST(:agora AS timestamptz),
                tipo_logout = 'EXPIRACAO',
                updated_at  = CAST(:agora AS timestamptz)
            WHERE sucesso = true
              AND data_logout IS NULL
              AND data_login < CAST(:limite AS timestamptz)
            """, nativeQuery = true)
    int fecharSessoesExpiradas(@Param("limite") Instant limite, @Param("agora") Instant agora);

    @Modifying
    @Query(value = "DELETE FROM login_log WHERE data_login < CAST(:limite AS timestamptz)",
            nativeQuery = true)
    int expurgarAnterioresA(@Param("limite") Instant limite);
}

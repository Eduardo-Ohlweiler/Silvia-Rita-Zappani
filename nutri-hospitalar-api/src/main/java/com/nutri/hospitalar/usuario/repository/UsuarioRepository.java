package com.nutri.hospitalar.usuario.repository;

import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.usuario.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByRole(Role role);

    // ─── Escopo do tenant ───────────────────────────────────────────────
    Optional<Usuario> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query(value = """
            SELECT u.* FROM usuario u
            WHERE u.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome  AS text) IS NULL
                   OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:email AS text) IS NULL
                   OR lower(u.email) LIKE lower('%' || CAST(:email AS text) || '%'))
              AND (CAST(:role  AS text) IS NULL OR u.role = CAST(:role AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR u.ativo = CAST(:ativo AS boolean))
            ORDER BY u.nome
            """,
            countQuery = """
            SELECT count(*) FROM usuario u
            WHERE u.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome  AS text) IS NULL
                   OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:email AS text) IS NULL
                   OR lower(u.email) LIKE lower('%' || CAST(:email AS text) || '%'))
              AND (CAST(:role  AS text) IS NULL OR u.role = CAST(:role AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR u.ativo = CAST(:ativo AS boolean))
            """,
            nativeQuery = true)
    Page<Usuario> findAllWithFilters(Pageable pageable,
                                     @Param("tenantId") UUID tenantId,
                                     @Param("nome") String nome,
                                     @Param("email") String email,
                                     @Param("role") String role,
                                     @Param("ativo") Boolean ativo);

    @Query(value = """
            SELECT u.* FROM usuario u
            WHERE u.tenant_id = CAST(:tenantId AS uuid)
              AND u.ativo = true
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
            ORDER BY u.nome
            LIMIT 100
            """, nativeQuery = true)
    List<Usuario> findForSelect(@Param("tenantId") UUID tenantId, @Param("nome") String nome);

    /**
     * Listagem através dos tenants — exclusiva de SUPERADMIN.
     *
     * <p>Sem ela, o superadmin cadastra um cliente novo e o usuário "desaparece":
     * a listagem por tenant efetivo mostra o tenant raiz, e o usuário recém
     * criado vive no tenant que acabou de nascer.
     *
     * <p>Mesmo papel do {@code LoginLogRepository#findAllGlobal} — um método
     * separado, com nome explícito, para que ler através dos tenants seja
     * sempre uma decisão visível no código.
     */
    @Query(value = """
            SELECT u.* FROM usuario u
            JOIN tenant t ON t.id = u.tenant_id
            WHERE (CAST(:tenantId AS uuid) IS NULL OR u.tenant_id = CAST(:tenantId AS uuid))
              AND (CAST(:nome  AS text) IS NULL
                   OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:email AS text) IS NULL
                   OR lower(u.email) LIKE lower('%' || CAST(:email AS text) || '%'))
              AND (CAST(:role  AS text) IS NULL OR u.role = CAST(:role AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR u.ativo = CAST(:ativo AS boolean))
            ORDER BY t.nome, u.nome
            """,
            countQuery = """
            SELECT count(*) FROM usuario u
            WHERE (CAST(:tenantId AS uuid) IS NULL OR u.tenant_id = CAST(:tenantId AS uuid))
              AND (CAST(:nome  AS text) IS NULL
                   OR unaccent(lower(u.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:email AS text) IS NULL
                   OR lower(u.email) LIKE lower('%' || CAST(:email AS text) || '%'))
              AND (CAST(:role  AS text) IS NULL OR u.role = CAST(:role AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR u.ativo = CAST(:ativo AS boolean))
            """,
            nativeQuery = true)
    Page<Usuario> findAllGlobal(Pageable pageable,
                                @Param("tenantId") UUID tenantId,
                                @Param("nome") String nome,
                                @Param("email") String email,
                                @Param("role") String role,
                                @Param("ativo") Boolean ativo);
}

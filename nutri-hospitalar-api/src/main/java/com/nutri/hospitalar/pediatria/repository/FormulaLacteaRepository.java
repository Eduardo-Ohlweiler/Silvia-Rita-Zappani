package com.nutri.hospitalar.pediatria.repository;

import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Catálogo meio global, meio do tenant.
 *
 * <p><b>Ler alcança as globais; escrever, não.</b> As consultas de leitura
 * usam {@code (tenant_id = :tenantId OR tenant_id IS NULL)}; a de escrita é
 * {@link #findByIdAndTenantId}, sem o {@code OR}, então uma fórmula do sistema
 * nem sequer é encontrada para alteração.
 */
@Repository
public interface FormulaLacteaRepository extends JpaRepository<FormulaLactea, UUID> {

    /** Leitura: enxerga a do tenant e a global. */
    @Query("""
            SELECT f FROM FormulaLactea f
            WHERE f.id = :id
              AND (f.tenant.id = :tenantId OR f.tenant IS NULL)
            """)
    Optional<FormulaLactea> findByIdVisivelPara(@Param("id") UUID id,
                                                @Param("tenantId") UUID tenantId);

    /** Escrita: só a do próprio tenant. A global não é encontrada — por isso. */
    Optional<FormulaLactea> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNomeIgnoreCase(UUID tenantId, String nome);

    boolean existsByTenantIdAndNomeIgnoreCaseAndIdNot(UUID tenantId, String nome, UUID id);

    @Query(value = """
            SELECT f.* FROM formula_lactea f
            WHERE (f.tenant_id = CAST(:tenantId AS uuid) OR f.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(f.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR f.ativo = CAST(:ativo AS boolean))
              AND (CAST(:global AS boolean) IS NULL
                   OR (CAST(:global AS boolean) = true  AND f.tenant_id IS NULL)
                   OR (CAST(:global AS boolean) = false AND f.tenant_id IS NOT NULL))
            ORDER BY f.nome
            """,
            countQuery = """
            SELECT count(*) FROM formula_lactea f
            WHERE (f.tenant_id = CAST(:tenantId AS uuid) OR f.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(f.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR f.ativo = CAST(:ativo AS boolean))
              AND (CAST(:global AS boolean) IS NULL
                   OR (CAST(:global AS boolean) = true  AND f.tenant_id IS NULL)
                   OR (CAST(:global AS boolean) = false AND f.tenant_id IS NOT NULL))
            """,
            nativeQuery = true)
    Page<FormulaLactea> findAllWithFilters(Pageable pageable,
                                           @Param("tenantId") UUID tenantId,
                                           @Param("nome") String nome,
                                           @Param("ativo") Boolean ativo,
                                           @Param("global") Boolean global);

    @Query(value = """
            SELECT f.* FROM formula_lactea f
            WHERE (f.tenant_id = CAST(:tenantId AS uuid) OR f.tenant_id IS NULL)
              AND f.ativo = true
              AND (CAST(:termo AS text) IS NULL
                   OR unaccent(lower(f.nome)) LIKE unaccent(lower('%' || CAST(:termo AS text) || '%')))
            ORDER BY f.nome
            LIMIT 100
            """, nativeQuery = true)
    List<FormulaLactea> findForSelect(@Param("tenantId") UUID tenantId,
                                      @Param("termo") String termo);
}

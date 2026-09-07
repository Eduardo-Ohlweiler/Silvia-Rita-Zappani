package com.nutri.hospitalar.clinica.repository;

import com.nutri.hospitalar.clinica.entity.ModeloFicha;
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
 * Catálogo meio global, meio do tenant — mesmo desenho de
 * {@code FormulaEnteralRepository}.
 *
 * <p><b>Ler alcança os do sistema; escrever, não.</b> A leitura usa
 * {@code (tenant_id = :tenantId OR tenant_id IS NULL)}; a escrita é
 * {@link #findByIdAndTenantId}, sem o {@code OR}, então um modelo do sistema nem
 * é <i>encontrado</i> para alteração — o service devolve 404, e não 403, porque
 * 403 confirmaria a existência do registro a quem não pode tocá-lo.
 */
@Repository
public interface ModeloFichaRepository extends JpaRepository<ModeloFicha, UUID> {

    /** Leitura: enxerga o do tenant e o do sistema. */
    @Query("""
            SELECT m FROM ModeloFicha m
            WHERE m.id = :id
              AND (m.tenant.id = :tenantId OR m.tenant IS NULL)
            """)
    Optional<ModeloFicha> findByIdVisivelPara(@Param("id") UUID id,
                                              @Param("tenantId") UUID tenantId);

    /** Escrita: só o do próprio tenant. O do sistema não é encontrado — por isso. */
    Optional<ModeloFicha> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNomeIgnoreCase(UUID tenantId, String nome);

    boolean existsByTenantIdAndNomeIgnoreCaseAndIdNot(UUID tenantId, String nome, UUID id);

    @Query(value = """
            SELECT m.* FROM modelo_ficha m
            WHERE (m.tenant_id = CAST(:tenantId AS uuid) OR m.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(m.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR m.ativo = CAST(:ativo AS boolean))
              AND (CAST(:doSistema AS boolean) IS NULL
                   OR (CAST(:doSistema AS boolean) = true  AND m.tenant_id IS NULL)
                   OR (CAST(:doSistema AS boolean) = false AND m.tenant_id IS NOT NULL))
            ORDER BY m.nome
            """,
            countQuery = """
            SELECT count(*) FROM modelo_ficha m
            WHERE (m.tenant_id = CAST(:tenantId AS uuid) OR m.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(m.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR m.ativo = CAST(:ativo AS boolean))
              AND (CAST(:doSistema AS boolean) IS NULL
                   OR (CAST(:doSistema AS boolean) = true  AND m.tenant_id IS NULL)
                   OR (CAST(:doSistema AS boolean) = false AND m.tenant_id IS NOT NULL))
            """,
            nativeQuery = true)
    Page<ModeloFicha> findAllWithFilters(Pageable pageable,
                                         @Param("tenantId") UUID tenantId,
                                         @Param("nome") String nome,
                                         @Param("ativo") Boolean ativo,
                                         @Param("doSistema") Boolean doSistema);

    @Query(value = """
            SELECT m.* FROM modelo_ficha m
            WHERE (m.tenant_id = CAST(:tenantId AS uuid) OR m.tenant_id IS NULL)
              AND m.ativo = true
              AND (CAST(:termo AS text) IS NULL
                   OR unaccent(lower(m.nome)) LIKE unaccent(lower('%' || CAST(:termo AS text) || '%')))
            ORDER BY m.nome
            LIMIT 100
            """, nativeQuery = true)
    List<ModeloFicha> findForSelect(@Param("tenantId") UUID tenantId,
                                    @Param("termo") String termo);
}

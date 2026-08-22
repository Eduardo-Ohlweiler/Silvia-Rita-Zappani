package com.nutri.hospitalar.uti.repository;

import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
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
 * <p>Além do CRUD, é aqui que moram as duas consultas que <b>o cálculo</b>
 * consome: {@link #findModulosProteicos} e {@link #findInsumoPorPapel}. São elas
 * que substituem as constantes cravadas no eroERP.
 */
@Repository
public interface ProdutoNutricionalRepository extends JpaRepository<ProdutoNutricional, UUID> {

    @Query("""
            SELECT p FROM ProdutoNutricional p
            WHERE p.id = :id
              AND (p.tenant.id = :tenantId OR p.tenant IS NULL)
            """)
    Optional<ProdutoNutricional> findByIdVisivelPara(@Param("id") UUID id,
                                                     @Param("tenantId") UUID tenantId);

    Optional<ProdutoNutricional> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndTipoAndNomeIgnoreCase(
            UUID tenantId, TipoProdutoNutricional tipo, String nome);

    boolean existsByTenantIdAndTipoAndNomeIgnoreCaseAndIdNot(
            UUID tenantId, TipoProdutoNutricional tipo, String nome, UUID id);

    // ─── O que o cálculo consome ────────────────────────────────────────

    /**
     * Os módulos proteicos ativos que este tenant enxerga.
     *
     * <p>É a consulta que faz a sugestão de módulo da dieta enteral iterar o
     * catálogo em vez de uma constante de três itens. Cadastrar um módulo passa
     * a ter efeito.
     */
    @Query("""
            SELECT p FROM ProdutoNutricional p
            WHERE (p.tenant.id = :tenantId OR p.tenant IS NULL)
              AND p.moduloProteico = true
              AND p.ativo = true
            ORDER BY p.nome
            """)
    List<ProdutoNutricional> findModulosProteicos(@Param("tenantId") UUID tenantId);

    /**
     * Os insumos ativos de um papel da receita artesanal.
     *
     * <p>Devolve lista, não um: o tenant pode cadastrar o seu próprio óleo ao
     * lado do global, e é a tela que escolhe. Ordena o do tenant primeiro —
     * quem cadastrou o próprio quer vê-lo antes.
     */
    @Query("""
            SELECT p FROM ProdutoNutricional p
            WHERE (p.tenant.id = :tenantId OR p.tenant IS NULL)
              AND p.papelArtesanal = :papel
              AND p.ativo = true
            ORDER BY CASE WHEN p.tenant IS NULL THEN 1 ELSE 0 END, p.nome
            """)
    List<ProdutoNutricional> findInsumoPorPapel(@Param("tenantId") UUID tenantId,
                                                @Param("papel") PapelArtesanal papel);

    // ─── CRUD ───────────────────────────────────────────────────────────

    @Query(value = """
            SELECT p.* FROM produto_nutricional p
            WHERE (p.tenant_id = CAST(:tenantId AS uuid) OR p.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:tipo AS text) IS NULL OR p.tipo = CAST(:tipo AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
              AND (CAST(:global AS boolean) IS NULL
                   OR (CAST(:global AS boolean) = true  AND p.tenant_id IS NULL)
                   OR (CAST(:global AS boolean) = false AND p.tenant_id IS NOT NULL))
            ORDER BY p.tipo, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM produto_nutricional p
            WHERE (p.tenant_id = CAST(:tenantId AS uuid) OR p.tenant_id IS NULL)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:tipo AS text) IS NULL OR p.tipo = CAST(:tipo AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
              AND (CAST(:global AS boolean) IS NULL
                   OR (CAST(:global AS boolean) = true  AND p.tenant_id IS NULL)
                   OR (CAST(:global AS boolean) = false AND p.tenant_id IS NOT NULL))
            """,
            nativeQuery = true)
    Page<ProdutoNutricional> findAllWithFilters(Pageable pageable,
                                                @Param("tenantId") UUID tenantId,
                                                @Param("nome") String nome,
                                                @Param("tipo") String tipo,
                                                @Param("ativo") Boolean ativo,
                                                @Param("global") Boolean global);

    @Query(value = """
            SELECT p.* FROM produto_nutricional p
            WHERE (p.tenant_id = CAST(:tenantId AS uuid) OR p.tenant_id IS NULL)
              AND p.ativo = true
              AND (CAST(:tipo AS text) IS NULL OR p.tipo = CAST(:tipo AS text))
              AND (CAST(:termo AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:termo AS text) || '%')))
            ORDER BY p.nome
            LIMIT 100
            """, nativeQuery = true)
    List<ProdutoNutricional> findForSelect(@Param("tenantId") UUID tenantId,
                                           @Param("tipo") String tipo,
                                           @Param("termo") String termo);
}

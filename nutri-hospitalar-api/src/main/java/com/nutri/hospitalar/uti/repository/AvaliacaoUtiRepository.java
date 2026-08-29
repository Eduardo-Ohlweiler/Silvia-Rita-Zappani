package com.nutri.hospitalar.uti.repository;

import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entidade de negócio: <b>tudo passa pelo tenant</b>. Não há consulta global
 * aqui — só usuários e log de acesso a têm, e nenhuma delas carrega dado
 * clínico.
 *
 * <p>Native query com {@code CAST} explícito, como manda a regra 2 do
 * {@code CLAUDE.md}: o JPQL do Hibernate 6 não tipa um parâmetro nulo solto e o
 * Postgres responde <i>"não foi possível determinar o tipo de dados do
 * parâmetro"</i>.
 */
@Repository
public interface AvaliacaoUtiRepository extends JpaRepository<AvaliacaoUti, UUID> {

    Optional<AvaliacaoUti> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Quantas avaliações usam esta fórmula — para a tela poder avisar. */
    long countByTenantIdAndFormulaEnteralId(UUID tenantId, UUID formulaEnteralId);

    /**
     * As avaliações que alimentam os painéis — <b>sem paginação</b>, porque a
     * série temporal precisa do período inteiro para ser desenhada. Ordenada por
     * data crescente: é o eixo X.
     *
     * <p>Todo filtro é opcional, daí o {@code CAST} explícito de cada parâmetro
     * (regra 2 do CLAUDE.md).
     */
    @Query(value = """
            SELECT a.* FROM avaliacao_uti a
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:formulaEnteralId AS uuid) IS NULL
                   OR a.formula_enteral_id = CAST(:formulaEnteralId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR a.data_avaliacao <= CAST(:ate AS date))
            ORDER BY a.data_avaliacao, a.created_at
            """, nativeQuery = true)
    List<AvaliacaoUti> findParaPainel(@Param("tenantId") UUID tenantId,
                                      @Param("pacienteId") UUID pacienteId,
                                      @Param("formulaEnteralId") UUID formulaEnteralId,
                                      @Param("de") LocalDate de,
                                      @Param("ate") LocalDate ate);

    @Query(value = """
            SELECT a.* FROM avaliacao_uti a
            JOIN pessoa p ON p.id = a.paciente_id
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:pacienteNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pacienteNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR a.data_avaliacao <= CAST(:ate AS date))
            ORDER BY a.data_avaliacao DESC, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM avaliacao_uti a
            JOIN pessoa p ON p.id = a.paciente_id
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:pacienteNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pacienteNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR a.data_avaliacao <= CAST(:ate AS date))
            """,
            nativeQuery = true)
    Page<AvaliacaoUti> findAllWithFilters(Pageable pageable,
                                          @Param("tenantId") UUID tenantId,
                                          @Param("pacienteId") UUID pacienteId,
                                          @Param("pacienteNome") String pacienteNome,
                                          @Param("de") LocalDate de,
                                          @Param("ate") LocalDate ate);
}

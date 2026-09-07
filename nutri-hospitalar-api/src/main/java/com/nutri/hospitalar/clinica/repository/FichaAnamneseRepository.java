package com.nutri.hospitalar.clinica.repository;

import com.nutri.hospitalar.clinica.entity.FichaAnamnese;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entidade de negócio: <b>tudo passa pelo tenant</b>. Não há consulta global
 * aqui — a ficha é dado clínico.
 */
@Repository
public interface FichaAnamneseRepository extends JpaRepository<FichaAnamnese, UUID> {

    Optional<FichaAnamnese> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query(value = """
            SELECT f.* FROM ficha_anamnese f
            JOIN pessoa p ON p.id = f.paciente_id
            WHERE f.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR f.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:modeloId   AS uuid) IS NULL OR f.modelo_id   = CAST(:modeloId   AS uuid))
              AND (CAST(:de  AS date) IS NULL OR f.data_preenchimento >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR f.data_preenchimento <= CAST(:ate AS date))
            ORDER BY f.data_preenchimento DESC, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM ficha_anamnese f
            JOIN pessoa p ON p.id = f.paciente_id
            WHERE f.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR f.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:modeloId   AS uuid) IS NULL OR f.modelo_id   = CAST(:modeloId   AS uuid))
              AND (CAST(:de  AS date) IS NULL OR f.data_preenchimento >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR f.data_preenchimento <= CAST(:ate AS date))
            """,
            nativeQuery = true)
    Page<FichaAnamnese> findAllWithFilters(Pageable pageable,
                                           @Param("tenantId") UUID tenantId,
                                           @Param("pacienteId") UUID pacienteId,
                                           @Param("modeloId") UUID modeloId,
                                           @Param("de") LocalDate de,
                                           @Param("ate") LocalDate ate);

    /**
     * Quantas perguntas cada ficha tem, e quantas foram respondidas.
     *
     * <p><b>Uma consulta para a página inteira</b>, e não uma por linha: a
     * coleção de respostas é preguiçosa, e lê-la dentro do laço da listagem
     * daria vinte consultas a mais por página. É a mesma troca que
     * {@code linhasPorIdade} faz no painel pediátrico.
     *
     * <p>Colunas: {@code ficha_id}, total, respondidas. Resposta em branco não
     * conta como respondida — nem nula, nem só espaço.
     */
    @Query(value = """
            SELECT r.ficha_id,
                   count(*),
                   count(*) FILTER (WHERE r.valor IS NOT NULL AND length(btrim(r.valor)) > 0)
            FROM resposta_ficha r
            WHERE r.ficha_id IN (:fichaIds)
            GROUP BY r.ficha_id
            """, nativeQuery = true)
    List<Object[]> contarRespostasPorFicha(@Param("fichaIds") Collection<UUID> fichaIds);

    /** Um modelo em uso não pode ser apagado sem que se saiba. */
    long countByModeloIdAndTenantId(UUID modeloId, UUID tenantId);
}

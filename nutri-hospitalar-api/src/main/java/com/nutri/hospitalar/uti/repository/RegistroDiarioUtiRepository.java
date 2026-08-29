package com.nutri.hospitalar.uti.repository;

import com.nutri.hospitalar.uti.entity.RegistroDiarioUti;
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
 * Entidade de negócio: tudo passa pelo tenant.
 */
@Repository
public interface RegistroDiarioUtiRepository extends JpaRepository<RegistroDiarioUti, UUID> {

    Optional<RegistroDiarioUti> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Já existe registro deste paciente neste dia?
     *
     * <p>A {@code UNIQUE} do banco é a garantia; esta consulta existe para o
     * service poder recusar com uma mensagem que diz <b>qual</b> dia, em vez de
     * deixar a violação de constraint chegar crua ao usuário.
     */
    Optional<RegistroDiarioUti> findByTenantIdAndPessoaIdAndData(
            UUID tenantId, UUID pessoaId, LocalDate data);

    /** Quantos dias estão presos a esta avaliação — para o 409 dizer o número. */
    long countByTenantIdAndAvaliacaoId(UUID tenantId, UUID avaliacaoId);

    /**
     * A avaliação mais recente daquele paciente <b>até</b> aquela data.
     *
     * <p>Até, e não a mais recente de todas: um dia de três meses atrás não deve
     * ser comparado com a prescrição de ontem.
     */
    @Query(value = """
            SELECT a.id FROM avaliacao_uti a
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND a.paciente_id = CAST(:pessoaId AS uuid)
              AND a.data_avaliacao <= CAST(:data AS date)
            ORDER BY a.data_avaliacao DESC, a.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findIdDaAvaliacaoVigente(@Param("tenantId") UUID tenantId,
                                            @Param("pessoaId") UUID pessoaId,
                                            @Param("data") LocalDate data);

    /**
     * Os dias que alimentam os painéis — sem paginação e em <b>ordem
     * cronológica crescente</b>, que é o eixo X dos gráficos. A lista da tela
     * ordena ao contrário porque lá o dia de hoje é o que importa primeiro.
     */
    @Query(value = """
            SELECT r.* FROM registro_diario_uti r
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            ORDER BY r.data
            """, nativeQuery = true)
    List<RegistroDiarioUti> findParaPainel(@Param("tenantId") UUID tenantId,
                                           @Param("pessoaId") UUID pessoaId,
                                           @Param("de") LocalDate de,
                                           @Param("ate") LocalDate ate);

    @Query(value = """
            SELECT r.* FROM registro_diario_uti r
            JOIN pessoa p ON p.id = r.pessoa_id
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:pessoaNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pessoaNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            ORDER BY r.data DESC, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM registro_diario_uti r
            JOIN pessoa p ON p.id = r.pessoa_id
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:pessoaNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pessoaNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            """,
            nativeQuery = true)
    Page<RegistroDiarioUti> findAllWithFilters(Pageable pageable,
                                               @Param("tenantId") UUID tenantId,
                                               @Param("pessoaId") UUID pessoaId,
                                               @Param("pessoaNome") String pessoaNome,
                                               @Param("de") LocalDate de,
                                               @Param("ate") LocalDate ate);
}

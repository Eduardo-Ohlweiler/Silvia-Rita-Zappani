package com.nutri.hospitalar.pediatria.repository;

import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
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
 */
@Repository
public interface AvaliacaoPediatricaRepository extends JpaRepository<AvaliacaoPediatrica, UUID> {

    Optional<AvaliacaoPediatrica> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * O conjunto que os painéis agregam.
     *
     * <p>Devolve as linhas e a soma acontece em Java. Oito consultas nativas de
     * agregação seriam mais rápidas e muito mais fáceis de errar — e o conjunto
     * é limitado pelo período (um ano, por padrão) dentro de um tenant. Se um
     * dia o volume justificar, o lugar de mudar é aqui, sem tocar na tela.
     */
    @Query(value = """
            SELECT a.* FROM avaliacao_pediatrica a
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:formulaLacteaId AS uuid) IS NULL OR a.formula_lactea_id = CAST(:formulaLacteaId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:mesesMin AS integer) IS NULL OR a.idade_meses >= CAST(:mesesMin AS integer))
              AND (CAST(:mesesMax AS integer) IS NULL OR a.idade_meses <= CAST(:mesesMax AS integer))
              AND (CAST(:sexo AS text) IS NULL OR a.sexo = CAST(:sexo AS text))
            ORDER BY a.data_avaliacao, a.idade_meses
            """, nativeQuery = true)
    List<AvaliacaoPediatrica> findParaPainel(@Param("tenantId") UUID tenantId,
                                             @Param("pacienteId") UUID pacienteId,
                                             @Param("formulaLacteaId") UUID formulaLacteaId,
                                             @Param("de") LocalDate de,
                                             @Param("mesesMin") Integer mesesMin,
                                             @Param("mesesMax") Integer mesesMax,
                                             @Param("sexo") String sexo);

    @Query(value = """
            SELECT a.* FROM avaliacao_pediatrica a
            JOIN pessoa p ON p.id = a.paciente_id
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:formulaLacteaId AS uuid) IS NULL OR a.formula_lactea_id = CAST(:formulaLacteaId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR a.data_avaliacao <= CAST(:ate AS date))
              AND (CAST(:mesesMin AS integer) IS NULL OR a.idade_meses >= CAST(:mesesMin AS integer))
              AND (CAST(:mesesMax AS integer) IS NULL OR a.idade_meses <= CAST(:mesesMax AS integer))
            ORDER BY a.data_avaliacao DESC, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM avaliacao_pediatrica a
            JOIN pessoa p ON p.id = a.paciente_id
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pacienteId AS uuid) IS NULL OR a.paciente_id = CAST(:pacienteId AS uuid))
              AND (CAST(:formulaLacteaId AS uuid) IS NULL OR a.formula_lactea_id = CAST(:formulaLacteaId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR a.data_avaliacao >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR a.data_avaliacao <= CAST(:ate AS date))
              AND (CAST(:mesesMin AS integer) IS NULL OR a.idade_meses >= CAST(:mesesMin AS integer))
              AND (CAST(:mesesMax AS integer) IS NULL OR a.idade_meses <= CAST(:mesesMax AS integer))
            """,
            nativeQuery = true)
    Page<AvaliacaoPediatrica> findAllWithFilters(Pageable pageable,
                                                 @Param("tenantId") UUID tenantId,
                                                 @Param("pacienteId") UUID pacienteId,
                                                 @Param("formulaLacteaId") UUID formulaLacteaId,
                                                 @Param("de") LocalDate de,
                                                 @Param("ate") LocalDate ate,
                                                 @Param("mesesMin") Integer mesesMin,
                                                 @Param("mesesMax") Integer mesesMax);
}

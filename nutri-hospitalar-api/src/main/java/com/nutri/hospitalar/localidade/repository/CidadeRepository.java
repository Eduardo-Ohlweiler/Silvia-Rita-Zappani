package com.nutri.hospitalar.localidade.repository;

import com.nutri.hospitalar.localidade.entity.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, UUID> {

    /**
     * Busca para combo. Native com {@code CAST} e {@code unaccent} — regra 2 do
     * CLAUDE.md — e {@code LIMIT 100}: são mais de cinco mil municípios, e o
     * combo nunca deve tentar trazer todos.
     *
     * <p>Sem termo, devolve as primeiras em ordem alfabética; com {@code
     * estadoId}, restringe à UF, que é como se acha "Bom Jesus" (existe em
     * nove estados).
     */
    @Query(value = """
            SELECT c.* FROM cidade c
            WHERE c.ativo = true
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(c.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:estadoId AS uuid) IS NULL OR c.estado_id = CAST(:estadoId AS uuid))
            ORDER BY c.nome
            LIMIT 100
            """, nativeQuery = true)
    List<Cidade> findForSelect(@Param("nome") String nome,
                               @Param("estadoId") UUID estadoId);
}

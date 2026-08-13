package com.nutri.hospitalar.tenant.repository;

import com.nutri.hospitalar.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * {@code expirandoEmDias} restringe a quem vence dentro da janela — é o
     * filtro "Expirando" da tela. Já venceu não entra: quem venceu aparece
     * pelo filtro de inativos.
     */
    @Query(value = """
            SELECT t.* FROM tenant t
            WHERE (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(t.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR t.ativo = CAST(:ativo AS boolean))
              AND (CAST(:expirandoEmDias AS integer) IS NULL
                   OR (t.acesso_expira_em IS NOT NULL
                       AND t.acesso_expira_em >= now()
                       AND t.acesso_expira_em < now()
                           + (CAST(:expirandoEmDias AS integer) * interval '1 day')))
            ORDER BY t.nome
            """,
            countQuery = """
            SELECT count(*) FROM tenant t
            WHERE (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(t.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:ativo AS boolean) IS NULL OR t.ativo = CAST(:ativo AS boolean))
              AND (CAST(:expirandoEmDias AS integer) IS NULL
                   OR (t.acesso_expira_em IS NOT NULL
                       AND t.acesso_expira_em >= now()
                       AND t.acesso_expira_em < now()
                           + (CAST(:expirandoEmDias AS integer) * interval '1 day')))
            """,
            nativeQuery = true)
    Page<Tenant> findAllWithFilters(Pageable pageable,
                                    @Param("nome") String nome,
                                    @Param("ativo") Boolean ativo,
                                    @Param("expirandoEmDias") Integer expirandoEmDias);

    @Query(value = """
            SELECT t.* FROM tenant t
            WHERE t.ativo = true
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(t.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
            ORDER BY t.nome
            LIMIT 100
            """, nativeQuery = true)
    List<Tenant> findForSelect(@Param("nome") String nome);

    /**
     * Desliga os clientes com prazo vencido. Roda uma vez por dia pelo
     * {@code ManutencaoJob}.
     *
     * <p>O tenant que abriga um SUPERADMIN nunca é desligado — é a trava que
     * impede ficar trancado para fora do próprio sistema por causa de uma data
     * gravada errado. {@code usuario.tenant_id} é NOT NULL, então o
     * {@code NOT IN} não corre risco de virar nulo e zerar o UPDATE.
     */
    @Modifying
    @Query(value = """
            UPDATE tenant
            SET ativo = false,
                updated_at = CAST(:agora AS timestamptz)
            WHERE ativo = true
              AND acesso_expira_em IS NOT NULL
              AND acesso_expira_em < CAST(:agora AS timestamptz)
              AND id NOT IN (SELECT tenant_id FROM usuario WHERE role = 'SUPERADMIN')
            """, nativeQuery = true)
    int inativarExpirados(@Param("agora") Instant agora);
}

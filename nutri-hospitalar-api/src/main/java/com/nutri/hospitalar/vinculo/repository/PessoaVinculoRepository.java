package com.nutri.hospitalar.vinculo.repository;

import com.nutri.hospitalar.vinculo.entity.PessoaVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PessoaVinculoRepository extends JpaRepository<PessoaVinculo, UUID> {

    /**
     * Os vínculos da pessoa nos dois sentidos — é o que faz uma linha só
     * aparecer nos dois cadastros.
     */
    @Query("""
            SELECT v FROM PessoaVinculo v
            WHERE v.tenant.id = :tenantId
              AND (v.pessoaOrigem.id = :pessoaId OR v.pessoaDestino.id = :pessoaId)
            """)
    List<PessoaVinculo> findAllByPessoaIdAndTenantId(@Param("pessoaId") UUID pessoaId,
                                                     @Param("tenantId") UUID tenantId);

    boolean existsByTenantIdAndPessoaOrigemIdAndPessoaDestinoId(UUID tenantId,
                                                                UUID pessoaOrigemId,
                                                                UUID pessoaDestinoId);
}

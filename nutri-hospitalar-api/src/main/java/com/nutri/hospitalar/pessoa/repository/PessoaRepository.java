package com.nutri.hospitalar.pessoa.repository;

import com.nutri.hospitalar.pessoa.entity.Pessoa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    // ─── Escopo do tenant — nunca findById solto ────────────────────────
    Optional<Pessoa> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByCpfAndTenantId(String cpf, UUID tenantId);

    boolean existsByCpfAndTenantIdAndIdNot(String cpf, UUID tenantId, UUID id);

    boolean existsByCnpjAndTenantId(String cnpj, UUID tenantId);

    boolean existsByCnpjAndTenantIdAndIdNot(String cnpj, UUID tenantId, UUID id);

    boolean existsByRgAndTenantId(String rg, UUID tenantId);

    boolean existsByRgAndTenantIdAndIdNot(String rg, UUID tenantId, UUID id);

    /**
     * Listagem com filtros opcionais.
     *
     * <p>Native com {@code CAST} explícito — regra 2 do CLAUDE.md. Um filtro
     * único de documento cobre CPF e CNPJ: quem digita na tela não sabe de
     * antemão qual dos dois a pessoa tem.
     */
    @Query(value = """
            SELECT p.* FROM pessoa p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%'))
                   OR unaccent(lower(coalesce(p.nome_fantasia, ''))) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:documento AS text) IS NULL
                   OR coalesce(p.cpf, '') LIKE '%' || CAST(:documento AS text) || '%'
                   OR coalesce(p.cnpj, '') LIKE '%' || CAST(:documento AS text) || '%')
              AND (CAST(:tipoPessoa AS text) IS NULL OR p.tipo_pessoa = CAST(:tipoPessoa AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
              AND (CAST(:tipoCadastroId AS uuid) IS NULL OR EXISTS (
                    SELECT 1 FROM pessoa_tipo_cadastro ptc
                    WHERE ptc.pessoa_id = p.id
                      AND ptc.tipo_cadastro_id = CAST(:tipoCadastroId AS uuid)))
            ORDER BY p.nome
            """,
            countQuery = """
            SELECT count(*) FROM pessoa p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:nome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%'))
                   OR unaccent(lower(coalesce(p.nome_fantasia, ''))) LIKE unaccent(lower('%' || CAST(:nome AS text) || '%')))
              AND (CAST(:documento AS text) IS NULL
                   OR coalesce(p.cpf, '') LIKE '%' || CAST(:documento AS text) || '%'
                   OR coalesce(p.cnpj, '') LIKE '%' || CAST(:documento AS text) || '%')
              AND (CAST(:tipoPessoa AS text) IS NULL OR p.tipo_pessoa = CAST(:tipoPessoa AS text))
              AND (CAST(:ativo AS boolean) IS NULL OR p.ativo = CAST(:ativo AS boolean))
              AND (CAST(:tipoCadastroId AS uuid) IS NULL OR EXISTS (
                    SELECT 1 FROM pessoa_tipo_cadastro ptc
                    WHERE ptc.pessoa_id = p.id
                      AND ptc.tipo_cadastro_id = CAST(:tipoCadastroId AS uuid)))
            """,
            nativeQuery = true)
    Page<Pessoa> findAllWithFilters(Pageable pageable,
                                    @Param("tenantId") UUID tenantId,
                                    @Param("nome") String nome,
                                    @Param("documento") String documento,
                                    @Param("tipoPessoa") String tipoPessoa,
                                    @Param("ativo") Boolean ativo,
                                    @Param("tipoCadastroId") UUID tipoCadastroId);

    @Query(value = """
            SELECT p.* FROM pessoa p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
              AND p.ativo = true
              AND (CAST(:termo AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:termo AS text) || '%'))
                   OR coalesce(p.cpf, '') LIKE '%' || CAST(:termo AS text) || '%'
                   OR coalesce(p.cnpj, '') LIKE '%' || CAST(:termo AS text) || '%')
              AND (CAST(:tipoCadastroId AS uuid) IS NULL OR EXISTS (
                    SELECT 1 FROM pessoa_tipo_cadastro ptc
                    WHERE ptc.pessoa_id = p.id
                      AND ptc.tipo_cadastro_id = CAST(:tipoCadastroId AS uuid)))
            ORDER BY p.nome
            LIMIT 100
            """, nativeQuery = true)
    List<Pessoa> findForSelect(@Param("tenantId") UUID tenantId,
                               @Param("termo") String termo,
                               @Param("tipoCadastroId") UUID tipoCadastroId);
}

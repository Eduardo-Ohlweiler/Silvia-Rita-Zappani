package com.nutri.hospitalar.contato.repository;

import com.nutri.hospitalar.contato.entity.RedeSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RedeSocialRepository extends JpaRepository<RedeSocial, UUID> {

    /** Sempre pelo par pessoa + tenant: contato de outro cliente não aparece. */
    List<RedeSocial> findAllByPessoaIdAndTenantId(UUID pessoaId, UUID tenantId);
}

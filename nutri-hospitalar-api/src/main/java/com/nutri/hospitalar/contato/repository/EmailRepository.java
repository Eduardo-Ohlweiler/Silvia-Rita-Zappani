package com.nutri.hospitalar.contato.repository;

import com.nutri.hospitalar.contato.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    /** Sempre pelo par pessoa + tenant: contato de outro cliente não aparece. */
    List<Email> findAllByPessoaIdAndTenantId(UUID pessoaId, UUID tenantId);
}

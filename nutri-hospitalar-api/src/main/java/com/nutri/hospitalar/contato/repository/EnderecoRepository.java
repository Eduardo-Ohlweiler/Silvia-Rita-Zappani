package com.nutri.hospitalar.contato.repository;

import com.nutri.hospitalar.contato.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, UUID> {

    /** Sempre pelo par pessoa + tenant: endereço de outro cliente não aparece. */
    List<Endereco> findAllByPessoaIdAndTenantId(UUID pessoaId, UUID tenantId);
}

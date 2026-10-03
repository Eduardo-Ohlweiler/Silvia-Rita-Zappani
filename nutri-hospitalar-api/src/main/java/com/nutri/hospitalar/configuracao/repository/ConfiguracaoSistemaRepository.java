package com.nutri.hospitalar.configuracao.repository;

import com.nutri.hospitalar.configuracao.entity.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Sem tenant: a configuração é do sistema. A migration 033 garante uma linha
 * só; o {@code findFirst} não escolhe entre várias, só dispensa saber o id.
 */
@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, UUID> {

    Optional<ConfiguracaoSistema> findFirstByOrderByCreatedAtAsc();
}

package com.nutri.hospitalar.clinica.repository;

import com.nutri.hospitalar.clinica.entity.RespostaFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * A resposta é alcançada através da ficha, que já é filtrada por tenant. Existe
 * como repositório próprio para o {@code orphanRemoval} da ficha ter par, e para
 * os testes poderem afirmar sobre o retrato sem passar pela API.
 */
@Repository
public interface RespostaFichaRepository extends JpaRepository<RespostaFicha, UUID> {

    long countByFichaId(UUID fichaId);
}

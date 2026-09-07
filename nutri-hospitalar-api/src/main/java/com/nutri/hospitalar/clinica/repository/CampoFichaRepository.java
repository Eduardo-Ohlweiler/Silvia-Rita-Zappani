package com.nutri.hospitalar.clinica.repository;

import com.nutri.hospitalar.clinica.entity.CampoFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * O campo é alcançado <b>através do modelo</b> — não há consulta por tenant aqui
 * porque a tabela não tem a coluna: o modelo dono pode ser do sistema. Quem
 * garante o isolamento é o {@code ModeloFichaRepository}.
 */
@Repository
public interface CampoFichaRepository extends JpaRepository<CampoFicha, UUID> {

    List<CampoFicha> findByModeloIdOrderByOrdemAsc(UUID modeloId);
}

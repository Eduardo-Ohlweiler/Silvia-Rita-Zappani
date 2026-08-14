package com.nutri.hospitalar.localidade.repository;

import com.nutri.hospitalar.localidade.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, UUID> {

    List<Estado> findAllByAtivoTrueOrderByNome();
}

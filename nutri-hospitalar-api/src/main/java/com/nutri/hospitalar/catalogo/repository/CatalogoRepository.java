package com.nutri.hospitalar.catalogo.repository;

import com.nutri.hospitalar.catalogo.entity.CatalogoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/**
 * Base dos quatro catálogos. Sem {@code AndTenantId} em nenhum método: catálogo
 * é dado de referência, comum a todos os clientes — ver
 * {@link com.nutri.hospitalar.catalogo.entity.CatalogoEntity}.
 */
@NoRepositoryBean
public interface CatalogoRepository<T extends CatalogoEntity> extends JpaRepository<T, UUID> {

    List<T> findAllByAtivoTrueOrderByNome();
}

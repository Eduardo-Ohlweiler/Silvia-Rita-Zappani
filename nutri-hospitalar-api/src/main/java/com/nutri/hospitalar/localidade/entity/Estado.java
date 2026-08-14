package com.nutri.hospitalar.localidade.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unidade federativa. Dado de referência do IBGE, sem {@code tenant_id} —
 * ver docs/01-multitenant.md §6.1.
 */
@Entity
@Table(name = "estado")
@Getter
@Setter
@NoArgsConstructor
public class Estado extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "sigla", nullable = false, length = 2)
    private String sigla;

    @Column(name = "codigo_ibge", nullable = false)
    private Integer codigoIbge;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}

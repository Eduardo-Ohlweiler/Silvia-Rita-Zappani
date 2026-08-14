package com.nutri.hospitalar.catalogo.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base dos catálogos do cadastro de pessoas: tipo de cadastro, de telefone, de
 * e-mail e de rede social.
 *
 * <p>Estende {@link BaseEntity} e <b>não</b> {@code TenantEntity} de propósito.
 * São dados de referência, não de negócio: "Celular" e "Paciente" significam a
 * mesma coisa em todo cliente, e não há dado de paciente aqui dentro. Ver a
 * justificativa completa na migration {@code 009-catalogos-pessoa.xml}.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CatalogoEntity extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}

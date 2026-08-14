package com.nutri.hospitalar.localidade.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Município do IBGE. Como {@link Estado}, é referência global sem tenant.
 *
 * <p>São mais de cinco mil linhas: toda busca é no servidor e limitada — nunca
 * carregue a tabela inteira para filtrar no navegador.
 */
@Entity
@Table(name = "cidade")
@Getter
@Setter
@NoArgsConstructor
public class Cidade extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(name = "codigo_ibge", nullable = false)
    private Integer codigoIbge;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}

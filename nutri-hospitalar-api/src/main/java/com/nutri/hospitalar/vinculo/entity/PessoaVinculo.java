package com.nutri.hospitalar.vinculo.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.vinculo.enums.TipoVinculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aresta entre duas pessoas. <b>Uma linha serve os dois cadastros.</b>
 *
 * <p>A orientação é canônica: {@code pessoaOrigem} é sempre a de menor id, e o
 * {@code tipo} está gravado nessa orientação. Quem lê pelo lado do destino vê
 * o {@link TipoVinculo#inverso()}. Sem isso, o mesmo par entraria duas vezes
 * invertido e a unique {@code uk_pessoa_vinculo_par} não veria a duplicata.
 */
@Entity
@Table(name = "pessoa_vinculo")
@Getter
@Setter
@NoArgsConstructor
public class PessoaVinculo extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_origem_id", nullable = false)
    private Pessoa pessoaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_destino_id", nullable = false)
    private Pessoa pessoaDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoVinculo tipo;

    @Column(name = "observacao", length = 255)
    private String observacao;
}

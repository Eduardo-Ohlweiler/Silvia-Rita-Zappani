package com.nutri.hospitalar.contato.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base de telefone, e-mail e rede social.
 *
 * <p>O {@code tenant_id} herdado de {@link TenantEntity} é redundante com o da
 * pessoa, e isso é deliberado: permite recusar um contato de outro cliente sem
 * carregar a pessoa antes — o mesmo que o eroERP faz com {@code cliente_id}.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ContatoEntity extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(name = "observacao", length = 255)
    private String observacao;
}

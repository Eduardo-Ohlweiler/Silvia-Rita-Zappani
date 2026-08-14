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
 * Base dos dados de contato de uma pessoa: telefone, e-mail, rede social e
 * endereço.
 *
 * <p>O {@code tenant_id} herdado de {@link TenantEntity} é redundante com o da
 * pessoa, e isso é deliberado: permite recusar um contato de outro cliente sem
 * carregar a pessoa antes — o mesmo que o eroERP faz com {@code cliente_id}.
 *
 * <p>Só o que os quatro têm em comum mora aqui. {@code observacao} fica nas
 * filhas que a possuem — endereço não tem essa coluna, e sim
 * {@code complemento}.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ContatoEntity extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;
}

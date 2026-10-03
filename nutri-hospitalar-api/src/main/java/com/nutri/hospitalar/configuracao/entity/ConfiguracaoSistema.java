package com.nutri.hospitalar.configuracao.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.configuracao.enums.PaletaSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuração do sistema — uma linha só, <b>sem tenant</b>.
 *
 * <p>É a exceção declarada à regra 1: não é dado de negócio de um cliente, é
 * o modo como o sistema se apresenta a todos eles. Lida por qualquer um (o
 * login já abre no tema), escrita só pelo superadmin. A unicidade da linha é
 * garantida no banco pela migration 033.
 */
@Entity
@Table(name = "configuracao_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoSistema extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "paleta", nullable = false, length = 40)
    private PaletaSistema paleta = PaletaSistema.PADRAO;
}

package com.nutri.hospitalar.contato.entity;

import com.nutri.hospitalar.catalogo.entity.TipoRedeSocial;
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
 * Rede social da pessoa. Sem {@code principal}: não existe "rede social
 * principal" — o que identifica cada linha é o tipo.
 */
@Entity
@Table(name = "rede_social")
@Getter
@Setter
@NoArgsConstructor
public class RedeSocial extends ContatoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_rede_social_id", nullable = false)
    private TipoRedeSocial tipoRedeSocial;

    @Column(name = "usuario", length = 255)
    private String usuario;

    @Column(name = "url", length = 500)
    private String url;
}

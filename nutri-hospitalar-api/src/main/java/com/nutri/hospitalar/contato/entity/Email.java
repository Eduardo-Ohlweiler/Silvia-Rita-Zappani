package com.nutri.hospitalar.contato.entity;

import com.nutri.hospitalar.catalogo.entity.TipoEmail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email")
@Getter
@Setter
@NoArgsConstructor
public class Email extends ContatoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_email_id", nullable = false)
    private TipoEmail tipoEmail;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;
}

package com.nutri.hospitalar.contato.entity;

import com.nutri.hospitalar.catalogo.entity.TipoTelefone;
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
@Table(name = "telefone")
@Getter
@Setter
@NoArgsConstructor
public class Telefone extends ContatoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_telefone_id", nullable = false)
    private TipoTelefone tipoTelefone;

    @Column(name = "codigo_pais", nullable = false, length = 4)
    private String codigoPais = "55";

    /** Só dígitos, com DDD — a máscara é da tela. */
    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;
}

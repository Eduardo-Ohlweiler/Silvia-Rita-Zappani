package com.nutri.hospitalar.contato.entity;

import com.nutri.hospitalar.catalogo.entity.TipoEndereco;
import com.nutri.hospitalar.localidade.entity.Cidade;
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
 * Endereço da pessoa. A cidade vem da tabela do IBGE, não de texto livre.
 */
@Entity
@Table(name = "endereco")
@Getter
@Setter
@NoArgsConstructor
public class Endereco extends ContatoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_endereco_id", nullable = false)
    private TipoEndereco tipoEndereco;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cidade_id", nullable = false)
    private Cidade cidade;

    /** Só dígitos — a máscara é da tela. */
    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "rua", length = 255)
    private String rua;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;
}

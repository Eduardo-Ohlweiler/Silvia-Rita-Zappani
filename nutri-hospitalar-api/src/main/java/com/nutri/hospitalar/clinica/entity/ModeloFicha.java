package com.nutri.hospitalar.clinica.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.tenant.entity.Tenant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de ficha de anamnese — o conjunto de perguntas que uma ficha faz.
 *
 * <p>É <b>catálogo híbrido</b>, como {@code FormulaEnteral} e
 * {@code FormulaLactea}: o {@code tenant} é anulável.
 *
 * <pre>
 *   tenant == null  →  modelo do sistema: visível a todo cliente, editável por
 *                      nenhum. Vem do seed da migration 030.
 *   tenant != null  →  modelo daquele cliente: só ele enxerga, e só ele edita.
 * </pre>
 *
 * <p>Adaptar um modelo do sistema é <b>clonar</b>: a cópia nasce com o tenant
 * preenchido e os mesmos campos, e a partir daí é do cliente. É o que impede
 * que um cliente estrague, para todos os outros, o modelo que todos usam.
 */
@Entity
@Table(name = "modelo_ficha")
@Getter
@Setter
@NoArgsConstructor
public class ModeloFicha extends BaseEntity {

    /** Anulável: nulo é modelo do sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /**
     * As perguntas, sempre na ordem em que se responde.
     *
     * <p>{@code orphanRemoval} porque a lista é o estado completo: o campo que
     * não veio no PUT foi removido pelo usuário, e some. É o contrato de
     * {@code ContatoService} (docs/08 §3.3).
     */
    @OneToMany(mappedBy = "modelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<CampoFicha> campos = new ArrayList<>();

    /** Modelo do sistema: visível para todo tenant, editável por nenhum. */
    public boolean ehGlobal() {
        return this.tenant == null;
    }

    /** Mantém os dois lados da relação coerentes ao montar o modelo. */
    public void adicionarCampo(CampoFicha campo) {
        campo.setModelo(this);
        this.campos.add(campo);
    }
}

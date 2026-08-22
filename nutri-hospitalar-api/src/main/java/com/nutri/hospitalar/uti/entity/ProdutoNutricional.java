package com.nutri.hospitalar.uti.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;
import com.nutri.hospitalar.uti.enums.TipoProdutoNutricional;
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

import java.math.BigDecimal;

/**
 * Produto nutricional por porção: suplemento oral, módulo proteico ou insumo de
 * dieta artesanal. Uma tabela, três papéis — ver {@link TipoProdutoNutricional}.
 *
 * <p><b>O motivo de existir é acabar com um CRUD órfão.</b> No eroERP os
 * suplementos são 26 linhas cadastradas que não alimentam cálculo nenhum,
 * enquanto os 3 módulos proteicos vivem em constante
 * ({@code calculoDietaEnteral.ts:16-20}) e os 4 insumos artesanais em outra
 * ({@code calculoSistemaAberto.ts:6-11}). Cadastrar um módulo novo lá não
 * produz efeito. Aqui o cálculo itera o catálogo.
 *
 * <p>Os {@code CHECK} da migration {@code 019} guardam três coerências que o
 * código não deveria ter de reconferir: papel artesanal existe se e só se o tipo
 * é insumo, a flag {@code modulo_proteico} não discorda do tipo, e o que entra
 * em cálculo tem composição.
 */
@Entity
@Table(name = "produto_nutricional")
@Getter
@Setter
@NoArgsConstructor
public class ProdutoNutricional extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoProdutoNutricional tipo;

    // ─── A medida a que a composição se refere ──────────────────────────

    /** Como a medida se chama no rótulo: "medida", "sachê", "frasco", "ml". */
    @Column(name = "medida_nome", nullable = false, length = 30)
    private String medidaNome;

    /** Gramas ou ml de UMA medida — o denominador da composição. */
    @Column(name = "medida_qtd", nullable = false)
    private BigDecimal medidaQtd;

    /**
     * Gramas ou ml da embalagem fechada.
     *
     * <p>Sem isto não existe cálculo de latas por mês, que é o que a
     * nutricionista leva para a compra: Trophic 800 g, Carbodex 500 g,
     * Albumix 500 g, óleo 900 ml.
     */
    @Column(name = "embalagem_qtd")
    private BigDecimal embalagemQtd;

    // ─── Composição, por medida ─────────────────────────────────────────
    @Column(name = "kcal")                private BigDecimal kcal;
    @Column(name = "proteina_g")          private BigDecimal proteinaG;
    @Column(name = "cho_g")               private BigDecimal choG;
    @Column(name = "acucar_g")            private BigDecimal acucarG;
    @Column(name = "lip_g")               private BigDecimal lipG;
    @Column(name = "sodio_mg")            private BigDecimal sodioMg;
    @Column(name = "potassio_mg")         private BigDecimal potassioMg;
    @Column(name = "fosforo_mg")          private BigDecimal fosforoMg;
    @Column(name = "ferro_mg")            private BigDecimal ferroMg;
    @Column(name = "fibras_g")            private BigDecimal fibrasG;
    @Column(name = "osmolaridade_mosm_l") private BigDecimal osmolaridadeMosmL;

    // ─── Papel no cálculo ───────────────────────────────────────────────

    /**
     * Redundante com {@code tipo == MODULO_PROTEICO}, e o banco impõe que os
     * dois concordem. Existe para a consulta do cálculo ser um índice sobre
     * booleano em vez de comparação de texto.
     */
    @Column(name = "modulo_proteico", nullable = false)
    private Boolean moduloProteico = false;

    /** Obrigatório em insumo artesanal, proibido nos outros dois. */
    @Enumerated(EnumType.STRING)
    @Column(name = "papel_artesanal", length = 20)
    private PapelArtesanal papelArtesanal;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /** Produto do sistema: visível para todo tenant, editável por nenhum. */
    public boolean ehGlobal() {
        return this.tenant == null;
    }
}

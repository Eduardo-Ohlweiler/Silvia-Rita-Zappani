package com.nutri.hospitalar.uti.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.tenant.entity.Tenant;
import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;
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
 * Fórmula enteral industrializada e a sua composição.
 *
 * <p><b>A composição é SEMPRE por litro.</b> Não há coluna de apresentação nem
 * ramo de 500 ml, e isso é deliberado: na planilha de origem o
 * {@code Fresubin 2kcal HP} traz a composição da embalagem de 500 ml, a aba
 * Contínuo divide por 1000 e a Intermitente por 500 — a mesma fórmula com
 * <b>proteína subestimada em 50 %</b> numa das duas telas. Normalizando o
 * cadastro, o erro deixa de ser representável. Ver {@code docs/10} §8.1.
 *
 * <p>O banco reforça isso com o {@code CHECK} de fechamento energético: a soma
 * dos macros por Atwater tem de ficar a menos de 12 % da densidade declarada.
 * Foi ele que encontrou os quatro produtos errados da planilha.
 *
 * <p>Como {@code FormulaLactea} da pediatria, estende {@link BaseEntity} e
 * declara o {@code tenant} <b>anulável</b>:
 *
 * <pre>
 *   tenant == null  →  fórmula global do sistema
 *   tenant != null  →  fórmula privada daquele cliente
 * </pre>
 */
@Entity
@Table(name = "formula_enteral")
@Getter
@Setter
@NoArgsConstructor
public class FormulaEnteral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    /** Rótulo de escolha, não operando de cálculo. Anulável de propósito. */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaFormulaEnteral categoria;

    /** kcal/ml. É o operando de que sai todo o resto da dieta. */
    @Column(name = "densidade_kcal_ml", nullable = false)
    private BigDecimal densidadeKcalMl;

    @Column(name = "proteina_g_l", nullable = false)
    private BigDecimal proteinaGL;

    @Column(name = "cho_g_l")
    private BigDecimal choGL;

    @Column(name = "lip_g_l")
    private BigDecimal lipGL;

    @Column(name = "fibras_g_l")
    private BigDecimal fibrasGL;

    @Column(name = "potassio_mg_l")
    private BigDecimal potassioMgL;

    @Column(name = "osmolaridade_mosm_l")
    private BigDecimal osmolaridadeMosmL;

    /**
     * Teor de água livre, em % do volume — <b>dado do rótulo do produto</b>.
     *
     * <p>Anulável, e o cálculo trata a ausência: sem este valor ele cai na
     * tabela por densidade e <b>diz que caiu</b>, em vez de fingir precisão.
     * Água livre não se deriva da densidade; a escada por densidade é
     * aproximação por faixa. Ver {@code docs/10} §5.1.
     */
    @Column(name = "agua_livre_perc")
    private BigDecimal aguaLivrePerc;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /** Fórmula do sistema: visível para todo tenant, editável por nenhum. */
    public boolean ehGlobal() {
        return this.tenant == null;
    }
}

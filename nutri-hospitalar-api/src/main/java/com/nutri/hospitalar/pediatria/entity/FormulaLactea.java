package com.nutri.hospitalar.pediatria.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Fórmula láctea e a sua composição por 100 ml.
 *
 * <p>Estende {@link BaseEntity} e declara o seu próprio {@code tenant}
 * <b>anulável</b> — não pode usar {@code TenantEntity}, cujo tenant é
 * {@code optional = false}. É a única entidade da fatia que foge do
 * {@code tenant_id NOT NULL}:
 *
 * <pre>
 *   tenant == null  →  fórmula global do sistema
 *   tenant != null  →  fórmula privada daquele cliente
 * </pre>
 *
 * <p>As 10 globais vêm da planilha e são produtos de mercado: a composição do
 * NAN 2 é a mesma em qualquer hospital. Ler é de todos; escrever continua sendo
 * dentro do tenant, e o service recusa alterar uma global.
 */
@Entity
@Table(name = "formula_lactea")
@Getter
@Setter
@NoArgsConstructor
public class FormulaLactea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "kcal_por_100ml", nullable = false)
    private BigDecimal kcalPor100ml;

    @Column(name = "proteina_por_100ml", nullable = false)
    private BigDecimal proteinaPor100ml;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /** Fórmula do sistema: visível para todo tenant, editável por nenhum. */
    public boolean ehGlobal() {
        return this.tenant == null;
    }
}

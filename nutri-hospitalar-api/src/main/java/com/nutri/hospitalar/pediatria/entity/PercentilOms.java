package com.nutri.hospitalar.pediatria.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Uma linha das curvas de crescimento da OMS: um sexo, uma idade em meses, e
 * os percentis de peso, estatura e IMC.
 *
 * <p>Estende {@link BaseEntity} e não {@code TenantEntity}: é dado de
 * referência internacional, e nenhuma clínica tem a sua própria curva da OMS.
 * Ver a migration {@code 015-create-percentil-oms.xml}.
 *
 * <p><b>Somente leitura.</b> A tabela é semeada pela migration e nenhum service
 * escreve nela — não há repository de escrita nem endpoint de cadastro.
 */
@Entity
@Table(name = "percentil_oms")
@Getter
@Setter
@NoArgsConstructor
public class PercentilOms extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 20)
    private Sexo sexo;

    @Column(name = "idade_meses", nullable = false)
    private Integer idadeMeses;

    // ─── Peso para a idade — kg ─────────────────────────────────────────
    @Column(name = "peso_p3",  nullable = false) private BigDecimal pesoP3;
    @Column(name = "peso_p15", nullable = false) private BigDecimal pesoP15;
    @Column(name = "peso_p50", nullable = false) private BigDecimal pesoP50;
    @Column(name = "peso_p85", nullable = false) private BigDecimal pesoP85;
    @Column(name = "peso_p97", nullable = false) private BigDecimal pesoP97;

    // ─── Estatura para a idade — cm ─────────────────────────────────────
    @Column(name = "estatura_p3",  nullable = false) private BigDecimal estaturaP3;
    @Column(name = "estatura_p15", nullable = false) private BigDecimal estaturaP15;
    @Column(name = "estatura_p50", nullable = false) private BigDecimal estaturaP50;
    @Column(name = "estatura_p85", nullable = false) private BigDecimal estaturaP85;
    @Column(name = "estatura_p97", nullable = false) private BigDecimal estaturaP97;

    // ─── IMC para a idade — kg/m² ───────────────────────────────────────
    @Column(name = "imc_p3",  nullable = false) private BigDecimal imcP3;
    @Column(name = "imc_p15", nullable = false) private BigDecimal imcP15;
    @Column(name = "imc_p50", nullable = false) private BigDecimal imcP50;
    @Column(name = "imc_p85", nullable = false) private BigDecimal imcP85;
    @Column(name = "imc_p97", nullable = false) private BigDecimal imcP97;
}

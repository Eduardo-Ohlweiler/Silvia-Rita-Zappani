package com.nutri.hospitalar.pediatria.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pediatria.enums.FaixaOms;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.usuario.entity.Usuario;
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
import java.time.LocalDate;

/**
 * Uma avaliação pediátrica registrada: as entradas, os resultados e o retrato
 * da fórmula usada.
 *
 * <p><b>Os resultados ficam gravados, e abrir a avaliação não recalcula.</b> Se
 * as curvas da OMS ou as DRIs mudarem amanhã, o registro de hoje continua
 * mostrando o que se decidiu hoje, com os dados que se tinha. Prontuário não se
 * reescreve sozinho.
 *
 * <p>Pela mesma razão a fórmula é copiada campo a campo ao lado da FK: editar
 * ou desativar a fórmula no catálogo não altera avaliação já feita.
 *
 * <p>Paciente e profissional são {@link Pessoa} — não há tabela de paciente
 * neste sistema, paciente é um tipo de cadastro (docs/08 §1).
 */
@Entity
@Table(name = "avaliacao_pediatrica")
@Getter
@Setter
@NoArgsConstructor
public class AvaliacaoPediatrica extends TenantEntity {

    // ─── Quem ───────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Pessoa paciente;

    /** Opcional: nem toda avaliação tem um profissional identificado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Pessoa profissional;

    // ─── Entradas ───────────────────────────────────────────────────────
    @Column(name = "data_avaliacao", nullable = false)
    private LocalDate dataAvaliacao;

    /** Copiado da pessoa, ou informado na hora: escolheu a curva da OMS. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 20)
    private Sexo sexo;

    @Column(name = "idade_meses", nullable = false)
    private Integer idadeMeses;

    @Column(name = "peso", nullable = false)
    private BigDecimal peso;

    @Column(name = "estatura")
    private BigDecimal estatura;

    // ─── Dieta prescrita, com o retrato da fórmula ──────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formula_lactea_id")
    private FormulaLactea formulaLactea;

    @Column(name = "formula_nome", length = 255)
    private String formulaNome;

    @Column(name = "formula_kcal_por_100ml")
    private BigDecimal formulaKcalPor100ml;

    @Column(name = "formula_proteina_por_100ml")
    private BigDecimal formulaProteinaPor100ml;

    @Column(name = "volume_ml")
    private BigDecimal volumeMl;

    /** Intervalo entre tomadas, em horas — não a quantidade delas. */
    @Column(name = "frequencia_horas")
    private BigDecimal frequenciaHoras;

    // ─── Resultados — estado nutricional ────────────────────────────────
    @Column(name = "imc")
    private BigDecimal imc;

    @Enumerated(EnumType.STRING)
    @Column(name = "classif_peso_idade", length = 20)
    private FaixaOms classifPesoIdade;

    @Enumerated(EnumType.STRING)
    @Column(name = "classif_estatura_idade", length = 20)
    private FaixaOms classifEstaturaIdade;

    @Enumerated(EnumType.STRING)
    @Column(name = "classif_imc_idade", length = 20)
    private FaixaOms classifImcIdade;

    // ─── Resultados — necessidades ──────────────────────────────────────
    @Column(name = "vet")
    private BigDecimal vet;

    @Column(name = "proteina_necessidade")
    private BigDecimal proteinaNecessidade;

    // ─── Resultados — dieta ─────────────────────────────────────────────
    @Column(name = "vezes_dia")       private BigDecimal vezesDia;
    @Column(name = "volume_total")    private BigDecimal volumeTotal;
    @Column(name = "calorias_totais") private BigDecimal caloriasTotais;
    @Column(name = "proteina_total")  private BigDecimal proteinaTotal;
    @Column(name = "perc_calorico")   private BigDecimal percCalorico;
    @Column(name = "perc_proteico")   private BigDecimal percProteico;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    // ─── Auditoria ──────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
}

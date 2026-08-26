package com.nutri.hospitalar.uti.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.uti.enums.SuporteVentilatorio;
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
 * Um dia de acompanhamento na UTI.
 *
 * <p><b>Só o que foi medido fica aqui.</b> Percentual recebido, kcal/kg de fato
 * ofertados, diurese em ml/kg/h e média de aceitação oral são <b>derivados na
 * leitura</b>, não colunas — porque dependem da avaliação vinculada, e duplicar
 * um valor que se pode calcular é criar duas versões que um dia divergem. No
 * eroERP o {@code % recebido} é campo digitável ao lado de um calculado, e os
 * dois vão para o banco.
 *
 * <p>O vínculo com a {@link AvaliacaoUti} é <b>opcional</b>: paciente que
 * internou de madrugada tem dia antes de avaliação, e exigir o vínculo travaria
 * o uso à beira do leito. Quando existe, é ele que dá o peso e a composição da
 * fórmula para os derivados.
 */
@Entity
@Table(name = "registro_diario_uti")
@Getter
@Setter
@NoArgsConstructor
public class RegistroDiarioUti extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /**
     * A avaliação que estava valendo no dia.
     *
     * <p>{@code ON DELETE RESTRICT}: apagar uma avaliação com dias vinculados é
     * recusado, e o service diz quantos. Um cascade levaria o acompanhamento
     * inteiro embora sem avisar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id")
    private AvaliacaoUti avaliacao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    // ─── Dieta e TNE ────────────────────────────────────────────────────
    @Column(name = "dieta", length = 255)     private String dieta;
    @Column(name = "vol_prescrito_24h")       private BigDecimal volPrescrito24h;
    @Column(name = "vol_recebido_24h")        private BigDecimal volRecebido24h;

    // ─── Laboratório ────────────────────────────────────────────────────
    @Column(name = "mg")        private BigDecimal mg;
    @Column(name = "k")         private BigDecimal k;
    @Column(name = "na")        private BigDecimal na;
    @Column(name = "lactato")   private BigDecimal lactato;
    @Column(name = "pcr")       private BigDecimal pcr;
    @Column(name = "ph")        private BigDecimal ph;
    @Column(name = "pco2")      private BigDecimal pco2;
    @Column(name = "hco3")      private BigDecimal hco3;

    /** Numérico. No eroERP é {@code VARCHAR(255)}, e por isso o painel não o plota. */
    @Column(name = "hgt")       private BigDecimal hgt;

    // ─── Clínica e balanço ──────────────────────────────────────────────

    /**
     * O modo, separado da FiO₂ — no eroERP os dois moram num
     * {@code VARCHAR(255)} rotulado "VM / O₂ (%)", com máscara numérica no
     * formulário e texto no banco.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "suporte_ventilatorio", length = 30)
    private SuporteVentilatorio suporteVentilatorio;

    @Column(name = "fio2_perc")         private BigDecimal fio2Perc;

    /** Duas colunas numéricas: "120/80" em texto livre não entra em gráfico. */
    @Column(name = "pa_sistolica")      private BigDecimal paSistolica;
    @Column(name = "pa_diastolica")     private BigDecimal paDiastolica;

    /** Pode ser negativo — é a única grandeza da tabela que pode. */
    @Column(name = "balanco_hidrico_ml") private BigDecimal balancoHidricoMl;

    @Column(name = "diurese_ml")        private BigDecimal diureseMl;
    @Column(name = "evacuacao", length = 100) private String evacuacao;

    // ─── Ingestão oral, % de aceitação por refeição ─────────────────────
    @Column(name = "cafe_manha")    private BigDecimal cafeManha;
    @Column(name = "lanche_manha")  private BigDecimal lancheManha;
    @Column(name = "almoco")        private BigDecimal almoco;
    @Column(name = "lanche_tarde")  private BigDecimal lancheTarde;
    @Column(name = "jantar")        private BigDecimal jantar;
    @Column(name = "ceia")          private BigDecimal ceia;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
}

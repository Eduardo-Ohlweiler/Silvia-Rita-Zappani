package com.nutri.hospitalar.uti.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.usuario.entity.Usuario;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;
import com.nutri.hospitalar.uti.enums.FaseTerapia;
import com.nutri.hospitalar.uti.enums.JanelaPerdaPeso;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.EnumSet;
import java.util.Set;

/**
 * Uma avaliação de terapia nutricional de UTI registrada: as entradas, os
 * resultados, o retrato da fórmula e <b>a origem de cada valor da cascata</b>.
 *
 * <p><b>Os resultados ficam gravados, e abrir a avaliação não recalcula.</b> Se
 * as equações mudarem amanhã, o registro de hoje continua mostrando o que se
 * decidiu hoje. Prontuário não se reescreve sozinho.
 *
 * <p>As classificações gravam <b>rótulo e tom</b>, não o número que os gerou: o
 * rótulo é o texto que o profissional leu e o tom é a cor que ele viu. Guardar
 * só o número e reclassificar na leitura mudaria o registro sempre que a régua
 * mudasse.
 *
 * <p>O retrato da fórmula vai <b>completo, com os macros</b> — o eroERP guarda
 * só nome, densidade e proteína, e reabrir uma avaliação cuja fórmula saiu do
 * catálogo mostra a composição pela metade.
 *
 * <p>Paciente e profissional são {@link Pessoa}: não há tabela de paciente neste
 * sistema (docs/08 §1). O profissional é opcional — no eroERP ele é um
 * {@link Usuario}, o que impede registrar o nutricionista sem login.
 */
@Entity
@Table(name = "avaliacao_uti")
@Getter
@Setter
@NoArgsConstructor
public class AvaliacaoUti extends TenantEntity {

    // ─── Quem ───────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Pessoa paciente;

    /** Opcional: nem toda avaliação tem um profissional identificado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Pessoa profissional;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDate dataAvaliacao;

    // ─── Entradas · antropometria ───────────────────────────────────────
    @Enumerated(EnumType.STRING) @Column(name = "sexo", length = 20)
    private Sexo sexo;

    @Enumerated(EnumType.STRING) @Column(name = "etnia", length = 20)
    private EtniaChumlea etnia;

    @Column(name = "idade_anos")            private Integer idadeAnos;
    @Column(name = "altura_cm")             private BigDecimal alturaCm;
    @Column(name = "altura_joelho_cm")      private BigDecimal alturaJoelhoCm;
    @Column(name = "circ_braco_cm")         private BigDecimal circBracoCm;
    @Column(name = "circ_panturrilha_cm")   private BigDecimal circPanturrilhaCm;
    @Column(name = "circ_abdominal_cm")     private BigDecimal circAbdominalCm;
    @Column(name = "peso_atual_kg")         private BigDecimal pesoAtualKg;
    @Column(name = "peso_usual_kg")         private BigDecimal pesoUsualKg;

    @Enumerated(EnumType.STRING) @Column(name = "janela_perda", length = 20)
    private JanelaPerdaPeso janelaPerda;

    @Enumerated(EnumType.STRING) @Column(name = "populacao_referencia", length = 30)
    private PopulacaoReferencia populacaoReferencia;

    @Enumerated(EnumType.STRING) @Column(name = "origem_peso_preferida", length = 30)
    private OrigemValor origemPesoPreferida;

    /**
     * Tabela filha, e não texto separado por vírgula: valor de enum concatenado
     * num VARCHAR não aceita {@code CHECK}, e um dia alguém o consulta com LIKE.
     *
     * <p>{@code EAGER} porque são no máximo oito linhas e toda leitura da
     * avaliação precisa delas — um {@code LAZY} aqui só produziria N+1.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "avaliacao_uti_segmento",
            joinColumns = @JoinColumn(name = "avaliacao_uti_id"))
    @Column(name = "segmento", length = 30)
    @Enumerated(EnumType.STRING)
    private Set<SegmentoAmputado> segmentosAmputados = EnumSet.noneOf(SegmentoAmputado.class);

    // ─── Entradas · necessidades ────────────────────────────────────────
    @Enumerated(EnumType.STRING) @Column(name = "fase", length = 20)
    private FaseTerapia fase;

    @Enumerated(EnumType.STRING) @Column(name = "terapia_renal", length = 30)
    private TerapiaRenal terapiaRenal;

    @Column(name = "kcal_por_kg_alvo")      private BigDecimal kcalPorKgAlvo;
    @Column(name = "proteina_por_kg_alvo")  private BigDecimal proteinaPorKgAlvo;

    // ─── Entradas · dieta, com o retrato da fórmula ─────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formula_enteral_id")
    private FormulaEnteral formulaEnteral;

    @Column(name = "formula_nome", length = 255)  private String formulaNome;
    @Column(name = "formula_densidade_kcal_ml")   private BigDecimal formulaDensidadeKcalMl;
    @Column(name = "formula_proteina_g_l")        private BigDecimal formulaProteinaGL;
    @Column(name = "formula_cho_g_l")             private BigDecimal formulaChoGL;
    @Column(name = "formula_lip_g_l")             private BigDecimal formulaLipGL;
    @Column(name = "formula_fibras_g_l")          private BigDecimal formulaFibrasGL;
    @Column(name = "formula_potassio_mg_l")       private BigDecimal formulaPotassioMgL;
    @Column(name = "formula_agua_livre_perc")     private BigDecimal formulaAguaLivrePerc;

    @Enumerated(EnumType.STRING) @Column(name = "modo_infusao", length = 20)
    private ModoInfusao modoInfusao;

    @Column(name = "volume_por_tempo")      private BigDecimal volumePorTempo;
    @Column(name = "tempo")                 private BigDecimal tempo;

    // ─── Entrada · hidratação ───────────────────────────────────────────
    @Column(name = "volume_dieta_manual_ml") private BigDecimal volumeDietaManualMl;

    // ─── Resultados · estimativas e cascata ─────────────────────────────
    @Column(name = "altura_estimada_cm")    private BigDecimal alturaEstimadaCm;
    @Column(name = "peso_chumlea_kg")       private BigDecimal pesoChumleaKg;
    @Column(name = "peso_jung_kg")          private BigDecimal pesoJungKg;
    @Column(name = "peso_rabito_kg")        private BigDecimal pesoRabitoKg;

    @Column(name = "peso_trabalho_kg")      private BigDecimal pesoTrabalhoKg;

    /**
     * "peso estimado · Rabito 2008", por extenso.
     *
     * <p>É coluna, não derivação, porque é parte do registro clínico: sem ela
     * ninguém sabe sobre que número a prescrição foi feita — o defeito 15 de
     * {@code docs/10} §11, levado até o banco.
     */
    @Column(name = "peso_trabalho_origem", length = 60)
    private String pesoTrabalhoOrigem;

    @Column(name = "altura_usada_cm")       private BigDecimal alturaUsadaCm;
    @Column(name = "altura_usada_origem", length = 60) private String alturaUsadaOrigem;

    // ─── Resultados · diagnóstico ───────────────────────────────────────
    @Column(name = "imc")                        private BigDecimal imc;
    @Column(name = "classif_imc_oms", length = 60)      private String classifImcOms;
    @Column(name = "classif_imc_oms_tom", length = 20)  private String classifImcOmsTom;
    @Column(name = "classif_imc_opas", length = 60)     private String classifImcOpas;
    @Column(name = "classif_imc_opas_tom", length = 20) private String classifImcOpasTom;

    @Column(name = "peso_ideal_kg")         private BigDecimal pesoIdealKg;
    @Column(name = "peso_ideal_imc25_kg")   private BigDecimal pesoIdealImc25Kg;
    @Column(name = "peso_ajustado_kg")      private BigDecimal pesoAjustadoKg;
    @Column(name = "peso_amputacao_kg")     private BigDecimal pesoAmputacaoKg;

    @Column(name = "perc_perda_peso")       private BigDecimal percPerdaPeso;
    @Column(name = "classif_perda_peso", length = 60)     private String classifPerdaPeso;
    @Column(name = "classif_perda_peso_tom", length = 20) private String classifPerdaPesoTom;

    @Column(name = "p50_circ_braco_cm")         private BigDecimal p50CircBracoCm;
    @Column(name = "adequacao_circ_braco_perc") private BigDecimal adequacaoCircBracoPerc;
    @Column(name = "classif_adequacao_cb", length = 60)     private String classifAdequacaoCb;
    @Column(name = "classif_adequacao_cb_tom", length = 20) private String classifAdequacaoCbTom;

    @Column(name = "circ_braco_ajustada_cm")    private BigDecimal circBracoAjustadaCm;
    @Column(name = "classif_massa_braco", length = 60)      private String classifMassaBraco;
    @Column(name = "classif_massa_braco_tom", length = 20)  private String classifMassaBracoTom;
    @Column(name = "circ_panturrilha_ajustada_cm") private BigDecimal circPanturrilhaAjustadaCm;
    @Column(name = "classif_deplecao_cp", length = 60)      private String classifDeplecaoCp;
    @Column(name = "classif_deplecao_cp_tom", length = 20)  private String classifDeplecaoCpTom;

    // ─── Resultados · necessidades ──────────────────────────────────────
    @Column(name = "energia_minima")        private BigDecimal energiaMinima;
    @Column(name = "energia_maxima")        private BigDecimal energiaMaxima;
    @Column(name = "proteina_minima")       private BigDecimal proteinaMinima;
    @Column(name = "proteina_maxima")       private BigDecimal proteinaMaxima;
    @Column(name = "meta_energetica")       private BigDecimal metaEnergetica;
    @Column(name = "meta_energetica_origem", length = 60) private String metaEnergeticaOrigem;
    @Column(name = "meta_proteica")         private BigDecimal metaProteica;
    @Column(name = "meta_proteica_origem", length = 60)   private String metaProteicaOrigem;
    @Column(name = "proteina_terapia_renal") private BigDecimal proteinaTerapiaRenal;

    @Column(name = "obeso", nullable = false) private Boolean obeso = false;
    @Column(name = "base_do_peso", length = 120) private String baseDoPeso;

    // ─── Resultados · dieta ─────────────────────────────────────────────
    @Column(name = "volume_total_ml")       private BigDecimal volumeTotalMl;
    @Column(name = "calorias_ofertadas")    private BigDecimal caloriasOfertadas;
    @Column(name = "proteina_ofertada")     private BigDecimal proteinaOfertada;
    @Column(name = "calorias_por_quilo")    private BigDecimal caloriasPorQuilo;
    @Column(name = "proteina_por_quilo")    private BigDecimal proteinaPorQuilo;
    @Column(name = "percentual_do_vct")     private BigDecimal percentualDoVct;
    @Column(name = "percentual_da_proteina") private BigDecimal percentualDaProteina;
    @Column(name = "cho_ofertado")          private BigDecimal choOfertado;
    @Column(name = "lip_ofertado")          private BigDecimal lipOfertado;
    @Column(name = "fibras_ofertadas")      private BigDecimal fibrasOfertadas;
    @Column(name = "potassio_ofertado")     private BigDecimal potassioOfertado;
    @Column(name = "volume_pleno")          private BigDecimal volumePleno;
    @Column(name = "proteina_no_volume_pleno") private BigDecimal proteinaNoVolumePleno;
    @Column(name = "proteina_suplementar")  private BigDecimal proteinaSuplementar;

    // ─── Resultados · hidratação ────────────────────────────────────────
    @Column(name = "hidratacao_necessidade_minima") private BigDecimal hidratacaoNecessidadeMinima;
    @Column(name = "hidratacao_necessidade_ideal")  private BigDecimal hidratacaoNecessidadeIdeal;
    @Column(name = "hidratacao_percentual_agua")    private BigDecimal hidratacaoPercentualAgua;
    @Column(name = "hidratacao_percentual_agua_origem", length = 60)
    private String hidratacaoPercentualAguaOrigem;
    @Column(name = "hidratacao_agua_na_dieta")      private BigDecimal hidratacaoAguaNaDieta;
    @Column(name = "hidratacao_agua_extra_minima")  private BigDecimal hidratacaoAguaExtraMinima;
    @Column(name = "hidratacao_agua_extra_ideal")   private BigDecimal hidratacaoAguaExtraIdeal;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    // ─── Auditoria de autoria ───────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
}

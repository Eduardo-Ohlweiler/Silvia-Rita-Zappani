package com.nutri.hospitalar.pediatria.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.usuario.entity.Usuario;
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
import java.time.LocalDate;

/**
 * Um dia de acompanhamento nutricional pediátrico. Especificação em
 * {@code docs/11}.
 *
 * <p><b>Só o que foi medido e ofertado fica aqui.</b> Percentual recebido,
 * kcal/kg, adequação calórica e proteica, IMC e as três classificações da OMS
 * são <b>derivados na leitura</b>, não colunas — porque dependem da avaliação
 * vinculada e da tabela de percentis, e duplicar um valor que se pode calcular é
 * criar duas versões que um dia divergem.
 *
 * <p><b>Peso e estatura são a diferença em relação ao dia de UTI.</b> Lá o peso é
 * da avaliação e não muda entre os dias; aqui a criança <b>cresce</b>, e é
 * exatamente isso que o acompanhamento existe para mostrar. A idade de cada
 * registro sai da data de nascimento e da data do registro
 * ({@code docs/11} §3), nunca copiada da avaliação: idade copiada envelheceria
 * calada.
 *
 * <p>O vínculo com a {@link AvaliacaoPediatrica} é <b>opcional</b>: criança que
 * internou de madrugada tem dia antes de avaliação, e exigir o vínculo travaria
 * o uso à beira do leito. Quando existe, é ele que dá as metas (VET e DRI) e a
 * composição da fórmula.
 */
@Entity
@Table(name = "registro_diario_pediatrico")
@Getter
@Setter
@NoArgsConstructor
public class RegistroDiarioPediatrico extends TenantEntity {

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
    private AvaliacaoPediatrica avaliacao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    // ─── Antropometria do dia ───────────────────────────────────────────

    /** A medida do dia, não a da avaliação — é o que faz a curva andar. */
    @Column(name = "peso_kg")      private BigDecimal pesoKg;

    /** Muda devagar: medir todo dia não é esperado, e por isso é opcional. */
    @Column(name = "estatura_cm")  private BigDecimal estaturaCm;

    // ─── Dieta láctea ───────────────────────────────────────────────────
    @Column(name = "vol_prescrito_24h") private BigDecimal volPrescrito24h;
    @Column(name = "vol_recebido_24h")  private BigDecimal volRecebido24h;

    /**
     * Tomadas, não refeições.
     *
     * <p>Lactente em fórmula láctea não faz café, almoço e ceia — o dia de UTI
     * tem seis colunas de refeição porque o paciente adulto come assim. Aqui a
     * unidade é a tomada, como {@code docs/09} §7 já modela a dieta.
     */
    @Column(name = "tomadas_previstas") private Integer tomadasPrevistas;
    @Column(name = "tomadas_aceitas")   private Integer tomadasAceitas;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
}

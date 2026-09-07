package com.nutri.hospitalar.clinica.entity;

import com.nutri.hospitalar.baseentity.BaseEntity;
import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
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

/**
 * Uma pergunta de um modelo de ficha.
 *
 * <p><b>Não tem {@code tenant}</b>, e isso é decisão e não esquecimento: o
 * modelo dono pode ser do sistema, com tenant nulo, então um tenant obrigatório
 * aqui seria impossível de preencher, e um anulável seria uma segunda fonte de
 * verdade para a mesma resposta. O campo é sempre alcançado <b>através</b> do
 * modelo, e o modelo é a guarda de isolamento.
 *
 * <p>O campo é mutável e pode ser apagado — por isso a resposta guarda o
 * <b>retrato</b> dele, e não uma referência. Ver {@link RespostaFicha}.
 */
@Entity
@Table(name = "campo_ficha")
@Getter
@Setter
@NoArgsConstructor
public class CampoFicha extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private ModeloFicha modelo;

    /** Agrupa as perguntas na tela e no papel. Nulo cai num bloco sem título. */
    @Column(name = "secao", length = 200)
    private String secao;

    @Column(name = "rotulo", nullable = false, length = 300)
    private String rotulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoCampoFicha tipo;

    /** JSON array. Obrigatório nos dois tipos de opção — o banco também exige. */
    @Column(name = "opcoes", columnDefinition = "TEXT")
    private String opcoes;

    /** Posição na lista, reatribuída pelo service a cada gravação. */
    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    @Column(name = "obrigatorio", nullable = false)
    private Boolean obrigatorio = false;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}

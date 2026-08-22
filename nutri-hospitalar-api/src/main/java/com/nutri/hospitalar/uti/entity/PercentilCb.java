package com.nutri.hospitalar.uti.entity;

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
 * P50 da circunferência do braço por faixa de idade e sexo — o denominador da
 * adequação de CB. Ver {@code docs/10} §2.8.
 *
 * <p>Estende {@link BaseEntity} e não {@code TenantEntity}: é dado de
 * referência, e nenhum hospital tem a sua própria tabela de percentil.
 * <b>Somente leitura</b>, semeada pela migration {@code 020}.
 *
 * <p>A faixa é <b>fechada nas duas pontas</b> — {@code idadeMin <= idade <=
 * idadeMax} — e é por isso que ela existe como par de colunas em vez de um
 * corte só. Na planilha o {@code PROCV} de faixa devolvia a linha anterior no
 * limite: aos 59 anos ela lê o P50 da faixa 30–39,9 (32,3 cm) em vez do da
 * 50–59,9 (32,6 cm). É o defeito 19 de {@code docs/10} §11.
 */
@Entity
@Table(name = "percentil_cb")
@Getter
@Setter
@NoArgsConstructor
public class PercentilCb extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 20)
    private Sexo sexo;

    /** Idade em anos, com uma casa: a faixa 50–59,9 tem max = 59.9. */
    @Column(name = "idade_min", nullable = false)
    private BigDecimal idadeMin;

    @Column(name = "idade_max", nullable = false)
    private BigDecimal idadeMax;

    /** P50 da CB daquela faixa, em cm. */
    @Column(name = "p50_cm", nullable = false)
    private BigDecimal p50Cm;
}

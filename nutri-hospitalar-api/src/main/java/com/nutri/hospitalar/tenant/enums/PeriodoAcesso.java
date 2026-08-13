package com.nutri.hospitalar.tenant.enums;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

/**
 * Prazo da licença do cliente, escolhido quando o tenant nasce e recontado a
 * cada renovação.
 *
 * <p>{@code INDETERMINADO} é acesso livre — sem data de expiração e sem rotina
 * que o desligue. É o default deliberado: o valor que nunca derruba um cliente
 * por esquecimento.
 */
public enum PeriodoAcesso {

    INDETERMINADO(null),
    UM_MES(Period.ofMonths(1)),
    DOIS_MESES(Period.ofMonths(2)),
    TRES_MESES(Period.ofMonths(3)),
    SEIS_MESES(Period.ofMonths(6)),
    UM_ANO(Period.ofYears(1)),
    DOIS_ANOS(Period.ofYears(2));

    private final Period duracao;

    PeriodoAcesso(Period duracao) {
        this.duracao = duracao;
    }

    /**
     * Fim do último dia do período, no fuso informado — {@code null} em
     * {@code INDETERMINADO}.
     *
     * <p>Expira no fim do dia, não no horário exato da contratação: "1 mês"
     * contratado dia 13 vale o dia 13 inteiro do mês seguinte. Cliente não
     * perde acesso no meio do expediente por causa do relógio.
     */
    public Instant expiracaoAPartirDe(LocalDate base, ZoneId zona) {
        return duracao == null
                ? null
                : base.plus(duracao).atTime(LocalTime.MAX).atZone(zona).toInstant();
    }
}

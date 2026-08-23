package com.nutri.hospitalar.uti.enums;

/**
 * Fase da terapia nutricional, que define a faixa de kcal/kg e g/kg.
 * Ver {@code docs/10} §3.1.
 *
 * <p>A faixa de proteína em reabilitação é rotulada {@code >1,5} na planilha, mas
 * as fórmulas calculam 1,5 × peso e 2,0 × peso — então a faixa implementada é
 * <b>1,5 a 2,0</b>.
 */
public enum FaseTerapia {

    AGUDA("Fase aguda"),
    REABILITACAO("Reabilitação");

    private final String descricao;

    FaseTerapia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

package com.nutri.hospitalar.uti.enums;

import java.math.BigDecimal;

/**
 * Terapia renal substitutiva — quando presente, ela <b>substitui</b> a faixa
 * proteica da fase. Ver {@code docs/10} §3.2.
 */
public enum TerapiaRenal {

    NENHUMA("Nenhuma", null),
    HEMODIALISE_INTERMITENTE("Hemodiálise intermitente", new BigDecimal("1.8")),
    HEMODIALISE_CONTINUA("Hemodiálise contínua", new BigDecimal("2.0"));

    private final String descricao;

    /** g/kg/dia. Nulo em {@link #NENHUMA} — ausência, não zero. */
    private final BigDecimal proteinaGKg;

    TerapiaRenal(String descricao, BigDecimal proteinaGKg) {
        this.descricao = descricao;
        this.proteinaGKg = proteinaGKg;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getProteinaGKg() {
        return proteinaGKg;
    }
}
